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

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import org.apache.log4j.Logger;

/**
 * Serial port wrapper for rxtx. This monitors the inbound connection buffering data that can be
 * picked up later. Writes are blocking meaning the caller will wait until the data is written out
 * 
 * @author Joe Freeman
 */
public class SerialDevice implements SerialPortEventListener {

    private static Logger LOG = Logger.getLogger(SerialDevice.class);

    private SerialPort serialPort = null;
    /** from the serial port */
    private InputStream inputStream = null;
    /** from the serial port */
    private OutputStream outputStream = null;
    /** this is ridiculously inefficient and should be some circular queue */
    private StringBuffer capturedCharacters = new StringBuffer(100);

    /**
     * Exists for mock testing. Do not use in the real app
     */
    protected SerialDevice() {

    }

    /**
     * 
     * @param portName
     * @param baudRate
     * @param dataBits
     * @param stopBits
     * @param parity
     */
    public SerialDevice(final String portName, final int baudRate, final int dataBits,
                    final int stopBits, final int parity) {
        serialPort = findSerialPort(portName, baudRate, dataBits, stopBits, parity);
        if (serialPort == null) {
            throw new IllegalStateException("unable to create serial port for " + portName);
        }
        try {
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
        } catch (final IOException e) {
            LogUtil.logWithThreadName(LOG, "IO Exception " + e, true);
        }
        // start listening when the thread starts
        try {
            serialPort.addEventListener(this);
        } catch (final TooManyListenersException e) {
            LogUtil.logWithThreadName(LOG, "too many listeners " + e, true);
        }
        serialPort.notifyOnDataAvailable(true);

    }

    /**
     * 
     * @return true if we have captured characters from the serial port
     */
    public int charactersAvailable() {
        return capturedCharacters.length();
    }

    /**
     * monitors writer thread - not implemented until we do queued writing
     * 
     * @return returns true if still writing characters
     */
    public boolean charactersQueued() {
        return false;
    }

    /**
     * close clean up and invalidate
     */
    public void close() {
        serialPort.notifyOnDataAvailable(false);
        try {
            inputStream.close();
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to close input ", e);
        }
        try {
            outputStream.close();
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to close output ", e);
        }
        serialPort.close();

        inputStream = null;
        outputStream = null;
        serialPort = null;

        capturedCharacters = null;
    }

    /**
     * 
     * @param portName
     * @param baudRate
     * @param dataBits
     * @param stopBits
     * @param parity
     * @return
     */
    @SuppressWarnings("unchecked")
    private SerialPort findSerialPort(final String portName, final int baudRate,
                    final int dataBits, final int stopBits, final int parity) {
        CommPortIdentifier portId;
        Enumeration<CommPortIdentifier> portList;
        portList = CommPortIdentifier.getPortIdentifiers();

        while (serialPort == null && portList.hasMoreElements()) {
            portId = portList.nextElement();

            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {

                if (portId.getName().equals(portName)) {
                    LogUtil.logWithThreadName(LOG, "Found port " + portName, true);
                    try {
                        serialPort = (SerialPort) portId.open("SimpleUtil", 2000);
                        try {
                            serialPort.setSerialPortParams(baudRate, dataBits, stopBits, parity);
                            break;
                        } catch (final UnsupportedCommOperationException e) {
                            LogUtil
                                .logWithThreadName(LOG, "Unable to setup port params " + e, true);
                            serialPort = null;
                        }
                    } catch (final PortInUseException e) {
                        LogUtil.logWithThreadName(LOG, "Port in use.", true);
                        continue;
                    }
                }
            }
        }
        return serialPort;
    }

    /**
     * 
     * @return any characters in the buffer
     */
    public synchronized String getAvailableCharacters() {
        final String extract = capturedCharacters.substring(0);
        final int extractLength = extract.length();
        capturedCharacters.delete(0, extractLength);
        // this is probably costly and should be handled some other way
        capturedCharacters.trimToSize();
        LogUtil.logWithThreadName(LOG, "returning " + extractLength + " characters: '" + extract
            + "'", true);
        return extract;
    }

    /**
     * Event handler call back to do the data capture
     */
    private void readDataFromPort() {
        final byte[] readBuffer = new byte[20];
        try {
            while (inputStream.available() > 0) {
                // gratuitous buffer usage here :-(
                final int numBytes = inputStream.read(readBuffer);
                final String capturedString = new String(readBuffer, 0, numBytes);
                capturedCharacters.append(capturedString);
                LogUtil.logWithThreadName(LOG, "numBytes:" + numBytes + " Received:"
                    + capturedCharacters, true);
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to read from port ", e);
        }
    }

    /**
     * Event callback
     * 
     * @param event
     * 
     * @see
     */
    public void serialEvent(final SerialPortEvent event) {
        switch (event.getEventType()) {
            case SerialPortEvent.BI:
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
                break;
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                // the output buffer is empty
                LogUtil.logWithThreadName(LOG, "Unexpected receipt of OUTPUT_BUFFER_EMPTY", true);
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                // the input buffer has data available
                readDataFromPort();
                break;
        }
    }

    /**
     * writes a message to the serial device
     * 
     * @param messageString
     */
    public void write(final String messageString) {
        LogUtil.logWithThreadName(LOG, "Writing \"" + messageString + "\" to "
            + serialPort.getName(), true);
        write(messageString.getBytes());
    }

    /**
     * writes raw bytes
     * 
     * @param messageBytes
     */
    public void write(final byte[] messageBytes) {
        try {
            outputStream.write(messageBytes);
        } catch (final IOException e) {
            System.err.println("failed to write " + e);
        }
    }

    /**
     * Provides mechanism to reset arduino and other boards via DTR
     * <p>
     * This method was added because rxtx changed the behavior between 2.1.7 and 2.2pre2. It's
     * probably ok because not all devices need to be reset so now the devices can call this method
     * in their constructor if they need it
     */
    public void resetViaDtr() {
        serialPort.setDTR(true);
        // should we set it false again?
    }
}
