/* $Id: Debugger.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator;



/**
 * Represents the interface between the debugger and the processor.
 *
 * @see Processor
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
public interface Debugger
{
  /**
   * Part of interlock sequence.  Gets called from the processor on setting
   * the debugger.  In case a debugger instance is removed from a processor it
   * will receive this call with a <code>null</code> argument.
   *
   * @param processor The processor that this debugger can control. It is
   *        possible to receive null here, meaning that the debugger currently
   *        is not linked to a processor and thus not active.
   */
  void setProcessor( Processor processor );



  /**
   * Called by the processor in single step mode.  The operation doesn't
   * define which thread will execute this callback.  In case a user interface
   * is controlled from here take care for possible synchronisation needs.
   *
   * @param pc The current program counter.
   */
  void step( int pc );
}
