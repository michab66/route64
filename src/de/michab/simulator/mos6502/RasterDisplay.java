/* $Id: RasterDisplay.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import de.michab.simulator.Clock;
import de.michab.simulator.*;

import java.awt.*;
import java.awt.image.*;
import java.util.Arrays;



/**
 * Responsible for rastering the whole VIC screen.  Controls the video mode
 * dependent rasterers as well as the sprite rasterer.
 *
 * TODO we need the full timing of the VIC.  The following questions have to
 * be answered:
 *  How many cycles does it take between two full images, i.e. how long does it
 *  take the beam to return from the bottom of the video screen to the top
 *  where the next pictures starts?
 *  What is the impact if the video screen is switched off (Like when doing a
 *  datasette load) Is that then for all raster lines the same as with the
 *  frame lines?
 *
 * @see ScanlineRasterer
 * @see RasterSprites
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
final class RasterDisplay
  extends Component
  implements Runnable
{
  /**
   *
   */
  private static final long serialVersionUID = 4468735705975651008L;


  private static final boolean _debug = false;


  /**
   * The height of the vertical frame.  Note that this also is the scanline
   * number of the first line of the display window.
   */
  private static final int FRAME_VERT = 51;



  /**
   * Visible frame height in pixels.  Used for the upper and lower frame.  Note
   * that this value can be adjusted freely without impacting the emulation.
   */
  private static final int VISIBLE_FRAME_VERT = 3*8;



  /**
   * The number of invisible scan lines.
   */
  static private final int VERTICAL_INVISIBLE =
    FRAME_VERT - VISIBLE_FRAME_VERT;



  /**
   * Frame width in pixels. Used for the right and left frame.
   */
  static final int FRAME_HORIZ = 4*8;



  /**
   * Height of the visible center window.
   */
  static final int INNER_VERT = 200;



  /**
   * Width of the visible center window.
   */
  static final int INNER_HORIZ = 320;



  /**
   * Overall width of the screen, includes the frame.
   */
  static final int OVERALL_W =
    FRAME_HORIZ + INNER_HORIZ + FRAME_HORIZ;



  /**
   * Overall height of the screen, includes the frame.
   */
  private static final int OVERALL_H =
    VISIBLE_FRAME_VERT + INNER_VERT + VISIBLE_FRAME_VERT;



  /**
   * Sprite coordinate system offset in y direction.
   */
  private static final int SPRITE_Y_OFFSET = 30;



  /**
   * A reference to the current video mode raster engine.
   */
  private ScanlineRasterer _currentVideoMode;
  private ScanlineRasterer _scheduledVideoMode;



  /**
   * The monochrome text rasterer.
   */
  private final ScanlineRasterer _txtNormal;



  /**
   * The multi colour text mode rasterer.
   */
  private final ScanlineRasterer _txtMulti;



  /**
   * The extended color text mode rasterer.
   */
  private final ScanlineRasterer _txtExt;



  /**
   * The monochrome bitmap rasterer.
   */
  private final ScanlineRasterer _gfxNormal;



  /**
   * The multi color bitmap rasterer.
   */
  private final ScanlineRasterer _gfxMulti;



  /**
   * The raster engine responsible for sprite rastering.
   */
  private final RasterSprites _spriteRasterer;



  /**
   * Our handle to the system clock.
   */
  private final Clock.ClockHandle _clockId;



  /**
   * Used for layout management.
   */
  private static final Dimension _componentsSize
    = new Dimension( OVERALL_W, OVERALL_H );



  /**
   * The display raster.  Each integer in this array represents one pixel
   * on the 64s screen in rgb color.
   */
  private final int[] _screen = new int[ OVERALL_W * OVERALL_H ];



  /**
   * The image that is ultimately drawn onto the component.
   */
  private final BufferedImage _bufferedImage =
    new BufferedImage( OVERALL_W,
                       OVERALL_H,
                       BufferedImage.TYPE_INT_RGB );


  /**
   * A reference to our home VIC.
   */
  private final Vic _vic;



  /**
   * A reference to the computer's main memory.
   */
  private final Memory _memory;



  /**
   * A reference to color RAM.
   */
  private final byte[] _colorRam;



  /**
   * This thread drives display drawing.
   */
  private final Thread _repaintThread;



  /**
   * The raster line that is currently drawn.
   */
  private int _currentRasterLine = 0;



  /**
   *
   */
  private int _characterSetAdr = 0;



  /**
   *
   */
  private int _videoRamAddress = 0;



  /**
   *
   */
  private int _bitmapAddress = 0;



  /**
   * The graphics object used for drawing.  The instance is injected by paint
   * calls from our UI context.
   *
   * @see RasterDisplay#paint(Graphics)
   */
  private Graphics _graphics = null;



  /**
   * Creates a raster display instance.
   *
   * @param vic The home VIC.
   * @param mem A reference to the main memory.
   * @param colorRam The color RAM.
   */
  RasterDisplay(
      Vic vic,
      Memory mem,
      byte[] colorRam,
      Clock clock )
  {
    setSize( _componentsSize );

    _vic = vic;
    _colorRam = colorRam;

    // Get a reference to the system's memory.
    _memory = mem;

    // Init the sprite rasterer.
    _spriteRasterer = new RasterSprites(
      _screen,
      _memory,
      vic );

    // Init the default video mode raster engine.
    _txtNormal = new RasterCharacter(
      vic,
      _screen,
      _memory,
      _colorRam );

    _txtMulti = new RasterCharacterMulti(
      vic,
      _screen,
      _memory,
      _colorRam );

    _txtExt = new RasterCharacterExtended(
      vic,
      _screen,
      _memory,
      _colorRam );

    _gfxNormal = new RasterBitmap(
      _screen,
      _memory );

    _gfxMulti = new RasterBitmapMulti(
      vic,
      _screen,
      _memory,
      _colorRam );

    // Set the default rasterer.
    _currentVideoMode = _txtNormal;

    // Register with the clock.
    _clockId = clock.register();
    // Create the repaint thread.
    _repaintThread = new Thread( this, getClass().getName() );
    _repaintThread.setDaemon( true );
    _repaintThread.start();
  }



  /**
   * Reset the component to its preferred size.
   */
  public void resetSize()
  {
    setSize( getPreferredSize() );
  }



  /**
   * Set the addresses of the different memory regions in a single step.
   *
   * @param charAdr
   * @param bitmap
   * @param videoram
   */
  synchronized final void setAddresses( int charAdr, int bitmap, int videoram )
  {
    _characterSetAdr = charAdr;
    _videoRamAddress = videoram;
    _bitmapAddress = bitmap & 0xe000;
    _currentVideoMode.startFrame(
      _characterSetAdr,
      _videoRamAddress,
      _bitmapAddress );
  }



  /**
   * Shuts down the raster thread.
   */
  void terminate()
  {
    _repaintThread.interrupt();
  }



  /**
   * Thread driven main repaint loop.
   */
  public void run()
  {
    _clockId.prepare();
    try
    {
      // We unschedule the repaint thread until we are actually receiving
      // the first paint notification.  For the reschedule() operation
      // see the paint() implementation.
      _clockId.unschedule();

      while ( ! _repaintThread.isInterrupted() )
      {
        _currentVideoMode.startFrame(
          _characterSetAdr,
          _videoRamAddress,
          _bitmapAddress );
        // Draw a single frame.
        drawFrame();
      }
    }
    // Catch all remaining untagged exceptions.  ArrayIndexOutOfBounds is quite
    // common here.
    catch ( Exception e )
    {
      e.printStackTrace();
      System.exit( 1 );
    }
  }



  /**
   * Draws a single frame.  The all-time goal for this method is: DRAW THE
   * RASTER FASTER.  This is an example for really *hot* code performance-wise.
   */
  private void drawFrame()
  {
    int rasterMax =
      FRAME_VERT +
      INNER_VERT +
      FRAME_VERT;

    // Iterate over one scanline after the other.
    for (
        _currentRasterLine = 0 ;
        _currentRasterLine < rasterMax ;
        _currentRasterLine++ )
    {
      if ( isBadLine( _currentRasterLine ) )
      {
        _vic.stealCycles( 40 );

        if ( _scheduledVideoMode != null )
        {
          _scheduledVideoMode.startFrame(
            _characterSetAdr,
            _videoRamAddress,
            _bitmapAddress );
          _currentVideoMode = _scheduledVideoMode;
          _scheduledVideoMode = null;
        }

        _currentVideoMode.badLine( _currentRasterLine );
      }

      drawRasterLine( _currentRasterLine );
      _clockId.advance( 64 );
    }

    // Raster screen is complete and up to date, now beam the whole thing into
    // the image...
    _bufferedImage.getRaster().setDataElements(
      0,
      0,
      OVERALL_W,
      OVERALL_H,
      _screen );

    // ...and bang out the data to where the sun always shines.
    _graphics.drawImage(
      _bufferedImage,
      0,
      0,
      getWidth(),
      getHeight(),
      0,
      0,
      _bufferedImage.getWidth(),
      _bufferedImage.getHeight(),
      null );
  }



  /**
   * Draws a particular raster line.
   *
   * @param rasterLine The raster line to draw.
   */
  private void drawRasterLine( int rasterLine )
  {
//    int offset = screenOffsetY();
    int frameColor = Vic.VIC_RGB_COLORS[ _vic.read( Vic.EXTERIORCOL ) ];

    // Check for raster irqs and their relatives.
    boolean isRasterInterruptLine =
      rasterLine == getInterruptRasterLine();
    if ( isRasterInterruptLine )
    {
      _vic.rasterInterrupt();
    }

    // Leave if not in the visible area.
    if ( rasterLine < VERTICAL_INVISIBLE ||
         rasterLine >= FRAME_VERT + INNER_VERT + VISIBLE_FRAME_VERT )
      return;

    // The index of the current raster line in the screen array.
    int rasterlineIdx =
      (rasterLine - VERTICAL_INVISIBLE) * OVERALL_W;

    /////////////////////////////////
    // Draw the inner character area.
    /////////////////////////////////
    int framePlusTopBottom = isWideBorderY() ? 7 : 0;
    if ( rasterLine >= (FRAME_VERT + framePlusTopBottom) &&
         rasterLine < (FRAME_VERT + INNER_VERT - framePlusTopBottom ) &&
         isScreenOn() )
    {
      // Fill the current scanline with background pixels.
      _currentVideoMode.backfill( rasterlineIdx + FRAME_HORIZ );

      // Raster the background sprites.
      _spriteRasterer.rasterBackInto(
        _videoRamAddress,
        rasterlineIdx,
        rasterLine-FRAME_VERT + SPRITE_Y_OFFSET );

      // The current raster mode is responsible for drawing the screen's
      // content.
      _currentVideoMode.rasterInto(
        rasterlineIdx+FRAME_HORIZ,
        rasterLine-FRAME_VERT );

      // Raster the foreground sprites.
      _spriteRasterer.rasterFrontInto(
        rasterlineIdx,
        rasterLine-FRAME_VERT + SPRITE_Y_OFFSET );

      // Draw the right and left frame.
      int framePlusLeft;
      int framePlusRight;
      if ( isWideBorderX() )
      {
        framePlusLeft = 7;
        framePlusRight = 9;
      }
      else
      {
        framePlusLeft = 0;
        framePlusRight = 0;
      }
      Arrays.fill(
        _screen,
        rasterlineIdx,
        rasterlineIdx + FRAME_HORIZ + framePlusLeft,
        frameColor );
      Arrays.fill(
        _screen,
        rasterlineIdx + FRAME_HORIZ + INNER_HORIZ - framePlusRight,
        rasterlineIdx + FRAME_HORIZ + INNER_HORIZ + FRAME_HORIZ,
        frameColor );
    }
    ///////////////////////
    // Draw the frame part.
    ///////////////////////
    else
    {
      // Draw the top and bottom frame scanlines.
      Arrays.fill(
        _screen,
        rasterlineIdx,
        rasterlineIdx + OVERALL_W,
        frameColor );
    }

    if ( _debug )
    {
      _screen[rasterlineIdx] =
        Vic.VIC_RGB_COLORS[ _currentVideoMode.getDebugColor() ];
      _screen[rasterlineIdx+1] =
        Vic.VIC_RGB_COLORS[ Vic.BLACK_IDX ];
      _screen[rasterlineIdx+2] =
        Vic.VIC_RGB_COLORS[
          isRasterInterruptLine ? Vic.WHITE_IDX : Vic.BLACK_IDX ];
      _screen[rasterlineIdx+3] =
        Vic.VIC_RGB_COLORS[ Vic.BLACK_IDX ];
      _screen[rasterlineIdx+4] = Vic.VIC_RGB_COLORS[
        isBadLine( rasterLine ) ? Vic.WHITE_IDX : Vic.BLACK_IDX ];
      _screen[rasterlineIdx+5] =
        Vic.VIC_RGB_COLORS[ Vic.BLACK_IDX ];
    }
  }



  /**
   * Set the video mode according to the passed flags.
   *
   * @param bitmap <code>True</code> if graphics mode is selected.
   * @param extended <code>True</code> if extended color mode is selected.
   * @param multi <code>True</code> if multi color mode is selected.
   */
  synchronized void setVideoMode(
      boolean bitmap,
      boolean extended,
      boolean multi )
  {
    ScanlineRasterer newVideoMode;

    if ( bitmap )
    {
      // Extended isn't supported for bitmap modes.
      if ( multi )
        newVideoMode = _gfxMulti;
      else
        newVideoMode = _gfxNormal;
    }
    else
    {
      // Not clear what to do if multi and extended are set.  Currently multi-
      // color just overrides extended.
      if ( multi )
        newVideoMode = _txtMulti;
      else if ( extended )
        newVideoMode = _txtExt;
      else
        newVideoMode = _txtNormal;
    }

    _scheduledVideoMode = newVideoMode;
  }



  /**
   * Returns the current raster line.
   *
   * @return The current raster line.
   */
  synchronized int getCurrentRasterLine()
  {
    return _currentRasterLine;
  }



  /**
   * Get the raster line that will trigger an interrupt on drawing.
   *
   * @return The interrupt raster line that is currently set.
   */
  private int getInterruptRasterLine()
  {
    // Note that we cannot read() the vic here since the RASTERIRQ register has
    // different behaviour on read and write:  Write sets the raster line on
    // which an interrupt is triggered, read reads the current raster line.
    int rasterLine = _vic.getRawRegisters()[ Vic.RASTERIRQ ];
    rasterLine &= 0xff;
    if ( (_vic.read( Vic.CTRL1 ) & Processor.BIT_7) != 0 )
      rasterLine |= 0x100;
    return rasterLine;
  }



  /**
   * Is the display switched on?
   *
   * @return True if display is on, else false.
   */
  private boolean isScreenOn()
  {
    byte ctrl1 = _vic.read( Vic.CTRL1 );
    return 0 != (ctrl1 & Processor.BIT_4);
  }



  /**
   * Get the offset of the visible display window.  This is a three bit value
   * that can be accessed in VIC register CTRL1 bits 2-0.
   *
   * @return The y offset of the visible display window.
   */
  private int screenOffsetY()
  {
    return screenOffset( Vic.CTRL1 );
  }



  /**
   * The common implementation for screen offset in x and y direction.
   *
   * @param register The register to read.  One of CTRL1 or CTRL2.
   * @return The offset of the visible display window.
   */
  private int screenOffset( int register )
  {
    byte ctrl = _vic.read( register );
    return ctrl & (Processor.BIT_2 | Processor.BIT_1 | Processor.BIT_0);
  }



  /**
   * Checks whether the right and left borders have to be drawn in wide mode.
   * This check is based on VIC register 0x16, bit 3.
   *
   * @return If the border is to be drawn extended, <code>true</code> is
   *         returned.
   */
  private boolean isWideBorderX()
  {
    return isWideBorder( Vic.CTRL2 );
  }



  /**
   * Checks whether the top and bottom borders have to be drawn in wide mode.
   * This check is based on VIC register 0x16, bit 3.
   *
   * @return If the border is to be drawn extended, <code>true</code> is
   *         returned.
   */
  private boolean isWideBorderY()
  {
    return isWideBorder( Vic.CTRL1 );
  }



  /**
   * The common implementation for the wide border tests.
   *
   * @param register Allowed are either Vic.CTRL1 or Vic.CTRL2.
   * @return True if bit 3 of the respective register is not set.
   */
  private boolean isWideBorder( int register )
  {
    return 0 == (_vic.read( register ) & Processor.BIT_3);
  }



  /**
   * Check whether this is a bad line.
   *
   * @return true if this is a bad line.
   */
  private boolean isBadLine( int scanline )
  {
    return (scanline & 7) == screenOffsetY();
  }



  /*
   * Inherit Javadoc.
   */
  public Dimension getPreferredSize()
  {
    return _componentsSize;
  }



  /*
   * Inherit Javadoc.
   */
  public Dimension getMinimumSize()
  {
    return getPreferredSize();
  }



  /**
   * Handles the component paint.  The implementation only has the
   * responsibility to compute the graphics object that is used for screen
   * updating.  Actual screen updating occurs in an internal thread, not here.
   *
   * @param g The graphics object to use for painting.
   * @see java.awt.Component#paint(java.awt.Graphics)
   */
  public void paint( Graphics g )
  {
    Graphics oldGfx = _graphics;
    // Since the graphics object passed into this method will be disposed()
    // by the ui subsystem, we create one for our own usage.
    _graphics = getGraphics();

    if ( oldGfx != null )
      oldGfx.dispose();
    else
      // We had no previous graphics object, so this must be the very first
      // paint.  Start the raster thread.
      _clockId.reschedule();
  }
}
