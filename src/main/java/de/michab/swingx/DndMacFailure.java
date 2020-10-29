/* $Id: 313 $ */
package de.michab.swingx;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 *
 */
class DndMacFailure extends DropTargetAdapter
{
    public DndMacFailure( Component host ) {
        host.setEnabled(
                true );
        host.setDropTarget( new DropTarget(
                host,
                this ) );
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        logContent( "Enter", getTransferFiles( dtde.getTransferable() ) );
    }

    private boolean overDone = false;

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if ( overDone )
            return;

        logContent( "Over", getTransferFiles( dtde.getTransferable() ) );

        overDone = true;
    }

    /*
     * Inherit javadoc.
     */
    @Override
    public void drop( DropTargetDropEvent dtde ) {

        boolean status = false;

        try {
            dtde.acceptDrop( DnDConstants.ACTION_COPY_OR_MOVE );
            logContent( "Drop", getTransferFiles( dtde.getTransferable() ) );
            status = true;
        }
        finally {
            dtde.dropComplete( status );
        }
    }

    @SuppressWarnings("unchecked")
    private List<File> getTransferFiles( Transferable transferable ) {
        assert transferable.isDataFlavorSupported( DataFlavor.javaFileListFlavor );

        try {
            return (List<File>)
                    transferable.getTransferData( DataFlavor.javaFileListFlavor );
        }
        catch ( Exception  e ) {
            throw new RuntimeException( e );
        }
    }

    private static void logContent( String status, List<File> files ) {
        if ( files == null ) {
            System.out.println( status + " no files." );
            return;
        }
        System.out.println( status + " " + files.size() + ":" );
        files.forEach( c ->
            System.out.println( c.toString() ) );
    }

    public static void main( String[] args ) {
        SwingUtilities.invokeLater( () -> {
            var frame = new JFrame( "Drop test." );
            frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            frame.setSize( new Dimension( 300, 200 ) );

            new DndMacFailure( frame );

            frame.setVisible( true );
        } );
    }
}

/*
Mac:
Enter no files.
Over no files.
Drop 1:
/Users/mic/Desktop/test.txt
*/
