/* $Id: RasterSprites.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2003 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import de.michab.simulator.Memory;



/**
 * Responsible for everything related to sprite rastering.  This includes
 * collision checking as well as foreground and background priority.
 *
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
final class RasterSprites
{
  /**
   * A reference to our host VIC.
   */
  private final Vic _vic;



  /**
   * The sprite raster heigth.
   */
  final static int SPRITE_HEIGHT = 21;



  /**
   * The sprite's raster width.
   */
  final static int SPRITE_WIDTH = 24;



  /**
   * The current video RAM address.
   */
  private int _videoRamAddress;



  /**
   * Reference to all sprites.
   */
  private final Sprite[] _sprites = new Sprite[ Vic.NUM_OF_SPRITES ];



  /**
   * A reference to system memory.
   */
  private final Memory _memory;



  /**
   * A single raster line used for sprite sprite collision checking.  A
   * negative value means that the pixel isn't set.  Zero and positive values
   * describe the number of the sprite at that place.
   */
  private final int[] _collisionSpriteSprite =
    new int[ RasterDisplay.OVERALL_W ];



  /**
   * Create the sprite rasterer.
   *
   * @param screen An array representing the screen that is drawn.
   * @param memory A reference to the system memory.
   * @param vic A reference to our home VIC.
   */
  RasterSprites( int[] screen, Memory memory, Vic vic )
  {
    _vic = vic;
    _memory = memory;

    // Create the Sprites.
    for ( int i = _sprites.length -1 ; i >= 0 ; i-- )
      _sprites[ i ] = new Sprite( 
        i,
        _collisionSpriteSprite,
        vic,
        _memory,
        screen );
  }



  /**
   * Raster all active front sprites into the current scanline.
   * 
   * @param scanlineOffset The offset of the current scanline in
   *        the screen array.
   * @param scanline The number of the current scanline.
   */
  public synchronized void rasterFrontInto( 
      int scanlineOffset,
      int scanline )
  {
    rasterInto( true, scanlineOffset, scanline );
  }



  /**
   * Raster all active background sprites into the current scanline.
   * 
   * @param videoRamAddress The current video RAM address.
   * @param scanlineOffset The offset of the current scanline in
   *        the screen array.
   * @param scanline The number of the current scanline.
   */
  public synchronized void rasterBackInto( int videoRamAddress,
                              int scanlineOffset,
                              int scanline )
  {
    // Clear the collision buffer.
    java.util.Arrays.fill( _collisionSpriteSprite, Integer.MIN_VALUE );

    _videoRamAddress = videoRamAddress;
    rasterInto( false, scanlineOffset, scanline );
  }



  /**
   * Raster all active sprites into the current scanline.
   */
  private void rasterInto( 
    boolean rasterFront, 
    int offset, 
    int scanline )
  {
    int vicSegment = _videoRamAddress & 0xc000;

    byte[] rawMemory = _memory.getRawMemory();
    
    // Spriteenabled holds a bit for each displayed sprite.  In case this is 
    // zero, no sprites are displayed.  We shift the bits one position to
    // the right on each loops cycle.
    for ( int i = 0, 
          spriteEnabled = 0xff & _vic.read( Vic.SPRITEENABLE ) 
          ; 
          spriteEnabled != 0 
          ; 
          i++, 
          spriteEnabled >>= 1 )
    {
      if ( 0 == (spriteEnabled & 1) )
        continue;

      Sprite current = _sprites[i];

      // Is it on the layer we are currently drawing?
      if ( rasterFront != current.isForeground() )
        continue;

      // Get the minimum and maximum scanline from the sprite.  This has to be
      // cached and forwarded to the raster method, since that could change
      // asynchronously under processor control.
      int miny = current.getMinimumY();
      if ( scanline < miny )
        continue;

      int maxy = current.getMaximumY();
      if ( scanline > maxy )
        continue;

      // The block number for the 8 sprites are located in the 8 bytes at the
      // end of the video ram.
      int spriteBlockNumber = 0xff &
        rawMemory[ _videoRamAddress + (1024 - Vic.NUM_OF_SPRITES) + i ];
      // Each sprite definition block is 64 byte in size.
      int spriteAddress = spriteBlockNumber * 0x40;
      // Map that into the current vic page and pass that final address to the
      // sprite.
      current.rasterInto( 
          vicSegment | spriteAddress, 
          offset, 
          scanline - miny );
    }
  }
}
