/* $Id: InputDeviceAction.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Route64.
 *
 * Released under Gnu Public License
 * Copyright © 2010 Michael G. Binz
 */

package de.michab.apps.route64.actions;

import java.awt.event.ActionEvent;

import org.jdesktop.smack.MackAction;

import de.michab.simulator.mos6502.c64.C64Core;

/**
 * The action used for the input device selections.  Input devices are
 * the joysticks and the keyboard.
 *
 * @version $Rev: 782 $
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class InputDeviceAction extends MackAction
{
    private final C64Core _home;

    private final C64Core.InputDevice _device;

    /**
     *
     * @param key The action key.
     * @param home A reference to the emulator bean.
     * @param device The device selector.
     */
    public InputDeviceAction(
            String key,
            C64Core home,
            C64Core.InputDevice device,
            boolean isSelected )
    {
        super( key );

        _device = device;
        _home = home;

        setStateAction();
        setSelected( isSelected );
        setGroup( getClass().getSimpleName() );
    }



    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed( ActionEvent e )
    {
        _home.setInputDevice( _device );
    }
}
