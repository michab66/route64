/* $Id: DefaultChip.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.simulator;



/**
 * This is a helper class providing a default implementation for the memory
 * attribute and a simple port array factory.
 *
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
public abstract class DefaultChip
  implements
    Chip
{
  /**
   * This creates an array of <code>Ports</code> tied to the <code>Chip</code>.
   * A write operation on the <code>Port</code> is forwarded to 
   * <code>Chip.write()</code>.  Just a helper, can be used by subclasses but 
   * not overridden.
   * 
   * @param numOfRegs The number of registers to be supported.
   * @return A newly allocated array of ports.  The array index represents the
   *         respective port number.
   */
  protected final Port[] createPorts( int numOfRegs )
  {
    Port[] result = new Port[ numOfRegs ];

    for ( int i = numOfRegs -1 ; i >= 0 ; i-- )
      result[i] = new Port( this, i );

    return result;
  }
}
