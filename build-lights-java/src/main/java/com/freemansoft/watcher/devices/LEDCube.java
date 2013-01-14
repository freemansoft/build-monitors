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

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.freemansoft.IBuildLightDevice;
import com.freemansoft.SerialDevice;

/**
 * Talk to Seeed Studio LED cube running joe's 1.0.7 firmware which supports 3D
 * mapping
 * <p>
 * 
 * @author Joe Freeman
 * @since Jan, 11, 2013
 */
public class LEDCube extends TimerTask implements IBuildLightDevice {

	private static Logger LOG = Logger.getLogger(LEDCube.class);

	private static byte FILL_COMMAND = 2;
	private static byte DRAW_DOT_COMMAND = 5;

	/**
	 * our connection to the hardware
	 */
	private final SerialDevice device;

	/**
	 * Determines if we should be blinking
	 */
	private final boolean blinkEnabled[] = { false, false, false, false };
	/** are the blinking lights on or off */
	private boolean blinkStateIsCurrentlySolid = true;

	/**
	 * used by the bld interface to support blink for four planes rgb values
	 */
	private final RGBTriplet lastFill[] = { new RGBTriplet(0, 0, 0),
			new RGBTriplet(0, 0, 0), new RGBTriplet(0, 0, 0),
			new RGBTriplet(0, 0, 0) };

	/**
	 * constructor
	 * 
	 * @param device
	 *            the serial device that we can use to communicate with this
	 *            cube
	 */
	public LEDCube(final SerialDevice device) {
		if (device == null) {
			throw new IllegalArgumentException("No serial device specified");
		}
		this.device = device;
		device.resetViaDtr();
		// the firmware starts with the string "initialized"
		while (device.charactersAvailable() == 0) {
			try {
				Thread.sleep(250);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
			}
		}

		LOG.debug("Hardware initialized returned string: "
				+ device.getAvailableCharacters());
		final Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(this, 1500, 1500);
	}

	/**
	 * Turn off all pixels. shortcut for fillAll(0,0,0);
	 */
	public void clearAll() {
		fillAll(0, 0, 0);
	}

	/**
	 * set all lights to the same color
	 */

	public void fillAll(final int red, final int green, final int blue) {
		final byte[] buffer = createCommand(FILL_COMMAND);
		buffer[2] = '\0';
		buffer[6] = '\0';
		buffer[3] = (byte) red;
		buffer[4] = (byte) green;
		buffer[5] = (byte) blue;
		device.write(buffer);
	}

	/**
	 * Set a single pixel to the requested color
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param red
	 * @param green
	 * @param blue
	 */
	public void drawDot(final int x, final int y, final int z, final int red,
			final int green, final int blue) {
		int startX = x;
		int endX = x;
		int startY = y;
		int endY = y;
		int startZ = z;
		int endZ = z;
		if (x < 0) {
			startX = 0;
			endX = 3;
		}
		if (y < 0) {
			startY = 0;
			endY = 3;
		}
		if (z < 0) {
			startZ = 0;
			endZ = 3;
		}
		for (int xIndex = startX; xIndex <= endX; xIndex++) {
			for (int yIndex = startY; yIndex <= endY; yIndex++) {
				for (int zIndex = startZ; zIndex <= endZ; zIndex++) {
					final byte[] buffer = createCommand(DRAW_DOT_COMMAND);
					buffer[6] = '0';
					buffer[2] = (byte) ((xIndex << 4) | (yIndex << 2) | zIndex);
					buffer[3] = (byte) red;
					buffer[4] = (byte) green;
					buffer[5] = (byte) blue;
					device.write(buffer);
				}
			}
		}
	}

	private byte[] createCommand(final byte command) {
		final byte[] buffer = new byte[7];
		buffer[0] = 'R';
		buffer[1] = command;
		return buffer;
	}

	/**
	 * fills a horizontal plane with a color -- usefull when using cube as 4
	 * build lights
	 * 
	 * @param deviceNumber
	 * @param red
	 * @param green
	 * @param blue
	 */
	private synchronized void fillHorizontalPlane(final int deviceNumber,
			final int red, final int green, final int blue) {
		for (int x = 0; x <= 3; x++) {
			for (int z = 0; z <= 3; z++) {
				drawDot(x, deviceNumber, z, red, green, blue);
			}
		}
	}

	/*-------------------------------------------------------------------------------
	 *
	 * Build light interface
	 *
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Doesn't blink so just calls fill
	 * <p>
	 * only supports one device (0)
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void bldBlink(final int deviceNumber, final int red,
			final int green, final int blue) {
		// pick it up on the next blink
		lastFill[deviceNumber] = new RGBTriplet(red, green, blue);
		blinkEnabled[deviceNumber] = true;
	}

	/**
	 * only supports one device (0)
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void bldClear(final int deviceNumber) {
		// only support one device
		bldSolid(deviceNumber, 0, 0, 0);
	}

	/**
	 * only supports one device (0)
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void bldSolid(final int deviceNumber, final int red,
			final int green, final int blue) {
		blinkEnabled[deviceNumber] = false;
		fillHorizontalPlane(deviceNumber, red, green, blue);
		lastFill[deviceNumber] = new RGBTriplet(red, green, blue);
	}

	/**
	 * only supports one device (0)
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public int bldGetMaxColor() {
		return 12;
	}

	/**
	 * only supports one device (0)
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public int bldGetNumLights() {
		return 4;
	}

	/**
	 * Simulates embedded blink behavior
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		blinkStateIsCurrentlySolid = !blinkStateIsCurrentlySolid;
		for (int i = 0; i <= 3; i++) {
			if (blinkEnabled[i]) {
				if (blinkStateIsCurrentlySolid) {
					fillHorizontalPlane(i, 0, 0, 0);
				} else {
					fillHorizontalPlane(i, lastFill[i].getRed(),
							lastFill[i].getGreen(), lastFill[i].getBlue());
				}
			}
		}
	}
}
