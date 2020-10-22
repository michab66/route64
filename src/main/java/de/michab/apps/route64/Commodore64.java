/* $Id: Commodore64.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (http://www.gnu.org/copyleft/gpl.html)
 * Copyright © 2000-2006 Michael G. Binz
 */
package de.michab.apps.route64;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import org.jdesktop.smack.MackActionManager;
import org.jdesktop.smack.actions.MackBooleanPropertyAction;

import de.michab.apps.route64.actions.InputDeviceAction;
import de.michab.apps.route64.actions.MemoryDisplay;
import de.michab.apps.route64.actions.Monitor;
import de.michab.apps.route64.actions.ResetAction;
import de.michab.simulator.mos6502.Cpu6510;
import de.michab.simulator.mos6502.c64.C64Core;
import de.michab.simulator.mos6502.c64.SystemFile;



/**
 * Implementation of a graphical user interface on top of the emulator.
 *
 * @version $Revision: 782 $
 * @author Michael G. Binz
 */
public final class Commodore64
{
    /**
     * The actual emulator instance tied to this UI.
     */
    private C64Core _emulator;



    /**
     * The original command line arguments.
     */
    private String[] _argv;



    /**
     * The quick-load component on the toolbar.
     */
    private LoadComponent _loadComponent;



    private final JFrame _mainFrame;

    private final JToolBar _toolbar =
            new JToolBar();

    /**
     * This implements and glues together the GUI for the emulator.
     */
    private Commodore64()
    {
        _mainFrame = new JFrame( getClass().getSimpleName() );
        _mainFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        _mainFrame.setPreferredSize( new Dimension( 400,  300 ) );
        _mainFrame.getContentPane().add( _toolbar, BorderLayout.NORTH );
    }



    /**
     * Creates the application's toolbar.
     */
    protected void addActions( MackActionManager am )
    {
        // Actually add all the actions...
        am.addAction(
                new ResetAction( _emulator ) );
        am.addAction(
                new Monitor(
                        (Cpu6510)_emulator.getCpu(),
                        this ) );

        am.addAction( _loadComponent );
        am.addAction(
                new InputDeviceAction(
                        "ACT_JOYSTICK_ONE",
                        _emulator,
                        C64Core.InputDevice.JOYSTICK_0,
                        false ) );
        am.addAction(
                new InputDeviceAction(
                        "ACT_JOYSTICK_TWO",
                        _emulator,
                        C64Core.InputDevice.JOYSTICK_1,
                        false ) );
        am.addAction(
                new InputDeviceAction(
                        "ACT_KEYBOARD",
                        _emulator,
                        C64Core.InputDevice.KEYBOARD,
                        true ) );
        //    am.addAction( new RestoreSizeAction( "ACT_ZOOMBACK", this ) );

        am.addAction(
                new MackBooleanPropertyAction(
                        "ACT_SOUND", "soundOn", _emulator, false ) );
        am.addAction(
                new MemoryDisplay( _emulator.getMemory(), this ) );
    }



    /**
     *
     */
    private SystemFile _imageFile = null;



    /**
     * Start the thing -- will this ever fly??
     */
    private void initialize( final String[] argv )
    {
        //      super.initialize( argv );

        _argv = argv;

        //    Utilities.setSystemLookAndFeel();

        // Since our main window holds a heavyweight component, we do not want the
        // menu items to be lightweight, appearing behind the windows content.
        JPopupMenu.setDefaultLightWeightPopupEnabled( false );
        // The same goes with the tool tips.
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled( false );

        if ( argv.length > 0 ) try
        {
            _imageFile = new SystemFile( new File( argv[0] ) );
        }
        catch ( IOException e )
        {
            JOptionPane.showMessageDialog(
                    null,
                    "Fatal: Could not read file: '" +
                            argv[0] +
                    "'" );
            System.exit( 1 );
        }
    }

    private void startup()
    {
        //      setMainFrame( new RoundFrame() );
        _emulator = new C64Core();
        //    _emulator.addPropertyChangeListener(
        //      C64Core.IMAGE_NAME,
        //      _imageChangeListener );

//        _loadComponent = new LoadComponent(
//                _emulator );

        //   super.startup();

        _emulator.start();

        if ( _argv.length > 0 )
        {
            _emulator.setImageFile( _imageFile );
        }

        if ( _argv.length > 1 )
            _emulator.load( _argv[1].getBytes() );

        _mainFrame.getContentPane().add(
                createMainComponent(),
                BorderLayout.CENTER );

        _mainFrame.pack();
        _mainFrame.setVisible( true );
    }

    static private void launch( String[] argv )
    {
        var c64 = new Commodore64();

        c64.initialize( argv );

        c64.startup();
    }

    /**
     * Application launch.
     *
     * @param argv The command line arguments.
     */
    public static void main( String[] argv )
    {
        SwingUtilities.invokeLater(
                () -> launch( argv ) );
    }

    /**
     * Get a reference to the internal emulator engine.
     */
    private C64Core getEmulatorEngine()
    {
        return _emulator;
    }



    //  /**
    //   * Listens to changes of the image file.  The image file title is displayed
    //   * in the message bar.
    //   */
    //  private java.beans.PropertyChangeListener _imageChangeListener =
    //    new java.beans.PropertyChangeListener()
    //  {
    //    @Override
    //    public void propertyChange( java.beans.PropertyChangeEvent pc )
    //    {
    //      if ( pc.getPropertyName() == C64Core.IMAGE_NAME )
    //      {
    //        SystemFile newImage = (SystemFile)pc.getNewValue();
    //
    //        getStatusBar().setMessage( newImage.getName() );
    //        _loadComponent.setDirectoryEntries( _emulator.getImageFileDirectory() );
    //      }
    //    }
    //  };



    /* (non-Javadoc)
     * @see de.michab.mack.MackApplication#createMainComponent()
     */
    private Component createMainComponent()
    {
        return _emulator.getDisplay();
    }



    //    /* (non-Javadoc)
    //     * @see de.michab.mack.MackApplication#load(FT[])
    //     */
    //    @Override
    //    public void load( SystemFile[] files )
    //    {
    //        if ( files.length > 0 )
    //            _emulator.setImageFile( files[0] );
    //    }
}
