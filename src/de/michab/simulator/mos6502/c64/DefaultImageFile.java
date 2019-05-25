/* $Id: DefaultImageFile.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2003 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;



/**
 * A default implementation of the ImageFile interface.  This should be able
 * to support all image files to come, so no other implementations should be
 * necessary.
 * Put all file format specific processing into the image file factory.
 *
 * @see ImageFile
 * @see ImageFileFactory
 */
class DefaultImageFile
  implements
    ImageFile
{
  /**
   * The image file's raw content.
   */
  private final byte[] _image;



  /**
   * A reference to the image file factory in charge.
   */
  private final ImageFileFactory _imageFileFactory;



  /**
   * The file that was used to load the image.
   *
   * @see DefaultImageFile#getFilename()
   */
  private String _filename;



  /**
   * Create an instance.
   *
   * @param iff The factory to use to decode the contents.
   * @param name The name of the file for display purposes.
   * @param contents The actual image.
   */
  DefaultImageFile( ImageFileFactory iff, String name, byte[] contents )
  {
    _imageFileFactory = iff;
    _filename = name;
    _image = contents;
  }



  /**
   * <p>Returns a file name for the encapsulated image file.  Note that this
   * does <i>not</i> have to be a name of a valid file and must only be used
   * for display purposes.</p>
   *
   * <p>An example where the filename would be invalid is if the file was
   * loaded via a WebStart client.</p>
   */
  public String getFilename()
  {
    return _filename;
  }



  /**
   * Returns a Commodore IEC bus device number.  This should represent the
   * device number that is commonly used for the encapsulated image file.  In
   * case of the Commodore 64's '.d64' image files this would be 8, since a
   * d64 file represents a floppy disk, which in turn normally has the number
   * 8 as a device number.  The returned number is used to generate 'right'
   * LOAD statements but this is basically only aesthetical, whatever number
   * is returned by this operation, this should not lead to technical problems.
   */
  public int getDeviceNumber()
  {
    return _imageFileFactory.getDeviceNumber();
  }



  /**
   * Access the image file's directory.
   */
  public byte[][] getDirectory()
  {
    return _imageFileFactory.getDirectory( _filename, _image );
  }



  /*
   * ImageFile#loadDirectoryEntry(byte[])
   */
  public byte[] loadDirectoryEntry( byte[] fileName )
  {
    return _imageFileFactory.loadEntry( fileName, _image );
  }
}
