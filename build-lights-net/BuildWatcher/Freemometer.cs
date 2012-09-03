/// Written by Joe Freeman joe@freemansoft.com
/// Driver for the Freemometer an arduino controlled analog gauge built in an Ikea alarm clock
///
/// commands are
/// ?: help
/// led red (0..9)
/// led green (0..9)
/// led off
/// bell ring (0..9)
/// bell off
/// servo set (0..100)
namespace BuildWatcher
{
    using System;
    using System.Collections.Generic;
    using System.IO.Ports;
    using System.Linq;
    using System.Text;
    using System.Timers; // for timer
    using log4net;

    /// <summary>
    /// The cover class for the ikea dekad clock indicator hack
    /// </summary>
    class Freemometer : IBuildIndicatorDevice
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(ArduinoDualRGB));

        private SerialPort device;
        private int bellPatternFailureComplete = 0;
        private int bellPatternFailurePartial = 0;
        private int bellRingTime = 0;

        /// <summary>
        /// Constructor sets the serial port and the ringer patterns to use
        /// </summary>
        /// <param name="device">serial port</param>
        /// <param name="bellPatternFailureComplete">pattern to use if any builds in set completely fail</param>
        /// <param name="bellPatternFailurePartial">pattern to use if some builds in set partially fail.</param>
        /// <param name="bellRingTime">how many msec to let bell ring after failure detected.  Will restart every polling interval</param>
        public Freemometer(System.IO.Ports.SerialPort device, int bellPatternFailureComplete, int bellPatternFailurePartial, int bellRingTime)
        {
            // TODO: Complete member initialization
            this.device = device;
            this.bellPatternFailureComplete = bellPatternFailureComplete;
            this.bellPatternFailurePartial = bellPatternFailurePartial;
            this.bellRingTime = bellRingTime;
        }

        /// <summary>
        ///  sets the indicator in device dependent fashion
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
                throw new ArgumentOutOfRangeException("Only on monitor on this device. Device number " + deviceNumber + " is out of range:" + 1);
            }

            int servoPosition;
            if (buildSetSize == 0)
            {
                servoPosition = 0;
            } 
            else 
            {
                // full range is "in the red" on the freemometer
                servoPosition = 100- (100 * lastBuildsWereSuccessfulCount / buildSetSize);
            }
            log.Debug("servo shows danger rating '100 - percentage builds successfull' ("+lastBuildsWereSuccessfulCount+"/"+buildSetSize+". Position set to " + servoPosition);

            this.device.Write("servo set " + servoPosition + "\r");
            if (lastBuildsWereSuccessfulCount == buildSetSize)
            {
                this.device.Write("led green 1\r");
            }
            else
            {
                this.device.Write("led red 1\r");
            }

            if ((lastBuildsWerePartiallySuccessfulCount > lastBuildsWereSuccessfulCount) && this.bellPatternFailureComplete > 0)
            {
                // number successful < number partially successful means some only partially succeeded
                this.device.Write("bell ring " + this.bellPatternFailureComplete + "\r");
                FireUpBellDisabler();
            }
            else 
            if ((buildSetSize > lastBuildsWerePartiallySuccessfulCount) && this.bellPatternFailurePartial > 0)
            {
                // number built greater than partial success rate means some partially failed
                this.device.Write("bell ring " + this.bellPatternFailurePartial + "\r");
                FireUpBellDisabler();
            }
            else
            {
                this.device.Write("bell ring " + 0 + "\r");
            }
        }

        /// <summary>
        /// creates timed event that will turn off the bell after amount of time configured via constructor
        /// </summary>
        private void FireUpBellDisabler()
        {
            SingleShotTimerContainingSerialPort thatWhichWillturnOffBell = new SingleShotTimerContainingSerialPort(this.device,this.bellRingTime);
            thatWhichWillturnOffBell.Elapsed += new ElapsedEventHandler(TurnOffRinger);
            thatWhichWillturnOffBell.Enabled = true;
        }

        /// <summary>
        /// timer call back to turn off the serial port
        /// </summary>
        /// <param name="serialPort">SerialPort to communicate over</param>
        private static void TurnOffRinger(Object source,ElapsedEventArgs e)
        {
            SingleShotTimerContainingSerialPort actualSource = (SingleShotTimerContainingSerialPort)source;
            actualSource.device.Write("bell ring 0\r");
        }

    }

}
