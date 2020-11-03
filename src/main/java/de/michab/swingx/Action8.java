/* $Id$
 *
 * Unpublished work.
 * Copyright © 2015 Michael G. Binz
 */
package de.michab.swingx;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import org.jdesktop.util.ResourceManager;
import org.jdesktop.util.ResourceMap;
import org.jdesktop.util.ServiceManager;

/**
* An action taking a method reference as the action delegate.
*
* @version $Rev$
* @author Michael Binz
*/
public class Action8 extends AbstractActionExt
{
    private final Consumer<ActionEvent> _consumer;

    /**
     * Create an instance.
     *
     * @param action A reference to a method 'void method( ActionEvent )'.
     */
    public Action8( Consumer<ActionEvent> action )
    {
        _consumer = action;
    }

    public Action8()
    {
        _consumer = this::actionPerformed;
    }
    /**
     * Create an instance.
     *
     * @param action A reference to a method 'void method()'.
     */
    public Action8( Runnable action )
    {
        _consumer = (s) -> action.run();
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
        _consumer.accept( e );
    }

    /**
     * Injects the action properties of the action's class, i.e.
     * {@link AbstractActionExt}.
     *
     * @param app The application reference.
     * @param context The home class of the action that is used to access the
     * resources.
     * @param key A prefix used to identify the resource keys used for property
     * injection.
     * @return A reference to the action for call chaining.
     */
    public Action8 inject( Class<?> context, String key )
    {
        ResourceManager rm =
                ServiceManager.getApplicationService( ResourceManager.class );
        ResourceMap map =
                rm.getResourceMap( context );
        rm.injectProperties(
                this,
                key + ".Action",
                map );
        return this;
    }

    /**
     * Injects this action's JavaBean properties. The property values are
     * resolved in the ResourceMap of the passed context class.
     *
     * @param app The application reference.
     * @param context The home class of the action that is used to access the
     * resources.
     * @param key A prefix used to identify the resource keys used for property
     * injection.
     * @return A reference to the action for call chaining.
     */
    public Action8 init( Class<?> context, String key )
    {
        org.jdesktop.util.ResourceManager rm =
                ServiceManager.getApplicationService( org.jdesktop.util.ResourceManager.class );
        org.jdesktop.util.ResourceMap map =
                rm.getResourceMap( context );

        String resourceKey = String.format(
                "%s.%s.Action",
                context.getSimpleName(),
                key );

        rm.injectProperties(
                this,
                resourceKey,
                map );
        return this;
    }

    /**
     * Generated for Action8.java.
     */
    private static final long serialVersionUID = -7056521329585878347L;
}
