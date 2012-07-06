///
/// BuildWatcher.cs created by Joe Freeman joe@freemansoft.com
/// 
namespace BuildWatcher
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Net;
    using log4net;
    using Microsoft.TeamFoundation.Build.Client;
    using Microsoft.TeamFoundation.Client;
    using Microsoft.TeamFoundation.VersionControl.Client;

    /// <summary>
    /// Contains all of the functionaly required to talk with TFS
    /// </summary>
    public class TfsBuildAdapter
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(BuildWatchDriver));

        /// <summary>
        /// Initializes a new instance of the <see cref="TsBuildAdapter"/> class. 
        /// Constructor for the instance that does the work
        /// </summary>
        /// <param name="connection">the connection we use to talk to the TFS server</param>
        /// <param name="teamProjectName">our project name</param>
        /// <param name="definitionNamePattern">a pattern used to select build definitions</param> 
        public TfsBuildAdapter(TfsBuildConnection connection, string teamProjectName, string definitionNamePattern)
        {
            if (connection != null)
            {
                this.Connection = connection;
            }
            else
            {
                throw new ArgumentException("Connection must be configured");
            }

            if (definitionNamePattern != null)
            {
                this.DefinitionNamePattern = definitionNamePattern;
            }
            else
            {
                throw new ArgumentException("DefinitionNamePattern must be set");
            }

            //// this should be in it's own method
            if (teamProjectName != null)
            {
                VersionControlServer versionControlServer = (VersionControlServer)connection.TfsTeamProjects.GetService<VersionControlServer>();
                this.OurTeamProject = versionControlServer.GetTeamProject(teamProjectName);
            }
            else
            {
                throw new ArgumentException("TeamProjectName must be set");
            }
        }

        /// <summary>
        /// Gets or sets team project from config file
        /// </summary>
        public TeamProject OurTeamProject { get; set; }

        /// <summary>
        /// Gets or sets build definition name pattern from config file
        /// </summary>
        public string DefinitionNamePattern { get; set; }

        /// <summary>
        /// Gets or sets build connection created from configuration info
        /// </summary>
        public TfsBuildConnection Connection { get; set; }

        /// <summary>
        /// Gets all of the Team Projects from the configured TFS server.  The Build definitions are one layer down from here
        /// </summary>
        /// <returns>All team projects from the server we are connected to</returns>
        public TeamProject[] GetAllProjects()
        {
            VersionControlServer versionControlServer = (VersionControlServer)this.Connection.TfsTeamProjects.GetService<VersionControlServer>();
            //// get all top level build collections
            TeamProject[] allProjects = versionControlServer.GetAllTeamProjects(true);
            return allProjects;
        }

        /// <summary>
        /// gets all build definitiosn fo rthe configure dpojrect
        /// </summary>
        /// <returns>definitions that match</returns>
        public IBuildDefinition[] GetBuildDefinitions()
        {
            return this.GetBuildDefinitions(this.OurTeamProject, this.DefinitionNamePattern);
        }

        /// <summary>
        /// gets all the definitions for the specified project that match the definition name
        /// </summary>
        /// <param name="oneProject">the project we are searching</param>
        /// <param name="definitionName">wildcards supported</param>
        /// <returns>list of build definitions</returns>
        public IBuildDefinition[] GetBuildDefinitions(TeamProject oneProject, string definitionName)
        {
            IBuildDefinitionSpec spec;
            if (definitionName != null)
            {
                spec = this.Connection.BuildServer.CreateBuildDefinitionSpec(oneProject.Name, definitionName);
            }
            else
            {
                spec = this.Connection.BuildServer.CreateBuildDefinitionSpec(oneProject.Name);
            }
            IBuildDefinitionQueryResult buildDefQueryRsult = this.Connection.BuildServer.QueryBuildDefinitions(spec);
            return buildDefQueryRsult.Definitions;
        }


        /// <summary>
        /// Retrieves the last two builds for all of the builds in our that match our definition name
        /// </summary>
        /// <returns>build results</returns>
        public LastTwoBuildResults[] GetLastTwoBuilds()
        {
            return GetLastTwoBuilds(this.OurTeamProject, this.DefinitionNamePattern);
        }

        /// <summary>
        /// Rets the last two builds for all build definitions matching the passe din parameters
        /// </summary>
        /// <param name="teamProject">the team project to uery against</param>
        /// <param name="buildDefinitionPattern">optional build definition pattern</param>
        /// <returns>build result pairs</returns>
        public LastTwoBuildResults[] GetLastTwoBuilds(TeamProject teamProject, String buildDefinitionPattern)
        {
            log.Debug("Finding build results for definition pattern " + buildDefinitionPattern);
            IBuildDetailSpec buildDetailsQuerySpec;
            if (buildDefinitionPattern != null)
            {
                buildDetailsQuerySpec = this.Connection.BuildServer.CreateBuildDetailSpec(teamProject.Name, buildDefinitionPattern);
            }
            else
            {
                buildDetailsQuerySpec = this.Connection.BuildServer.CreateBuildDetailSpec(teamProject.Name);
            }
            //// last and previous
            buildDetailsQuerySpec.MaxBuildsPerDefinition = 2;
            //// use start time descending because InProgress builds don't seem to sort correctly when using EndTimeDescending
            buildDetailsQuerySpec.QueryOrder = BuildQueryOrder.StartTimeDescending;
            IBuildQueryResult buildResults = this.Connection.BuildServer.QueryBuilds(buildDetailsQuerySpec);

            IDictionary<string, LastTwoBuildResults> results = new Dictionary<string, LastTwoBuildResults>();
            foreach (IBuildDetail oneDetail in buildResults.Builds)
            {
                if (!results.ContainsKey(oneDetail.BuildDefinition.Name))
                {
                    results.Add(oneDetail.BuildDefinition.Name, new LastTwoBuildResults(oneDetail.BuildDefinition, null, null));
                }
            }
            foreach (IBuildDetail oneDetail in buildResults.Builds)
            {
                LastTwoBuildResults corresponding = results[oneDetail.BuildDefinition.Name];
                //// sorted by start time descending so latest should always come first
                if (corresponding.LastBuild == null)
                {
                    corresponding.LastBuild = oneDetail;
                }
                else
                {
                    corresponding.PreviousBuild = oneDetail;
                }
            }
            if (log.IsDebugEnabled)
            {
                foreach (String key in results.Keys)
                {
                    LastTwoBuildResults oneResult = results[key];
                    log.Debug(" Build Definition " + oneResult.BuildDefinition.Name);
                    log.Debug("  Build " + oneResult.LastBuild.BuildNumber + " " + oneResult.LastBuild.Status);
                    if (oneResult.PreviousBuild != null)
                    {
                        log.Debug("  Build " + oneResult.PreviousBuild.BuildNumber + " " + oneResult.PreviousBuild.Status);
                    }
                }
            }
            LastTwoBuildResults[] resultsAsArray = new LastTwoBuildResults[results.Values.Count];
            results.Values.CopyTo(resultsAsArray, 0);
            return resultsAsArray;
        }

        /// <summary>
        /// Retrieves the queued builds for the Team Project this class is configured for
        /// </summary>
        /// <returns>all of the queued builds on the server</returns>
        public IQueuedBuild[] GetQueuedBuilds()
        {
            return this.GetQueuedBuilds(this.OurTeamProject, null);
        }

        /// <summary>
        /// Retrieves the queued builds for a specific build definition in the specified project
        /// </summary>
        /// <param name="oneProject">project we are interested in</param>
        /// <param name="definitionName">the build definition we are looking at to check for queued, null means no matching on name</param>
        /// <returns>an array of queued builds that match the parameters criterea</returns>
        public IQueuedBuild[] GetQueuedBuilds(TeamProject oneProject, string definitionName)
        {
            IQueuedBuildSpec qbs;
            if (definitionName != null)
            {
                qbs = this.Connection.BuildServer.CreateBuildQueueSpec(oneProject.Name, definitionName);
            }
            else
            {
                qbs = this.Connection.BuildServer.CreateBuildQueueSpec(oneProject.Name);
            }
            IQueuedBuildQueryResult foundQueuedBuilds = this.Connection.BuildServer.QueryQueuedBuilds(qbs);
            IQueuedBuild[] extractedQueuedBuilds = foundQueuedBuilds.QueuedBuilds;
            return extractedQueuedBuilds;
        }

        /// <summary>
        /// used to tell user if any builds in this set are in progress
        /// </summary>
        /// <param name="buildResults">buld results to be analyzed</param>
        /// <returns>true if any build in the result set is In Progress</returns>
        public bool SomeoneIsBuilding(LastTwoBuildResults[] buildResults)
        {
            foreach (LastTwoBuildResults buildResult in buildResults)
            {
                if (buildResult.LastBuild.Status == BuildStatus.InProgress)
                {
                    log.Debug("Found build in progress: " + buildResult.BuildDefinition.Name);
                    return true;
                }
            }

            return false;
        }

        /// <summary>
        /// success means !failure in this method
        /// </summary>
        /// <param name="buildResults">build results to be analyzed</param>
        /// <returns>true of all of the last actual builds for each result was successful looks earlier if last is in progress</returns>
        public bool AllLastBuildsWereSuccessful(LastTwoBuildResults[] buildResults)
        {
            foreach (LastTwoBuildResults buildResult in buildResults)
            {
                if (buildResult.LastBuild.Status == BuildStatus.InProgress)
                {
                    // don't return false if in progress and was no previous build
                    if (buildResult.PreviousBuild != null
                        && buildResult.PreviousBuild.Status != BuildStatus.Succeeded)
                    {
                        log.Debug("Found previous build that was not successful " + buildResult.BuildDefinition.Name + " - " + buildResult.PreviousBuild.Status);
                        return false;
                    }
                }
                else if (buildResult.LastBuild != null
                        && buildResult.LastBuild.Status != BuildStatus.Succeeded)
                {
                    log.Debug("Found last build that was not successful " + buildResult.BuildDefinition.Name + " - " + buildResult.LastBuild.Status);
                    //// not in progress and last build did not succeed
                    return false;
                }
            }
            //// assume success
            return true;
        }

        /// <summary>
        /// success means !failure in this method
        /// </summary>
        /// <param name="buildResults">a set of build results</param>
        /// <returns>true if all of the build results are successful or partiallys succssful</returns>
        public bool AllLastBuildsWerePartiallySuccessful(LastTwoBuildResults[] buildResults)
        {
            foreach (LastTwoBuildResults buildResult in buildResults)
            {
                if (buildResult.LastBuild.Status == BuildStatus.InProgress)
                {
                    // don't return false if in progress and was no previous build
                    if (buildResult.PreviousBuild != null
                        && buildResult.PreviousBuild.Status != BuildStatus.Succeeded
                        && buildResult.PreviousBuild.Status != BuildStatus.PartiallySucceeded)
                    {
                        log.Debug("Found previous build that failed" + buildResult.BuildDefinition.Name);
                        return false;
                    }
                }
                else if (buildResult.LastBuild != null
                        && buildResult.LastBuild.Status != BuildStatus.Succeeded
                        && buildResult.LastBuild.Status != BuildStatus.PartiallySucceeded)
                {
                    log.Debug("Found last build that failed " + buildResult.BuildDefinition.Name);
                    // not in progress and last build did not succeed
                    return false;
                }
            }

            // assume success
            return true;
        }

        /// <summary>
        /// Totally dummy method derived from something on the internet I didn't fell like deleting yet
        /// </summary>
        public void GrabAllBuildsForAllTime()
        {
            TeamProject[] allProjects = this.GetAllProjects();

            // move across all Team projects listing all build results for all time
            foreach (TeamProject proj in allProjects)
            {
                IBuildDetail[] buildDetailsForNamedBuild = this.Connection.BuildServer.QueryBuilds(proj.Name);
                foreach (IBuildDetail build in buildDetailsForNamedBuild)
                {
                    IBuildDefinition buildDef = build.BuildDefinition;
                    string oneBuildName = buildDef.Name;
                    BuildStatus oneBuildStatus = build.Status;
                    log.Debug(oneBuildName + " current status: " + oneBuildStatus);
                }
            }
        }
    }
}
