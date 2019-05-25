/* $Id: RasterCharacterExtended.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import java.awt.image.*;

import de.michab.simulator.Memory;



/**
 * Implements a rasterer for extended color character mode.  Part of the
 * video interface chip.
 *
 * @see de.michab.simulator.mos6502.Vic
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
final class RasterCharacterExtended
  implements
    ScanlineRasterer
{
  /**
   * Our host VIC.
   */
  private final Vic _vic;



  /**
   *
   */
  private final byte[] _colorRam;



  /**
   *
   */
  private final int[] _screen;



  /**
   * A reference to the system memory.
   */
  private final Memory _memory;



  /**
   * This sample model allows the access to a character set in the special
   * c64 memory layout:  8 consecutive bytes in memory make up the pixel
   * pattern for a single character.
   */
  private final MultiPixelPackedSampleModel _charModel =
    new MultiPixelPackedSampleModel(
      DataBuffer.TYPE_BYTE,
      8,
      8 * 512,
      1 );



  /**
   * Create a data buffer according to the sample model's layout
   * information...
   */
  private final DataBufferByte charModelBufferByte
    = (DataBufferByte)_charModel.createDataBuffer();



  /**
   * The unpacked character set data in a save and secure place.
   */
  private final int[] _characterSet = new int[ 8 * 8 * 512 ];



  /**
   *
   */
  private int _videoRamAddress;
  private int _characterAddress;


  /**
   * Index pointing to the start of the current line in main memory.
   */
  private int _characterLineAdr;



  /**
   * Index pointing to the start of the current line in color memory.
   */
  private int _colorLineAdr;



  /**
   *
   */
  private final int[] _backgroundRgb = new int[ 4 ];



  /**
   * Constructor.
   */
  RasterCharacterExtended(
    Vic vic,
    int[] screen,
    Memory memory,
    byte[] colorRam )
  {
    _vic = vic;
    _colorRam = colorRam;
    _screen = screen;
    _memory = memory;
  }



  /*
   * Inherit Javadoc.
   */
  public synchronized void startFrame( 
    int characterAddress, 
    int videoRamAddress, 
    int dummy )
  {
    _characterAddress = characterAddress;
    _videoRamAddress = videoRamAddress;
  }



  /*
   * Inherit Javadoc.
   */
  public synchronized void rasterInto( int offset, int currentScanline )
  {
    // Check which scanline of a character stripe we are drawing.  A
    // character stripe has 8 lines, so we get values [0..7].
    int characterScanline = currentScanline % 8;

    // The following computations are only needed if a new character stripe
    // starts.
    if ( characterScanline == 0 )
    {
      // Compute the line idx in C64 character coordinates.  C64 has 25
      // lines, so here we get a range of [0..24].
      int characterLine = currentScanline / 8;
      // Preinit color and char adresses into byte oriented display memory.
      _characterLineAdr = _colorLineAdr = characterLine * Vic.TXT_COLUMNS;
      // Compute the pointer into the character memory.
      _characterLineAdr += _videoRamAddress;
    }

    byte[] rawMemory = _memory.getRawMemory();
    for ( int charColumn = 0 ; charColumn < Vic.TXT_COLUMNS ; charColumn++ )
    {
      // Read the character idx to draw from the 64s main memory...
      int characterIdx = rawMemory[ _characterLineAdr + charColumn ];
      // ...get the topmost two bits, which are the background color index...
      int colorIdx = (characterIdx >> 6) & 0x3;
      // ...and mask out the sign and the topmost bits.
      characterIdx &= 0x3f;

      // Note that the following two values are pixel addresses:
      // Compute the source character index in the character set.
      int tmpSourceIdx = (characterIdx * 64) + (characterScanline * 8);
      // Compute the target character index in the display raster.
      int tmpTargetIdx = offset + (charColumn * 8);
      // Get the front color for the next character.  This is 4 bits wide.
      int frontColorRgb = 
        Vic.VIC_RGB_COLORS[ 0xf & _colorRam[ _colorLineAdr + charColumn ] ];

      // Loop over the eight bits of a single character's scan line.
      for ( int i = 0 ; i < 8 ; i++ )
      {
        // Make sure that the screen pixels are only touched on colors that
        // are not the background color.
        if ( colorIdx > 0 )
        {
          // Transform 1 bits into frontColor pixels and for 0 bits use the
          // color from color memory.
          _screen[ tmpTargetIdx+i ] =
            _characterSet[ tmpSourceIdx+i ] == 0 ?
              // The two topmost bits select background color.
              _backgroundRgb[ colorIdx ] :
              frontColorRgb;
        }
      }
    }
  }



  /**
   * Decode the packed character set as it exists in main memory into something
   * easier to handle.  TODO Note that this has to be changed -- we have to 
   * read the
   * characters in sync with raster updating from main memory.
   * 
   * @param characterAdr The address of the character set.
   */
  private void unpack( int characterAdr )
  {
    // Get the embedded byte array.
    byte[] tmpByte = 
      charModelBufferByte.getData();

    // Initialise the data buffer.
    System.arraycopy( 
        _memory.getRawMemory(),
        characterAdr,
        tmpByte,
        0,
        tmpByte.length );

    // Finally use everything set up so far to decode the magic character
    // data into a second array where an integer corresponds to a single
    // pixel.
    for ( int y = 0 ; y < 8 * 512 ; y++ )
      for ( int x = 0 ; x < 8 ; x++ )
        _characterSet[ (y*8)+x ] =
          _charModel.getSample( x, y, 0, charModelBufferByte );
  }



  /*
   * Inherit Javadoc.
   */
  public synchronized void backfill( int offset )
  {
    java.util.Arrays.fill(
      _screen,
      offset,
      offset + RasterDisplay.INNER_HORIZ,
      _backgroundRgb[0] );
  }



  /*
   * Inherit Javadoc.
   */
  public int getDebugColor()
  {
    return Vic.GREEN_IDX;
  }



  /*
   * Inherit Javadoc.
   */
  public void badLine( int currentScanline )
  {
    unpack( _characterAddress );

    _backgroundRgb[0] =
      Vic.VIC_RGB_COLORS[ _vic.read( Vic.BACKGRDCOL0 ) ];
    _backgroundRgb[1] =
      Vic.VIC_RGB_COLORS[ _vic.read( Vic.BACKGRDCOL1 ) ];
    _backgroundRgb[2] =
      Vic.VIC_RGB_COLORS[ _vic.read( Vic.BACKGRDCOL2 ) ];
    _backgroundRgb[3] =
      Vic.VIC_RGB_COLORS[ _vic.read( Vic.BACKGRDCOL3 ) ];
  }
}
