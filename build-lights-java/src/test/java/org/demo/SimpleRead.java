package org.demo;

/*
 * @(#)SimpleRead.java 1.12 98/06/25 SMI
 * 
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved. SUN PROPRIETARY/CONFIDENTIAL. Use is
 * subject to license terms.
 * 
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use, modify and
 * redistribute this software in source and binary code form, provided that i) this copyright notice
 * and license appear on all copies of the software; and ii) Licensee does not utilize the software
 * in a manner which is disparaging to Sun.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL EXPRESS OR IMPLIED
 * CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE
 * USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGES.
 * 
 * This software is not designed or intended for use in on-line control of aircraft, air traffic,
 * aircraft navigation or aircraft communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */
import gnu.io.SerialPort;

import org.apache.log4j.Logger;

import com.freemansoft.SerialDevice;

/**
 * Class declaration
 * 
 * 
 * @author
 * @version 1.8, 08/03/00
 */
public class SimpleRead {

	private static Logger LOG = Logger.getLogger(SerialDevice.class);

	/**
	 * Method declaration
	 * 
	 * 
	 * @param args
	 * 
	 * @see
	 */
	public static void main(final String[] args) {
		String defaultPort = "COM9";

		if (args.length > 0) {
			defaultPort = args[0];
		}

		final SerialDevice device = new SerialDevice(defaultPort, 115200,
				SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);
		if (device != null) {
			try {
				LOG.debug("Sleeping waiting for call back from port");
				// read for 5 seconds
				Thread.sleep(5000);
			} catch (final InterruptedException e) {
				LOG.debug("Reader thread is interrupted");
			}
			LOG.debug("device captured: '" + device.getAvailableCharacters()
					+ "'");
			device.close();
		} else {
			LOG.debug("port " + defaultPort + " not found.");
		}
		LOG.debug("main is done");

	}

}
