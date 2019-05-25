/* $Id: Keyboard.java 272 2010-04-05 13:21:28Z Michael $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright © 2000-2010 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.michab.simulator.Bus;
import de.michab.simulator.Forwarder;
import de.michab.simulator.mos6502.Cpu6510;



/**
 * <p>Emulates the C64's keyboard.  This implementation is responsible for
 * handling the incoming key events from the user interface and mapping these
 * onto the states of the keyboard's IO lines.</p>
 *
 * <p>Information regarding the C64's keyboard layout and handling can be found
 * in <i>64 intern</i> pp. 318, the keyboard checking routine in the C64's ROM
 * starts at 0xea87.<p>
 *
 * <p>The following table describes the emulator's keyboard mapping.  Most of
 * the C64's keys do have a direct equivalent on a modern keyboard, these are
 * not described in the table below.  The table only describes the mappings
 * for keys that have no direct equivalent on a modern keyboard or are only
 * available on certain kinds of keyboards.</p>
 *
 * <TABLE BORDER="2"
 *        CELLPADDING="2"
 *        CELLSPACING="0"
 *        SUMMARY="The keyboard mapping">
 *    <TR>
 *          <TH>C64 Key</TH>
 *          <TH>Keyboard key</TH>
 *    </TR>
 *    <TR>
 *      <TD>Arrow Right</TD>
 *      <TD>Is represented as Unicode character 0x2190.  I never needed this
 *          character on the 64, so it is deliberately not mapped.  It can
 *          be generated though if you use OS specific mechanisms to directly
 *          enter the Unicode.</TD>
 *    </TR>
 *    <TR>
 *      <TD>Pound</TD>
 *      <TD>Is represented as Unicode character 0x00a3.  Directly available
 *          only on certain keyboards, like the English and Italian.</TD>
 *    </TR>
 *    <TR>
 *      <TD>RUN/STOP</TD>
 *      <TD>ESC key is stop, shift-ESC is run functionality</TD>
 *    </TR>
 *    <TR>
 *      <TD>Restore</TD>
 *      <TD>Break, remember the C64 key combination RUN/STOP with Restore, this
 *          results in a soft reset.</TD>
 *    </TR>
 *    <TR>
 *      <TD>Clear/Home</TD>
 *      <TD>Shift+Home / Home</TD>
 *    </TR>
 *    <TR>
 *      <TD>Commodore key</TD>
 *      <TD>Control</TD>
 *    </TR>
 * </TABLE>
 *
 * @version $Revision: 272 $
 * @author Michael G. Binz
 */
final class Keyboard
  implements
    KeyListener,
    Bus
{
    private final static Logger _log =
        Logger.getLogger( Keyboard.class.getName() );

    private final static Level _chipLogLevel =
        Level.FINE;

    private final static boolean _doLogging =
        _log.isLoggable( _chipLogLevel );



  /**
   * This is the local state of the bus.
   */
  private byte _busState;



  /**
   * A reference to the CPU.  Used only for creating a NMI on pressing of the
   * escape key (on the original keyboard this is 'Restore'.)  Note that while
   * this key is also part of the keyboard it has nothing to do with the
   * keyboard matrix, but is connected directly to the processor's NMI line.
   */
  private final Cpu6510 _cpu;



  private enum KeyboardStatus { MODE_RAW, MODE_COOKED };



  /**
   * <p>The current interpretation mode of the incoming key events.  By default
   * this is equal to <code>MODE_RAW</code>.  This means that the incoming
   * key pressed and released events are interpreted and forwarded to the
   * emulator.  In this mode the events are also always expected to pair, so
   * that a press of shift, followed by a press of the key 'A' has to be
   * followed by a release of the both keys to return in the start state.
   *
   * <p>If a key typed event is received and a mapping for this does exist in
   * the <code>_charToBusMap</code>, then the interpretation mode is switched
   * to <code>MODE_COOCKED</code>.  In this mode interpretation of the key
   * events is much simpler:  As soon as a key released event is received in
   * the cooked mode the bus lines for the 64 are cleared.  As soon as a key
   * pressed event is received, the mode is switched to raw again.</p>
   *
   * @see Keyboard#_charToBusMap
   * @see Keyboard#MODE_RAW
   * @see Keyboard#MODE_COOKED
   */
  private KeyboardStatus _status = KeyboardStatus.MODE_RAW;



  /**
   * This represents the key matrix.  Logically a 8*8 bit array.  Has to be
   * handled as a 64bit unsigned value.
   *
   * This matrix:
   *    B0 B1 etc...
   * A0 0  0  000000
   * A1 0  0  000000
   * A2 0  0  000000
   * A3 0  0  000000
   * A4 0  0  000000
   * A5 0  0  000000
   * A6 0  0  000000
   * A7 0  0  000000
   *
   * is mapped: A0/B0 LSB of long, A7/B7 MSB of long.
   */
  private long _keyboardMatrix = 0;



  /**
   * A keyboard is a special kind of bus.  This is the reference to the
   * listener on this bus.
   */
  private Forwarder _listener = null;



  /**
   * Create a keyboard instance.
   *
   * @param cpu The emulation's processor.  This is needed since the keyboard
   *        can trigger a NMI on the processor.
   */
  public Keyboard( Cpu6510 cpu )
  {
    _cpu = cpu;
  }



  /**
   * Set the bit corresponding to the given key in the keyboard matrix.
   *
   * @param k The key event.
   * @see KeyListener#keyPressed(java.awt.event.KeyEvent)
   */
  public void keyPressed( KeyEvent k )
  {
    if ( _doLogging )
    {
      _log.log( _chipLogLevel, "Pressed: " + k );
      showStatus();
    }
    if ( _status == KeyboardStatus.MODE_COOKED )
      _status = KeyboardStatus.MODE_RAW;

    int keyCode = k.getKeyCode();

    // If this is the key that represents the 64's 'Restore'...
    if ( keyCode == KeyEvent.VK_PAUSE )
    {
      _log.log( _chipLogLevel, "Keyboard NMI" );
      // ...send an NMI to the processor.
      _cpu.NMI();
      // ...and reset the keyboard matrix.  This is normally not needed, but
      // helps on some architectures to get into a defined state again.
      _keyboardMatrix = 0;
      return;
    }

    // Get the bit for the key that is pressed...
    long bitPressed = getKeysBitPattern( keyCode );
    // ...and set that bit in our keyboard matrix.
   _keyboardMatrix |= bitPressed;
  }



  /**
   * Clear the bit corresponding to the given key in the keyboard matrix.
   *
   * @param k The key event.
   * @see KeyListener#keyReleased(java.awt.event.KeyEvent)
   */
  public void keyReleased( KeyEvent k )
  {
    if ( _status == KeyboardStatus.MODE_COOKED )
    {
      _keyboardMatrix = 0;
    }
    else
    {
      // Get the bit for the key that is released...
      long bitPressed = getKeysBitPattern( k.getKeyCode() );
      // ...and clear that bit in our keyboard matrix.
      _keyboardMatrix &= (~bitPressed);
    }

    if ( _doLogging )
    {
      _log.log( _chipLogLevel, "Released: " + k );
      showStatus();
    }
  }



  /**
   * Checks for cooked keys.  This represents high level key handling.
   *
   * @param k The key event.
   * @see KeyListener#keyTyped(java.awt.event.KeyEvent)
   */
  public void keyTyped( KeyEvent k )
  {
    if ( _doLogging )
    {
      _log.log( _chipLogLevel, "Typed: " + k );
      showStatus();
    }
    char c = k.getKeyChar();
    // First check if the key is really defined.
    if ( c == KeyEvent.CHAR_UNDEFINED )
      return;
    // Now check whether we have a mapping for that guy.
    Long l = _charToBusMap.get( Character.valueOf( c ) );

    if ( l == null )
      return;
    // We have a mapping.  Switch to cooked mode...
    _status = KeyboardStatus.MODE_COOKED;
    // ...and set the key.
    _keyboardMatrix = l.longValue();
  }



  /**
   * Maps the key pressed and released key codes to the respective keyboard bus
   * settings.  This represents low level key decoding and is used only for
   * the character keys.
   *
   * @param keyCode The Java key code as defined by java.awt.event.KeyEvent.
   * @return The bit that represents the passed key in the 64s key matrix.  If
   *         the passed key code has no corresponding key on the 64s keyboard,
   *         then zero is returned.
   * @see Keyboard#keyPressed(KeyEvent)
   * @see Keyboard#keyReleased(KeyEvent)
   */
  private static final long getKeysBitPattern( int keyCode )
  {
    long result = 0;

    switch ( keyCode )
    {
      //
      // Input bus line 0
      //
      case KeyEvent.VK_BACK_SPACE:
        result = K_DELETE;
        break;

      case KeyEvent.VK_ENTER:
        result = K_ENTER;
        break;

      // Also see the custom mappings at the end of this switch.
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_KP_RIGHT:
        result = K_RIGHT;
        break;

      // Also see the custom mappings at the end of this switch.
      case KeyEvent.VK_F7:
        result = K_F7;
        break;

      // Also see the custom mappings at the end of this switch.
      case KeyEvent.VK_F1:
        result = K_F1;
        break;

      // Also see the custom mappings at the end of this switch.
      case KeyEvent.VK_F3:
        result = K_F3;
        break;

      // Also see the custom mappings at the end of this switch.
      case KeyEvent.VK_F5:
        result = K_F5;
        break;

      // Also see the custom mappings at the end of this switch.
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_KP_DOWN:
        result = K_DOWN;
        break;

      //
      // Input bus line 1
      //

//      case KeyEvent.VK_3:
//      case KeyEvent.VK_NUMPAD3:
//        result = K_3;
//        break;

      case KeyEvent.VK_W:
        result = K_W;
        break;

      case KeyEvent.VK_A:
        result = K_A;
        break;

//      case KeyEvent.VK_4:
//      case KeyEvent.VK_NUMPAD4:
//        result = K_4;
//        break;

      case KeyEvent.VK_Z:
        result = K_Z;
        break;

      case KeyEvent.VK_S:
        result = K_S;
        break;

      case KeyEvent.VK_E:
        result = K_E;
        break;

      case KeyEvent.VK_SHIFT:
        result = K_LSHIFT;
        break;

      //
      // Input bus line 2
      //

//      case KeyEvent.VK_5:
//      case KeyEvent.VK_NUMPAD5:
//        result = K_5;
//        break;

      case KeyEvent.VK_R:
        result = K_R;
        break;

      case KeyEvent.VK_D:
        result = K_D;
        break;

//      case KeyEvent.VK_6:
//      case KeyEvent.VK_NUMPAD6:
//        result = K_6;
//        break;

      case KeyEvent.VK_C:
        result = K_C;
        break;

      case KeyEvent.VK_F:
        result = K_F;
        break;

      case KeyEvent.VK_T:
        result = K_T;
        break;

      case KeyEvent.VK_X:
        result = K_X;
        break;

      //
      // Input bus line 3
      //

//      case KeyEvent.VK_7:
//      case KeyEvent.VK_NUMPAD7:
//        result = K_7;
//        break;

      case KeyEvent.VK_Y:
        result = K_Y;
        break;

      case KeyEvent.VK_G:
        result = K_G;
        break;

//      case KeyEvent.VK_8:
//      case KeyEvent.VK_NUMPAD8:
//        result = K_8;
//        break;

      case KeyEvent.VK_B:
        result = K_B;
        break;

      case KeyEvent.VK_H:
        result = K_H;
        break;

      case KeyEvent.VK_U:
        result = K_U;
        break;

      case KeyEvent.VK_V:
        result = K_V;
        break;

      //
      // Input bus line 4
      //

//      case KeyEvent.VK_9:
//      case KeyEvent.VK_NUMPAD9:
//        result = K_9;
//        break;

      case KeyEvent.VK_I:
        result = K_I;
        break;

      case KeyEvent.VK_J:
        result = K_J;
        break;

      // This is zero.
//      case KeyEvent.VK_0:
//      case KeyEvent.VK_NUMPAD0:
//        result = K_0;
//        break;

      case KeyEvent.VK_M:
        result = K_M;
        break;

      case KeyEvent.VK_K:
        result = K_K;
        break;

      // This is otto.
      case KeyEvent.VK_O:
        result = K_O;
        break;

      case KeyEvent.VK_N:
        result = K_N;
        break;

      //
      // Input bus line 5
      //

//      case KeyEvent.VK_PLUS:
//      case KeyEvent.VK_ADD:
//        result = K_PLUS;
//        break;

      case KeyEvent.VK_P:
        result = K_P;
        break;

      case KeyEvent.VK_L:
        result = K_L;
        break;

//      case KeyEvent.VK_MINUS:
//      case KeyEvent.VK_SUBTRACT:
//        result = K_MINUS;
//        break;

//      case KeyEvent.VK_PERIOD:
//      case KeyEvent.VK_DECIMAL:
//        result = K_PERIOD;
//        break;

//      case KeyEvent.VK_COLON:
//        result = K_COLON;
//        break;

//      case KeyEvent.VK_AT:
//        result = K_AT;
//        break;

//      case KeyEvent.VK_COMMA:
//        result = K_COMMA;
//        break;

      //
      // Input bus line 6
      //

      // Uh, a british pound key is pretty rare!
      // case KeyEvent.VK_POUND:
      //  result = getBitMaskFor( 6, 0 );
      //  break;

      // Asterisk is available from numeric keypad.
//      case KeyEvent.VK_MULTIPLY:
//      case KeyEvent.VK_ASTERISK:
//        result = K_ASTERISK;
//        break;

//      case KeyEvent.VK_SEMICOLON:
//        result = K_SEMICOLON;
//        break;

      case KeyEvent.VK_HOME:
        result = K_HOME;
        break;

      // Right shift:  Not mapped currently.
      // case KeyEvent.VK_SHIFT:
      //  result = getBitMaskFor( 6, 4 );
      //  break;

//      case KeyEvent.VK_EQUALS:
//        result = K_EQUALS;
//        break;

//      case KeyEvent.VK_CIRCUMFLEX:
//        result = K_ARROW_UP;
//        break;

//      case KeyEvent.VK_SLASH:
//      case KeyEvent.VK_DIVIDE:
//        result = K_SLASH;
//        break;

      //
      // Input bus line 7
      //

//      case KeyEvent.VK_1:
//      case KeyEvent.VK_NUMPAD1:
//        result = K_1;
//        break;

//      case KeyEvent.VK_GREATER:
//        result = K_GREATER;
//        break;

      // This is the CNTRL key.  Mapped to tab.
      case KeyEvent.VK_TAB:
        result = K_CTRL;
        break;

//      case KeyEvent.VK_2:
//      case KeyEvent.VK_NUMPAD2:
//        result = K_2;
//        break;

      case KeyEvent.VK_SPACE:
        result = K_SPACE;
        break;

      // This is the Commodore key.  Mapped to the control key.
      case KeyEvent.VK_CONTROL:
        result = K_COMMODORE;
        break;

      case KeyEvent.VK_Q:
        result = K_Q;
        break;

      // Run/Stop is mapped to ESC.
      case KeyEvent.VK_ESCAPE: // 'RUN/STOP'
        result = K_STOP;
        break;

      //
      // This is handling for custom keys -- i.e. keys that have no direct
      // equivalent on the 64s keyboard but are very handy.  We use always the
      // right-shift-key bit pattern since this is and can not be used in the
      // other switches.  (AWT doesn't support left/right shift -- only shift.)
      //
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_KP_LEFT:
        result = K_LEFT;
        break;

      case KeyEvent.VK_UP:
      case KeyEvent.VK_KP_UP:
        result = K_UP;
        break;

      case KeyEvent.VK_INSERT:
        result = K_INSERT;
        break;

      case KeyEvent.VK_F2:
        result = K_F2;
        break;

      case KeyEvent.VK_F4:
        result = K_F4;
        break;

      case KeyEvent.VK_F6:
        result = K_F6;
        break;

      case KeyEvent.VK_F8:
        result = K_F8;
        break;
    }

    return result;
  }



  /**
   * This implements the mapping for the keyboard matrix.
   *
   * @param y The key's y position on the keyboard matrix.
   * @param x The key's x position on the keyboard matrix.
   * @return The bit mask for the passed position.
   */
  private static final long getBitMaskFor( int y, int x )
  {
    int bitPosition = (8 * x) + y;

    return 1L << bitPosition;
  }



  /**
   * Add a listener to this bus.
   *
   * @param listener The listener to add.
   */
  public void setListener( Forwarder listener )
  {
    _listener = listener;
  }



  /**
   * Write an input byte onto the keyboard's key matrix.  This will result in
   * a byte written to the keyboard's output, where all bits are set where
   * currently a key is pressed.
   *
   * @param inputLines A byte holding the values of the eight input lines in
   *        its bits.
   */
  public void write( byte inputLines )
  {
    byte result = 0;

    if ( _keyboardMatrix != 0 )
    {
      // Build the AND mask for the complete keyboard test.  This is 8 times
      // the input bit pattern in a long.  E.g. if the input byte was 0x02 then
      // we build a long mask of 0x0202020202020202.
      // Note that we have to first invert the incoming bits since we are working
      // with negative logic -- the CIA chip we are cooperating with is low-
      // active.
      long mask = ~inputLines & 0xff;
      mask |= (mask << 8);
      mask |= (mask << 16);
      mask |= (mask << 32);

      // Now we do really the test of the keyboard matrix.  Since we have heavy
      // pre and post processing the actual test is quite simple.
      mask &= _keyboardMatrix;

      // Decompose the result into our output byte.
      for ( int bit = 1 ; bit <= (1<<7) ; bit <<= 1, mask >>>=8 )
      {
        if ( (mask & 0xff) != 0 )
          result |= bit;
      }
    }

    // Remember the value.
    _busState = result;


    // Note that we have to invert the bits here again.  This is symmetric
    // to the inversion of the incoming value above.
    result = (byte)~result;

    if ( _doLogging )
      _log.log(
          _chipLogLevel,
          "Keyboard : write : " + Integer.toHexString( result & 0xff ) );

    _listener.write( result );
  }



  /**
   * Read from the Bus.  In the 64 this is called, if a program reads register
   * CIA-1 PRA, which is not commonly done.
   *
   * @see Forwarder#write(byte)
   */
  public byte read()
  {
    byte result = _listener.read();

    if ( _doLogging )
      _log.log(
          _chipLogLevel,
          "Keyboard : write : " + Integer.toHexString( result & 0xff ) );

    return result;
  }



  /**
   * The container holding the mappings for characters to IO lines.
   */
  private static final HashMap<Character, Long> _charToBusMap =
	  new HashMap<Character, Long>();



  // Here come the canonical C64's key definitions.  The keys are ordered by
  // their position on the 64's keyboard, beginning with the top row.
  // Row 1
  private static final long K_ARROW_LEFT =
    getBitMaskFor( 7, 1 );
  private static final long K_1 =
    getBitMaskFor( 7, 0 );
  private static final long K_2 =
    getBitMaskFor( 7, 3 );
  private static final long K_3 =
    getBitMaskFor( 1, 0 );
  private static final long K_4 =
    getBitMaskFor( 1, 3 );
  private static final long K_5 =
    getBitMaskFor( 2, 0 );
  private static final long K_6 =
    getBitMaskFor( 2, 3 );
  private static final long K_7 =
    getBitMaskFor( 3, 0 );
  private static final long K_8 =
    getBitMaskFor( 3, 3 );
  private static final long K_9 =
    getBitMaskFor( 4, 0 );
  private static final long K_0 =
    getBitMaskFor( 4, 3 );
  private static final long K_PLUS =
    getBitMaskFor( 5, 0 );
  private static final long K_MINUS =
    getBitMaskFor( 5, 3 );
  private static final long K_POUND =
    getBitMaskFor( 6, 0 );
  private static final long K_HOME =
    getBitMaskFor( 6, 3 );
  private static final long K_DELETE =
    getBitMaskFor( 0, 0 );
  private static final long K_F1 =
    getBitMaskFor( 0, 4 );
  // Row 2
  private static final long K_CTRL =
    getBitMaskFor( 7, 2 );
  private static final long K_Q =
    getBitMaskFor( 7, 6 );
  private static final long K_W =
    getBitMaskFor( 1, 1 );
  private static final long K_E =
    getBitMaskFor( 1, 6 );
  private static final long K_R =
    getBitMaskFor( 2, 1 );
  private static final long K_T =
    getBitMaskFor( 2, 6 );
  private static final long K_Y =
    getBitMaskFor( 3, 1 );
  private static final long K_U =
    getBitMaskFor( 3, 6 );
  private static final long K_I =
    getBitMaskFor( 4, 1 );
  private static final long K_O =
    getBitMaskFor( 4, 6 );
  private static final long K_P =
    getBitMaskFor( 5, 1 );
  private static final long K_AT =
    getBitMaskFor( 5, 6 );
  private static final long K_ASTERISK =
    getBitMaskFor( 6, 1 );
  private static final long K_ARROW_UP =
    getBitMaskFor( 6, 6 );
  // The restore key would go here, but is not part of the keyboard matrix.
  private static final long K_F3 =
    getBitMaskFor( 0, 5 );
  // Row 3
  private static final long K_STOP =
    getBitMaskFor( 7, 7 );
  // Shift-lock would go here, but is not part of the keyboard matrix.
  private static final long K_A =
    getBitMaskFor( 1, 2 );
  private static final long K_S =
    getBitMaskFor( 1, 5 );
  private static final long K_D =
    getBitMaskFor( 2, 2 );
  private static final long K_F =
    getBitMaskFor( 2, 5 );
  private static final long K_G =
    getBitMaskFor( 3, 2 );
  private static final long K_H =
    getBitMaskFor( 3, 5 );
  private static final long K_J =
    getBitMaskFor( 4, 2 );
  private static final long K_K =
    getBitMaskFor( 4, 5 );
  private static final long K_L =
    getBitMaskFor( 5, 2 );
  private static final long K_COLON =
    getBitMaskFor( 5, 5 );
  private static final long K_SEMICOLON =
    getBitMaskFor( 6, 2 );
  private static final long K_EQUALS =
    getBitMaskFor( 6, 5 );
  private static final long K_ENTER =
    getBitMaskFor( 0, 1 );
  private static final long K_F5 =
    getBitMaskFor( 0, 6 );
  // Row 4
  private static final long K_COMMODORE =
    getBitMaskFor( 7, 5 );
  private static final long K_LSHIFT =
    getBitMaskFor( 1, 7 );
  private static final long K_Z =
    getBitMaskFor( 1, 4 );
  private static final long K_X =
    getBitMaskFor( 2, 7 );
  private static final long K_C =
    getBitMaskFor( 2, 4 );
  private static final long K_V =
    getBitMaskFor( 3, 7 );
  private static final long K_B =
    getBitMaskFor( 3, 4 );
  private static final long K_N =
    getBitMaskFor( 4, 7 );
  private static final long K_M =
    getBitMaskFor( 4, 4 );
  private static final long K_COMMA =
    getBitMaskFor( 5, 7 );
  private static final long K_PERIOD =
    getBitMaskFor( 5, 4 );
  private static final long K_SLASH =
    getBitMaskFor( 6, 7 );
  private final static long K_RSHIFT =
    getBitMaskFor( 6, 4 );
  private static final long K_DOWN =
    getBitMaskFor( 0, 7 );
  private static final long K_RIGHT =
    getBitMaskFor( 0, 2 );
  private static final long K_F7 =
    getBitMaskFor( 0, 3 );
  // Row 5
  private static final long K_SPACE =
    getBitMaskFor( 7, 4 );

  // Row 1 shift
  private static final long K_EXCLAMATION =
    K_1 | K_RSHIFT;
  private static final long K_DQUOTE =
    K_2 | K_RSHIFT;
  private static final long K_NUMBER =
    K_3 | K_RSHIFT;
  private static final long K_DOLLAR =
    K_4 | K_RSHIFT;
  private static final long K_PERCENT =
    K_5 | K_RSHIFT;
  private static final long K_AMPERSAND =
    K_6 | K_RSHIFT;
  private static final long K_QUOTE =
    K_7 | K_RSHIFT;
  private static final long K_LBRACE =
    K_8 | K_RSHIFT;
  private static final long K_RBRACE =
    K_9 | K_RSHIFT;
  // Mapped to Shift+Home, so we need no special definition for K_CLEAR.
  // private static final long K_CLEAR = K_HOME | K_RSHIFT;
  private static final long K_INSERT =
    K_DELETE | K_RSHIFT;
  private static final long K_F2 =
    K_F1 | K_RSHIFT;
  // Row 2 shift
  private static final long K_F4 =
    K_F3 | K_RSHIFT;
  // Row 3 shift
  private static final long K_LBRACKET =
    K_COLON | K_RSHIFT;
  private static final long K_RBRACKET =
    K_SEMICOLON | K_RSHIFT;
  private static final long K_F6 =
    K_F5 | K_RSHIFT;
  // Row 4 shift
  private static final long K_LESS =
    K_COMMA | K_RSHIFT;
  private static final long K_GREATER =
    K_PERIOD | K_RSHIFT;
  private static final long K_QUESTIONMARK =
    K_SLASH | K_RSHIFT;
  private static final long K_UP =
    K_DOWN | K_RSHIFT;
  private static final long K_LEFT =
    K_RIGHT | K_RSHIFT;
  private static final long K_F8 =
    K_F7 | K_RSHIFT;
  // Row 5 shift



  static
  {
    // Map the keys that have a keyboard equivalent and a key typed code.  This
    // allows us to support all the local keyboards supported by java.

    // Row 1
    // Left arrow, only available on Marsian keyboards.
    addMapping( '\u2190', K_ARROW_LEFT );
    addMapping( '1', K_1 );
    addMapping( '2', K_2 );
    addMapping( '3', K_3 );
    addMapping( '4', K_4 );
    addMapping( '5', K_5 );
    addMapping( '6', K_6 );
    addMapping( '7', K_7 );
    addMapping( '8', K_8 );
    addMapping( '9', K_9 );
    addMapping( '0', K_0 );
    addMapping( '+', K_PLUS );
    addMapping( '-', K_MINUS );
    // British pound
    addMapping( '\u00a3', K_POUND );
    // Row 2
    addMapping( '@', K_AT );
    addMapping( '*', K_ASTERISK );
    addMapping( '^', K_ARROW_UP );
    // Row 3
    addMapping( ':', K_COLON );
    addMapping( ';', K_SEMICOLON );
    addMapping( '=', K_EQUALS );
    // Row 4
    addMapping( ',', K_COMMA );
    addMapping( '.', K_PERIOD );
    addMapping( '/', K_SLASH );
    // Row 5
//    addMapping( ' ', K_SPACE );

    // Row 1 shift
    addMapping( '!', K_EXCLAMATION );
    addMapping( '"', K_DQUOTE );
    addMapping( '#', K_NUMBER );
    addMapping( '$', K_DOLLAR );
    addMapping( '%', K_PERCENT );
    addMapping( '&', K_AMPERSAND );
    addMapping( '\'', K_QUOTE );
    addMapping( '(', K_LBRACE );
    addMapping( ')', K_RBRACE );
    // Row 2 shift
    // Row 3 shift
    addMapping( '[', K_LBRACKET );
    addMapping( ']', K_RBRACKET );
    // Row 4 shift
    addMapping( '<', K_LESS );
    addMapping( '>', K_GREATER );
    addMapping( '?', K_QUESTIONMARK );
  }



  /**
   * Add a mapping for the passed parameters.  Used for initialization of the
   * <code>_charToBusMap</code>.
   *
   * @param c The character to be mapped.
   * @param busValue The vale to put onto the bus for the character.
   * @see Keyboard#_charToBusMap
   */
  private static void addMapping( char c, long busValue )
  {
    _charToBusMap.put( c, busValue );
  }



  /**
   *
   */
  private void showStatus()
  {
    _log.log( _chipLogLevel,
      "Keyboard status: " +
      _status +
      " " +
      _busState +
      " " +
      _keyboardMatrix );
  }
}
