package de.michab.swingx;

import javax.swing.JButton;



/**
 * The apps control frame.
 *
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class ControlFrame extends javax.swing.JFrame
{
    /**
     * @param argv The command line arguments.
     */
    public static void main( String[] argv )
    {
        java.awt.EventQueue.invokeLater( new Runnable()
        {
            public void run()
            {
                RoundFrame dotty =
                    new RoundFrame( "Michael's round one..." );

                dotty.setDefaultCloseOperation( EXIT_ON_CLOSE );

                dotty.getContentPane().add(
                        new JButton( "North" ),
                        RoundBorderLayout.Position.NORTH );
                dotty.getContentPane().add(
                        new JButton( "West" ),
                        RoundBorderLayout.Position.WEST );
                dotty.getContentPane().add(
                        new JButton( "Hello, World!" ),
                        RoundBorderLayout.Position.CENTER );
                dotty.getContentPane().add(
                        new JButton( "East" ),
                        RoundBorderLayout.Position.EAST );
                dotty.getContentPane().add(
                        new JButton( "South" ),
                        RoundBorderLayout.Position.SOUTH );

                dotty.pack();
                dotty.setVisible( true );
            }
        } );
    }
}
