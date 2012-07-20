namespace BuildWatcher
{
    using System;
    using System.Collections.Generic;
    using System.Configuration;
    using System.IO;
    using System.IO.Ports;
    using System.Text;
    using System.Net;
    //// a log4net dependency  caused me to have to 
    //// change the VS2010 target from .Net Framework 4 Client Profile to .Net Framework 4
    using log4net;
    using Microsoft.TeamFoundation;
    using Microsoft.TeamFoundation.Framework.Client; //// sometimes we lose a DB in the middle of the night
    using Microsoft.TeamFoundation.Build.Client;
    using Microsoft.TeamFoundation.VersionControl.Client;

    public static class BuildWatchDriver
    {
        private static ILog log = null;

        /// <summary>
        /// main program that instantiates the watcher and runs it
        /// </summary>
        /// <param name="args">some random arguments</param>
        public static void Main(string[] args)
        {
            log4net.Config.BasicConfigurator.Configure();
            log = log4net.LogManager.GetLogger(typeof(BuildWatchDriver));

            //// comment out these two lines if you don't have an arduino
            SerialPort port = ConfigureSerialPort();
            ArduinoDualRGB device = new ArduinoDualRGB(port,
                Convert.ToBoolean(ConfigurationManager.AppSettings["ArduinoResetOnConnect"]),
                Convert.ToInt32(ConfigurationManager.AppSettings["ArduinoNumberOfLamps"]));
            //// end comment out

            TfsBuildConnection ourBuildConnection = new TfsBuildConnection(
                ConfigurationManager.AppSettings["TfsUrl"],
                ConfigurationManager.AppSettings["TfsUsername"],
                ConfigurationManager.AppSettings["TfsPassword"],
                ConfigurationManager.AppSettings["TfsDomain"]
              );

            List<TfsBuildAdapter> allAdapters = new List<TfsBuildAdapter>();

            //// we don't verify the length of the list vs the hardware's actual number of lights
            int index = 1;
            while (ConfigurationManager.AppSettings["TeamProjectName" + index] != null)
            {
                log.Debug("Assembling Build adapter " + index + " for " + ConfigurationManager.AppSettings["BuildDefinitionNamePattern" + index]);
                TfsBuildAdapter ourBuildWatcher = new TfsBuildAdapter(
                    ourBuildConnection,
                    ConfigurationManager.AppSettings["TeamProjectName" + index],
                    ConfigurationManager.AppSettings["BuildDefinitionNamePattern" + index]
                    );
                allAdapters.Add(ourBuildWatcher);
                index++;
            }


            //// this never returns so comment this out if you just want to see status of server on console
            //// comment out this block if you don't have an arduino
            MonitorStatus(allAdapters, device);
            port.Dispose();
            //// end comment out

            //// reenable this block if you want to test without any Extreme Feedback Device (lights)
            //// some gratuitous demonstration code that shows how we get all this stuff
            //// ignore the configured project name and build definition pattern and search everything
            ////BuildWatcher.QueryWholeTree(ourBuildWatcher);
            //// here we use the configuration in the build watcher
            ////BuildWatcher.QueryViaConfiguration(ourBuildWatcher);
            //// end reenable blocks
        }

        /// <summary>
        /// Queries TFS for each build adapter and updates the passed in device.  
        /// The device is assumed to support multiple lamps , one for each adapter
        /// </summary>
        /// <param name="allAdapters">a list of build specifications</param>
        /// <param name="device">our potentially multi-indicator device</param>
        private static void MonitorStatus(List<TfsBuildAdapter> allAdapters, ArduinoDualRGB device)
        {
            while (true)
            {
                int index = 0;
                try
                {
                    //// we share a connection between all adapters so probably can get by only connecting once per sweep (or less)
                    allAdapters[0].Connection.Connect();
                    foreach (TfsBuildAdapter ourBuildWatcher in allAdapters)
                    {
                        //// if they shared a connection we might only have to connect on the first one in the list
                        //// how often do we really need to get new build definitions?
                        LastTwoBuildResults[] buildResults = ourBuildWatcher.GetLastTwoBuilds();
                        if (buildResults.Length > 0)
                        {
                            bool someoneIsBuilding = ourBuildWatcher.SomeoneIsBuilding(buildResults);
                            bool allLastBuildsWereSuccessful = ourBuildWatcher.AllLastBuildsWereSuccessful(buildResults);
                            if (allLastBuildsWereSuccessful)
                            {
                                device.SetColor(index, 0, 13, 8); // green with blue is good
                            }
                            else
                            {
                                bool allLastBuildsWerePartiallySuccessful = ourBuildWatcher.AllLastBuildsWerePartiallySuccessful(buildResults);
                                if (allLastBuildsWerePartiallySuccessful)
                                {
                                    device.SetColor(index, 12, 9, 0); // pink is partial
                                }
                                else
                                {
                                    device.SetColor(index, 13, 0, 0); // red is broken
                                }
                            }

                            if (someoneIsBuilding)
                            {
                                device.SetBlink(index, 3, 3);
                            }
                            else
                            {
                                device.SetBlink(index, 3, 0);
                            }
                        }
                        index++;
                    }
                }
                catch (TeamFoundationServiceUnavailableException e)
                {
                    log.Error("Server unavailable " + e);
                    //// could do additional pause here but just assume some transient problem
                }
                catch (IOException e)
                {
                    log.Error("IO Exception - often unexpected eof " + e);
                    //// could do additional pause here but just assume some transient problem
                }
                catch (WebException e)
                {
                    log.Error("WebException - sometimes a timeout reading from stream if system is busy or goes down" + e);
                }
                catch (DatabaseConnectionException e)
                {
                    log.Error("Database gone " + e);
                    System.Threading.Thread.Sleep(Convert.ToInt32(ConfigurationManager.AppSettings["ExceptionPauseInMilliseconds"]));
                }
                System.Threading.Thread.Sleep(Convert.ToInt32(ConfigurationManager.AppSettings["PollPauseInMilliseconds"]));
            }
        }

        /// <summary>
        /// configurs a serial port base on the Cofniguratin manager settings
        /// </summary>
        /// <returns>a configured serial port</returns>
        private static SerialPort ConfigureSerialPort()
        {
            SerialPort port = new SerialPort();
            port.PortName = ConfigurationManager.AppSettings["ArduinoSerialPort"];
            port.BaudRate = Convert.ToInt32(ConfigurationManager.AppSettings["ArduinoBaudRate"]);
            port.Handshake = Handshake.None;
            port.ReadTimeout = 500;
            port.WriteTimeout = 500;
            port.Encoding = Encoding.ASCII;
            port.Open();
            if (!port.IsOpen)
            {
                throw new ApplicationException("Unable to talk with device");
            }

            return port;
        }

        /// <summary>
        /// demonstrate how we would actually use this when monitoring a set of configured builds
        /// </summary>
        /// <param name="ourBuildWatcher">The Build adapter we use to communicat with TFS</param>
        private static void QueryViaConfiguration(TfsBuildAdapter ourBuildWatcher)
        {
            //// query using our loaded configuration
            {
                IBuildDefinition[] ourBuildDefinitions = ourBuildWatcher.GetBuildDefinitions();
                log.Debug("Project " + ourBuildWatcher.OurTeamProject.Name + " with " + ourBuildDefinitions.Length + " " + ourBuildWatcher.DefinitionNamePattern + " Build definitions");
                IQueuedBuild[] allQueuedForProj = ourBuildWatcher.GetQueuedBuilds();
                log.Debug("Project " + ourBuildWatcher.OurTeamProject.Name + " with " + allQueuedForProj.Length + " queued builds");

                LastTwoBuildResults[] resultsForCIBuilds = ourBuildWatcher.GetLastTwoBuilds();
                bool someoneIsBuilding = ourBuildWatcher.SomeoneIsBuilding(resultsForCIBuilds);
                bool allLastBuildsWereSuccessful = ourBuildWatcher.AllLastBuildsWereSuccessful(resultsForCIBuilds);
                bool allLastBuildsWerePartiallySuccessful = ourBuildWatcher.AllLastBuildsWerePartiallySuccessful(resultsForCIBuilds);
                log.Debug("Someone is Building: " + someoneIsBuilding
                    + " all completed builds successful: " + allLastBuildsWereSuccessful
                    + " all completed builds partially successful: " + allLastBuildsWerePartiallySuccessful
                    );
            }
        }

        /// <summary>
        /// show how we would do this in a "non configured" situation where we wanted to walk the whole tree
        /// </summary>
        /// <param name="ourBuildWatcher">the build connection we use</param>
        private static void QueryWholeTree(TfsBuildAdapter ourBuildWatcher)
        {
            //// query the whole tree
            TeamProject[] allProjects = ourBuildWatcher.GetAllProjects();
            foreach (TeamProject oneProject in allProjects)
            {
                IBuildDefinition[] allDefsForProj = ourBuildWatcher.GetBuildDefinitions(oneProject, null);
                log.Debug("Project " + oneProject.Name + " with " + allDefsForProj.Length + " build definitions");
                IBuildDefinition[] ciBuildDefinitions = ourBuildWatcher.GetBuildDefinitions(oneProject, "CI*");
                log.Debug("Project " + oneProject.Name + " with " + ciBuildDefinitions.Length + " CI Build definitions");
                IQueuedBuild[] allQueuedForProj = ourBuildWatcher.GetQueuedBuilds(oneProject, null);
                log.Debug("Project " + oneProject.Name + " with " + allQueuedForProj.Length + " queued builds");
            }
        }
    }
}
