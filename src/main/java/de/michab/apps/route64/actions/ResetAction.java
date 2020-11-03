/* $Id: ResetAction.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Route64.
 *
 * Released under Gnu Public License
 * Copyright © 2010 Michael G. Binz
 */
package de.michab.apps.route64.actions;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;

import de.michab.simulator.mos6502.c64.C64Core;

/**
 * Reset the emulation.
 *
 * @version $Rev: 782 $
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class ResetAction extends AbstractAction
{
    private final C64Core _target;

    public ResetAction( C64Core target )
    {
        super( "Reset" );
//        super( "ACT_RESET" );

        _target = Objects.requireNonNull( target );
    }

    /**
     * Reset the emulation and set focus back to the emulator display.
     */
    @Override
    public void actionPerformed( ActionEvent ae )
    {
        _target.reset( true );

        _target.getDisplay().requestFocusInWindow();
    }
}
