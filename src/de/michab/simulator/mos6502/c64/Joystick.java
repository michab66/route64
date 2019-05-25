/* $Id: Joystick.java 615 2012-10-28 14:01:09Z Michael $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright © 2000-2010 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;

import de.michab.simulator.Bus;
import de.michab.simulator.Forwarder;
import de.michab.simulator.Processor;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Implements a Joystick.  Cursor keys select the directions, space is the
 * joystick button.
 *
 * @version $Revision: 615 $
 * @author Michael G. Binz
 */
final class Joystick
  implements
    KeyListener,
    Bus
{
    private final static Logger _log =
        Logger.getLogger( Joystick.class.getName() );

    private final static Level _chipLogLevel =
        Level.FINE;

    private final boolean _doLogging =
        _log.isLoggable( _chipLogLevel );

    /**
     * The name of the CIA for logging purposes.
     */
    private final String _logPrefix;



  /**
   * This is the next one in the chain of responsibility of handling key
   * events, i.e. all key events that aren't handled by the Joystick are
   * passed on to that sublistener.
   */
//  private final KeyListener _subKeyListener;



  /**
   * The raw registers of the CIA we are connected to.
   * TODO make final and required, since never dynamically changed.
   * TODO currently we could need synchronisation on the outport, since
   * it may be null.
   */
  private Forwarder _outPort;




  /**
   * Create an instance using the passed listener as the target for key events
   * not consumed by the joystick.
   *
   * @param subListener A listener receiving all events that are not consumed
   *        by the new <code>Joystick</code> instance.
   * @param num The number of the Joystick for log purposes.
   */
  public Joystick( KeyListener subListener, int num )
  {
    _logPrefix = "Joystick-" + num;

//    _subKeyListener = subListener;
  }



  /**
   * This component's key listener.  Responsible for handling the cursor keys
   * without mouse interaction.
   *
   * @param k The key event.
   * @param isPressed <code>true</code> if the key was pressed, otherwise
   *        the key was released.
   * @see java.awt.Component#processKeyEvent
   */
  private void handleKeyEvent( KeyEvent k, boolean isPressed )
  {
    // Check if this is one of the keys we handle...
    switch ( k.getKeyCode() )
    {
      case KeyEvent.VK_UP:
        handleUp( isPressed );
        break;

      case KeyEvent.VK_DOWN:
        handleDown( isPressed );
        break;

      case KeyEvent.VK_LEFT:
        handleLeft( isPressed );
        break;

      case KeyEvent.VK_RIGHT:
        handleRight( isPressed );
        break;

      case KeyEvent.VK_SPACE:
        handleButton( isPressed );
        break;

      // ...and forward the event to the next handler in the chain if nobody
      // feels responsible for it.
/*      default:
        if ( _subKeyListener != null )
        {
          if ( isPressed )
            _subKeyListener.keyPressed( k );
          else
            _subKeyListener.keyReleased( k );
        }
        break; */
    }
  }



  /**
   * Eventually handles joystick up signals.
   *
   * @param pressed <code>true</code> if the key was pressed, otherwise
   *        the key was released.
   */
  private void handleUp( boolean pressed )
  {
      if ( _doLogging )
          log( pressed, "Up" );

    setRegisterBit( pressed, Processor.BIT_0 );
  }



  /**
   * Eventually handles joystick down signals.
   *
   * @param pressed <code>true</code> if the key was pressed, otherwise
   *        the key was released.
   */
  private void handleDown( boolean pressed )
  {
      if ( _doLogging )
          log( pressed, "Down" );

    setRegisterBit( pressed, Processor.BIT_1 );
  }



  /**
   * Eventually handles joystick left signals.
   *
   * @param pressed <code>true</code> if the key was pressed, otherwise
   *        the key was released.
   */
  private void handleLeft( boolean pressed )
  {
      if ( _doLogging )
          log( pressed, "Left" );

    setRegisterBit( pressed, Processor.BIT_2 );
  }



  /**
   * Eventually handles joystick right signals.
   *
   * @param pressed <code>true</code> if the key was pressed, otherwise
   *        the key was released.
   */
  private void handleRight( boolean pressed )
  {
      if ( _doLogging )
          log( pressed, "Right" );

    setRegisterBit( pressed, Processor.BIT_3 );
  }



  /**
   * Eventually handles joystick button signals.
   *
   * @param pressed <code>true</code> if the key was pressed, otherwise
   *        the key was released.
   */
  private void handleButton( boolean pressed )
  {
    if ( _doLogging )
      log( pressed, "Button" );

    setRegisterBit( pressed, Processor.BIT_4 );
  }



  private int _joystickValue = 0;


  /**
   * Finally set the register bit.
   *
   * @param pressed <code>true</code> if the key was pressed, otherwise
   *        the key was released.
   * @param theBit The bit to set.
   */
  private void setRegisterBit( boolean pressed, int theBit )
  {
    int value = _joystickValue; //_ciaRegisters[ _registerIdx ];

    if ( _doLogging )
      _log.fine( _logPrefix + "== $" + Integer.toHexString( value ) );

    if ( pressed )
        value |= theBit;
    else
        value &= ~theBit;

    if ( _doLogging )
      _log.fine( _logPrefix + ":= $" + Integer.toHexString( value ) );

    _joystickValue = value;
  }



  /*
   * java.awt.event.KeyListener#keyTyped
   */
  public void keyTyped( KeyEvent k )
  {
    // TODO forward only keys that we do not handle.
    // _subKeyListener.keyTyped(k);
  }



  /*
   * java.awt.event.KeyListener#keyPressed
   */
  public void keyPressed( KeyEvent k )
  {
    handleKeyEvent( k, true );
  }



  /*
   * java.awt.event.KeyListener#keyReleased
   */
  public void keyReleased( KeyEvent k )
  {
    handleKeyEvent( k, false );
  }



  /* (non-Javadoc)
   * @see de.michab.simulator.Bus#setListener(de.michab.simulator.Forwarder)
   */
  @Override
  public void setListener( Forwarder listener )
  {
    _outPort = listener;
  }



  /* (non-Javadoc)
   * @see de.michab.simulator.Forwarder#read()
   */
  @Override
  public byte read()
  {
    byte result = _outPort.read();
    if ( _doLogging )
      _log.fine( _logPrefix + ": read : " + Integer.toHexString( result & 0xff ) );

    return (byte)~_joystickValue;
  }



  /* (non-Javadoc)
   * @see de.michab.simulator.Forwarder#write(byte)
   */
  @Override
  public void write( byte inputLines )
  {
    inputLines = (byte)~inputLines;

    if ( _outPort == null )
      return;

    byte result = (byte)~( _joystickValue & inputLines );

    if ( _doLogging )
      _log.fine( _logPrefix + ": write : " + Integer.toHexString( result & 0xff ) );

    _outPort.write( result );
  }



  /**
   *
   * @param pressed
   * @param key
   */
  private void log( boolean pressed, String key )
  {
    if ( ! _log.isLoggable( _chipLogLevel ) )
        return;

    _log.log(
        _chipLogLevel,
        key +
        " : " +
        (pressed ? "pressed" : "released") );
  }
}
