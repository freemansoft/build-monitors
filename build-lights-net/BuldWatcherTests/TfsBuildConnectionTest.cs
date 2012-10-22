using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using BuildWatcher;

namespace BuldWatcherTests
{
    [TestClass]
    public class TfsBuildConnectionTest
    {
        [TestMethod]
        [ExpectedException(typeof(System.UriFormatException))]
        public void TestMethod1()
        {
            string tfsPath = "a";
            string userId = "b";
            string password = "c";
            string domain = "d";
            TfsBuildConnection tbc = new TfsBuildConnection(tfsPath, userId, password, domain);
        }

        [TestMethod]
        [ExpectedException(typeof(System.Net.WebException))]
        public void TestMethod2()
        { 
            string tfsPath = "http://foo.bar.bat/collection";
            string userId = "b";
            string password = "c";
            string domain = "d";
            TfsBuildConnection tbc = new TfsBuildConnection(tfsPath, userId, password, domain);
        }

    }
}
