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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.freemansoft.IBuildLightDevice;
import com.freemansoft.SerialDevice;

/**
 * Device adapter for the Ambient Orb
 * 
 * This talks with an ambient orb over a USB-->TTL adapter based serial port.
 * The Ambient Orb has a 5V (TTL) serial port on the connector on the back as
 * documented in the ambient orb developer guide: <br>
 * http://www.ambientdevices.com/developer/DIYSerialDeveloperBoard.html <br>
 * and the ambient orb programmer guide for ORB AML: <br>
 * http://www.ambientdevices.com/developer/Orb%20AML%202_0%20v2.pdf <br>
 * <p>
 * You can create a cable for this and attach to a PC over a USB-->TTL adapter.
 * Most hacked up phone cables are 3.3v which may get fried by 5V. I used an
 * FTDI 5V ttl to usb adapter
 * 
 * @author Joe Freeman
 */
public class AmbOrb implements IBuildLightDevice {

	private static Logger LOG = Logger.getLogger(AmbOrb.class);

	/**
	 * RGB color values for the standard palette. essentially 7 values per pixel
	 * including 0. so 2.5 bits or so
	 */
	private static final int AMBIENT_COLOR_CHART[][] = {//
	{ 255, 0, 0 }, { 255, 43, 0 }, { 255, 85, 0 }, { 255, 128, 0 },
			{ 255, 170, 0 }, { 255, 213, 0 }, { 255, 255, 0 }, { 212, 255, 0 },
			{ 170, 255, 0 }, { 128, 255, 0 }, { 85, 255, 0 }, { 43, 255, 0 },
			{ 0, 255, 0 }, { 0, 255, 42 }, { 0, 255, 85 }, { 0, 255, 128 },
			{ 0, 255, 170 }, { 0, 255, 212 }, { 0, 255, 255 }, { 0, 212, 255 },
			{ 0, 170, 255 }, { 0, 128, 255 }, { 0, 85, 255 }, { 0, 42, 255 },
			{ 0, 0, 255 }, { 42, 0, 255 }, { 85, 0, 255 }, { 128, 0, 255 },
			{ 170, 0, 255 }, { 213, 0, 255 }, { 255, 0, 255 }, { 255, 0, 213 },
			{ 255, 0, 170 }, { 255, 0, 128 }, { 255, 0, 85 }, { 255, 0, 43 },
			{ 255, 255, 255 }

	};

	/**
	 * RGB string (0..6) that can be used to reverse map to an index to look
	 * into AMBIENT_COLOR_CHART
	 */
	private static final String AMBIENT_INDEX_COLOR_CHART[] = { "600", "610",
			"620", "630", "640", "650", "660", "560", "460", "360", "260",
			"160", "060", "061", "062", "063", "064", "065", "066", "056",
			"046", "036", "026", "016", "006", "106", "206", "306", "406",
			"506", "606", "605", "604", "603", "602", "601", "666" };

	private static final Map<String, Integer> mapping = new HashMap<String, Integer>();

	static {
		for (int i = 0; i < AMBIENT_INDEX_COLOR_CHART.length; i++) {
			mapping.put(AMBIENT_INDEX_COLOR_CHART[i], Integer.valueOf(i));
		}
	}

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
	public AmbOrb(final SerialDevice device) {
		if (device == null) {
			throw new IllegalArgumentException("No serial device specified");
		}
		this.device = device;
		// the device cannot be reset via DTR so have to hop it is in the ready
		// state
		device.write("~GT"); // turn off pager
		// device.write("~I"); // query for info
		// the firmware starts with the string any string, the echo of the
		// commands
		while (device.charactersAvailable() == 0) {
			try {
				Thread.sleep(300);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
			}
		}
		LOG.debug("Hardware initialized returned string: "
				+ device.getAvailableCharacters());
	}

	/**
	 * Infers an entry in the palette from the values passed in. This supports
	 * <ul>
	 * <li>all on</li>
	 * <li>all off</li>
	 * <li>any two colors on at any brightness</li>
	 * </ul>
	 * It does not support
	 * <ul>
	 * <li>all three colors on at any brightness other than 0 or max</li>
	 * </ul>
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return the index of the color palette
	 */
	protected int calculateAmlColor(final int red, final int green,
			final int blue) {
		final String stringMatch = "" + red + "" + green + "" + blue;
		final Integer position = mapping.get(stringMatch);
		if (position == null) {
			throw new IllegalArgumentException("Invalid color combo "
					+ stringMatch);
		}
		return position.intValue();
	}

	/**
	 * converts the color and blink command into an Orb compatible string and
	 * sends it
	 * 
	 * @param calculateAmlColor
	 * @param blinkStyle
	 */
	private void sendBasicAml(final int calculateAmlColor, final int blinkStyle) {
		final byte sendBuffer[] = basicAmlString(calculateAmlColor, blinkStyle);
		LOG.info("should have sent AML for color " + calculateAmlColor
				+ " blink " + blinkStyle);
		device.write(sendBuffer);
	}

	/**
	 * calculate a string for an aml color and blink style
	 * 
	 * @param calculateAmlColor
	 * @param blinkStyle
	 * @return
	 */
	protected byte[] basicAmlString(final int calculateAmlColor,
			final int blinkStyle) {
		final byte byteOne = calculateAmlByteOne(calculateAmlColor, blinkStyle);
		final byte byteTwo = calculateAmlByteTwo(calculateAmlColor, blinkStyle);
		final byte sendBuffer[] = new byte[4];
		sendBuffer[0] = '~';
		sendBuffer[1] = 'A';
		sendBuffer[2] = byteOne;
		sendBuffer[3] = byteTwo;
		return sendBuffer;
	}

	/**
	 * byte one from the programming guide
	 * 
	 * @param calculateAmlColor
	 * @param blinkStyle
	 * @return
	 */
	private byte calculateAmlByteTwo(final int calculateAmlColor,
			final int blinkStyle) {
		return (byte) ((calculateAmlColor + (37 * blinkStyle)) % 94 + 32);
	}

	/**
	 * byte two from the programming guide
	 * 
	 * @param calculateAmlColor
	 * @param blinkStyle
	 * @return
	 */
	private byte calculateAmlByteOne(final int calculateAmlColor,
			final int blinkStyle) {
		return (byte) ((calculateAmlColor + (37 * blinkStyle)) / 94 + 32);
	}

	/*-----------------------------------------------------------------------
	 * 
	 * BuildLightDevice interface
	 * 
	 * -----------------------------------------------------------------------
	 */
	@Override
	public void bldBlink(final int deviceNumber, final int red,
			final int green, final int blue) {
		sendBasicAml(calculateAmlColor(red, green, blue), 3);
	}

	/**
	 * No way to turn off the orb. Could turn all lights (white)
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void bldClear(final int deviceNumber) {
		throw new NotImplementedException();
	}

	/**
	 * limited color palate since we're not sending RGB because that mode
	 * doesn't blink
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public int bldGetMaxColor() {
		return 6;
	}

	@Override
	public int bldGetNumLights() {
		return 1;
	}

	@Override
	public void bldSolid(final int deviceNumber, final int red,
			final int green, final int blue) {
		sendBasicAml(calculateAmlColor(red, green, blue), 0);
	}

}
