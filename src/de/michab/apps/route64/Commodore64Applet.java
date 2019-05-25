/* $Id: Commodore64Applet.java 410 2010-09-27 16:10:47Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (http://www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2004-2005 Michael G. Binz
 */
package de.michab.apps.route64;

import java.applet.Applet;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.michab.simulator.mos6502.c64.C64Core;
import de.michab.simulator.mos6502.c64.SystemFile;



/**
 * Route 64 as an applet.
 *
 * @author Michael Binz
 */
public class Commodore64Applet
  extends
    Applet
{
  private static final long serialVersionUID = 2215694763328364563L;



  // The logger for this class.
  private static Logger log =
    Logger.getLogger( Commodore64Applet.class.getName() );



  /**
   * The emulation engine.
   */
  private C64Core _core = null;



  /**
   * Create an instance.
   */
  public Commodore64Applet()
  {
    log.fine( "Creating Applet." );
  }



  /*
   * @see java.applet.Applet#init()
   */
  public void init()
  {
    log.entering(
        Commodore64Applet.class.getName(),
        "init" );

    _core = new C64Core();
    add( _core.getDisplay() );
    addKeyListener( _core );

    log.exiting(
        Commodore64Applet.class.getName(),
        "init" );
  }



  /*
   * @see java.applet.Applet#destroy()
   */
  public void destroy()
  {
    log.entering(
        Commodore64Applet.class.getName(),
        "destroy()" );

    try
    {
      _core.shutdown();
      _core = null;
      _isStarted = false;
    }
    finally
    {
      log.exiting(
          Commodore64Applet.class.getName(),
          "destroy()" );
    }
  }



  /**
   * Flag to ensure that we only once start the emulation engine.
   */
  private boolean _isStarted = false;



  /*
   * @see java.applet.Applet#start()
   */
  public void start()
  {
    if ( _isStarted )
      return;

    _isStarted = true;

    _core.start();

    String imageName = getStringParameter( PARAM_IMAGE_FILE );
    String startName = getStringParameter( PARAM_START_NAME );
    int deviceId = getIntegerParameter( PARAM_DEVICE_ID );

    if ( imageName != null )
      loadImage( imageName, startName );
    if ( deviceId >= 0 )
      setInputDevice( deviceId );
  }



  /*
   * @see java.applet.Applet#getAppletInfo()
   */
  public String getAppletInfo()
  {
    return getClass().getName() + " $Name:  $";
  }



  /**
   * Allows to load a named image from JavaScript.
   *
   * @param imageName The name of the image file in the host file system.
   * @param fileName The name of the Commodore 64 file to load from the
   *        image.  If <code>null</code> then the image is attached and
   *        nothing is loaded.
   */
  public void loadImage( String imageName, String fileName )
  {
    log.entering( getClass().getName(), "loadImage()" );

    URL base = getCodeBase();

    try
    {
      base = new URL( base.toExternalForm() + imageName  );

      InputStream is = new BufferedInputStream( base.openStream() );
      SystemFile sf = new SystemFile( imageName, is );
      is.close();

      _core.setImageFile( sf );

      if ( fileName != null )
      {
        _core.load( fileName.getBytes() );
      }
    }
    catch ( IOException e )
    {
      log.log( Level.SEVERE, e.getMessage(), e );
    }
    finally
    {
      log.exiting( getClass().getName(), "loadImage()" );
    }
  }



  /**
   * Attach a named image from JavaScript.
   *
   * @param imageName The name of the image to load. This will be resolved
   *        against the code base URL.
   */
  public void loadImage( String imageName )
  {
    loadImage( imageName, null );
  }



  /**
   * Reset the emulation.  Note that a reset will not work if the program that
   * is executed in the emulation modified the reset vector.  While this is
   * authentic behaviour of the Commodore 64, we should add the ability to
   * perform a hard reset.
   */
  public void reset()
  {
    _core.reset();
  }



  /**
   * Set the input device to either the keyboard, joystick one, or joystick
   * two.
   *
   * @param device 0 = keyboard, 1 = joystick 1, 2 = joystick 2.
   */
  public void setInputDevice( int pDevice )
  {
    C64Core.InputDevice device = C64Core.InputDevice.KEYBOARD;

    if ( pDevice == 0 )
      device = C64Core.InputDevice.KEYBOARD;
    else if ( pDevice == 1 )
      device = C64Core.InputDevice.JOYSTICK_0;
    else if ( pDevice == 2 )
      device = C64Core.InputDevice.JOYSTICK_1;
    else
    {
      String msg =
        "Bad input device id: " + device;
      log.log( Level.WARNING, msg );
      showStatus( msg );
      return;
    }

    _core.setInputDevice( device );
  }



  public static final String PARAM_IMAGE_FILE = "image";
  public static final String PARAM_START_NAME = "start";
  public static final String PARAM_DEVICE_ID = "device";
  // TODO not implemented. Should grap focus on start.
  public static final String PARAM_GRAB_FOCUS = "grabFocus";
  // TODO not implemented.
  public static final String PARAM_COMMA_ONE  = "commaOne";



  /*
   * @see java.applet.Applet#getParameterInfo()
   */
  public String[][] getParameterInfo()
  {
    return new String[][]{
        { PARAM_IMAGE_FILE,
          "string",
          "Name of image file to attach." },
        { PARAM_START_NAME,
          "string",
          "Name of Commodore 64 binary to be loaded and started." },
        { PARAM_DEVICE_ID,
          "0 = keyboard, 1 = joystick 1, 2 = joystick 2",
          "Input device to be used." }
    };
  }



  /**
   * Access a string parameter.
   *
   * @param name The parameter name.
   * @return The parameter value as a non-empty string or <code>null</code> if
   *         the parameter was not set, or was set to the empty string.
   */
  private String getStringParameter( String name )
  {
    String result = getParameter( name );
    if ( result != null && result.length() == 0 )
      result = null;
    return result;
  }



  /**
   * Access an integer applet parameter.
   *
   * @param name The parameter name.
   * @return The value as an integer, if a conversion was possible.
   *         <code>Integer.MIN_VALUE</code> is returned if the parameter was
   *         not set or the conversion of the value to integer failed.
   */
  private int getIntegerParameter( String name )
  {
    String value = getStringParameter( name );
    if ( value == null )
      return Integer.MIN_VALUE;

    int result;

    try
    {
      result = Integer.parseInt( value );
    }
    catch ( NumberFormatException e )
    {
      log.log( Level.SEVERE,
        "Bad integer parameter '" +
        name +
        "': '" +
        value +
        "'." );
      result = Integer.MIN_VALUE;
    }

    return result;
  }
}
