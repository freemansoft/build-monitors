package com.freemansoft.watcher.devices;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.freemansoft.ConfigurationPropertiesKeys;

public class ArduinoEthernetTest {

	String deviceUrl;

	@Before
	public void setUp() throws IOException {
		Properties props = new Properties();
		InputStream resourceStream = this.getClass().getClassLoader()
				.getResourceAsStream("test.properties");
		props.load(resourceStream);
		resourceStream.close();
		deviceUrl = props
				.getProperty(ConfigurationPropertiesKeys.DEVICE_NET_CONNECT_URI);
	}

	@Test
	public void solidTest() {
		ArduinoEthernet device = new ArduinoEthernet(deviceUrl);
		for (int i = 0; i < device.bldGetNumLights(); i++) {
			if (i % 3 == 0) {
				device.bldSolid(i, device.bldGetMaxColor(), 0, 0);
			}
			if (i % 3 == 1) {
				device.bldSolid(i, 0, device.bldGetMaxColor(), 0);
			}
			if (i % 3 == 2) {
				device.bldBlink(i, 0, 0, device.bldGetMaxColor());
			}
		}
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			// we're done
		}
		for (int i = 0; i < device.bldGetNumLights(); i++) {
			device.bldClear(i);
		}
	}
}
