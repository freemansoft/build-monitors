// -----------------------------------------------------------------------
// <copyright file="ArduinoEthernetLEDStrip.cs" company="">
// TODO: Update copyright text.
// </copyright>
// -----------------------------------------------------------------------
// This is for an arduino controlled LED light strip where the arduino 
// has an ethernet port and built in web server

namespace BuildWatcher.Devices
{
    using System;
    using System.Collections.Generic;
    using System.Net;
    using System.Text;

    using log4net;
    using BuildWatcher.Tfs;
    using System.IO;
    //// HttpUtility only shows up if you have add a reference in the project
    ////project-->AddReference-->FrameWork Select System.Web
    using System.Web;


    /// <summary>
    /// Note that this device behaves differently. It uses the long strand to show the status of each individul build 
    /// in a set ignores the lamp. This means the strip cycles through the various build sets
    /// </summary>
    [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1709:IdentifiersShouldBeCasedCorrectly", MessageId = "LED"), System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "Arduino")]
    public class ArduinoEthernetLEDStrip : IBuildIndicatorDevice
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(ArduinoEthernetLEDStrip));
        /// <summary>
        /// arduino URL
        /// </summary>
        private readonly string uri = "http://arduino_led_strip.local/";
        /// <summary>
        /// Number of LEDs in our strand
        /// </summary>
        private readonly int numberOfLamps = 30;

        /// <summary>
        /// constructor that tells us where and how big our strip is
        /// </summary>
        /// <param name="url">arduino ethernet device url ignores value if null</param>
        /// <param name="numberOfLamps">number of LEDs in the strand ignores value if <= 0</param>
        public ArduinoEthernetLEDStrip(String uri, int numberOfLamps)
        {
            if (numberOfLamps > 0)
            {
                this.numberOfLamps = numberOfLamps;
            }
            if (uri != null)
            {
                this.uri = uri;
            }
        }

        /// <summary>
        /// Light up the LEDs based, one build to each LED. See createPostDataSet() to see 
        /// this handles situations where the number of LEDs and number of builds are not the same
        /// as will be the case most of the time.
        /// </summary>
        /// <param name="deviceNumber">ignored</param>
        /// <param name="buildSetSize">number of builds in this build set</param>
        /// <param name="lastBuildsWereSuccessfulCount">number of successful builds out of this build set</param>
        /// <param name="lastBuildsWerePartiallySuccessfulCount">number of partially successful builds out of this build set</param>
        /// <param name="someoneIsBuildingCount"></param>
        public void Indicate(int deviceNumber, int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount, int someoneIsBuildingCount)
        {
            Dictionary<String, String> postSet = CreatePostDataSet(buildSetSize, lastBuildsWereSuccessfulCount, lastBuildsWerePartiallySuccessfulCount);
            string postParameters = CreatePostParameters(postSet);
            DoPost(postParameters);
        }

        /// <summary>
        /// Just set all the lamps to red if we have a problem
        /// </summary>
        /// <param name="deviceNumber"></param>
        public void IndicateProblem(int deviceNumber)
        {
            this.Indicate(deviceNumber, numberOfLamps, 0, 0, 0);
        }

        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Performance", "CA1822:MarkMembersAsStatic")]
        internal string CreatePostParameters(Dictionary<string, string> postSet)
        {
            log.Debug("Posting " + postSet.Count + " values");
            StringBuilder parameters = new StringBuilder();
            bool isFirst = true;
            foreach (KeyValuePair<string, string> oneField in postSet)
            {
                if (!isFirst)
                {
                    parameters.Append("&");
                }
                else
                {
                    isFirst = false;
                }
                parameters.Append(oneField.Key);
                parameters.Append("=");
                parameters.Append(HttpUtility.UrlEncode(oneField.Value));
            }
            return parameters.ToString();
        }

        /// <summary>
        /// Single color brightness
        /// </summary>
        public const string MaxBright = "200";
        /// <summary>
        /// Brightness when more then one LED is used
        /// </summary>
        public const string MixBright = "150";
        /// <summary>
        /// go dark
        /// </summary>
        public const string NoBright = "0";

        /// <summary>
        /// Create a set of light post parameters in a dictionary that set all the lamps based on the number
        /// of builds in each status type
        /// </summary>
        /// <param name="buildSetSize"></param>
        /// <param name="lastBuildsWereSuccessfulCount"></param>
        /// <param name="lastBuildsWerePartiallySuccessfulCount"></param>
        /// <returns></returns>
        internal Dictionary<String, String> CreatePostDataSet(int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount)
        {
            Dictionary<String, String> postSet = new Dictionary<string, string>();
            int numberFailed = buildSetSize - lastBuildsWereSuccessfulCount - lastBuildsWerePartiallySuccessfulCount;
            postSet.Add("s0", "Builds:  " + buildSetSize);
            postSet.Add("s1", "Success: " + lastBuildsWereSuccessfulCount);
            postSet.Add("s2", "Partial: " + lastBuildsWerePartiallySuccessfulCount);
            postSet.Add("s3", "Failed:  " + numberFailed);
            postSet.Add("s4", "            ");
            postSet.Add("s5", "            ");
            int lamp = 0;
            while (lamp < numberOfLamps && lamp < buildSetSize)
            {
                if (lamp < numberFailed)
                {
                    postSet.Add("r" + lamp, MaxBright);
                    postSet.Add("g" + lamp, NoBright);
                    postSet.Add("b" + lamp, NoBright);
                }
                else if (lamp < numberFailed + lastBuildsWerePartiallySuccessfulCount)
                {
                    postSet.Add("r" + lamp, MixBright);
                    postSet.Add("g" + lamp, MixBright);
                    postSet.Add("b" + lamp, NoBright);
                }
                else if (lamp < buildSetSize)
                {
                    postSet.Add("r" + lamp, NoBright);
                    postSet.Add("g" + lamp, MaxBright);
                    postSet.Add("b" + lamp, NoBright);
                }
                else
                {
                    postSet.Add("r" + lamp, NoBright);
                    postSet.Add("g" + lamp, NoBright);
                    postSet.Add("b" + lamp, NoBright);
                }
                lamp++;
            }
            return postSet;
        }

        /// <summary>
        /// post to our configured url, assume the post parameter string is the form post data already formatted
        /// </summary>
        /// <param name="postParameters">post parameters in key=value&key=value&key=value format</param>
        internal void DoPost(string postParameters)
        {
            UTF8Encoding ourEncoder = new UTF8Encoding();

            Byte[] byteArray = ourEncoder.GetBytes(postParameters);
            //// WebRequest is an abstract class.  Create will create the right type for the URL
            WebRequest request = WebRequest.Create(this.uri);
            request.Method = "POST";
            request.ContentType = "application/x-www-form-urlencoded";
            request.ContentLength = byteArray.Length;
            Stream dataStream = request.GetRequestStream();
            dataStream.Write(byteArray, 0, byteArray.Length);
            dataStream.Close();
            WebResponse response = request.GetResponse();
            // response.dosomething
            response.Close();

        }

    }
}
