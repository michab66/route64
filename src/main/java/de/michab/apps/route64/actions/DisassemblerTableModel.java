/* $Id: DisassemblerTableModel.java 400 2010-09-06 02:01:43Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2000-2004 Michael G. Binz
 */
package de.michab.apps.route64.actions;

import java.util.*;
import javax.swing.table.*;
import de.michab.simulator.*;
import de.michab.simulator.mos6502.Opcodes;



/**
 * TableModel that disassembles code starting from a given PC.
 *
 * @author Stefan K&uuml;hnel
 */
class DisassemblerTableModel extends AbstractTableModel
{
  private static final long serialVersionUID = 9122932763869416643L;



  /**
   * The column names.
   */
  private static final String[] _names = new String[]
  {
    "Address",
    "Instruction"
  };



  /**
   *
   */
  private final ArrayList<AssemblerLine> _vect =
    new ArrayList<AssemblerLine>();



  /**
   *
   */
  private final Memory _mem;



  /**
   * The radix used for integer number display of the table model.
   */
  private static final int _radix = 16;



  /**
   * Create a disassembler table based on the passed memory.
   *
   * @param mem The memory to disassemble.
   */
  public DisassemblerTableModel( Memory mem )
  {
    _mem = mem;
  }



  /**
   * Informs the model about the current pc and returns the respective line
   * number for the decoded pc.
   *
   * @param pc The program counter to disassemble.
   * @return The table's line number for the passed PC position.
   */
  public int setPC(int pc)
  {
    AssemblerLine ali = new AssemblerLine( pc, disassemble( pc, _mem ) );

    int pos=0;
    if (_vect.contains(ali))
    {
      pos=_vect.indexOf(ali);
      _vect.set(pos,ali);
      fireTableCellUpdated(pos,1);
    }
    else
    {
      _vect.add(ali);
      java.util.Collections.sort( _vect );
      pos = _vect.indexOf( ali );
      fireTableRowsInserted(pos,pos);
    }

    return pos;
  }



  /*
   * Parent javadoc.
   */
  public int getRowCount()
  {
    return _vect.size();
  }



  /*
   * Parent javadoc.
   */
  public int getColumnCount()
  {
    return _names.length;
  }



  /*
   * Parent javadoc.
   */
  public Object getValueAt( int row, int col )
  {
    String result = "";

    if ( (_vect.size()>0) && (row<=_vect.size()) )
    {
      AssemblerLine ali= _vect.get(row);

      if ( col == 0 )
      {
          result = Integer.toString( ali.getAddr(), _radix );
      }
      else if ( col == 1 )
      {
          result = ali.getLine();
      }
      else
        throw new InternalError();
    }

    return result;
  }



  /*
   * Parent javadoc.
   */
  public String getColumnName( int columnIndex )
  {
    return _names[ columnIndex ];
  }



  /**
   * Disassemble the code at the given address.
   *
   * @param pc The address of the instruction to decode.
   * @param mem The memory containing the instruction to decode.
   */
  private static String disassemble( int pc, Memory mem )
  {
    int opcode = byte2int( mem.read(pc) );

    int len = Opcodes.getEncodingLength( opcode );

    int value = 0;
    if ( len == 2 )
    {
      value = byte2int( mem.read(pc+1) );
    }
    else if ( len == 3 )
    {
      value = mem.getVectorAt( pc+1 );
    }

    // Well, in many cases this is unnecessary since we only have a single
    // argument opcode, but it doesn't hurt and is simple.
    String displayValue = "$" + Integer.toHexString( value );

    return java.text.MessageFormat.format( Opcodes.getText( opcode ),
                                           new Object[]{ displayValue } );
  }



  /**
   * Converts an unsigned byte into an integer without sign propagation.
   *
   * @param b The byte to convert.
   * @return The converted value.
   */
  private static int byte2int(byte b)
  {
    return b & 0xff;
  }
}
