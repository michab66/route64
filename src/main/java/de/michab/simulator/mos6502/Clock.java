/* $Id: Clock.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;



/**
 * Represents a CIA's internal time of day clock.
 *
 * @see de.michab.simulator.mos6502.Cia
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
final class Clock
  implements Runnable
{
  private static final int TENTH_PER_SEC = 10;
  private static final int TENTH_PER_MIN = 60 * TENTH_PER_SEC;
  private static final int TENTH_PER_HOUR = 60 * TENTH_PER_MIN;
  private static final int TENTH_PER_DAY = 24 * TENTH_PER_HOUR;

  /**
   * 
   */
  private final static boolean _debug = false;



  /**
   * The thread driving this unit.
   */
  private final Thread _worker;



  /**
   * A name for this unit for debugging purposes.
   */
  private final String _name;



  /**
   * The home CIA.
   */
  private final Cia _home;



  /**
   *
   */
  private int _hours = 0;
  private int _minutes = 0;
  private int _seconds = 0;
  private int _tenthSecs = 0;
  private int _alarmHours = 0;
  private int _alarmMinutes = 0;
  private int _alarmSeconds = 0;
  private int _alarmTenthSecs = 0;


  /**
   * The current time in tenth of seconds.
   */
  private int _currentTime = 0;



  /**
   * The point in time where the next alarm will be fired.  The initial
   * values will guarantee for inactivity as long as no new values are set.
   */
  private int _alarmTime = -1;



  /**
   * A short term buffer for time reading.  If empty the value is negative.
   */
  private int _readBuffer = -1;



  private final de.michab.simulator.Clock.ClockHandle _clockHandle;
  private final int ticksPerTenthSecond;


  /**
   * Creates an instance of a CIAs real time clock.
   *
   * @param home A reference to the CIA the Clock is part of.
   * @param name A name for the internal thread.  Used for debug purposes.
   */
  Clock( Cia home, de.michab.simulator.Clock systemClock, String name )
  {
    _clockHandle = systemClock.register();
    ticksPerTenthSecond = (int)systemClock.getResolution() / 10;

    _home = home;
    _name = name;
    _worker = new Thread( this, _name );
    _worker.setDaemon( true );
    _worker.start();
  }



  /**
   * Implements the alarm functionality.
   */
  public void run()
  {
    _clockHandle.prepare();

    while ( true )
    {
      _clockHandle.advance( ticksPerTenthSecond );
      
      if ( _currentTime < TENTH_PER_DAY )
        _currentTime++;
      else
        _currentTime = 0;
      
      if ( _currentTime == _alarmTime )
        _home.alarm();
    }
  }



  /**
   * Set tenth of seconds.  In case the alarm bit is true then the alarm time
   * is set else the start time.
   *
   * @param value Tenth of seconds value.
   * @param alarm If true then alarm time is set else start time.
   */
  public synchronized void setTenthSeconds( int value, boolean alarm )
  {
    if ( _debug )
      System.err.println( _name + " tenth: " + value );

    if ( alarm )
      _alarmTenthSecs = value;
    else
      _tenthSecs = value;

    // In case of tenth seconds we also have to update our internal registers.
    if ( alarm )
    {
      _alarmTime =
        (_alarmHours * TENTH_PER_HOUR) +
        (_alarmMinutes * TENTH_PER_MIN) +
        (_alarmSeconds * TENTH_PER_SEC) +
        _alarmTenthSecs;
    }
    else
    {
      _currentTime =
        (_hours * TENTH_PER_HOUR) +
        (_minutes * TENTH_PER_MIN) +
        (_seconds * TENTH_PER_SEC) +
        _tenthSecs;
    }
  }



  /**
   * Set seconds field.
   * 
   * @param value The seconds value.
   * @param alarm If <code>true</code> the alarm time is set, otherwise the
   *        start time.
   */
  public synchronized void setSeconds( int value, boolean alarm )
  {
    if ( _debug )
      System.err.println( _name + " seconds: " + value );

    if ( alarm )
      _alarmSeconds = value;
    else
      _seconds = value;
  }



  /**
   * Set minutes field.
   * 
   * @param value The minutes value.
   * @param alarm If <code>true</code> the alarm time is set, otherwise the
   *        start time.
   */
  public synchronized void setMinutes( int value, boolean alarm )
  {
    if ( _debug )
      System.err.println( _name + " minutes: " + value );

    if ( alarm )
      _alarmMinutes = value;
    else
      _minutes = value;
  }



  /**
   * Set hours field.
   * 
   * @param value The hours value.
   * @param alarm If <code>true</code> the alarm time is set, otherwise the
   *        start time.
   */
  public synchronized void setHours( int value, boolean alarm )
  {
    if ( _debug )
      System.err.println( _name + " hours: " + value );

    if ( alarm )
      _alarmHours = value;
    else
      _hours = value;
  }



  /**
   * Read the current tenth of seconds value.
   * 
   * @return The contents of the tenth of seconds register.
   */
  public synchronized int getTenthSeconds()
  {
    int currentTime =
      (_readBuffer < 0) ?
        _currentTime :
        _readBuffer;

    // Free the intermediate time buffer.
    _readBuffer = -1;

    return currentTime % 10;
  }



  /**
   * Read the current seconds value.
   * 
   * @return The contents of the seconds register.
   */
  public synchronized int getSeconds()
  {
    int currentTime =
      (_readBuffer < 0) ?
        _currentTime :
        _readBuffer;

    return (currentTime/TENTH_PER_SEC) % 60;
  }



  /**
   * Read the current minutes value.
   * 
   * @return The contents of the minutes register.
   */
  public synchronized int getMinutes()
  {
    int currentTime =
      (_readBuffer < 0) ?
        _currentTime :
        _readBuffer;

    return (currentTime/TENTH_PER_MIN) % 60;
  }



  /**
   * Returns the clock's current hour value.  Result is in range [0..23].
   */
  public synchronized int getHours()
  {
    _readBuffer = _currentTime;

    return _readBuffer/TENTH_PER_HOUR;
  }



  /*
   * Inherit Javadoc.
   */
  synchronized void reset()
  {
    _alarmHours = _alarmMinutes = _alarmSeconds = _alarmTenthSecs = 0;
    _hours = _minutes = _seconds = _tenthSecs = 0;

    _currentTime = 0;
    _alarmTime = -1;
  }
}
