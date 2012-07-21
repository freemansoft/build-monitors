/// Written by Joe Freeman joe@freemansoft.com
/// Driver for the Freemometer an arduino controlled analog gauge built in an Ikea alarm clock
///
/// commands are
/// ?: help
/// led red
/// led green
/// led off
/// bell ring (msec)
/// bell silence
/// servo set (0..100)
namespace BuildWatcher
{
    using System;
    using System.Collections.Generic;
    using System.IO.Ports;
    using System.Linq;
    using System.Text;
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
        private int bellTimeInMsec;

        public Freemometer(System.IO.Ports.SerialPort device, int bellTimeInMsec)
        {
            // TODO: Complete member initialization
            this.device = device;
            this.bellTimeInMsec = bellTimeInMsec;
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
                throw new ArgumentOutOfRangeException("device number " + deviceNumber + " is out of range:" + 1);
            }

            int servoPosition;
            if (buildSetSize == 0)
            {
                servoPosition = 0;
            } 
            else 
            {
                servoPosition = 100 * lastBuildsWereSuccessfulCount / buildSetSize;
            }
            log.Debug("servo position set to " + servoPosition);

            this.device.Write("servo set " + servoPosition + "\r");
            if (lastBuildsWereSuccessfulCount == buildSetSize)
            {
                this.device.Write("led green\r");
            }
            else
            {
                this.device.Write("led red\r");
            }

            if (buildSetSize > lastBuildsWerePartiallySuccessfulCount && this.bellTimeInMsec != 0)
            {
                this.device.Write("bell ring " + this.bellTimeInMsec+"\r");
            }
        }
    }
}
