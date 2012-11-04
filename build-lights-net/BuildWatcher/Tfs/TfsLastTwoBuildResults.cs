namespace BuildWatcher.Tfs
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
    /// Holds last two build results. We need the previous build result in case the latest build is still running
    /// </summary>
    public class TfsLastTwoBuildResults
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="TfsLastTwoBuildResults"/> class. 
        /// a result container that holds a build definition and the results of the last two builds
        /// </summary>
        /// <param name="buildDefinition">build definitin </param>
        /// <param name="lastBuild">last build</param>
        /// <param name="previousBuild">build before last</param>
        public TfsLastTwoBuildResults(IBuildDefinition buildDefinition, IBuildDetail lastBuild, IBuildDetail previousBuild)
        {
            this.BuildDefinition = buildDefinition;
            this.LastBuild = lastBuild;
            this.PreviousBuild = previousBuild;
        }

        /// <summary>
        /// Gets or sets the build definition
        /// </summary>
        public IBuildDefinition BuildDefinition { get; set; }

        /// <summary>
        /// Gets or sets results of the last build
        /// </summary>
        public IBuildDetail LastBuild { get; set; }

        /// <summary>
        /// Gets or sets results of the build before the last one in case the last one is in progress
        /// </summary>
        public IBuildDetail PreviousBuild { get; set; }
    }
}