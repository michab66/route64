/**
 * @(#)SID.java	Created date: 99-10-24
 */
package de.michab.simulator.mos6502;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import de.michab.simulator.Processor;



/**
 * A single SID voice. This implementation is from JaC64 on Sourceforge. The
 * original file was http://cvs.sourceforge.net/viewcvs.py/jac64/c64/C64SID.java
 * CVS revision 1.2
 * 
 * @author Joakim Eriksson (joakime@sics.se)
 * @version $Revision: 11 $, $Date: 2005/09/17 12:30:08 $
 */
class Voice
{
  // The logger for this class.
  private final static Logger _log = 
    Logger.getLogger( Voice.class.getName() );



  /**
   * 
   */
  private final SourceDataLine dataLine;

  /**
   * 
   */
  private final FloatControl volume;

  /**
   * 
   */
  private final int[] memory;

  /**
   * This voice's base address in memory.
   */
  private final int _sidbase;

  // How is this defined?
  private static final double FRQCONV = 0.060975609;



  /**
   * Triangle wave form marker.
   * @see #_waveform
   */
  private static final int WAV_TRIANGLE = Processor.BIT_4;
//  private static final int WAV_TRIANGLE = 0x1;



  /**
   * Saw wave form marker.
   * @see #_waveform
   */
  private static final int WAV_SAW = Processor.BIT_5;
//  private static final int WAV_SAW = 0x2;



  /**
   * Pulse wave form marker.
   * @see #_waveform
   */
  private static final int WAV_PULSE = Processor.BIT_6;
//  private static final int WAV_PULSE = 0x4;



  /**
   * Noise wave form marker.
   * @see #_waveform
   */
  private static final int WAV_NOISE = Processor.BIT_7;
//  private static final int WAV_NOISE = 0x8;



  /**
   * Initial wave form marker. TODO Is that needed?
   * @see #_waveform
   */
  private static final int WAV_NONE = 0x0;



  /**
   * The currently selected wave form.  This may hold several of the WAV_*
   * constants ored together.
   */
  private int _waveform = WAV_NONE;



  // Should be public when used in software synths...
  private final static int ATTACK = 1;

  private final static int DECAY = 2;

  private final static int SUSTAIN = 3;

  private final static int RELEASE = 4;

  private final static int WAVE_LEN = 44000;

  /**
   * The absolute min/max value for the precalculated waves.
   */
  private final static int MAX_VAL = 100;

  // The wavebuffers for the precalculated waves (shared between SIDs)
  /**
   * Values from -maxval to maxval continually growing.
   */
  private final static byte[] sawWave = new byte[WAVE_LEN];

  private final static byte[] triangleWave = new byte[WAVE_LEN];

  private final static byte[] sawTriangleWave = new byte[WAVE_LEN];

  /**
   * TODO why is this twice as long as the others?  Has that impact on
   * sound quality?
   * Lower half is filled with -maxVal upper with maxval.
   */
  private final static byte[] pulseWave = new byte[2 * WAVE_LEN];

  private final static float[] triangleWaveRing = new float[WAVE_LEN];

  private final static int GENLEN = 440;

  private final byte[] buffer = new byte[GENLEN];

  private int nextSample = 0;

  private boolean _sync = false;

  private boolean _ring = false;



  /**
   * 
   */
  private static final float GAIN_MULT_15 = (float) (20.0 / 15.0);

  // Start stop ADSR stuff!! (not yet implemented, and how to do with gain?)
  // Number of adsr "steps" (1 ms)
  private final int time1[] = { 
      2, 8, 16, 24, 
      38, 56, 68, 80, 
      100, 250, 500, 800, 
      1000, 3000, 5000, 8000 
  };

  private final int time2[] = { 
      6, 24, 48, 72, 
      114, 168, 204, 240, 
      300, 750, 1500, 2400,
      3000, 9000, 15000, 24000 };

  // ADSR level between 0 - 1;
  private float sidVol;

  private float adsrLevel = 0;

  private float adsrDelta = 0;

  private final static float adsrInterval = (float) 1.0;

  private float adsrSusLevel = 0;

  //private int adsrAtt = 0;

  private int adsrDec = 0;

  private int adsrRel = 0;

  private int adsrPos = 0;

  private int adsrNextPos = 0;

  private int _adsrPhase = ATTACK;

  private boolean soundOn = false;

  // Maybe frq should not be integer?
  private int frq = 1;



  /**
   * 
   */
  private long _noiseReg = 0x7ffff8;



  /**
   * 
   */
  private Voice _next;



  /**
   * Create an instance.
   * 
   * @param mem The array of chip registers.
   * @param sb This voice's register base address.
   * @throws LineUnavailableException If the audio resources could not be
   *         allocated.
   */
  Voice( int mem[], int sb )
    throws LineUnavailableException
  {
    memory = mem;
    _sidbase = sb;

    AudioFormat af = new AudioFormat( WAVE_LEN, 8, 1, true, false);
    DataLine.Info dli = new DataLine.Info(SourceDataLine.class, af, 16384);

    dataLine = (SourceDataLine) AudioSystem.getLine(dli);

    dataLine.open(dataLine.getFormat(), 16384);
    volume = (FloatControl)
          dataLine.getControl(FloatControl.Type.MASTER_GAIN);
    dataLine.start();

    // Create SAW
    //for (int i = 0; i < WAVE_LEN; i++) {
      // Fill saw wave from -100 to 100.
    //  sawWave[i] = (byte) val;
    //  val = val + dval;
    //}

    // Create PULSE
    //  for (int i = 0; i < WAVE_LEN; i++) {
    //    pulseWave[i] = -MAX_VAL;
    //    pulseWave[i + WAVE_LEN] = MAX_VAL;
    //  }

    // Create Triangle
//    val = -MAX_VAL;
//    dval = (MAX_VAL * 4) / (double) WAVE_LEN;
//    for (int i = 0; i < WAVE_LEN / 2; i++) {
//      triangleWave[i] = (byte) val;
//      val = val + dval;
//    }
//    val = MAX_VAL;
//    for (int i = WAVE_LEN / 2; i < WAVE_LEN; i++) {
//      triangleWave[i] = (byte) val;
//      val = val - dval;
//    }

    // Create sawTriangle
//    for (int i = 0; i < WAVE_LEN; i++) {
//      sawTriangleWave[i] = (byte) (sawWave[i] & triangleWave[i]);
//    }

    // Create Triangle Ring
   /* val = -1.0;
    dval = 4.0 / WAVE_LEN;
    for (int i = 0; i < WAVE_LEN / 2; i++) {
      triangleWaveRing[i] = (float) val;
      val = val + dval;
    }
    val = 1.0;
    for (int i = WAVE_LEN / 2; i < WAVE_LEN; i++) {
      triangleWaveRing[i] = (float) val;
      val = val - dval;
    }*/
  }



  /**
   * Set the reference to the next voice.
   *
   * @param next The reference to the next voice.
   */
  void setNext( Voice next )
  {
    _next = next;
  }



  /**
   * 
   */
  private void soundOn()
  {
    if (!soundOn) 
    {
      adsrLevel = 0;
      adsrPos = 0;
      int adsrAtt = time1[(memory[_sidbase + 5] & 0xf0) >> 4];
      adsrDec = time2[(memory[_sidbase + 5] & 0x0f)];
      adsrDelta = adsrInterval / adsrAtt;
      adsrNextPos = adsrAtt;
      _adsrPhase = ATTACK;      
      soundOn = true;
    }
  }

  private void soundOff()
  {
    if ( soundOn )
    {
      adsrRel = time2[(memory[_sidbase + 6] & 0x0f)];
      adsrDelta = -(adsrLevel / adsrRel);
      adsrNextPos = adsrPos + adsrRel;
      _adsrPhase = RELEASE;
      soundOn = false;
    }
  }



  /**
   * 
   * @param vol
   */
  void setVolume( int vol )
  {
    sidVol = GAIN_MULT_15 * vol;
    volume.setValue(-40 + sidVol + 15);
  }



  /*
   * Inherit Javadoc.
   */
  public String toString()
  {
    StringBuffer result = new StringBuffer();

    result.append("Wave: ");
    switch (_waveform)
    {
      case WAV_NONE:
        result.append("NONE");
        break;
      case WAV_TRIANGLE:
        result.append("TRIANGLE");
        break;
      case WAV_SAW:
        result.append("SAW");
        break;
      case WAV_PULSE:
        result.append("PULSE");
        break;
      case WAV_NOISE:
        result.append("NOISE");
        break;
      default:
        result.append("OTHER...: " + Integer.toString(_waveform, 16));
        break;
    }
    result.append( " Frequency: " + getFrequency() ); //+ "  PWid:" + _pwid);
    result.append( " SIDVolume:" + sidVol);
    result.append( " ADSRLevel:" + adsrLevel);
    result.append( " Volume:" + (sidVol + 20 * adsrLevel - 40));
    result.append( " ADSRDelta:" + adsrDelta + " Phase:" + _adsrPhase);
    if (_ring)
      result.append(" RING MODULATION");
    if (_sync)
      result.append(" SYNCHRONIZATION");
    
    return result.toString();
  }



  /**
   * Called if the voices control register is written.
   * TODO This may be merged with the Voice.updateSound operation.
   * 
   * @param data
   */
  void updateVoice( int data )
  {
    _waveform = data & 0xf0;
//    _waveform = data >> 4;
    // Handles the test bit.
    if ((data & Processor.BIT_3) != 0)
      _waveform = Voice.WAV_NONE;

    if ((data & Processor.BIT_0) != 0)
      soundOn();
    else
      soundOff();

    _sync = (data & Processor.BIT_1) != 0;
    _ring = (data & Processor.BIT_2) != 0;
  }



  /**
   * Update the sound machinery.
   */
  void updateSound()
  {
    if (adsrPos++ == adsrNextPos)
    {
      switch ( _adsrPhase )
      {
        case ATTACK:
        {
          _adsrPhase = DECAY;
          // Down to sustain level?
          int adsrSustain = (memory[_sidbase + 6] & 0xf0) >> 4;

          adsrSusLevel = adsrInterval * adsrSustain / 15.0f;
          adsrDelta = (-adsrInterval + adsrSusLevel) / adsrDec;
          adsrNextPos += adsrDec;
          break;
        }
        case  DECAY:
        {
          _adsrPhase = SUSTAIN;
          adsrDelta = 0;
          adsrNextPos = -1;
          break;
        }
        case RELEASE:
        {
          adsrDelta = 0;
          break;
        }
        // TODO unclear how and if to handle the rest
      }
    }

    adsrLevel += adsrDelta;
    // ADSR end

    if (dataLine.available() > GENLEN)
    {
      byte[] wbuf;

      frq = (int)(0.5 + getFrequency() * FRQCONV);
      int pulseWidth = 
        (getPulseWidth() * WAVE_LEN) / 4095;

      int next_nextSample = _next.nextSample;

      switch (_waveform)
      {
        case WAV_NONE:
          Arrays.fill( buffer, (byte)0 );
          break;
        case WAV_TRIANGLE:
          if (_ring) 
          {
            for (int i = 0; i < GENLEN; i++)
            {
              buffer[i] = (byte) (triangleWave[nextSample] * triangleWaveRing[next_nextSample]);
              nextSample = (nextSample + frq) % WAVE_LEN;
              next_nextSample = (next_nextSample + _next.frq) % WAVE_LEN;
            }
          }
          else if (!_sync) 
          {
            for ( int i = 0 ; i < GENLEN ; i++ )
            {
                buffer[i] = triangleWave[nextSample];
                nextSample = (nextSample + frq) % WAVE_LEN;
            }
          }
          else 
          {
            // SYNCH
            for (int i = 0; i < GENLEN; i++) {
              buffer[i] = triangleWave[nextSample];
              nextSample = (nextSample + frq) % WAVE_LEN;
              next_nextSample += _next.frq;
              if (next_nextSample > WAVE_LEN) {
                nextSample = 0;
                next_nextSample -= WAVE_LEN;
              }
            }
          }
          break;
        case WAV_SAW:
        case WAV_SAW | WAV_TRIANGLE:
          if (_waveform == WAV_SAW)
            wbuf = sawWave;
          else
            wbuf = sawTriangleWave;
          if (!_sync) {
            for (int i = 0; i < GENLEN; i++) {
              buffer[i] = wbuf[nextSample];
              nextSample = (nextSample + frq) % WAVE_LEN;
            }
          }
          else {
            // SYNCH
            for (int i = 0; i < GENLEN; i++) {
              buffer[i] = wbuf[nextSample];
              nextSample = (nextSample + frq) % WAVE_LEN;
              next_nextSample += _next.frq;
              if (next_nextSample > WAVE_LEN) {
                nextSample = 0;
                next_nextSample -= WAVE_LEN;
              }
            }
          }
          break;
        case WAV_PULSE:
          if (!_sync) {
            for (int i = 0; i < GENLEN; i++) {
              buffer[i] = pulseWave[pulseWidth + nextSample];
              nextSample = (nextSample + frq) % WAVE_LEN;
            }
          }
          else 
          {
            for (int i = 0; i < GENLEN; i++) 
            {
              buffer[i] = pulseWave[pulseWidth + nextSample];
              nextSample = (nextSample + frq) % WAVE_LEN;
              next_nextSample += _next.frq;
              if (next_nextSample > WAVE_LEN) {
                nextSample = 0;
                next_nextSample -= WAVE_LEN;
              }
            }
          }
          break;
        case WAV_PULSE | WAV_SAW:
        case WAV_PULSE | WAV_TRIANGLE:
        case WAV_PULSE | WAV_SAW | WAV_TRIANGLE:
          if (_waveform == (WAV_PULSE | WAV_SAW))
            wbuf = sawWave;
          else if (_waveform == (WAV_PULSE | WAV_TRIANGLE)) {
            wbuf = triangleWave;
          }
          else
            wbuf = sawTriangleWave;

          if (!_sync) 
          {
            for (int i = 0; i < GENLEN; i++) 
            {
              buffer[i] = (byte) (pulseWave[pulseWidth + nextSample] & wbuf[i]);
              nextSample = (nextSample + frq) % WAVE_LEN;
            }
          }
          else {
            for (int i = 0; i < GENLEN; i++) {
              buffer[i] = (byte) (pulseWave[pulseWidth + nextSample] & wbuf[i]);
              nextSample = (nextSample + frq) % WAVE_LEN;
              next_nextSample += _next.frq;
              if (next_nextSample > WAVE_LEN) {
                nextSample = 0;
                next_nextSample -= WAVE_LEN;
              }
            }
          }
          break;

        case WAV_NOISE:
        case WAV_NOISE | WAV_PULSE:
        case WAV_NOISE | WAV_TRIANGLE:
        case WAV_NOISE | WAV_SAW:
        case WAV_NOISE | WAV_PULSE | WAV_SAW:
        case WAV_NOISE | WAV_TRIANGLE | WAV_SAW:
        case WAV_NOISE | WAV_PULSE | WAV_TRIANGLE:
        case WAV_NOISE | WAV_PULSE | WAV_TRIANGLE | WAV_SAW:
          // Noise:
          // The noise output is taken from intermediate bits of a 23-bit shift
          // register which is clocked by bit 19 of the accumulator.
          // NB! The output is actually delayed 2 cycles after bit 19 is set
          // high.  This is not modeled.
          //
          // Operation: Calculate EOR result, shift register, set bit 0 =
          // result.
          //
          // ----------------------->---------------------
          // | |
          // ----EOR---- |
          // | | |
          // 2 2 2 1 1 1 1 1 1 1 1 1 1 |
          // Register bits: 2 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0 <---
          // | | | | | | | |
          // OSC3 bits : 7 6 5 4 3 2 1 0
          //
          // Since waveform output is 12 bits the output is left-shifted 4
          // times.
          //
          // Shift noise_register (according to re-sid)

          int delay = WAVE_LEN / 32;
          byte noiseData = 0;
          for (int i = 0; i < GENLEN; i++) 
          {
            if (delay < 0) {
              int bit0 = (int) ((_noiseReg >> 22) ^ (_noiseReg >> 17)) & 0x1;
              _noiseReg <<= 1;
              _noiseReg &= 0x7fffff;
              _noiseReg |= bit0;

              noiseData = (byte)
                   (((_noiseReg & 0x400000) >> 15)
                  | ((_noiseReg & 0x100000) >> 14)
                  | ((_noiseReg & 0x010000) >> 11)
                  | ((_noiseReg & 0x002000) >> 9)
                  | ((_noiseReg & 0x000800) >> 8)
                  | ((_noiseReg & 0x000080) >> 5)
                  | ((_noiseReg & 0x000010) >> 3) 
                  | ((_noiseReg & 0x000004) >> 2));

              delay += WAVE_LEN / 32;
            }
            delay -= frq;
            buffer[i] = noiseData;
          }
          break;
        default:
          _log.log( Level.SEVERE, "WAVE NOT IMPLEMENTED: " + _waveform );
      }

      // Test volume? ? vol = 0 - 1 ???
      float floatVol = adsrLevel;
      if (sidVol == 0)
        adsrLevel = 0;
      for (int i = 0; i < GENLEN; i++)
        buffer[i] *= floatVol; // (byte) (buffer[i] * floatVol);

      dataLine.write(buffer, 0, GENLEN);
    }
  }



  /**
   * Access the frequency from the chip registers.  This is a 16 bit integer.
   *
   * @return The frequency value.
   */
  private int getFrequency()
  {
    int hi = memory[_sidbase + 1];
    hi &= 0xff;
    hi <<= 8;
    int lo = memory[_sidbase];
    lo &= 0xff;
    return hi | lo;
  }



  /**
   * Access the pulse width value from the chip registers.  This is a
   * 12 bit integer.
   *
   * @return The pulse width.
   */
  private int getPulseWidth()
  {
    int hi = memory[_sidbase + 3];
    hi &= 0xf;
    hi <<= 8;
    int lo = memory[_sidbase + 2];
    lo &= 0xff;

    return hi | lo;
  }



  static
  {
    int min = -MAX_VAL; // -100
    int max = MAX_VAL; // 100
    int span = max - min;
    int len = sawWave.length;
    for ( int i = 0 ; i < len ; i++ )
    {
      sawWave[i] = (byte)(min + ((span * i) / len));
    }
  }

  static
  {
    byte min = (byte)-MAX_VAL;
    byte max = (byte)MAX_VAL;
    int len = pulseWave.length / 2;
    
    for ( int i = 0 ; i < len ; i++ )
    {
      pulseWave[i] = min;
      pulseWave[pulseWave.length-1-i] = max;
    }
  }

  static 
  {
    int min = -MAX_VAL; // -100
    int max = MAX_VAL; // 100
    int span = max - min;
    int len = triangleWave.length / 2;
    for ( int i = 0 ; i < len ; i++ )
    {
      triangleWave[i] = (byte)(min + ((span * i) / len));
      triangleWave[triangleWave.length-1-i] = triangleWave[i];
    }
  }
  
  static
  {
    for (int i = 0; i < WAVE_LEN; i++) 
    {
      sawTriangleWave[i] = (byte) (sawWave[i] & triangleWave[i]);
    }
  }

  static
  {
    // Create triangle ring.
    // TODO we do not reach the maximum.
    float min = -1.0f;
    float max = 1.0f;
    float span = max - min;
    int len = triangleWaveRing.length / 2;
    for ( int i = 0 ; i < len ; i++ )
    {
      triangleWaveRing[i] = (min + ((span * i) / len));
      triangleWaveRing[triangleWaveRing.length-1-i] = triangleWaveRing[i];
    }
  }
}
