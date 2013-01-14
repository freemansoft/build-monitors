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
 * Arduino controlled pair of Sparkfun RGB LED breakout boards. Arduino running
 * custom firmware with blink support
 * 
 * @since Jan 11 2013
 */
public class ArduinoDualRGB implements IBuildLightDevice {

	private static final byte STANDARD_PREFIX = '~';
	private static final byte STANDARD_SUFFIX = ';';
	private static final byte COLOR_COMMAND = 'c';
	private static final byte BLINK_COMMAND = 'b';

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
	public ArduinoDualRGB(final SerialDevice device) {
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
	}

	private void setColor(final int deviceNumber, final int red,
			final int green, final int blue) {
		final byte buffer[] = new byte[7];
		buffer[0] = STANDARD_PREFIX;
		buffer[1] = COLOR_COMMAND;
		buffer[2] = convertIntToAsciiChar(deviceNumber);
		buffer[3] = convertIntToAsciiChar(red);
		buffer[4] = convertIntToAsciiChar(green);
		buffer[5] = convertIntToAsciiChar(blue);
		buffer[6] = STANDARD_SUFFIX;
		sendAndWaitForAck(buffer);
	}

	private void setBlink(final int deviceNumber, final int onTimeHalfSeconds,
			final int offTimeHalfSeconds) {
		final byte buffer[] = new byte[10];
		buffer[0] = STANDARD_PREFIX;
		buffer[1] = BLINK_COMMAND;
		buffer[2] = convertIntToAsciiChar(deviceNumber);
		buffer[3] = convertIntToAsciiChar(onTimeHalfSeconds);
		buffer[4] = convertIntToAsciiChar(onTimeHalfSeconds);
		buffer[5] = convertIntToAsciiChar(onTimeHalfSeconds);
		buffer[6] = convertIntToAsciiChar(offTimeHalfSeconds);
		buffer[7] = convertIntToAsciiChar(offTimeHalfSeconds);
		buffer[8] = convertIntToAsciiChar(offTimeHalfSeconds);
		buffer[9] = STANDARD_SUFFIX;
		sendAndWaitForAck(buffer);
	}

	/*------------------------------------------------------------
	 * 
	 * Build light interface
	 * 
	 * ------------------------------------------------------------
	 */
	private static final int ON_TIME_HALF_SECONDS = 3;
	private static final int OFF_TIME_HALF_SECONDS = 3;;

	@Override
	public synchronized void bldBlink(final int deviceNumber, final int red,
			final int green, final int blue) {
		setColor(deviceNumber, red, green, blue);
		setBlink(deviceNumber, ON_TIME_HALF_SECONDS, OFF_TIME_HALF_SECONDS);
	}

	@Override
	public synchronized void bldClear(final int deviceNumber) {
		bldSolid(deviceNumber, 0, 0, 0);
	}

	@Override
	public int bldGetMaxColor() {
		return 15;
	}

	@Override
	public int bldGetNumLights() {
		return 2;
	}

	@Override
	public synchronized void bldSolid(final int deviceNumber, final int red,
			final int green, final int blue) {
		setColor(deviceNumber, red, green, blue);
		// no blink
		setBlink(deviceNumber, ON_TIME_HALF_SECONDS * 2, 0);
	}

	/**
	 * sends a buffer to the device and waits for a response. This firmware
	 * always responds with "+<command>" for any command "<command>" that it
	 * understands and "-<command>" if it doesn't
	 * 
	 * @param buffer
	 */
	private void sendAndWaitForAck(final byte[] buffer) {
		LOG.debug("Sending: " + new String(buffer));
		device.write(buffer);
		// we should actually get back buffer.length+1
		while (device.charactersAvailable() < buffer.length + 1) {
			try {
				// we should keep some timer/counter here so we know if we've
				// really waited too long
				// we should probably also see if we got a '-' string back as a
				// nack
				// sleep to give time to reply
				Thread.sleep(20);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
			}
		}
		LOG.debug("Received ack: " + device.getAvailableCharacters());
	}

	/**
	 * convert a number 0-15 into HEX ascii
	 * 
	 * <pre>
	 * 0 --> '0'
	 * ...
	 * 9 --> '9'
	 * 10 --> 'A'
	 * ...
	 * 15 --> 'F'
	 * </pre>
	 * 
	 * @param number
	 * @return character byte
	 */
	protected byte convertIntToAsciiChar(final int number) {
		if (number < 0 || number > 15) {
			throw new IllegalArgumentException(
					"number out of single digit hex range " + number);
		}
		byte result;
		if (number > 9) {
			result = (byte) ('A' + number - 10); // we start at 10
		} else {
			result = (byte) ('0' + number);
		}
		// LOG.debug("converted " + number + " to " + (char) result);
		return result;
	}

}
