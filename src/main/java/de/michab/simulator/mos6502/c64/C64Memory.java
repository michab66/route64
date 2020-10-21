/* $Id: C64Memory.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;

import de.michab.simulator.*;
import java.io.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Models a Commmodore 64's memory.  Emulates ROM/RAM/IO and memory banking.
 *
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
final class C64Memory
  implements
    Memory
{
  // The logger for this class.
  private final static Logger _log = 
    Logger.getLogger( C64Memory.class.getName() );



  /**
   * Constants for the possible memory configurations.
   */
  private static final int MAP_11 = 0x03; // 0000 0011
  private static final int MAP_10 = 0x02; // 0000 0010
//  private static final int MAP_01 = 0x01; // 0000 0001
  private static final int MAP_00 = 0x00; // 0000 0000



  /**
   * The path name to the package containing the ROM images.
   */
  private static final String ROM_PACKAGE_POSITION
    = "de/michab/simulator/mos6502/c64/roms/";



  /**
   * Start address of the kernel ROM.  For unknown reasons in the Commodore
   * community 'kernel' is spelled with 'a'.  Let's follow that tradition.
   */
  public static final int ADR_KERNAL = 0xe000;



  /**
   * Start address of the BASIC ROM.
   */
  public static final int ADR_BASIC =  0xa000;



  /**
   * Start address of the character ROM.
   */
  public static final int ADR_CHAR =   0xd000;



  /**
   * Start address of the IO page.
   */
  private static final int ADR_IO = ADR_CHAR;



  /**
   * An array representing the 64k of ram memory byte-wise.
   */
  private final byte[] _memory = new byte[ 0xffff +1 ];



  /**
   * The famous address 1.
   */
  private int _address1;



  /**
   * The 64s lo rom area
   */
  private static final byte[] _loRom =
    readResource( ROM_PACKAGE_POSITION + "BASIC.ROM", 0x2000 );



  /**
   * The 64s hi rom area.
   */
  private static final byte[] _hiRom =
    readResource( ROM_PACKAGE_POSITION + "KERNAL.ROM", 0x2000 );



  /**
   * The character rom.
   */
  private static final byte[] _charRom =
    readResource( ROM_PACKAGE_POSITION + "CHAR.ROM", 0x1000 );



  /**
   * An array of ports that exists in parallel to the memory array.  On each
   * memory access this array will be checked and if a non-null entry is found
   * for the given memory address that is forwarded to the port.
   */
  private final Forwarder[] _ports;



  /**
   * Create one.  Nuff said.
   */
  public C64Memory()
  {
    // Allocate and initialise the port array.
    _ports  = new Forwarder[ _memory.length ];
    Arrays.fill( _ports, null );

    reset();
  }



  /**
   * Reset the memory to its initial state.
   */
  public synchronized void reset()
  {
    // Initialise address 1.
    _address1 = MAP_11 | Processor.BIT_2;
    // Init ram.
    mapIntoRam( _charRom, ADR_CHAR );
  }



  /**
   * Returns a listener being interested in the processor port 1.
   *
   * @return A <code>Forwarder</code> tp address 1.
   */
  public synchronized Forwarder getAddress1Listener()
  {
    return new Forwarder(){
      public synchronized byte read()
      {
        return (byte)_address1;
      }
      public synchronized void write( byte value )
      {
        _address1 = value;
      }
    };
  }



  /**
   * Read the given memory location.  If a port is mapped for this location
   * then this one is read instead.
   *
   * @param location The address of the memory location to read.
   * @return The byte set at this memory address.
   */
  public synchronized byte read( int location )
  {
    int map = _address1 & MAP_11;
    boolean charen = (_address1 & Processor.BIT_2) == 0;

    if ( location < ADR_BASIC )
      return readRam( location );
    else if ( location >= ADR_IO && location < ADR_IO + 0x1000 )
      return readIo( location, map, charen );
    else if ( location >= ADR_BASIC && location < ADR_BASIC + 0x2000 )
      return readBasicRom( location, map );
    else if ( location >= ADR_KERNAL )
      return readKernalRom( location, map );
    else
      return readRam( location );
  }



  /**
   * Write a value to a memory address.  If a port is mapped for this address
   * the value is written to the port instead.
   *
   * @param location The address to write.
   * @param value The value to write.
   */
  public synchronized void write( int location, byte value )
  {
    int map = _address1 & MAP_11;
    boolean charen = (_address1 & Processor.BIT_2) == 0;

    // If the access is in the IO page.
    if ( location < ADR_BASIC )
      writeRam( location, value );
    else if ( location >= ADR_IO && location < ADR_IO + 0x1000 )
      writeIo( location, value, map, charen );
    else if ( location >= ADR_BASIC && location < ADR_BASIC + 0x2000 )
      writeBasicRom( location, value, map );
    else if ( location >= ADR_KERNAL )
      writeKernalRom( location, value, map );
    else
      writeRam( location, value );
  }



  /*
   * Inherit docs.
   */
  public synchronized int getVectorAt( int address )
  {
    int hi = read( address+1 );
    hi &= 0xff;
    int lo = read( address );
    lo &= 0xff;
    return (hi << 8) | lo;
  }



  /**
   * Writes into the RAM area.
   *
   * @param adr The address to write.
   * @param value The value to write.
   */
  private void writeRam( int adr, byte value )
  {
    Forwarder p = _ports[ adr ];

    if ( p != null )
      p.write( value );
    else
      _memory[ adr ] = value;
  }



  /**
   * Reads from the RAM area.
   *
   * @param adr The address to read.
   */
  private byte readRam( int adr )
  {
    Forwarder p = _ports[ adr ];

    if ( p != null )
      return p.read();
    
    return _memory[ adr ];
  }



  /**
   * Writes into the BASIC ROM area.  Depending on the memory map settings
   * the ports or the RAM sitting on the same addresses will be written.
   *
   * @param adr The address to write to.
   * @param value The value to write.
   * @param map The memory map setting.
   */
  private void writeBasicRom( int adr, byte value, int map )
  {
    Forwarder port = _ports[ adr ];

    if ( map == MAP_11 && port != null )
      port.write( value );
    else
      _memory[adr] = value;
  }



  /**
   *
   */
  private byte readBasicRom( int adr, int map )
  {
    Forwarder port = _ports[ adr ];

    switch ( map )
    {
      case MAP_11:
      {
        if ( null != port )
          return port.read();
        return _loRom[ adr - ADR_BASIC ];
      }

      default:
        return _memory[ adr ];
    }
  }



  /**
   *
   */
  private void writeKernalRom( int adr, byte value, int map )
  {
    Forwarder port = _ports[ adr ];

    switch ( map )
    {
      case MAP_11:
      case MAP_10:
        if ( null != port )
          port.write( value );
        else
          _memory[ adr ] = value;
        break;

      default:
        _memory[ adr ] = value;
        break;
    }
  }



  /**
   *
   */
  private byte readKernalRom( int adr, int map )
  {
    byte result;

    Forwarder port = _ports[ adr ];

    switch ( map )
    {
      case MAP_11:
      case MAP_10:
        if ( null != port )
          result = port.read();
        else
          result = _hiRom[ adr - ADR_KERNAL ];
        break;

      default:
        result = _memory[ adr ];
        break;
    }

    return result;
  }



  /**
   * Implements a write in the IO/CHAR page.
   */
  private void writeIo( int adr, byte value, int map, boolean charen )
  {
    switch ( map )
    {
      case MAP_00:
        _memory[ adr ] = value;
        break;

      default:
        if ( charen )
          _memory[ adr ] = value;
        else if ( _ports[ adr ] != null )
          _ports[ adr ].write( value );
        // TODO:  this is needed because we don't have a dedicated color
        // ram so far.
        else
          _memory[ adr ] = value;
    }
  }



  /**
   * Implements a read in the IO/CHAR page.
   *
   * @param adr The address to read.
   * @param map The memory map settings.
   * @param charen Whether character ROM is visible or not.
   * @return The read byte.
   */
  private byte readIo( int adr, int map, boolean charen )
  {
    switch ( map )
    {
      case MAP_00:
        return _memory[ adr ];

      default:
        if ( charen )
          return _charRom[ adr - ADR_CHAR ];
        else if ( _ports[ adr ] != null )
          return _ports[ adr ].read();
        else
          return _memory[ adr ];
    }
  }



  /*
   * Inherit documentation.
   */
  public synchronized byte[] getRawMemory()
  {
    return _memory;
  }



  /**
   * Map the passed <code>Chip</code> into memory.  This means that the
   * <code>Chip</code>'s registers replace the existing memory cells starting
   * with the one specified by the <code>base</code> argument.  Note that the
   * same <code>Chip</code> instance can be mapped to several base addresses.
   *
   * @param chip The <code>Chip</code> instance to be mapped.
   * @param base The base address for the <code>Chip</code>.
   */
  public synchronized void mapInto( Chip chip, int base )
  {
    // Get the chip's ports...
    Forwarder[] theChipsPorts = chip.getPorts();
    // ...and copy them into our port array.
    System.arraycopy( theChipsPorts, 0, _ports, base, theChipsPorts.length );
  }



  /**
   * Set a <code>Forwarder</code> to the passed position.
   *
   * @param f The forwarder to set.
   * @param idx The port index for the forwarder.
   */
  public synchronized void set( Forwarder f, int idx )
  {
    if ( _ports[ idx ] != null )
      _log.warning( "Port override @ idx " + idx );
    _ports[ idx ] = f;
  }



  /*
   * Inherit Javadoc.
   */
  public int getSize()
  {
    return _memory.length;
  }



  /**
   * Maps the passed resource file into memory.  This is used to initialise
   * RAM areas from existing memory dumps.  Note that it has to be possible to
   * map the passed memory into RAM at that position, no range checking is
   * performed.
   *
   * @param memoryToMap A byte array containing the memory to map.
   * @param address The address where the memory is mapped to.
   * @exception ArrayIndexOutOfBoundsException If the passed memory doesn't fit.
   */
  private void mapIntoRam( byte[] memoryToMap, int address )
  {
    System.arraycopy( memoryToMap, 0, _memory, address, memoryToMap.length );
  }



  /**
   * Creates a byte array and reads the resource for the passed name into it.
   * This is used for reading the ROM contents.
   *
   * @return A byte array containing the resource data.
   * @param resourceName The name of the resource to be read.
   * @param expectedSize The size in bytes that should be available in the
   *                     resource file.
   */
  private static byte[] readResource( String resourceName, int expectedSize )
  {
    byte[] result = null;

    try
    {
      // Get a stream for the requested resource.
      InputStream is = Memory.class.getClassLoader().
        getResourceAsStream( resourceName );
      // Check if we had success.
      if ( is == null )
        throw new FileNotFoundException( resourceName );

      DataInputStream dis = new DataInputStream( is );

      // Create an array in the size expected from the rom file.
      result = new byte[ expectedSize ];

      dis.readFully( result );
      dis.close();
    }
    catch ( EOFException e )
    {
      _log.severe( "Fatal error on reading " + resourceName );
      _log.severe( "Expected " + expectedSize + ", received less." );
      System.exit( 1 );
    }
    catch ( Exception e )
    {
      _log.log( Level.SEVERE, e.getMessage(), e );
      System.exit( 1 );
    }

    return result;
  }
}
