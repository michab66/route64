/* $Id: RasterCharacter.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.MultiPixelPackedSampleModel;

import de.michab.simulator.Memory;



/**
 * Implements a rasterer for single color character mode.  Part of the video
 * interface chip.
 *
 * @see de.michab.simulator.mos6502.Vic
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
final class RasterCharacter
  implements
    ScanlineRasterer
{
  /**
   * A reference to the video chip this rasterer belongs to.
   */
  private final Vic _vic;



  /**
   * A reference to the display data buffer.
   */
  private final int[] _screen;



  /**
   * A reference to the system memory.
   */
  private final Memory _memory;



  /**
   *
   */
  private final byte[] _colorMemory;



  /**
   *
   */
  private int _videoRamAddress;



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
   * A data buffer according to the sample model's layout information.
   */
  private final DataBufferByte _charModelBufferByte
    = (DataBufferByte)_charModel.createDataBuffer();



  /**
   *
   */
  private final byte[] _hotModelBuffer = _charModelBufferByte.getData();



  /**
   *
   */
  private final int _hotModelBufferLength = _hotModelBuffer.length;



  /**
   * The character line in C64 line numbers with range [0..24].
   */
  private int _characterLine;



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
  private int _characterAddress;



  /**
   * Creates an instance of this class.
   *
   * @param vic The video chip this instance belongs to.
   * @param screen The screen buffer to use.
   * @param memory A reference to the system memory.
   * @param colorMemory The color memory to use.
   */
  RasterCharacter( Vic vic,
                   int[] screen,
                   Memory memory,
                   byte[] colorMemory )
  {
    _vic = vic;
    _screen = screen;
    _memory = memory;
    _colorMemory = colorMemory;
  }



  /*
   * Inherit Javadoc.
   */
  public synchronized void startFrame( 
      int characterAddress, 
      int videoRamAddress, 
      int dummy )
  {
    _videoRamAddress = videoRamAddress;
    _characterAddress = characterAddress;
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
      _characterLine = currentScanline / 8;
      // Preinit color and char adresses into byte oriented display memory.
      _characterLineAdr = _colorLineAdr = _characterLine * 40;
      // Compute the pointer into the character memory.
      _characterLineAdr += _videoRamAddress;
    }

    for ( int charColumn = 0 ; charColumn < 40 ; charColumn++ )
    {
      // Read the character idx to draw from the 64s main memory...
      int characterIdx = _memory.read( _characterLineAdr + charColumn );
      // ...and mask out the sign.
      characterIdx &= 0xff;

      // Compute the source character line in the character set.
      int y = (characterIdx * 8) + characterScanline;

      // Compute the target character index in the display raster.
      int tmpTargetIdx = offset + (charColumn * 8);
      // Get the front color for the next character.  This is 4 bits wide.
      int frontColorRgb =
        Vic.VIC_RGB_COLORS[ 0xf & _colorMemory[ _colorLineAdr + charColumn ] ];

      // Loop over the eight bits of a single character's scan line.
      for ( int x = 0 ; x < 8 ; x++ )
      {
        if ( 0 != _charModel.getSample( x, y, 0, _charModelBufferByte ) )
          _screen[ tmpTargetIdx+x ] = frontColorRgb;
      }
    }
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
      Vic.VIC_RGB_COLORS[ _vic.read( Vic.BACKGRDCOL0 ) ] );
  }



  /*
   * Inherit Javadoc.
   */
  public int getDebugColor()
  {
    return Vic.CYAN_IDX;
  }



  /*
   * Inherit Javadoc.
   */
  public void badLine( int currentScanline )
  {
    // Read the character data.
    System.arraycopy(
        _memory.getRawMemory(),
        _characterAddress,
        _hotModelBuffer,
        0,
        _hotModelBufferLength );
  }
}
