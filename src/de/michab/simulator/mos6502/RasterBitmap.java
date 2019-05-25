/* $Id: RasterBitmap.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.MultiPixelPackedSampleModel;

import de.michab.simulator.Memory;



/**
 * Implements a rasterer for single color bitmap mode.
 *
 * @author Michael G. Binz
 */
final class RasterBitmap
  implements
    ScanlineRasterer
{
  /**
   *
   */
  private final int[] _screen;



  /**
   *
   */
  private final Memory _memory;



  /**
   *
   */
  private int _gfxAddress = 0;



  /**
   * This sample model allows the access to a character set in the special
   * c64 memory layout:  8 consecutive bytes in memory make up the pixel
   * pattern for a single character.
   */
  private final MultiPixelPackedSampleModel _charModel =
    new MultiPixelPackedSampleModel(
      DataBuffer.TYPE_BYTE,
      8,
      25 * 40 * 8,
      1 );



  /**
   * Create a data buffer according to the sample model's layout
   * information...
   */
  private final DataBufferByte charModelBufferByte = 
    (DataBufferByte)_charModel.createDataBuffer();



  /**
   * Create an instance.
   * 
   * @param screen The screen that receives the raster data.
   * @param memory The emulation's memory.
   */
  RasterBitmap( int[] screen, Memory memory )
  {
    _screen = screen;
    _memory = memory;
  }



  /**
   * 
   */
  private int _videoRamAddress;



  /*
   * Inherit javadoc.
   */
  public synchronized void startFrame(
      int dummy,
      int videoRamAddress,
      int bitmapAddress )
  {
    _gfxAddress = bitmapAddress;
    _videoRamAddress = videoRamAddress;
/*
    // Get the model's buffer...
    byte[] array = charModelBufferByte.getData();

    // ...and initialise it.
    System.arraycopy( _memory.getRawMemory(),
                      _gfxAddress,
                      array,
                      0,
                      array.length );*/
  }



  // The character line in C64 line numbers with range [0..24].
  private int _characterLine;
  // Index pointing to the start of the current line in color memory.
  private int _colorAddress;



  /*
   * Inherit javadoc.
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
      _colorAddress = (_characterLine * 40) + _videoRamAddress;
    }

    for ( int charColumn = 0 ; charColumn < 40 ; charColumn++ )
    {
      // Read the character idx to draw from the 64s main memory...
      int colorIdx = 
        _memory.read( _colorAddress + charColumn );
      int color0 =
        Vic.VIC_RGB_COLORS[ colorIdx & 0xf ];
      int color1 =
        Vic.VIC_RGB_COLORS[ (colorIdx >> 4) & 0xf ];

      // Compute the target character index in the display raster.
      int tmpTargetIdx = 
        offset + 
        (charColumn * 8);

      // Loop over the eight bits of a single character's scan line.
      for ( int x = 0 ; x < 8 ; x++ )
      {
        int y = 
          (8 * ((_characterLine * 40) + charColumn)) + 
          characterScanline;

        _screen[ tmpTargetIdx+x ] =
          _charModel.getSample( x, y, 0, charModelBufferByte ) == 0 ?
            color0 :
            color1 ;
      }
    }
  }



  /*
   * Inherit javadoc.
   */
  public synchronized void backfill( int offset )
  {
    // Note: Color is computed for each 8 pixel cell from color
    // memory in this graphics mode.  As a concequence we cannot
    // backfill() the scanline.
  }



  /*
   * Inherit Javadoc.
   */
  public int getDebugColor()
  {
    return Vic.RED_IDX;
  }



  /*
   * Inherit Javadoc.
   */
  public void badLine( int currentScanline )
  {
    // Get the model's buffer...
    byte[] array = charModelBufferByte.getData();

    // ...and initialise it.
    System.arraycopy( _memory.getRawMemory(),
                      _gfxAddress,
                      array,
                      0,
                      array.length );
  }
}
