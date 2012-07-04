package com.freemansoft;

/**
 * mock serial class so we don't actually have to connect. This shold capture call counts
 * 
 * @author joe
 * @version $Revision: #1 $ $Change: 74965 $ $DateTime: 2008/06/27 11:49:28 $ $Author: joe $
 * @since May 15, 2011
 */
public class MockSerialDevice extends SerialDevice {

    /**
     * constructor for testing
     */
    public MockSerialDevice() {
        super();
    }

    /**
     * no-op cover for parent {@inheritDoc}
     */
    @Override
    public int charactersAvailable() {
        return "test string".length();
    }

    /**
     * no-op cover for parent {@inheritDoc}
     */
    @Override
    public boolean charactersQueued() {
        return true;
    }

    /**
     * no-op cover for parent {@inheritDoc}
     */
    @Override
    public void close() {
    // no-op
    }

    /**
     * no-op cover for parent {@inheritDoc}
     */
    @Override
    public String getAvailableCharacters() {
        return "test string";
    }

    /**
     * no-op cover for parent {@inheritDoc}
     */
    @Override
    public void resetViaDtr() {

    }

    /**
     * no-op cover for parent {@inheritDoc}
     */
    @Override
    public void write(final byte[] data) {

    }

    /**
     * no-op cover for parent {@inheritDoc}
     */
    @Override
    public void write(final String data) {

    }
}
