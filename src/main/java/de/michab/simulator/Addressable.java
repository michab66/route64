/* $Id: Addressable.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator;

/**
 * Base interface for addressable components.
 *
 * @author Michael G. Binz
 */
public interface Addressable {
    /**
     * Read a byte from the given address.
     *
     * @param address The source address.
     * @return The byte read.
     */
    byte read(int address);

    /**
     * Read a byte from the given address.
     *
     * @param address The source address.
     * @return The byte read as an int in the range 0..255.
     */
    default int read8(int address) {
        return read(address) & 0xff;
    }

    /**
     * Write a byte into the given address.
     *
     * @param address The target address.
     * @param value   The value to be written.
     */
    void write(int address, byte value);
}
