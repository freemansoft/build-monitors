package com.freemansoft;

import gnu.io.CommPortIdentifier;

import org.junit.Test;

/**
 * simple rxtx program to enumerate ports run with -
 * <p>
 * -Djava.library.path=c:/dev/build-lights/target/lib
 */
public class EnumeratePortsTest {

    @Test
    public void testListPorts() {
        listPorts();
    }

    static void listPorts() {
        final java.util.Enumeration<CommPortIdentifier> portEnum =
            CommPortIdentifier.getPortIdentifiers();
        while (portEnum.hasMoreElements()) {
            final CommPortIdentifier portIdentifier = portEnum.nextElement();
            System.out.println(portIdentifier.getName() + " - "
                + getPortTypeName(portIdentifier.getPortType()));
        }
    }

    static String getPortTypeName(final int portType) {
        switch (portType) {
            case CommPortIdentifier.PORT_I2C:
                return "I2C";
            case CommPortIdentifier.PORT_PARALLEL:
                return "Parallel";
            case CommPortIdentifier.PORT_RAW:
                return "Raw";
            case CommPortIdentifier.PORT_RS485:
                return "RS485";
            case CommPortIdentifier.PORT_SERIAL:
                return "Serial";
            default:
                return "unknown type";
        }
    }
}
