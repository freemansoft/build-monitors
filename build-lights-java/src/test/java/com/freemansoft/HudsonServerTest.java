package com.freemansoft;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Basic tests for the Hudson server class
 * 
 * @author joe
 * @version $Revision: #1 $ $Change: 74965 $ $DateTime: 2008/06/27 11:49:28 $
 *          $Author: joe $
 * @since May 8, 2011
 */
public class HudsonServerTest {

	/** logger */
	private static Logger LOG = Logger.getLogger(HudsonServerTest.class);

	/* a public server (moved from http to https in 2011 or 2012) */
	private String serverUrl = null;
	/* this needs to be the name of an active project on the server */
	private String projectName = null;

	/** update interval in seconds */
	private final int updateInterval = 5;

	@Before
	public void setUp() throws IOException {
		Properties props = new Properties();
		InputStream resourceStream = this.getClass().getClassLoader()
				.getResourceAsStream("test.properties");
		props.load(resourceStream);
		resourceStream.close();
		serverUrl = props
				.getProperty(ConfigurationPropertiesKeys.BUILD_SERVER_URI_KEY);
		// CHEAT! assume only one projectName in the test file so we don't have
		// to parse
		projectName = props
				.getProperty(ConfigurationPropertiesKeys.BUILD_SERVER_PROJECTS_KEY);
	}

	@Test
	public void testServer() {
		final HudsonServer server = new HudsonServer(serverUrl, updateInterval);
		server.startPollingServer();
		final HudsonProjectStatus result = server.extractElement(projectName);
		assertNotNull(result);
		LOG.info("found project " + result);
		try {
			// keep for at least 1 interval so we can see in logs(!) it cycled
			// and fetched again
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
		final HudsonServer server = new HudsonServer(serverUrl + "junk",
				updateInterval);
		server.startPollingServer();
	}

}
