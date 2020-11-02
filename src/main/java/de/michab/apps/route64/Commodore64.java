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
import java.io.File;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

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
    private final JToolBar _toolbarBottom =
            new JToolBar();

    /**
     * This implements and glues together the UI of the emulator.
     */
    private Commodore64()
    {
        _toolbar.setFloatable( false );
        _toolbarBottom.setFloatable( false );

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
                _toolbarBottom,
                BorderLayout.SOUTH );
    }

    /**
     * Populate the application's toolbars.
     */
    private void addActions( JToolBar am, JToolBar bottom )
    {
        am.add(
                new ResetAction( _emulator ) );
        am.add(
                new Monitor(
                        (Cpu6510)_emulator.getCpu(),
                        this ) );

        bottom.add( _loadComponent );

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

    private void load( File f )
    {
        try
        {
            _emulator.setImageFile(
                    new SystemFile( f ) );
            var dir = _emulator.getImageFileDirectory();
            _loadComponent.setDirectoryEntries( dir );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    private boolean filterFile( File f )
    {
        if ( f.isDirectory() )
            return true;
        if ( f.getPath().endsWith( ".d64" ) )
            return false;
        if ( f.getPath().endsWith( ".t64" ) )
            return false;
        if ( f.getPath().endsWith( ".p00" ) )
            return false;
        return true;
    }

    /**
     * Start the thing -- will this ever fly??
     */
    private void initialize( final String[] argv )
    {
        _argv = argv;
        //      setMainFrame( new RoundFrame() );
        //    _emulator.addPropertyChangeListener(
        //      C64Core.IMAGE_NAME,
        //      _imageChangeListener );

        addActions( _toolbar, _toolbarBottom );
        _emulator.start();

        if ( argv.length > 0 ) try
        {
            _emulator.setImageFile(
                    new SystemFile( new File( argv[0] ) ) );
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
                createMainComponent(),
                BorderLayout.CENTER );

        // Add drag and drop loading.
        new DropHandler(
                _mainFrame,
                f -> load( f ) )
            .setFilter( this::filterFile );

        _mainFrame.pack();
        _mainFrame.setVisible( true );
    }

    static private void launch( String[] argv )
    {
        var c64 = new Commodore64();

        c64.initialize( argv );
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
