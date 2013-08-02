/// Written by Joe Freeman joe@freemansoft.com
/// beerware license
/// 
/// TFS watcher driver for a Adafruit NeoPixel shield with WS2812 LEds with 2811 embedded controllers
/// We use the whole shield to show status of as many builds as possible in set
/// ?\r - help
/// rgb index r g b pattern\r --> index:0..numPix r:0..255 g:0.255 b:0.255 pattern:0..9
/// 

namespace BuildWatcher.Devices
{
    using System;
    using System.Collections.Generic;
    using System.IO.Ports;
    using System.Linq;
    using System.Text;
    //// a log4net dependency  caused me to have to 
    //// change the VS2010 target from .Net Framework 4 Client Profile to .Net Framework 4
    using log4net;
    using log4net.Config;

    /// <summary>
    /// TODO: Update summary.
    /// </summary>
    [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1709:IdentifiersShouldBeCasedCorrectly", MessageId = "MSP")]
    public class ArduinoNeoPixel : IBuildIndicatorDevice
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(ArduinoNeoPixel));

        private SerialPort device;
        private int signalPatternFailureComplete = 0;
        private int signalPatternFailurePartial = 0;
        private int numLamps = 0;

        public ArduinoNeoPixel(SerialPort device, int signalPatternFailureComplete, int signalPatternFailurePartial, int numLamps)
        {
            if (device == null)
            {
                throw new ArgumentNullException("device", "Serial Port Device is required");
            }
            this.device = device;
            this.signalPatternFailureComplete = signalPatternFailureComplete;
            this.signalPatternFailurePartial = signalPatternFailurePartial;
            this.numLamps = numLamps;
            log.Info("Created ArduinoNeoPixel on port " + device.PortName);
        }

        /// <summary>
        ///  sets the indicator in device dependent fashion.
        ///  Ignores any deviceNumber beyond 0
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        /// <param name="buildSetSize">number of builds in set</param>
        /// <param name="lastBuildsWereSuccessfulCount">number of completely successful builds</param>
        /// <param name="lastBuildsWerePartiallySuccessfulCount">number of partially successful builds</param>
        /// <param name="someoneIsBuildingCount">number of builds in progress</param>
        public void Indicate(int deviceNumber, int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount, int someoneIsBuildingCount)
        {
            this.device.Write("blank\r");
            // ignore the device number because we aren't doing one build-set per pixel but one build per pixel as we iterate across sets
            for (int buildIndex = numLamps - 1; buildIndex >= 0; buildIndex--)
            {
                if (buildIndex < buildSetSize - lastBuildsWereSuccessfulCount - lastBuildsWerePartiallySuccessfulCount)
                {
                    this.device.Write("rgb " + buildIndex + " 10 0 0 " + signalPatternFailureComplete + "\r");
                }
                else if (buildIndex < buildSetSize - lastBuildsWereSuccessfulCount)
                {
                    this.device.Write("rgb " + buildIndex + " 10 10 0 " + signalPatternFailurePartial + "\r");
                }
                else if (buildIndex < buildSetSize)
                {
                    this.device.Write("rgb " + buildIndex + " 0 10 0 1\r");
                }
                else if (buildIndex < numLamps)
                {
                    this.device.Write("rgb " + buildIndex + " 0 0 0 0\r");
                }
            }
        }

        /// <summary>
        ///  Indicates some vcs problem like timeouts, errors. currently only support "problem" without types.
        ///  Ignores any deviceNumber beyond 0
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        public void IndicateProblem(int deviceNumber)
        {
            this.device.Write("rgb -1 10 10 0 " + "9" + "\r");
        }


    }
}
