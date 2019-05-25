/* $Id: RestoreSizeAction.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Route64.
 *
 * Released under Gnu Public License
 * Copyright © 2010 Michael G. Binz
 */

package de.michab.apps.route64.actions;

import java.awt.event.ActionEvent;

import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.smack.MackAction;


/**
 *
 * @version $Rev: 782 $
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class RestoreSizeAction extends MackAction
{
    private SingleFrameApplication _application;

    /**
     * @param key
     */
    public RestoreSizeAction( String key, SingleFrameApplication app )
    {
        super( key );

        _application = app;
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed( ActionEvent e )
    {
        _application.getMainFrame().pack();
    }
}
