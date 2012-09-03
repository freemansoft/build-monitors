namespace BuildWatcher
{
    using System;
    using System.IO.Ports;
    using System.Timers; // for timer

    public class SingleShotTimerContainingSerialPort : Timer
    {
        public SerialPort device { get; set; }

        public SingleShotTimerContainingSerialPort(SerialPort device, int timerInterval)
        {
            this.device = device;
            Interval = timerInterval;
            AutoReset = false;
        }
    }
}
