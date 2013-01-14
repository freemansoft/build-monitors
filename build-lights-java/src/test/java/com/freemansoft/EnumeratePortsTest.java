package com.freemansoft;

import gnu.io.CommPortIdentifier;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * simple rxtx program to enumerate ports
 * <p>
 * Run this unit test withwith
 * 
 * <pre>
 * -Djava.library.path=<path to dll>
 * </pre>
 * 
 * Unit tests run from the root of the project so the dll can be found here
 * 
 * <pre>
 * -Djava.library.path=target/lib
 * </pre>
 */
public class EnumeratePortsTest {

	/** logger */
	private static Logger LOG = Logger.getLogger(EnumeratePortsTest.class);

	@Test
	public void testListPorts() {
		listPorts();
	}

	static void listPorts() {
		final java.util.Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier
				.getPortIdentifiers();
		while (portEnum.hasMoreElements()) {
			final CommPortIdentifier portIdentifier = portEnum.nextElement();
			LOG.info(portIdentifier.getName() + " - "
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
