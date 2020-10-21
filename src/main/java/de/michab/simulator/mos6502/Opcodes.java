/* $Id: Opcodes.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license).
 * Copyright (c) 2003-2005 Michael G. Binz
 */
package de.michab.simulator.mos6502;

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
    return getText( opcode ) == UNKNOWN_OPCODE;
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
  public static final int  BCC_REL  = 0x90;
  public static final int BCS_REL  = 0xb0;
  public static final int BEQ_REL  = 0xf0;
  public static final int BIT_ZP   = 0x24;
  // Added for 65C02
  public static final int BIT_ZPX  = 0x34;
  public static final int BIT_ABS  = 0x2c;
  // Added for 65C02
  public static final int BIT_ABSX = 0x3c;
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
  public static final int EOR_IMM  = 0x49;
  public static final int EOR_ZP   = 0x45;
  public static final int EOR_ZPX  = 0x55;
  public static final int EOR_ABS  = 0x4D;
  public static final int EOR_ABSX = 0x5D;
  public static final int EOR_ABSY = 0x59;
  public static final int EOR_IZX  = 0x41;
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
   * Here's where the undocumented opcodes start.
   */
  public static final int uLSE_ZP   = 0x47;
  public static final int uLSE_ZPX  = 0x57;
  public static final int uLSE_IZX  = 0x43;
  public static final int uLSE_IZY  = 0x53;
  public static final int uLSE_ABS  = 0x4f;
  public static final int uLSE_ABSX = 0x5f;
  public static final int uLSE_ABSY = 0x5b;
  public static final int uSAX_ZP   = 0x87;
  public static final int uSAX_ZPY  = 0x97;
  public static final int uSAX_IZX  = 0x83;
  public static final int uSAX_ABS  = 0x8f;



  /**
   * Text used for unknown opcodes.
   */
  private static final String UNKNOWN_OPCODE = "???";




  private final static int IMP = 0; 
  private final static int REL = 1;
  private final static int IMM = 2;
  private final static int ZP = 3;
  private final static int ZPX = 4;
  private final static int ZPY = 5;
  private final static int IZX = 6;
  private final static int IZY = 7;
  private final static int ABS = 8;
  private final static int ABSX = 9;
  private final static int ABSY = 10;
  private final static int IND = 11;
  private final static int NUM_OF_ADRESSING_MODES = 12;



  /**
   * An array holding for each addressing mode the length of the encoded
   * instruction in bytes.
   */
  private static int[] ENCODING_LENGTH = 
    new int[ NUM_OF_ADRESSING_MODES ];

  /**
   * Disassembler opcode templates for the adressing modes.
   */
  private static String[] OPCODE_TEMPLATE = 
    new String[ NUM_OF_ADRESSING_MODES ];

  static
  {
    ENCODING_LENGTH[IMP] = 1;
    ENCODING_LENGTH[REL] = 2;
    ENCODING_LENGTH[IMM] = 2;
    ENCODING_LENGTH[ZP] = 2;
    ENCODING_LENGTH[ZPX] = 2;
    ENCODING_LENGTH[ZPY] = 2;
    ENCODING_LENGTH[IZX] = 2;
    ENCODING_LENGTH[IZY] = 2;
    ENCODING_LENGTH[ABS] = 3;
    ENCODING_LENGTH[ABSX] = 3;
    ENCODING_LENGTH[ABSY] = 3;
    ENCODING_LENGTH[IND] = 3;

    OPCODE_TEMPLATE[IMP] = "";
    OPCODE_TEMPLATE[REL] = "{0}";
    OPCODE_TEMPLATE[IMM] = "#{0}";
    OPCODE_TEMPLATE[ZP]  = "{0}";
    OPCODE_TEMPLATE[ZPX] = "{0},X";
    OPCODE_TEMPLATE[ZPY] = "{0},Y";
    OPCODE_TEMPLATE[IZX] = "({0},X)";
    OPCODE_TEMPLATE[IZY] = "({0}),Y";
    OPCODE_TEMPLATE[ABS] = "{0}";
    OPCODE_TEMPLATE[ABSX] = "{0},X";
    OPCODE_TEMPLATE[ABSY] = "{0},Y";
    OPCODE_TEMPLATE[IND] = "({0})";
  }



  static class Opcode
  {
    Opcode(
        String name, 
        int baseTime, 
        int addressingMode )
    {
      _name = name + " " + OPCODE_TEMPLATE[addressingMode];
      _length = ENCODING_LENGTH[ addressingMode ];
      _time = baseTime;
      _adressingMode = addressingMode;
    }
    Opcode(
        String name, 
        int baseTime )
    {
      this( name, baseTime, IMP );
    }

    final String _name;
    final int _length;
    final int _time;
    final int _adressingMode;
  }


  final static Opcode[] OPCODES =
    new Opcode[ 256 ];

  static
  {
    Arrays.fill( OPCODES, new Opcode( UNKNOWN_OPCODE, 88 ) );

    OPCODES[ ADC_IMM ] = 
      new Opcode( "ADC", 2, IMM );
    OPCODES[ ADC_ZP ] =
      new Opcode( "ADC", 3, ZP );
    OPCODES[ ADC_ZPX ] =
      new Opcode( "ADC", 4, ZPX );
    OPCODES[ ADC_ABS ] =
      new Opcode( "ADC", 4, ABS );
    OPCODES[ ADC_ABSX ] =
      new Opcode( "ADC", +4, ABSX );
    OPCODES[ ADC_ABSY ] =
      new Opcode( "ADC", +4, ABSY );
    OPCODES[ ADC_IZX ] =
      new Opcode( "ADC", 6, IZX );
    OPCODES[ ADC_IZY ] =
      new Opcode( "ADC", +5, IZY );
    OPCODES[ AND_IMM ] =
      new Opcode( "AND", 2, IMM );      
    OPCODES[ AND_ZP  ] =
      new Opcode( "AND", 2, ZP );
    OPCODES[ AND_ZPX ] =
      new Opcode( "AND", 3, ZPX );
    OPCODES[ AND_ABS ] =
      new Opcode( "AND", 4, ABS );
    OPCODES[ AND_ABSX ] =
      new Opcode( "AND", +4, ABSX );
    OPCODES[ AND_ABSY ] =
      new Opcode( "AND", +4, ABSY );
    OPCODES[ AND_IZX ] =
      new Opcode( "AND", 6, IZX );
    OPCODES[ AND_IZY] =
      new Opcode( "AND", +5, IZY );
    OPCODES[ ASL_IMP  ] =
      new Opcode( "ASL", 2 );
    OPCODES[ ASL_ZP ] =
      new Opcode( "ASL", 5, ZP );
    OPCODES[ ASL_ZPX ] =
      new Opcode( "ASL", 6, ZPX );
    OPCODES[ ASL_ABS ] =
      new Opcode( "ASL", 6, ABS );
    OPCODES[ ASL_ABSX ] =
      new Opcode( "ASL", 7, ABSX );
    OPCODES[ BCC_REL ] =
      new Opcode( "BCC", 2, REL );
    OPCODES[ BCS_REL  ] =       
      new Opcode( "BCS", 2, REL );
    OPCODES[ BEQ_REL] =
      new Opcode( "BEQ", 2, REL );
    OPCODES[ BIT_ZP ] =
      new Opcode( "BIT", 3, ZP );
    OPCODES[ BIT_ZPX ]  =
      new Opcode( "BIT", 4, ZPX );
    OPCODES[ BIT_ABS  ] =
      new Opcode( "BIT", 4, ABS );
    OPCODES[ BIT_ABSX] =
      new Opcode( "BIT", +4, ABSX );    
    OPCODES[ BMI_REL] =
      new Opcode( "BMI", 2, REL );
    OPCODES[ BNE_REL ] =
      new Opcode( "BNE", 2, REL );
    OPCODES[ BPL_REL ] =
      new Opcode( "BPL", 2, REL );
    OPCODES[ BRK_IMP ] =
      new Opcode( "BRK", 7 );
    OPCODES[ BVC_REL ] =
      new Opcode( "BVC", 2, REL );
    OPCODES[ BVS_REL ] =
      new Opcode( "BVS", 2, REL );
    OPCODES[ CLC_IMP ] =
      new Opcode( "CLC", 2 );
    OPCODES[ CLD_IMP ] =
      new Opcode( "CLD", 2 );
    OPCODES[ CLI_IMP ] =
      new Opcode( "CLI", 2 );
    OPCODES[ CLV_IMP ] =
      new Opcode( "CLV", 2 );
    OPCODES[ CMP_IMM ] =
      new Opcode( "CMP", 2, IMM );
    OPCODES[ CMP_ZP ] =
      new Opcode( "CMP", 3, ZP );
    OPCODES[ CMP_ZPX ] =
      new Opcode( "CMP", 4, ZPX );
    OPCODES[ CMP_IZX ] =
      new Opcode( "CMP", 6, IZX );
    OPCODES[ CMP_IZY ] =
      new Opcode( "CMP", +5, IZY );
    OPCODES[ CMP_ABS ] =
      new Opcode( "CMP", 4, ABS );
    OPCODES[ CMP_ABSX ] =
      new Opcode( "CMP", +4, ABSX );
    OPCODES[ CMP_ABSY ] =
      new Opcode( "CMP", +4, ABSY );
    OPCODES[ CPX_IMM ] =
      new Opcode( "CPX", 2, IMM );
    OPCODES[ CPX_ZP ] =
      new Opcode( "CPX", 3, ZP );
    OPCODES[ CPX_ABS ] =
      new Opcode( "CPX", 4, ABS );
    OPCODES[ CPY_IMM ] =
      new Opcode( "CPY", 2, IMM );
    OPCODES[ CPY_ZP ]  = 
      new Opcode( "CPY", 3, ZP );
    OPCODES[ CPY_ABS ]  =
      new Opcode( "CPY", 4, ABS );
    OPCODES[ DEC_ZP  ]  =
      new Opcode( "DEC", 5, ZP );
    OPCODES[ DEC_ZPX ]  =
      new Opcode( "DEC", 6, ZPX );
    OPCODES[ DEC_ABS ]  =
      new Opcode( "DEC", 6, ABS );
    OPCODES[ DEC_ABSX ] =
      new Opcode( "DEC", 7, ABSX );
    OPCODES[ DEX_IMP] =
      new Opcode( "DEX", 2, IMP );
    OPCODES[ DEY_IMP ] =
      new Opcode( "DEY", 2, IMP );
    OPCODES[ EOR_IMM ] =
      new Opcode( "EOR", 2, IMM );
    OPCODES[ EOR_ZP ] =
      new Opcode( "EOR", 3, ZP );
    OPCODES[ EOR_ZPX ] =
      new Opcode( "EOR", 4, ZPX );
    OPCODES[ EOR_ABS ] =
      new Opcode( "EOR", 4, ABS );
    OPCODES[ EOR_ABSX ] =
      new Opcode( "EOR", +4, ABSX );
    OPCODES[ EOR_ABSY ] =
      new Opcode( "EOR", +4, ABSY );
    OPCODES[ EOR_IZX ] =
      new Opcode( "EOR", 6, IZX );
    OPCODES[ EOR_IZY ] =
      new Opcode( "EOR", +5, IZY );
    OPCODES[ INC_ZP ]  =
      new Opcode( "INC", 5, ZP );
    OPCODES[ INC_ZPX ] =
      new Opcode( "INC", 6, ZPX );
    OPCODES[ INC_ABS ] =
      new Opcode( "INC", 6, ABS );
    OPCODES[ INC_ABSX ] =
      new Opcode( "INC", 7, ABSX );
    OPCODES[ INX_IMP ] =
      new Opcode( "INX", 2 );
    OPCODES[ INY_IMP ] =
      new Opcode( "INY", 2 );
    OPCODES[ JMP_ABS  ] =
      new Opcode( "JMP", 3, ABS );
    OPCODES[ JMP_IND  ] =
      new Opcode( "JMP", 5, IND );
    OPCODES[ JSR_ABS  ] =
      new Opcode( "JSR", 6, ABS );
    OPCODES[ LDA_IMM  ] =
      new Opcode( "LDA", 2, IMM );
    OPCODES[ LDA_ZP  ] =
      new Opcode( "LDA", 3, ZP );
    OPCODES[ LDA_ZPX  ] =
      new Opcode( "LDA", 4, ZPX );
    OPCODES[ LDA_ABS ] =
      new Opcode( "LDA", 4, ABS );
    OPCODES[ LDA_ABSX ] =
      new Opcode( "LDA", +4, ABSX );
    OPCODES[ LDA_ABSY ] =
      new Opcode( "LDA", +4, ABSY );
    OPCODES[ LDA_IZX ] =
      new Opcode( "LDA", 6, IZX );
    OPCODES[ LDA_IZY ] =
      new Opcode( "LDA", +5, IZY );
    OPCODES[ LDX_IMM  ]  =
      new Opcode( "LDX", 2, IMM );
    OPCODES[ LDX_ZP ]  =
      new Opcode( "LDX", 3, ZP );
    OPCODES[ LDX_ZPY ]  =
      new Opcode( "LDX", 4, ZPY );
    OPCODES[ LDX_ABS ]  =
      new Opcode( "LDX", 4, ABS );
    OPCODES[ LDX_ABSY ] =
      new Opcode( "LDX", +4, ABSY );
    OPCODES[ LDY_IMM ] =
      new Opcode( "LDY", 2, IMM );
    OPCODES[ LDY_ZP ] =
      new Opcode( "LDY", 3, ZP );
    OPCODES[ LDY_ZPX ]  =
      new Opcode( "LDY", 4, ZPX );
    OPCODES[ LDY_ABS ]  =
      new Opcode( "LDY", 4, ABS );
    OPCODES[ LDY_ABSX ]  =
      new Opcode( "LDY", +4, ABSX );
    OPCODES[ LSR_IMP ]  =
      new Opcode( "LSR", 2 );
    OPCODES[ LSR_ZP ]  =
      new Opcode( "LSR", 5, ZP );
    OPCODES[ LSR_ZPX  ]  =
      new Opcode( "LSR", 6, ZPX );
    OPCODES[ LSR_ABS   ]  =
      new Opcode( "LSR", 6, ABS );
    OPCODES[ LSR_ABSX ] =
      new Opcode( "LSR", 7, ABSX );
    OPCODES[ NOP_IMP ] =
      new Opcode( "NOP", 2 );
    OPCODES[ ORA_IMM ] =
      new Opcode( "ORA", 2, IMM );
    OPCODES[ ORA_ZP ] =
      new Opcode( "ORA", 2, ZP );
    OPCODES[ ORA_ZPX  ]  =
      new Opcode( "ORA", 3, ZPX );
    OPCODES[ ORA_IZX ] =
      new Opcode( "ORA", 6, IZX );
    OPCODES[ ORA_IZY ] =
      new Opcode( "ORA", +5, IZY );
    OPCODES[ ORA_ABS ] =
      new Opcode( "ORA", 4, ABS );
    OPCODES[ ORA_ABSX ] =
      new Opcode( "ORA", +4, ABSX );
    OPCODES[ ORA_ABSY ] =
      new Opcode( "ORA", +4, ABSY );
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
      new Opcode( "ROL", 5, ZP );
    OPCODES[ ROL_ZPX ] =
      new Opcode( "ROL", 6, ZPX );
    OPCODES[ ROL_ABS ] =
      new Opcode( "ROL", 6, ABS );
    OPCODES[ ROL_ABSX ] =
      new Opcode( "ROL", 7, ABSX );
    OPCODES[ ROR_IMP ] =
      new Opcode( "ROR", 2 );
    OPCODES[ ROR_ZP ] =
      new Opcode( "ROR", 5, ZP );
    OPCODES[ ROR_ZPX ]  =
      new Opcode( "ROR", 6, ZPX );
    OPCODES[ ROR_ABS   ]  =
      new Opcode( "ROR", 6, ABS );
    OPCODES[ ROR_ABSX ]  =
      new Opcode( "ROR", 7, ABSX );
    OPCODES[ RTI_IMP  ]  =
      new Opcode( "RTI", 6 );
    OPCODES[ RTS_IMP  ]  =
      new Opcode( "RTS", 6 );  
    OPCODES[ SBC_IMM ]  =
      new Opcode( "SBC", 2, IMM );
    OPCODES[ SBC_ZP ]  =
      new Opcode( "SBC", 3, ZP );
    OPCODES[ SBC_ZPX ]  =
      new Opcode( "SBC", 4, ZPX );
    OPCODES[ SBC_ABS  ]  =
      new Opcode( "SBC", 4, ABS );
    OPCODES[ SBC_ABSX  ]  =
      new Opcode( "SBC", +4, ABSX );
    OPCODES[ SBC_ABSY ]  =
      new Opcode( "SBC", +4, ABSY );
    OPCODES[ SBC_IZX  ]  = 
      new Opcode( "SBC", 6, IZX );
    OPCODES[ SBC_IZY ]  =
      new Opcode( "SBC", +5, IZY );
    OPCODES[ SEC_IMP ]  =
      new Opcode( "SEC", 2 );
    OPCODES[ SED_IMP ]  =
      new Opcode( "SED", 2 );
    OPCODES[ SEI_IMP ]  =
      new Opcode( "SEI", 2 );
    OPCODES[ STA_ZP ] =
      new Opcode( "STA", 3, ZP );
    OPCODES[ STA_ZPX ] =
      new Opcode( "STA", 4, ZPX );
    OPCODES[ STA_ABS ] =
      new Opcode( "STA", 4, ABS );
    OPCODES[ STA_ABSX ] =
      new Opcode( "STA", 5, ABSX );
    OPCODES[ STA_ABSY ] =
      new Opcode( "STA", 5, ABSY );
    OPCODES[ STA_IZX ] =
      new Opcode( "STA", 6, IZX );
    OPCODES[ STA_IZY ] =
      new Opcode( "STA", 6, IZY );
    OPCODES[ STX_ZP ] =
      new Opcode( "STX", 3, ZP );
    OPCODES[ STX_ZPY ] =
      new Opcode( "STX", 4, ZPY );
    OPCODES[ STX_ABS ] =
      new Opcode( "STX", 4, ABS );
    OPCODES[ STY_ZP ]  =
      new Opcode( "STY", 3, ZP );
    OPCODES[ STY_ZPX ] =
      new Opcode( "STY", 4, ZPX );
    OPCODES[ STY_ABS ] =
      new Opcode( "STY", 4, ABS );
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
    OPCODES[ uSAX_ZP ] =
      new Opcode( "SAX", 3, ZP );
    OPCODES[ uSAX_ZPY ] =
      new Opcode( "SAX", 4, ZPY );
    OPCODES[ uSAX_IZX ] =
      new Opcode( "SAX", 6, IZX );
    OPCODES[ uSAX_ABS ] =
      new Opcode( "SAX", 4, ABS );
  }
}
