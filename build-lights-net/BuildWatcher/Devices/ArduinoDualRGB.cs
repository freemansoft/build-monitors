/// Written by Joe Freeman joe@freemansoft.com
/// Arduino RGB adapter for Arduino build light firmware used for 2, 4 and 32 RGB lamp build lights.
/// <br></br>
/// Standard commands are
/// color: ~c#[red][green][blue];
/// where red, green and blue have values 0-15 representing brightness
/// blink: ~b#[red on][green on][blue on][red off][green off][blue off];
/// where on and off have values 0-15 representing the number of half seconds.
namespace BuildWatcher.Devices
{
    using System;
    using System.IO.Ports;
    using System.Text;
    using log4net;

    /// <summary>
    /// Arduino driver class
    /// </summary>
    [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "Arduino"), System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1709:IdentifiersShouldBeCasedCorrectly", MessageId = "RGB")]
    public class ArduinoDualRGB : IBuildIndicatorDevice
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(ArduinoDualRGB));

        /// <summary>
        /// command prefix
        /// </summary>
        private static byte standardPrefix = (byte)'~';

        /// <summary>
        /// last character of commands
        /// </summary>
        private static byte standardSuffix = (byte)';';

        /// <summary>
        /// the command to chagne a color
        /// </summary>
        private static byte colorCommand = (byte)'c';

        /// <summary>
        /// the command to change a blink rate
        /// </summary>
        private static byte blinkCommand = (byte)'b';

        /// <summary>
        /// Serial port we communicate with Arduino over
        /// </summary>
        private SerialPort device;

        /// <summary>
        /// the number of lamps in the device
        /// </summary>
        private int numLamps;

        /// <summary>
        /// Initializes a new instance of the <see cref="ArduinoDualRGB"/> class. a proxy for the Arduino controlled dual RGB unit
        /// </summary>
        /// <param name="device">Serial port the device is connected two.  Can be virtual com port for bluetooth</param>
        /// <param name="canReset">determines if the device can be reset through DTR or if is actually reset on connect</param>
        /// <param name="numLamps">the number of lamps in the device, used when turning off all the lights</param>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "num")]
        public ArduinoDualRGB(SerialPort device, bool canReset, int numLamps)
        {
            if (device == null)
            {
                throw new ArgumentNullException("device","Serial Port Device is required");
            }
            this.device = device;

            if (canReset)
            {
                //// can we reset with DTR like this?
                device.DtrEnable = true;
                //// the firmware starts with the string "initialized"

                System.Threading.Thread.Sleep(250);
                byte[] readBuffer = new byte["initialized".Length];
                for (int i = 0; i < readBuffer.Length; i++)
                {
                    readBuffer[i] = (byte)this.device.ReadByte();
                    log.Debug("read " + i);
                }

                log.Debug("Hardware initialized returned string: " + readBuffer);
            }
            else
            {
                string trashInBuffer = device.ReadExisting();
                if (trashInBuffer.Length > 0)
                {
                    log.Debug("Found some cruft left over in the channel " + trashInBuffer);
                }
            }

            this.numLamps = numLamps;
            this.TurnOffLights(numLamps);
            log.Info("created Arduino Device on port "+device.PortName+" with "+numLamps+" lamps");
        }

        /// <summary>
        /// Turns off the number of lamps specified
        /// </summary>
        /// <param name="numLampsToTurnOff">number of lamps to clear</param> 
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "num"), System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1702:CompoundWordsShouldBeCasedCorrectly", MessageId = "TurnOff")]
        public void TurnOffLights(int numLampsToTurnOff)
        {
            for (int deviceNumber = 0; deviceNumber < numLampsToTurnOff; deviceNumber++)
            {
                this.SetColor(deviceNumber, 0, 0, 0);
                this.SetBlink(deviceNumber, 2, 0);
            }
        }

        /// <summary>
        /// sets the color of one of the lamps using RGB
        /// </summary>
        /// <param name="deviceNumber">Number of lights in a device 0-1</param>
        /// <param name="red">value of red 0-15</param>
        /// <param name="green">vlaue of green 0-15</param>
        /// <param name="blue">vlaue of 0-15</param>
        public void SetColor(int deviceNumber, int red, int green, int blue)
        {
            if (deviceNumber >= this.numLamps)
            {
                throw new ArgumentOutOfRangeException("device number " + deviceNumber + " is out of range:" + this.numLamps);
            }

            byte[] buffer = new byte[7];
            buffer[0] = standardPrefix;
            buffer[1] = colorCommand;
            buffer[2] = this.ConvertIntToAsciiChar(deviceNumber);
            buffer[3] = this.ConvertIntToAsciiChar(red);
            buffer[4] = this.ConvertIntToAsciiChar(green);
            buffer[5] = this.ConvertIntToAsciiChar(blue);
            buffer[6] = standardSuffix;
            this.SendAndWaitForAck(buffer);
        }

        /// <summary>
        /// Sets the blink rate of one of the lamps.  All bulbs in a lamp blink at the same rate and time
        /// </summary>
        /// <param name="deviceNumber">lamp number in device 0-1</param>
        /// <param name="onTimeHalfSeconds">blink on time 0-15</param>
        /// <param name="offTimeHalfSeconds">blink off time 0-15</param>
        public void SetBlink(int deviceNumber, int onTimeHalfSeconds, int offTimeHalfSeconds)
        {
            if (deviceNumber >= this.numLamps)
            {
                throw new ArgumentOutOfRangeException("device number " + deviceNumber + " is out of range:" + this.numLamps);
            }

            byte[] buffer = new byte[10];
            buffer[0] = standardPrefix;
            buffer[1] = blinkCommand;
            buffer[2] = this.ConvertIntToAsciiChar(deviceNumber);
            buffer[3] = this.ConvertIntToAsciiChar(onTimeHalfSeconds);
            buffer[4] = this.ConvertIntToAsciiChar(onTimeHalfSeconds);
            buffer[5] = this.ConvertIntToAsciiChar(onTimeHalfSeconds);
            buffer[6] = this.ConvertIntToAsciiChar(offTimeHalfSeconds);
            buffer[7] = this.ConvertIntToAsciiChar(offTimeHalfSeconds);
            buffer[8] = this.ConvertIntToAsciiChar(offTimeHalfSeconds);
            buffer[9] = standardSuffix;
            this.SendAndWaitForAck(buffer);
        }

        /// <summary>
        /// Sends a message and waits on the return ack
        /// </summary>
        /// <param name="buffer">bytes to be sent to arduino</param>
        private void SendAndWaitForAck(byte[] buffer)
        {
            log.Debug("Sending: " + Encoding.UTF8.GetString(buffer, 0, buffer.Length));
            this.device.Write(buffer, 0, buffer.Length);
            System.Threading.Thread.Sleep(20);
            //// should handle timeout with exception catch block
            //// always replies with the command plus a + or - key.  '+' means command understood
            byte[] readBuffer = new byte[buffer.Length + 1];
            for (int i = 0; i < buffer.Length + 1; i++)
            {
                readBuffer[i] = (byte)this.device.ReadByte();
            }

            log.Debug("Received ack: " + Encoding.UTF8.GetString(readBuffer, 0, readBuffer.Length));
        }

        /// <summary>
        ///  sets the indicator in device dependent fashion.
        ///  Ignores any deviceNumber beyond configured numLamps
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        /// <param name="buildSetSize">number of builds in set</param>
        /// <param name="lastBuildsWereSuccessfulCount">number of completely successful builds</param>
        /// <param name="lastBuildsWerePartiallySuccessfulCount">number of partially successful builds</param>
        /// <param name="someoneIsBuildingCount">number of builds in progress</param>
        public void Indicate(int deviceNumber, int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount, int someoneIsBuildingCount)
        {
            log.Debug("lamp:" + deviceNumber
                + " numBuilds:" + buildSetSize
                + " completelySuccessful:" + lastBuildsWereSuccessfulCount
                + " partiallySuccessful:" + lastBuildsWerePartiallySuccessfulCount
                + " currentlyBuilding:" + someoneIsBuildingCount);
            if (deviceNumber >= this.numLamps)
            {
                return;
            }

            if (lastBuildsWereSuccessfulCount == buildSetSize)
            {
                this.SetColor(deviceNumber, 0, 13, 8); // green with blue is good
            }
            else
            {
                if (lastBuildsWerePartiallySuccessfulCount == buildSetSize)
                {
                    this.SetColor(deviceNumber, 12, 9, 0); // pink is partial
                }
                else
                {
                    this.SetColor(deviceNumber, 13, 0, 0); // red is broken
                }
            }

            if (someoneIsBuildingCount > 0)
            {
                this.SetBlink(deviceNumber, 3, 3);
            }
            else
            {
                this.SetBlink(deviceNumber, 3, 0);
            }
        }

        /// <summary>
        ///  Indicates some vcs problem like timeouts, errors. currently only support "problem" without types.
        ///  Ignores any deviceNumber beyond configured numLamps
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        public void IndicateProblem(int deviceNumber)
        {
            if (deviceNumber >= this.numLamps)
            {
                return;
            }
            this.SetColor(deviceNumber, 12, 12, 0);
            this.SetBlink(deviceNumber, 1, 1);
        }


        /// <summary>
        /// Converts a number ot it's hex ascii equivalent
        /// </summary>
        /// <param name="number">input between 0-15 </param>
        /// <returns>ASCII character Hex equivalent of the number </returns>
        private byte ConvertIntToAsciiChar(int number)
        {
            if (number < 0 || number > 15)
            {
                throw new ArgumentException("number out of single digit hex range " + number);
            }

            byte result;
            if (number > 9)
            {
                result = (byte)('A' + number - 10); // we start at 10
            }
            else
            {
                result = (byte)('0' + number);
            }

            return result;
        }
    }
}
