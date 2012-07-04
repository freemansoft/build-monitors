package com.freemansoft;

import static org.junit.Assert.assertNotNull;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Basic tests for the Hudson server class
 * 
 * @author joe
 * @version $Revision: #1 $ $Change: 74965 $ $DateTime: 2008/06/27 11:49:28 $ $Author: joe $
 * @since May 8, 2011
 */
public class HudsonServerTest {

    /** logger */
    private static Logger LOG = Logger.getLogger(HudsonServerTest.class);

    private final String serverUrl = "http://ci.jenkins-ci.org/cc.xml";

    /** update interval in seconds */
    private final int updateInterval = 20;

    @Test
    public void testServer() {
        final HudsonServer server = new HudsonServer(serverUrl, updateInterval);
        server.startPollingServer();
        final HudsonProjectStatus result = server.extractElement("plugins_swarm");
        assertNotNull(result);
        LOG.debug("found project " + result);
        try {
            // seep for at least 1 interval so we can see in logs(!) it cycled and fetched again
            Thread.sleep(updateInterval * 3 * 1000);
        } catch (final InterruptedException e) {
            // just ignore it
        }
        LOG.debug("about to stop polling.");
        server.stopPollingServer();
    }

    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void testFoo() {
        final HudsonServer server = new HudsonServer(serverUrl + "junk", updateInterval);
        server.startPollingServer();
    }

}
