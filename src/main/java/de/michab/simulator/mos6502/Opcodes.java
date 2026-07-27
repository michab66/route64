/*  https://github.com/michab66/route64
 *
 * Copyright (c) 2003-2026 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import de.michab.simulator.Memory;
import java.util.Arrays;

/**
 * <p>Defines instruction encodings.  Note that the attribute names are used to
 * formally encode the addressing mode.  This is used in reflective code to
 * compute a human readable instruction representation.</p>
 *
 * <p>The general format is
 * <code>&lt;instructionName&gt;_&lt;addressingMode&gt;</code>, where the name
 * is defined in numerous 6502 assembly language books and the addressingMode
 * is one of:</p>
 *
 * <table
 *    border="2"
 *    cellpadding="2"
 *    cellpadding="0"
 *    summary="Addressing mode encodings.">
 *
 *    <tr>
 *          <th>Suffix</th>
 *          <th>Comment</th>
 *    </tr>
 *    <tr>
 *      <td><code>IMP</code></td> <td>Implicit</td>
 *    </tr>
 *    <tr>
 *      <td><code>IMM</code></td> <td>Immediate</td>
 *    </tr>
 *    <tr>
 *      <td><code>ZP</code></td>  <td>Zero page</td>
 *    </tr>
 *    <tr>
 *      <td><code>ZPX</code></td> <td>Zero page, x-register relative</td>
 *    </tr>
 *    <tr>
 *      <td><code>ZPY</code></td> <td>Zero page, y-register relative</td>
 *    </tr>
 *    <tr>
 *      <td><code>ABS</code></td> <td>Absolute</td>
 *    </tr>
 *    <tr>
 *      <td><code>ABSX</code></td> <td>Absolute x</td>
 *    </tr>
 *    <tr>
 *      <td><code>ABSY</code></td> <td>Absolute y</td>
 *    </tr>
 *    <tr>
 *      <td><code>IND</code></td> <td>Indirect</td>
 *    </tr>
 *    <tr>
 *      <td><code>IZX</code></td> <td>X indirect</td>
 *    </tr>
 *    <tr>
 *      <td><code>IZY</code></td> <td>Y indirect</td>
 *    </tr>
 *    <tr>
 *      <td><code>REL</code></td> <td>Relative</td>
 *    </tr>
 * </table>
 *
 * @author Michael G. Binz
 */
final public class Opcodes
{
  /**
   * Prevents instantiation.
   */
  private Opcodes()
  {
  }

    /**
     * Decode an instruction.
     *
     * @param pc The buffer position of the instruction to decode.
     * @param buffer The buffer containing the instruction.
     * @return The decoded instruction as a String.
     */
    public static String decode( int pc, byte[] buffer )
    {
        int opcode =
            (buffer[pc]&0xff);
        int len =
            Opcodes.getEncodingLength( opcode );

        // Check if there is enough room in the buffer to hold the parameters.
        if ( pc+len > buffer.length) {
            throw new IllegalArgumentException(
                "Buffer too small for instruction at pc "+pc);
        }

        String text=Opcodes.getText( opcode );

        if ( len == 1 ) {
            return text;
        }

        int val = len == 2 ?
            (buffer[pc+1]) & 0xff :
            ((buffer[pc+2]&0xff) << 8) | (buffer[pc+1]&0xff);

        return String.format(text, val);
    }

    public static String decode( int pc, Memory memory )
    {
        int opcode =
            memory.read(pc) & 0xff;

        byte [] buffer = new byte[Opcodes.getEncodingLength( opcode )];

        for ( int i = 0; i < buffer.length; i++ )
            buffer[i] = memory.read( (pc+i) & 0xffff );

        return decode( 0, buffer );
    }

  /**
   * Returns the textual representation for the passed opcode.  This may
   * contain argument slots in a <code>java.text.MessageFormat</code>
   * compatible way.  Note that only the lower eight bits of the passed
   * opcode are used for lookup.
   *
   * @param opcode The opcode to decode.
   * @return The textual representation for the passed opcode.
   */
  public static String getText( int opcode )
  {
    return OPCODES[ 0xff & opcode ]._name;
  }



  /**
   * Get the length that an instruction based on the passed opcode uses in
   * memory.
   *
   * @param opcode The opcode to decode.
   * @return The length of an instruction based on the passed opcode.
   */
  public static int getEncodingLength( int opcode )
  {
    return OPCODES[ 0xff & opcode ]._length;
  }



  /**
   * Get the number of cycles required to execute the passed opcode.
   *
   * @param opcode The opcode.
   * @return The number of cycles required to execute the passed instruction.
   */
  public static int getTime( int opcode )
  {
    return OPCODES[ 0xff & opcode ]._time;
  }



  /**
   * Checks whether the passed opcode is valid.  <i>Valid</i> means that this
   * class knows a string representation for this opcode, it does <i>not</i>
   * mean that it is a <i>documented</i> opcode.
   *
   * @param opcode The opcode to check.
   * @return <code>true</code> in case the opcode can be translated into a
   *         mnemonic.
   */
  public static boolean isValidOpcode( int opcode )
  {
    return opcode >= 0 && opcode < OPCODES.length &&
           !OPCODES[ opcode ]._name.equals( UNKNOWN_OPCODE );
  }

    /*
    * This list has to be kept alphabetically sorted!  Since the mnemonics for
    * UNDOCUMENTED opcodes are not really well defined, put these *not* in this
    * list, instead put these in the smaller list after the official opcodes.
    * Search for UNDOCUMENTED.
    */
    public static final int ADC_IMM  = 0x69;
    public static final int ADC_ZP   = 0x65;
    public static final int ADC_ZPX  = 0x75;
    public static final int ADC_ABS  = 0x6d;
    public static final int ADC_ABSX = 0x7d;
    public static final int ADC_ABSY = 0x79;
    public static final int ADC_IZX  = 0x61;
    public static final int ADC_IZY  = 0x71;
    public static final int AND_IMM  = 0x29;
    public static final int AND_ZP   = 0x25;
    public static final int AND_ZPX  = 0x35;
    public static final int AND_IZX  = 0x21;
    public static final int AND_IZY  = 0x31;
    public static final int AND_ABS  = 0x2d;
    public static final int AND_ABSX = 0x3d;
    public static final int AND_ABSY = 0x39;
    public static final int ASL_IMP  = 0x0a;
    public static final int ASL_ZP   = 0x06;
    public static final int ASL_ZPX  = 0x16;
    public static final int ASL_ABS  = 0x0e;
    public static final int ASL_ABSX = 0x1e;
    public static final int  BCC_REL = 0x90;
    public static final int BCS_REL  = 0xb0;
    public static final int BEQ_REL  = 0xf0;
    public static final int BIT_ZP   = 0x24;
    public static final int BIT_ABS  = 0x2c;
    public static final int BMI_REL  = 0x30;
    public static final int BNE_REL  = 0xD0;
    public static final int BPL_REL  = 0x10;
    public static final int BRK_IMP  = 0x00;
    public static final int BVC_REL  = 0x50;
    public static final int BVS_REL  = 0x70;
    public static final int CLC_IMP  = 0x18;
    public static final int CLD_IMP  = 0xD8;
    public static final int CLI_IMP  = 0x58;
    public static final int CLV_IMP  = 0xB8;
    public static final int CMP_IMM  = 0xC9;
    public static final int CMP_ZP   = 0xC5;
    public static final int CMP_ZPX  = 0xD5;
    public static final int CMP_IZX  = 0xc1;
    public static final int CMP_IZY  = 0xd1;
    public static final int CMP_ABS  = 0xCD;
    public static final int CMP_ABSX = 0xDD;
    public static final int CMP_ABSY = 0xD9;
    public static final int CPX_IMM  = 0xE0;
    public static final int CPX_ZP   = 0xE4;
    public static final int CPX_ABS  = 0xEC;
    public static final int CPY_IMM  = 0xC0;
    public static final int CPY_ZP   = 0xC4;
    public static final int CPY_ABS  = 0xCC;
    public static final int DEC_ZP   = 0xC6;
    public static final int DEC_ZPX  = 0xD6;
    public static final int DEC_ABS  = 0xCE;
    public static final int DEC_ABSX = 0xDE;
    public static final int DEX_IMP  = 0xCA;
    public static final int DEY_IMP  = 0x88;
    public static final int EOR_IZX  = 0x41;
    public static final int EOR_ZP   = 0x45;
    public static final int EOR_IMM  = 0x49;
    public static final int EOR_ABS  = 0x4D;
    public static final int EOR_ZPX  = 0x55;
    public static final int EOR_ABSX = 0x5D;
    public static final int EOR_ABSY = 0x59;
    public static final int EOR_IZY  = 0x51;
    public static final int INC_ZP   = 0xe6;
    public static final int INC_ZPX  = 0xf6;
    public static final int INC_ABS  = 0xee;
    public static final int INC_ABSX = 0xfe;
    public static final int INX_IMP  = 0xe8;
    public static final int INY_IMP  = 0xc8;
    public static final int JMP_ABS  = 0x4c;
    public static final int JMP_IND  = 0x6c;
    public static final int JSR_ABS  = 0x20;
    public static final int LDA_IMM  = 0xa9;
    public static final int LDA_ZP   = 0xa5;
    public static final int LDA_ZPX  = 0xb5;
    public static final int LDA_ABS  = 0xad;
    public static final int LDA_ABSX = 0xbd;
    public static final int LDA_ABSY = 0xb9;
    public static final int LDA_IZX  = 0xa1;
    public static final int LDA_IZY  = 0xb1;
    public static final int LDX_IMM  = 0xa2;
    public static final int LDX_ZP   = 0xa6;
    public static final int LDX_ZPY  = 0xb6;
    public static final int LDX_ABS  = 0xae;
    public static final int LDX_ABSY = 0xbe;
    public static final int LDY_IMM  = 0xa0;
    public static final int LDY_ZP   = 0xa4;
    public static final int LDY_ZPX  = 0xb4;
    public static final int LDY_ABS  = 0xac;
    public static final int LDY_ABSX = 0xbc;
    public static final int LSR_IMP  = 0x4a;
    public static final int LSR_ZP   = 0x46;
    public static final int LSR_ZPX  = 0x56;
    public static final int LSR_ABS  = 0x4e;
    public static final int LSR_ABSX = 0x5e;
    public static final int NOP_IMP  = 0xea;
    public static final int ORA_IMM  = 0x09;
    public static final int ORA_ZP   = 0x05;
    public static final int ORA_ZPX  = 0x15;
    public static final int ORA_IZX  = 0x01;
    public static final int ORA_IZY  = 0x11;
    public static final int ORA_ABS  = 0x0d;
    public static final int ORA_ABSX = 0x1d;
    public static final int ORA_ABSY = 0x19;
    public static final int PHA_IMP  = 0x48;
    public static final int PHP_IMP  = 0x08;
    public static final int PLA_IMP  = 0x68;
    public static final int PLP_IMP  = 0x28;
    public static final int ROL_IMP  = 0x2a;
    public static final int ROL_ZP   = 0x26;
    public static final int ROL_ZPX  = 0x36;
    public static final int ROL_ABS  = 0x2e;
    public static final int ROL_ABSX = 0x3e;
    public static final int ROR_IMP  = 0x6a;
    public static final int ROR_ZP   = 0x66;
    public static final int ROR_ZPX  = 0x76;
    public static final int ROR_ABS  = 0x6e;
    public static final int ROR_ABSX = 0x7e;
    public static final int RTI_IMP  = 0x40;
    public static final int RTS_IMP  = 0x60;
    public static final int SBC_IMM  = 0xe9;
    public static final int SBC_ZP   = 0xe5;
    public static final int SBC_ZPX  = 0xf5;
    public static final int SBC_ABS  = 0xed;
    public static final int SBC_ABSX = 0xfd;
    public static final int SBC_ABSY = 0xf9;
    public static final int SBC_IZX  = 0xe1;
    public static final int SBC_IZY  = 0xf1;
    public static final int SEC_IMP  = 0x38;
    public static final int SED_IMP  = 0xf8;
    public static final int SEI_IMP  = 0x78;
    public static final int STA_ZP   = 0x85;
    public static final int STA_ZPX  = 0x95;
    public static final int STA_ABS  = 0x8d;
    public static final int STA_ABSX = 0x9d;
    public static final int STA_ABSY = 0x99;
    public static final int STA_IZX  = 0x81;
    public static final int STA_IZY  = 0x91;
    public static final int STX_ZP   = 0x86;
    public static final int STX_ZPY  = 0x96;
    public static final int STX_ABS  = 0x8e;
    public static final int STY_ZP   = 0x84;
    public static final int STY_ZPX  = 0x94;
    public static final int STY_ABS  = 0x8c;
    public static final int TAX_IMP  = 0xaa;
    public static final int TAY_IMP  = 0xa8;
    public static final int TSX_IMP  = 0xba;
    public static final int TXA_IMP  = 0x8a;
    public static final int TXS_IMP  = 0x9a;
    public static final int TYA_IMP  = 0x98;

    /*
     * Undocumented opcodes.
     */
    public static final int JAM_02 = 0x02; // JAM / KIL / HLT
    public static final int SLO_03 = 0x03;
    public static final int NOP_04 = 0x04; // NOP / SKB
    public static final int SLO_07 = 0x07; // SLO / ASO
    public static final int ANC_0b = 0x0b; // ANC / ANC2
    public static final int NOP_0c = 0x0c; // NOP / SKW
    public static final int SLO_0f = 0x0f; // SLO / ASO
    public static final int JAM_12 = 0x12; // JAM / KIL / HLT
    public static final int SLO_13 = 0x13; // SLO / ASO
    public static final int NOP_14 = 0x14; // NOP / SKB
    public static final int SLO_17 = 0x17; // SLO / ASO
    public static final int NOP_1a = 0x1a; // NOP / IMP
    public static final int SLO_1b = 0x1b; // SLO / ASO
    public static final int NOP_1c = 0x1c; // NOP / SKW
    public static final int SLO_1f = 0x1f; // SLO / ASO
    public static final int ANC_2b = 0x2b; // ANC / ANC2
    public static final int JAM_22 = 0x22; // JAM / KIL / HLT
    public static final int RLA_23 = 0x23; // RLA
    public static final int RLA_2f = 0x2f; // RLA
    public static final int RLA_27 = 0x27; // RLA
    public static final int JAM_32 = 0x32; // JAM / KIL / HLT
    public static final int RLA_33 = 0x33; // RLA
    public static final int NOP_34 = 0x34;
    public static final int RLA_37 = 0x37; // RLA
    public static final int NOP_3a = 0x3a; // NOP / IMP
    public static final int RLA_3b = 0x3b; // RLA
    public static final int NOP_3c = 0x3c; // NOP / SKW / IGN
    public static final int RLA_3f = 0x3f; // RLA
    public static final int JAM_42 = 0x42; // JAM / KIL / HLT
    public static final int LSE_43 = 0x43; // LSE
    public static final int NOP_44 = 0x44; // NOP / SKB
    public static final int LSE_47 = 0x47; // LSR / LSE
    public static final int ALR_4b = 0x4b; // ALR / ASR
    public static final int LSE_4f = 0x4f; // LSE
    public static final int JAM_52 = 0x52; // JAM / KIL / HLT
    public static final int LSE_53 = 0x53; // LSE
    public static final int NOP_54 = 0x54; // NOP / SKB
    public static final int LSE_57 = 0x57; // LSE
    public static final int NOP_5a = 0x5a; // NOP / IMP
    public static final int LSE_5b = 0x5b; // LSE
    public static final int NOP_5c = 0x5c; // NOP / SKW
    public static final int LSE_5f = 0x5f; // LSE
    public static final int JAM_62 = 0x62; // JAM / KIL / HLT
    public static final int RRA_63 = 0x63; // RRA
    public static final int NOP_64 = 0x64; // NOP / SKB
    public static final int RRA_67 = 0x67; // RRA
    public static final int ARR_6b = 0x6b; // ARR
    public static final int RRA_6f = 0x6f; // RRA
    public static final int JAM_72 = 0x72; // JAM / KIL / HLT
    public static final int RRA_73 = 0x73; // RRA
    public static final int NOP_74 = 0x74; // NOP / SKB
    public static final int RRA_77 = 0x77; // RRA
    public static final int NOP_7a = 0x7a; // NOP / IMP
    public static final int RRA_7b = 0x7b; // RRA
    public static final int NOP_7c = 0x7c; // NOP / SKW
    public static final int RRA_7f = 0x7f; // RRA
    public static final int NOP_80 = 0x80; // NOP / SKB
    public static final int NOP_82 = 0x82; // NOP / SKB
    public static final int SAX_83 = 0x83; // SAX
    public static final int SAX_87 = 0x87; // SAX
    public static final int NOP_89 = 0x89; // NOP / SKB
    public static final int XXA_8b = 0x8b; // XXA / ANE
    public static final int SAX_8f = 0x8f; // SAX
    public static final int JAM_92 = 0x92; // JAM / KIL / HLT
    public static final int AHX_93 = 0x93; // AHX / SHA / AXA
    public static final int SAX_97 = 0x97; // SAX
    public static final int TAS_9b = 0x9b; // TAS / SHS / XAS
    public static final int SHY_9c = 0x9c; // SHY / SAY
    public static final int SHX_9e = 0x9e; // SHX / SXA
    public static final int AHX_9f = 0x9f; // AHX / SHA / AXA
    public static final int LAX_a3 = 0xa3; // LAX
    public static final int LAX_a7 = 0xa7; // LAX
    public static final int LXA_ab = 0xab; // LXA / ATX / LAX / OAL
    public static final int LAX_af = 0xaf; // LAX
    public static final int JAM_b2 = 0xb2; // JAM / KIL / HLT
    public static final int LAX_b3 = 0xb3; // LAX
    public static final int LAX_b7 = 0xb7; // LAX
    public static final int LAS_bb = 0xbb; // LAS / LAR
    public static final int LAX_bf = 0xbf; // LAX
    public static final int NOP_c2 = 0xc2; // NOP / SKB
    public static final int DCM_c3 = 0xc3; // DCM
    public static final int DCM_c7 = 0xc7; // DCM
    public static final int AXS_cb = 0xcb; // AXS / SBX
    public static final int DCM_cf = 0xcf; // DCM
    public static final int JAM_d2 = 0xd2; // JAM / KIL / HLT
    public static final int DCM_d3 = 0xd3; // DCM
    public static final int NOP_d4 = 0xd4; // NOP / SKB
    public static final int DCM_d7 = 0xd7; // DCM
    public static final int NOP_da = 0xda; // NOP / IMP
    public static final int DCM_db = 0xdb; // DCM
    public static final int NOP_dc = 0xdc; // NOP / SKW
    public static final int DCM_df = 0xdf; // DCM
    public static final int NOP_e2 = 0xe2; // NOP / SKB
    public static final int ISC_e3 = 0xe3; // ISC /ISB
    public static final int ISC_e7 = 0xe7; // ISC
    public static final int SBC_eb = 0xeb; // SBC
    public static final int ISC_ef = 0xef; // ISC
    public static final int JAM_f2 = 0xf2; // JAM / KIL / HLT
    public static final int ISC_f3 = 0xf3; // ISC
    public static final int NOP_f4 = 0xf4; // NOP / SKB
    public static final int ISC_f7 = 0xf7; // ISC
    public static final int NOP_fa = 0xfa; // NOP / IMP
    public static final int ISC_fb = 0xfb; // ISC
    public static final int NOP_fc = 0xfc; // NOP / SKW
    public static final int ISC_ff = 0xff; // ISC

    /**
     * Text used for unknown opcodes.
     */
    private static final String UNKNOWN_OPCODE = "???";

    /**
     * Carries the addressing mode specific information.
     */
    public enum AddressingMode {
        /**
         * Implicit.
         */
        IMP(1, ""),
        /**
         * Relative.
         */
        REL(2, "$%x"),
        /**
         * Immediate.
         */
        IMM(2, "#$%x"),
        /**
         * Zero Page.
         */
        ZP(2, "$%x"),
        /**
         * Zero Page,X.
         */
        ZPX(2, "$%x,X"),
        /**
         * Zero Page,Y.
         */
        ZPY(2, "$%x,Y"),
        /**
         * Indexed Indirect (X).
         */
        IZX(2, "($%x,X)"),
        /**
         * Indirect Indexed (Y).
         */
        IZY(2, "($%x),Y"),
        /**
         * Absolute.
         */
        ABS(3, "$%x"),
        /**
         * Absolute,X.
         */
        ABSX(3, "$%x,X"),
        /**
         * Absolute,Y.
         */
        ABSY(3, "$%x,Y"),
        /**
         * Indirect.
         */
        IND(3, "($%x)");

        private final int encodingLength;
        private final String format;

        AddressingMode(int encodingLength, String format) {
            this.encodingLength = encodingLength;
            this.format = format;
        }

        public int getEncodingLength() {
            return encodingLength;
        }

        public String getFormat() {
            return format;
        }
    }

    static class Opcode
    {
      Opcode(
          String name,
          int baseTime,
          AddressingMode addressingMode )
      {
        var opcodeSuffix = addressingMode.format;

        _name = opcodeSuffix.length() == 0 ?
          name :
          name + " " + opcodeSuffix;

        _length = addressingMode.encodingLength;
        _time = baseTime;
        _addressingMode = addressingMode;
      }

      Opcode(
          String name,
          int baseTime )
      {
        this( name, baseTime, AddressingMode.IMP );
      }

      final String _name;
      final int _length;
      final int _time;
      final AddressingMode _addressingMode;
    }

    final static Opcode[] OPCODES =
      new Opcode[ 256 ];

    static
    {
        Arrays.fill( OPCODES, new Opcode( UNKNOWN_OPCODE, 88 ) );

        OPCODES[ ADC_IMM ] =
          new Opcode( "ADC", 2, AddressingMode.IMM );
        OPCODES[ ADC_ZP ] =
          new Opcode( "ADC", 3, AddressingMode.ZP );
        OPCODES[ ADC_ZPX ] =
          new Opcode( "ADC", 4, AddressingMode.ZPX );
        OPCODES[ ADC_ABS ] =
          new Opcode( "ADC", 4, AddressingMode.ABS );
        OPCODES[ ADC_ABSX ] =
          new Opcode( "ADC", +4, AddressingMode.ABSX );
        OPCODES[ ADC_ABSY ] =
          new Opcode( "ADC", +4, AddressingMode.ABSY );
        OPCODES[ ADC_IZX ] =
          new Opcode( "ADC", 6, AddressingMode.IZX );
        OPCODES[ ADC_IZY ] =
          new Opcode( "ADC", +5, AddressingMode.IZY );
        OPCODES[ AND_IMM ] =
          new Opcode( "AND", 2, AddressingMode.IMM );
        OPCODES[ AND_ZP  ] =
          new Opcode( "AND", 2, AddressingMode.ZP );
        OPCODES[ AND_ZPX ] =
          new Opcode( "AND", 3, AddressingMode.ZPX );
        OPCODES[ AND_ABS ] =
          new Opcode( "AND", 4, AddressingMode.ABS );
        OPCODES[ AND_ABSX ] =
          new Opcode( "AND", +4, AddressingMode.ABSX );
        OPCODES[ AND_ABSY ] =
          new Opcode( "AND", +4, AddressingMode.ABSY );
        OPCODES[ AND_IZX ] =
          new Opcode( "AND", 6, AddressingMode.IZX );
        OPCODES[ AND_IZY] =
          new Opcode( "AND", +5, AddressingMode.IZY );
        OPCODES[ ASL_IMP  ] =
          new Opcode( "ASL", 2 );
        OPCODES[ ASL_ZP ] =
          new Opcode( "ASL", 5, AddressingMode.ZP );
        OPCODES[ ASL_ZPX ] =
          new Opcode( "ASL", 6, AddressingMode.ZPX );
        OPCODES[ ASL_ABS ] =
          new Opcode( "ASL", 6, AddressingMode.ABS );
        OPCODES[ ASL_ABSX ] =
          new Opcode( "ASL", 7, AddressingMode.ABSX );
        OPCODES[ BCC_REL ] =
          new Opcode( "BCC", 2, AddressingMode.REL );
        OPCODES[ BCS_REL  ] =
          new Opcode( "BCS", 2, AddressingMode.REL );
        OPCODES[ BEQ_REL] =
          new Opcode( "BEQ", 2, AddressingMode.REL );
        OPCODES[ BIT_ZP ] =
          new Opcode( "BIT", 3, AddressingMode.ZP );
        OPCODES[ NOP_34 ]  =
          new Opcode( "BIT", 4, AddressingMode.ZPX );
        OPCODES[ BIT_ABS  ] =
          new Opcode( "BIT", 4, AddressingMode.ABS );
        OPCODES[ BMI_REL] =
          new Opcode( "BMI", 2, AddressingMode.REL );
        OPCODES[ BNE_REL ] =
          new Opcode( "BNE", 2, AddressingMode.REL );
        OPCODES[ BPL_REL ] =
          new Opcode( "BPL", 2, AddressingMode.REL );
        OPCODES[ BRK_IMP ] =
          new Opcode( "BRK", 7 );
        OPCODES[ BVC_REL ] =
          new Opcode( "BVC", 2, AddressingMode.REL );
        OPCODES[ BVS_REL ] =
          new Opcode( "BVS", 2, AddressingMode.REL );
        OPCODES[ CLC_IMP ] =
          new Opcode( "CLC", 2 );
        OPCODES[ CLD_IMP ] =
          new Opcode( "CLD", 2 );
        OPCODES[ CLI_IMP ] =
          new Opcode( "CLI", 2 );
        OPCODES[ CLV_IMP ] =
          new Opcode( "CLV", 2 );
        OPCODES[ CMP_IMM ] =
          new Opcode( "CMP", 2, AddressingMode.IMM );
        OPCODES[ CMP_ZP ] =
          new Opcode( "CMP", 3, AddressingMode.ZP );
        OPCODES[ CMP_ZPX ] =
          new Opcode( "CMP", 4, AddressingMode.ZPX );
        OPCODES[ CMP_IZX ] =
          new Opcode( "CMP", 6, AddressingMode.IZX );
        OPCODES[ CMP_IZY ] =
          new Opcode( "CMP", 5, AddressingMode.IZY );
        OPCODES[ CMP_ABS ] =
          new Opcode( "CMP", 4, AddressingMode.ABS );
        OPCODES[ CMP_ABSX ] =
          new Opcode( "CMP", 4, AddressingMode.ABSX );
        OPCODES[ CMP_ABSY ] =
          new Opcode( "CMP", 4, AddressingMode.ABSY );
        OPCODES[ CPX_IMM ] =
          new Opcode( "CPX", 2, AddressingMode.IMM );
        OPCODES[ CPX_ZP ] =
          new Opcode( "CPX", 3, AddressingMode.ZP );
        OPCODES[ CPX_ABS ] =
          new Opcode( "CPX", 4, AddressingMode.ABS );
        OPCODES[ CPY_IMM ] =
          new Opcode( "CPY", 2, AddressingMode.IMM );
        OPCODES[ CPY_ZP ]  =
          new Opcode( "CPY", 3, AddressingMode.ZP );
        OPCODES[ CPY_ABS ]  =
          new Opcode( "CPY", 4, AddressingMode.ABS );
        OPCODES[ DEC_ZP  ]  =
          new Opcode( "DEC", 5, AddressingMode.ZP );
        OPCODES[ DEC_ZPX ]  =
          new Opcode( "DEC", 6, AddressingMode.ZPX );
        OPCODES[ DEC_ABS ]  =
          new Opcode( "DEC", 6, AddressingMode.ABS );
        OPCODES[ DEC_ABSX ] =
          new Opcode( "DEC", 7, AddressingMode.ABSX );
        OPCODES[ DEX_IMP] =
          new Opcode( "DEX", 2, AddressingMode.IMP );
        OPCODES[ DEY_IMP ] =
          new Opcode( "DEY", 2, AddressingMode.IMP );
        OPCODES[ EOR_IMM ] =
          new Opcode( "EOR", 2, AddressingMode.IMM );
        OPCODES[ EOR_ZP ] =
          new Opcode( "EOR", 3, AddressingMode.ZP );
        OPCODES[ EOR_ZPX ] =
          new Opcode( "EOR", 4, AddressingMode.ZPX );
        OPCODES[ EOR_ABS ] =
          new Opcode( "EOR", 4, AddressingMode.ABS );
        OPCODES[ EOR_ABSX ] =
          new Opcode( "EOR", +4, AddressingMode.ABSX );
        OPCODES[ EOR_ABSY ] =
          new Opcode( "EOR", +4, AddressingMode.ABSY );
        OPCODES[ EOR_IZX ] =
          new Opcode( "EOR", 6, AddressingMode.IZX );
        OPCODES[ EOR_IZY ] =
          new Opcode( "EOR", +5, AddressingMode.IZY );
        OPCODES[ INC_ZP ]  =
          new Opcode( "INC", 5, AddressingMode.ZP );
        OPCODES[ INC_ZPX ] =
          new Opcode( "INC", 6, AddressingMode.ZPX );
        OPCODES[ INC_ABS ] =
          new Opcode( "INC", 6, AddressingMode.ABS );
        OPCODES[ INC_ABSX ] =
          new Opcode( "INC", 7, AddressingMode.ABSX );
        OPCODES[ INX_IMP ] =
          new Opcode( "INX", 2 );
        OPCODES[ INY_IMP ] =
          new Opcode( "INY", 2 );
        OPCODES[ JMP_ABS  ] =
          new Opcode( "JMP", 3, AddressingMode.ABS );
        OPCODES[ JMP_IND  ] =
          new Opcode( "JMP", 5, AddressingMode.IND );
        OPCODES[ JSR_ABS  ] =
          new Opcode( "JSR", 6, AddressingMode.ABS );
        OPCODES[ LDA_IMM  ] =
          new Opcode( "LDA", 2, AddressingMode.IMM );
        OPCODES[ LDA_ZP  ] =
          new Opcode( "LDA", 3, AddressingMode.ZP );
        OPCODES[ LDA_ZPX  ] =
          new Opcode( "LDA", 4, AddressingMode.ZPX );
        OPCODES[ LDA_ABS ] =
          new Opcode( "LDA", 4, AddressingMode.ABS );
        OPCODES[ LDA_ABSX ] =
          new Opcode( "LDA", +4, AddressingMode.ABSX );
        OPCODES[ LDA_ABSY ] =
          new Opcode( "LDA", +4, AddressingMode.ABSY );
        OPCODES[ LDA_IZX ] =
          new Opcode( "LDA", 6, AddressingMode.IZX );
        OPCODES[ LDA_IZY ] =
          new Opcode( "LDA", +5, AddressingMode.IZY );
        OPCODES[ LDX_IMM  ]  =
          new Opcode( "LDX", 2, AddressingMode.IMM );
        OPCODES[ LDX_ZP ]  =
          new Opcode( "LDX", 3, AddressingMode.ZP );
        OPCODES[ LDX_ZPY ]  =
          new Opcode( "LDX", 4, AddressingMode.ZPY );
        OPCODES[ LDX_ABS ]  =
          new Opcode( "LDX", 4, AddressingMode.ABS );
        OPCODES[ LDX_ABSY ] =
          new Opcode( "LDX", +4, AddressingMode.ABSY );
        OPCODES[ LDY_IMM ] =
          new Opcode( "LDY", 2, AddressingMode.IMM );
        OPCODES[ LDY_ZP ] =
          new Opcode( "LDY", 3, AddressingMode.ZP );
        OPCODES[ LDY_ZPX ]  =
          new Opcode( "LDY", 4, AddressingMode.ZPX );
        OPCODES[ LDY_ABS ]  =
          new Opcode( "LDY", 4, AddressingMode.ABS );
        OPCODES[ LDY_ABSX ]  =
          new Opcode( "LDY", +4, AddressingMode.ABSX );
        OPCODES[ LSR_IMP ]  =
          new Opcode( "LSR", 2 );
        OPCODES[ LSR_ZP ]  =
          new Opcode( "LSR", 5, AddressingMode.ZP );
        OPCODES[ LSR_ZPX  ]  =
          new Opcode( "LSR", 6, AddressingMode.ZPX );
        OPCODES[ LSR_ABS   ]  =
          new Opcode( "LSR", 6, AddressingMode.ABS );
        OPCODES[ LSR_ABSX ] =
          new Opcode( "LSR", 7, AddressingMode.ABSX );
        OPCODES[ NOP_IMP ] =
          new Opcode( "NOP", 2 );
        OPCODES[ ORA_IMM ] =
          new Opcode( "ORA", 2, AddressingMode.IMM );
        OPCODES[ ORA_ZP ] =
          new Opcode( "ORA", 2, AddressingMode.ZP );
        OPCODES[ ORA_ZPX  ]  =
          new Opcode( "ORA", 3, AddressingMode.ZPX );
        OPCODES[ ORA_IZX ] =
          new Opcode( "ORA", 6, AddressingMode.IZX );
        OPCODES[ ORA_IZY ] =
          new Opcode( "ORA", +5, AddressingMode.IZY );
        OPCODES[ ORA_ABS ] =
          new Opcode( "ORA", 4, AddressingMode.ABS );
        OPCODES[ ORA_ABSX ] =
          new Opcode( "ORA", +4, AddressingMode.ABSX );
        OPCODES[ ORA_ABSY ] =
          new Opcode( "ORA", +4, AddressingMode.ABSY );
        OPCODES[ PHA_IMP ] =
          new Opcode( "PHA", 3 );
        OPCODES[ PHP_IMP ] =
          new Opcode( "PHP", 3 );
        OPCODES[ PLA_IMP ] =
          new Opcode( "PLA", 4 );
        OPCODES[ PLP_IMP ] =
          new Opcode( "PLP", 4 );
        OPCODES[ ROL_IMP ] =
          new Opcode( "ROL", 2 );
        OPCODES[ ROL_ZP ] =
          new Opcode( "ROL", 5, AddressingMode.ZP );
        OPCODES[ ROL_ZPX ] =
          new Opcode( "ROL", 6, AddressingMode.ZPX );
        OPCODES[ ROL_ABS ] =
          new Opcode( "ROL", 6, AddressingMode.ABS );
        OPCODES[ ROL_ABSX ] =
          new Opcode( "ROL", 7, AddressingMode.ABSX );
        OPCODES[ ROR_IMP ] =
          new Opcode( "ROR", 2 );
        OPCODES[ ROR_ZP ] =
          new Opcode( "ROR", 5, AddressingMode.ZP );
        OPCODES[ ROR_ZPX ]  =
          new Opcode( "ROR", 6, AddressingMode.ZPX );
        OPCODES[ ROR_ABS   ]  =
          new Opcode( "ROR", 6, AddressingMode.ABS );
        OPCODES[ ROR_ABSX ]  =
          new Opcode( "ROR", 7, AddressingMode.ABSX );
        OPCODES[ RTI_IMP  ]  =
          new Opcode( "RTI", 6 );
        OPCODES[ RTS_IMP  ]  =
          new Opcode( "RTS", 6 );
        OPCODES[ SBC_IMM ]  =
          new Opcode( "SBC", 2, AddressingMode.IMM );
        OPCODES[ SBC_ZP ]  =
          new Opcode( "SBC", 3, AddressingMode.ZP );
        OPCODES[ SBC_ZPX ]  =
          new Opcode( "SBC", 4, AddressingMode.ZPX );
        OPCODES[ SBC_ABS  ]  =
          new Opcode( "SBC", 4, AddressingMode.ABS );
        OPCODES[ SBC_ABSX  ]  =
          new Opcode( "SBC", 4, AddressingMode.ABSX );
        OPCODES[ SBC_ABSY ]  =
          new Opcode( "SBC", 4, AddressingMode.ABSY );
        OPCODES[ SBC_IZX  ]  =
          new Opcode( "SBC", 6, AddressingMode.IZX );
        OPCODES[ SBC_IZY ]  =
          new Opcode( "SBC", 5, AddressingMode.IZY );
        OPCODES[ SEC_IMP ]  =
          new Opcode( "SEC", 2 );
        OPCODES[ SED_IMP ]  =
          new Opcode( "SED", 2 );
        OPCODES[ SEI_IMP ]  =
          new Opcode( "SEI", 2 );
        OPCODES[ STA_ZP ] =
          new Opcode( "STA", 3, AddressingMode.ZP );
        OPCODES[ STA_ZPX ] =
          new Opcode( "STA", 4, AddressingMode.ZPX );
        OPCODES[ STA_ABS ] =
          new Opcode( "STA", 4, AddressingMode.ABS );
        OPCODES[ STA_ABSX ] =
          new Opcode( "STA", 5, AddressingMode.ABSX );
        OPCODES[ STA_ABSY ] =
          new Opcode( "STA", 5, AddressingMode.ABSY );
        OPCODES[ STA_IZX ] =
          new Opcode( "STA", 6, AddressingMode.IZX );
        OPCODES[ STA_IZY ] =
          new Opcode( "STA", 6, AddressingMode.IZY );
        OPCODES[ STX_ZP ] =
          new Opcode( "STX", 3, AddressingMode.ZP );
        OPCODES[ STX_ZPY ] =
          new Opcode( "STX", 4, AddressingMode.ZPY );
        OPCODES[ STX_ABS ] =
          new Opcode( "STX", 4, AddressingMode.ABS );
        OPCODES[ STY_ZP ]  =
          new Opcode( "STY", 3, AddressingMode.ZP );
        OPCODES[ STY_ZPX ] =
          new Opcode( "STY", 4, AddressingMode.ZPX );
        OPCODES[ STY_ABS ] =
          new Opcode( "STY", 4, AddressingMode.ABS );
        OPCODES[ TAX_IMP ] =
          new Opcode( "TAX", 2 );
        OPCODES[ TAY_IMP ] =
          new Opcode( "TAY", 2 );
        OPCODES[ TSX_IMP ] =
          new Opcode( "TSX", 2 );
        OPCODES[ TXA_IMP ] =
          new Opcode( "TXA", 2 );
        OPCODES[ TXS_IMP ] =
          new Opcode( "TXS", 2 );
        OPCODES[ TYA_IMP ] =
          new Opcode( "TYA", 2 );

        // Undocumented opcodes.
        OPCODES[ JAM_02 ] =
          new Opcode( "JAM_02", 2, AddressingMode.ZP );
        OPCODES[ SLO_03 ] =
          new Opcode( "SLO_03", 8, AddressingMode.IZX );
        OPCODES[ NOP_04 ] =
          new Opcode( "NOP_04", 3, AddressingMode.ZP );
        OPCODES[ SLO_07 ] =
          new Opcode( "SLO_07", 5, AddressingMode.ZP );
        OPCODES[ SLO_0f ] =
          new Opcode( "SLO_0f", 6, AddressingMode.ABS );
        OPCODES[ ANC_0b ] =
          new Opcode( "ANC_0b", 3, AddressingMode.ZP );
        OPCODES[ NOP_0c ] =
          new Opcode( "NOP_0c", 4, AddressingMode.ABS );
        OPCODES[ JAM_12 ] =
          new Opcode( "JAM_12", 2, AddressingMode.ZP );
        OPCODES[ SLO_13 ] =
          new Opcode( "SLO_13", 8, AddressingMode.IZY );
        OPCODES[ NOP_14 ] =
          new Opcode( "NOP_14", 3, AddressingMode.ZPX );
        OPCODES[ SLO_17 ] =
          new Opcode( "SLO_17", 5, AddressingMode.ZP );
        OPCODES[ NOP_1a ] =
          new Opcode( "NOP_1a", 2, AddressingMode.IMP );
        OPCODES[ SLO_1b ] =
          new Opcode( "SLO_1b", 7, AddressingMode.ABSY );
        OPCODES[ NOP_1c ] =
          new Opcode( "NOP_1c", 4, AddressingMode.ABSX );
        OPCODES[ SLO_1f ] =
          new Opcode( "SLO_1f", 7, AddressingMode.ABSX );
        OPCODES[ JAM_22 ] =
          new Opcode( "JAM_22", 2, AddressingMode.ZP );
        OPCODES[ RLA_23 ] =
          new Opcode( "RLA_23", 8, AddressingMode.IZX );
        OPCODES[ RLA_27 ] =
          new Opcode( "RLA_27", 6, AddressingMode.ABS );
        OPCODES[ ANC_2b ] =
          new Opcode( "ANC_2b", 2, AddressingMode.IMM );
        OPCODES[ RLA_2f ] =
          new Opcode( "RLA_2f", 6, AddressingMode.ABS );
        OPCODES[ JAM_32 ] =
          new Opcode( "JAM_32", 2, AddressingMode.ZP );
        OPCODES[ RLA_33 ] =
          new Opcode( "RLA_33", 8, AddressingMode.IZY );
        OPCODES[ RLA_37 ] =
          new Opcode( "RLA_37", 6, AddressingMode.ZPX );
        OPCODES[ NOP_3a ] =
          new Opcode( "NOP_3a", 2, AddressingMode.IMP );
        OPCODES[ RLA_3b ] =
          new Opcode( "RLA_3b", 7, AddressingMode.ABSY );
        OPCODES[ NOP_3c ] =
          new Opcode( "NOP_3c", 4, AddressingMode.ABSX );
        OPCODES[ RLA_3f ] =
          new Opcode( "RLA_3f", 7, AddressingMode.ABSX );
        OPCODES[ JAM_42 ] =
          new Opcode( "JAM_42", 2, AddressingMode.ZP );
        OPCODES[ LSE_43 ] =
          new Opcode( "LSE_43", 8, AddressingMode.IZX );
        OPCODES[ NOP_44 ] =
          new Opcode( "NOP_44", 3, AddressingMode.ZP );
        OPCODES[ LSE_47 ] =
          new Opcode( "LSE_47", 5, AddressingMode.ZP );
        OPCODES[ LSE_4f ] =
          new Opcode( "LSE_4f", 6, AddressingMode.ABS );
        OPCODES[ ALR_4b ] =
          new Opcode( "ALR_4b", 2, AddressingMode.IMM );
        OPCODES[ JAM_52 ] =
          new Opcode( "JAM_52", 2, AddressingMode.ZP );
        OPCODES[ LSE_53 ] =
          new Opcode( "LSE_53", 8, AddressingMode.IZY );
        OPCODES[ NOP_54 ] =
          new Opcode( "NOP_54", 4, AddressingMode.IMP );
        OPCODES[ LSE_57 ] =
          new Opcode( "LSE_57", 6, AddressingMode.ZPX );
        OPCODES[ NOP_5a ] =
          new Opcode( "NOP_5a", 2, AddressingMode.IMP );
        OPCODES[ LSE_5b ] =
          new Opcode( "LSE_5b", 7, AddressingMode.ABSY );
        OPCODES[ NOP_5c ] =
          new Opcode( "NOP_5c", 4, AddressingMode.ABSX );
        OPCODES[ LSE_5f ] =
          new Opcode( "LSE_5f", 7, AddressingMode.ABSX );
        OPCODES[ JAM_62 ] =
          new Opcode( "JAM_62", 2, AddressingMode.ZP );
        OPCODES[ RRA_63 ] =
          new Opcode( "RRA_63", 8, AddressingMode.IZX );
        OPCODES[ NOP_64 ] =
          new Opcode( "NOP_64", 3, AddressingMode.ZP );
        OPCODES[ RRA_67 ] =
          new Opcode( "RRA_67", 5, AddressingMode.ZP );
        OPCODES[ ARR_6b ] =
          new Opcode( "ARR_6b", 2, AddressingMode.IMM );
        OPCODES[ RRA_6f ] =
          new Opcode( "RRA_6f", 6, AddressingMode.ABS );
        OPCODES[ JAM_72 ] =
          new Opcode( "JAM_72", 2, AddressingMode.ZP );
        OPCODES[ RRA_73 ] =
          new Opcode( "RRA_73", 8, AddressingMode.IZY );
        OPCODES[ NOP_74 ] =
          new Opcode( "NOP_74", 4, AddressingMode.IMP );
        OPCODES[ RRA_77 ] =
          new Opcode( "RRA_77", 6, AddressingMode.ZPX );
        OPCODES[ NOP_7a ] =
          new Opcode( "NOP_7a", 2, AddressingMode.IMP );
        OPCODES[ RRA_7b ] =
          new Opcode( "RRA_7b", 7, AddressingMode.ABSY );
        OPCODES[ NOP_7c ] =
          new Opcode( "NOP_7c", 4, AddressingMode.ABSX );
        OPCODES[ RRA_7f ] =
          new Opcode( "RRA_7f", 7, AddressingMode.ABSX );
        OPCODES[ NOP_80 ] =
          new Opcode( "NOP_80", 2, AddressingMode.IMM );
        OPCODES[ NOP_82 ] =
          new Opcode( "NOP_82", 2, AddressingMode.IMM );
        OPCODES[ SAX_83 ] =
          new Opcode( "SAX_83", 6, AddressingMode.IZX );
        OPCODES[ SAX_87 ] =
          new Opcode( "SAX_87", 3, AddressingMode.ZP );
        OPCODES[ NOP_89 ] =
          new Opcode( "NOP_89", 2, AddressingMode.IMM );
        OPCODES[ XXA_8b ] =
          new Opcode( "XXA_8b", 2, AddressingMode.IMM );
        OPCODES[ SAX_8f ] =
          new Opcode( "SAX_8f", 4, AddressingMode.ABS );
        OPCODES[ JAM_92 ] =
          new Opcode( "JAM_92", 2, AddressingMode.ZP );
        OPCODES[ AHX_93 ] =
          new Opcode( "AHX_93", 2, AddressingMode.ZP );
        OPCODES[ SAX_97 ] =
          new Opcode( "SAX_97", 4, AddressingMode.ZPY );
        OPCODES[ TAS_9b ] =
          new Opcode( "TAS_9b", 2, AddressingMode.ABSY );
        OPCODES[ SHY_9c ] =
          new Opcode( "SHY_9c", 5, AddressingMode.ABS );
        OPCODES[ SHX_9e ] =
          new Opcode( "SHX_9e", 5, AddressingMode.ABS );
        OPCODES[ AHX_9f ] =
          new Opcode( "AHX_9f", 5, AddressingMode.ABS );
        OPCODES[ LAX_a3 ] =
          new Opcode( "LAX_a3", 6, AddressingMode.IZX );
        OPCODES[ LAX_a7 ] =
          new Opcode( "LAX_a7", 3, AddressingMode.ZP );
        OPCODES[ LXA_ab ] =
          new Opcode( "LXA_ab", 2, AddressingMode.IMM );
        OPCODES[ LAX_af ] =
          new Opcode( "LAX_af", 4, AddressingMode.ABS );
        OPCODES[ JAM_b2 ] =
          new Opcode( "JAM_B2", 2, AddressingMode.ZP );
        OPCODES[ LAX_b3 ] =
          new Opcode( "LAX_b3", 5, AddressingMode.IZY );
        OPCODES[ LAX_b7 ] =
          new Opcode( "LAX_b7", 4, AddressingMode.ZPY );
        OPCODES[ LAS_bb ] =
          new Opcode( "LAS_bb", 4, AddressingMode.ABSY );
        OPCODES[ LAX_bf ] =
          new Opcode( "LAX_bf", 4, AddressingMode.ABSY );
        OPCODES[ NOP_c2 ] =
          new Opcode( "NOP_c2", 2, AddressingMode.IMM );
        OPCODES[ DCM_c3 ] =
          new Opcode( "DCM_c3", 8, AddressingMode.IZX );
        OPCODES[ DCM_c7 ] =
          new Opcode( "DCM_c7", 5, AddressingMode.ZP );
        OPCODES[ AXS_cb ] =
          new Opcode( "AXS_cb", 2, AddressingMode.IMM );
        OPCODES[ DCM_cf ] =
          new Opcode( "DCM_cf", 6, AddressingMode.ABS );
        OPCODES[ JAM_d2 ] =
          new Opcode( "JAM_D2", 2, AddressingMode.ZP );
        OPCODES[ DCM_d3 ] =
          new Opcode( "DCM_d3", 8, AddressingMode.IZY );
        OPCODES[ NOP_d4 ] =
          new Opcode( "NOP_d4", 4, AddressingMode.IMP );
        OPCODES[ DCM_d7 ] =
          new Opcode( "DCM_d7", 6, AddressingMode.ZPX );
        OPCODES[ NOP_da ] =
          new Opcode( "NOP_da", 2, AddressingMode.IMP );
        OPCODES[ DCM_db ] =
          new Opcode( "DCM_db", 7, AddressingMode.ABSY );
        OPCODES[ NOP_dc ] =
          new Opcode( "NOP_dc", 4, AddressingMode.ABSX );
        OPCODES[ DCM_df ] =
          new Opcode( "DCM_df", 7, AddressingMode.ABSX );
        OPCODES[ NOP_e2 ] =
          new Opcode( "NOP_e2", 2, AddressingMode.IMM );
        OPCODES[ ISC_e3 ] =
          new Opcode( "ISC_e3", 8, AddressingMode.IZX );
        OPCODES[ ISC_e7 ] =
          new Opcode( "ISC_e7", 5, AddressingMode.ZP );
        OPCODES[ SBC_eb ] =
          new Opcode( "SBC_eb", 2, AddressingMode.IMM );
        OPCODES[ ISC_ef ] =
          new Opcode( "ISC_ef", 6, AddressingMode.ABS );
        OPCODES[ JAM_f2 ] =
          new Opcode( "JAM_F2", 2, AddressingMode.ZP );
        OPCODES[ ISC_f3 ] =
          new Opcode( "ISC_f3", 8, AddressingMode.IZY );
        OPCODES[ NOP_f4 ] =
          new Opcode( "NOP_f4", 4, AddressingMode.IMP );
        OPCODES[ ISC_f7 ] =
          new Opcode( "ISC_f7", 6, AddressingMode.ZPX );
        OPCODES[ NOP_fa ] =
          new Opcode( "NOP_fa", 2, AddressingMode.IMP );
        OPCODES[ ISC_fb ] =
          new Opcode( "ISC_fb", 7, AddressingMode.ABSY );
        OPCODES[ NOP_fc ] =
          new Opcode( "NOP_fc", 4, AddressingMode.ABSX );
        OPCODES[ ISC_ff ] =
          new Opcode( "ISC_ff", 7, AddressingMode.ABSX );
    }
}
