/* $Id: LoadDevice.java 412 2010-09-27 17:22:58Z Michael $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2020 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;

import java.io.File;
import java.io.IOException;

import de.michab.simulator.Memory;
import de.michab.simulator.mos6502.Cpu6510;
import de.michab.simulator.mos6502.Extension;



/**
 * An extension responsible for patching the load logic into the emulator.  Is
 * patched onto the 64 ROM's load vector at 0xffd5.  This code replaces the
 * entire load logic including disk and tape loading.
 *
 * @see de.michab.simulator.mos6502.c64.ImageFile
 * @see de.michab.simulator.mos6502.c64.ImageFileFactory
 * @version $Revision: 412 $
 * @author Michael Binz
 */
class LoadDevice
extends
Extension
{
    // These are the Commodore IO error codes.
    public static final int C64_ERROR_BREAK = 0;
    public static final int C64_TOO_MANY_FILES = 1;
    public static final int C64_FILE_OPEN = 2;
    public static final int C64_FILE_NOT_OPEN = 3;
    public static final int C64_FILE_NOT_FOUND = 4;
    public static final int C64_DEVICE_NOT_PRESENT = 5;
    public static final int C64_NOT_INPUT_FILE = 6;
    public static final int C64_NOT_OUTPUT_FILE = 7;
    public static final int C64_MISSING_FILE_NAME = 8;
    public static final int C64_ILLEGAL_DEVICE_NUMBER = 9;



    /**
     * The address whose execution will trigger this extension.
     */
    private static final int LOAD_VECTOR = 0xffd5;



    /**
     * The processor responsible for activating this extension.
     */
    private final Cpu6510 _processor;



    /**
     * The input file that is read.
     */
    private ImageFile _imageFile = null;



    /**
     * The name of the currently loaded image file.
     */
    private File _file = null;



    /**
     * The available file format factories.  TODO has to be configurable or
     * fully automatic.
     */
    private final ImageFileFactory[] _factories = new ImageFileFactory[]
            {
                    new AdaptD64(),
                    new AdaptT64(),
                    new AdaptP00(),
                    new AdaptPrg()
            };



    /**
     * Create an instance.
     *
     * @param p The emulation's processor.
     * @param m The emulation's memory.
     */
    public LoadDevice( Cpu6510 p, Memory m )
    {
        super( m );
        _processor = p;
    }



    /*
     * @see Extension#getBaseAddress
     */
    @Override
    public int getBaseAddress()
    {
        return LOAD_VECTOR;
    }



    /**
     * The main entry point of this extension.
     */
    @Override
    public void extensionCalled( Memory m )
    {
        int secondaryAddress = getSecondaryAddress( m );

        // Check if the format adapter is properly initialised.
        if ( _imageFile == null )
        {
            postLoadFailed( C64_DEVICE_NOT_PRESENT );
            return;
        }

        // Try to load the image.
        byte[] image = _imageFile.loadDirectoryEntry( getLoadName( m ) );

        // Check if we got an image.
        if ( null != image && image.length >= 2 )
        {
            // Yes, load was successful.  Compute the memory target address.
            int destinationAddress;

            // If none or a zero secondary address was given...
            if ( secondaryAddress == 0 )
            {
                // ...load to the position specified in the x/y registers.
                int hi = _processor.getY();
                int lo = _processor.getX();
                destinationAddress = (hi << 8) | lo;
            }
            else
            {
                // In the other case load to the address specified by the first byte
                // pair in the image.
                int lo = image[0] & 0xff;
                int hi = image[1] & 0xff;
                destinationAddress = (hi << 8) | lo;
            }

            // Now perform the copy.  Copying starts at index 2 since the first two
            // bytes are occupied by the load address (see above).
            for ( int i = 2 ; i < image.length ; i++ )
                m.write( destinationAddress++, image[i] );
            // Set the end address in x/y registers.
            --destinationAddress;
            _processor.setX( destinationAddress  );
            _processor.setY( destinationAddress>>8 );

            // Set registers and memory for success.
            postLoadSuccess();
        }
        else
        {
            postLoadFailed( C64_FILE_NOT_FOUND );
        }
    }



    /**
     * Looks up an ImageFileFactory that is able to handle the passed file.  If
     * no factory is found <code>null</code> is returned.
     * @param f The file we need a factory for handling.
     * @return A factory that can handle the passed file.  If no factory is found
     *         <code>null</code> is returned.
     */
    private ImageFileFactory findFactoryFor( File f )
    {
        for ( int i = 0 ; i < _factories.length ; i++ )
        {
            if ( _factories[i].isValid( f ) )
                return _factories[i];
        }
        return null;
    }



    /**
     * Check if the passed file can be loaded.
     */
    boolean isValid( File f )
    {
        // See if we can find a factory for loading.
        return null != findFactoryFor( f );
    }

    /**
     * This sets the name of the file that should be used to read image data
     * from.  Note that this has nothing to do with the file name that is given
     * on the 64's command line.
     */
    void setFile( File f )
            throws IOException
    {
        ImageFileFactory factory = findFactoryFor( f );

        if ( factory == null )
            throw new IOException( "Type not supported." );

        _imageFile = factory.create( f );

        _file = f;
    }

    /**
     * Returns the directory of the currently set image file.  If none is set a
     * null is returned.  Note that this returns the directory of the files
     * contained *inside* the image file.
     */
    byte[][] getDirectory()
    {
        byte[][] result = null;

        if ( _imageFile != null )
            result = _imageFile.getDirectory();

        return result;
    }



    /**
     * Returns the logical device number of the attached image file.  If no image
     * file is currently is attached, 8 -- the number for the floppy disk -- is
     * returned
     */
    int getDeviceNumber()
    {
        // Default is the disk device number.
        int result = 8;

        if ( _imageFile != null )
            result = _imageFile.getDeviceNumber();

        return result;
    }



    /**
     * Returns the currently attached image file.
     *
     * @return The currently attached image file or <code>null</code> if no image
     *         file is attached.
     */
    File getFile()
    {
        return _file;
    }



    /**
     * Called in case a load succeeded.
     */
    private void postLoadSuccess()
    {
        int status = _processor.getStatusRegister();

        // Delete the carry flag...
        status &= (~Cpu6510.STATUS_FLAG_CARRY);
        // ...and set the new status register.
        _processor.setStatusRegister( (byte)status );
    }



    /**
     * Called in case a load failed.  Provides the needed error settings to the
     * 64's operation system.  Currently triggers a file not found error.
     */
    private void postLoadFailed( int errorCode )
    {
        // Set the carry flag.
        int status = _processor.getStatusRegister();
        status |= Cpu6510.STATUS_FLAG_CARRY;
        _processor.setStatusRegister( (byte)status );

        // Set the error code.
        _processor.setAccu( errorCode );
    }



    /**
     * Read the name of the file to load.  The implementation uses c64 zeropage
     * addresses 0xb7 (length of name) and 0xbb/bc (pointer to name).
     */
    private byte[] getLoadName( Memory m )
    {
        int length = m.read( 0xb7 );
        byte[] result = new byte[ length ];

        System.arraycopy( m.getRawMemory(),
                m.getVectorAt( 0xbb ),
                result,
                0,
                length );

        return result;
    }



    /**
     * Get the secondary address for the file to load.  Implementation uses
     * zeropage address 0xb9 (secondary address).
     */
    private int getSecondaryAddress( Memory m )
    {
        return m.read( 0xb9 );
    }
}
