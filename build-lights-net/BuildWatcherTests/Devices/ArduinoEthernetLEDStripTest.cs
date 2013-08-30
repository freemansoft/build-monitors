using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using log4net;
using log4net.Config;
using BuildWatcher.Devices;

namespace BuildWatcherTests.Devices

{
    // don't forget to make the class public or it won't get picked up!
    [TestClass]
    public class ArduinoEthernetLEDStripTest
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(ArduinoEthernetLEDStripTest));

        [TestInitialize]
        public void SetUpTests()
        {
            BasicConfigurator.Configure();
        }

        [TestMethod]
        public void CreatePostDataSetTest()
        {
            // use the default configuration
            ArduinoEthernetLEDStrip stripController = new ArduinoEthernetLEDStrip(null, 0);
            Dictionary<string, string> postSet = stripController.CreatePostDataSet(3, 1, 1);
            Assert.AreEqual(9,postSet.Count);
            Assert.IsNotNull(postSet["r0"].Equals(ArduinoEthernetLEDStrip.MaxBright));
            Assert.IsNotNull(postSet["g0"].Equals(ArduinoEthernetLEDStrip.NoBright));
            Assert.IsNotNull(postSet["b0"].Equals(ArduinoEthernetLEDStrip.NoBright));

            Assert.IsNotNull(postSet["r1"].Equals(ArduinoEthernetLEDStrip.MixBright));
            Assert.IsNotNull(postSet["g1"].Equals(ArduinoEthernetLEDStrip.MixBright));
            Assert.IsNotNull(postSet["b1"].Equals(ArduinoEthernetLEDStrip.NoBright));

            Assert.IsNotNull(postSet["r2"].Equals(ArduinoEthernetLEDStrip.NoBright));
            Assert.IsNotNull(postSet["g2"].Equals(ArduinoEthernetLEDStrip.MaxBright));
            Assert.IsNotNull(postSet["b2"].Equals(ArduinoEthernetLEDStrip.NoBright));
        }

        [TestMethod]
        public void CreatePostParametersTest()
        {
            // use the default configuration
            ArduinoEthernetLEDStrip stripController = new ArduinoEthernetLEDStrip(null, 0);
            Dictionary<string, string> postSet = stripController.CreatePostDataSet(3, 1, 1);
            Assert.AreEqual(9,postSet.Count);
            string parameters = stripController.CreatePostParameters(postSet);
            log.Info(parameters);
            Assert.AreEqual(( 3 * 3 *3) /*3 leds with R&G&B labels with =*/ + 8/*form &*/ +5/*L0*/ +7/*L1*/ +5/*L2 */,parameters.Length);
        }



    }
}
