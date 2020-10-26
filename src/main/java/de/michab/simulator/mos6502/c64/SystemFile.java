/* $Id: SystemFile.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;



/**
 * Represents a file system file object.  This is a value object used as a
 * file abstraction that can be used also in a WebStart environment.
 *
 * @version $Revision: 782 $
 */
public class SystemFile
{
    /**
     * This <code>SystemFile</code>'s name.
     */
    private String _name;



    /**
     * This <code>SystemFile</code>'s contents.
     */
    private final byte[] _contents;



    /**
     * Convenience constructor.
     *
     * @param file The file to read from.
     */
    public SystemFile( File file )
            throws IOException
    {
        _name = file.getName();

        try ( FileInputStream fips = new FileInputStream( file ) )
        {
            _contents = readContent(
                    fips,
                    (int)file.length() );
        }
    }



    /**
     * Create a system file.  Note that this is less efficient than the
     * constructor offering also a <code>size</code> argument.
     *
     * @param name  The name to be displayed.
     * @param is An input stream used for reading the content.
     * @throws IOException In case of io errors.
     * @see SystemFile#SystemFile(String, InputStream, int)
     */
    public SystemFile( String name, InputStream is )
            throws IOException
    {
        _name = name;
        _contents = readContent( is );
    }



    /**
     * Create a system file.
     *
     * @param name  The name to be displayed.
     * @param is An input stream used for reading the content.
     * @param size The expected number of bytes to be read.
     * @throws IOException In case of io errors.
     * @see SystemFile#SystemFile(String, InputStream )
     */
    public SystemFile( String name, InputStream is, int size )
            throws IOException
    {
        _name = name;
        _contents = readContent( is, size );
    }



    /**
     * Returns the name of this system file for display purposes.
     */
    public String getName()
    {
        return _name;
    }



    /**
     * Get the length of the system file.
     */
    public int getLength()
    {
        return _contents.length;
    }



    /**
     * Get the file's content.
     */
    public byte[] getContent()
    {
        return _contents;
    }



    /**
     * Read the contents of a file with known size.
     *
     * @param fips The stream to read from.
     * @param len The number of bytes that are expected on the stream.
     * @throws EOFException In case less than the expected <code>len</code>
     *         number of characters were available on the stream.
     */
    private static byte[] readContent( InputStream fips, int len )
            throws IOException
    {
        // Init our result buffer...
        byte[] result = new byte[ len ];
        // ...and slurp the file into it.
        DataInputStream dis = new DataInputStream( fips );
        dis.readFully( result );
        return result;
    }



    /**
     * A buffer size used reading file with unknown size.
     */
    private static final int CHUNK_SIZE = 1024;



    /**
     * Reads the content of the passed input stream into a byte array.  Note that
     * it should be made sure that the input stream contains a limited size of
     * bytes.
     *
     * @param is The stream to read.
     * @return An array holding the content from the passed stream.
     */
    private static byte[] readContent( InputStream is )
            throws
            IOException
    {
        // A container for the read chunks.
        Vector<byte[]> chunks = new Vector<byte[]>();

        while ( true )
        {
            // Allocate a new chunk.
            byte[] buffer = new byte[ CHUNK_SIZE ];
            // Fill the chunk from the stream.
            for ( int i = 0 ; i < buffer.length ; i++ )
            {
                // Get a byte.
                int currentByte = is.read();
                // Check for end of file.
                if ( currentByte == -1 )
                {
                    // Reached end of file.  Calculate size of result buffer...
                    int resultSize =
                            (chunks.size() * CHUNK_SIZE) +
                            i;
                    // ...and allocate the according result buffer.
                    byte [] result = new byte[ resultSize ];
                    // Copy the existing chunks into the result and...
                    for ( int j = 0 ; j < chunks.size() ; j++ )
                    {
                        byte[] currentChunk = chunks.get( j );
                        System.arraycopy(
                                currentChunk,
                                0,
                                result,
                                j * CHUNK_SIZE,
                                CHUNK_SIZE );
                    }
                    // ... add the content of the final and maybe smaller chunk.
                    System.arraycopy(
                            buffer,
                            0,
                            result,
                            chunks.size() * CHUNK_SIZE,
                            i );
                    // Ready and leave.
                    return result;
                }

                // We got a valid byte, put it into the chunk.
                buffer[ i ] = (byte)currentByte;
            }
            // The chunk is filled up.  Add it to the set of chunks read so far.
            chunks.add( buffer );
        }
    }
}
