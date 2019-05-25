/* $Id: AdaptT64.java 412 2010-09-27 17:22:58Z Michael $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;

import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Loader for the .t64 image format.
 *
 * @author Michael G. Binz
 */
class AdaptT64
  extends
    ImageFileFactory
{
    private final static Logger _log =
        Logger.getLogger( AdaptT64.class.getName() );

    private final static Level _chipLogLevel =
        Level.FINE;

  private static final int DIR_OFFSET = 0x40;
  private static final int DIR_ENTRY_SIZE = 32;
  // Directory entry offsets
  private static final int DE_START_ADDRESS = 0x02;
  private static final int DE_START_ADDRESS_LO = 0x02;
  private static final int DE_START_ADDRESS_HI = 0x03;
  private static final int DE_END_ADDRESS = 0x04;
  //private static final int DE_END_ADDRESS_LO = 0x04;
  //private static final int DE_END_ADDRESS_HI = 0x05;
  private static final int DE_IMAGE_OFFSET = 0x08;



  /**
   * Creates an ImageFileFactory for the .64 format.
   */
  public AdaptT64()
  {
    super( "Tape image", "t64", 1 );
  }



  /**
   * Return the directory for the passed image.
   *
   * @param image The byte image.
   * @see ImageFileFactory#getDirectory( byte[] )
   */
  public byte[][] getDirectory( byte[] image )
  {
    byte[][] result = new byte[ getUsedDirEntries( image ) ][];

    for ( int i = result.length-1 ; i >= 0 ; i-- )
      result[i] = getEntryName( i, image );

    return result;
  }



  /**
   * Loads the named entry from the image file.
   *
   * @param name The name of the file to read.
   * @param image The image to read from.
   * @return The requested file.
   * @see ImageFileFactory#loadEntry( byte[], byte[] )
   */
  public byte[] loadEntry( byte[] name, byte[] image )
  {
    byte[][] dir = getDirectory( image );

    for ( int i = 0 ; i < dir.length ; i++ )
    {
      if ( namesEqual( dir[i], name ) )
        return loadDirEntry( i, image );
    }

    return null;
  }



  /**
   * Loads the directory entry at the given index.
   */
  static private byte[] loadDirEntry( int dirEntryIdx, byte[] image )
  {
    int currentDirEntryOffset = getDirEntryOffset( dirEntryIdx );

    // Compute the size for the result array.  This is load end address minus
    // load start address plus two bytes for the target address that has to be
    // included in the first two bytes of the returned array (see description
    // of FormatAdapter#getImage().
    int startAdr =
      getWordAt( currentDirEntryOffset + DE_START_ADDRESS, image );
    int endAdr =
      getWordAt( currentDirEntryOffset + DE_END_ADDRESS, image );

    // Create the result array.
    byte[] result = new byte[ 2 + (endAdr - startAdr) ];

    // Write the target load address.
    result[0] = image[ currentDirEntryOffset + DE_START_ADDRESS_LO ];
    result[1] = image[ currentDirEntryOffset + DE_START_ADDRESS_HI ];

    // Fill in the load image.
    int fromOffset =
      getDwordAt( currentDirEntryOffset + DE_IMAGE_OFFSET, image );
    // This is special processing of a known error -- see t64.txt.
    if ( endAdr == 0xc3c6 )
    {
      _log.log( _chipLogLevel, "SpecialHandling..." );
      byte[] newResult = new byte[ 2 + (image.length - fromOffset) ];
      newResult[0] = result[0];
      newResult[1] = result[1];
      _log.log( _chipLogLevel, "t64: old.len == " + result.length  );
      _log.log( _chipLogLevel, "t64: new.len == " + newResult.length  );
      result = newResult;
    }
    System.arraycopy( image, fromOffset, result, 2, result.length - 2 );

    return result;
  }



  /**
   * Returns the name of a directory entry.  Removes pad bytes.
   *
   * @param idx The idx of the directory entry.  This has to be smaller than
   *            what getDirectorySize() delivers.
   * @param image The file image.
   */
  private static byte[] getEntryName( int idx, byte[] image )
  {
    int nameStart = getDirEntryOffset( idx ) + 0x10;
    int nameEnd = nameStart + 0x0f;

    return stripBytes( image, nameStart, nameEnd, (byte)0x20 );
  }



  /**
   * Returns the number of directory entries.  The code implements a fix for a
   * problem that several t64 files had: The number of used dir entries returns
   * zero but there is really (at least) a single directory entry.  So in case
   * the image reports zero used dir entries a one is returned.
   *
   * @param image The file image.
   * @return The number of used diretory entries.
   */
  private static int getUsedDirEntries( byte[] image )
  {
    int result = getWordAt( 0x24, image );

    // In case no entries are used...
    if ( result == 0 )
      // ...expect that at least one is really used and the information is
      // wrong.
      result =  1;

    return result;
  }



  /**
   * Compute the offset for the passed dir entry index.
   */
  private static int getDirEntryOffset( int idx )
  {
    return DIR_OFFSET + (idx * DIR_ENTRY_SIZE);
  }
}
