/// Written by Joe Freeman joe@freemansoft.com
/// beerware license
/// 
/// TFS watcher driver for the simple TILaunchpad / MSP430 based device described on http://joe.blog.freemansoft.com
/// The device has a single RGB LED and supports blink patterns like the Freemometer
/// commands
/// ?\r - help
/// rgb r g b pattern\r - r:0..255 g:0.255 b:0.255 pattern:0..9
/// 

namespace BuildWatcher
{
    using System;
    using System.Collections.Generic;
    using System.IO.Ports;
    using System.Linq;
    using System.Text;

    /// <summary>
    /// TODO: Update summary.
    /// </summary>
    public class CheapLaunchpadMSP430 : IBuildIndicatorDevice
    {

        private SerialPort device;
        private int signalPatternFailureComplete = 0;
        private int signalPatternFailurePartial = 0;

        public CheapLaunchpadMSP430(SerialPort device, int signalPatternFailureComplete, int signalPatternFailurePartial)
        {
            this.device = device;
            this.signalPatternFailureComplete = signalPatternFailureComplete;
            this.signalPatternFailurePartial = signalPatternFailurePartial;
        }

        public void Indicate(int deviceNumber, int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount, int someoneIsBuildingCount)
        {
            if (deviceNumber > 0)
            {
                throw new ArgumentOutOfRangeException("Only on monitor on this device. Device number " + deviceNumber + " is out of range:" + 1);
            }
            if (lastBuildsWereSuccessfulCount == buildSetSize)
            {
                this.device.Write("rgb 0 250 0 1\r");
            }
            else if (lastBuildsWerePartiallySuccessfulCount == buildSetSize){
                this.device.Write("rgb 250 0 0 "+signalPatternFailurePartial+"\r");
            }
            else
            {
                // sometimes we use a pink here
                this.device.Write("rgb 200 100 0 "+signalPatternFailurePartial+"\r");
            }
        }
    }
}
