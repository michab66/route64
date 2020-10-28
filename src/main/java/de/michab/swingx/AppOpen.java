/* $Id: AppOpen.java 48 2014-11-23 10:28:59Z michab66 $
 *
 * Mp3 tagger.
 *
 * Released under Gnu Public License
 * Copyright © 2008-2010 Michael G. Binz
 */
package de.michab.swingx;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.smack.util.FileUtil;

/**
 * The general application open action.  This encapsulates classic
 * dialog-based loading as well as drag and drop driven loading.
 *
 * @param FT Document type
 *
 * @version $Rev: 48 $
 * @author Michael Binz
 */
class AppOpen
    implements
        DropTargetListener
{
    private static final Logger LOG =
            Logger.getLogger( AppOpen.class.getName() );

    /**
     * The used file filter.
     */
    private FileFilter _filter;

    /**
     * Flag for directory resolution.  If this is true, then
     * directories are allowed on drop operations.  If a directory is
     * dropped its contents is scanned for files matching the filter.
     */
    private boolean _resolveDirs;

    /**
     * A transformer from File to FT,
     */
    //private Transformer<FT,File> _transformer;


    private final Consumer<File> _consumer;

    /**
     * Create an instance.
     */
    public AppOpen(
            Component host,
            Consumer<File> consumer )
    {
        LOG.setLevel( Level.WARNING );
        _consumer = Objects.requireNonNull(
                consumer );

        host.setEnabled(
                true );
        host.setDropTarget( new DropTarget(
                host,
                this ) );
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde)
    {

        LOG.info( "Enter: " + isDropAcceptable( dtde ) );

        if ( isDropAcceptable( dtde ) )
            dtde.acceptDrag( DnDConstants.ACTION_COPY_OR_MOVE );
        else
            dtde.rejectDrag();
    }

    @Override
    public void dragExit(DropTargetEvent dte)
    {
        LOG.info( "Exit" );
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde)
    {
        LOG.info( "Over" );
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

                File[] files = getTransferFiles( dtde.getTransferable() );

                if ( files.length > 0)
                {
                    _consumer.accept( files[0] );
                }

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
    private File[] getTransferFiles( Transferable x )
    {
        File[] result;

        try
        {
            // Now get the transferred data and be as defensive as possible.
            // We expect a java.util.List of java.io.Files.
            List<File> fileList = (List<File>)
                    x.getTransferData( DataFlavor.javaFileListFlavor );
            result = fileList.toArray( new File[fileList.size()] );
        }
        catch ( Exception  e )
        {
            LOG.log( Level.FINE, e.getLocalizedMessage(), e );
            return new File[0];
        }

        if ( _resolveDirs )
            result = FileUtil.resolveDirectories( result );

        if ( _filter != null )
            result = FileUtil.filterFiles( result, _filter );

        return result;
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde)
    {
    }

    /**
     *
     * @param dtde
     * @return
     */
    private boolean isDropAcceptable( DropTargetDragEvent dtde )
    {
        if (!dtde.isDataFlavorSupported( DataFlavor.javaFileListFlavor ))
            return false;

//        File[] files = getTransferFiles( dtde.getTransferable() );
//
//        if ( files.length < 1 )
//            return false;

        return true;
    }

    /**
     * The public mack api that allows to load files.  Encapsulates
     * transforming these files to FTs.
     *
     * @param files The files to load.
     */
    private static void cload( File[] files )
    {
        LOG.info( "count=" + files.length );
        for ( int i = 0 ; i < files.length ; i++ )
        {
            var c = files[i];
            LOG.info( i + " " + c );
        }
    }
    private static void cload( File file )
    {
        LOG.warning( "load=" + file );
    }

    public static void main( String[] args )
    {
        SwingUtilities.invokeLater( () -> {
            var frame = new JFrame( "Drop test." );
            frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            frame.setSize( new Dimension( 300, 200 ) );

            new AppOpen( frame, AppOpen::cload );

            frame.setVisible( true );
        } );
    }
}
