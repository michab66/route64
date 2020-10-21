/* $Id: Extension.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import de.michab.simulator.*;



/**
 * This is an extension a.k.a. a patch.  An instance of this class can be
 * installed in memory just like a <code>Chip</code> (which it implements
 * actually.)  If execution hits the memory address, the
 * <code>extensionCalled()</code> template method is activated and an RTS
 * opcode is returned.
 *
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
abstract public class Extension
  implements Chip
{
  /**
   * The opcode returned when the processor executes the location this
   * extension is mapped to.
   */
  private static final byte RTS_OP = Opcodes.RTS_IMP;



  /**
   * The ports array for an extension only contains a single port that forwards
   * read access to the actual extension
   */
  private Port[] _ports = new Port[]{ new Port( this, 0 ) };



  /**
   *
   */
  private final Memory _memory;



  /**
   * Create an instance.
   *
   * @param m The emulation's memory.
   */
  public Extension( Memory m )
  {
    _memory = m;
  }



  /**
   * Default implementation of write for an extension.  Just ignores the call.
   *
   * @see Addressable#write(int, byte)
   */
  public void write( int port, byte value )
  {
    ;
  }



  /**
   * Default read implementation.  Activates the <code>extensionCalled()</code> 
   * template method and returns an RTS opcode.
   *
   * @see Addressable#read(int)
   */
  public byte read( int port )
  {
    // Check if we should be safe for recursive port reads (e.g. a read on the
    // port inside the extensionCalled() method.

    // Activate the template method...
    extensionCalled( _memory );
    // ...and return RTS.
    return RTS_OP;
  }



  /**
   * Returns the memory this extension is installed in.
   *
   * @return The memory this extension is installed in.
   */
  public Memory getMemory()
  {
    return _memory;
  }



  /*
   * Inherit javadoc
   */
  public Port[] getPorts()
  {
    return _ports;
  }



  /**
   * An empty implementation of the reset method.
   *
   * @see de.michab.simulator.Chip#reset
   */
  public void reset()
  {
    return;
  }



  /**
   * This is the template method called on hit of the <code>Extension</code> in 
   * memory.  The passed <code>Memory</code> is the same that would be
   * returned by <code>getMemory()</code>.
   *
   * @param m A reference to the memory the extension is installed in.
   * @see Extension#getMemory()
   */
  protected abstract void extensionCalled( Memory m );



  /**
   * Returns this <code>Extension</code>'s base address.
   *
   * @return This <code>Extension</code>'s base address.
   */
  public abstract int getBaseAddress();
}
