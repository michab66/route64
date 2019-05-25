/* $Id: ScanlineRasterer.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;



/**
 * Hides the internals of a video mode or a related raster operation like
 * sprite rastering.
 *
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
interface ScanlineRasterer
{
  /**
   * Called when drawing of a new frame begins.  Note that this does not have
   * to be a full screen.  If interrupt logic switched graphics modes in the
   * middle of a screen drawing operation, then this will be called also.
   */
  public void startFrame( int characterAdr, int videoRamAdr, int bitmapAdr );



  /**
   * Gets called if the current line is a 'bad line'.  A bad line is a raster
   * line where the VIC has to re-read character set information from main
   * memory.  This is <i>bad</i> because this requires more processor cycles 
   * than normally available for the VIC and thus will block the CPU.
   *
   * @param currentScanline The number of the bad scanline.
   */
  public void badLine( int currentScanline );
  


  /**
   * Gets called for a single raster line.  Has to raster its information into
   * the array window that is defined by the passed parameters.  The values
   * that are written into this array are actual RGB values.
   * Bits that are to be rastered with background color must not be touched
   * in the implementation.  This is needed to allow layered sprite rastering.
   *
   * @param scanlineOffset The start offset for this operation.
   * @param scanline The actual scanline that has to be rastered.  Should be
   *                 used for optimisation purposes.
   */
  public void rasterInto( int scanlineOffset, int scanline );



  /**
   * Has to initialise the background. Is called for each raster line before
   * anything else is done.
   *
   * @param scanlineOffset The start offset for this operation.
   */
  public void backfill( int scanlineOffset );
  
  
  
  /**
   * Returns an VIC color code used to encode the graphics mode in the display
   * frame.
   *
   * @return An VIC color code.
   */
  public int getDebugColor();
}
