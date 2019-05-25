/* $Id: Commodore64.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (http://www.gnu.org/copyleft/gpl.html)
 * Copyright © 2000-2006 Michael G. Binz
 */
package de.michab.apps.route64;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import org.jdesktop.smack.MackActionManager;
import org.jdesktop.smack.MackAppEditor;
import org.jdesktop.smack.actions.MackBooleanPropertyAction;

import de.michab.apps.route64.actions.InputDeviceAction;
import de.michab.apps.route64.actions.MemoryDisplay;
import de.michab.apps.route64.actions.Monitor;
import de.michab.apps.route64.actions.ResetAction;
import de.michab.apps.route64.actions.RestoreSizeAction;
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
    extends MackAppEditor<SystemFile, Component>
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



  /**
   * This implements and glues together the GUI for the emulator.
   */
  private Commodore64()
  {
      super( SystemFile.class );
  }



  /**
   * Creates the application's toolbar.
   */
  @Override
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
    am.addAction( new RestoreSizeAction( "ACT_ZOOMBACK", this ) );

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
  @Override
protected void initialize( final String[] argv )
  {
      super.initialize( argv );

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



  /**
   *
   */
  @Override
protected void startup()
  {
//      setMainFrame( new RoundFrame() );
    _emulator = new C64Core();
    _emulator.addPropertyChangeListener(
      C64Core.IMAGE_NAME,
      _imageChangeListener );

    _loadComponent = new LoadComponent(
            _emulator );

    super.startup();

    _emulator.start();

    if ( _argv.length > 0 )
    {
      _emulator.setImageFile( _imageFile );
    }

    if ( _argv.length > 1 )
      _emulator.load( _argv[1].getBytes() );
  }



  /**
   * Application launch.
   *
   * @param argv The command line arguments.
   */
  public static void main( String[] argv )
  {
      launch( Commodore64.class, argv );
  }



  /**
   * Get a reference to the internal emulator engine.
   */
  C64Core getEmulatorEngine()
  {
    return _emulator;
  }



  /**
   * Listens to changes of the image file.  The image file title is displayed
   * in the message bar.
   */
  private java.beans.PropertyChangeListener _imageChangeListener =
    new java.beans.PropertyChangeListener()
  {
    @Override
    public void propertyChange( java.beans.PropertyChangeEvent pc )
    {
      if ( pc.getPropertyName() == C64Core.IMAGE_NAME )
      {
        SystemFile newImage = (SystemFile)pc.getNewValue();

        getStatusBar().setMessage( newImage.getName() );
        _loadComponent.setDirectoryEntries( _emulator.getImageFileDirectory() );
      }
    }
  };



    /* (non-Javadoc)
     * @see de.michab.mack.MackApplication#createMainComponent()
     */
    @Override
    protected Component createMainComponent()
    {
        return _emulator.getDisplay();
    }



    /* (non-Javadoc)
     * @see de.michab.mack.MackApplication#load(FT[])
     */
    @Override
    public void load( SystemFile[] files )
    {
        if ( files.length > 0 )
            _emulator.setImageFile( files[0] );
    }
}
