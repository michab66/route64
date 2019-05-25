/* $Id: Clock.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2006 Michael G. Binz
 */
package de.michab.simulator;

import java.util.ArrayList;



/**
 * <p>The central clock management for an emulation.  Represents a clock with
 * configurable resolution.  The clock is the synchronisation point for the
 * emulation time and is also responsible to keep in sync with real time, that
 * is, if the emulation is faster than real time, the clock throttles the
 * emulation by performing intermediate sleep cycles.</p>
 *
 * <p>Actual communication from the clock and clock clients is
 * performed through an instance of <code>Clock.ClockHandle</code> created by
 * a call to <code>register()</code>.</p>
 *
 * TODO debugging, in this case emulation is *much* slower, so sync is
 * meaningless.
 *
 * @version $Revision: 11 $
 * @author Michael Binz
 */
public class Clock
{
  /**
   * The clock's resolution.
   */
  private final long _ticksPerSecond;



  /**
   * This is simply precomputed by dividing _ticksPerSecond by 1000.
   */
  private final long _ticksPerMillisecond;



  /**
   * Holds references to the registered clock clients.
   */
  private final ArrayList<ClockHandle> _clients =
	  new ArrayList<ClockHandle>();



  /**
   * A handle for the internal throttling client.
   *
   * @see #throttle()
   */
  private final ClockHandle _throttleHandle;



  /**
   * Has the clock been started yet?  Used as a base for illegal state
   * detection:  After the clock started, registration is no longer
   * allowed.
   */
  private boolean _isStarted = false;



  /**
   * Creates a clock with the specified frequency.
   *
   * @param ticksPerSecond This clock's frequency.
   */
  public Clock( long ticksPerSecond )
  {
    _ticksPerSecond = ticksPerSecond;
    _ticksPerMillisecond = _ticksPerSecond / 1000;

    _throttleHandle = register();
    Thread _throttle = new Thread( new Runnable()
    {
      public void run()
      {
        throttle();
      }
    }, "ClockThrottle" );

    _throttle.setDaemon( true );
    _throttle.start();
  }



  /**
   * Starts dispatching.  This will ensure that all registered clients have
   * successfully called <code>prepare()</code> before thread scheduling is
   * started.
   */
  public synchronized void start()
  {
    _isStarted = true;

    // First ensure that all registered clock clients have successfully
    // prepared.
    synchronized ( _clients )
    {
      while ( _remainingPreparations > 0 )
      {
        try
        {
          _clients.wait();
        }
        catch ( InterruptedException e )
        {
        }
      }
    }

    // Perform the initial schedule on one of our clients.
    ClockHandle initial = _clients.get( 0 );
    synchronized ( initial )
    {
      initial.notify();
    }
  }



  /**
   * A counter that holds the number of outstanding preparations.  It is
   * incremented on each call to <code>register()</code> and decremented
   * on each call to <code>ClockClient.prepare()</code>.  Modifications
   * of this value have to be guarded with a lock on <code>_clients</code>.
   *
   * @see Clock#register()
   * @see ClockHandle#prepare()
   * @see Clock#start()
   */
  private int _remainingPreparations = 0;



  /**
   * Registers a client with this clock.  Note that registration is only
   * allowed <i>before</i> the clock has been started.
   *
   * @return A clock handle that represents the client's main interface to
   *         the clock.
   * @throws IllegalStateException When the clock has been started yet.
   * @see Clock#start()
   */
  public synchronized ClockHandle register()
  {
    if ( _isStarted )
      throw new IllegalStateException( "Clock is started." );

    ClockHandle result = new ClockHandle( this );

    synchronized ( _clients )
    {
      _clients.add( result );
      _remainingPreparations++;
    }

    return result;
  }



  /**
   * Performs a new schedule of the calling thread.  The thread with the
   * earliest local time is scheduled, all other threads are blocked.
   *
   * @param cc The calling client's clock handle.
   * @throws InterruptedException
   */
  private void schedule( ClockHandle cc )
    throws InterruptedException
  {
    // Select the next client to run.  This is the client that
    // has the earliest local time.
    ClockHandle minCc = minimumTime();

    // Perform the actual thread switch.  If the current client is
    // the next one to schedule then...
    if ( minCc == cc )
      // ...simply let it continue.
      return;

    // Another client has an older time, so we schedule that one.
    synchronized( cc )
    {
      synchronized ( minCc )
      {
        // The client that is farthest behind now starts...
        minCc.notify();
      }
      // ...and the original caller now goes into a wait until the others
      // have caught up.
      cc.wait();
    }
  }



  /**
   * Get the clock's current time.  This is the minimum time across all
   * associated chips.
   *
   * @return The clock's federated current time.
   */
  public synchronized long currentTime()
  {
    return minimumTime().currentLocalTime();
  }



  /**
   * Get the clock's resolution in ticks per second.
   *
   * @return The clock's resolution.
   */
  public long getResolution()
  {
    return _ticksPerSecond;
  }



  /**
   * Compute the client index with the earliest local time.
   *
   * @return The client id with the earliest local time.
   */
  private ClockHandle minimumTime()
  {
    int cIdx = _clients.size()-1;
    ClockHandle result = _clients.get( cIdx );
    long minimum = result._time;

    for ( int i = cIdx-1 ; i >= 0 ; i-- )
    {
      ClockHandle ccc = _clients.get(i);

      long currentTime = ccc._time;

      if ( currentTime < minimum )
      {
        minimum = currentTime;
        result = ccc;
      }
    }

    return result;
  }



  /**
   * The number of realtime synchronisations per second.
   */
  private static final int SYNCS_PER_SEC = 800;



  /**
   * <p>Synchronises the emulation with realtime.  On modern and fast
   * processors this means that this method throttles the emulation by adding
   * intermediate sleep cycles.</p>
   *
   * TODO:
   *   Currently if we perform 900 syncs per sec we create less system load
   *   than with syncs per sec = 100. (abt 35 vs. 80%).  WHY???
   * TODO:
   *   We can implement the local time handling in a way that doesn't require
   *   any calls nor divisions or modulus stuff.  Do that for the next release.
   */
  private void throttle()
  {
    int throttleWait = (int)(_ticksPerSecond / SYNCS_PER_SEC);

    _throttleHandle.prepare();

    // This is our local time on the first call.
    long startCycles = 0;
    long startTime = System.currentTimeMillis();

    while ( true )
    {
      long cycleNow = _throttleHandle.advance( throttleWait );
      long now = System.currentTimeMillis();

      // Compute our age.
      long realtimeAgeMs = now - startTime;

      if ( realtimeAgeMs >= 0 )
      {
        long cycleAgeMs = (cycleNow - startCycles) / _ticksPerMillisecond;
        long waitTime = cycleAgeMs - realtimeAgeMs;

        // If the difference accumulated to more than a microsecond.
        if ( waitTime > 0 )
        {
    //      System.err.println("sleep: " +waitTime);
          // Sleep the accumulated difference and give the real world a chance to
          // catch up with the simulation.
          sleep( waitTime );
          // Reset our time base.  We looked on the watch before and after the
          // sleep it should be waitTime later.
    //      startTime = now;
          // The wait above only handled the cycles that accumulated above one
          // millisecond.  We do keep the remainder as a base for next time around.
    //      startCycles = cycleNow - (cycleAge % _ticksPerMillisecond);
        }
//        else
  //        System.err.print( "*" +waitTime );
        // If our last probe is older than a second, we re-initialise the system.
        // This is primarily required when using the debugger.
  //      else if ( realtimeAgeMs > 1000 )
  //      {
  //        startTime = now;
  //        cycles = 0;
  //      }
      }
      else
        System.err.print( ":" );
    }
  }



  /**
   * Put the calling thread to sleep for the passed amount of milliseconds.
   *
   * @param ms Time to sleep in milliseconds.
   */
  private void sleep( long ms )
  {
    try
    {
      Thread.sleep( ms );
    }
    catch ( InterruptedException  e )
    {
      ;
    }
  }



  /**
   * Each clock client receives a <code>ClockHandle</code> as the result
   * of performing the <code>register()</code> operation.  This handle is
   * used for further communication with the clock.  The client is
   * responsible to call <code>prepare()</code> on the clock handle as soon
   * as it is ready to be scheduled.
   *
   * @see #prepare()
   */
  public class ClockHandle
  {
    /**
     * Create an instance.
     *
     * @param home A reference to the <code>Clock</code> the new instance
     *        is associated with.
     */
    private ClockHandle( Clock home )
    {
      _home = home;
      _time = 0;
    }



    /**
     * Signals to the <code>Clock</code> that the calling thread is ready to
     * be scheduled.  Actual scheduling for all registered <code>Clock</code>
     * clients is started by a call to <code>Clock.start()</code>.  As a
     * result each thread calling <code>prepare()</code> is blocked until
     * scheduling is started.
     */
    public synchronized void prepare()
    {
      // Decrease the counter for outstanding preparations.
      synchronized ( _clients )
      {
        _remainingPreparations--;
        _clients.notify();
      }
      // Finally block and expect a wakeup when thread scheduling starts.
      try
      {
        wait();
      }
      catch ( InterruptedException e )
      {

      }
    }



    /**
     * Advances the local time of this client for the given number of ticks.
     * The calling thread is subject to a thread switch.
     *
     * @param ticks The number of ticks to advance.
     * @return This client's local time.  The value returned here is
     *         equivalent to a call to <code>currentLocalTime()</code> but
     *         prevents another call.
     * @throws IllegalArgumentException This is thrown if zero is passed.
     * @see #currentLocalTime()
     */
    public long advance( int ticks )
    {
      if ( ticks <= 0 )
        throw new IllegalArgumentException( "0 not allowed." );

      try
      {
        _time += ticks;
        _home.schedule( this );
        return _time;
      }
      catch ( InterruptedException e )
      {
        // TODO check interruption strategy.
        return currentTime();
      }
    }
    /**
     * experimental
     *
     * @param number The number of ticks to steal.
     * @return The updated time.
     */
    public synchronized long stealTicks( int number )
    {
      _time += number;
      return _time;
    }



    /**
     * Remove the calling thread from the list of threads that can be
     * scheduled.  The calling thread will be blocked until
     * <code>reschedule()</code> is called.
     *
     * @return The current local time of the newly scheduled client.
     * @throws InterruptedException
     */
    public long unschedule()
      throws InterruptedException
    {
      _time = Long.MAX_VALUE;
      _home.schedule( this );
      return _time;
    }



    /**
     * Signals that the thread that is responsible for this handle is ready to
     * be scheduled again.  Note that this must not (cannot) be called from the
     * actual thread that is to be scheduled again since this is blocked in
     * <code>unschedule()</code>.  Instead <code>reschedule()</code> has to be
     * called from a different thread.  The calling thread will not be blocked.
     *
     * @return The current local time of the newly scheduled client.
     */
    public long reschedule()
    {
      _time = _home.currentTime();
      return _time;
    }



    /**
     * Returns the client's local time.  Note that <code>advance()</code>
     * also returns the local time.
     *
     * @return The local time.
     * @see #advance(int)
     */
    public long currentLocalTime()
    {
      return _time;
    }



    /**
     * Returns the current time of the <code>Clock<code>.  This represents
     * overall clock time which is different and normally earlier than
     * the clock client's local time.
     *
     * @return The current clock time.
     * @see Clock#currentTime()
     * @see #currentLocalTime()
     */
    public long currentTime()
    {
      return _home.currentTime();
    }



    /**
     * The local time.
     */
    private long _time;



    /**
     * The clock responsible for dispatching this handle.
     */
    private final Clock _home;
  }
}
