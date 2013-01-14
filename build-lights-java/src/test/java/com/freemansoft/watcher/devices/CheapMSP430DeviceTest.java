package com.freemansoft.watcher.devices;

/*
 * Copyright 2011 FreemanSoft Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

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
 * Remember to set the command line arguments to tell it where the rxtx dll is
 * <p>
 * -Djava.library.path=target/lib
 * 
 * @since Jan 11 2013
 * 
 */
public class CheapMSP430DeviceTest {

	/** logger */
	private static Logger LOG = Logger.getLogger(ArduinoDualRGBTest.class);

	private CheapMSP430Device blinkenLights;
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
			blinkenLights = new CheapMSP430Device(device);
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
		try {
			// test light 0
			Thread.sleep(3000);
			LOG.info("filling with red");
			blinkenLights.bldSolid(0, maxColor, 0, 0);
			Thread.sleep(3000);
			LOG.info("filling with blue");
			blinkenLights.bldSolid(0, 0, 0, maxColor);
			Thread.sleep(3000);
			LOG.info("filling with blinking yellow");
			blinkenLights.bldBlink(0, maxColor, maxColor, 0);
			LOG.info("waiting before exiting");
			Thread.sleep(6000);
			blinkenLights.bldClear(0);
			LOG.info("Exiting test");
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			LOG.error("Unexpected " + e);
		}
	}

}
