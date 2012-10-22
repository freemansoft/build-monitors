namespace BuildWatcher.Devices
{

    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Text;

    /// <summary>
    /// standard interface for TFS build indicator devices
    /// </summary>
    public interface IBuildIndicatorDevice
    {
        /// <summary>
        ///  Sets the indicator in device dependent fashion.
        ///  Devices should ignore commands for any deviceNumber beyond what they support
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        /// <param name="buildSetSize">number of builds in set</param>
        /// <param name="lastBuildsWereSuccessfulCount">number of completely successful builds</param>
        /// <param name="lastBuildsWerePartiallySuccessfulCount">number of partially successful builds</param>
        /// <param name="someoneIsBuildingCount">number of builds in progress</param>
        void Indicate(int deviceNumber, int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount, int someoneIsBuildingCount);

        /// <summary>
        ///  Indicates some vcs problem like timeouts, errors. currently only support "problem" without types.
        ///  Devices should ignore commands for any deviceNumber beyond what they support
        /// </summary>
        /// <param name="deviceNumber">build number or light number, 0 based</param>
        void IndicateProblem(int deviceNumber);
    }
}
