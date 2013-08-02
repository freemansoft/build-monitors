/// Written by Joe Freeman joe@freemansoft.com
///
/// Placeholder for Delcom 2nd generation USB Visual Indicator HID USBHIVID
/// 
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace BuildWatcher.Devices
{
    class DelcomUSBVIHID : IBuildIndicatorDevice
    {
        public void Indicate(int deviceNumber, int buildSetSize, int lastBuildsWereSuccessfulCount, int lastBuildsWerePartiallySuccessfulCount, int someoneIsBuildingCount)
        {
            throw new NotImplementedException();
        }

        public void IndicateProblem(int deviceNumber)
        {
            throw new NotImplementedException();
        }
    }
}
