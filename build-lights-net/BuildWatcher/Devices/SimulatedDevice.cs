using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using log4net;

namespace BuildWatcher.Devices
{
    class SimulatedDevice : IBuildIndicatorDevice
    {
        /// <summary>
        /// log4net logger
        /// </summary>
        private static ILog log = log4net.LogManager.GetLogger(typeof(SimulatedDevice));

        /// <summary>
        ///  Sets the indicator in device dependent fashion.
        ///  Behaves differently from physical devices in that it works for any number of lamps
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        /// <param name="buildSetSize">number of builds in set</param>
        /// <param name="lastBuildsWereSuccessfulCount">number of completely successful builds</param>
        /// <param name="lastBuildsWerePartiallySuccessfulCount">number of partially successful builds</param>
        /// <param name="someoneIsBuildingCount">number of builds in progress</param>
        public void Indicate(int deviceNumber, int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount, int someoneIsBuildingCount)
        {
            log.Info("Indicate: "+deviceNumber+", build set size:"+buildSetSize+", complete successs count:"+lastBuildsWereSuccessfulCount+
                ", at least partial success count:"+lastBuildsWerePartiallySuccessfulCount);
        }

        /// <summary>
        ///  Indicates some vcs problem like timeouts, errors. currently only support "problem" without types.
        ///  Behaves differently from physical devices in that it works for any number of lamps
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        public void IndicateProblem(int deviceNumber)
        {
            log.Info("IndicateProblem for device " + deviceNumber);
        }
    }
}
