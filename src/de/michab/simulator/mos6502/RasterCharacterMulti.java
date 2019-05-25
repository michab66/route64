/* $Id: RasterCharacterMulti.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import java.awt.image.*;
import de.michab.simulator.*;



/**
 * Implements a rasterer for the multicolor character mode.  Part of the video
 * interface chip.
 *
 * @see de.michab.simulator.mos6502.Vic
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
final class RasterCharacterMulti
  implements
    ScanlineRasterer
{
  /**
   * Our host VIC.
   */
  private final Chip _vic;



  /**
   * The screen array in RGB pixels.
   */
  private final int[] _screen;



  /**
   * A reference to system memory.
   */
  private final Memory _memory;



  /**
   *
   */
  private final byte[] _colorMemory;



  /**
   *
   */
  int _videoRamAddress;

    // Index pointing to the start of the current line in main memory.
  private int _characterLineAdr;
    // Index pointing to the start of the current line in color memory.
  private int _colorLineAdr;



  /**
   * This sample model allows the access to a character set in the special
   * c64 memory layout:  8 consecutive bytes in memory make up the pixel
   * pattern for a single character.
   */
  private final MultiPixelPackedSampleModel _charModel =
    new MultiPixelPackedSampleModel(
      DataBuffer.TYPE_BYTE,
                  4,
                  8 * 512,
                  2 );



  /**
   * Create a data buffer according to the sample model's layout
   * information...
   */
  private final DataBufferByte charModelBufferByte
    = (DataBufferByte)_charModel.createDataBuffer();



  /**
   *
   */
  private final byte[] _charModelBuffer = charModelBufferByte.getData();



  /**
   * 
   */
  private final int _charModelBufferLength = _charModelBuffer.length;



  /**
   * Create an instance.
   * 
   * @param vic The video chip we are part of.
   * @param screen The actual screen array that is ultimately written out to the
   *        screen.
   * @param rawMemory The 64's raw memory.
   * @param colorMemory The colour ram.
   */
  RasterCharacterMulti( 
    Vic vic, 
    int[] screen, 
    Memory rawMemory, 
    byte[] colorMemory )
  {
    _colorMemory = colorMemory;
    _vic = vic;
    _screen = screen;
    _memory = rawMemory;
  }



  private int _characterAddress;



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
      // Preinit color and char adresses into byte oriented display memory.
      _characterLineAdr = _colorLineAdr = 
        (currentScanline / 8) * Vic.TXT_COLUMNS;
      // Compute the pointer into the character memory.
      _characterLineAdr += _videoRamAddress;
    }

    for ( int charColumn = 0 ; charColumn < Vic.TXT_COLUMNS ; charColumn++ )
    {
      // Read the character idx to draw from the 64s main memory...
      int characterIdx = _memory.read( _characterLineAdr + charColumn );
      // ...and mask out the sign.
      characterIdx &= 0xff;

      // Compute the source character index in the character set.
      int y = (characterIdx * 8) +characterScanline;
      // Compute the target character index in the display raster.
      int tmpTargetIdx = offset + (charColumn * 8);
      // Get the front color for the next character.  This is 4 bits wide.
      int frontColor = 0xf & _colorMemory[ _colorLineAdr + charColumn ];
      // Transform that into an rgb value.
      int frontColorRgb = Vic.VIC_RGB_COLORS[ frontColor ];

      // If the third bit of the color value is not set...
      if ( 0 == (frontColor & Processor.BIT_3) )
      {
        // Then do a standard draw as in normal char mode.
        // Loop over the eight bits of a single character's scan line.
        for ( int i = 0 ; i < 8 ; i+=2 )
        {
          switch ( _charModel.getSample( i/2, y, 0, charModelBufferByte ) )
          {
            case 1:
              _screen[ tmpTargetIdx+i+1 ] = frontColorRgb;
              break;

            case 2:
              _screen[ tmpTargetIdx+i ] = frontColorRgb;
              break;

            case 3:
              _screen[ tmpTargetIdx+i ] =
                _screen[ tmpTargetIdx+i+1 ] =
                  frontColorRgb;
              break;
          }
        }
      }
      else
      {
        int background1Rgb =
          Vic.VIC_RGB_COLORS[ _vic.read( Vic.BACKGRDCOL1 ) ];
        int background2Rgb =
          Vic.VIC_RGB_COLORS[ _vic.read( Vic.BACKGRDCOL2 ) ];
        // Do a multicolor draw.
        // Loop over the eight bits of a single character's scan line.
        for ( int i = 0 ; i < 8 ; i+=2 )
        {
          switch ( _charModel.getSample( i/2, y, 0, charModelBufferByte ) )
          {
            case 1:
              _screen[ tmpTargetIdx+i ] =
                _screen[ tmpTargetIdx+i+1 ] =
                  background1Rgb;
              break;

            case 2:
              _screen[ tmpTargetIdx+i ] =
                _screen[ tmpTargetIdx+i+1 ] =
                  background2Rgb;
              break;

            case 3:
              _screen[ tmpTargetIdx+i ] =
                _screen[ tmpTargetIdx+i+1 ] =
                  Vic.VIC_RGB_COLORS[ 0x7 & frontColor ];
              break;
          }
        }
      }
    }
  }



  /*
   * Inherit javadoc.
   */
  public synchronized void backfill( int offset )
  {
    java.util.Arrays.fill(
      _screen,
      offset,
      offset + RasterDisplay.INNER_HORIZ,
      Vic.VIC_RGB_COLORS[ _vic.read( Vic.BACKGRDCOL0 ) ] );
  }



  /*
   * Inherit Javadoc.
   */
  public int getDebugColor()
  {
    return Vic.YELLOW_IDX;
  }



  /*
   * Inherit Javadoc.
   */
  public void badLine( int currentScanline )
  {
    // Initialise the data buffer.
    System.arraycopy(
        _memory.getRawMemory(),
        _characterAddress,
        _charModelBuffer,
        0,
        _charModelBufferLength );
  }
}
