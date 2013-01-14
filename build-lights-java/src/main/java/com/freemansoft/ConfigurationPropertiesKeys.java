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

/**
 * Well known keys in the properties file
 * 
 * @since Jan, 11, 2013
 * 
 */
public class ConfigurationPropertiesKeys {
	/** configuration parameter sets the device */
	public static final String DEVICE_SERIAL_PORT_KEY = "device.serial.port";
	/** configuration parameter sets communication rates */
	public static final String DEVICE_SERIAL_SPEED_KEY = "device.serial.speed";
	/** configuration when device on network instead of serial port */
	public static final String DEVICE_NET_CONNECT_URI = "device.net.uri";
	/** configuration parameter hardware device to display status */
	public static final String DEVICE_CLASS_NAME_KEY = "device.class.name";

	/** configuration parameter target hudson/jenkins server */
	public static final String BUILD_SERVER_URI_KEY = "buildserver.uri";
	/** configuration parameter projects to be monitored */
	public static final String BUILD_SERVER_PROJECTS_KEY = "buildserver.projects";

}
