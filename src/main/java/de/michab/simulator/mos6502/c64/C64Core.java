/* $Id: C64Core.java 403 2010-09-12 00:30:24Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license
 * Copyright © 2000-2020 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;


import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import de.michab.simulator.Chip;
import de.michab.simulator.Clock;
import de.michab.simulator.Forwarder;
import de.michab.simulator.Memory;
import de.michab.simulator.Processor;
import de.michab.simulator.mos6502.Cia;
import de.michab.simulator.mos6502.Cpu6510;
import de.michab.simulator.mos6502.Sid;
import de.michab.simulator.mos6502.Vic;



/**
 * <p>A facade to a single instance of a Commodore 64.  Years ago that cost
 * $1000, today only a constructor is needed.  The <code>KeyListener</code>
 * implemented by this class has to be connected to the component receiving the
 * emulation's keyboard input.</p>
 *
 * <p>The display property provides a component that represents the Commodore
 * 64's video screen.  This has to be displayed in a GUI environment.</p>
 *
 * <p>As soon as the minimum setup -- connect of the KeyListener and display of
 * the display component -- has been done a call to the <code>start()</code>
 * method starts the emulation.</p>
 *
 * @version $Revision: 403 $
 * @author Michael G. Binz
 */
public final class C64Core
  implements
    KeyListener, MouseListener
{
    public final static String IMAGE_NAME = "imageNameProperty";

    /**
     * The supported input devices.
     */
    public enum InputDevice
    {
        KEYBOARD,
        JOYSTICK_0,
        JOYSTICK_1
    };



    /**
     * Used for implementing bound properties.
     */
    private PropertyChangeSupport _pcs = new PropertyChangeSupport( this );



    /**
     * Position of the color ram.  This is non-moveable.
     */
    public static final int ADR_COLOR_RAM_NEW = 0xd800;



    private final Clock _systemClock =
            new Clock( C64Core.PAL_TICKS_PER_SEC ); // 5 );



    /**
     * Main memory.
     */
    private final C64Memory _memory;



    /**
     * System cpu.
     */
    private final Cpu6510 _processor;
    private static final int PROCESSOR_BASE = 0x0000;



    /**
     * Keyboard
     */
    private Keyboard _keyboard;



    /**
     * Joystick 1
     */
    private Joystick _joystick0 = null;



    /**
     * Joystick 2
     */
    private Joystick _joystick1 = null;



    /**
     * Video chip.
     */
    private final Vic _vic;
    private static final int VIC_BASE = 0xd000;



    /**
     * This C64's CIA 1.
     */
    private final Cia _cia1;
    private static final int CIA1_BASE = 0xdc00;



    /**
     * This C64's CIA 2.
     */
    private final Cia _cia2;
    private static final int CIA2_BASE = 0xdd00;



    /**
     * Sound chip.
     */
    private final Sid _sid;
    private static final int SID_BASE = 0xd400;



    /**
     * @see LoadDevice
     */
    private LoadDevice _ld;



    /**
     * @see SystemInput
     */
    private SystemInput _systemInput;



    /**
     *
     */
    private KeyListener _currentKeyListener = null;



    /**
     *
     */
    public static final int PAL_TICKS_PER_SEC = 980000;



    /**
     *
     */
    public static final int NTSC_TICKS_PER_SEC = 1000000;



    /**
     * Creates an instance of a Commodore 64.  Note that the thread priority
     * of the calling thread is used as a reference priority.  This means that
     * other threads that are created to control the contained chips are
     * placed on priority levels relative to the one of the calling thread.
     */
    public C64Core()
    {
        // Create the 64's memory.
        _memory = new C64Memory();

        // Create a processor.
        _processor = new Cpu6510( _memory, _systemClock );
        _memory.mapInto( _processor, PROCESSOR_BASE );
        // Connect the memory to the processor's port 1.
        _processor.setPortListener( 1, _memory.getAddress1Listener() );

        // Create the SID.
        _sid = new Sid();
        _memory.mapInto( _sid, SID_BASE );

        // Create the VIC.
        // The VIC registers are repeated each 64 bytes in the area $d000-$d3ff,
        // i.e. register 0 appears on addresses $d000, $d040, $d080 etc.
        _vic = new C64Vic( _processor, _memory, ADR_COLOR_RAM_NEW, _systemClock );
        for ( int i = VIC_BASE ; i < 0xd3ff ; i += 0x40 )
            _memory.mapInto( _vic, i );

        // Create the CIAs;
        _cia1 = new Cia( _processor, _systemClock );
        _memory.mapInto( _cia1, CIA1_BASE );
        _memory.mapInto( _cia1, CIA1_BASE + 0x10 );

        _cia2 = new Cia( _processor, _systemClock );
        _memory.mapInto( _cia2, CIA2_BASE );
        // Connect the two least significant bits of cia2's port a to the video
        // chip base address.  Bits are low active.
        _cia2.connectPortA( new Forwarder(){

            @Override
            public void write( byte value )
            {
                _vic.setPageAddress( ~value );
            }
            @Override
            public byte read()
            {
                return (byte)~_vic.getPageAddress();
            }
        } );

        setInputDevice( InputDevice.KEYBOARD );

        // Finally add extensions
        addExtensions();
    }



    /**
     * Check if the passed file is a valid image file.
     *
     * @param file The file to check.
     */
    public boolean isImageFileValid( File file )
    {
        return _ld.isValid( file );
    }

    /**
     * Attaches a file to the emulator.  This is the bound property
     * <code>IMAGE_NAME</code>.
     *
     * @param file The file to attach.
     */
    public void setImageFile( File file )
            throws IOException
    {
        var oldFile = _ld.getFile();

        // If the old file name differs from the new one...
        if ( oldFile == null || !oldFile.equals( file ) )
        {
            // ...set the new one...
            _ld.setFile( file );
            // ...and fire a change event in case of success.
            _pcs.firePropertyChange( IMAGE_NAME, oldFile, file );
        }
    }

    /**
     * Get the currently attached image file.  Bound property IMAGE_NAME.
     *
     * @return The currently attached image file.  Returns <code>null</code> if
     *         no image file is attached.
     */
    public File getImageFile()
    {
        return _ld.getFile();
    }

    /**
     * Returns the directory of the currently loaded image file.  If none is
     * loaded null is returned.  Note that this is the directory structure
     * *inside* the image file, not the directory that holds the image file
     * itself.  The array entries contain the valid input for the load() method.
     *
     * @return The file names in the directory.
     * @see C64Core#load
     */
    public byte[][] getImageFileDirectory()
    {
        return _ld.getDirectory();
    }

    /**
     * Load and start a program.  Note that an image file has to be already been
     * loaded.  Calling this method is equivalent to entering
     * <pre><code>
     *   LOAD "fileName",8,1<br>
     *   RUN <br>
     * </code></pre>
     * on the 64's command line.  Note that the ",8" part is auto detected.
     *
     * @param fileName The name of the file to load in CBM ASCII.  This file has
     *        to be contained in the currently set image file.  The name passed
     *        in here should also be part of the directory list of the image
     *        file.  If the file can't be found a C64 error message is printed.
     * @see C64Core#setImageFile(SystemFile)
     * @see C64Core#getImageFileDirectory()
     */
    public void load( byte[] fileName )
    {
        // Build the basic command as a string...
        StringBuffer buffer = new StringBuffer();
        buffer.append( "LOAD\"" );
        buffer.append( new String( fileName ) );
        buffer.append( "\"," );
        buffer.append( _ld.getDeviceNumber() );
        buffer.append( ",1\rRUN\r" );

        // ...and write that into the 64's keyboard input buffer.
        _systemInput.writeInput( buffer.toString().getBytes() );
    }

    /**
     * Add all the extensions into the simulation.  This means all the patches
     * that replace a standard piece of functionality.  An example is the file
     * loader extension.  This completely replaces the original load logic.
     */
    private void addExtensions()
    {
        // Add image loading extensions.
        _ld = new LoadDevice( _processor, _memory );
        _memory.mapInto( _ld, _ld.getBaseAddress() );

        // Add the external write port into the 64's key buffer.
        _systemInput = new SystemInput( _memory );
        _memory.mapInto( _systemInput, _systemInput.getBaseAddress() );
    }

    /**
     * Reset the emulation.
     *
     * @param hard A module is detected if the memory location 0x8004 and
     *             following hold the string 'CBM80' in Commodore ASCII.  If this
     *             is the case a reset results in a JMP($8000) which was used by
     *             many games to get reset save.  Passing <code>true</code> here
     *             results in a reset even in case a module marker exists.
     */
    public void reset( boolean hard )
    {
        // The following line prevents module autostart if a hard reset was
        // requested.
        if ( hard )
            _memory.write( 0x8004, (byte)0 );

        _memory.reset();

        _processor.reset();
    }

    /**
     * Performs a soft reset.
     *
     * @see #reset(boolean)
     */
    public void reset()
    {
        reset( false );
    }

    /**
     * Shutdown the emulator and release all resources held.  It is not possible
     * to restart after <code>shutdown()</code> was called.
     */
    public void shutdown()
    {
        _vic.terminate();
    }

    /**
     * Starts execution of the system.
     */
    public void start()
    {
        _systemClock.start();
    }

    /**
     * Returns a reference to the emulation's video interface chip (aka VIC).
     */
    public Chip getVic()
    {
        return _vic;
    }

    /**
     * Returns a reference to the emulation's sound interface device (aka SID).
     */
    public Chip getSid()
    {
        return _sid;
    }

    /**
     * Returns a reference to the emulation's pair of CIA chips.
     */
    public Chip[] getCia()
    {
        return new Chip[]{ _cia1, _cia2 };
    }

    /**
     * Returns a reference to the emulation's CPU.
     */
    public Processor getCpu()
    {
        return _processor;
    }

    /**
     * Get a reference to the emulation's memory.
     */
    public Memory getMemory()
    {
        return _memory;
    }

    private Component _display = null;

    /**
     * Returns a reference on the component that the display is drawn into.
     *
     * @return The hot component containing the emulation's raster screen.
     */
    public Component getDisplay()
    {
        if ( _display == null )
        {
            _display = _vic.getComponent();
            _display.addKeyListener( this );
            _display.addMouseListener( this );
        }

        return _display;
    }

    /**
     * Returns the frame color as set in the C64's VIC chip.  The returned
     * color can be used for advanced embedding of the display component in a
     * user interface.
     *
     * @return The current frame color.
     */
    public Color getFrameColor()
    {
        if ( _vic != null )
        {
            return _vic.getExteriorColor();
        }
        return Color.black;
    }

    /**
     * Selects an input device.
     *
     * @param device One of the {@link InputDevice} enumeration elements.
     */
    public void setInputDevice( InputDevice device )
    {
        switch ( device )
        {
        case JOYSTICK_0:
        {
            if ( _joystick0 == null )
                _joystick0 = new Joystick( _keyboard, 1 );

            _cia1.connectPortB( null );
            _cia1.connectPortA( _joystick0 );
            _joystick0.setListener( _cia1.getInputPortA() );
            _currentKeyListener = _joystick0;
            break;
        }

        case JOYSTICK_1:
        {
            if ( _joystick1 == null )
                _joystick1 = new Joystick(  _keyboard, 2 );

            _cia1.connectPortA( null );
            _cia1.connectPortB( _joystick1 );
            _joystick1.setListener( _cia1.getInputPortB() );
            _currentKeyListener = _joystick1;
            break;
        }

        case KEYBOARD:
        {
            if ( _keyboard == null )
                _keyboard = new Keyboard( _processor );

            // Connect to CIA 1 Port A which is the hardware input...
            _cia1.connectPortA( _keyboard );
            _cia1.connectPortB( null );

            // ...and to CIA 1 Port B which is the hardware output.
            _keyboard.setListener( _cia1.getInputPortB() );

            _currentKeyListener = _keyboard;
            break;
        }

        default:
            throw new InternalError( "Unexpected input device: " + device );
        }
    }

    /**
     * Check whether sound is enabled.
     *
     * @return <code>true</code> if sound is enabled, <code>false</code>
     *         otherwise.
     */
    public boolean isSoundOn()
    {
        return _sid.isSoundOn();
    }

    /**
     * Switch sound on or off.
     *
     * @param what <code>true</code> to switch sound on, <code>false</code>
     *        otherwise.
     */
    public void setSoundOn( boolean what )
    {
        _sid.setSoundOn( what );
    }

    /**
     * Adds a property change listener to this bean.
     */
    public void addPropertyChangeListener(
            String name,
            PropertyChangeListener pcl )
    {
        _pcs.addPropertyChangeListener( name, pcl );
    }

    /**
     * Remove a property change listener from this bean.
     */
    public void removePropertyChangeListener(
            String name,
            PropertyChangeListener pcl )
    {
        _pcs.removePropertyChangeListener( name,  pcl );
    }

    /*
     * Inherit javadoc.
     */
    @Override
    public void keyTyped(KeyEvent e)
    {
        _currentKeyListener.keyTyped( e );
    }

    /*
     * Inherit javadoc.
     */
    @Override
    public void keyPressed(KeyEvent e)
    {
        _currentKeyListener.keyPressed( e );
    }

    /*
     * Inherit javadoc.
     */
    @Override
    public void keyReleased(KeyEvent e)
    {
        _currentKeyListener.keyReleased( e );
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseClicked( MouseEvent e )
    {
        ((Component)e.getSource()).requestFocusInWindow();
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseEntered( MouseEvent e )
    {
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseExited( MouseEvent e )
    {
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    @Override
    public void mousePressed( MouseEvent e )
    {
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseReleased( MouseEvent e )
    {
    }
}
