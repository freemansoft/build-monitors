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

import java.util.TimerTask;

import com.freemansoft.IBuildLightDevice;

/**
 * Unfinished! Work NEVER completed for Arduino controlled 4-wire sparkfun
 * addressable strip
 * 
 */
public class AddressableStrip extends TimerTask implements IBuildLightDevice {

	@Override
	public void bldBlink(final int deviceNumber, final int red,
			final int green, final int blue) {
		// TODO Auto-generated method stub

	}

	@Override
	public void bldClear(final int deviceNumber) {
		// TODO Auto-generated method stub

	}

	@Override
	public int bldGetMaxColor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int bldGetNumLights() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void bldSolid(final int deviceNumber, final int red,
			final int green, final int blue) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
