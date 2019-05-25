/* $Id: ImageFile.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2003 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;



/**
 * Encapsulates an image file which can be used by an emulation to read a file
 * contained in that image.
 *
 * @see de.michab.simulator.mos6502.c64.ImageFileFactory
 * @author Michael G. Binz
 */
interface ImageFile
{
  /**
   * Returns a file name for the encapsulated image file.  Note that this does
   * <i>not</i> have to be a name of a valid file and must only be used for
   * display purposes.
   */
  public String getFilename();



  /**
   * Returns a Commodore IEC bus device number.  This should represent the
   * device number that is commonly used for the encapsulated image file.  In
   * case of the Commodore 64's '.d64' image files this would be 8, since a
   * d64 file represents a floppy disk, which in turn normally has the number
   * 8 as a device number.  The returned number is used to generate 'right'
   * LOAD statements but this is basically only aesthetical, whatever number
   * is returned by this operation, this should not lead to technical problems.
   */
  public int getDeviceNumber();



  /**
   * Access the image file's directory.  Note that this is the directory
   * structure <i>inside</i> the image file, not the directory that holds the
   * image file.
   */
  public byte[][] getDirectory();



  /**
   * <p>Returns the image for the specified file name.  It is up to the adapter
   * to support an empty or null name.  A null return array signals that the
   * specified file could not be found.</p>
   *
   * <p>The first two bytes in the returned array designate a memory address
   * where the file should be loaded to when the secondary load address was not
   * zero (as in <code>LOAD "X",8,1</code>).  If the secondary address was not
   * specified (as in <code>LOAD "X",8</code>) or was zero (as in
   * <code>LOAD "X",8,0</code>) then the file will be loaded into BASIC
   * memory.</p>
   *
   * @param fileName The name of the image to return.  Null may be passed to get
   *             the default image.  It is recommended to always implement
   *             directory access if a name "$" is specified.
   * @return The image as to be mapped into memory.  A null return value means
   *         that the image wasn't found.
   */
  public byte[] loadDirectoryEntry( byte[] fileName );
}
