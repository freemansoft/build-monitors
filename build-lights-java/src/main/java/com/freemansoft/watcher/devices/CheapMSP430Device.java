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

import org.apache.log4j.Logger;

import com.freemansoft.IBuildLightDevice;
import com.freemansoft.SerialDevice;

/**
 * TI Launchpad style MSP430 simple board with one RGB LED. The simple $10 build
 * light.
 * <p>
 * Plans are available on http://joe.blog.freemansoft.com
 * 
 * @since Jan 11 2013
 * 
 */
public class CheapMSP430Device implements IBuildLightDevice {

	private static Logger LOG = Logger.getLogger(ArduinoDualRGB.class);

	/**
	 * our connection to the hardware
	 */
	private final SerialDevice device;

	/**
	 * constructor
	 * 
	 * @param device
	 *            the serial device that we can use to communicate with this
	 *            cube
	 */
	public CheapMSP430Device(final SerialDevice device) {
		if (device == null) {
			throw new IllegalArgumentException("No serial device specified");
		}
		this.device = device;
		LOG.debug("Using device " + device);
		bldClear(0);
	}

	@Override
	public int bldGetNumLights() {
		return 1;
	}

	@Override
	public int bldGetMaxColor() {
		return 255;
	}

	@Override
	public void bldClear(int deviceNumber) {
		if (deviceNumber >= bldGetNumLights()) {
			throw new IllegalArgumentException("Requested device "
					+ deviceNumber + " but this only supports "
					+ bldGetNumLights() + " lights");
		}
		device.write("rgb 0 0 0 0\r");
	}

	@Override
	public void bldSolid(int deviceNumber, int red, int green, int blue) {
		if (deviceNumber >= bldGetNumLights()) {
			throw new IllegalArgumentException("Requested device "
					+ deviceNumber + " but this only supports "
					+ bldGetNumLights() + " lights");
		}
		// continuous
		device.write("rgb " + red + " " + green + " " + blue + " 1" + "\r");
	}

	@Override
	public void bldBlink(int deviceNumber, int red, int green, int blue) {
		if (deviceNumber >= bldGetNumLights()) {
			throw new IllegalArgumentException("Requested device "
					+ deviceNumber + " but this only supports "
					+ bldGetNumLights() + " lights");
		}
		// blink pattern should be selectable instead of hard coded
		device.write("rgb " + red + " " + green + " " + blue + " 2" + "\r");
	}

}
