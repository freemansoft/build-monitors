package com.freemansoft.watcher.devices;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.freemansoft.BuildMonitorDriver;
import com.freemansoft.ConfigurationPropertiesKeys;
import com.freemansoft.SerialDevice;

/**
 * test class
 * <p>
 * Run this unit test withwith
 * 
 * <pre>
 * -Djava.library.path=<path to dll>
 * </pre>
 * 
 * Unit tests run from the root of the project so the dll can be found here
 * 
 * <pre>
 * -Djava.library.path=target/lib
 * </pre>
 * 
 * @author joe
 * @version $Revision: #1 $ $Change: 74965 $ $DateTime: 2008/06/27 11:49:28 $
 *          $Author: joe $
 * @since May 8, 2011
 */
public class ArduinoDualRGBTest {

	/** logger */
	private static Logger LOG = Logger.getLogger(ArduinoDualRGBTest.class);

	private ArduinoDualRGB blinkenLights;
	private SerialDevice device;

	@Before
	public void setUp() throws IOException {
		// should get from properties
		Properties props = new Properties();
		InputStream resourceStream = this.getClass().getClassLoader()
				.getResourceAsStream("test.properties");
		props.load(resourceStream);
		resourceStream.close();

		device = BuildMonitorDriver.createSerialIfConfigured(props);

		if (device != null) {
			blinkenLights = new ArduinoDualRGB(device);
		} else {
			LOG.error("port "
					+ props.getProperty(ConfigurationPropertiesKeys.DEVICE_SERIAL_PORT_KEY)
					+ " not found.");
		}
	}

	/**
	 * close port after every test
	 */
	@After
	public void tearDown() {
		blinkenLights = null;
		device.close();
	}

	@Test
	public void testLights() {
		final int maxColor = blinkenLights.bldGetMaxColor();
		LOG.info("initial clear");
		blinkenLights.bldClear(0);
		blinkenLights.bldClear(1);
		try {
			// test light 0
			Thread.sleep(3000);
			LOG.info("filling with red");
			blinkenLights.bldSolid(0, maxColor, 0, 0);
			blinkenLights.bldSolid(1, maxColor, 0, 0);
			Thread.sleep(3000);
			LOG.info("filling with blue");
			blinkenLights.bldSolid(0, 0, 0, maxColor);
			blinkenLights.bldSolid(1, 0, 0, maxColor);
			Thread.sleep(3000);
			LOG.info("filling with blinking yellow");
			blinkenLights.bldBlink(0, maxColor, maxColor, 0);
			blinkenLights.bldBlink(1, maxColor, maxColor, 0);
			LOG.info("waiting before exiting");
			Thread.sleep(5000);
			blinkenLights.bldClear(0);
			LOG.info("Exiting test");
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			LOG.error("Unexpected " + e);
		}
	}

	@Test
	public void testConversion() {
		final ArduinoDualRGB device = new ArduinoDualRGB(null);
		assertEquals('0', device.convertIntToAsciiChar(0));
		assertEquals('9', device.convertIntToAsciiChar(9));
		assertEquals('A', device.convertIntToAsciiChar(10));
		assertEquals('F', device.convertIntToAsciiChar(15));
	}
}
