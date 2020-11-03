/* $Id: LoadComponent.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2000-2012 Michael G. Binz
 */
package de.michab.apps.route64;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import de.michab.simulator.mos6502.c64.C64Core;

/**
 * Implements a component that allows to load a file from an image file's
 * directory.
 *
 * @version $Revision: 782 $
 * @author Michael Binz
 */
@SuppressWarnings("serial")
final class LoadComponent extends JToolBar
{
    /**
     * A reference to the emulator.
     */
    private final C64Core _core;

    /**
     * A component that simplifies loading of entries contained in image files.
     */
    public LoadComponent( C64Core core )
    {
        super( "ACT_LOAD_COMPONENT" );

        setFloatable( false );
        add( new JLabel( "Drag an image file into this window." ) );

        _core = Objects.requireNonNull( core );

        setEnabled( false );
    }

    /**
     * Set the passed file as current image.
     *
     * @param file The image to set.
     */
    public void load( File file )
    {
        try
        {
            if ( _core.getImageFile() != null )
                _core.reset( true );

            _core.setImageFile(
                    file );

            setDirectoryEntries(
                    _core.getImageFileDirectory() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Set the list of directory entries that have to be displayed by the
     * component. If the passed array of entries is empty or null, then the
     * component is disabled.
     *
     * @param entries
     *            The list of directory entries.
     */
    public void setDirectoryEntries( byte[][] entries )
    {
        if ( entries != null && entries.length > 0 )
        {
            removeAll();
            repaint();

            for ( byte[] entry : entries )
                add( new PopItem( entry ) );
        }
    }

    private class PopItem extends AbstractAction
    {
        private final byte[] _raw;

        PopItem( byte[] rawText )
        {
            super( new String( rawText ).trim() );
            _raw = rawText;
        }

        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        @Override
        public void actionPerformed( ActionEvent e )
        {
            _core.load( _raw );
            _core.getDisplay().requestFocusInWindow();
        }
    };
}
