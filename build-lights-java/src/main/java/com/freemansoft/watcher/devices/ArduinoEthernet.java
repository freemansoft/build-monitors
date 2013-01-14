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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.freemansoft.IBuildLightDevice;

/**
 * This drives an arduino based ethernet controlled 3 wire led device. The
 * arduino is running both bonjour and the webduino web server software
 * <p>
 * see http://joe.blog.freemansoft.com
 * <p>
 * This can have a race condition when the program terminates where the timer
 * loop could turn on a light even though the light was just cleared. The light
 * turn on in the timer task can be queued up and blocked in the sychronized
 * method and complete after the clear
 * 
 */
public class ArduinoEthernet extends TimerTask implements IBuildLightDevice {

	/** our logger */
	private static Logger LOG = Logger.getLogger(ArduinoEthernet.class);

	/** hack. This should be configured */
	public static final int DEFAULT_NUM_LIGHTS_PER_STRIP = 30;
	private int bldGetNumLights = 0;

	/** URI path to the ethernet based build light */
	private String pathToDevice = null;

	/** RGB array one for each light */
	private final RGBTriplet ledLastFill[];
	/** are the blinking lights on or off */
	private boolean blinkStateIsCurrentlySolid = true;
	/** which lamps currently have blink enabled */
	private final boolean blinkEnabled[];

	public ArduinoEthernet(String pathToDevice) {
		if (pathToDevice == null) {
			throw new IllegalArgumentException("No network path specified");
		}
		LOG.debug("Using device at " + pathToDevice);
		this.pathToDevice = pathToDevice;
		// should be configured
		this.bldGetNumLights = DEFAULT_NUM_LIGHTS_PER_STRIP;

		// configure all blinking off at power up
		blinkEnabled = new boolean[this.bldGetNumLights];
		ledLastFill = new RGBTriplet[this.bldGetNumLights];
		blinkStateIsCurrentlySolid = true;
		for (int i = 0; i < this.bldGetNumLights; i++) {
			blinkEnabled[i] = false;
			ledLastFill[i] = new RGBTriplet(0, 0, 0);
		}
		// start the blink timer
		final Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(this, 2000, 1500);
	}

	@Override
	/**
	 * thirty lights per meter
	 */
	public int bldGetNumLights() {
		return bldGetNumLights;
	}

	/**
	 * 8 bit per channel
	 */
	@Override
	public int bldGetMaxColor() {
		return 255;
	}

	@Override
	public void bldClear(int deviceNumber) {
		LOG.info("Received bldClr()");
		bldSolid(deviceNumber, 0, 0, 0);
	}

	@Override
	public void bldSolid(int deviceNumber, int red, int green, int blue) {
		if (deviceNumber >= bldGetNumLights()) {
			throw new IllegalArgumentException("LED requested: " + deviceNumber
					+ " is larger than maximum " + bldGetNumLights());
		}
		LOG.info("Received bldSolid(" + deviceNumber + "," + red + "," + green
				+ "," + blue + ")");
		// remove any blinking
		ledLastFill[deviceNumber] = new RGBTriplet(red, green, blue);
		blinkEnabled[deviceNumber] = false;
		postLightChangeToDevice(deviceNumber, ledLastFill[deviceNumber]);
	}

	@Override
	public void bldBlink(int deviceNumber, int red, int green, int blue) {
		if (deviceNumber >= bldGetNumLights()) {
			throw new IllegalArgumentException("LED requested: " + deviceNumber
					+ " is larger than maximum " + bldGetNumLights());
		}
		LOG.info("Received bldBlink(" + deviceNumber + "," + red + "," + green
				+ "," + blue + ")");
		// device does not have intrinsic blinking
		ledLastFill[deviceNumber] = new RGBTriplet(red, green, blue);
		blinkEnabled[deviceNumber] = true;
		// pick up on next blink change. this avoids short cycles
		// postLightChangeToDevice(deviceNumber, ledLastFill[deviceNumber]);
	}

	/**
	 * creates the post data and then calls the post routine
	 * 
	 * @param deviceNumber
	 * @param color
	 */
	private void postLightChangeToDevice(int deviceNumber, RGBTriplet color) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("r" + deviceNumber, "" + color.getRed()));
		nvps.add(new BasicNameValuePair("g" + deviceNumber, ""
				+ color.getGreen()));
		nvps.add(new BasicNameValuePair("b" + deviceNumber, ""
				+ color.getBlue()));
		postToDevice(nvps);

	}

	/**
	 * Post this form data to the arduino ethernet device
	 * <p>
	 * Only allow one network connection to the device at a time. Can get
	 * overlap if we receive build light command while blink is running
	 * 
	 * @param nvps
	 */
	private synchronized void postToDevice(List<NameValuePair> nvps) {
		LOG.debug("Posting the following to arduino " + nvps);

		HttpClient client = new DefaultHttpClient();
		HttpPost postHandler = new HttpPost(pathToDevice);

		try {
			postHandler.setEntity(new UrlEncodedFormEntity(nvps));
		} catch (UnsupportedEncodingException e) {
			LOG.error(
					"Unable to Encode form post you're about to get more errors",
					e);
		}
		HttpResponse response;
		try {
			response = client.execute(postHandler);
			System.out.println(response.getStatusLine());
			HttpEntity responseEntity = response.getEntity();
			// do something useful with the response body
			// and ensure it is fully consumed
			EntityUtils.consume(responseEntity);
		} catch (ClientProtocolException e) {
			if (e.getCause() instanceof ProtocolException
					&& e.getCause().getMessage().startsWith("Redirect")) {
				LOG.info("The webduino 303 redirect (POST/redirect) confuses Apache client "
						+ "I think because it tries to find house with trailng '/' :-(");
			} else {
				LOG.error(
						"Some kind of protocol excepton while trying to execute POST",
						e);
			}
		} catch (IOException e) {
			LOG.error("Some kind of IO excepton while trying to execute POST",
					e);
		} finally {
			postHandler.releaseConnection();
		}
	}

	private static final RGBTriplet ledOffState = new RGBTriplet(0, 0, 0);

	/**
	 * Simulates embedded blink behavior
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		blinkStateIsCurrentlySolid = !blinkStateIsCurrentlySolid;
		for (int i = 0; i < bldGetNumLights(); i++) {
			if (blinkEnabled[i]) {
				LOG.debug("changing state of blinking light " + i);
				if (blinkStateIsCurrentlySolid) {
					postLightChangeToDevice(i, this.ledLastFill[i]);
				} else {
					postLightChangeToDevice(i, ledOffState);
				}
			}
		}
	}

}
