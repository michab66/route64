/* $Id: SystemInput.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;

import de.michab.simulator.*;
import de.michab.simulator.mos6502.*;



/**
 * <p>This class is responsible for writing into the 64's keyboard input 
 * buffer. Ensures that the simulation is in a shape that makes it possible to 
 * really process the input i.e. waits until the simulation is running before
 * inserting the characters.</p>
 *
 * @author Michael G. Binz
 */
class SystemInput
  extends
    Extension
{
  
  /**
   * Zero page entry holding the number of key that are waiting in the 64's
   * input key buffer.
   */
  private static final int ZP_NUMKEYS = 198;



  /**
   * The input buffer size.
   */
  private final static int KEY_BUFFER_SIZE = 10;



  /**
   * The address of the 64's input key buffer.  This contains the keys waiting
   * for being processed by the 64's ROM.  {@link #ZP_NUMKEYS ZP_NUMKEYS}
   * holds the number of valid characters in the key buffer.
   *
   * @see #KEY_BUFFER_SIZE
   * @see #ZP_NUMKEYS
   */
  private final static int KEY_BUFFER = 631;



  /**
   * A buffer containing the characters to write.
   */
  private byte[] _buffer;



  /**
   * The next character to write from _buffer;
   */
  private int _currentBufferIndex;



  /**
   * Creates an instance.
   * 
   * @param m The memory this instance refers to.
   */
  SystemInput( Memory m )
  {
    super( m );
  }



  /*
   * Parent javadoc.
   */
  public int getBaseAddress()
  {
    return 0xe5cf;
  }



  /*
   * Parent javadoc.
   */
  public byte read( int port )
  {
    Memory m = getMemory();

    // Triggers extensionCalled().  But this is a secret.
    extensionCalled( m );
    return (byte)0x85;
  }



  /*
   * Inherit javadoc.
   */
  public void extensionCalled( Memory m )
  {
    // If the buffer is empty -- leave.
    if ( null == _buffer )
      return;

    byte[] rawMemory = getMemory().getRawMemory();

    // Check if there are characters in the 64's input key buffer.  If there
    // are characters we simply return, since we want to the emulation to
    // process the waiting keys before we add new keys.
    if ( rawMemory[ ZP_NUMKEYS ] != 0 )
      return;

    int numToWrite = _buffer.length - _currentBufferIndex;
    if ( numToWrite > KEY_BUFFER_SIZE )
      numToWrite = KEY_BUFFER_SIZE;

    System.arraycopy( _buffer,
                      _currentBufferIndex,
                      rawMemory, KEY_BUFFER, numToWrite );
    rawMemory[ZP_NUMKEYS]=(byte)numToWrite;

    _currentBufferIndex += numToWrite;
    if ( _currentBufferIndex > _buffer.length )
      _buffer = null;
  }



  /**
   * Accepts a string that is written into the 64's input buffer for
   * evaluation.
   */
  public void writeInput( byte[] toWrite )
  {
    _currentBufferIndex = 0;
    _buffer = toWrite;
  }
}
