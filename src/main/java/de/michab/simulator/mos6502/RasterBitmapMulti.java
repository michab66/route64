/* $Id: RasterBitmapMulti.java 11 2008-09-20 11:06:39Z binzm $
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
 * Implements a rasterer for multi color bitmap mode.  This is used by the game
 * Time Pilot (Space Pilot).
 *
 * @version $Revision: 11 $
 * @author Michael Binz
 */
final class RasterBitmapMulti
  implements
    ScanlineRasterer
{
  /**
   * A reference to the video chip.
   */
  private final Vic _vic;


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
  private final byte[] _colorRam;



  /**
   * Holds this raster's memory address.
   */
  private int _gfxAddress = 0;



  /**
   * This sample model allows the access to a character set in the special
   * c64 memory layout:  8 consecutive bytes in memory make up the pixel
   * pattern todo.
   */
  private final MultiPixelPackedSampleModel _charModel =
    new MultiPixelPackedSampleModel(
      DataBuffer.TYPE_BYTE,
      4,
      25 * 40 * 8,
      2 );



  /**
   * Create a data buffer according to the sample model's layout
   * information...
   */
  private final DataBufferByte charModelBufferByte
    = (DataBufferByte)_charModel.createDataBuffer();



  /**
   * Create an instance.
   *
   * @param vic A reference to the video chip.
   * @param screen A reference to the output sreen area.
   * @param memory A reference to system memory.
   * @param colorRam A reference to color ram.
   */
  RasterBitmapMulti( 
      Vic vic, 
      int[] screen, 
      Memory memory, 
      byte[] colorRam )
  {
    _vic = vic;
    _screen = screen;
    _memory = memory;
    _colorRam = colorRam;
  }



  /**
   * 
   */
  private int _videoRamAddress;



  /*
   * Inherit javadoc.
   */
  public void startFrame( int dummy, int videoRamAddress, int bitmapAddress )
  {
    _gfxAddress = bitmapAddress;
    _videoRamAddress = videoRamAddress;
  }



  // The character line in C64 line numbers with range [0..24].
  private int _characterLine;
  // Index pointing to the start of the current line in color memory.
  private int _colorAddress1;
  private int _colorAddress2;



  /*
   * Inherit javadoc.
   */
  public void rasterInto( int offset, int currentScanline )
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
      // Preinit color adress into byte oriented display memory.  This one is
      // an offset into the video ram, used for color 1 and 2.
      _colorAddress1 = (_characterLine * 40) + _videoRamAddress;
      _colorAddress2 = (_characterLine * 40);
    }

    for ( int charColumn = 0 ; charColumn < 40 ; charColumn++ )
    {
      // Read the character idx to draw from the 64s main memory...
      int colorIdx1 = _memory.read( _colorAddress1 + charColumn );
      int colorIdx2 = _colorRam[ _colorAddress2 + charColumn ];

      // Compute the target block index in the display raster.  Block means a
      // character block.
      int targetBlockIdx = offset + (charColumn * 8);

      // Loop over the eight bits of a single character's scan line.
      for ( int x = 0 ; x < 4 ; x++ )
      {
        int y = (_characterLine * 40 * 8) + (charColumn * 8) + characterScanline;

        int targetPixelIdx = targetBlockIdx + x + x;

        // Now read the pixel value.  This is in the range [0..4].  Transform
        // that value into the color.
        switch ( _charModel.getSample( x, y, 0, charModelBufferByte ) )
        {
          case 1:
            _screen[ targetPixelIdx ] = _screen[ targetPixelIdx+1 ] =
              Vic.VIC_RGB_COLORS[ (colorIdx1 >> 4) & 0xf ];
            break;

          case 2:
            _screen[ targetPixelIdx ] = _screen[ targetPixelIdx+1 ] =
              Vic.VIC_RGB_COLORS[ colorIdx1 & 0xf ];
            break;

          case 3:
            _screen[ targetPixelIdx ] = _screen[ targetPixelIdx+1 ] =
              Vic.VIC_RGB_COLORS[ colorIdx2 & 0xf ];
            break;
        }
      }
    }
  }



  /*
   * Inherit javadoc.
   */
  public void backfill( int offset )
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
    return Vic.ORANGE_IDX;
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
