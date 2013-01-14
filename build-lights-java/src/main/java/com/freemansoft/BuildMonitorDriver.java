package com.freemansoft;

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

import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Simple command line program that monitors Hudson and displays build status on
 * physical status devices.
 * 
 * This is not spring configured and is setup to run with command line
 * arguments.
 * <p>
 * run with
 * 
 * <pre>
 * -Djava.library.path=<path to lib directory>
 * </pre>
 * 
 * in Eclipse
 * 
 * <pre>
 * -Djava.library.path=target/lib
 * </pre>
 * <p>
 * Sample Program Arguments if running from inside Eclipse
 * <ul>
 * <li>LED Cube pointed at public hudson server with 4 builds<br>
 * 
 * <pre>
 * device.serial.port=COM9 
 * device.serial.speed=115200
 * device.class.name=com.freemansoft.watcher.devices.LEDCube
 * buildserver.uri=http://ci.jenkins-ci.org/cc.xml
 * buildserver.projects=plugins_virtualbox,plugins_vmware,plugins_chucknorris,plugins_clover
 * </pre>
 * 
 * </li>
 * 
 * <li>Ambient orb point at public Hudson server with one build<br>
 * 
 * <pre>
 * device.serial.port=COM4 
 * device.serial.speed=19200
 * device.class.name=com.freemansoft.watcher.devices.AmbOrb
 * buildserver.uri=http://ci.jenkins-ci.org/cc.xml
 * buildserver.projects=plugins_cvs-tag
 * </pre>
 * 
 * </li>
 * </ul>
 * 
 * @author Joe Freeman
 */
public class BuildMonitorDriver {

	private static Logger LOG = Logger.getLogger(BuildMonitorDriver.class);

	private static final int SERVER_POLLING_INTERVAL = 60;

	/** object build from the server connection string */
	private HudsonServer ciServer;
	/** com port device built from the port string passed in on command line */
	private SerialDevice communicationChannel;
	/** proxy for the physical (or mock) build light */
	private IBuildLightDevice buildLight = null;
	/* configuration parameter projects after being parsed and split */
	private String projectNames[] = new String[0];

	private Properties config = new Properties();

	/**
	 * @param args
	 *            name of the properties files
	 * 
	 * @throws IOException
	 */
	public static void main(final String[] args) {

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				LOG.info("Exited!");
			}
		});

		if (args.length > 1) {
			LOG.error("must run with 0 or 1 args where the one arg is the name of the properties file name");
			System.exit(-1);
		}
		// set the default "by default"
		String propsFileName = "default.properties";
		// the allow override from command line arguments
		if (args.length > 0) {
			propsFileName = args[0];
		}

		final BuildMonitorDriver master = new BuildMonitorDriver();
		try {
			master.configFromPropertiesResource(propsFileName);
		} catch (IOException e) {
			LOG.fatal("Terminating because failed to load props ", e);
			System.exit(-1);
		}
		master.run();

		LOG.info("main is done");
		System.exit(0);
	}

	/**
	 */
	public BuildMonitorDriver() {
		// code moved from here to config methods
	}

	/**
	 * loads the properties file and configurs the device
	 * 
	 * @param propsFileName
	 * @throws IOException
	 *             if we can't find the props file
	 * @throws IllegalStateExcepton
	 *             if configured but does not exist
	 */
	private void configFromPropertiesResource(String propsFileName)
			throws IOException {
		Properties config = new Properties();
		InputStream iStream = BuildMonitorDriver.class.getClassLoader()
				.getResourceAsStream(propsFileName);
		config.load(iStream);
		iStream.close();

		this.config = config;
		projectNames = config.getProperty(
				ConfigurationPropertiesKeys.BUILD_SERVER_PROJECTS_KEY).split(
				",");
		if (projectNames.length == 0) {
			throw new IllegalArgumentException("No project names specified");
		}
		this.communicationChannel = this.createSerialIfConfigured(this.config);
		buildLight = createDeviceProxy(
				config.getProperty(ConfigurationPropertiesKeys.DEVICE_CLASS_NAME_KEY),
				this.communicationChannel,
				config.getProperty(ConfigurationPropertiesKeys.DEVICE_NET_CONNECT_URI));
		if (projectNames.length > buildLight.bldGetNumLights()) {
			throw new IllegalArgumentException("Device supports "
					+ buildLight.bldGetNumLights() + " but "
					+ projectNames.length + " were requested");
		}
	}

	/**
	 * 
	 * @return configured serial device if one was specified in properties
	 * @throws IllegalStateExcepton
	 *             if configured but does not exist
	 */
	public static SerialDevice createSerialIfConfigured(Properties configProps) {
		if (configProps
				.containsKey(ConfigurationPropertiesKeys.DEVICE_SERIAL_PORT_KEY)) {
			SerialDevice newCommChannel = null;
			// configuration parameter sets communication rates
			int dataRate = Integer
					.valueOf(
							configProps
									.getProperty(ConfigurationPropertiesKeys.DEVICE_SERIAL_SPEED_KEY))
					.intValue();
			try {
				newCommChannel = new SerialDevice(
						configProps
								.getProperty(ConfigurationPropertiesKeys.DEVICE_SERIAL_PORT_KEY),
						dataRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
			} catch (IllegalStateException e) {
				newCommChannel = null;
				LOG.error(
						"COM port specified in properties but unable to create, find or use serial port "
								+ configProps
										.getProperty(ConfigurationPropertiesKeys.DEVICE_SERIAL_PORT_KEY),
						e);
				// throw e;
			}
			return newCommChannel;
		} else {
			return null;
		}
	}

	/**
	 * instantiates a device from the class name and passes the attachedChannel
	 * in as a constructor argument. This would be a lot easier with Spring.
	 * 
	 * @param deviceClassName
	 * @param attachedChannel
	 * @return
	 */
	private IBuildLightDevice createDeviceProxy(final String deviceClassName,
			SerialDevice possibleSerialDevice, String possibleUri) {
		IBuildLightDevice blinkenDevice;
		try {
			Class<?> deviceClass = BuildMonitorDriver.class.getClassLoader()
					.loadClass(deviceClassName);
			Constructor<?> deviceClassConstructor;

			if (possibleSerialDevice != null) {
				deviceClassConstructor = deviceClass
						.getConstructor(SerialDevice.class);
				blinkenDevice = (IBuildLightDevice) deviceClassConstructor
						.newInstance(possibleSerialDevice);
			} else if (possibleUri != null) {
				deviceClassConstructor = deviceClass
						.getConstructor(String.class);
				blinkenDevice = (IBuildLightDevice) deviceClassConstructor
						.newInstance(possibleUri);
			} else {
				throw new IllegalArgumentException(
						"No path to physical device specified");
			}

		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException("Failed building light device "
					+ deviceClassName + " with com " + possibleSerialDevice
					+ " or uri " + "<null>", e);
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("Failed building light device "
					+ deviceClassName + " with com " + possibleSerialDevice
					+ " or uri " + "<null>", e);
		} catch (final SecurityException e) {
			throw new IllegalArgumentException("Failed building light device "
					+ deviceClassName + " with com " + possibleSerialDevice
					+ " or uri " + "<null>", e);
		} catch (final InstantiationException e) {
			throw new IllegalArgumentException("Failed building light device "
					+ deviceClassName + " with com " + possibleSerialDevice
					+ " or uri " + "<null>", e);
		} catch (final IllegalAccessException e) {
			throw new IllegalArgumentException("Failed building light device "
					+ deviceClassName + " with com " + possibleSerialDevice
					+ " or uri " + "<null>", e);
		} catch (final InvocationTargetException e) {
			throw new IllegalArgumentException("Failed building light device "
					+ deviceClassName + " with com " + possibleSerialDevice
					+ " or uri " + "<null>", e);
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException("Failed building light device "
					+ deviceClassName + " with com " + possibleSerialDevice
					+ " or uri " + "<null>", e);
		}
		return blinkenDevice;
	}

	/**
	 * the main program's processing loop
	 */
	private void loop() {
		try {
			while (true) {
				for (int i = 0; i < projectNames.length; i++) {
					final HudsonProjectStatus status = ciServer
							.extractElement(projectNames[i]);
					LOG.debug("About to tell device about status " + status);
					switch (status.getStatus()) {
					case SUCCESS:
						if (status.getActivity() == HudsonActivity.BUILDING) {
							buildLight.bldBlink(i, 0, 0,
									buildLight.bldGetMaxColor());
						} else {
							buildLight.bldSolid(i, 0, 0,
									buildLight.bldGetMaxColor());
						}
						break;
					case FAILURE:
						if (status.getActivity() == HudsonActivity.BUILDING) {
							buildLight.bldBlink(i, buildLight.bldGetMaxColor(),
									0, 0);
						} else {
							buildLight.bldSolid(i, buildLight.bldGetMaxColor(),
									0, 0);
						}
						break;
					case EXCEPTION:
					case UNKNOWN:
					default:
						buildLight.bldBlink(i, buildLight.bldGetMaxColor(),
								buildLight.bldGetMaxColor(), 0);
						break;
					}
				}
				// 1/4 of hudson server polling rate so we're never more than
				// 1/4 interval behind
				Thread.sleep(SERVER_POLLING_INTERVAL / 4 * 1000);
			}
		} catch (final InterruptedException e) {
			// assume we're done
		}
	}

	/**
	 * Runs the build server polling loop where we poll for status and then
	 * update the device based on that status.
	 */
	private void run() {
		if (buildLight != null) {
			ciServer = new HudsonServer(
					config.getProperty(ConfigurationPropertiesKeys.BUILD_SERVER_URI_KEY),
					SERVER_POLLING_INTERVAL);
			ciServer.startPollingServer();
			loop();
			ciServer.stopPollingServer();
			communicationChannel.close();
		}

	}
}
