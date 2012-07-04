package com.freemansoft;

import gnu.io.SerialPort;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Demo to verify LEDCube class that talks to SeedStudio running 1.0.7 firmwar that supports 3D
 * <p>
 * -Djava.library.path=c:/dev/build-lights/target/lib
 */
public class LEDCubeTest {

    /** logger */
    private static Logger LOG = Logger.getLogger(LEDCube.class);

    private LEDCube cube;
    private SerialDevice device;

    @Before
    public void setUp() {
        // should get from properties
        final String defaultPort = "COM9";

        device =
            new SerialDevice(defaultPort, 115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        if (device != null) {
            cube = new LEDCube(device);
        } else {
            LogUtil.logWithThreadName(LOG, "port " + defaultPort + " not found.", true);
        }
    }

    /**
     * close port after every test
     */
    @After
    public void tearDown() {
        cube = null;
        device.close();
    }

    /**
     * not really a unit test more of a demo
     */
    @Test
    @Ignore
    public void testCubeFill() {
        LOG.info("clearing");
        cube.clearAll();
        try {
            Thread.sleep(2000);
            LOG.info("filling with red");
            cube.fillAll(14, 0, 0);
            Thread.sleep(2000);
            LOG.info("filling with blue");
            cube.fillAll(0, 0, 14);
            LOG.info("waiting before exiting");
            Thread.sleep(2000);
            cube.clearAll();
            LOG.info("Exiting test");
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            LOG.error("Unexpected " + e);
        }
    }

    int[][] colorChart =
        { { 7, 0, 0 }, { 14, 0, 0 }, { 14, 7, 0 }, { 14, 14, 0 }, { 7, 14, 0 }, { 0, 14, 0 },
            { 0, 14, 7 }, { 0, 14, 14 }, { 0, 7, 14 }, { 0, 0, 14 }, { 7, 0, 14 }, { 14, 0, 14 },
            { 14, 0, 7 }, { 14, 0, 0 } };

    /**
     * not really a unit test more of a demo
     * <p>
     * thread.sleep statements were added so user could se color changes
     */
    @Test
    public void testCube3D() {
        LOG.info("starting testCube3D");
        try {
            // do some horizontal planes
            for (int repCount = 0; repCount < 3; repCount++) {
                for (int y = 0; y <= 3; y++) {
                    cube.clearAll();
                    Thread.sleep(100);
                    for (int colorIndex = 0; colorIndex < colorChart.length; colorIndex++) {
                        cube.drawDot(-1, y, -1, colorChart[colorIndex][0],
                            colorChart[colorIndex][1], colorChart[colorIndex][2]);
                        Thread.sleep(100);
                    }
                }
            }
            // now do some vertical bars
            for (int repCount = 0; repCount < 3; repCount++) {
                for (int x = 0; x <= 3; x++) {
                    for (int z = 0; z <= 3; z++) {
                        cube.clearAll();
                        for (int colorIndex = 0; colorIndex < colorChart.length; colorIndex++) {
                            cube.drawDot(x, -1, z, colorChart[colorIndex][0],
                                colorChart[colorIndex][1], colorChart[colorIndex][2]);
                        }
                        Thread.sleep(50);
                    }
                }
            }
            Thread.sleep(2000);
            cube.clearAll();
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            LOG.error("Unexpected " + e);
        }
        LOG.info("Exiting test");
    }
}
