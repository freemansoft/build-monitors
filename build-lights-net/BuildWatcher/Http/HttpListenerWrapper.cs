///
/// written by freeemansoft.com
/// 
/// A wrapper for the http listener that caches the build results.
/// We could have normalized under the Device API if we changed that API to take more than counts
/// 
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Net;
using System.Text;
using Microsoft.TeamFoundation.Build.Client;

using log4net;
using BuildWatcher.Tfs;

namespace BuildWatcher.Http
{
    /// <summary>
    /// A wrapper for an HttpListener class so that the program can response to web requests.
    /// Note that we only start ONE listener thread so only ONE person can connect at a time
    /// </summary>
    public class HttpListenerWrapper
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(HttpListenerWrapper));

        private HttpListener myListener;
        /// <summary>
        /// A dictionary of build results grouped by name patterns. 
        /// This is so that we know the grouping we want to create on the http page
        /// </summary>
        private Dictionary<String, TfsLastTwoBuildResults[]> buildResults = new Dictionary<String, TfsLastTwoBuildResults[]>();


        public HttpListenerWrapper(HttpListener myListener, string serviceUri)
        {
            // should we validate the service Uri and listener objects?
            this.myListener = myListener;
            log.Debug("Adding listener prefix " + serviceUri);
            myListener.Prefixes.Add(serviceUri);
        }

        /// <summary>
        /// external entities provide data for this page
        /// </summary>
        /// <param name="key">The grouping key, usually the build name pattern used to run the TFS query</param>
        /// <param name="oneBuildSet">The results of all the builds retrieved as a group under the key</param>
        public void AddData(String key, TfsLastTwoBuildResults[] oneBuildSet)
        {
            buildResults[key] =  oneBuildSet;
        }

        /// <summary>
        /// This is required so that we don't blow up in the constructor with configuration errors.
        /// Note that the error does not bubble up so the app will continue if the Start fails.  
        /// I'm ok with tihs because the web server feature is not core to my main purpose.
        /// </summary>
        /// <returns>true if we were able to Start and hook up the listener</returns>
        public bool Start()
        {
            try
            {
                myListener.Start();
                // we can support multiple listeners if we ran this command multiple times on many threads
                IAsyncResult result = myListener.BeginGetContext(new AsyncCallback(ListenerCallback), this);
                // could look at result
            }
            catch (HttpListenerException e)
            {
                log.Error("***********************");
                log.Error("Unable to configure web server that makes build results available on prefix."
                    +"It could be port # related if 'The parameter is incorrect' or permission related if 'Access is Denied'", e);
                log.Error("***********************");
                return false;
            }
            return true;
        }

        //// from MSDN examples
        public static void ListenerCallback(IAsyncResult result)
        {
            log.Debug("received http listener callback");
            HttpListenerWrapper listener = (HttpListenerWrapper)result.AsyncState;
            // Call EndGetContext to complete the asynchronous operation.
            HttpListenerContext context = listener.myListener.EndGetContext(result);
            HttpListenerRequest request = context.Request;
            // Obtain a response object.
            HttpListenerResponse response = context.Response;
            // Construct a response. 
            string responseString = "<HTML>";
            responseString += "<head>";
            responseString += "<META HTTP-EQUIV='REFRESH' CONTENT='10'>";
            responseString += "<title>Build Status last shown on " + DateTime.Now.ToShortTimeString() + "</title>";
            responseString += "</head>";
            responseString +="<BODY>";
            responseString += "<table cellspacing='0' border='1'>";
            foreach (String buildName in listener.buildResults.Keys)
            {
                responseString += "<tr><td bgcolor='silver' colspan='2'>" + buildName + "</td></tr>";
                TfsLastTwoBuildResults[] theBuildSet = listener.buildResults[buildName];
                foreach (TfsLastTwoBuildResults aResultPair in theBuildSet) 
                {
                    if (aResultPair.LastBuild != null){
                        IBuildDetail lastBuild =  aResultPair.LastBuild;
                        String bgcolor = "white";
                        if (lastBuild.Status == BuildStatus.Succeeded)
                        {
                            bgcolor = "green";
                        } 
                        else if (lastBuild.Status == BuildStatus.PartiallySucceeded)
                        {
                            bgcolor = "yellow";
                        }
                        else if (lastBuild.Status == BuildStatus.Failed)
                        {
                            bgcolor = "red";
                        }
                        else
                        {
                            bgcolor = "gray";
                        }
                        responseString += "<tr>";
                        responseString += "<td bgcolor='" + bgcolor + "' >" + lastBuild.BuildDefinition.Name + "</td>";
                        responseString += "<td bgcolor='" + bgcolor + "' >" + lastBuild.Status + "</td>";
                        responseString += "</tr>";
                    }
                }
            }
            responseString += "</table>";
            responseString+="</BODY></HTML>";
            byte[] buffer = System.Text.Encoding.UTF8.GetBytes(responseString);
            // Get a response stream and write the response to it.
            response.ContentLength64 = buffer.Length;
            System.IO.Stream output = response.OutputStream;
            output.Write(buffer, 0, buffer.Length);
            // You must close the output stream.
            output.Close();
            // reenable the listener so we will be ready for another request
            listener.myListener.BeginGetContext(new AsyncCallback(ListenerCallback), listener);
        }

    }
}
