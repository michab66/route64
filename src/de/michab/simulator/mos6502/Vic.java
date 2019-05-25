/* $Id: Vic.java 403 2010-09-12 00:30:24Z Michael $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import de.michab.simulator.Clock;
import java.awt.*;
import de.michab.simulator.*;
import java.util.Arrays;



/**
 * Represents the C64's MOS6569 video interface chip.
 *
 * @version $Revision: 403 $
 * @author Michael G. Binz
 */
public class Vic
  extends
    DefaultChip
{
  /**
   * <code>True</code> means debug output is on.
   */
  private static final boolean _debug = false;



  /**
   * This defines a table of the 64s colors.  The table index maps to the c64
   * colour code.
   */
  public static final int[] VIC_RGB_COLORS = new int[]
  {
    // alpha red green blue - 8 bit encoding.  All alpha values are zero.
    // This data comes from VICE's default.vpl palette file.  The commented
    // number is the dither value used there (whatever that means).

    // Black
    0x000000, // 0
    // White
    0xFDFEFC, // E
    // Red
    0xBE1A24, // 4
    // Cyan
    0x30E6C6, // C
    // Purple
    0xB41AE2, // 8
    // Green
    0x1FD21E, // 4
    // Blue
    0x211BAE, // 4
    // Yellow
    0xDFF60A, // C
    // Orange
    0xB84104, // 4
    // Brown
    0x6A3304, // 4
    // Light Red
    0xFE4A57, // 8
    // Dark Gray
    0x424540, // 4
    // Medium Gray
    0x70746F, // 8
    // Light Green
    0x59FE59, // 8
    // Light Blue
    0x5F53FE, // 8
    // Light Gray
    0xA4A7A2, // C
  };



  final static int BLACK_IDX = 0;
  final static int WHITE_IDX = 1;
  final static int RED_IDX = 2;
  final static int CYAN_IDX = 3;
  final static int GREEN_IDX = 5;
  final static int YELLOW_IDX = 7;
  final static int ORANGE_IDX = 8;



  /**
   * The number of sprites supported by the VIC.
   */
  static final int NUM_OF_SPRITES = 8;



  /**
   * The number of columns in text mode.
   */
  static final int TXT_COLUMNS = 40;



  /**
   * The number of lines in text mode.
   */
  static final int TXT_LINES = 25;



  /**
   * The associated RasterDisplay.
   */
  private final RasterDisplay _raster;



  /**
   * A reference to the system's processor.
   */
  private final Cpu6510 _cpu;



  /* TODO collision checking
   * A single raster line used for sprite backgound collision checking.  A true
   * value means that this pixel is set.
  private boolean[] _collisionSpriteBack =
    new boolean[ RasterDisplay.OVERALL_W ];
   */



  /**
   * X coordinate sprite 0.
   */
  static final int S0X = 0;



  /**
   * Y coordinate sprite 0.
   */
  static final public int S0Y = 1;
  static final public int S1X = 2;
  static final public int S1Y = 3;
  static final public int S2X = 4;
  static final public int S2Y = 5;
  static final public int S3X = 6;
  static final public int S3Y = 7;
  static final public int S4X = 8;
  static final public int S4Y = 9;
  static final public int S5X = 10;
  static final public int S5Y = 11;
  static final public int S6X = 12;
  static final public int S6Y = 13;
  static final public int S7X = 14;
  static final public int S7Y = 15;



  /**
   * Most significant bit for X coordinate of all sprites.  Bit 0 represents
   * sprite 0 and so on.
   */
  static final public int MSBX = 16;




  /**
   * Control register 1
   */
  static final public int CTRL1 = 17;



  /**
   * Number of the rasterline that triggers irq.
   */
  static final public int RASTERIRQ = 18;


  /**
   * Light pen X.
   */
  static final public int STROBEX = 19;



  /**
   * Light pen Y.
   */
  static final public int STROBEY = 20;



  /**
   * Sprite enabled.
   */
  static final public int SPRITEENABLE = 21;
  static final public int CTRL2 = 22;

  static final public int SPRITEEXPANDY = 23;
  static final public int VIDEOMEMBASE = 24;
  static final public int INTERRUPTREQUEST = 25;
  static final public int INTERRUPTMASK = 26;
  static final public int SPRITEBACKGRD = 27;
  static final public int SPRITEMULTICOL = 28;
  static final public int SPRITEEXPANDX = 29;
  static final public int SPRITESPRITECOLL = 30;
  static final public int SPRITEBACKCOLL = 31;
  static final public int EXTERIORCOL = 32;
  static final public int BACKGRDCOL0 = 33;
  static final public int BACKGRDCOL1 = 34;
  static final public int BACKGRDCOL2 = 35;
  static final public int BACKGRDCOL3 = 36;
  static final public int SPRITEMULTIC0 = 37;
  static final public int SPRITEMULTIC1 = 38;
  static final public int SPRITECOL0 = 39;
  static final public int SPRITECOL1 = 40;
  static final public int SPRITECOL2 = 41;
  static final public int SPRITECOL3 = 42;
  static final public int SPRITECOL4 = 43;
  static final public int SPRITECOL5 = 44;
  static final public int SPRITECOL6 = 45;
  static final public int SPRITECOL7 = 46;



  /*
   * Represent the chip's unused registers.  Read requests result in 0xff,
   * write requests are ignored.
   */
  static final private int UNUSED1 = 0x2f;
  static final private int UNUSED2 = 0x30;
  static final private int UNUSED3 = 0x31;
  static final private int UNUSED4 = 0x32;
  static final private int UNUSED5 = 0x33;
  static final private int UNUSED6 = 0x34;
  static final private int UNUSED7 = 0x35;
  static final private int UNUSED8 = 0x36;
  static final private int UNUSED9 = 0x37;
  static final private int UNUSED10 = 0x38;
  static final private int UNUSED11 = 0x39;
  static final private int UNUSED12 = 0x3a;
  static final private int UNUSED13 = 0x3b;
  static final private int UNUSED14 = 0x3c;
  static final private int UNUSED15 = 0x3d;
  static final private int UNUSED16 = 0x3e;
  static final private int UNUSED17 = 0x3f;



  /**
   * This chip's number of registers.
   */
  static final private int NUM_OF_REGS = UNUSED17+1;



  /**
   * The internal set of registers.
   */
  private final int[] _registers = new int[NUM_OF_REGS];



  /**
   * The color ram
   */
  private final byte[] _colorRam = new byte[ 1024 ];



  /**
   * This chip's ports.
   */
  private final Port[] _ports;



  /**
   * This is the VIC's current base address.  Can be moved in steps of 16k.
   * Note that this is a precomputed value that can be used directly for
   * or-ing with the offsets.
   */
  private int _pageAddress = 0;



  /**
   * Address of the character ROM.
   */
  private int _characterRomOffset = 0;



  /**
   * The video RAM address.
   */
  private int _videoRamOffset = 0;



  /**
   * Creates a VIC with the passed initial references.
   *
   * @param cpu A reference to the CPU for interrupt purposes.
   * @param memory The memory for the VIC to operate on.
   * @param colorRamAddress The address color ram should be mapped to.
   */
  public Vic(
      Cpu6510 cpu,
      Memory memory,
      int colorRamAddress,
      Clock clock )
  {
    _cpu = cpu;

    // Create this chip's array of ports.
    _ports = createPorts( _registers.length );

    // Map our color memory area into memory.
    Forwarder[] colorForwarders = ArrayPort.createForwarders( _colorRam );
    for ( int i = 0 ; i < colorForwarders.length ;  i++  )
      memory.set( colorForwarders[i], i + colorRamAddress );

    // Create a display component on this memory.
    _raster = new RasterDisplay( this, memory, _colorRam, clock );

    // Reset the chip.
    reset();
  }



  /**
   * Shuts down the <code>Vic</code>.  After this call the object should not
   * longer be used.
   */
  public void terminate()
  {
    _raster.terminate();
  }



  /*
   * Inherit Javadoc.
   */
  public synchronized byte read( int portId )
  {
    int result;

    switch ( portId )
    {
      case RASTERIRQ:
        result = _raster.getCurrentRasterLine();
        break;

      case CTRL1:
        result = _registers[ portId ] & 0x7f;
        int rasterLine = _raster.getCurrentRasterLine() & 0x100;
        rasterLine >>= 1;
        result |= rasterLine;
        break;

      case EXTERIORCOL:
      case BACKGRDCOL0:
      case BACKGRDCOL1:
      case BACKGRDCOL2:
      case BACKGRDCOL3:
      case SPRITEMULTIC0:
      case SPRITEMULTIC1:
      case SPRITECOL0:
      case SPRITECOL1:
      case SPRITECOL2:
      case SPRITECOL3:
      case SPRITECOL4:
      case SPRITECOL5:
      case SPRITECOL6:
      case SPRITECOL7:
        result = 0x0f & _registers[ portId ];
        break;

      case UNUSED1:
      case UNUSED2:
      case UNUSED3:
      case UNUSED4:
      case UNUSED5:
      case UNUSED6:
      case UNUSED7:
      case UNUSED8:
      case UNUSED9:
      case UNUSED10:
      case UNUSED11:
      case UNUSED12:
      case UNUSED13:
      case UNUSED14:
      case UNUSED15:
      case UNUSED16:
      case UNUSED17:
        result = 0xff;
        break;

      case S0X:
      case S0Y:
      case S1X:
      case S1Y:
      case S2X:
      case S2Y:
      case S3X:
      case S3Y:
      case S4X:
      case S4Y:
      case S5X:
      case S5Y:
      case S6X:
      case S6Y:
      case S7X:
      case S7Y:
      case MSBX:
      default:
        result = _registers[ portId ];
    }

    if ( _debug )
      System.err.println( "Vic: read:" + portId + " = " + result );

    return (byte)result;
  }



  /*
   * Inherit Javadoc.
   */
  public synchronized void write( int portId, byte value )
  {
    if ( _debug )
      System.err.println( "Vic: write:" + portId + " = " + value );

    // Save the value written into the register.  This is always done since
    // it allows a very simple implementation of the read switch.  As long as
    // the last value written into a register is returned on the next read no
    // special implementation is needed.
    _registers[ portId ] = value;

    switch ( portId )
    {
      case CTRL1:
      case CTRL2:
      {
        // Set the new video mode.
        _raster.setVideoMode(
            0 != (_registers[ CTRL1 ] & Processor.BIT_5),
            0 != (_registers[ CTRL1 ] & Processor.BIT_6),
            0 != (_registers[ CTRL2 ] & Processor.BIT_4) );
        break;
      }

      case VIDEOMEMBASE:
      {
        // Mask out the character ram bits and mov'em into the right position
        // for simple or'ing.
        _characterRomOffset =
          (value & 0x0e) << 10;
        _videoRamOffset =
          (value & 0xf0) << 6;
        setAddresses(
          _pageAddress | _characterRomOffset,
          _pageAddress | _characterRomOffset,
          _pageAddress | _videoRamOffset );
        break;
      }

      // What follows are the cases that are entirely handled by the default
      // assignment of value done as the very first step.  We keep that list
      // to be able to catch addressing errors in the default case below.
      case S0X:
      case S0Y:
      case S1X:
      case S1Y:
      case S2X:
      case S2Y:
      case S3X:
      case S3Y:
      case S4X:
      case S4Y:
      case S5X:
      case S5Y:
      case S6X:
      case S6Y:
      case S7X:
      case S7Y:
      case MSBX:
      case RASTERIRQ:
      case SPRITEEXPANDY:
      case SPRITEEXPANDX:
      case SPRITEBACKGRD:
      case STROBEX:
      case STROBEY:
      case SPRITEENABLE:
      case INTERRUPTREQUEST:
      case INTERRUPTMASK:
      case SPRITESPRITECOLL:
      case SPRITEBACKCOLL:
      case EXTERIORCOL:
      case SPRITEMULTICOL:
      case BACKGRDCOL0:
      case BACKGRDCOL1:
      case BACKGRDCOL2:
      case BACKGRDCOL3:
      case SPRITEMULTIC0:
      case SPRITEMULTIC1:
      case SPRITECOL0:
      case SPRITECOL1:
      case SPRITECOL2:
      case SPRITECOL3:
      case SPRITECOL4:
      case SPRITECOL5:
      case SPRITECOL6:
      case SPRITECOL7:
      case UNUSED1:
      case UNUSED2:
      case UNUSED3:
      case UNUSED4:
      case UNUSED5:
      case UNUSED6:
      case UNUSED7:
      case UNUSED8:
      case UNUSED9:
      case UNUSED10:
      case UNUSED11:
      case UNUSED12:
      case UNUSED13:
      case UNUSED14:
      case UNUSED15:
      case UNUSED16:
      case UNUSED17:
        break;

      default:
        System.err.println( "Vic: Invalid port offset: " + portId );
        System.exit( 1 );
        break;
    }
  }



  /**
   * Triggers a raster interrupt.
   */
  synchronized void rasterInterrupt()
  {
    handleInterrupt( Processor.BIT_0 );
  }



  /**
   * Handles sprite/sprite collision interrupt.
   *
   * @param a The first colliding sprite.
   * @param b The second colliding sprite.
   */
  void collisionSpriteSprite( int a, int b )
  {
    // Compute the value of the collision register...
    _registers[ SPRITESPRITECOLL ] =
      (1 << a) | (1 << b);
    // ...and handle the interrupt logic.
    handleInterrupt( Processor.BIT_2 );
  }



  /**
   * Called if a sprite collided with the background.
   */
  void collisionSpriteBackground()
  {
    handleInterrupt( Processor.BIT_1 );
  }



  /**
   * Steals the passed amount of cycles from the CPU.
   *
   * @param number The number of cycles to steal.
   */
  void stealCycles(int number)
  {
    _cpu.stealCycles( number );
  }



  /**
   * Encapsulates the VIC interrupt handling logic.  Implements the interrupt
   * mask and interrupt request logic.
   *
   * @param theBit The bit denoting this interrupt in the VIC's INTERRUPTMASK
   *        and INTERRUPTREQUEST registers.
   */
  private void handleInterrupt( int theBit )
  {
    int interruptMask =
      _registers[ INTERRUPTMASK ] & 0xff;
    int interruptReq  =
      _registers[ INTERRUPTREQUEST ] & 0xff;

    // Compute the new interrupt request register value.  Whenever a bit is
    // set, the MSB is also set.
    interruptReq |=
      (theBit | Processor.BIT_7);
    // Write the new value.
    _registers[ INTERRUPTREQUEST ] =
      (byte)interruptReq;

    // In case a bit is set in both the interrupt mask and the interrupt
    // request register generate an interrupt.
    if ( 0 != (interruptMask & interruptReq) )
    {
      _cpu.IRQ();
    }
  }



  /**
   * Returns the raw register array.  Access is only allowed for the aggregated
   * elements of the VIC.
   *
   * @return The raw register array.
   */
  int[] getRawRegisters()
  {
    return _registers;
  }



  /*
   * Use parent class javadoc.
   */
  public Port[] getPorts()
  {
    return _ports;
  }



  /**
   * Set the page address.  Only the least 2 significant bits are used which
   * means pages zero to three are available.
   *
   * @param page The page address.
   */
  synchronized public void setPageAddress( int page )
  {
    _pageAddress = (page << 14) & 0xffff;

    setAddresses(
        _pageAddress | _characterRomOffset,
        _pageAddress | _characterRomOffset,
        _pageAddress | _videoRamOffset );
  }



  /**
   * Return the current page address.  This is a number in the range
   * [0..3].
   *
   * @return The current page address.
   */
  public int getPageAddress()
  {
    return (_pageAddress >> 14) & 0x3;
  }


  /**
   * Set the chip's character address.  Note that a full address is required,
   * not only the character block.  The VIC's page address isn't taken into
   * account.
   *
   * @param charAdr The character memory address.
   */
  protected void setAddresses( int charAdr, int bitmap, int videoram )
  {
    _raster.setAddresses( charAdr, bitmap, videoram );
  }



  /**
   * Returns a <code>java.awt.Component</code> that will show this VIC's
   * output.  This has to be used to place the emulator's display in a user
   * interface.
   *
   * @return The component that will show the VIC output.
   */
  public Component getComponent()
  {
    return _raster;
  }



  /**
   * Reset all registers to a zero value.
   */
  public void reset()
  {
    Arrays.fill( _registers, 0 );
  }



  /**
   * Get the current frame color.
   *
   * @return The current frame color.
   */
  public Color getExteriorColor()
  {
    return new Color( VIC_RGB_COLORS[ _registers[ EXTERIORCOL ] & 0xf ] );
  }
}
