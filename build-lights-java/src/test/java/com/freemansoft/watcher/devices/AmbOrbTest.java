package com.freemansoft.watcher.devices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.freemansoft.MockSerialDevice;
import com.freemansoft.watcher.devices.AmbOrb;

public class AmbOrbTest {

    /**
     * object under test
     */
    private AmbOrb fixture;

    /**
     * create an orb without any serial port
     */
    @Before
    public void setUp() {
        fixture = new AmbOrb(new MockSerialDevice());

    }

    /**
     * 
     */
    public void testGetConfig() {
        assertEquals(1, fixture.bldGetNumLights());
        assertEquals(6, fixture.bldGetMaxColor());
    }

    /**
     * test calculateAmlColor
     */
    @Test
    public void testCalculateAmlColor() {
        try {
            fixture.calculateAmlColor(0, 0, 0);
            fail("failed to detect illegal combination 000");
        } catch (final IllegalArgumentException e) {
            // yeah
        }
        try {
            fixture.calculateAmlColor(7, 7, 7);
            fail("failed to detect illegal combination 777");
        } catch (final IllegalArgumentException e) {
            // yeah
        }
        // pick a couple
        assertEquals(36, fixture.calculateAmlColor(6, 6, 6));
        assertEquals(0, fixture.calculateAmlColor(6, 0, 0));
        assertEquals(1, fixture.calculateAmlColor(6, 1, 0));
        assertEquals(34, fixture.calculateAmlColor(6, 0, 2));
        try {
            fixture.calculateAmlColor(3, 3, 3);
            fail("failed to detect illegal combination 333");
        } catch (final IllegalArgumentException e) {
            // yeah
        }
    }

    @Test
    public void testAmlString() {
        final byte string00[] = { '~', 'A', ' ', ' ' };
        final byte result00[] = fixture.basicAmlString(0, 0);
        assertTrue(Arrays.equals(string00, result00));
        final byte string240[] = { '~', 'A', ' ', '8' };
        final byte result240[] = fixture.basicAmlString(24, 0);
        assertTrue(Arrays.equals(string240, result240));
        final byte string02[] = { '~', 'A', ' ', 'j' };
        final byte result02[] = fixture.basicAmlString(0, 2);
        assertTrue(Arrays.equals(string02, result02));
    }
}
