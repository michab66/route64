/* $Id: Timer.java 410 2010-09-27 16:10:47Z Michael $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.michab.simulator.Clock;



/**
 * Implements a single CIA timer.
 *
 * @see de.michab.simulator.mos6502.Cia
 * @version $Revision: 410 $
 * @author Michael G. Binz
 */
final class Timer
  implements Runnable
{
  // The logger for this class.
  private static final Logger _log =
    Logger.getLogger( Timer.class.getName() );



  /**
   * The thread driving this unit.
   */
  private Thread _worker = null;



  /**
   * <code>True</code> if the timer is currently running.  <code>False</code>
   * if the timer is off.
   */
  private boolean _running = false;



  /**
   * This is the time of day when we started waiting.  Needed for computing
   * the current timer value for intermediate requests.
   */
  private long _startWait;



  /**
   * Sleep time.
   */
  private int _countdownValue;



  /**
   * True if this timer is cyclic, false otherwise.
   */
  private boolean _cyclicTimer = false;



  /**
   * Our host CIA.
   */
  private final Cia _cia;



  /**
   *
   */
  private final Clock.ClockHandle _clock;



  /**
   * A reference to a timer that receives timer underflow notifications.
   */
// TODO activate  private final Timer _coTimer;



  /**
   * Creates a timer object.
   *
   * @param host The timer's host CIA.
   * @param coTimer A reference to a timer object that is to receive
   *        timer underflow notifications.
   * @param clock A reference to the system clock.
   * @param threadName A name to use for the internal thread for debug
   *        purposes.
   */
  Timer(
      Cia host,
      Timer coTimer,
      de.michab.simulator.Clock clock,
      String threadName )
  {
    _cia = host;
    // TODO
    // _coTimer = coTimer;

    // Register with the central system clock.
    _clock = clock.register();

    // Init our thread.
    _worker = new Thread( this, threadName );
    _worker.setDaemon( true );

    // Start the timer thread...
    _worker.start();
  }



  /**
   * Set the low byte of this timer's start value.
   *
   * @param loByte The low byte of this timer's start value.
   */
  synchronized void setStartValueLo( byte loByte )
  {
    if (  _log.isLoggable( Level.FINE ) )
      _log.fine( _worker.getName() + ":startValueLo:" + loByte );

    int orable = loByte;
    orable &= 0xff;
    _countdownValue &= 0xff00;
    _countdownValue |= orable;
  }



  /**
   * Set the high byte of this timer's start value.
   *
   * @param hiByte The high byte of this timer's start value.
   */
  synchronized void setStartValueHi( byte hiByte )
  {
    if ( _log.isLoggable( Level.FINE ) )
      _log.fine( _worker.getName() + ":startValueHi:" + hiByte );
    int orable = hiByte;
    orable &= 0xff;
    orable <<= 8;
    _countdownValue &= 0xff;
    _countdownValue |= orable;
  }



  /**
   * Set the timer's one shot mode.  One shot means that the timer counts down
   * only once and then stops.  If set to false -- not one shot -- the timer is
   * set back to the initial values and restarted after every down count.
   *
   * @param value If <code>true</code> this means one-shot mode on.
   */
  synchronized void setOneshot( boolean value )
  {
    _cyclicTimer = ! value;
  }



  /**
   *
   */
  synchronized void forceLoad()
  {
    if ( _log.isLoggable( Level.FINE ) )
      _log.fine( _worker.getName() + ":forceLoad:" + _running );

    if ( _running )
    {
      _clock.reschedule();
    }
  }



  /**
   * Start this timer.  As a result, the thread starts to count down the
   * currently set sleep time and triggers an interrupt.  This method is called
   * from some other thread, not the one that controls this unit's internals.
   * Note that this method has nothing to do with the Thread start() method.
   */
  synchronized void start()
  {
    if ( _log.isLoggable( Level.FINE ) )
      _log.fine(
          _worker.getName() +
          ":start:" +
          _countdownValue );
		if  (_countdownValue == 0 )
			return;
    _clock.reschedule();
  }



  /**
   * Calculates the remaining wait time.
   *
   * @return The remaining wait time.
   */
  private int getCurrentValue()
  {
      return
        // Overall wait time...
        _countdownValue -
        // ...minus the time we waited so far.
        (int)(_clock.currentTime() - _startWait);
  }



  /**
   * Get the lower byte of the currently remaining wait time.
   *
   * @return The lower byte of the remaining wait time.
   * @see #getCurrentValueHi()
   */
  synchronized byte getCurrentValueLo()
  {
    return (byte)getCurrentValue();
  }



  /**
   * Get the higher byte of the remaining wait time.
   *
   * @return The high yte of the remaining wait time.
   * @see #getCurrentValueLo()
   */
  synchronized byte getCurrentValueHi()
  {
    int cv = getCurrentValue();
    cv >>= 8;
    return (byte)cv;
  }



  /*
   * Parent javadoc.
   */
  public void run()
  {
    _clock.prepare();

    while ( true )
    {
      _running = false;
      try
      {
        if ( _log.isLoggable( Level.FINE ) )
          _log.fine( _worker.getName() + ":unschedule" );

        _clock.unschedule();

        _running = true;

        do
        {
          _startWait = _clock.currentLocalTime();
          _clock.advance( _countdownValue );
          // Notify our home CIA of the timer finish.
          _cia.timerFinished( this );
        }
        while ( _cyclicTimer );
      }
      catch ( InterruptedException e )
      {
      }
    }
  }
}
