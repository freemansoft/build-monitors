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

/**
 * Dummy Build light that implements the Ethernet based constructor.
 * <p>
 * Mainly for testing.
 * 
 * @since Jan, 11, 2013
 */
public class DummyNetBuildLightDevice extends DummyBuildLightDevice {

	public DummyNetBuildLightDevice(final String devicePath) {
		if (devicePath == null) {
			throw new IllegalArgumentException("No network device specified");
		}
		Logger.getLogger(this.getClass()).info("Dummy logger created");
	}

}
