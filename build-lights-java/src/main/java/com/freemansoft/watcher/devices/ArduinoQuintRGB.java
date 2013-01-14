package com.freemansoft.watcher.devices;

import com.freemansoft.SerialDevice;

/**
 * This class assumes you have a 5 light Arduino. My sample uses the Jee Labs Dimmer plug which has
 * 16 PWM ports or 5 RGB lights. The 11.06 firmware auto-detects if it has a Dimmer Plug in it and
 * supports that so no querying or other activity is required
 * 
 * @author joe
 * @version $Revision: #1 $ $Change: 74965 $ $DateTime: 2008/06/27 11:49:28 $ $Author: joe $
 * @since May 31, 2011
 */
public class ArduinoQuintRGB extends ArduinoDualRGB {

    /**
     * standard device constructor
     * 
     * @param device serial divce we connect to
     */
    public ArduinoQuintRGB(final SerialDevice device) {
        super(device);
    }

    @Override
    public int bldGetNumLights() {
        return 5;
    }

}
