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
    /// An active TFS connection used by the BuildAdapter retrieve build information.
    /// Located in a separate class so we can share across monitors
    /// </summary>
    public class TfsBuildConnection
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="TfsBuildConnection"/> class.
        /// </summary>
        /// <param name="tfsPath">pathto tfs build server </param>
        /// <param name="userId">user id credentials</param>
        /// <param name="password">password credentials</param>
        /// <param name="domain">authenticating domain, might be different from this program</param>
        public TfsBuildConnection(string tfsPath, string userId, string password, string domain)
        {
            this.NetCredentials = new NetworkCredential(userId, password, domain);
            this.TfsUrl = new Uri(tfsPath);
            this.Connect();
        }

        /// <summary>
        ///  Gets or sets the url to the TFS server retrieved from configuration
        /// </summary>
        public Uri TfsUrl { get; set; }
        
        /// <summary>
        /// Gets or sets the network credentials created from userid, password and domain
        /// </summary>
        public NetworkCredential NetCredentials { get; set; }

        /// <summary>
        /// Gets or sets the team project collection found by connecting to the TFS server
        /// </summary>
        public TfsTeamProjectCollection TfsTeamProjects { get; set; }

        /// <summary>
        /// Gets or sets build server we connected to with the connection information
        /// </summary>
        public IBuildServer BuildServer { get; set; }

        /// <summary>
        ///  separate method so we can reconnect using same object
        /// </summary>
        public void Connect()
        {
            if (this.NetCredentials != null)
            {
                this.TfsTeamProjects = new TfsTeamProjectCollection(this.TfsUrl, this.NetCredentials);
            }
            else
            {
                this.TfsTeamProjects = new TfsTeamProjectCollection(this.TfsUrl, new UICredentialsProvider());
            }

            this.TfsTeamProjects.EnsureAuthenticated();
            this.BuildServer = (IBuildServer)this.TfsTeamProjects.GetService<IBuildServer>();
        }
    }
}