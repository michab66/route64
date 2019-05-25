/* $Id: Processor.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.simulator;



/**
 * The base interface to be implemented by processor emulation engines.
 *
 * @see de.michab.simulator.Debugger
 * @author Michael G. Binz
 */
public interface Processor
{
  /**
   * A constant with bit zero set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0000 0000 0001</code>.
   */
  public static final int BIT_0 = 0x01;

  /**
   * A constant with bit one set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0000 0000 0010</code>.
   */
  public static final int BIT_1 = 0x02;

  /**
   * A constant with bit two set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0000 0000 0100</code>.
   */
  public static final int BIT_2 = 0x04;

  /**
   * A constant with bit three set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0000 0000 1000</code>.
   */
  public static final int BIT_3 = 0x08;

  /**
   * A constant with bit four set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0000 0001 0000</code>.
   */
  public static final int BIT_4 = 0x10;

  /**
   * A constant with bit five set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0000 0010 0000</code>.
   */
  public static final int BIT_5 = 0x20;

  /**
   * A constant with bit six set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0000 0100 0000</code>.
   */
  public static final int BIT_6 = 0x40;

  /**
   * A constant with bit seven set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0000 1000 0000</code>.
   */
  public static final int BIT_7 = 0x80;

  /**
   * A constant with bit eight set and all other bits unset.  The binary value
   * is <code>0000 0000 0000 0000 0000 0001 0000 0000</code>.
   */
  public static final int BIT_8 = BIT_7 << 1;

  // BCD constants.
  public static final int DEC_0 = 0x00;
  public static final int DEC_1 = 0x01;
  public static final int DEC_2 = 0x02;
  public static final int DEC_3 = 0x03;
  public static final int DEC_4 = 0x04;
  public static final int DEC_5 = 0x05;
  public static final int DEC_6 = 0x06;
  public static final int DEC_7 = 0x07;
  public static final int DEC_8 = 0x08;
  public static final int DEC_9 = 0x09;



  /**
   * Sets a debugger for this processor.  The debugger can be removed by
   * passing a null reference.
   *
   * @param debugger The debugger instance to be used.  Set this to null for
   *                 switching off debugging.
   */
  void setDebugger( Debugger debugger );
}
