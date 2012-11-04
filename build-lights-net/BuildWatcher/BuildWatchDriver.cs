namespace BuildWatcher
{
    using System;
    using System.Collections.Generic;
    using System.Configuration;
    using System.IO;
    using System.IO.Ports;
    using System.Net;
    using System.Text;
    //// a log4net dependency  caused me to have to 
    //// change the VS2010 target from .Net Framework 4 Client Profile to .Net Framework 4
    using log4net;
    using log4net.Config;
    using Microsoft.TeamFoundation;
    using Microsoft.TeamFoundation.Build.Client;
    using Microsoft.TeamFoundation.Framework.Client; //// sometimes we lose a DB in the middle of the night
    using Microsoft.TeamFoundation.VersionControl.Client;
    using BuildWatcher.Devices;
    using Spring.Context;
    using Spring.Context.Support;
    using BuildWatcher.Tfs;
    using BuildWatcher.Http;

    /// <summary>
    /// The main run loop for the build watcher
    /// </summary>
    public class BuildWatchDriver
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = null;

        /// <summary>
        /// spring wired list of build adapters, build sets to be monitored
        /// </summary>
        private List<TfsBuildAdapter> allAdapters;
        /// <summary>
        /// spring wired device that displays the build status
        /// </summary>
        private IBuildIndicatorDevice device;
        /// <summary>
        /// HTTP Listener wrapper for web page
        /// </summary>
        private HttpListenerWrapper httpListenerWrapper;

        /// <summary>
        /// delay between loops
        /// </summary>
        private int pollPauseInMilliseconds;
        /// <summary>
        /// delay after receiving any exception, give server time to recover
        /// </summary>
        private int exceptionPauseInMilliseconds;

        /// <summary>
        /// main program that instantiates the watcher and runs it
        /// </summary>
        public static void Main()
        {
            //// need to make the GetLogger call as early as possible before any external assemblies have been loaded and invoked
            //// http://logging.apache.org/log4net/release/manual/configuration.html
            //// load from App.config
            log4net.Config.XmlConfigurator.Configure();
            log = log4net.LogManager.GetLogger(typeof(BuildWatchDriver));
            //// configure spring http://www.springframework.net
            IApplicationContext ctx = ContextRegistry.GetContext();

            try
            {
                //// the only thing I don't like about the spring config is that connection initialization errors are buried
                //// so we break out this bean retreival separately so we can identify TFS connection problems
                ctx.GetObject("myBuildServerConnection");
            } catch (Spring.Objects.Factory.ObjectCreationException e) {
                log.Error("Unable to connect to the TFS server.  Check configuration in App.config and that your server is up", e);
                return;
            }
            //// get our driver from spring and run the Monitor loop
            BuildWatchDriver myDriverInstance =
                ctx.GetObject("myDriverInstance") as BuildWatchDriver;
            myDriverInstance.MonitorStatus();
        }

        /// <summary>
        /// Instantiantes an instanceof the driver with it's configuration parameters
        /// </summary>
        /// <param name="allAdapters"></param>
        /// <param name="device"></param>
        public BuildWatchDriver(List<TfsBuildAdapter> allAdapters, IBuildIndicatorDevice device,
            int pollPauseInMilliseconds,
            int exceptionPauseInMilliseconds,
            HttpListenerWrapper httpListenerWrapper)
        {
            this.allAdapters = allAdapters;
            this.device = device;
            this.pollPauseInMilliseconds = pollPauseInMilliseconds;
            this.exceptionPauseInMilliseconds = exceptionPauseInMilliseconds;
            this.httpListenerWrapper = httpListenerWrapper;
        }

        /// <summary>
        /// Queries TFS for each build adapter and updates the passed in device.  
        /// The device is assumed to support multiple lamps , one for each adapter
        /// </summary>
        /// <param name="allAdapters">a list of build specifications</param>
        /// <param name="device">our potentially multi-indicator device</param>
        private  void MonitorStatus()
        {
            // this may fail silently if the URL isn't right or doesn't have permissions to open port
            this.httpListenerWrapper.Start();
            while (true)
            {
                int index = 0;
                try
                {
                    //// we share a connection between all adapters so probably can get by only connecting once per sweep (or less)
                    allAdapters[index].Connection.Connect();
                    foreach (TfsBuildAdapter ourBuildWatcher in allAdapters)
                    {
                        //// if they shared a connection we might only have to connect on the first one in the list
                        //// how often do we really need to get new build definitions?
                        TfsLastTwoBuildResults[] buildResults = ourBuildWatcher.GetLastTwoBuilds();
                        if (buildResults.Length > 0)
                        {
                            int someoneIsBuildingCount = ourBuildWatcher.SomeoneIsBuilding(buildResults);
                            int lastBuildsWereSuccessfulCount = ourBuildWatcher.NumberOfSuccessfulBuilds(buildResults);
                            int lastBuildsWerePartiallySuccessfulCount = ourBuildWatcher.NumberOfPartiallySuccessfulBuilds(buildResults);
                            if (device != null)
                            {
                                device.Indicate(index, buildResults.Length, lastBuildsWereSuccessfulCount, lastBuildsWerePartiallySuccessfulCount, someoneIsBuildingCount);
                            }
                            if (this.httpListenerWrapper != null)
                            {
                                this.httpListenerWrapper.AddData(ourBuildWatcher.DefinitionNamePattern, buildResults);
                            }
                        }
                        index++;
                    }
                }

                catch (TeamFoundationServiceUnavailableException e)
                {
                    log.Error("Server unavailable " + e);
                    this.IndicateProblem();
                }
                catch (TeamFoundationServerInvalidResponseException e)
                {
                    //// our lame server sometimes returns this with Http code 500 "The number of HTTP requests per minute exceeded the configured limit"
                    log.Error("Invalid Response , probably a 500: " + e);
                    //// the problem usually is transient so lets try again
                    this.IndicateProblem();
                }
                catch (IOException e)
                {
                    log.Error("IO Exception - often unexpected eof " + e);
                    this.IndicateProblem();
                }
                catch (WebException e)
                {
                    log.Error("WebException - sometimes a timeout reading from stream if system is busy or goes down" + e);
                    this.IndicateProblem();
                }
                catch (DatabaseConnectionException e)
                {
                    log.Error("Database gone " + e);
                    this.IndicateProblem();
                }
                catch (BuildServerException e)
                {
                    log.Error("TF246021 sometimes a SQL server error under the hood " + e);
                    this.IndicateProblem();
                }
                catch (System.Xml.XmlException e)
                {
                    log.Error("Weird parsing or incomplete XML.  Usually a something in the night thing so retry after somee sleep " + e);
                    this.IndicateProblem();
                }
                System.Threading.Thread.Sleep(pollPauseInMilliseconds);
            }

        }

        /// <summary>
        /// flash all lights or signals that there is a problem
        /// </summary>
        /// <param name="allAdapters">list of build sets, number of lights</param>
        /// <param name="device">actual device</param>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Performance", "CA1804:RemoveUnusedLocals", MessageId = "ourBuildWatcher")]
        private void IndicateProblem()
        {
            int index = 0;
            foreach (TfsBuildAdapter ourBuildWatcher in allAdapters)
            {
                device.IndicateProblem(index++);
            }
            System.Threading.Thread.Sleep(exceptionPauseInMilliseconds);
        }

    }
}
