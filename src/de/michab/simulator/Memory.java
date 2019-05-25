/* $Id: Memory.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.simulator;



/**
 * Base interface for addressable components.
 *
 * @version $Revision: 11 $
 * @author Michael Binz
 */
public interface Memory
  extends Addressable
{
  /**
   * Add a forwarder to the passed memory position.
   * 
   * @param f The <code>Forwarder</code> to add.
   * @param where The memory address where the <code>Forwarder</code> is to be
   *        placed.
   */
  void set( Forwarder f, int where );



  /**
   * Get the memory's size.
   *
   * @return The size of the memory.
   */
  int getSize();



  /**
   * Get a reference on a raw version of the current memory configuration.
   * It may not always possible to use that to get something meaningful -- e.g.
   * in case of paged memory, but if something useful is returned access speed
   * will be hilarious.
   *
   * @return The raw memory bytes in an array.
   */
  byte[] getRawMemory();



  /**
   * Returns a 16 bit address from the given memory address.  Respects byte
   * order.
   *
   * @param adr The address to read.
   * @return The 16 bit address located at the passed memory position.
   */
  int getVectorAt( int adr );
}
