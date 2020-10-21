/* $Id: Cia.java 272 2010-04-05 13:21:28Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright © 2000-2010 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.michab.simulator.ArrayPort;
import de.michab.simulator.DefaultChip;
import de.michab.simulator.Forwarder;
import de.michab.simulator.Port;
import de.michab.simulator.Processor;



/**
 * Represents an instance of the 64's 6526 <i>C</i>omplex <i>I</i>nput/output
 * <i>A</i>dapter.  Represents a facade to the classes implementing the
 * functional components of the chip and is responsible for dispatching the
 * values written into or read from this chip's registers.
 *
 * @version $Revision: 272 $
 * @author Michael G. Binz
 */
public final class Cia
  extends
    DefaultChip
{
  private final static Logger _log =
      Logger.getLogger( Cia.class.getName() );

  /**
   * This chip's name for debug and logging purposes.
   */
  private final String _name;

  // TODO needs additional interfaces for HW equivalents like pins/busses to be
  // able to circumvent the register writes with additional logic when actually
  // reading/writing hardware lines/pins.
  private static final int PRA = 0;
  private static final int PRB = 1;
  private static final int DDRA = 2;
  private static final int DDRB = 3;
  private static final int TALO = 4;
  private static final int TAHI = 5;
  private static final int TBLO = 6;
  private static final int TBHI = 7;
  private static final int TOD10THS = 8;
  private static final int TODSEC = 9;
  private static final int TODMIN = 10;
  private static final int TODHR = 11;
  private static final int SDR = 12;
  private static final int ICR = 13;
  private static final int CRA =  14;
  private static final int CRB = 15;

  private static final String[] _registerNames =
  {
      "PRA",
      "PRB",
      "DDRA",
      "DDRB",
      "TALO",
      "TAHI",
      "TBLO",
      "TBHI",
      "TOD10THS",
      "TODSEC",
      "TODMIN",
      "TODHR",
      "SDR",
      "ICR",
      "CRA",
      "CRB"
  };


  /**
   * This is used to generate a unique id per CIA instance.
   */
  private static int _ciaCount = 0;



  /**
   * The id of this CIA.  Used for debug and logging purposes.
   */
  //private int _ciaNumber = ;



  /**
   * This CIA's timer A.
   */
  private final Timer _timerA;



  /**
   * This CIA's timer B.
   */
  private final Timer _timerB;



  /**
   * This CIA's clock
   */
  private final Clock _clock;


  private final Forwarder[] _ioPort = new Forwarder[2];
  private static final int A=0;
  private static final int B=1;

  /**
   * This CIA's interrupt mask.
   */
  private byte _interruptData = 0;



  /**
   * A reference to our cpu for interrupt purposes.  TODO This has to change to be
   * able to connect a CIA to IRQ or NMI as done in the 64.
   */
  private final Cpu6510 _cpu;



  /**
   * The number of registers of this chip.
   */
  static final private int NUM_OF_REGS = 16;



  /**
   * This CIA's internal registers.  These are accessible from others via the
   * ports defined below.
   */
  private byte[] _registers = new byte[NUM_OF_REGS];



  /**
   * This CIA's port array.
   */
  private final Port[] _ports;


  private final Forwarder[] _forwarders;


  /**
   * Creates a CIA.  The passed CPU reference is used for interrupt purposes.
   *
   * @param cpu The CPU receiving the interrupts.
   */
  public Cia( Cpu6510 cpu, de.michab.simulator.Clock clock )
  {
    _name = "CIA-" + (++_ciaCount);

    // Save the CPU reference.
    _cpu = cpu;

    // Create our Timers.
    // TODO check which one counts the underflows of the other.
    _timerA = new Timer(
        this,
        null,
        clock,
        _name + "TA" );
    _timerB = new Timer(
        this,
        null,
        clock,
        _name + "TB" );

    // Create our real time clock.
    _clock = new Clock( this, clock, _name + "C" );

    // Create this CIA's ports.
    _ports = createPorts( _registers.length );

    _forwarders = ArrayPort.createForwarders( _registers );
  }



  /**
   * Connects IO port A to a bus that is written on each internal write
   * operation on that port.  Only output lines are written.
   */
  public void connectPortA( Forwarder listener )
  {
    _ioPort[A] = listener;
  }



  /**
   * Connects IO port B to a bus that is written on each internal write
   * operation on that port.  Only output lines are written.
   */
  public void connectPortB( Forwarder listener )
  {
    _ioPort[B] = listener;
  }



  /**
   * Get a writable reference to port A.  The returned forwarder can be
   * connected to bus systems.
   */
  public Forwarder getInputPortA()
  {
    return _forwarders[ PRA ];
  }



  /**
   * Get a writable reference to port B.  The returned forwarder can be
   * connected to bus systems.
   */
  public Forwarder getInputPortB()
  {
    return _forwarders[ PRB ];
  }



  /**
   * The central read entry.  Called on each write to one of the internal
   * registers (Ports).
   */
  public synchronized byte read( int portId )
  {
    // Initialize our default result from our registers.
    int result = _registers[ portId ];

    switch ( portId )
    {
      // The set of registers handled by the default above.
      case PRA:
        if ( null != _ioPort[A] )
          result = _ioPort[A].read();
        break;
      // PRB -- Port register B.  State of pins PB0-7.  R:
      case PRB:
      {
        if ( null != _ioPort[B] )
          result = _ioPort[B].read();
        break;
// This is the right code, but is not working currently:
//        int DDR_OFFSET = DDRA-PRA;
//        result =
//          _registers[portId] | ~_registers[portId+DDR_OFFSET];
//        break;
      }

      // DDRA -- Data direction register port A.  R:
// End 11
      case DDRA:
      case DDRB:
      case SDR:
      case CRA:
      case CRB:
        break;

      // TA-LO -- Timer A - low byte.  R:
      case TALO:
        result = _timerA.getCurrentValueLo();
        break;

      // TA-HI -- Timer A - hi byte.  R:
      case TAHI:
        result = _timerA.getCurrentValueHi();
        break;

      // TB-LO -- Timer B - low byte.  R:
      case TBLO:
        result = _timerB.getCurrentValueLo();
        break;

      // TB-HI -- Timer B - hi byte.  R:
      case TBHI:
        result = _timerB.getCurrentValueHi();
        break;

      // TOD 10ths secs -- Clock 10ths secs in BCD.  R:
      case TOD10THS:
        result = integerToBcd( _clock.getTenthSeconds() );
        break;

      // TOD secs -- Clock secs in BCD.  R:
      case TODSEC:
        result = integerToBcd( _clock.getSeconds() );
        break;

      // TOD min -- Clock mins in BCD.  R:
      case TODMIN:
        result = integerToBcd( _clock.getMinutes() );
        break;

      // TOD hour -- Clock hours in BCD.  R:
      case TODHR:
      {
        int hours = _clock.getHours();
        boolean ten = false;
        boolean pm = false;

        if ( hours > 12 )
        {
          hours -= 12;
          pm = true;
        }
        if ( hours > 10 )
        {
          hours -= 10;
          ten = true;
        }
        //
        result = integerToBcd( hours );
        if ( pm )
          result |= Processor.BIT_7;
        if ( ten )
          result |= Processor.BIT_4;

        break;
      }

      // ICR -- Interrupt control register.  R:
      case ICR:
        result = icrRead();
        break;

      default:
        _log.severe( _name + ": invalid port read: " + portId );
        System.exit( 1 );
    }

    if ( _doLogging )
        logRead( portId, result );

    return (byte)result;
  }



  /**
   * This is the common write entry.
   *
   * @param portId The port to write.
   * @param value The value to write.
   */
  public synchronized void write( int portId, byte value )
  {
    if ( _doLogging )
        logWrite( portId, value );

    //_registers[ portId ] = value;

    switch ( portId )
    {
      // PRA -- Port register A.  State of pins PA0-7.  W:
      case PRA:
        _registers[ portId ] = value;
        if ( null != _ioPort[A] )
          _ioPort[A].write( value );
        break;

      // PRB -- Port register B.  State of pins PB0-7.  W:
      case PRB:
        _registers[ portId ] = value;
        if ( null != _ioPort[B] )
          _ioPort[B].write( value );
        break;

      // The registers that rely fully on the default handling.
      case DDRA:
      case DDRB:
      case SDR:
      case CRB:
        _registers[ portId ] = value;
        break;

      // TA-LO -- Timer A - low byte.  W: new value.
      case TALO:
        _timerA.setStartValueLo( value );
        break;

      // TA-HI -- Timer A - hi byte.  W: new value.
      case TAHI:
        _timerA.setStartValueHi( value );
        break;

      // TB-LO -- Timer B - low byte.  W: new value.
      case TBLO:
        _timerB.setStartValueLo( value );
        break;

      // TB-HI -- Timer B - hi byte.  W: new value.
      case TBHI:
        _timerB.setStartValueHi( value );
        break;

      // TOD 10ths secs -- Clock 10ths secs in BCD.  W: Set alarm time
      case TOD10THS:
        _clock.setTenthSeconds(
          bcdToInteger( value ),
          0 != (_registers[ CRB ] & Processor.BIT_7) );
        break;

      // TOD secs -- Clock secs in BCD.  W: Set alarm time.
      case TODSEC:
        _clock.setSeconds(
          bcdToInteger( value ),
          0 != (_registers[ CRB ] & Processor.BIT_7) );
        break;

      // TOD min -- Clock mins in BCD.  W: Set alarm time.
      case TODMIN:
        _clock.setMinutes(
          bcdToInteger( value ),
          0 != (_registers[ CRB ] & Processor.BIT_7) );
        break;

      // TOD hour -- Clock hours in BCD.  W: Set alarm time
      case TODHR:
        _clock.setHours(
          bcdToInteger( value ),
          0 != (_registers[ CRB ] & Processor.BIT_7) );
        break;

      // ICR -- Interrupt control register.  W: Interrupt mask.
      case ICR:
        icrWrite( value );
        break;

      // CRA -- Control register A.  W:
      case CRA:
        // Note that the actual start of the timer comes last in the sequence of
        // handled bits.

        // Bit 3: If set then oneshot, else cyclic.
        _timerA.setOneshot( (value & Processor.BIT_3 ) != 0 );
        // Bit 4: If set then force load.
        if ( (value & Processor.BIT_4 ) != 0 )
          _timerA.forceLoad();
        // Bit 0: Start timer if set.
        if ( ( value & Processor.BIT_0 ) != 0 )
          _timerA.start();

        // This is the list of bits that we don't handle at the moment.
        // Bit 1: Output pin PB6 handling - not supported.
        // Bit 2: Output pin PB6 handling - not supported.
        // Bit 5: Select timer driver:  System clock or CNT?
        // Bit 6: SP input/output.
        // Bit 7: TOD 60/50 Hz.
        break;

      default:
        _log.severe( _name + ": Invalid Port written: " + portId );
        System.exit( 1 );
    }
  }



  /**
   * Called if the real time clock reached alarm time.
   */
  void alarm()
  {
    byte mask = _registers[ ICR ];

    _interruptData |= Processor.BIT_2;

    // If we have any bits set in interrupt data as well as in the interrupt
    // mask then send an IRQ to the CPU.
    if ( (mask & _interruptData) != 0 )
      _cpu.IRQ();
  }



  /**
   * This is called if a timer ran down to zero.
   *
   * @param whichOne A reference to the timer that finished.
   */
  void timerFinished( Timer whichOne )
  {
    byte mask = _registers[ ICR ];

    // Find the timer that's finished.
    if ( whichOne == _timerA )
    {
      // Set bit 0 in interrupt data.
      _interruptData |= Processor.BIT_0;
    }
    else
    {
      // Set bit 1 in interrupt data.
      _interruptData |= Processor.BIT_1;
    }

    // If we have any bits set in interrupt data as well as in the interrupt
    // mask then send an IRQ to the cpu.
    if ( (mask & _interruptData) != 0 )
      _cpu.IRQ();
  }



  /*
   * Inherit Javadoc.
   */
  public Port[] getPorts()
  {
    return _ports;
  }



  /**
   * Resets the Chip.
   */
  public void reset()
  {
    _clock.reset(); // Off in 11

    _timerA.setOneshot( true );
    _timerB.setOneshot( true ); // Off in 11

    _timerA.forceLoad();
    _timerB.forceLoad(); // Off in 11
  }


  /**
   * Returns the CIA's name for debugging purposes.
   */
  public String toString()
  {
      return _name;
  }



  /**
   * <p>Handle write calls to the ICR (interrupt control register).  A write
   * call means the interrupt mask is written and that is the content of our
   * internal register array.</p>
   *
   * <p>The special rule is that if in the bit pattern written bit 7 is set all
   * other bits that are set are also set in the interrupt mask.  If bit 7 is
   * not set that means all set bits in the passed value designate the places
   * where bits will be deleted in the interrupt mask.</p>
   *
   * @param value The new contents for the ICR.
   */
  private void icrWrite( int value )
  {
    // Get the interrupt mask and make sure that bit 7 is 0.
    int interruptMask = (_registers[ ICR ] & 0x7f);

    if ( (value & Processor.BIT_7) != 0 )
    {
      // Set the bits in the interrupt mask where a 1 bit was written and
      // don't touch the remaining bits.
      value &= 0x7f;
      interruptMask |= value;
    }
    else
    {
      // Clear the bits in the interrupt mask where a 1 bit was written and
      // don't touch the rest.
      // Set bit 7...
      value |= Processor.BIT_7;
      // ...flip all bits...
      value = ~value;
      // ...and or that into the interrupt mask.
      interruptMask |= value;
    }

    _registers[ ICR ] = (byte)interruptMask;
  }



  /**
   * Read the <i>Interrupt Control Register</i>.
   *
   * @return The ICR value.
   */
  private byte icrRead()
  {
    int id = _interruptData & 0x7f;
    int im = _registers[ ICR ] & 0x7f;

    // If there are bits set in the mask as well as in interrupt data...
    if (  (id & im) != 0 )
      // ...set bit 7 in our result.
      id |= Processor.BIT_7;

    // Delete the interrupt data register.
    _interruptData = 0;

    return (byte)id;
  }



  /**
   * Convert a byte containing two binary coded decimal digits into an integer.
   * Note that it is not ensured that this is a valid BCD number, i.e. it is
   * possible to convert 0x0f which results in 16, though BCD is only defined
   * for decimal digits.
   *
   * @param bcd The BCD number to convert.
   * @return The integer value.
   */
  private static int bcdToInteger( byte bcd )
  {
    // Get the msb digit...
    int result = bcd & 0xf0;
    // ...multiply that by ten (decimal shift left)...
    result *= 10;
    // ...and add the lsb digit.
    result += bcd & 0xf;

    return result;
  }



  /**
   * Convert an integer into a two digit bcd value.  No error if values larger
   * than decimal 99 are passed.
   */
  private static byte integerToBcd( int integer )
  {
    // Get the ones digit, e.g. integer is decimal 59, this results in ones ==
    // 9.
    int ones = integer % 10;
    // Decimal shift right.
    integer /= 10;
    // Get the tens digit, e.g. decimal 59. tens == 5
    int tens = integer % 10;

    // Compute the resulting value.  Dec 59 in, results in 0x59.
    int result = (tens << 4) | ones;

    return (byte)result;
  }


  private static Level _chipLogLevel =
      Level.FINE;

  private static final boolean _doLogging =
      _log.isLoggable( _chipLogLevel );

  /**
   * Log a register read access.
   *
   * @param register The register name.
   * @param value The returned value.
   */
  private void logRead( int register, int value )
  {
      if ( ! _doLogging )
          return;

      _log.log(
          _chipLogLevel,
          _name + ":" +
          _registerNames[ register ] + ":read -> $" +
          " $" + Integer.toHexString( value & 0xff ) );
  }

  /**
   * Logs a register write access.
   *
   * @param register The register name.
   * @param value The written value.
   */
  private void logWrite( int register, int value )
  {
      if ( ! _doLogging )
          return;

      _log.log(
              _chipLogLevel,
              _name + ":" +
              _registerNames[ register ] + ":write -> $" +
              " $" + Integer.toHexString( value & 0xff ) );
  }
}

