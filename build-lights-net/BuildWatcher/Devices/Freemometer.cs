/// Written by Joe Freeman joe@freemansoft.com
/// beerware license
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
/// 
///  is this still right with the latest firmware?
namespace BuildWatcher.Devices
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
    [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Performance", "CA1812:AvoidUninstantiatedInternalClasses")]
    class Freemometer : IBuildIndicatorDevice
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(Freemometer));

        private SerialPort device;
        private int signalPatternFailureComplete = 0;
        private int signalPatternFailurePartial = 0;
        private int bellRingTime = 0;

        /// <summary>
        /// Constructor sets the serial port and the ringer patterns to use
        /// </summary>
        /// <param name="device">serial port</param>
        /// <param name="signalPatternFailureComplete">pattern to use if any builds in set completely fail</param>
        /// <param name="signalPatternFailurePartial">pattern to use if some builds in set partially fail.</param>
        /// <param name="bellRingTime">how many msec to let bell ring after failure detected.  Will restart every polling interval</param>
        public Freemometer(System.IO.Ports.SerialPort device, int signalPatternFailureComplete, int signalPatternFailurePartial, int bellRingTime)
        {
            if (device == null)
            {
                throw new ArgumentNullException("device", "Serial Port Device is required");
            }
            // TODO: Complete member initialization
            this.device = device;
            this.signalPatternFailureComplete = signalPatternFailureComplete;
            this.signalPatternFailurePartial = signalPatternFailurePartial;
            this.bellRingTime = bellRingTime;
            log.Info("Created Freemometer on port " + device.PortName);
        }

        /// <summary>
        ///  sets the indicator in device dependent fashion
        ///  Ignores any deviceNumber beyond 0
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        /// <param name="buildSetSize">number of builds in set</param>
        /// <param name="lastBuildsWereSuccessfulCount">number of completely successful builds</param>
        /// <param name="lastBuildsWerePartiallySuccessfulCount">number of partially successful builds</param>
        /// <param name="someoneIsBuildingCount">number of builds in progress</param>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Globalization", "CA1303:Do not pass literals as localized parameters", MessageId = "System.IO.Ports.SerialPort.Write(System.String)")]
        public void Indicate(int deviceNumber, int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount, int someoneIsBuildingCount)
        {
            if (deviceNumber > 0)
            {
                return;
            }

            int servoPosition;
            if (buildSetSize == 0)
            {
                servoPosition = 0;
            }
            else
            {
                // full range is "in the red" on the freemometer
                servoPosition = 100 - (100 * lastBuildsWereSuccessfulCount / buildSetSize);
            }
            log.Debug("servo shows danger rating '100 - percentage builds successfull' (" + lastBuildsWereSuccessfulCount + "/" + buildSetSize + ". Position set to " + servoPosition);

            this.device.Write("servo set " + servoPosition + "\r");
            if (lastBuildsWereSuccessfulCount == buildSetSize)
            {
                this.device.Write("led green 1\r");
            }
            else
            {
                this.device.Write("led red 1\r");
            }

            // failures are more bad that partial failures
            if ((buildSetSize > lastBuildsWereSuccessfulCount) && this.signalPatternFailureComplete > 0)
            {
                this.device.Write("bell ring " + this.signalPatternFailureComplete + "\r");
                FireUpBellDisabler();
            }
            else
                if ((lastBuildsWerePartiallySuccessfulCount > 0) && this.signalPatternFailurePartial > 0)
                {
                    this.device.Write("bell ring " + this.signalPatternFailurePartial + "\r");
                    FireUpBellDisabler();
                }
                else
                {
                    this.device.Write("bell ring " + 0 + "\r");
                }
        }

        /// <summary>
        ///  Indicates some vcs problem like timeouts, errors. currently only support "problem" without types.
        ///  Ignores any deviceNumber beyond 0
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Globalization", "CA1303:Do not pass literals as localized parameters", MessageId = "System.IO.Ports.SerialPort.Write(System.String)")]
        public void IndicateProblem(int deviceNumber)
        {
            if (deviceNumber > 0)
            {
                return;
            }
            this.device.Write("bell ring " + 9 + "\r");
            this.device.Write("led red 1\r");
        }

        /// <summary>
        /// creates timed event that will turn off the bell after amount of time configured via constructor
        /// </summary>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Reliability", "CA2000:Dispose objects before losing scope", Justification="should get cleaned up after event fired")]
        private void FireUpBellDisabler()
        {
            SingleShotTimerContainingSerialPort thatWhichWillturnOffBell = new SingleShotTimerContainingSerialPort(this.device, this.bellRingTime);
            thatWhichWillturnOffBell.Elapsed += new ElapsedEventHandler(TurnOffRinger);
            thatWhichWillturnOffBell.Enabled = true;
        }

        /// <summary>
        /// timer call back to turn off the serial port
        /// </summary>
        /// <param name="serialPort">SerialPort to communicate over</param>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Globalization", "CA1303:Do not pass literals as localized parameters", MessageId = "System.IO.Ports.SerialPort.Write(System.String)")]
        private static void TurnOffRinger(Object source, ElapsedEventArgs e)
        {
            SingleShotTimerContainingSerialPort actualSource = (SingleShotTimerContainingSerialPort)source;
            actualSource.Device.Write("bell ring 0\r");
        }

    }

}
