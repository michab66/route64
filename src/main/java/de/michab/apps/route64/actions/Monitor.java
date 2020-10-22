/* $Id: Monitor.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license) 2000
 * Copyright (c) 2000-2005 Michael G. Binz
 */
package de.michab.apps.route64.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.jdesktop.smack.MackAction;

import de.michab.apps.route64.Commodore64;
import de.michab.simulator.Debugger;
import de.michab.simulator.Processor;
import de.michab.simulator.mos6502.Cpu6510;



/**
 * <p>An archaic debugger.  Supports a single breakpoint depending on the
 * register contents and single-step control.</p>
 * <p>That was what we used in the eighties...</p>
 *
 * @version $Revision: 782 $
 * @author Stefan K&uuml;hnel
 * @author Michael G. Binz
 */
public final class Monitor
  extends
    MackAction
  implements
    Debugger
{
  private static final long serialVersionUID = 227824685216209211L;



  // TODO: Feature:  Display somewhere whether single step mode is active or
  // not.  E.g. Title:  Monitor - Running... vs. Monitor - Single step.
  /**
   *
   */
  private final JFrame _frame;



  /**
   * A reference to the <code>Cpu</code> instance we are debugging.
   */
  private final Cpu6510 _cpu;



  /**
   * The table showing the disassembled code.
   */
  private DisassemblerTableModel _disassembled = null;



  /**
   * The current program counter value.  This is only valid in case the
   * debugger controls the cpu thread, i.e. the cpu thread is waiting on the
   * _lock object.  If not valid this is negative.
   */
  private int _currentPc = -1;



  /**
   * The lock object used for single stepping.
   */
  private final Object _lock = new Object();



  /**
   * The radix used for number display.
   */
  private final static int _displayRadix = 16;



  /**
   * A second radix used for number display.  Currently this is used in the
   * tooltips of components that use the <code>_displayRadix</code> in their
   * display to display an alternate number representation.
   */
  private final static int _secondaryDisplayRadix = 10;



  /**
   *
   */
  private final static String BREAKPOINT_STATUS_NONE = "No breakpoint.";



  /**
   * A fixed width font.  Used for processor data and the like.  See the static
   * initialiser for more info.
   */
  private static final java.awt.Font _fixedWidthFont;

  static
  {
    // Get the default font size from a vanilla textfield...
    int defaultFontSize = new JTextField().getFont().getSize();
    // ...and use that to initialise our fixed width font.
    _fixedWidthFont = new Font( "Monospaced", Font.PLAIN, defaultFontSize );
  }



  /**
   * Constant means no breakpoint set.
   */
  private final static String BRKTYPE_NONE = "None";



  /**
   * Breakpoint on program counter.  Break if <code>_breakpointValue</code>
   * equals the current program counter.
   */
  private final static String BRKTYPE_PC   = "PC";



  /**
   * Breakpoint on accumulator. Break if <code>_breakpointValue</code>
   * equals the current accumulator contents.
   */
  private final static String BRKTYPE_ACCU = "Accu";


  /**
   * Breakpoint on X register.  Break if <code>_breakpointValue</code>
   * equals the current X register contents.
   */
  private final static String BRKTYPE_X    = "X";



  /**
   * Breakpoint on Y register.  Break if <code>_breakpointValue</code>
   * equals the current Y register contents.
   */
  private final static String BRKTYPE_Y    = "Y";



  /**
   * The type of the currently set breakpoint.
   */
  private String _breakpointType = BRKTYPE_NONE;



  /**
   * The breakpoint's condition value.  This value is only valid if
   * <code>_breakPointType</code> is not <code>BRKTYPE_NONE</code> and if it is
   * greater or equal zero.
   */
  private int _breakpointValue = -1;



  /**
   * True if single stepping.
   */
  private boolean _singleStep = false;



  /**
   *
   */
  private JTable _tbl = null;



  /**
   * Labels for registers.
   */
  private final JTextField _pcValue = new JTextField( 5 );
  private final JTextField _accuValue = new JTextField( 3 );
  private final JTextField _xValue = new JTextField( 3 );
  private final JTextField _yValue = new JTextField( 3 );



  /**
   * Checkboxes for Status register
   */
  private JToggleButton _flagN = new JToggleButton( "N" );
  private JToggleButton _flagV = new JToggleButton( "V" );
  private JToggleButton _flagC = new JToggleButton( "C" );
  private JToggleButton _flagZ = new JToggleButton( "Z" );
  private JToggleButton _flagB = new JToggleButton( "B" );
  private JToggleButton _flagI = new JToggleButton( "I" );
  private JToggleButton _flagD = new JToggleButton( "D" );



  /*
   * GUI elements for breakpoint definition.
   */
  private final JLabel _bpStatusLabel =
    new JLabel( BREAKPOINT_STATUS_NONE );
  private JComboBox<String> _breakpointTypeSelector = null;
  private final JTextField _bpValueTxt = new JTextField();
  private final JButton _setBreakpointButton = new JButton( "Set" );
  private final JButton _breakpointClearButton = new JButton( "Clear" );


  private final Commodore64 _home;

  /**
   * Create the visual system monitor.
   *
   * @param cpu A reference to the cpu to be controlled by this monitor.
   */
  public Monitor( Cpu6510 cpu, Commodore64 home )
  {
    super( "ACT_DEBUG" );

    _home = home;

    _cpu = cpu;

    _frame = new JFrame();
    _frame.setName( "monitorFrame" );

    _disassembled = new DisassemblerTableModel( _cpu.getMemory() );

    Container cp = _frame.getContentPane();
    cp.add( makeRegistersPanel(), BorderLayout.WEST );
    cp.add( makeDisassemblyPanel(), BorderLayout.CENTER );
    cp.add( makeBreakpointPanel(), BorderLayout.NORTH );
    cp.add( makeControlPanel(), BorderLayout.SOUTH );

    _frame.pack();

    // Closing the window performs a continue action.
    _frame.addWindowListener(
      new WindowAdapter()
      {
        @Override
        public void windowClosing( WindowEvent we )
        {
          Frame f = (Frame)we.getSource();
          f.setVisible( false );
          actionClearBreakpointImpl();
          actionContinueImpl();
        }
      }
    );
  }



  /**
   *
   */
  @Override
public void actionPerformed( ActionEvent ae )
  {
      throw new InternalError( "TODO" );
      //_home.show( _frame );
  }



  /**
   * Perform configuration.
   *
   * @param r The resources to use for the configuration.
   */
  public void configureFrom( ResourceBundle r )
  {
    // We access the default icon of the configurable action.
    Icon icon = (Icon)getValue( SMALL_ICON );
    // Replace the java cup in the top left window corner with something
    // more sensible.
    if ( icon instanceof ImageIcon )
      _frame.setIconImage( ((ImageIcon)icon).getImage() );
  }



  /*
   * Inherit Javadoc.
   */
  @Override
public void setProcessor( Processor processor )
  {
  }



  /**
   * Check whether the breakpoint is hit.
   *
   * @param pc The current program counter.
   * @return <code>True</code> if the breakpoint is hit.
   */
  private boolean isBreakpointHit( int pc )
  {
    boolean result=false;

    if ( _breakpointType == BRKTYPE_NONE )
        result = false;
    else if ( _breakpointType == BRKTYPE_PC )
        result = _breakpointValue == pc;
    else if ( _breakpointType == BRKTYPE_ACCU )
        result = _cpu.getAccu() == _breakpointValue;
    else if ( _breakpointType == BRKTYPE_X )
        result = _cpu.getX() == _breakpointValue;
    else if ( _breakpointType == BRKTYPE_Y )
        result = _cpu.getY() == _breakpointValue;
    else
        throw new InternalError( "Invalid breakpoint type." );

    return result;
  }



  /**
   * This method is part of the debugger interface.
   *
   * @param pc The program counter.
   */
  @Override
public void step( final int pc )
  {
    if ( ! _singleStep && ! isBreakpointHit( pc ) )
      return;

    // Call the user interface relevant parts on the event handler thread.
    SwingUtilities.invokeLater( new Runnable()
    {
      @Override
    public void run()
      {
        stepUi( pc );
      }
    } );

    // Lock the processor thread.
    try
    {
      synchronized (_lock)
      {
        _lock.wait();
      }

      // This is later-on the only place from where it is allowed to write new
      // register values to the cpu.
    }
    catch ( InterruptedException iex)
    {
      // No action on interruption.
    }
  }



  /**
   * Implements the user interface relevant parts of single stepping.
   *
   * @param pc The current program counter.
   * @throws InternalError If not called on event dispatch thread.
   * @see Monitor#step
   */
  private void stepUi( int pc )
  {
    if ( ! SwingUtilities.isEventDispatchThread() )
      throw new InternalError( "Not on EventDispatchThread." );

    _currentPc = pc;

    updateLabels();
    setControlButtons( false, true, true );
  }



  /**
   * Set the states of the control buttons.
   */
  private void setControlButtons( boolean brk, boolean step, boolean cnt )
  {
    _actionStep.setEnabled( step );
    _actionBreak.setEnabled( brk );
    _actionContinue.setEnabled( cnt );
  }



  /**
   *
   */
  private void updateTable()
  {
    int table_current_line = _disassembled.setPC( _currentPc );
    _tbl.changeSelection( table_current_line, 0, false, false );
  }



  /**
   *
   */
  private void updateLabels()
  {
    if ( _currentPc < 0 )
      throw new InternalError( "Invalid PC." );

    // Update the table display.
    updateTable();

    // Update the register display.
    setRegisterTextComponent( _pcValue, _currentPc );
    setRegisterTextComponent( _accuValue, _cpu.getAccu() );
    setRegisterTextComponent( _xValue, _cpu.getX() );
    setRegisterTextComponent( _yValue, _cpu.getY() );
    _flagN.setSelected(
      _cpu.isStatusFlagSet( Cpu6510.STATUS_FLAG_NEGATIVE ) );
    _flagV.setSelected(
      _cpu.isStatusFlagSet( Cpu6510.STATUS_FLAG_OVERFLOW ) );
    _flagC.setSelected(
      _cpu.isStatusFlagSet( Cpu6510.STATUS_FLAG_CARRY ) );
    _flagZ.setSelected(
      _cpu.isStatusFlagSet( Cpu6510.STATUS_FLAG_ZERO ) );
    _flagB.setSelected(
      _cpu.isStatusFlagSet( Cpu6510.STATUS_FLAG_BREAK ) );
    _flagI.setSelected(
      _cpu.isStatusFlagSet( Cpu6510.STATUS_FLAG_INTERRUPT ) );
    _flagD.setSelected(
      _cpu.isStatusFlagSet( Cpu6510.STATUS_FLAG_DECIMAL ) );
  }



  /**
   *
   */
  private static void setRegisterTextComponent( JTextField c, int value )
  {
    c.setText( Integer.toString( value, _displayRadix ) );
    c.setToolTipText( Integer.toString( value, _secondaryDisplayRadix ) );
  }



  /**
   * Switch the register display into a passive state used while no stepping
   * functionality is active.
   */
  private void deactivateRegisterDisplay()
  {
    _pcValue.setText( "" );
    _pcValue.setToolTipText( "" );
    _accuValue.setText( "" );
    _accuValue.setToolTipText( "" );
    _xValue.setText( "" );
    _xValue.setToolTipText( "" );
    _yValue.setText( "" );
    _yValue.setToolTipText( "" );

    _flagN.setSelected( false );
    _flagV.setSelected( false );
    _flagC.setSelected( false );
    _flagZ.setSelected( false );
    _flagB.setSelected( false );
    _flagI.setSelected( false );
    _flagD.setSelected( false );
  }



  /**
   * Set the necessary breakpoint information.
   */
  private void actionSetBreakpoint()
  {
    _breakpointType = (String)_breakpointTypeSelector.getSelectedItem();

    String breakpointMessage = _breakpointType + " = ";

    try
    {
      // Get the numeric value from the textfield.
      _breakpointValue =
        Integer.parseInt( _bpValueTxt.getText(), _displayRadix );

      breakpointMessage += "$";

      breakpointMessage +=
        Integer.toString( _breakpointValue, _displayRadix );

      // Display the message...
      _bpStatusLabel.setText( breakpointMessage );
      // ...and activate the debugger.
      _cpu.setDebugger(this);
    }
    catch ( NumberFormatException nfex )
    {
      _bpValueTxt.setText( "???" );
      _breakpointType = BRKTYPE_NONE;
      _breakpointValue = -1;
      _bpStatusLabel.setText( BREAKPOINT_STATUS_NONE );
    }
  }



  /**
   *
   */
  private final Action _setBreakpointAction = new AbstractAction()
  {
    private static final long serialVersionUID = -3778363354649432201L;

    @Override
    public void actionPerformed( ActionEvent ae )
    {
      actionSetBreakpoint();
    }
  };



  /**
   * Interrupt CPU execution and activate debugger.
   */
  private void actionBreakImpl()
  {
    _singleStep = true;
    setControlButtons( false, true, true );
    _cpu.setDebugger( this );
  }



  /**
   *
   */
  private final Action _actionBreak = new AbstractAction( "Break" )
  {
    private static final long serialVersionUID = -2038578875398455422L;

    @Override
    public void actionPerformed( ActionEvent ae )
    {
      actionBreakImpl();
    }
  };



  /**
   *
   */
  private void actionStepImpl()
  {
    _singleStep = true;

    synchronized ( _lock )
    {
     _lock.notify();
    }
  }



  /**
   *
   */
  private final Action _actionStep = new AbstractAction( "Step" )
  {
    private static final long serialVersionUID = 8387428089197138303L;

    @Override
    public void actionPerformed( ActionEvent ae )
    {
      actionStepImpl();
    }
  };



  /**
   * Continue until breakpoint, or - if no breakpoint is set - until
   * BREAK is pressed.
   */
  private void actionContinueImpl()
  {
    // Invalidate our program counter.
    _currentPc = -1;
    // Update user interface.
    deactivateRegisterDisplay();
    setControlButtons( true, false, false );
    // Leave single step mode.
    _singleStep = false;

    // If no breakpoint is set...
    if ( _breakpointType == BRKTYPE_NONE )
    {
      // ... we unlink ourselfes from the CPU.
      _cpu.setDebugger( null );
    }

    // Finally unlock the CPU thread and continue.
    synchronized ( _lock )
    {
      _lock.notify();
    }
  }



  /**
   *
   */
  private final Action _actionContinue = new AbstractAction( "Continue" )
  {
    private static final long serialVersionUID = 3543277983417810754L;

    @Override
    public void actionPerformed( ActionEvent ae )
    {
      actionContinueImpl();
    }
  };



  /**
   *
   */
  private void actionClearBreakpointImpl()
  {
    _breakpointType = BRKTYPE_NONE;
    _bpValueTxt.setText( "" );
    _bpStatusLabel.setText( BREAKPOINT_STATUS_NONE );
  }



  /**
   *
   */
  private final ActionListener _actionClearBreakpoint = new ActionListener()
  {
    @Override
    public void actionPerformed( ActionEvent ae )
    {
      actionClearBreakpointImpl();
    }
  };



  /**
   * Creates the register display panel.
   *
   * @return The register display panel.
   */
  private Component makeRegistersPanel()
  {
    JLabel pcLabel = new JLabel( "PC " );
    pcLabel.setLabelFor( _pcValue );
    JLabel accuLabel = new JLabel( "Accu " );
    accuLabel.setLabelFor( _accuValue );
    JLabel xLabel = new JLabel( "X " );
    xLabel.setLabelFor( _xValue );
    JLabel yLabel = new JLabel( "Y " );
    yLabel.setLabelFor( _yValue );

    _pcValue.setFont( _fixedWidthFont );
    _accuValue.setFont( _fixedWidthFont );
    _xValue.setFont( _fixedWidthFont );
    _yValue.setFont( _fixedWidthFont );

    Box box = Box.createVerticalBox();
    box.add(pcLabel);
    box.add(_pcValue);
    box.add(Box.createHorizontalStrut(5));
    box.add(accuLabel);
    box.add(_accuValue);
    box.add(Box.createHorizontalStrut(5));
    box.add(xLabel);
    box.add(_xValue);
    box.add(Box.createHorizontalStrut(5));
    box.add(yLabel);
    box.add(_yValue);
    box.add(Box.createHorizontalStrut(5));

    box.add(_flagN);
    box.add(_flagV);
    box.add(_flagC);
    box.add(_flagZ);
    box.add(_flagB);
    box.add(_flagI);
    box.add(_flagD);

    JPanel result =  new JPanel();
    TitledBorder rpBorder = new TitledBorder( "Registers" );
    result.setBorder( rpBorder );
    result.add( box );
    result.setName( "monitorRegistersPanel" );

    return result;
  }



  /**
   *
   */
  private Component makeControlPanel()
  {
    JToolBar result = new JToolBar( JToolBar.HORIZONTAL );
    result.setName( "monitorControlPanel" );

    result.setFloatable( false );
    result.add( _actionBreak );
    result.add( _actionStep );
    result.add( _actionContinue );

    setControlButtons( true, false, false );

    return result;
  }



  /**
   * Creates the breakpoint panel.
   *
   * @return The breakpoint panel.
   */
  private Component makeBreakpointPanel()
  {
    Box box1 = Box.createHorizontalBox();
    box1.add( _bpStatusLabel );
    box1.add( Box.createHorizontalGlue() );

    Box box2 = Box.createHorizontalBox();
    _breakpointTypeSelector = new JComboBox<String>();
    _breakpointTypeSelector.addItem( BRKTYPE_PC );
    _breakpointTypeSelector.addItem( BRKTYPE_ACCU );
    _breakpointTypeSelector.addItem( BRKTYPE_X );
    _breakpointTypeSelector.addItem( BRKTYPE_Y );
    box2.add( _breakpointTypeSelector );
    box2.add( new JLabel( " = " ) );
    _bpValueTxt.setColumns( 5 );
    box2.add( _bpValueTxt );
    box2.add( _setBreakpointButton );
    box2.add( _breakpointClearButton );

    JPanel result = new JPanel();
    result.setName( "monitorBreakpointPanel" );

    result.setLayout( new BoxLayout( result, BoxLayout.Y_AXIS ) );
    result.setBorder( new TitledBorder( "Breakpoint" ) );

    result.add( box1 );
    result.add( box2 );

    // Add the listener connections.
    _setBreakpointButton.addActionListener( _setBreakpointAction );
    _breakpointClearButton.addActionListener( _actionClearBreakpoint );

    return result;
  }



  /**
   * Creates the panel displaying the disassembly.
   *
   * @return The panel displaying the disassembly.
   */
  private Component makeDisassemblyPanel()
  {
    _tbl = new JTable( _disassembled );
    _tbl.setName( "monitorDisassemblyPanel" );

    _tbl.setFont( _fixedWidthFont );

    JScrollPane result =
      new JScrollPane (
        _tbl,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

    // TODO create smaller table.
    return result;
  }
}
