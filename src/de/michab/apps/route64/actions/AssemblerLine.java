/* $Id: AssemblerLine.java 400 2010-09-06 02:01:43Z Michael $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2003 Michael G. Binz
 */
package de.michab.apps.route64.actions;



/**
 * Represents a decoded instruction.
 *
 * @version $Revision: 400 $
 * @author Stefan K&uuml;hnel
 */
final class AssemblerLine implements Comparable<AssemblerLine>
{
  /**
   * The start address of the instruction.
   */
  private final int _addr;



  /**
   * The decoded line.
   */
  private final String _line;



  /**
   *
   */
  AssemblerLine( int address, String mnemonic )
  {
    _addr = address;
    _line = mnemonic;
  }



  /**
   *
   */
  public boolean equals(Object o2)
  {
    try
    {
      return ((AssemblerLine)o2).getAddr() == _addr;
    }
    catch ( ClassCastException e )
    {
      return false;
    }
  }



  /**
   *
   */
  int getAddr()
  {
    return _addr;
  }



  /**
   *
   */
  String getLine()
  {
    return _line;
  }



  /**
   *
   */
  public int compareTo ( AssemblerLine o2 )
  {
    int result;

    if ( _addr<o2.getAddr() )
      result = -1;
    else if ( _addr>o2.getAddr() )
      result = 1;
    else
      result = 0;

    return result;
  }
}
