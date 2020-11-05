/* $Id: AppOpen.java 48 2014-11-23 10:28:59Z michab66 $
 *
 * Released under Gnu Public License
 * Copyright © 2008-2020 Michael G. Binz
 */
package de.michab.apps.route64;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdesktop.util.PlatformType;

/**
 * Handles file drops by passing the dropped file to a consumer.
 *
 * @author Michael Binz
 */
class DropHandler
    extends
        DropTargetAdapter
{
    private static final Logger LOG =
            Logger.getLogger( DropHandler.class.getName() );

    /**
     * The used file filter.
     */
    private Predicate<File> _filter;

    private final Consumer<File> _consumer;

    /**
     * Create an instance.
     */
    public DropHandler(
            Component host,
            Consumer<File> consumer )
    {
        LOG.setLevel( Level.WARNING );

        _consumer = Objects.requireNonNull(
                consumer );

        host.setEnabled(
                true );
        new DropTarget(
                host,
                this );
    }

    /**
     * Set a filter.
     * @param filter The filter to set.  Files are removed if the
     * passed predicate returns true.
     * @return Object instance for chained assignment.
     */
    public DropHandler setFilter( Predicate<File> filter )
    {
        _filter = Objects.requireNonNull( filter.negate() );
        return this;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde)
    {
        // On Mac it is not possible to access the dragged
        // content inside the DragEvent, so we simply accept.
        if ( PlatformType.is( PlatformType.OS_X ) || isDropAcceptable( dtde ) )
            dtde.acceptDrag( DnDConstants.ACTION_COPY_OR_MOVE );
        else
            dtde.rejectDrag();
    }

    /*
     * Inherit javadoc.
     */
    @Override
    public void drop( DropTargetDropEvent dtde )
    {
        LOG.info( "Drop" );

        // Used on dnd protocol completion in 'finally' below.
        boolean status = false;

        try
        {
            if ( dtde.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) )
            {
                // First we accept the drop to get the dnd protocol into the
                // needed state for getting the data.
                dtde.acceptDrop( DnDConstants.ACTION_COPY_OR_MOVE );

                List<File> files = getTransferFiles( dtde.getTransferable() );

                if ( files.size() > 0)
                    _consumer.accept( files.get( 0 ) );

                // Everything went fine.
                status = true;
            }
            else
                dtde.rejectDrop();
        }
        // Catch potential IO exceptions and keep dnd protocol in sync.
        catch (Exception e)
        {
            LOG.log( Level.WARNING, e.getLocalizedMessage(), e );
            dtde.rejectDrop();
        }
        // And again: Last step in dnd protocol. After that we are ready to
        // accept the next drop.
        finally
        {
            dtde.dropComplete( status );
        }
    }

    /**
     *
     * @param x
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<File> getTransferFiles( Transferable x )
    {
        try
        {
            List<File> fileList = (List<File>)
                    x.getTransferData( DataFlavor.javaFileListFlavor );
            // Move files into a list that implements remove.
            fileList = new ArrayList<File>( fileList );
            if ( _filter != null )
                fileList.removeIf( _filter );

            return fileList;
        }
        catch ( Exception  e )
        {
            LOG.log( Level.WARNING, e.getLocalizedMessage(), e );
            return Collections.emptyList();
        }
    }

    private boolean isDropAcceptable( DropTargetDragEvent dtde )
    {
        if (!dtde.isDataFlavorSupported( DataFlavor.javaFileListFlavor ))
            return false;

        List<File> files = getTransferFiles( dtde.getTransferable() );

        if ( files.size() < 1 )
            return false;

        return true;
    }

//    private static void cload( File file )
//    {
//        LOG.warning( "load=" + file );
//    }
//
//    private static boolean filterPredicate( File f )
//    {
//        if ( f.isDirectory() )
//            return true;
//        if ( f.getPath().endsWith( ".txt" ) )
//            return false;
//        return true;
//    }
//
//    public static void main( String[] args )
//    {
//        SwingUtilities.invokeLater( () -> {
//            var frame = new JFrame( "Drop test." );
//            frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//            frame.setSize( new Dimension( 300, 200 ) );
//
//            new DropHandler( frame, DropHandler::cload ).
//                setFilter( DropHandler::filterPredicate );
//
//            frame.setVisible( true );
//        } );
//    }
}
