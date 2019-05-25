/* $Id: ImageFileFactory.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;



/**
 * <p>Represents the base class to specialise for a file format that the 
 * emulator can handle.  Only a single instance of that specialisation will be 
 * created, being responsible for handling all files of the corresponding
 * type.</p>
 * 
 * <p>An <code>ImageFileFactory</code> encapsulates the <i>algorithm</i> or 
 * services needed to read a file format, while the <code>ImageFile</code>
 * holds the actual data that is available in the files.</p>
 *
 * <p>Most of the methods in ImageFile implementations should delegate to an
 * ImageFileFactory.  This helps to keep all file format related functionality
 * in a single place and allows one ImageFile implementation that is used for
 * many file types.</p>
 *
 * @see de.michab.simulator.mos6502.c64.ImageFile
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
abstract class ImageFileFactory
{
  /**
   * Holds a human presentable name for this image format or null.
   */
  private final String _description;



  /**
   * Holds the suffix for this type of file.
   */
  private final String _suffix;



  /**
   * Holds the C64 device id that is represented by this type of image.
   */
  private final int _deviceId;



  /**
   * Creates a rudimentary <code>ImageFileFactory</code> with some attributes 
   * initialised.
   * 
   * @param description A description of this <code>ImageFileFactory</code>.
   * @param suffix The file suffix this <code>ImageFileFactory</code> can 
   *        handle.
   * @param deviceId The Commodore device identifier that this
   *        <code>ImageFileFactory</code> normally represents.  For example
   *        this would be 8 for '.d64' files, since this format represents a
   *        floppy (which has the 8 identifier on a Commodore 64).  Note that
   *        this is only for documentation purposes, a wrong value here does
   *        not result in malfunction of the emulator.
   */
  protected ImageFileFactory( String description, String suffix, int deviceId )
  {
    _description = description;
    _suffix = suffix.toLowerCase();
    _deviceId = deviceId;
  }



  /**
   * Get the a name for the represented type of image file that can be
   * displayed in the user interface.
   * 
   * @return A description of this <code>ImageFileFactory</code>.
   */
  public String getDescription()
  {
    return _description;
  }



  /**
   * Get a file name suffix for the represented type of image file.  If there
   * is no commonly used file name suffix return null.
   * 
   * @return The suffix of files that can be handled by this
   *         <code>ImageFileFactory</code>.
   */
  public String getFilenameSuffix()
  {
    return _suffix;
  }



  /**
   * Checks if the passed file is a valid file of the type that is represented
   * by this interface.  This default implementation checks if the file is
   * readable, exists and if the filename ends with the filename suffix.
   */
  protected boolean isValid( SystemFile imageFile )
  {
    String lowerName = imageFile.getName().toLowerCase();

    return
      lowerName.endsWith( getFilenameSuffix() );
  }



  /**
   * Creates an ImageFile from a file.  The created image file encapsulates the
   * passed file and allows access to the files contained in the image.
   */
  public ImageFile create( SystemFile file )
  {
    // Init our embedded buffer...
    byte[] image = file.getContent();
    if ( image == null )
    {
      System.err.println( "Failed to load: " + file.getName() );
      return null;
    }

    return new DefaultImageFile( this, file.getName(), image );
  }



  /**
   * Returns the device number for the given format.
   *
   * @see ImageFile#getDeviceNumber()
   */
  public int getDeviceNumber()
  {
    return _deviceId;
  }



  /**
   * Get the directory from the passed image.  One of the
   * <code>getDirectory()</code> operations has to be overridden by the 
   * implementation of a file format.
   *
   * @param imageFile The content of the image file.
   * @return The directory of the image file.
   * @see ImageFileFactory#getDirectory(String, byte[])
   * @throws InternalError If neither this nor the other 
   * <code>getDirectory()</code> operation is overridden.
   */
  public byte[][] getDirectory( byte[] imageFile )
  {
    throw new InternalError( "getDirectory()" );
  }



  /**
   * Get the directory from the passed image.  The name of the file that the
   * image data was loaded from is also available here.  This can be overridden
   * for file formats where the directory content depends on the name of the
   * original image file in the host file system of the emulator.
   * 
   * @param fileName The name of the image file.
   * @param imageFile The content of the image file.
   * @return The directory of the image file.
   * @see ImageFileFactory#getDirectory(byte[])
   */
  public byte[][] getDirectory( String fileName, byte[] imageFile )
  {
    return getDirectory( imageFile );
  }



  /**
   * @see ImageFile#loadDirectoryEntry(byte[])
   */
  public abstract byte[] loadEntry( byte[] name, byte[] imageFile );



  /**
   * This implements the 64s filename comparison algorithm.  This includes the
   * '*' and '?' wildcards.  File names are equal
   * o if they contain the same bytes and have the same length.
   * o if they contain the same bytes upto the position where the pattern has
   *   an asterisk.
   * o if they differ only in byte positions where the pattern contains a '?'.
   */
  protected boolean namesEqual( byte[] fullname, byte[] pattern )
  {
    // With empty patterns we accept everything.
    if ( pattern.length == 0 )
      return true;

    for ( int i = 0 ; i < pattern.length ; i++ )
    {
      if ( pattern[i] == '*' )
        return true;
      else if ( pattern[i] == '?' )
        ;
      else if ( (fullname.length > i) &&  (fullname[i] == pattern[i]) )
        ;
      else
        return false;
    }

    // Finally check if the lengths differ,  content is equal up to pattern
    // length.
    return fullname.length == pattern.length;
  }



  /**
   * Returns the 16 bit word at the passed offset.
   * 
   * @param offset The offset to be read.
   * @param image The image to be read.
   * @return The word at the specified position as an integer.  Only the lower
   *         16 bit of the integer are used, the upper 16 bit are zero.
   */
  public static int getWordAt( int offset, byte[] image )
  {
    int hi = image[ offset+1 ] & 0xff;
    hi <<= 8;
    int lo = image[ offset ] & 0xff;

    return hi | lo;
  }



  /**
   * Returns the 32 bit double word at the passed offset.
   *
   * @param offset The offset to be read.
   * @param image The image to be read.
   * @return The double word at the specified position as an integer.
   */
  public static int getDwordAt( int offset, byte[] image )
  {
    int hi16 = getWordAt( offset + 2, image ) & 0xffff;
    hi16 <<= 16;
    int lo16 = getWordAt( offset, image ) & 0xffff;
    return hi16 | lo16;
  }



  /**
   * Takes a byte array and removes all leading and trailing occurences of the
   * specified byte value from the array.
   *
   * @param array The original array.
   * @param toStrip The byte value to strip.
   * @return An array with the stripped bytes removed.  This array is
   *         potentially smaller than the input array.  In case no bytes where
   *         stripped, this is a reference to the input array in other cases
   *         the result array is newly allocated.
   */
  public static final byte[] stripBytes( byte[] array, byte toStrip )
  {
    return stripBytes( array, 0, array.length-1, toStrip );
  }



  /**
   * Takes a byte array subset and removes all leading and trailing occurences
   * of the specified byte value from the array.
   *
   * @param array The array containing the data to strip.
   * @param startIdx The start index into the array.
   * @param endIdx The end index into the array.
   * @param toStrip The character that is to strip.
   * @return A newly allocated array that is of size end index minus start
   *         index minus number of leading and trailing strip characters.
   */
  public static final byte[] stripBytes(
    byte[] array,
    int startIdx,
    int endIdx,
    byte toStrip )
  {
    if ( startIdx > endIdx || endIdx > (array.length-1) || startIdx < 0 )
      throw new IllegalArgumentException();

    // Find the index of the first character in the result string.
    for ( ; startIdx  <= endIdx && toStrip == array[startIdx] ; startIdx++ )
      ;
    // Find the index of the last character in the result string.
    for ( ; endIdx >= startIdx && toStrip == array[endIdx] ; endIdx-- )
      ;

    byte[] result = new byte[ (endIdx - startIdx) +1 ];
    System.arraycopy( array, startIdx, result, 0, result.length );
    return result;
  }
}
