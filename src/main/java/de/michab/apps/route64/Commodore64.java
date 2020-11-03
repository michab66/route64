/* $Id: Commodore64.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (http://www.gnu.org/copyleft/gpl.html)
 * Copyright © 2000-2020 Michael G. Binz
 */
package de.michab.apps.route64;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import de.michab.apps.route64.actions.ResetAction;
import de.michab.simulator.mos6502.c64.C64Core;



/**
 * Implementation of a graphical user interface on top of the emulator.
 *
 * @version $Revision: 782 $
 * @author Michael G. Binz
 */
public final class Commodore64
{
    private static Logger LOG = Logger.getLogger(
            Commodore64.class.getName() );

    /**
     * The actual emulator instance tied to this UI.
     */
    private final C64Core _emulator =
            new C64Core();

    /**
     * The original command line arguments.
     */
    private String[] _argv;

    /**
     * The quick-load component on the toolbar.
     */
    private LoadComponent _loadComponent =
            new LoadComponent( _emulator );
    private final JFrame _mainFrame =
            new JFrame( getClass().getSimpleName() );
    private final JToolBar _toolbar =
            new JToolBar();

    /**
     * Implements and glues together the UI of the emulator.
     */
    private Commodore64()
    {
        _toolbar.setFloatable( false );

        // Since our main window holds a heavyweight component, we do not want the
        // menu items to be lightweight, appearing behind the windows content.
        JPopupMenu.setDefaultLightWeightPopupEnabled(
                false );
        // The same goes with the tool tips.
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(
                false );
        _mainFrame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE );
        _mainFrame.getContentPane().add(
                _toolbar,
                BorderLayout.NORTH );
        _mainFrame.getContentPane().add(
                _loadComponent,
                BorderLayout.SOUTH );
    }

    /**
     * Populate the application's toolbar.
     */
    private void addActions( JToolBar am )
    {
        am.add(
                new ResetAction( _emulator ) );
//        am.add(
//                new Monitor(
//                        (Cpu6510)_emulator.getCpu(),
//                        this ) );

        JComboBox<C64Core.InputDevice> combo =
                new JComboBox<C64Core.InputDevice>(
                        C64Core.InputDevice.values() );
        combo.setSelectedIndex( 0 );
        combo.addActionListener( (e) -> {
            var selectedIndex = combo.getSelectedIndex();
            if ( selectedIndex < 0 )
                return;
            _emulator.setInputDevice(
                    combo.getItemAt( selectedIndex ) );
        });
        am.add( combo );

        _emulator.setSoundOn( false );
    }

    private boolean filterFile( File f )
    {
        if ( f.isDirectory() )
            return true;

        var path = f.getPath().toLowerCase();

        if ( path.endsWith( ".d64" ) )
            return false;
        if ( path.endsWith( ".t64" ) )
            return false;
        if ( path.endsWith( ".p00" ) )
            return false;
        return true;
    }

    public void imageFileChanged( PropertyChangeEvent evt )
    {
        File imageFile = (File)evt.getNewValue();

        _mainFrame.setTitle(
                String.format(
                        "%s : %s",
                        getClass().getSimpleName(),
                        imageFile.getName() ) );
    }

    /**
     * Start the thing -- will this ever fly??
     */
    private void initialize( final String[] argv )
    {
        _argv = argv;

        _emulator.addPropertyChangeListener(
              C64Core.IMAGE_NAME,
              this::imageFileChanged );

        addActions( _toolbar );
        _emulator.start();

        if ( argv.length > 0 ) try
        {
            _emulator.setImageFile(
                    new File( argv[0] ) );
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

        if ( _argv.length > 1 )
            _emulator.load( _argv[1].getBytes() );

        _mainFrame.getContentPane().add(
                _emulator.getDisplay(),
                BorderLayout.CENTER );

        // Add drag and drop loading.
        new DropHandler(
                _mainFrame,
                f -> _loadComponent.load( f ) )
            .setFilter( this::filterFile );

        _mainFrame.pack();
        _mainFrame.setVisible( true );
    }

    /**
     *
     */
    private static void initLogging()
    {
        try ( var loggingProperties =
                Commodore64.class.getClassLoader().getResourceAsStream(
                        "logging.properties") )
        {
            if ( loggingProperties == null )
            {
                LOG.warning( "No logging.properties found." );
                return;
            }

            LogManager.getLogManager().readConfiguration(
                    loggingProperties );
        }
        catch ( Exception e )
        {
            LOG.log( Level.WARNING, "Error reading logging.properties.", e );
        }
    }

    /**
     * Application launch.
     *
     * @param argv The command line arguments.
     */
    public static void main( String[] argv )
    {
        initLogging();

        SwingUtilities.invokeLater(
                () -> new Commodore64().initialize( argv ) );
    }
}
