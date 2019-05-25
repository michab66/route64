/* $Id: KeyTester.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2003 Michael G. Binz
 */
package de.michab.utils.tools;

import javax.swing.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.lang.reflect.*;



/**
 * Support tool for keyboard event research.
 * 
 * @version $Revision: 11 $
 */
class KeyTester
  extends
    JList
  implements
    KeyListener
{
  private static Hashtable _integer2Keyname = null;



  /**
   *
   */
  private KeyTester()
  {
    super( new DefaultListModel() );
  }



  /**
   *
   */
  private static String int2KeyEventName( int code )
  {
    Integer icode = new Integer( code );

    String result = (String)_integer2Keyname.get( icode );

    if ( result == null )
      result = "Not defined: " + code;

    return result;
  }


  static
  {
    _integer2Keyname = new Hashtable();

    Class ke = KeyEvent.class;
    Field[] fields = ke.getFields();

    for ( int i = 0 ; i < fields.length ; i++ )
    {
      Class type = fields[i].getType();
      int modifier = fields[i].getModifiers();

      // Field has to be static final integer
      if ( Integer.TYPE.equals( type ) &&
           Modifier.isStatic( modifier ) &&
           Modifier.isFinal( modifier ) )
      {
        try
        {
          Object value = fields[i].get( null );
          _integer2Keyname.put( value, fields[i].getName() ) ;
        }
        catch ( IllegalAccessException e )
        {
          System.err.println( "Illegal access on field: " + fields[i].getName() );
        }
      }
    }
  }


  /**
   * Invoked when a key has been typed.
   * This event occurs when a key press is followed by a key release.
   */
  public void keyTyped( KeyEvent e )
  {
    if ( e.getKeyCode() != KeyEvent.VK_UNDEFINED )
      writeMessage( "Typed: " +  int2KeyEventName( e.getKeyCode()  ) );
    else if ( e.getKeyChar() != KeyEvent.CHAR_UNDEFINED )
      writeMessage( "Typed: '" +  e.getKeyChar() + "'"  );
    else
      writeMessage( "Typed:  VK_UNDEFINED + CHAR_UNDEFINED" );
  }

  /**
   * Invoked when a key has been pressed.
   */
  public void keyPressed( KeyEvent e )
  {
    writeMessage( "Pressed: " +  int2KeyEventName( e.getKeyCode()  ) );
    // e.getWhen();
  }

  /**
   * Invoked when a key has been released.
   */
  public void keyReleased( KeyEvent e )
  {
    writeMessage( "Released: " +  int2KeyEventName( e.getKeyCode()  ) );
  }


  /**
   *
   */
  private void writeMessage( String message )
  {
    int MAX_LIST_CONTENTS = 20;
    DefaultListModel model = ((DefaultListModel)getModel());

    if ( model.size() > MAX_LIST_CONTENTS )
      model.remove( MAX_LIST_CONTENTS );
    model.add( 0, message );
  }



  public static void main( String[] argv )
  {
    FocusManager.disableSwingFocusManager();

    JFrame tester = new JFrame( "Bluh" );
    KeyTester keyTester = new KeyTester();

    tester.getContentPane().add( keyTester  );
    tester.addKeyListener( keyTester );

    tester.addWindowListener( new WindowAdapter()
    {
      public void windowClosing( WindowEvent e )
      {
        e.getWindow().setVisible( false );
        e.getWindow().dispose();
        System.exit( 0 );
      }
    } );

    tester.pack();
    tester.setSize( 100, 100 );
    tester.setVisible( true );
  }
}
