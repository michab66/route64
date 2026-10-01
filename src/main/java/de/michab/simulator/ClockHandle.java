package de.michab.simulator;

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
     * The local time.
     */
    private long _time;

    /**
     * Create an instance.
     *
     * @param home A reference to the <code>Clock</code> the new instance
     *        is associated with.
     */
    public ClockHandle()
    {
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
        
        _time += ticks;
        return _time;
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
        return _time;
    }
}
