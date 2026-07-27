/* https://github.com/michab66/route64
 *
 * Released under Gnu Public License
 * Copyright (c) 2000-2026 Michael G. Binz
 *
 * Comments contain text from the Commodore 64 emulator and Program Development
 * System written by John West (john@ucc.gu.uwa.edu.au) and Marko Mäkelä
 * (msmakela@cc.hut.fi).
 */
package de.michab.simulator.mos6502;

import de.michab.simulator.Clock;
import de.michab.simulator.*;

/**
 * Implements a MOS6510 processor as built into the C64. For the implementation
 * the following documentation was used:
 * <ul>
 * <li><a href="http://www.6502.org/tutorials/6502opcodes.htm">
 * http://www.6502.org/tutorials/6502opcodes.htm</a></li>
 * <li><a href="http://www.digitpress.com/the_digs/vic20/melick/6502b.txt">
 * www.digitpress.com/the_digs/vic20/melick/6502b.txt</a></li>
 * </ul>
 *
 * @author Michael Binz
 */
public class Cpu6510
    extends
        DefaultChip
    implements
      Processor
{
    // TODO(MB) class has to be split up into 6502 and 6510.
    public static final int STATUS_FLAG_CARRY = BIT_0;
    public static final int STATUS_FLAG_ZERO = BIT_1;
    public static final int STATUS_FLAG_INTERRUPT = BIT_2;
    public static final int STATUS_FLAG_DECIMAL = BIT_3;
    public static final int STATUS_FLAG_BREAK = BIT_4;
    private static final int STATUS_FLAG_CONST_ONE = BIT_5;
    public static final int STATUS_FLAG_OVERFLOW = BIT_6;
    public static final int STATUS_FLAG_NEGATIVE = BIT_7;

    /**
     * Switch on debugging.
     */
    private static final boolean _debug = false;

    /**
      * The number of cycles executed since the last realtime synchronisation has
     * been executed.
     */
    private int _cycles = 0;

    /**
     * The memory we are working on.
     */
    private final Memory _memory;

    private final ClockHandle _clockId;

    /**
     * This defines the total number of this chip's ports.
     */
    private static final int NUM_OF_PORTS = 2;

    /**
      * The actual byte array implementing this chip's ports.
      */
    private final byte[] _portMemory = new byte[ NUM_OF_PORTS ];

    /**
     * This processor's ports.
     */
    private final Port[] _ports;

    /**
     * These could override the ports above if set.
     */
    private final Forwarder[] _portListeners = new Forwarder[]{ null, null };

    /**
     * Non-null if a debugger is set.
     */
    private Debugger _debugger = null;

    /**
     * Processor specific constants.
     */
    public static final int RESET_VECTOR = 0xfffc;
    public static final int IRQ_VECTOR = 0xfffe;
    public static final int NMI_VECTOR = 0xfffa;
    private static final int STACK_PAGE = 0x0100;

    /**
     * The accumulator register.
     */
    private byte _accu;

    /**
     * The x register.
     */
    private byte _x;

    /**
     * The y register.
     */
    private byte _y;

    /**
     * <p>The stack pointer.  The NMOS 65xx processors have 256 bytes of stack
     * memory, ranging from $0100 to $01FF. The S register is a 8-bit offset to
     * the stack page. In other words, whenever anything is being pushed on the
     * stack, it will be stored to the address $0100+S.</p>
     *
     * <p>The Stack pointer can be read and written by transfering its value to
     * or from the index register X (see below) with the TSX and TXS
     * instructions.</p>
     *
     * <p>The stack pointer must only be modified by the increment/decrementStack
     * operations.  These ensure that in all cases the stack page gets ored in as
     * needed.  Only in the TSA/TAS operations direct access is allowed.</p>
     *
     * @see Cpu6510#incrementStack()
     * @see Cpu6510#decrementStack()
     */
    private byte _stack = 0;

    /**
     * <p>Overflow flag.  Like the Negative flag, this flag is intended to be
     * used with 8-bit signed integer numbers. The flag will be affected by
     * addition and subtraction, the instructions PLP, CLV and BIT, and the
     * hardware signal -SO.</p>
     *
     * <p>The CLV instruction clears the V flag, and the PLP and BIT instructions
     * copy the flag value from the bit 6 of the topmost stack entry or from
     * memory.</p>
     *
     * <p>After a binary addition or subtraction, the V flag will be set on a
     * sign overflow, cleared otherwise.  What is a sign overflow?  For instance,
     * if you are trying to add 123 and 45 together, the result (168) does not
     * fit in a 8-bit signed integer (upper limit 127 and lower limit -128).</p>
     *
     * <p>Similarly, adding -123 to -45 causes the overflow, just like
     * subtracting -45 from 123 or 123 from -45 would do.</p>
     */
    private boolean _overflow = false;

    /**
     * <p>This flag is used in additions, subtractions, comparisons and bit
     * rotations.  In additions and subtractions, it acts as a 9th bit and lets
     * you to chain operations to calculate with bigger than 8-bit numbers. When
     * subtracting, the Carry flag is the negative of Borrow: if an overflow
     * occurs, the flag will be clear, otherwise set.  Comparisons are a special
     * case of subtraction: they assume Carry flag set and Decimal flag clear,
     * and do not store the result of the subtraction anywhere.</p>
     *
     * <p>There are four kinds of bit rotations.  All of them store the bit that
     * is being rotated off to the Carry flag.  The left shifting instructions
     * are ROL and ASL.  ROL copies the initial Carry flag to the lowmost bit of
     * the byte; ASL always clears it.  Similarly, the ROR and LSR instructions
     * shift to the right.</p>
     */
    private boolean _carry = false;

    /**
     * <p>Negative flag.  This flag will be set after any arithmetic operations
     * (when any of the registers A, X or Y is being loaded with a value).
     * Generally, the N flag will be copied from the topmost bit of the register
     * being loaded.</p>
     *
     * <p>Note that TXS (Transfer X to S) is not an arithmetic operation. Also
     * note that the BIT instruction affects the Negative flag just like
     * arithmetic operations.  Finally, the negative flag behaves differently in
     * decimal operations.</p>
     *
     * @see Cpu6510#_decimal
     */
    private boolean _negative = false;

    /**
     * <p>Decimal mode flag.  This flag is used to select the (Binary Coded)
     * Decimal mode for addition and subtraction.  In most applications, the flag
     * is zero.</p>
     *
     * <p>The Decimal mode has many oddities, and it operates differently on CMOS
     * processors.</p>
     */
    private boolean _decimal = false;

    /**
     * Zero flag.  The zero flag will be affected in the same cases as the
     * negative flag.  Generally, it will be set if an arithmetic register is
     * being loaded with the value zero, and cleared otherwise. The flag will
     * behave differently in decimal operations.
     */
    private boolean _zero = false;

    /**
     * <p>Break flag.  This flag is used to distinguish software (BRK) interrupts
     * from hardware interrupts (IRQ or NMI). The B flag is always set except
     * when the P register is being pushed on stack when jumping to an interrupt
     * routine to process only a hardware interrupt.</p>
     * <p>
     * The official NMOS 65xx documentation claims that the BRK instruction could
     * only cause a jump to the IRQ vector ($FFFE). However, if an NMI interrupt
     * occurs  while executing a BRK instruction, the processor will jump to the
     * NMI vector ($FFFA), and the P register will be pushed on the stack with
     * the B flag set.</p>
     */
    private boolean _break = true;

    /**
     * Interrupt disable flag.  This flag can be used to prevent the processor
     * from jumping to the IRQ handler vector ($FFFE) whenever the hardware line
     * -IRQ is active. The flag will be automatically set after taking an
     * interrupt, so that the processor would not keep jumping to the interrupt
     * routine if the -IRQ signal remains low for several clock cycles.
     */
    private boolean _interrupt = false;

    /**
     * <p>Program counter.  This register points the address from which the next
     * instruction byte (opcode or parameter) will be fetched.  Unlike other
     * registers, this one is 16 bits in length. The low and high 8-bit halves
     * of the register are called PCL and PCH, respectively.</p>
     *
     * <p>The program counter may be read by pushing its value on the stack. This
     * can be done either by jumping to a subroutine or by causing an
     * interrupt.</p>
     */
    private int _pc;

    /**
     * Interrupt pending address.  Set if an interrupt is signalled.  Jump is
     * executed in the main decode/execute loop after finishing the current
     * instruction.  Null value for this address is Integer.MIN_VALUE.
     */
    private int _interruptPending = Integer.MIN_VALUE;

    /**
     * The type of the pending interrupt.  Only valid if _interruptPending is
     * true.
     *
     * @see Cpu6510#_interruptPending
     */
    private int _interruptPendingType = INT_NONE;
    private static final int INT_NONE = 0;
    private static final int INT_IRQ = 1;
    private static final int INT_NMI = 2;
    private static final int INT_RESET = 3;

    /**
     * Create a processor tied to the passed memory.  Note that this memory has
     * to contain some valid instruction sequences.  Especially a valid reset
     * vector has to be included to successfully start the emulation.
     *
     * @param mem The memory to attach to the new processor instance.
     * @param clock A reference to the system clock.
     */
    public Cpu6510( Memory mem, Clock clock )
    {
      _clockId = clock.register();
      // Create this processor's ports.
      _ports = createPorts( _portMemory.length );
      // Link to the passed memory image...
      _memory = mem;
      // ..and reset our internal state.
      reset();

      start();
    }

    /**
     * Used in tests.
     *
     * @param mem The memory to attach to the new processor instance.
     * @param clock A reference to the system clock.
     */
    public Cpu6510( Memory mem, ClockHandle clock )
    {
      _clockId = clock;
      _ports = createPorts( _portMemory.length );
      _memory = mem;
    }

    /**
     * Get the <code>Memory</code> this <code>CPU</code> is attached to.
     *
     * @return The <code>Memory</code> this <code>CPU</code> is attached to.
     */
    public Memory getMemory()
    {
      return _memory;
    }

    /**
     * Write this processor's ports.
     *
     * @param i The port number to write.
     * @param b The value to write.
     */
    public synchronized void write( int i, byte b )
    {
      if ( _portListeners[ i ] != null )
        _portListeners[i].write( b );
      else
        _portMemory[ i ] = b;
    }

    /**
     * Read this processor's ports.
     *
     * @param i The port number.
     * @return The value read from the port.
     */
    public synchronized byte read( int i )
    {
      byte result;

      if ( _portListeners[ i ] != null )
        result = _portListeners[i].read();
      else
        result = _portMemory[ i ];

      return result;
    }

    /**
     * Sets a listener on the passed port.
     *
     * @param portId The port the listener is assigned to.
     * @param listener The listener for the port.
     */
    public void setPortListener( int portId, Forwarder listener )
    {
      _portListeners[ portId ] = listener;
    }

    /**
     * Get this processors ports.
     *
     * @return An array of ports.
     */
    public Port[] getPorts()
    {
      return _ports;
    }

  /**
   * Read the x register.  Result is normalised into range [0..255].
   *
   * @return The contents of the x register.
   */
  public int getX()
  {
    return _x & 0xff;
  }

  /**
  * Sets the x register.  Note that this must be called from the thread
  * running the processor.  This is not enforced because of performance
  * reasons.
  *
  * @param x The new content for the x register.
  */
  public void setX( int x )
  {
    _x = (byte)x;
  }



  /**
   * Read the y register.  Result is normalised into range [0..255].

   * @return The contents of the y register.
  */
  public int getY()
  {
    return _y & 0xff;
  }

  /**
  * Sets the y register.  Note that this must be called from the thread
  * running the processor.  This is not enforced because of performance
  * reasons.
  *
  * @param y The new value for the y register.
  */
  public void setY( int y )
  {
    _y = (byte)y;
  }

  /**
   * Read the accumulator register.  Result is normalised into range [0..255].
   *
   * @return The contents of the accumulator register.
   */
  public int getAccu()
  {
    return _accu & 0xff;
  }

  /**
  * Sets the accu register.  Note that this must be called from the thread
  * running the processor.  This is not enforced because of performance
  * reasons.
  *
  * @param a The new value for the accumulator.
  */
  public void setAccu( int a )
  {
    _accu = (byte)a;
  }



  /**
  * Set the program counter to a new address.
  *
  * @param pc The new address for the program counter.
  */
  public void setPC( int pc )
  {
    _pc = pc & 0xffff;
  }
  public void incPC( int inc )
  {
    _pc = (_pc + inc) & 0xffff;
  }

  public void setStack(int stack)
  {
    _stack = (byte)stack;
  }
  public int getStack()
  {
    return _stack & 0xff;
  }

  /**
  * Read the program counter.
  *
  * @return the current PC.
  */
  public int getPC()
  {
    return _pc;
  }



  /**
  * Handles the IRQ interrupt.  This method will be the entry point for
  * threads controlling other chips in the simulation, hence needs to be
  * synchronised.
  */
  public synchronized void IRQ()
  {
    if ( ! _interrupt )
      {
      // Set the interrupt pending address.  TODO what to do if this is
      // set yet??
      _interruptPending = _memory.getVectorAt( IRQ_VECTOR );
      _interruptPendingType = INT_IRQ;
    }
  }

  /**
   * Handles the NMI interrupt.
   */
  public synchronized void NMI()
  {
    // Set the interrupt pending address.
    _interruptPending = _memory.getVectorAt( NMI_VECTOR );
    _interruptPendingType = INT_NMI;
  }

  /**
  * experimental TODO
  * @param count
  */
  public void stealCycles( int count )
  {
    _clockId.stealTicks( count );
  }



  /**
  * Start the thread executing the processor.
  */
  private void start()
  {
    Thread t = new Thread(
      new Runnable()
      {
        public void run()
        {
          _clockId.prepare();
          dispatchTicks();
        }
      },
      getClass().getName() );

      // Set the priority of the clock dispatching thread below the normal
      // to prevent us from bogging down the system as a whole.  See also
      // class implementation comment.
      t.setPriority( Thread.NORM_PRIORITY - 1 );

      t.start();
    }



    /**
    * The processor driver loop.
    */
    private void dispatchTicks()
    {
      while ( true )
        {
        tick();
      }
    }



      /**
       * Common code for opcodes 9c, 9d, 9f.
       */
      private void SH_YXA( int a, int b )
      {
        // SHX: mem[base+Y] = X & (base_high+1); on page cross, high byte of addr is also replaced.
        int base = Memory.mask8( abs() );
        int sum  = base + b;
        int val  = a & ((base >> 8) + 1);

        int addr = (sum > 0xff) ?
          ((val << 8) | Memory.mask8(sum)) :
          (Memory.mask16( base + b ));

        _memory.write( addr, (byte)val );

        incPC(3);
      }

    /**
     * Perform a single instruction cycle.
     */
    public void tick()
    {
      // Check for pending interrupt.  If there is an interrupt pending...
      if ( _interruptPending != Integer.MIN_VALUE )
        {
        if ( _debug )
          System.err.println( "Interrupt *********" );
        // ...set the break flag for hardware interrupts...
        _break = false;
        // ... push the program counter ...
        pushPc();
        // ... and the status register ...
        _memory.write( decrementStack(), (byte)getStatusRegister() );
        // ... and set the new program counter.
        _pc = _interruptPending;
        // At last clear the interrupt pending flag/address...
        _interruptPending = Integer.MIN_VALUE;
        // ...and set the interrupt flag only in case this is not a reset.
        _interrupt = _interruptPendingType != INT_RESET;
      }

      // Single stepping only if not in interrupt mode. TODO this disables
      // breakpoints in interrupts.  Solution is to rework the interface:
      // It can be specified whether it should be possible to debug interrupts.
      if ( _debugger != null && ! _interrupt )
        _debugger.step( _pc );

      int opcode = _memory.read8( _pc );

      _cycles = Opcodes.getTime( opcode );

      // Switch over the valid instruction bytes.
      switch ( opcode )
      {
        // ORA
        case Opcodes.ORA_IMM:
        ORA( imm() );
        incPC( 2 );
        break;

        case Opcodes.ORA_ZP:
        ORA( zp() );
        incPC( 2 );
        break;

        case Opcodes.ORA_ZPX:
        ORA( zpx() );
        incPC( 2 );
        break;

        case Opcodes.ORA_IZX:
        ORA( izx() );
        incPC( 2 );
        break;

        case Opcodes.ORA_IZY:
        ORA( izy() );
        incPC( 2 );
        break;

        case Opcodes.ORA_ABS:
        ORA( abs() );
        incPC( 3 );
        break;

        case Opcodes.ORA_ABSX:
        ORA( abx() );
        incPC( 3 );
        break;

        case Opcodes.ORA_ABSY:
        ORA( aby() );
        incPC( 3 );
        break;

        // AND imm ////////////////////////////////////////////////////////////
        case Opcodes.AND_IMM:
        AND( imm() );
        incPC( 2 );
        break;

        case Opcodes.AND_ZP:
        AND( zp() );
        incPC( 2 );
        break;

        case Opcodes.AND_ZPX:
        AND( zpx() );
        incPC( 2 );
        break;

        case Opcodes.AND_IZX:
        AND( izx() );
        incPC( 2 );
        break;

        case Opcodes.AND_IZY:
        AND( izy() );
        incPC( 2 );
        break;

        case Opcodes.AND_ABS:
        AND( abs() );
        incPC( 3 );
        break;

        case Opcodes.AND_ABSX:
        AND( abx() );
        incPC( 3 );
        break;

        case Opcodes.AND_ABSY:
        AND( aby() );
        incPC( 3 );
        break;

        // EOR imm ////////////////////////////////////////////////////////////
        case Opcodes.EOR_IMM:
        EOR( imm() );
        incPC(2);
        break;

        case Opcodes.EOR_ZP:
        EOR( zp() );
        incPC( 2 );
        break;

        case Opcodes.EOR_ZPX:
        EOR( zpx() );
        incPC( 2 );
        break;

        case Opcodes.EOR_IZX:
        EOR( izx() );
        incPC( 2 );
        break;

        case Opcodes.EOR_IZY:
        EOR( izy() );
        incPC( 2 );
        break;

        case Opcodes.EOR_ABS:
        EOR( abs() );
        incPC( 3 );
        break;

        case Opcodes.EOR_ABSX:
        EOR( abx() );
        incPC(3);
        break;

        case Opcodes.EOR_ABSY:
        EOR( aby() );
        incPC(3);
        break;

        // ADC imm ////////////////////////////////////////////////////////////
        case Opcodes.ADC_IMM:
        ADC( imm() );
        incPC( 2 );
        break;

        case Opcodes.ADC_ZP:
        ADC( zp() );
        incPC( 2 );
        break;

        case Opcodes.ADC_ZPX:
        ADC( zpx() );
        incPC( 2 );
        break;

        case Opcodes.ADC_IZX:
        ADC( izx() );
        incPC( 2 );
        break;

        case Opcodes.ADC_IZY:
        ADC( izy() );
        incPC( 2 );
        break;

        case Opcodes.ADC_ABS:
        ADC( abs() );
        incPC( 3 );
        break;

        case Opcodes.ADC_ABSX:
        ADC( abx() );
        incPC( 3 );
        break;

        case Opcodes.ADC_ABSY:
        ADC( aby() );
        incPC( 3 );
        break;

        // SBC
        case Opcodes.SBC_IMM:
        SBC( imm() );
        incPC( 2 );
        break;

        case Opcodes.SBC_ZP:
        SBC( zp() );
        incPC( 2 );
        break;

        case Opcodes.SBC_ZPX:
        SBC( zpx() );
        incPC( 2 );
        break;

        case Opcodes.SBC_IZX:
        SBC( izx() );
        incPC( 2 );
        break;

        case Opcodes.SBC_IZY:
        SBC( izy() );
        incPC( 2 );
        break;

        case Opcodes.SBC_ABS:
        SBC( abs() );
        incPC( 3 );
        break;

        case Opcodes.SBC_ABSX:
        SBC( abx() );
        incPC( 3 );
        break;

        case Opcodes.SBC_ABSY:
        SBC( aby() );
        incPC( 3 );
        break;

        // CMP
        case Opcodes.CMP_IMM:
        CMP( imm() );
        incPC( 2 );
        break;

        case Opcodes.CMP_ZP:
        CMP(  zp() );
        incPC( 2 );
        break;

        case Opcodes.CMP_ZPX:
        CMP( zpx() );
        incPC( 2 );
        break;

        case Opcodes.CMP_IZX:
        CMP( izx() );
        incPC( 2 );
        break;

        case Opcodes.CMP_IZY:
        CMP( izy() );
        incPC( 2 );
        break;

        case Opcodes.CMP_ABS:
        CMP( abs() );
        incPC( 3 );
        break;

        case Opcodes.CMP_ABSX:
        CMP( abx() );
        incPC( 3 );
        break;

        case Opcodes.CMP_ABSY:
        CMP( aby() );
        incPC( 3 );
        break;

        // CPX
        case Opcodes.CPX_IMM:
        CPX( imm() );
        incPC( 2 );
        break;

        case Opcodes.CPX_ZP:
        CPX( zp() );
        incPC( 2 );
        break;

        case Opcodes.CPX_ABS:
        CPX( abs() );
        incPC( 3 );
        break;

        // CPY
        case Opcodes.CPY_IMM:
        CPY( imm() );
        incPC( 2 );
        break;

        case Opcodes.CPY_ZP:
        CPY( zp() );
        incPC( 2 );
        break;

        case Opcodes.CPY_ABS:
        CPY( abs() );
        incPC( 3 );
        break;

        // DEC
        case Opcodes.DEC_ZP:
        DEC( zp() );
        incPC( 2 );
        break;

        case Opcodes.DEC_ZPX:
        DEC( zpx() );
        incPC( 2 );
        break;

        case Opcodes.DEC_ABS:
        DEC( abs() );
        incPC( 3 );
        break;

        case Opcodes.DEC_ABSX:
        DEC( abx() );
        incPC( 3 );
        break;

        // DEX
        case Opcodes.DEX_IMP:
        DEX();
        incPC( 1 );
        break;

        // DEY
        case Opcodes.DEY_IMP:
        DEY();
        incPC( 1 );
        break;

        // INC /////////////////////////////////////////////////////////////
        case Opcodes.INC_ZP:
        INC( zp() );
        incPC( 2 );
        break;

        case Opcodes.INC_ZPX:
        INC( zpx() );
        incPC( 2 );
        break;

        case Opcodes.INC_ABS:
        INC( abs() );
        incPC( 3 );
        break;

        case Opcodes.INC_ABSX:
        INC( abx() );
        incPC( 3 );
        break;

        // INX ////////////////////////////////////////////////////////////
        case Opcodes.INX_IMP:
        INX();
        incPC( 1 );
        break;

        // INY ////////////////////////////////////////////////////////////
        case Opcodes.INY_IMP:
        INY();
        incPC( 1 );
        break;

        // ASL ////////////////////////////////////////////////////////////
        case Opcodes.ASL_IMP:
        ASL();
        incPC( 1 );
        break;

        case Opcodes.ASL_ZP:
        ASL( zp() );
        incPC(2);
        break;

        case Opcodes.ASL_ZPX:
        ASL( zpx() );
        incPC(2);
        break;

        case Opcodes.ASL_ABS:
        ASL( abs() );
        incPC(3);
        break;

        case Opcodes.ASL_ABSX:
        ASL( abx() );
        incPC(3);
        break;

        // ROL imp ////////////////////////////////////////////////////////////
        case Opcodes.ROL_IMP:
        ROL();
        incPC( 1 );
        break;

        case Opcodes.ROL_ZP:
        ROL( zp() );
        incPC( 2 );
        break;

        case Opcodes.ROL_ZPX:
        ROL( zpx() );
        incPC( 2 );
        break;

        case Opcodes.ROL_ABS:
        ROL( abs() );
        incPC( 3 );
        break;

        case Opcodes.ROL_ABSX:
        ROL( abx() );
        incPC( 3 );
        break;

        // LSR imp ////////////////////////////////////////////////////////////
        case Opcodes.LSR_IMP:
        LSR();
        incPC( 1 );
        break;

        case Opcodes.LSR_ZP:
        LSR( zp() );
        incPC( 2 );
        break;

        case Opcodes.LSR_ZPX:
        LSR( zpx() );
        incPC( 2 );
        break;

        case Opcodes.LSR_ABS:
        LSR( abs() );
        incPC( 3 );
        break;

        case Opcodes.LSR_ABSX:
        LSR( abx() );
        incPC( 3 );
        break;

        // ROR //
        case Opcodes.ROR_IMP:
        ROR();
        incPC( 1 );
        break;

        case Opcodes.ROR_ZP:
        ROR( zp() );
        incPC( 2 );
        break;

        case Opcodes.ROR_ZPX:
        ROR( zpx() );
        incPC( 2 );
        break;

        case Opcodes.ROR_ABS:
        ROR( abs() );
        incPC( 3 );
        break;

        case Opcodes.ROR_ABSX:
        ROR( abx() );
        incPC( 3 );
        break;

        // LDA ////////////////////////////////////////////////////////////
        case Opcodes.LDA_IMM:
        LDA( imm() );
        incPC( 2 );
        break;

        case Opcodes.LDA_ZP:
        LDA( zp() );
        incPC( 2 );
        break;

        case Opcodes.LDA_ZPX:
        LDA( zpx() );
        incPC( 2 );
        break;

        case Opcodes.LDA_IZX:
        LDA( izx() );
        incPC( 2 );
        break;

        case Opcodes.LDA_IZY:
        LDA( izy() );
        incPC( 2 );
        break;

        case Opcodes.LDA_ABS:
        LDA( abs() );
        incPC( 3 );
        break;

        case Opcodes.LDA_ABSX:
        LDA( abx() );
        incPC( 3 );
        break;

        case Opcodes.LDA_ABSY:
        LDA( aby() );
        incPC( 3 );
        break;

        // STA zp /////////////////////////////////////////////////////////////
        case Opcodes.STA_ZP:
        STA( zp() );
        incPC( 2 );
        break;

        case Opcodes.STA_ZPX:
        STA( zpx() );
        incPC( 2 );
        break;

        case Opcodes.STA_IZX:
        STA( izx() );
        incPC( 2 );
        break;

        case Opcodes.STA_IZY:
        STA( izy() );
        incPC( 2 );
        break;

        case Opcodes.STA_ABS:
        STA( abs() );
        incPC( 3 );
        break;

        case Opcodes.STA_ABSX:
        STA( abx() );
        incPC( 3 );
        break;

        case Opcodes.STA_ABSY:
        STA( aby() );
        incPC( 3 );
        break;

        // LDX imm ////////////////////////////////////////////////////////////
        case Opcodes.LDX_IMM:
        LDX( imm() );
        incPC( 2 );
        break;

        case Opcodes.LDX_ZP:
        LDX( zp() );
        incPC( 2 );
        break;

        case Opcodes.LDX_ZPY:
        LDX( zpy() );
        incPC( 2 );
        break;

        case Opcodes.LDX_ABS:
        LDX( abs() );
        incPC( 3 );
        break;

        case Opcodes.LDX_ABSY:
        LDX( aby() );
        incPC( 3 );
        break;

        // STX zp /////////////////////////////////////////////////////////////
        case Opcodes.STX_ZP:
        STX( zp() );
        incPC( 2 );
        break;

        case Opcodes.STX_ZPY:
        STX( zpy() );
        incPC( 2 );
        break;

        case Opcodes.STX_ABS:
        STX( abs() );
        incPC( 3 );
        break;

        // LDY ////////////////////////////////////////////////////////////
        case Opcodes.LDY_IMM:
        LDY( imm() );
        incPC( 2 );
        break;

        case Opcodes.LDY_ZP:
        LDY( zp() );
        incPC( 2 );
        break;

        case Opcodes.LDY_ZPX:
        LDY( zpx() );
        incPC( 2 );
        break;

        case Opcodes.LDY_ABS:
        LDY( abs() );
        incPC( 3 );
        break;

        case Opcodes.LDY_ABSX:
        LDY( abx() );
        incPC( 3 );
        break;

        // STY zp /////////////////////////////////////////////////////////////
        case Opcodes.STY_ZP:
        STY( zp() );
        incPC( 2 );
        break;

        case Opcodes.STY_ZPX:
        STY( zpx() );
        incPC( 2 );
        break;

        case Opcodes.STY_ABS:
        STY( abs() );
        incPC( 3 );
        break;

        // TAX imp ////////////////////////////////////////////////////////////
        case Opcodes.TAX_IMP:
        TAX();
        incPC( 1 );
        break;

        // TXA imp ////////////////////////////////////////////////////////////
        case Opcodes.TXA_IMP:
        TXA();
        incPC( 1 );
        break;

        // TAY imp ////////////////////////////////////////////////////////////
        case Opcodes.TAY_IMP:
        TAY();
        incPC( 1 );
        break;

        // TYA imp ////////////////////////////////////////////////////////////
        case Opcodes.TYA_IMP:
        TYA();
        incPC( 1 );
        break;

        // TSX imp ////////////////////////////////////////////////////////////
        case Opcodes.TSX_IMP:
        TSX();
        incPC( 1 );
        break;

        // TXS imp ////////////////////////////////////////////////////////////
        case Opcodes.TXS_IMP:
        TXS();
        incPC( 1 );
        break;

        // PLA imp ////////////////////////////////////////////////////////////
        case Opcodes.PLA_IMP:
        PLA();
        incPC( 1 );
        break;

        // PHA imp ////////////////////////////////////////////////////////////
        case Opcodes.PHA_IMP:
        PHA();
        incPC( 1 );
        break;

        // PLP imp ////////////////////////////////////////////////////////////
        case Opcodes.PLP_IMP:
        PLP();
        incPC( 1 );
        break;

        // PHP imp ////////////////////////////////////////////////////////////
        case Opcodes.PHP_IMP:
        PHP();
        incPC( 1 );
        break;

        // BPL rel ////////////////////////////////////////////////////////////
        case Opcodes.BPL_REL:
        branchOn( ! _negative );
        break;

        // BMI rel ////////////////////////////////////////////////////////////
        case Opcodes.BMI_REL:
        branchOn ( _negative );
        break;

        // BVC rel ////////////////////////////////////////////////////////////
        case Opcodes.BVC_REL:
        branchOn( ! _overflow );
        break;

        // BVS rel ////////////////////////////////////////////////////////////
        case Opcodes.BVS_REL:
        branchOn( _overflow );
        break;

        // BCC rel ////////////////////////////////////////////////////////////
        case Opcodes.BCC_REL:
        branchOn( ! _carry );
        break;

        // BCS rel ////////////////////////////////////////////////////////////
        case Opcodes.BCS_REL:
        branchOn( _carry );
        break;

        // BNE rel ////////////////////////////////////////////////////////////
        case Opcodes.BNE_REL:
        branchOn( ! _zero );
        break;

        // BEQ rel ////////////////////////////////////////////////////////////
        case Opcodes.BEQ_REL:
        branchOn( _zero );
        break;

        // BRK imp ////////////////////////////////////////////////////////////
        case Opcodes.BRK_IMP:
        BRK();
        break;

        // RTI imp ////////////////////////////////////////////////////////////
        case Opcodes.RTI_IMP:
        RTI();
        break;

        // JSR abs ////////////////////////////////////////////////////////////
        case Opcodes.JSR_ABS:
        {
          var lo = _memory.read( _pc + 1 );

          // A dummy read, performed in the original 6502 and verified in
          // the test suite.
          _memory.read( _pc + 2 );

          // Push *address* of third byte.
          pushPc( _pc + 2 );

          // Compute the new program counter.  Note that the low byte is read
          // again.  Required to pass some edge case tests.
          // See test 20.json "20 55 13"
          _pc = Memory.mask16((_memory.read( _pc + 2 ) << 8) | (lo & 0xff));

          break;
        }

        // RTS imp ////////////////////////////////////////////////////////////
        case Opcodes.RTS_IMP:
        // Pop program counter.
        popPc();
        // Step program counter.  Read the comments for JSR rgd. address
        // decoding.  TODO check RTI and friends.
        incPC( 1 );
        break;

        // JMP abs ////////////////////////////////////////////////////////////
        case Opcodes.JMP_ABS:
        // Get the next execution address.
        _pc = abs();
        break;

        case Opcodes.JMP_IND:
        // Get the next execution address.
        _pc = ind();
        break;

        // BIT zp /////////////////////////////////////////////////////////////
        case Opcodes.BIT_ZP:
        BIT( zp() );
        incPC( 2 );
        break;

        case Opcodes.BIT_ABS:
        BIT( abs() );
        incPC( 3 );
        break;

        case Opcodes.NOP_3c:
        abx();
        incPC( 3 );
        break;

        // CLC imp ////////////////////////////////////////////////////////////
        case Opcodes.CLC_IMP:
        _carry = false;
        incPC( 1 );
        break;

        // SEC imp ////////////////////////////////////////////////////////////
        case Opcodes.SEC_IMP:
        _carry = true;
        incPC( 1 );
        break;

        // CLD imp ////////////////////////////////////////////////////////////
        case Opcodes.CLD_IMP:
        _decimal = false;
        incPC( 1 );
        break;

        // SED imp ////////////////////////////////////////////////////////////
        case Opcodes.SED_IMP:
        _decimal = true;
        incPC( 1 );
        break;

        // CLI imp ////////////////////////////////////////////////////////////
        case Opcodes.CLI_IMP:
        _interrupt = false;
        incPC( 1 );
        break;

        // SEI imp ////////////////////////////////////////////////////////////
        case Opcodes.SEI_IMP:
        _interrupt = true;
        incPC( 1 );
        break;

        // CLV imp ////////////////////////////////////////////////////////////
        case Opcodes.CLV_IMP:
        _overflow = false;
        incPC( 1 );
        break;

        // NOP imp ////////////////////////////////////////////////////////////
        case Opcodes.NOP_IMP:
        incPC( 1 );
        break;

        // Illegal opcodes ////////////////////////////////////////////////////

        case Opcodes.NOP_1a:
        case Opcodes.NOP_3a:
        case Opcodes.NOP_5a:
        case Opcodes.NOP_7a:
        case Opcodes.NOP_da:
        case Opcodes.NOP_fa:
        incPC( 1 );
        break;

        case Opcodes.NOP_14:
        case Opcodes.NOP_34:
        case Opcodes.NOP_54:
        case Opcodes.NOP_74:
        case Opcodes.NOP_d4:
        case Opcodes.NOP_f4:
        _memory.read( Memory.mask8( _memory.read8( _pc + 1 ) + getX() ) );
        incPC(2);
        break;


        case Opcodes.JAM_02:
        case Opcodes.JAM_12:
        case Opcodes.JAM_22:
        case Opcodes.JAM_32:
        case Opcodes.JAM_42:
        case Opcodes.JAM_52:
        case Opcodes.JAM_62:
        case Opcodes.JAM_72:
        case Opcodes.JAM_92:
        case Opcodes.JAM_b2:
        case Opcodes.JAM_d2:
        case Opcodes.JAM_f2:
          _memory.read( _pc );
          incPC( 1 );
          break;

        case Opcodes.SLO_07:
        SLO( zp() ); // 5
        incPC(2);
        break;

        case Opcodes.ANC_0b:
        case Opcodes.ANC_2b:
        setAccu( getAccu() & _memory.read( imm() ) );
        setRegsNZ( _accu );
        _carry = _accu < 0;
        incPC(2);
        break;

        case Opcodes.SLO_17:
        SLO( zpx() ); // 6
        incPC(2);
        break;

        case Opcodes.SLO_03:
        SLO( izx() ); // 8
        incPC(2);
        break;

        case Opcodes.SLO_13:
        SLO( izy() ); // 8
        incPC(2);
        break;

        case Opcodes.SLO_0f:
        SLO( abs() ); // 6
        incPC(3);
        break;

        case Opcodes.SLO_1f:
        SLO( abx() ); // 7
        incPC(3);
        break;

        case Opcodes.SLO_1b:
        SLO( aby() ); // 7
        incPC(3);
        break;

        // DCM -- undocumented opcode /////////////////////////////////////////
        case Opcodes.DCM_c7:
        DCM( zp() );
        incPC(2);
        break;

        case Opcodes.DCM_d7:
        DCM( zpx() );
        incPC(2);
        break;

        case Opcodes.DCM_c3:
        DCM( izx() );
        incPC(2);
        break;

        case Opcodes.DCM_d3:
        DCM( izy() );
        incPC(2);
        break;

        case Opcodes.DCM_cf:
        DCM( abs() );
        incPC(3);
        break;

        case Opcodes.DCM_df:
        DCM( abx() );
        incPC(3);
        break;

        case Opcodes.DCM_db:
        DCM( aby() );
        incPC(3);
        break;

        // LAX -- illegal opcode //////////////////////////////////////////////
        case Opcodes.LAX_a7:
        LAX( zp() );
        incPC(2);
        break;

        case Opcodes.LAX_b7:
        LAX( zpy() );
        incPC(2);
        break;

        case Opcodes.LAX_a3:
        LAX( izx() );
        incPC(2);
        break;

        case Opcodes.LAX_b3:
        LAX( izy() );
        incPC(2);
        break;

        case Opcodes.LAX_af:
        LAX( abs() );
        incPC(3);
        break;

        case Opcodes.LAX_bf:
        LAX( aby() );
        incPC(3);
        break;

        // RLA -- illegal opcode //////////////////////////////////////////////
        case Opcodes.RLA_27:
        RLA( zp() );
        incPC(2);
        break;

        case Opcodes.RLA_37:
        RLA( zpx() );
        incPC(2);
        break;

        case Opcodes.RLA_23:
        RLA( izx() );
        incPC(2);
        break;

        case Opcodes.RLA_33:
        RLA( izy() );
        incPC(2);
        break;

        case Opcodes.RLA_2f:
        RLA( abs() );
        incPC(3);
        break;

        case Opcodes.RLA_3f:
        RLA( abx() );
        incPC(3);
        break;

        case Opcodes.RLA_3b:
        RLA( aby() );
        incPC(3);
        break;

        // RRA -- illegal opcode //////////////////////////////////////////////
        case Opcodes.RRA_67:
        RRA( zp() );
        incPC(2);
        break;

        case Opcodes.RRA_77:
        RRA( zpx() );
        incPC(2);
        break;

        case Opcodes.RRA_63:
        RRA( izx() );
        incPC(2);
        break;

        case Opcodes.RRA_73:
        RRA( izy() );
        incPC(2);
        break;

        case Opcodes.RRA_6f:
        RRA( abs() );
        incPC(3);
        break;

        case Opcodes.RRA_7f:
        RRA( abx() );
        incPC(3);
        break;

        case Opcodes.RRA_7b:
        RRA( aby() );
        incPC(3);
        break;

        // SAX -- undocumented opcode /////////////////////////////////////////
        case Opcodes.SAX_87:
        SAX( zp() );
        incPC(2);
        break;

        case Opcodes.SAX_97:
        SAX( zpy() );
        incPC(2);
        break;

        case Opcodes.SAX_83:
        SAX( izx() );
        incPC(2);
        break;

        case Opcodes.SAX_8f:
        SAX( abs() );
        incPC(3);
        break;

        // ISC zp -- undocumented opcode //////////////////////////////////////
        case Opcodes.ISC_e7:
        ISC( zp() );
        incPC(2);
        break;

        case Opcodes.ISC_f7:
        ISC( zpx() );
        incPC(2);
        break;

        case Opcodes.ISC_e3:
        ISC( izx() );
        incPC(2);
        break;

        case Opcodes.ISC_f3:
        ISC( izy() );
        incPC(2);
        break;

        case Opcodes.ISC_ef:
        ISC( abs() );
        incPC(3);
        break;

        case Opcodes.ISC_ff:
        ISC( abx() );
        incPC(3);
        break;

        case Opcodes.ISC_fb:
        ISC( aby() );
        incPC(3);
        break;

        // LSE -- undocumented opcode /////////////////////////////////////////
        case Opcodes.LSE_47:
        LSE( zp() );
        incPC(2);
        break;

        case Opcodes.LSE_57:
        LSE( zpx() );
        incPC(2);
        break;

        case Opcodes.LSE_43:
        LSE( izx() );
        incPC(2);
        break;

        case Opcodes.LSE_53:
        LSE( izy() );
        incPC(2);
        break;

        case Opcodes.LSE_4f:
        LSE( abs() );
        incPC(3);
        break;

        case Opcodes.LSE_5f:
        LSE( abx() );
        incPC(3);
        break;

        case Opcodes.LSE_5b:
        LSE( aby() );
        incPC(3);
        break;

        // NOP1 (SKB) - Undocumented opcode, skips 1 byte.
        case Opcodes.NOP_80:
        case Opcodes.NOP_82:
        case Opcodes.NOP_89:
        case Opcodes.NOP_c2:
        case Opcodes.NOP_e2:
        incPC(2);
        break;

        case Opcodes.NOP_04:
        case Opcodes.NOP_44:
        case Opcodes.NOP_64:
        incPC(2);
        break;

        // NOP2 (SKW) - Undocumented opcode, skips 2 bytes.
        case Opcodes.NOP_0c:
        case Opcodes.NOP_1c:
        case Opcodes.NOP_5c:
        case Opcodes.NOP_7c:
        case Opcodes.NOP_dc:
        case Opcodes.NOP_fc:
        incPC(3);
        break;

        case Opcodes.ALR_4b:
        AND( imm() );
        LSR();
        incPC( 2 );
        break;

        case Opcodes.ARR_6b:
        {
          // ARR: AND with immediate, ROR through carry; flag rules differ from ROR.
          int tmp = (_accu & _memory.read( imm() )) & 0xff;
          int t   = (tmp >> 1) | (_carry ? 0x80 : 0);
          setRegsNZ( (byte)t );
          _overflow = ((t >> 6) & 1) != ((t >> 5) & 1);
          if ( _decimal )
            {
            // Decimal conditions use the pre-rotation AND result (tmp).
            int result = t;
            if ( (tmp & 0x0f) + (tmp & 0x01) > 5 )
              result = (result & 0xf0) | ((result + 6) & 0x0f);
            if ( (tmp & 0xf0) + (tmp & 0x10) > 0x50 )
              {
              result = (result & 0x0f) | ((result + 0x60) & 0xf0);
              _carry = true;
            }
            else
              _carry = false;
            _accu = (byte)result;
          }
          else
            {
            _carry = (t & 0x40) != 0;
            _accu  = (byte)t;
          }
          incPC(2);
          break;
        }

        case Opcodes.XXA_8b:
        {
          // XXA/ANE: unstable; magic constant 0xEE matches the test suite's target chip.
          _accu = (byte)((_accu | 0xEE) & _x & _memory.read( imm() ));
          setRegsNZ( _accu );
          incPC(2);
          break;
        }

        case Opcodes.AHX_93:
        {
          // AHX: undocumented; stores A & X & high byte of address.
          _memory.write( zp() , (byte)(_accu & _x & ((_pc >> 8) + 1)) );
          incPC(2);
          break;
        }

        case Opcodes.TAS_9b:
        {
          // TAS/SHS: S = A&X; mem[base+Y] = A&X & (high(base)+1)
          int base = abs();
          int effective = Memory.mask16( base + getY() );
          _stack = (byte)(_accu & _x);
          _memory.write( effective, (byte)(_stack & ((base >> 8) + 1)) );
          incPC(3);
          break;
        }

        case Opcodes.SHY_9c:
        {
          // SHY/SAY: mem[base+X] = Y & (base_high+1); on page cross, high byte of addr is also replaced.
          SH_YXA(getX(), getY());
          break;
        }

        case Opcodes.SHX_9e:
        {
          // SHX: mem[base+Y] = X & (base_high+1); on page cross, high byte of addr is also replaced.
          SH_YXA(getY(), getX());
          break;
        }

        case Opcodes.AHX_9f:
        {
          // AHX/SHA: mem[base+Y] = A&X & (base_high+1); on page cross, high byte of addr is also replaced.
          SH_YXA( getX() & getAccu() , getY());
          break;
        }

        case Opcodes.LXA_ab:
        {
          // LXA/ATX/OAL: unstable; magic constant 0xEE matches the test suite's target chip.
          _accu = (byte)((_accu | 0xEE) & _memory.read( imm() ));
          _x = _accu;
          setRegsNZ( _accu );
          incPC(2);
          break;
        }

        case Opcodes.LAS_bb:
        {
          // LAS/LAR: result = mem[abs+Y] & S; A = X = S = result
          int val = (_memory.read( aby() ) & _stack) & 0xff;
          _accu = (byte)val;
          _x    = (byte)val;
          _stack = (byte)val;
          setRegsNZ( (byte)val );
          incPC(3);
          break;
        }

        case Opcodes.AXS_cb:
        {
          // AXS/SBX: X = (A & X) - imm; no borrow in, C = no-borrow out.
          int ax  = (_accu & _x) & 0xff;
          int op  = _memory.read( imm() ) & 0xff;
          int res = ax - op;
          _x     = (byte)res;
          _carry = (ax >= op);
          setRegsNZ( _x );
          incPC(2);
          break;
        }

        case Opcodes.SBC_eb:
        {
          SBC( imm() );
          incPC(2);
          break;
        }

        // Handle unknown opcodes.
        default:
        System.err.println( "Unknown opcode $" +
        Integer.toHexString( opcode ) +
        "@" +
        Integer.toHexString( _pc & 0xffff ) );
        System.err.println( "State: " + toString() );
        reset();
      }

      _clockId.advance( _cycles );
    }



    /**
    * Get the status flags.
    *
    * @return The contents of the status register.  Only bit7-0 are valid,
    * remaining bits are 0.
    * @see de.michab.simulator.mos6502.Cpu6510#setStatusRegister
    */
    public final int getStatusRegister()
    {
      // Bit 5 is always set.
      int result = STATUS_FLAG_CONST_ONE;
      // Compose a byte containing all status register flags...
      if ( _negative )
        result |= STATUS_FLAG_NEGATIVE; // Bit 7.
      if ( _overflow )
        result |= STATUS_FLAG_OVERFLOW; // Bit 6.
      if ( _break )
        result |= STATUS_FLAG_BREAK; // Bit 4.
      if ( _decimal )
        result |= STATUS_FLAG_DECIMAL; // Bit 3.
      if ( _interrupt )
        result |= STATUS_FLAG_INTERRUPT; // Bit 2.
      if ( _zero )
        result |= STATUS_FLAG_ZERO;
      if ( _carry )
        result |= STATUS_FLAG_CARRY; // Bit 0.
      // ...and return that byte.
      return Memory.mask8(result);
    }



    /**
    * Set our status flags from a byte value.
    *
    * @param status The new value of the status register.
    */
    public final void setStatusRegister( byte status )
    {
      _carry     = 0 != (status & STATUS_FLAG_CARRY);
      _zero      = 0 != (status & STATUS_FLAG_ZERO);
      _interrupt = 0 != (status & STATUS_FLAG_INTERRUPT);
      _decimal   = 0 != (status & STATUS_FLAG_DECIMAL);
      _break     = 0 != (status & STATUS_FLAG_BREAK);
      _overflow  = 0 != (status & STATUS_FLAG_OVERFLOW);
      _negative  = 0 != (status & STATUS_FLAG_NEGATIVE);
    }



    /**
    * Test a single status register flag.
    *
    * @param statusEnum One of the status register constants, e.g.
    *        STATUS_FLAG_BREAK.
    * @return The boolean value of the status flag.
    * @see Cpu6510#STATUS_FLAG_BREAK
    * @see Cpu6510#STATUS_FLAG_CARRY
    * @see Cpu6510#STATUS_FLAG_DECIMAL
    * @see Cpu6510#STATUS_FLAG_INTERRUPT
    * @see Cpu6510#STATUS_FLAG_NEGATIVE
    * @see Cpu6510#STATUS_FLAG_OVERFLOW
    * @see Cpu6510#STATUS_FLAG_ZERO
    */
    public boolean isStatusFlagSet( int statusEnum )
    {
      boolean result = false;

      switch ( statusEnum )
      {
        case STATUS_FLAG_CARRY:
        result = _carry;
        break;

        case STATUS_FLAG_ZERO:
        result = _zero;
        break;

        case STATUS_FLAG_INTERRUPT:
        result = _interrupt;
        break;

        case STATUS_FLAG_DECIMAL:
        result = _decimal;
        break;

        case STATUS_FLAG_BREAK:
        result = _break;
        break;

        case STATUS_FLAG_OVERFLOW:
        result = _overflow;
        break;

        case STATUS_FLAG_NEGATIVE:
        result = _negative;
        break;

        default:
        System.err.println( "Invalid enumeration element: " + statusEnum );
        new Throwable().printStackTrace();
        System.exit( 1 );
      }

      return result;
    }



    /**
    * Initialises the program counter from the reset vector.
    */
    public void reset()
    {
      // Fetch the start vector from memory.  Registers aren't cleared on reset.
      _interruptPending = _memory.getVectorAt( RESET_VECTOR );
      _interruptPendingType = INT_RESET;
    }



    /**
    * Push the program counter onto the stack.
    */
    private void pushPc()
    {
      byte lo = (byte)_pc;
      byte hi = (byte)(_pc >>> 8);
      _memory.write( decrementStack(), hi );
      _memory.write( decrementStack(), lo );
    }
    private void pushPc( int pc )
    {
      byte lo = (byte)pc;
      byte hi = (byte)(pc >>> 8);
      _memory.write( decrementStack(), hi );
      _memory.write( decrementStack(), lo );
    }



    /**
    * Get the program counter from top of stack.
    */
    private void popPc()
    {
      int lo = _memory.read8( incrementStack() );
      int hi = _memory.read8( incrementStack() );
      hi <<= 8;
      _pc = hi | lo;
    }

    /**
    * Add with carry.  Status register setting is checked against VICE.
    *
    * @param operandAdr The address of the operand.
    * @see de.michab.simulator.mos6502.Cpu6510#SBC
    */
    private void ADC( int operandAdr )
    {
      // Note:  Just for clarification, we have two accu variables in this
      // method.  'accu' w/o underscore means the local copy of the value from
      // the '_accu' register.
      int accu =_accu;
      accu &= 0xff;
      int op = _memory.read( operandAdr );
      op &= 0xff;

      if ( _decimal )
        {
        // This code came from vice, simple transformation.

        // Add the lsb digits.
        int result = (accu & 0xf) + (op & 0xf) + (_carry ? 1 : 0);
        // If we left the bcd range...
        if ( result > 0x9 )
          // ...normalise the result.
        result += 0x6;
        // If we stayed in the lsb digit...
        if ( result <= 0x0f )
          // ...add up the msb digit.
        result = (result & 0xf) + (accu & 0xf0) + (op & 0xf0);
        else
          result = (result & 0xf) + (accu & 0xf0) + (op & 0xf0) + 0x10;

        // Compute the status register flags.
        _zero = 0 == ((accu + op + (_carry ? 1 : 0)) & 0xff);
        _negative = 0 != (result & 0x80);
        _overflow = (0 != (((accu ^ result) & 0x80)) &&
        (0 == ((accu ^ op) & 0x80)));
        if ((result & 0x1f0) > 0x90)
          result += 0x60;
        _carry = ((result & 0xff0) > 0xf0);
        _accu = (byte)result;
      }
      else
        {
        int result = accu + op + (_carry ? 1 : 0);

        _accu = (byte)result;

        // Set registers.
        setRegsNZ( _accu );
        // Overflow is set if the initial accu and the value to add have the same
        // sign *and* if the signs of the initial accu and the result differ.
        _overflow = (((accu ^ op) & BIT_7) == 0)
        && (((accu ^ result) & BIT_7) != 0);
        // Carry flag is true when the result doesn't fit into an unsigned byte.  This is
        // tested in VICE with the following code: _carry = result >
        // 0xff; where result is unsigned.  Since java knows nothing
        // about unsigned integers this has to be translated into the following
        // code:
        _carry = (result & 0xffffff00) != 0;
      }
    }



    /**
    * And.
    *
    * @param operandAdr The operand's address.
    */
    private void AND( int operandAdr )
    {
      _accu &= _memory.read( operandAdr );
      // Set registers.
      setRegsNZ( _accu );
    }



    /**
    * Arithmetic shift left implicit.
    */
    private void ASL()
    {
      // Move the most significant bit into the carry register.
      _carry = _accu < 0;
      // Perform the shift.
      _accu <<= 1;
      // Set registers.
      setRegsNZ( _accu );
    }



    /**
    * Arithmetic shift left.
    *
    * @param operandAdr The operand's address.
    */
    private void ASL( int operandAdr )
    {
      // Read the operand.
      byte operand = _memory.read( operandAdr );
      // Move the most significant bit into the carry register.
      _carry = operand < 0;
      // Perform the shift.
      operand <<= 1;
      // Write the operand.
      _memory.write( operandAdr, operand );
      // Set registers.
      setRegsNZ( operand );
    }

    /**
    * Arithmetic shift left and OR.  ASLs the contents of a memory location and
    * then ORs the result with the accumulator.  This is an illegal opcode.
    *
    * @param operandAdr The operand's address.
    */
    private void SLO( int operandAdr )
    {
      ASL( operandAdr );
      ORA( operandAdr );
    }

    /**
    * The magic BIT instruction.  Never understood this one.
    *
    * @param operandAdr The operand's address.
    */
    private void BIT( int operandAdr )
    {
      byte operand = _memory.read( operandAdr );
      _negative = operand < 0;
      _overflow = (operand & BIT_6) != 0;
      _zero = (_accu & operand) == 0;
    }

    /**
    * Implements the common logic for the branch instructions.
    *
    * @param condition The condition value for the branch.
    */
    private void branchOn( boolean condition )
    {
      if ( condition )
        _pc = rel();
      else
        incPC(2);
    }



    /**
    * Break (software interrupt).
    *
    * @see de.michab.simulator.mos6502.Cpu6510#RTI
    */
    private void BRK()
    {
      // Push  program counter for the next opcode.
      _pc += 1;
      pushPc();
      // Set break flag.
      _break = true;
      // Push the status register
      _memory.write( decrementStack(), (byte)getStatusRegister() );
      // Reset the break flag.  This is only set in the pushed status register.
      _break = false;
      // Set interrupt flag *after* pushing the status register.
      _interrupt = true;
      // Set the break vector.
      _pc = _memory.getVectorAt( IRQ_VECTOR );
    }



    /**
    * This implements the common logic of the compare instructions CMP, CPX, CPY
    * across all adressing modes.
    *
    * @param operandAdr The operand address.
    * @param register The referred register's contents.
    * @see de.michab.simulator.mos6502.Cpu6510#_negative
    * @see de.michab.simulator.mos6502.Cpu6510#_carry
    * @see de.michab.simulator.mos6502.Cpu6510#_zero
    */
    private void cmpImpl( int operandAdr, byte register )
    {
      int scratch_integer_1 = Memory.mask8(register);
      int scratch_integer_2 = _memory.read8( operandAdr );
      scratch_integer_1 -= scratch_integer_2;
      byte scratch_byte_1 = (byte)scratch_integer_1;

      // Set the flags.
      setRegsNZ( scratch_byte_1 );
      // Carry flag is true when the result fits into an unsigned byte.  This is
      // tested in VICE with the following code: _carry = scratch_integer_1 <
      // 0x100; where scratch_integer_1 is unsigned.  Since java knows nothing
      // about unsigned integers this has to be translated into the following
      // code:
      _carry = (scratch_integer_1 & 0xffffff00) == 0;
    }

    /**
    * Compare.
    *
    * @param operandAdr The operand's address.
    */
    private void CMP( int operandAdr )
    {
      cmpImpl( operandAdr, _accu );
    }

    /**
    * Compare to x register.
    *
    * @param operandAdr The operand's address.
    */
    private void CPX( int operandAdr )
    {
      cmpImpl( operandAdr, _x );
    }



    /**
    * Compare to y register.
    *
    * @param operandAdr The operand's address.
    */
    private void CPY( int operandAdr )
    {
      cmpImpl( operandAdr, _y );
    }



    /**
    * Decrement and compare memory, also known as DCP.  Decrements the contents
    * of a memory location and then compares the result with the accu.  This is
    * an undocumented opcode.
    *
    * @param operandAdr The operand's address.
    */
    private void DCM( int operandAdr )
    {
      DEC( operandAdr );
      CMP( operandAdr );
    }



    /**
    * Decrement.
    *
    * @param operandAdr The operand's address.
    */
    private void DEC( int operandAdr )
    {
      // Read the operand from memory.
      byte operand = _memory.read( operandAdr );
      // Perform the operation.
      operand--;
      // Write the result back.
      _memory.write( operandAdr, operand );
      // Set registers.
      setRegsNZ( operand );
    }



    /**
    * Decrement x register.
    *
    * @see Cpu6510#DEY()
    */
    private void DEX()
    {
      // Perform the operation.
      _x--;
      // Set registers.
      setRegsNZ( _x );
    }



    /**
    * Decrement y register.
    *
    * @see Cpu6510#DEX()
    */
    private void DEY()
    {
      // Perform the operation.
      _y--;
      // Set registers.
      setRegsNZ( _y );
    }



    /**
    * Exclusive or.
    *
    * @param operandAdr The operand's address.
    */
    private void EOR( int operandAdr )
    {
      // Perform the operation.
      _accu ^= _memory.read( operandAdr );
      // Set registers
      setRegsNZ( _accu );
    }



    /**
    * Increment memory location.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#INX()
    * @see de.michab.simulator.mos6502.Cpu6510#INY()
    */
    private void INC( int operandAdr )
    {
      // Read the operand.
      byte operand = _memory.read( operandAdr );
      // Perform the operation.
      operand++;
      // Write the result.
      _memory.write( operandAdr, operand );
      // Set registers.
      setRegsNZ( operand );
    }



    /**
    * Increment the x register.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#INC
    * @see de.michab.simulator.mos6502.Cpu6510#INY
    */
    private void INX()
    {
      // Perform the operation.
      _x++;
      // Set registers.
      setRegsNZ( _x );
    }



    /**
    * Increment the y register.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#INC
    * @see de.michab.simulator.mos6502.Cpu6510#INX
    */
    private void INY()
    {
      // Perform the operation.
      _y++;
      // Set registers.
      setRegsNZ( _y );
    }



    /**
    * ISC This is an illegal opcode.
    *
    * @param operandAddr The operand's address.
    */
    private void ISC( int operandAddr )
    {
      int val = (_memory.read( operandAddr ) + 1) & 0xff;
      _memory.write( operandAddr, (byte)val );
      SBC( operandAddr );
    }



    /**
    * Load accu and x register.  This is an illegal instruction.
    *
    * @param operandAdr The operand's address.
    */
    private void LAX( int operandAdr )
    {
      LDA( operandAdr );
      LDX( operandAdr );
    }



    /**
    * Load accu.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#LDX
    * @see de.michab.simulator.mos6502.Cpu6510#LDY
    */
    private void LDA( int operandAdr )
    {
      // Perform the operation.
      _accu = _memory.read( operandAdr );
      // Set the registers.
      setRegsNZ( _accu );
    }



    /**
    * Load x register.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#LDY
    * @see de.michab.simulator.mos6502.Cpu6510#LDA
    */
    private void LDX( int operandAdr )
    {
      _x = _memory.read( operandAdr );
      // Set the registers.
      setRegsNZ( _x );
    }



    /**
    * Load y register.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#LDX
    * @see de.michab.simulator.mos6502.Cpu6510#LDA
    */
    private void LDY( int operandAdr )
    {
      // Perform the operation.
      _y = _memory.read( operandAdr );
      // Set the registers.
      setRegsNZ( _y );
    }



    /**
    * LSE (SRE) LSRs the contents of a memory location and then EORs the result
    * with the accumulator.  This is an undocumented opcode.
    *
    * @param operandAdr The operand's address.
    */
    private void LSE( int operandAdr )
    {
      LSR( operandAdr );
      EOR( operandAdr );
    }

    /**
    * Logical shift right implicit.
    */
    private void LSR()
    {
      // We can't shift this in place because of 'Unary numeric promotion'.  See
      // Java language specification �5.6.1
      int wideAccu =  Memory.mask8( _accu );
      // Move the least significant bit into carry.
      _carry = (wideAccu & 1) != 0;
      // Perform the shift...
      wideAccu >>>= 1;
      // ...and write the result back into the accu.
      _accu = (byte)wideAccu;
      // Set registers.
      setRegsNZ( _accu );
    }

    /**
    * Logical shift right
    *
    * @param operandAdr The operand's address.
    */
    private void LSR( int operandAdr )
    {
      // Read the operand from memory.  This has to be of int type because of
      // 'Unary numeric promotion'.  See Java language specification �5.6.1
      int operand = _memory.read8( operandAdr );
      // Move the least significant bit into carry.
      _carry = (operand & 1) != 0;
      // Perform the shift.  This is an unsigned divide.
      operand >>>= 1;
      // Write the result.
      _memory.write( operandAdr, (byte)operand );
      // Set registers.
      setRegsNZ( (byte)operand );
    }

    /**
    * Or accu.
    *
    * @param operandAdr The operand's address.
    */
    private void ORA( int operandAdr )
    {
      _accu |= _memory.read( operandAdr );
      // Set registers.
      setRegsNZ( _accu );
    }



    /**
    * Push accu.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#PLA
    */
    private void PHA()
    {
      // Perform the operation.
      _memory.write( decrementStack(), _accu );
    }



    /**
    * Push processor flags.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#PLP
    */
    private void PHP()
    {
      // Perform the operation.
      _memory.write( decrementStack(), (byte)getStatusRegister() );
    }



    /**
    * Pull accu.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#PHA
    */
    private void PLA()
    {
      // Perform the operation.
      _accu = _memory.read( incrementStack() );
      // Set the registers.
      setRegsNZ( _accu );
    }



    /**
    * Pull processor flags.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#PHP
    */
    private void PLP()
    {
      setStatusRegister( _memory.read( incrementStack() ) );
      _break = false;

    }



    /**
    * ROLs the contents of a memory location and then ANDs the result with the
    * accumulator.  Illegal opcode.
    *
    * @param operandAdr The operand's address.
    */
    private void RLA( int operandAdr )
    {
      ROL( operandAdr );
      AND( operandAdr );
    }



    /**
    * Rotate left implicit.
    * @see de.michab.simulator.mos6502.Cpu6510#ROR()
    */
    private void ROL()
    {
      // Save carry.
      boolean carrySave = _carry;
      // Check the msb and set carry accordingly.
      _carry = _accu < 0;
      // Do the shift.
      _accu <<= 1;
      // Add ol' carry
      if ( carrySave )
        _accu |= 1;
      // Registers.
      setRegsNZ( _accu );
    }



    /**
    * Rotate left.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#ROR(int)
    */
    private void ROL( int operandAdr )
    {
      byte operand = _memory.read( operandAdr );
      // Save carry.
      boolean originalCarry = _carry;
      // Check the msb and set carry accordingly.
      _carry = operand < 0;
      // Do the shift.
      operand <<= 1;
      // Add ol' carry
      if ( originalCarry )
        operand |= 1;
      // Write result back.
      _memory.write( operandAdr, operand );
      // Set registers.
      setRegsNZ( operand );
    }



    /**
    * Rotate right implicit.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#ROL()
    */
    private void ROR()
    {
      // We can't shift this in place because of 'Unary numeric promotion'.  See
      // Java language specification �5.6.1
      int wideAccu = _accu & 0xff;
      // Place the existing carry bit into bit 8...
      if ( _carry )
        wideAccu |= 0x100;
      // ...and compute the new one.
      _carry = (wideAccu & 1) != 0;
      // Perform the operation...
      wideAccu >>>= 1;
      // ...and place the result in the accu.
      _accu = (byte)wideAccu;
      // Set the registers.
      setRegsNZ( _accu );
    }



    /**
    * Rotate right.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#ROL(int)
    */
    private void ROR( int operandAdr )
    {
      // Read the operand from memory.  This has to be of int type because of
      // 'Unary numeric promotion'.  See Java language specification �5.6.1
      int operand = _memory.read8( operandAdr );
      // Place the existing carry bit into bit 8...
      if ( _carry )
        operand |= 0x100;
      // ...and compute the new one.
      _carry = (operand & 1) != 0;
      // Do the shift...
      operand >>>= 1;
      // ...and write the result back into memory...
      _memory.write( operandAdr, (byte)operand );
      // ...and set the registers.
      setRegsNZ( (byte)operand );
    }



    /**
    * RORs the contents of a memory location and then ADCs the result with the
    * accumulator.  This is an illegal opcode.
    *
    * @param operandAdr The operand's address.
    */
    private void RRA( int operandAdr )
    {
      ROR( operandAdr );
      ADC( operandAdr );
    }



    /**
    * Return from interrupt.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#BRK()
    */
    private void RTI()
    {
      // Pop status register.
      setStatusRegister( _memory.read( incrementStack() ) );
      _break = false;

      // Pop program counter.
      popPc();
    }



    /**
    * SAX.  Set operand address to A&X.  This is an illegal opcode.
    *
    * @param operandAdr The operand's address.
    */
    private void SAX( int operandAdr )
    {
      int accu = Memory.mask8( _accu );
      int x = Memory.mask8( _x );

      _memory.write( operandAdr, (byte)(accu & x) );
    }



    /**
    * Subtract with carry.  Checked against VICE status register setting.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#ADC
    */
    private void SBC( int operandAdr )
    {
      // Perform the operation.
      int accu = Memory.mask8( _accu );
      int op = _memory.read8( operandAdr );

      boolean originalCarry = _carry;
      int result = accu - op;
      if ( ! _carry )
        result--;

      // Set registers.  This is common code for decimal and non-decimal mode.
      setRegsNZ( (byte)result );
      // Overflow is set if the initial accu and the result have different signs
      // *and* initial accu and the initial operand have different signs.
      _overflow = (((accu ^ result) & 0x80) != 0)
      && (((accu ^ op) & 0x80) != 0);
      // Carry flag is true when the result fits into an unsigned byte.  This is
      // tested in VICE with the following code: _carry = result <
      // 0x100; where result is unsigned.  Since java knows nothing
      // about unsigned integers this has to be translated into the following
      // code:
      _carry = (result & 0xffffff00) == 0;

      // Check if we are in decimal mode.
      if ( _decimal )
        {
        // NMOS 6502 decimal adjust: al tracks low nibble with borrow propagation.
        int al = (accu & 0x0f) - (op & 0x0f) - (originalCarry ? 0 : 1);
        if ( al < 0 )
          al = ((al - 6) & 0x0f) - 0x10;
        int deca = (accu & 0xf0) - (op & 0xf0) + al;
        if ( deca < 0 )
          deca -= 0x60;

        _accu = (byte)deca;
      }
      else
        _accu = (byte)result;
    }



    /**
    * Store accu.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#STX(int)
    * @see de.michab.simulator.mos6502.Cpu6510#STY(int)
    */
    private void STA( int operandAdr )
    {
      _memory.write( operandAdr, _accu );
    }



    /**
    * Store x register.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#STY(int)
    * @see de.michab.simulator.mos6502.Cpu6510#STA(int)
    */
    private void STX( int operandAdr )
    {
      _memory.write( operandAdr, _x );
    }



    /**
    * Store y register.
    *
    * @param operandAdr The operand's address.
    * @see de.michab.simulator.mos6502.Cpu6510#STA(int)
    * @see de.michab.simulator.mos6502.Cpu6510#STX(int)
    */
    private void STY( int operandAdr )
    {
      _memory.write( operandAdr, _y );
    }



    /**
    * Transfer accu to x register.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#TXA()
    * @see de.michab.simulator.mos6502.Cpu6510#TAY()
    * @see de.michab.simulator.mos6502.Cpu6510#TYA()
    * @see de.michab.simulator.mos6502.Cpu6510#TSX()
    * @see de.michab.simulator.mos6502.Cpu6510#TXS()
    */
    private void TAX()
    {
      // Perform the operation.
      _x = _accu;
      // Set the registers.
      setRegsNZ( _x );
    }



    /**
    * Transfer accu to y register.
    *
    * @see de.michab.simulator.mos6502.Cpu6510#TAX()
    * @see de.michab.simulator.mos6502.Cpu6510#TXA()
    * @see de.michab.simulator.mos6502.Cpu6510#TAY()
    * @see de.michab.simulator.mos6502.Cpu6510#TYA()
    * @see de.michab.simulator.mos6502.Cpu6510#TSX()
    * @see de.michab.simulator.mos6502.Cpu6510#TXS()
    */
    private void TAY()
    {
      // Perform the operation.
      _y = _accu;
      // Set the registers.
      setRegsNZ( _y );
    }



    /**
    * Transfer stack to x register.
    */
    private void TSX()
    {
      // Perform the operation.  This is the only one place where the stack
      // pointer is touched as it is, not via in/decrement.
      _x = _stack;
      // Set the registers.
      setRegsNZ( _x );
    }



    /**
    * Transfer x register to accu.
    */
    private void TXA()
    {
      // Perform the operation.
      _accu = _x;
      // Set the registers.
      setRegsNZ( _accu );
    }



    /**
    * Transfer x register to stack pointer.  Touching the stack pointer
    * attribute is ok here.
    */
    private void TXS()
    {
      // Assign the register to the stack.
      _stack = _x;
    }



    /**
    * Transfer y register to accu.
    */
    private void TYA()
    {
      // Perform the operation.
      _accu = _y;
      // Set the registers.
      setRegsNZ( _accu );
    }



    /**
    * Set negative and zero register based on passed value.  The passed value
    * represents a register's contents that is the base for the computation of
    * the new zero and negative status flags.
    *
    * @param onWhat The respective register's contents.
    */
    private void setRegsNZ( byte onWhat )
    {
      _negative = onWhat < 0;
      _zero = onWhat == 0;
    }

    /**
    * Addressing mode immediate #$00
    * Cycles: +0
    *
    * @return The operand address.
    */
    private int imm()
    {
      return Memory.mask16( _pc + 1 );
    }

    /**
    * Addressing mode zeropage $00
    *
    * @return The operand address.
    */
    private int zp()
    {
      return _memory.read8(
        Memory.mask16( _pc+1 ) );
      }

      /**
      * Addressing mode zeropage with index.
      *
      * @param index
      * @return The operand address.
      */
      private int zp( byte index )
      {
        int result = Memory.mask8(index);
        result += zp();
        return Memory.mask8(result);
      }

      /**
      * Addressing mode zeropage $00,x
      *
      * @return The operand address.
      */
      private int zpx()
      {
        return zp( _x );
      }

      /**
      * Addressing mode zeropage $00,y
      *
      * @return The operand address.
      */
      private int zpy()
      {
        return zp( _y );
      }

      /**
      * Addressing mode: indexed indirect ($00,X)
      *
      * @return The effective address.
      */
      private int izx()
      {
        return ind( zpx() );
      }

      /**
      * Addressing mode: indirect indexed ($00),Y
      *
      * @return The operand address.
      */
      private int izy()
      {
        // Perform indirect addressing.
        int base = _memory.read8( Memory.mask16( _pc+1 ) );
        base = ind( base );

        int offset = Memory.mask8( _y );
        int result = Memory.mask16(base + offset);

        if ( ! samePage( base, result ) )
          _cycles++;

        return result;
      }



      /**
      * Addressing mode: absolute $0000
      *
      * @return The operand address.
      */
      private int abs()
      {
        return _memory.getVectorAt( Memory.mask16( _pc+1 ) );
      }

      /**
      * Absolute addressing.  Increments the cycle counter by one if a page
      * boundary is crossed.
      *
      * @param index
      * @return The operand address.
      */
      private int abs( byte index )
      {
        int base = abs();

        int result = Memory.mask16(
          Memory.mask8(index) + base );

          if ( ! samePage( base, result ) )
            _cycles++;

          return result;
        }

        /**
        * Addressing mode: absolute indexed x $0000,x
        *
        * @return The operand address.
        */
        private int abx()
        {
          return abs( _x );
        }



        /**
        * Addressing mode: absolute indexed y $0000,y
        *
        * @return The operand address.
        */
        private int aby()
        {
          return abs( _y );
        }



        /**
        * Addressing mode: indirect ($0000)
        *
        * @return The operand address.
        */
        private int ind()
        {
          return ind( abs() );
        }



        /**
        * Indirect addressing mode with the indirection address as an argument.
        * Note that indirect addressing will never cross page boundaries, i.e. if
        * an address of 0x00ff is passed into this operation, the effective address
        * is loaded from 0x00ff and 0x0000.
        *
        * @param address The indirection address.
        * @return The effective address.
        */
        private int ind( int address )
        {
          // Indirect addressing does not handle page crossing.  If our initial
          // address above was 01ff, then the following expression accesses the high
          // byte at 0x0100.
          int page = address & 0xff00;
          int offset = address & 0xff;
          int hiByte = _memory.read( page | (0xff & ( offset + 1 )) );
          hiByte &= 0xff;
          // Low byte access is easy.
          int loByte = _memory.read( address );
          loByte &= 0xff;
          return (hiByte << 8) | loByte;
        }



        /**
        * Addressing mode: relative (branch instructions)
        *
        * @return The operand address.
        */
        private int rel()
        {
          // Adress of the next instruction.
          int next =
          Memory.mask16( _pc + 2 );
          // Add the signed offset.
          int target =
          next +
          Memory.mask16( _memory.read( _pc + 1 ) );

          // Update cycle count.  We are only called in case the branch is taken.
          // Takes one cycle on same page, two if page is crossed.
          if ( samePage( next, target ) )
            _cycles += 1;
          else
            _cycles += 2;

          return Memory.mask16( target );
        }



        /**
        * Access the stack with pre increment.  Used for pop operations.
        *
        * @return The incremented stack value.
        */
        private int incrementStack()
        {
          // Increment the stack.
          ++_stack;
          // And read the stack.
          return readStack();
        }



        /**
        * Access the stack with post decrement.  Used for push operations.
        *
        * @return The stack value.
        */
        private int decrementStack()
        {
          // Get the current stack pointer.
          int result = readStack();
          // Decrement the stack.
          _stack--;
          // Return the initial stack pointer.
          return result;
        }



        /**
        * Get the current stack pointer.
        *
        * @return The current stack pointer.
        */
        private int readStack()
        {
          // Make an unsigned integer from the stack.
          int result = _stack;
          result &= 0xff;
          // Compute the current stack pointer.
          return STACK_PAGE | result;
        }



        /**
        * Returns a string representation of this processor for debug purposes.
        *
        * @return A string representation of this processor for debug purposes.
        */
        public String toString()
        {
          int sp = readStack();

          return "Cpu(" +
          " pc = " + Integer.toHexString(_pc) +
          "; zero = " + _zero +
          "; negative = " + _negative +
          "; break = " + _break +
          "; carry = " + _carry +
          "; interrupt = " + _interrupt +
          "; overflow = " + _overflow +
          "; decimal = " + _decimal +
          "; x = " + Integer.toHexString( getX() ) +
          "; y = " + Integer.toHexString( getY() ) +
          "; accu = " + Integer.toHexString( getAccu() ) +
          "; stack = " + Integer.toHexString( sp ) +
          ":" + Integer.toHexString( 0xff & _memory.read( sp+1 ) ) +
          "." + Integer.toHexString( 0xff & _memory.read( sp+2 ) ) +
          "." + Integer.toHexString( 0xff & _memory.read( sp+3 ) ) +
          "." + Integer.toHexString( 0xff & _memory.read( sp+4 ) ) +
          "." + Integer.toHexString( 0xff & _memory.read( sp+5 ) ) +
          "; cycles=" + _cycles + " )";
        }



        /**
        * Sets a debugger on this processor instance.  To switch off debugging set
        * this to <code>null</code>.
        *
        * @param d The debugger to be set.
        * @see de.michab.simulator.Processor#setDebugger( Debugger )
        */
        public void setDebugger( Debugger d )
        {
          // If there is a current debugger...
          if ( _debugger != null )
            // ...reset its processor back ref.
          _debugger.setProcessor( null );

          // If a new debugger is set...
          if ( d != null )
            // ...set its processor back ref.
          d.setProcessor( this );

          // Finally set our debugger.
          _debugger = d;
        }



        /**
        * Checks whether the passed 16 bit addresses are located on the same 256
        * byte memory page.  This is the case if the upper 8 bit are equal.
        *
        * @param a The first address.
        * @param b The second address.
        * @return If both addresses are on the same page, <code>true</code> is
        *         returned, otherwise <code>false</code>.
        */
        private boolean samePage( int a, int b )
        {
          return (a & 0xff00) == (b & 0xff00);
        }
      }
