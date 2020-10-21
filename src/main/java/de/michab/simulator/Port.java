/* $Id: Port.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2008 Michael G. Binz
 */
package de.michab.simulator;



/**
 * <p>A <code>Port</code> models a <code>Chip</code>'s register cells.  These 
 * may be mapped into the memory space.  This results in a forwarding of all 
 * memory read/write accesses to the associated <code>Chip</code>'s 
 * registers.</p>
 * <p>Note that the read/write operations must not be synchronized -- a Port
 * access may in some cases result in a further Port access, which can lead to
 * deadlocks.</p>
 *
 * @see de.michab.simulator.Chip
 */
public class Port
  implements Forwarder
{
  /**
   * The chip owning this port.
   */
  private final Chip _home;



  /**
   * This port's register number.  Note that numbering is chip relative, not
   * memory relative -- this has nothing to do with the address of the mapped
   * port in memory.
   */
  private final int _portId;



  /**
   * Creates an instance.
   *
   * @param home The <code>Chip</code> this <code>Port</code> belongs to.
   * @param portId The <code>port</code> number.
   */
  public Port( Chip home, int portId )
  {
    _home = home;
    _portId = portId;
  }



  /**
   * Read a byte from this port.  This is actually forwarded to the hosting
   * <code>Chip</code> of this port.
   *
   * @return The port's contents.
   */
  public byte read()
  {
    return _home.read( _portId );
  }



  /**
   * Write a byte to this port.  This is actually forwarded to the hosting
   * <code>Chip</code> of this port.
   *
   * @param value The byte to be written.
   */
  public void write( byte value )
  {
    _home.write( _portId, value );
  }
}
