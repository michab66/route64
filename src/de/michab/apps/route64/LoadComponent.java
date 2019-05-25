/* $Id: LoadComponent.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Project: Route64
 *
 * Released under GNU public license (www.gnu.org/copyleft/gpl.html)
 * Copyright (c) 2000-2012 Michael G. Binz
 */
package de.michab.apps.route64;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JPopupMenu;

import org.jdesktop.smack.MackAction;

import de.michab.simulator.mos6502.c64.C64Core;



/**
 * Implements a component that allows to load a file from an image file's
 * directory.
 *
 * @version $Revision: 782 $
 * @author Michael Binz
 */
@SuppressWarnings("serial")
final class LoadComponent extends MackAction
{
    /**
     * A reference to the emulator.
     */
    private final C64Core _core;

    /**
     * The list of image names.
     */
    private byte[][] _currentEntries = null;

    /**
     * A component that simplifies loading of entries contained in image files.
     */
    public LoadComponent( C64Core core )
    {
        super( "ACT_LOAD_COMPONENT" );

        assert core != null;

        _core = core;
        setEnabled( false );
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
            // Remember the entries set.
            _currentEntries = entries;
        }
        else
        {
            _currentEntries = null;
        }

        setEnabled( _currentEntries != null );
    }

    /**
     * Handles the button press.
     *
     * @param actionEvent
     *            The associated event.
     */
    public void actionPerformed( ActionEvent actionEvent )
    {
        if ( _currentEntries == null )
            return;

        JPopupMenu popup = new JPopupMenu();

        for ( byte[] entry : _currentEntries )
            popup.add( new PopItem( entry ) );

        AbstractButton source =
            (AbstractButton)actionEvent.getSource();

        // TODO(michab) This is a workaround to prevent an exception when the
        // action is placed in a menu.
        if ( ! source.isShowing() )
            return;

        popup.show(
            source,
            0,
            source.getHeight() );
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
