///
/// Written by Joe Freeman joe@freemansoft.com
/// Arduino RGB adapter for Arduino build light firmware used for 2, 4 and 32 RGB lamp build lights.
/// 
/// Standard commands are
/// color: ~c#[red][green][blue];
/// where red, green and blue have values 0-15 representing brightness
/// blink: ~b#[red on][green on][blue on][red off][green off][blue off];
/// where on and off have values 0-15 representing the number of half seconds.
namespace BuildWatcher
{
    using System;
    using System.IO.Ports;
    using System.Text;
    using log4net;

    public class ArduinoDualRGB
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
        /// Initializes a new instance of the <see cref="ArduinoDualRGB"/> class. a proxy for the Arduino controlled dual RGB unit
        /// </summary>
        /// <param name="device">Serial port the device is connected two.  Can be virtual com port for bluetooth</param>
        /// <param name="canReset">determines if the device can be reset through DTR or if is actually reset on connect</param>
        public ArduinoDualRGB(SerialPort device, bool canReset, int numLamps)
        {
            if (device == null)
            {
                throw new ArgumentNullException("device", "Device is required");
            }
            else
            {
                this.device = device;
            }

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

            TurnOffLights(numLamps);
        }

        /// <summary>
        /// Turns off the number of lamps specified
        /// </summary>
        /// <param name="numLamps">number of lamps to clear</param> 
        public void TurnOffLights(int numLamps)
        {
            for (int deviceNumber = 0; deviceNumber < numLamps; deviceNumber++)
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
        /// Converts a number ot it's hex ascii equivalent
        /// </summary>
        /// <param name="number">input between 0-15 </param>
        /// <returns>ASCII character Hex equivalent of the number </returns>
        public byte ConvertIntToAsciiChar(int number)
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
    }
}
