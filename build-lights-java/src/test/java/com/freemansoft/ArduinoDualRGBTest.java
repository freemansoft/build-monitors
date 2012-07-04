package com.freemansoft;

import static org.junit.Assert.assertEquals;
import gnu.io.SerialPort;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * test class
 * <p>
 * -Djava.library.path=c:/dev/build-lights/target/lib
 * 
 * @author joe
 * @version $Revision: #1 $ $Change: 74965 $ $DateTime: 2008/06/27 11:49:28 $ $Author: joe $
 * @since May 8, 2011
 */
public class ArduinoDualRGBTest {

    /** logger */
    private static Logger LOG = Logger.getLogger(ArduinoDualRGBTest.class);

    private ArduinoDualRGB blinkenLights;
    private SerialDevice device;

    @Before
    public void setUp() {
        // should get from properties
        final String defaultPort = "COM3";

        device =
            new SerialDevice(defaultPort, 9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        if (device != null) {
            blinkenLights = new ArduinoDualRGB(device);
        } else {
            LogUtil.logWithThreadName(LOG, "port " + defaultPort + " not found.", true);
        }
    }

    /**
     * close port after every test
     */
    @After
    public void tearDown() {
        blinkenLights = null;
        device.close();
    }

    @Test
    public void testLights() {
        final int maxColor = blinkenLights.bldGetMaxColor();
        LOG.info("initial clear");
        blinkenLights.bldClear(0);
        blinkenLights.bldClear(1);
        try {
            // test light 0
            Thread.sleep(3000);
            LOG.info("filling with red");
            blinkenLights.bldSolid(0, maxColor, 0, 0);
            blinkenLights.bldSolid(1, maxColor, 0, 0);
            Thread.sleep(3000);
            LOG.info("filling with blue");
            blinkenLights.bldSolid(0, 0, 0, maxColor);
            blinkenLights.bldSolid(1, 0, 0, maxColor);
            Thread.sleep(3000);
            LOG.info("filling with blinking yellow");
            blinkenLights.bldBlink(0, maxColor, maxColor, 0);
            blinkenLights.bldBlink(1, maxColor, maxColor, 0);
            LOG.info("waiting before exiting");
            Thread.sleep(5000);
            blinkenLights.bldClear(0);
            LOG.info("Exiting test");
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            LOG.error("Unexpected " + e);
        }
    }

    @Test
    public void testConversion() {
        final ArduinoDualRGB device = new ArduinoDualRGB(null);
        assertEquals('0', device.convertIntToAsciiChar(0));
        assertEquals('9', device.convertIntToAsciiChar(9));
        assertEquals('A', device.convertIntToAsciiChar(10));
        assertEquals('F', device.convertIntToAsciiChar(15));
    }
}
