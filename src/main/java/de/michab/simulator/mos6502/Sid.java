/* $Id: Sid.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.michab.simulator.*;



/**
 * <p>The MOS 6581 Sound Interface Device.</p>
 *
 * @see de.michab.simulator.mos6502.Voice
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
public final class Sid 
  extends 
    DefaultChip
{
  private static Logger log = 
    Logger.getLogger( Sid.class.getName() );

  /**
   *
   */
  static final private int NUM_OF_REGS = 28;



  /**
   * The chip's registers.
   */
  private int[] _registers = new int[NUM_OF_REGS];



  /**
   * The chip's ports.
   */
  private final Port[] _ports;



  /**
   * The voices.
   */
  private final Voice[] _voices = new Voice[ 3 ];



  /**
   * If this flag is false sound is not played.
   */
  private boolean _soundOn = true;



  /**
   * If this flag is false sound is not played.
   */
  private boolean _error = false;



  /**
   * Create an instance.
   */
  public Sid()
  {
    _ports = createPorts( _registers.length );

    try
    {
      _voices[0] = new Voice( _registers, 0 );
      _voices[1] = new Voice( _registers, 7 );
      _voices[2] = new Voice( _registers, 14 );
      _voices[0].setNext( _voices[2] );
      _voices[1].setNext( _voices[0] );
      _voices[2].setNext( _voices[1] );
    }
    catch ( Exception e )
    {
      log.log( Level.SEVERE, "Error while initializing SID chip.", e );
      _error = true;
    }
  }



  /*
   * Inherit javadoc.
   */
  public synchronized byte read( int portId )
  {
    int result;

    switch ( portId )
    {
      // Offers a random value from noise generator 3.  The current
      // implementation simply returns random values.
      case 27:
        result = (int)(Math.random() * 256.0);
        break;

      // AD converter registers.  Currently a dummy implementation, in a real
      // 64 this allowed to use paddles.
      case 25:
      case 26:
        // Fall through...

      // Returns the current relative volume of voice 3.  Whatever that means.
      // The implementation returns the contents that has been written into
      // the register before.
      case 28:
        result = _registers[ portId ];
        break;

      // All the other registers are write only.  Reads result in 0.
      // TODO check what really is returned if a SID write-only-register is
      // read.
      default:
        result = 0;
    }

    if ( log.isLoggable( Level.FINE ) )
      log.fine( "Sid: read: " + portId + " = " + result );

    return (byte)result;
  }



  /*
   * Inherit javadoc.
   */
  public synchronized void write( int portId, byte value )
  {
    // Mask only the lower 8 bit.
    int data = value & 0xff;

    if ( log.isLoggable( Level.FINE ) )
      log.fine( "Sid: write: " + portId + " = " + data );

    if ( ! isSoundOn() )
      return;

    switch ( portId )
    {
      case 0x4:
      {
        _voices[0].updateVoice( data );
        break;
      } 
      case 0xb:
      {
        _voices[1].updateVoice( data );
        break;
      }
      case 0x12:
      {
        _voices[2].updateVoice( data );
        break;
      }

      // Volume.
      case 0x18:
      {
        _voices[0].setVolume(data & 0xf);
        _voices[1].setVolume(data & 0xf);
        _voices[2].setVolume(data & 0xf);
        break;
      }

      default:
        _registers[ portId ] = data;
        break;
    }

    updateSound();
  }



  /*
   * Inherit Javadoc.
   */
  public synchronized Port[] getPorts()
  {
    return _ports;
  }



  /*
   * Inherit Javadoc.
   */
  public void reset()
  {
  }



  /**
   * Check whether sound is active.
   *
   * @return <code>true</code> if the sound is switched on.
   * @see #setSoundOn(boolean)
   */
  public synchronized boolean isSoundOn()
  {
    return (! _error) && _soundOn;
  }



  /**
   * Activate or deactivate sound.
   *
   * @param what <code>True</code> if the sound is to be switched on,
   *        <code>false</code> to switch sound off.
   */
  public synchronized void setSoundOn( boolean what )
  {
    _soundOn = what;
  }



  /**
   * Simply propagate the sound update to the three voices.
   */
  private void updateSound()
  {
      _voices[0].updateSound();
      _voices[1].updateSound();
      _voices[2].updateSound();
  }
}
