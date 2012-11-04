/// Written by Joe Freeman joe@freemansoft.com
/// beerware license
/// 
/// TFS watcher driver for the simple TILaunchpad / MSP430 based device described on http://joe.blog.freemansoft.com
/// The device has a single RGB LED and supports blink patterns like the Freemometer
/// commands
/// ?\r - help
/// rgb r g b pattern\r - r:0..255 g:0.255 b:0.255 pattern:0..9
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
    public class CheapLaunchpadMSP430 : IBuildIndicatorDevice
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(ArduinoDualRGB));

        private SerialPort device;
        private int signalPatternFailureComplete = 0;
        private int signalPatternFailurePartial = 0;

        public CheapLaunchpadMSP430(SerialPort device, int signalPatternFailureComplete, int signalPatternFailurePartial)
        {
            if (device == null)
            {
                throw new ArgumentNullException("device", "Serial Port Device is required");
            }
            this.device = device;
            this.signalPatternFailureComplete = signalPatternFailureComplete;
            this.signalPatternFailurePartial = signalPatternFailurePartial;
            log.Info("Created CheapLaunchpadMSP430 on port "+device.PortName);
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
            if (deviceNumber > 0)
            {
                return;
            }
            if (lastBuildsWereSuccessfulCount == buildSetSize)
            {
                this.device.Write("rgb 0 250 0 1\r");
            }
            else if (lastBuildsWerePartiallySuccessfulCount > 0)
            {
                // sometimes we use a pink here
                this.device.Write("rgb 200 100 0 " + signalPatternFailurePartial + "\r");
            }
            else
            {
                this.device.Write("rgb 250 0 0 " + signalPatternFailureComplete + "\r");
            }
        }

        /// <summary>
        ///  Indicates some vcs problem like timeouts, errors. currently only support "problem" without types.
        ///  Ignores any deviceNumber beyond 0
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        public void IndicateProblem(int deviceNumber)
        {
            if (deviceNumber > 0)
            {
                return;
            }
            this.device.Write("rgb 128 128 0 " + "9" + "\r");
        }


    }
}
