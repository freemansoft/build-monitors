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
 * Interface for devices that can be used as build lights
 * 
 * @author Joe Freeman
 */
public interface IBuildLightDevice {

    /**
     * @return the number of build lights supported on this device
     */
    int bldGetNumLights();

    /**
     * 
     * @return the maximum value for a color on this device
     */
    int bldGetMaxColor();

    /**
     * clear everything
     * 
     * @param deviceNumber the light set number in this device starting at 0
     */
    void bldClear(int deviceNumber);

    /**
     * fill with single color
     * 
     * @param deviceNumber the light set number in this device starting at 0
     * @param red color
     * @param green color
     * @param blue color
     */
    void bldSolid(int deviceNumber, final int red, final int green, final int blue);

    /**
     * blink all lights with the passed in color
     * 
     * @param deviceNumber the light set number in this device starting at 0
     * @param red color
     * @param green color
     * @param blue color
     */
    void bldBlink(int deviceNumber, final int red, final int green, final int blue);
}
