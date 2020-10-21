/* $Id: Sprite.java 410 2010-09-27 16:10:47Z Michael $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import java.awt.image.*;

import de.michab.simulator.Memory;



/**
 * Represents a single Sprite, a small image than can be moved on screen and is
 * used primarily in games to represent game characters.  A sprite can either
 * have a single colour or multi colours with reduced size.
 *
 * @see de.michab.simulator.mos6502.Vic
 * @version $Revision: 410 $
 * @author Michael G. Binz
 */
final class Sprite
{
  /**
   * Sprite coordinate system offset in x direction.
   */
  private static final int SPRITE_X_OFFSET = 8;



  /**
   * Sample model for single color sprites.  Note that the size of the sample
   * holds exactly a single sprite raster line and that the single instance is
   * used to draw all sprites.
   */
  private static final MultiPixelPackedSampleModel _spriteModel =
    new MultiPixelPackedSampleModel(
      DataBuffer.TYPE_BYTE,
      RasterSprites.SPRITE_WIDTH,
      1,
      1 );



  /**
   * Sample model for multi color sprites.  Note that the size of the sample
   * holds exactly a single sprite raster line and that the single instance is
   * used to draw all sprites.
   */
  private static final MultiPixelPackedSampleModel _spriteModelMulti =
    new MultiPixelPackedSampleModel(
      DataBuffer.TYPE_BYTE,
      RasterSprites.SPRITE_WIDTH/2,
      1,
      2 );



  /**
   * A data buffer for the single color model.
   */
  private static final DataBufferByte _spriteModelBuffer =
    (DataBufferByte)_spriteModel.createDataBuffer();



  /**
   * A data buffer for the multi color model.
   */
  private static final DataBufferByte _spriteModelMultiBuffer =
    (DataBufferByte)_spriteModelMulti.createDataBuffer();



  /**
   * This sprite's index number [0..7].
   */
  private final int _spriteIdx;



  /**
   * A bit mask where the bit at the sprite index position is set.
   */
  private final int _idxBit;



  /**
   * A reference to the computer's system memory.
   */
  private final Memory _memory;



  /**
   * A reference to the whole screen.  These are RGB pixels.
   */
  private final int[] _screen;



  /**
   *
   */
// TODO Activate collision checking.
  // private int[] _collisionSpriteSprite;



  /**
   *
   */
  private final Vic _vic;



  /**
   * Constructor.
   */
  Sprite( int spriteIdx,
          int[] spriteSpriteCollision,
          Vic home,
          Memory memory,
          int[] screen )
  {
    _spriteIdx = spriteIdx;
    _idxBit = 1 << spriteIdx;
// TODO    _collisionSpriteSprite = spriteSpriteCollision;
    _memory = memory;
    _screen = screen;
    _vic = home;
  }



  /*
   * Inherit Javadoc.
   */
  public synchronized String toString()
  {
    StringBuffer result = new StringBuffer( "Sp#" );
    result.append( _spriteIdx );
    result.append( '(' );

    if ( isOn() )
    {
      result.append( " x/y=" );
      result.append( getX() );
      result.append( '/' );
      result.append( getY() );
      if ( isMulticolor() )
      {
        result.append( ", Multicolor=" );
        result.append( _vic.read( Vic.SPRITEMULTIC0 ) );
        result.append( '/' );
        result.append( _vic.read( Vic.SPRITEMULTIC1 ) );
        result.append( '/' );
        result.append( _vic.read( Vic.SPRITECOL0 + _spriteIdx ) );
      }
      else
      {
        result.append( ", Color=" );
        result.append( _vic.read( Vic.SPRITECOL0 + _spriteIdx ) );
      }
      result.append( ", DBLW/H=" );
      result.append( isDoubleWidth() );
      result.append( isDoubleHeight() );
    }
    else
      result.append( " off" );

    result.append( ')' );

    return result.toString();
  }



  /**
   * Raster this sprite into the current scanline.
   *
   * @param spriteLine The sprite's scanline.
   */
  final synchronized void rasterInto(
      int adr,
      int scanlineOffset,
      int spriteLine )
  {
    int colorRgb = Vic.VIC_RGB_COLORS[
      _vic.read( Vic.SPRITECOL0 + _spriteIdx ) ];

    DataBufferByte currentModelBuffer;
    MultiPixelPackedSampleModel currentModel;

    // Read this sprite's colors.
    boolean isMulticolor = isMulticolor();
    int multiColor0Rgb = 0;
    int multiColor1Rgb = 0;
    if ( isMulticolor )
    {
      multiColor0Rgb =
        Vic.VIC_RGB_COLORS[ _vic.read( Vic.SPRITEMULTIC0 ) ];
      multiColor1Rgb =
        Vic.VIC_RGB_COLORS[ _vic.read( Vic.SPRITEMULTIC1 ) ];
      currentModelBuffer = _spriteModelMultiBuffer;
      currentModel = _spriteModelMulti;
    }
    else
    {
      currentModelBuffer = _spriteModelBuffer;
      currentModel = _spriteModel;
    }

    // Adjust the current sprite scanline if we are in double height mode.
    if ( isDoubleHeight() )
      spriteLine /= 2;

    // Blit the data into our buffer.
    {
      byte[] hotBuffer = currentModelBuffer.getData();
      System.arraycopy(
          _memory.getRawMemory(),
          adr + (spriteLine*hotBuffer.length),
          hotBuffer,
          0,
          hotBuffer.length );
    }

    int targetBaseIdx =
      scanlineOffset +
      getX() +
      SPRITE_X_OFFSET;

    // This code draws multicolor as well as single color sprites.
    for ( int srcx = currentModel.getWidth()-1,
          // Width of a single color area on the target screen.
          step = ( isDoubleWidth() ? 2 : 1  ) * currentModel.getSampleSize(0),
          tgtx = targetBaseIdx + (srcx*step);

          srcx >= 0 ;

          srcx--,
          tgtx-=step )
    {
      switch ( currentModel.getSample( srcx, 0, 0, currentModelBuffer ) )
      {
        // This case switch is used for multi and single color sprites.  So it
        // is the only place where we have to differenciate the handling.
        // Maybe not the fastest...
        case 1:
          for ( int i = tgtx + step -1 ; i >= tgtx ; i-- )
          {
            _screen[ i ] = isMulticolor ? multiColor0Rgb : colorRgb;
            //if ( _collisionSpriteSprite[i] >= 0 )
            //  _vic.collisionSpriteSprite( _spriteIdx, _collisionSpriteSprite[i] );
            //else
            //  _collisionSpriteSprite[i] = _spriteIdx;
          }
          break;

        case 2:
          for ( int i = tgtx + step -1 ; i >= tgtx ; i-- )
          {
            _screen[ i ] = multiColor1Rgb;
            //if ( _collisionSpriteSprite[i] >= 0 )
            //  _vic.collisionSpriteSprite( _spriteIdx, _collisionSpriteSprite[i] );
            //else
            //  _collisionSpriteSprite[i] = _spriteIdx;
          }
          break;

        case 3:
          for ( int i = tgtx + step -1 ; i >= tgtx ; i-- )
          {
            _screen[ i ] = colorRgb;
            //if ( _collisionSpriteSprite[i] >= 0 )
            //  _vic.collisionSpriteSprite( _spriteIdx, _collisionSpriteSprite[i] );
            //else
            //  _collisionSpriteSprite[i] = _spriteIdx;
          }
          break;
      }
    }
  }



  /**
   * Set sprite double width mode.
   */
  private boolean isDoubleWidth()
  {
    return isIndexBitSet( Vic.SPRITEEXPANDX );
  }



  /**
   * Get sprite double height mode.
   */
  private boolean isDoubleHeight()
  {
    return isIndexBitSet( Vic.SPRITEEXPANDY );
  }



  /**
   *
   */
  synchronized boolean isForeground()
  {
    return ! isIndexBitSet( Vic.SPRITEBACKGRD );
  }



  /**
   * Returns whether this sprite is displayed.
   *
   * @return A <code>true</code> if the sprite is displayed.
   */
  synchronized boolean isOn()
  {
    return isIndexBitSet( Vic.SPRITEENABLE );
  }



  /**
   * Returns the minimum scanline the sprite uses.  This value depends on the
   * VIC's scanline system.
   */
  synchronized int getMinimumY()
  {
    int result =
      getY() -
      (RasterSprites.SPRITE_HEIGHT - 1);

    if ( result < 0 )
      result = 0;

    return result;
  }



  /**
   * Returns the maximum scanline that is used by this sprite.  The value
   * depends on the VIC's scanline system.
   */
  synchronized int getMaximumY()
  {
    int result = getY();

    if ( isDoubleHeight() )
      result += RasterSprites.SPRITE_HEIGHT;

    return result;
  }



  /**
   * Get this sprite's y coordinate.
   */
  private int getY()
  {
    return _vic.read( Vic.S0Y + (2*_spriteIdx) ) & 0xff;
  }



  /**
   * Get this sprite's x coordinate.
   */
  private int getX()
  {
    int result =
      0xff & _vic.read( Vic.S0X + (2*_spriteIdx) );
    boolean bitEight =
      isIndexBitSet( Vic.MSBX );
    if ( bitEight )
      result |= (1<<8);
    return result;
  }



  /**
   * Check if this sprite is in multicolor mode.
   *
   * @return True f this sprite is in multicolor mode.
   */
  private boolean isMulticolor()
  {
    return isIndexBitSet( Vic.SPRITEMULTICOL );
  }



  /**
   * Check whether this Sprite's bit is set in the passed register.
   *
   * @param vicRegister The VIC register to read.
   * @return True if this Sprite's bit was set.
   */
  private boolean isIndexBitSet( int vicRegister )
  {
    int value = _vic.read( vicRegister );
    return 0 != (value & _idxBit);
  }
}
