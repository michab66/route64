/* $Id: AdaptD64.java 412 2010-09-27 17:22:58Z Michael $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 *
 * Implementation depends on the file d64.txt contained in formats.zip.  This
 * is written by TODO and available from www.TODO...
 */
package de.michab.simulator.mos6502.c64;

import java.io.File;
import java.util.Vector;



/**
 * Models a rudimentary 1541 needed to read the d64 file type.  Currently only
 * read access is supported.  Write access could be at least possible.
 *
 * @author Michael Binz
 */
final class AdaptD64
  extends
    ImageFileFactory
{
  /**
   * This is the expected bytesize for the file that is read initially.  Used
   * for validation.
   */
  private static final int MAGIC_D64_FILESIZE = 174848;



  /**
   * Size of a sector.
   */
  private static final int RAW_SECTOR_LENGTH = 256;



  /**
   * Size of net data contained in a sector (Two bytes less than
   * RAW_SECTOR_LENGTH, since these are used for sector chaining).
   */
  private static final int NET_SECTOR_LENGTH = RAW_SECTOR_LENGTH - 2;
  private static final int DIR_ENTRY_SIZE = 0x20;
  private static final int DIR_TYPE_OFFSET = 0x02;
  private static final int DIR_NAME_OFFSET = 0x05;
  private static final int DIR_SIZE_OFFSET = 0x1e;
  private static final int DIR_START_TRACK = 0x03;
  private static final int DIR_START_SECTOR = 0x04;


  /**
   * Create an Adapter for the passed d64 file.
   */
  public AdaptD64()
  {
    super( "D64 disk image", "d64", 8 );
  }



  /**
   * Checks if the image has the filesize that is defined for D64 files.
   *
   * @see ImageFileFactory#isValid
   */
  @Override
public boolean isValid( File f )
  {
    return
      // We perform the default validity tests...
      super.isValid( f ) &&
      // ...and check if the filesize is the one that is defined for files of
      // type D64.
      (f.length() == MAGIC_D64_FILESIZE);
  }



  /*
   * Inherit javadoc.
   */
  @Override
public byte[][] getDirectory( byte[] image )
  {
    byte[][] result = createDirectory( image );
    return result;
  }



  /*
   * Inherit javadoc.
   */
  @Override
public byte[] loadEntry( byte[] name, byte[] image )
  {
    int dirOffset = findDirEntryFor( name, image );
    if ( dirOffset == -1 )
      return null;

    return getFileImage( dirOffset, image );
  }



  /**
   * Returns the length of the file specified by the directory entry in bytes.
   */
  static private int getFileLength( int dirOffset, byte[] image )
  {
    // Get the number of sectors from the dir entry.
    int numSectors = dirSectorLength( dirOffset, image );
    // Get the first sector offset from the dir entry.
    int currentSector = getBlockOffset( dirStartTrack( dirOffset, image ),
                                        dirStartSector( dirOffset, image ) );

    int result = 0;

    // Run along the chain of sectors...
    while ( --numSectors > 0 )
    {
      // ...summing up the sizes along the way.
      currentSector = nextOffset( currentSector, image );
      result += NET_SECTOR_LENGTH;
    }

    // In a chain of file sectors, the last chaining pair has a track of zero
    // and a sector value that denotes the number of bytes used in the last
    // sector.  This is added here.
    if ( 0 == nextTrack( currentSector, image ) )
      result += nextSector( currentSector, image );
    else
      // Ending up here means that the file has a problem:  What should be the
      // last sector according to the dir entry has a valid (= non null)
      // pointer to the next sector.  So we accept the whole sector as filled.
      result += NET_SECTOR_LENGTH;

    return result;
  }



  /**
   * Return the specified c64 file as a byte array.  Note that this means a
   * file that is embedded in the image file--nothing that is visible in the
   * host operating system.
   */
  static private byte[] getFileImage( int dirOffset, byte[] image )
  {
    int numSectors = dirSectorLength( dirOffset, image );

    // Compute the size of the raw data we will return...
    int byteLength = getFileLength( dirOffset, image );
    // ...and allocate the result buffer accordingly.
    byte[] result = new byte[ byteLength ];

    // Get the first sector offset from the dir entry.
    int srcPosition = getBlockOffset( dirStartTrack( dirOffset, image ),
                                      dirStartSector( dirOffset, image ) );
    int dstPosition = 0;

    while ( numSectors-- > 0 )
    {
       // Compute the size of the chunk we have to copy.  Special case is the
       // last sector having a track pointer of zero and where the next sector
       // denotes the number of valid bytes within the current sector.
       int chunkSize = NET_SECTOR_LENGTH;
       if ( 0 == nextTrack( srcPosition, image ) )
         chunkSize = nextSector( srcPosition, image );

       // Do the copy.
       System.arraycopy( image,
                         srcPosition+2,
                         result,
                         dstPosition,
                         chunkSize );

       // Step the destination.
       dstPosition += NET_SECTOR_LENGTH;
       // Step to the next sector.
       srcPosition = nextOffset( srcPosition, image );
    }

    return result;
  }



  /**
   * Looks for a directory entry for the given name in the image file.
   *
   * @return The raw offset for a found directory entry or -1 if nothing was
   *         found.
   */
  private int findDirEntryFor( byte[] name, byte[] image )
  {
    // Track 18, sector 1 contains the dir entries.
    int track = 18;
    int sector = 1;
    // Range [0..7], denotes the current directory entry.
    int entryNum  = 0;
    int currentSector = 0;
    int currentDirEntry = 0;

    do
    {
      currentSector = getBlockOffset( track, sector );
      currentDirEntry = currentSector + (entryNum * DIR_ENTRY_SIZE);

      if ( namesEqual( getDirEntryName( currentDirEntry, image ), name ) )
        return currentDirEntry;

      // Compute the directory entry position for the next loop.

      // If we are not on the last dir entry...
      if ( entryNum < 8 )
        // ...go to the next one.
        entryNum++;
      else
      {
        // Go to the next directory sector.
        track = nextTrack( currentSector, image );
        sector = nextSector( currentSector, image );
        entryNum = 0;
      }

    } while ( track != 0 );

    // No success, return error code.
    return -1;
  }



  /**
   * Create a directory listing.
   *
   * @param image The file image to search.
   */
  private static byte[][] createDirectory( byte[] image )
  {
    Vector<byte[]> directory = new Vector<byte[]>();

    // Track 18, sector 1 contains the dir entries.
    int track = 18;
    int sector = 1;
    // Range [0..7], denotes the current directory entry.
    int entryNum  = 0;
    int currentSector = 0;
    int currentDirEntry = 0;

    do
    {
      currentSector = getBlockOffset( track, sector );
      currentDirEntry = currentSector + (entryNum * DIR_ENTRY_SIZE);

      if ( 0 == image[ currentDirEntry + DIR_TYPE_OFFSET ] )
        break;

      directory.add( getDirEntryName( currentDirEntry, image ) );

      // Compute the directory entry position for the next loop.

      // If we are not on the last dir entry...
      if ( entryNum < 8 )
        // ...go to the next one.
        entryNum++;
      else
      {
        // Go to the next directory sector.
        track = nextTrack( currentSector, image );
        sector = nextSector( currentSector, image );
        entryNum = 0;
      }
    } while ( track != 0 );

    byte[][] result = new byte[ directory.size() ][];
    for ( int i = 0 ; i < result.length ; i++ )
      result[i] = directory.elementAt( i );
    return result;
  }



  /**
   * Returns a directory entry's name without pad bytes.
   */
  static private byte[] getDirEntryName( int begin, byte[] image )
  {
    return stripBytes( image, begin + DIR_NAME_OFFSET, begin+15, (byte)0xa0 );
  }



  /**
   * Returns the length of the file described by the passed directory sector.
   *
   * @param directoryEntryOffset An integer offset pointing to a directory
   *        sector.
   * @param image The image to traverse.
   */
  static private int dirSectorLength( int directoryEntryOffset, byte[] image )
  {
    return getWordAt( directoryEntryOffset + DIR_SIZE_OFFSET, image );
  }



  /**
   * Get the start track out of a directory entry.
   *
   * @param directoryEntryOffset A directory entry offset.
   * @param image The image to traverse.
   */
  static private int dirStartTrack( int directoryEntryOffset, byte[] image )
  {
    return image[ directoryEntryOffset + DIR_START_TRACK ];
  }



  /**
   * Get the start sector out of a directory entry.
   *
   * @param directoryEntryOffset A directory entry offset.
   * @param image The image to traverse.
   */
  static private int dirStartSector( int directoryEntryOffset, byte[] image )
  {
    return image[ directoryEntryOffset + DIR_START_SECTOR ];
  }



  /**
   * Get the offset of the next sector in the chain.
   *
   * @param currentSector A valid sector offset.
   * @param image The image to traverse.
   */
  static private int nextOffset( int currentSector, byte[] image )
  {
    return getBlockOffset( nextTrack( currentSector, image ),
                           nextSector( currentSector, image ) );
  }



  /**
   * Returns the offset of the next track.
   *
   * @param image The image to traverse.
   */
  static private int nextTrack( int currentSector, byte[] image )
  {
    // Next track is on sector offset zero.
    return image[ currentSector ];
  }



  /**
   * Returns the next sector's offset.
   *
   * @param image The image to traverse.
   */
  static private int nextSector( int currentSector, byte[] image )
  {
    // Next sector is on sector offset one.
    return image[ currentSector +1 ] & 0xff;
  }



  /**
   * Computes the image offset from a track/sector number pair.
   *
   * @param track The target track.
   * @param sector The target sector.
   * @return The offset into the image to reach the passed track/sector.
   */
  static private int getBlockOffset( int track, int sector )
  {
    int result = sector;

    // This is an implementation of the track/sector mappings given in the
    // document referenced in the class comment.
    for ( int i = 1 ; i < track ; i++ )
    {
      if ( i <= 17 )
        result += 21;
      else if ( i <= 24 )
        result += 19;
      else if ( i <= 30 )
        result += 18;
      else
        result += 17;
    }

    // Result now contains the overall sector number.  Transform that into
    // a raw offset.
    return result * RAW_SECTOR_LENGTH;
  }
}
