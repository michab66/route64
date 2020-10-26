/* $Id: RoundBorderLayout.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Laboratory.
 *
 * Released under Gnu Public License
 * Copyright © 2011 Michael G. Binz
 */

package de.michab.swingx;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.EnumMap;

import org.smack.util.MathUtil;





/**
 *
 * @version $Rev: 782 $
 * @author Michael Binz
 */
public class RoundBorderLayout implements LayoutManager2
{
    public enum Position
    {
        NORTH,
        WEST,
        CENTER,
        EAST,
        SOUTH
    };



    private Position toPosition( Object o )
    {
        if ( o instanceof Position )
            return (Position)o;

        if ( o == BorderLayout.CENTER )
            return Position.CENTER;
        else if ( o == BorderLayout.NORTH )
            return Position.NORTH;
        else if ( o == BorderLayout.EAST )
            return Position.EAST;
        else if ( o == BorderLayout.SOUTH )
            return Position.SOUTH;
        else if ( o == BorderLayout.WEST )
            return Position.WEST;

        throw new RuntimeException( "" + o );
    }
    /**
     * The set of components held by the layout.
     */
    private final EnumMap<Position, Component> _components =
        new EnumMap<Position, Component>( Position.class );



    private static final Dimension ZERO_DIM =
        new Dimension();



    /**
     * Create an instance.
     */
    public RoundBorderLayout()
    {
    }



    /**
     * The minimum layout size is the same as the preferred layout size.
     */
    @Override
    public Dimension minimumLayoutSize( Container target )
    {
        return preferredLayoutSize( target );
    }



    /**
     * Determines the preferred size of the <code>target</code> container using
     * this layout manager, based on the components in the container.
     * <p>
     * Most applications do not call this method directly. This method is called
     * when a container calls its <code>getPreferredSize</code> method.
     *
     * @param target The container in which to do the layout.
     * @return the preferred dimensions to lay out the subcomponents of the
     *         specified container.
     * @see java.awt.Container
     * @see java.awt.BorderLayout#minimumLayoutSize
     * @see java.awt.Container#getPreferredSize()
     */
    @Override
    public Dimension preferredLayoutSize( Container target )
    {
        synchronized ( target.getTreeLock() )
        {
            EnumMap<Position, Dimension> psizes =
                getPreferredSizes();

            int centerHeight =
                MathUtil.max(
                        psizes.get( Position.WEST ).height,
                        psizes.get( Position.CENTER ).height,
                        psizes.get( Position.EAST ).height );
            int centerWidth =
                MathUtil.max(
                        psizes.get( Position.NORTH ).width,
                        psizes.get( Position.CENTER ).width,
                        psizes.get( Position.SOUTH ).width );

            Rectangle hRect =
                new Rectangle();

            hRect.height =
                centerHeight;
            hRect.width =
                psizes.get( Position.WEST ).width +
                centerWidth +
                psizes.get( Position.EAST ).width;
            hRect.x =
                0;
            hRect.y =
                psizes.get( Position.NORTH ).height;

            Rectangle vRect =
                new Rectangle();

            vRect.width =
                centerWidth;
            vRect.height =
                psizes.get( Position.NORTH ).height +
                centerHeight +
                psizes.get( Position.SOUTH ).height;
            vRect.x =
                psizes.get( Position.WEST ).width;
            vRect.y =
                0;

            // Compute center point.  Note that this represents the center of
            // the center component:
            Point _center = new Point();
            _center.x =
                vRect.x + (vRect.width/2);
            _center.y =
                hRect.y + (hRect.height/2);

            // Compute max distance from center to the
            // rectangle corners.
            int maxRadius =
                MathUtil.max(
                    // North
                    MathUtil.distanceInt(
                            _center,
                            new Point( vRect.x, 0 ) ),
                    MathUtil.distanceInt(
                            _center,
                            new Point( vRect.x + vRect.width, 0 ) ),
                    // East
                    MathUtil.distanceInt(
                            _center,
                            new Point( hRect.width, hRect.y ) ),
                    MathUtil.distanceInt(
                            _center,
                            new Point( hRect.width, hRect.y + hRect.height ) ),
                    // South
                    MathUtil.distanceInt(
                            _center,
                            new Point( vRect.x + vRect.width, vRect.height ) ),
                    MathUtil.distanceInt(
                            _center,
                            new Point( vRect.x, vRect.height ) ),
                    // East
                    MathUtil.distanceInt(
                            _center,
                            new Point( 0, hRect.x + hRect.height ) ),
                    MathUtil.distanceInt(
                            _center,
                            new Point( 0, hRect.x ) )
                );

            int diameter =
                maxRadius * 2;
            return new Dimension( diameter, diameter );
        }
    }



    private int pythagoras( int hypotenuse, int a )
    {
        if ( hypotenuse > a )
            return MathUtil.pythagoras(
                    hypotenuse,
                    a );

        return 0;
    }



   /**
    *
    */
   @Override
public final void layoutContainer( Container target )
   {
       synchronized ( target.getTreeLock() )
       {
           EnumMap<Position, Dimension> psizes =
               getPreferredSizes();

           int centerHeight =
               MathUtil.max(
                       psizes.get( Position.WEST ).height,
                       psizes.get( Position.CENTER ).height,
                       psizes.get( Position.EAST ).height );
           int centerWidth =
               MathUtil.max(
                       psizes.get( Position.NORTH ).width,
                       psizes.get( Position.CENTER ).width,
                       psizes.get( Position.SOUTH ).width );

           Rectangle hRect =
               new Rectangle();

           hRect.height =
               centerHeight;
           hRect.width =
               psizes.get( Position.WEST ).width +
               centerWidth +
               psizes.get( Position.EAST ).width;
           hRect.x =
               0;
           hRect.y =
               psizes.get( Position.NORTH ).height;

           Rectangle vRect =
               new Rectangle();

           vRect.width =
               centerWidth;
           vRect.height =
               psizes.get( Position.NORTH ).height +
               centerHeight +
               psizes.get( Position.SOUTH ).height;
           vRect.x =
               psizes.get( Position.WEST ).width;
           vRect.y =
               0;

           // Compute center point.
           Point center = new Point(
                target.getWidth() / 2,
                target.getHeight() / 2 );
           int radius = center.x;

           Rectangle r =
               new Rectangle();

           if ( _components.containsKey( Position.NORTH ) )
           {
               Dimension nDim = psizes.get( Position.NORTH );

               r.x = radius - (nDim.width / 2);
               r.y = radius - centerHeight/2 - nDim.height;
               r.width = nDim.width;
               r.height = nDim.height;

               _components.get( Position.NORTH ).setBounds( r );
           }

           if ( _components.containsKey( Position.SOUTH ) )
           {
               Dimension sDim = psizes.get( Position.SOUTH );

               int y = pythagoras(
                           radius,
                           sDim.width / 2 );

               r.x = radius - (sDim.width / 2);
               r.y = radius + centerHeight/2;
               r.width = sDim.width;
               r.height = sDim.height;

               _components.get( Position.SOUTH ).setBounds( r );
           }

           if ( _components.containsKey( Position.WEST ) )
           {
               Dimension wDim = psizes.get( Position.WEST );

               int x = pythagoras(
                           radius,
                           wDim.height / 2 );

               r.x = radius - x;
               r.y = radius - (wDim.height/2);
               r.width = wDim.width;
               r.height = wDim.height;

               _components.get( Position.WEST ).setBounds( r );
           }

           if ( _components.containsKey( Position.EAST ) )
           {
               Dimension eDim = psizes.get( Position.EAST );

               int x = pythagoras(
                           radius,
                           eDim.height / 2 );

               r.x = radius + x - eDim.width;
               r.y = radius - (eDim.height/2);
               r.width = eDim.width;
               r.height = eDim.height;

               _components.get( Position.EAST ).setBounds( r );
           }

           if ( _components.containsKey( Position.CENTER ) )
           {
               Dimension cDim = psizes.get( Position.CENTER );

               r.x = radius - (cDim.width/2);
               r.y = radius - (cDim.height/2);
               r.width = cDim.width;
               r.height = cDim.height;

               _components.get( Position.CENTER ).setBounds( r );
           }
       }
   }



    /**
     * Create a map that holds the preferred sizes of all components.
     * For missing components the maps holds a [0/0] preferred size
     * to prevent the need for special cases.
     *
     * @return The newly created map.  Never {@code null}.
     */
    private EnumMap<Position, Dimension> getPreferredSizes()
    {
        EnumMap<Position, Dimension> result =
            new EnumMap<Position,Dimension>( Position.class );

        for ( Position c : Position.values() )
        {
            Dimension cDim =
                ZERO_DIM;
            Component comp =
                _components.get( c );
            if ( comp != null )
                cDim = comp.getPreferredSize();
            result.put(
                c,
                cDim );
        }

        return result;
    }



    /* (non-Javadoc)
     * @see java.awt.LayoutManager#addLayoutComponent(java.lang.String, java.awt.Component)
     */
    @Override
    public final void addLayoutComponent( String name, Component comp )
    {
        _components.put(
            Position.valueOf( name ),
            comp );
    }



    /* (non-Javadoc)
     * @see java.awt.LayoutManager#removeLayoutComponent(java.awt.Component)
     */
    @Override
    public void removeLayoutComponent( Component comp )
    {
        for ( Position c : Position.values() )
        {
            if ( _components.get( c ).equals(  comp ) );
                _components.put( c, null );
        }
    }



    /**
     * Add a component.
     *
     * @param comp The component to add.
     * @param constraints One of the {@link Position} constants.
     */
    @Override
    public final void addLayoutComponent( Component comp, Object constraints )
    {
        Position p = toPosition( constraints );
        _components.put(
                p,
                comp );
    }



    /* (non-Javadoc)
     * @see java.awt.LayoutManager2#maximumLayoutSize(java.awt.Container)
     */
    @Override
    public Dimension maximumLayoutSize( Container target )
    {
        return new Dimension( Integer.MAX_VALUE, Integer.MAX_VALUE );
    }



    /* (non-Javadoc)
     * @see java.awt.LayoutManager2#getLayoutAlignmentX(java.awt.Container)
     */
    @Override
    public final float getLayoutAlignmentX( Container target )
    {
        // Centered.
        return 0.5f;
    }



    /* (non-Javadoc)
     * @see java.awt.LayoutManager2#getLayoutAlignmentY(java.awt.Container)
     */
    @Override
    public final float getLayoutAlignmentY( Container target )
    {
        // Centered.
        return 0.5f;
    }



    /* (non-Javadoc)
     * @see java.awt.LayoutManager2#invalidateLayout(java.awt.Container)
     */
    @Override
    public void invalidateLayout( Container target )
    {
        // Release resources.
    }
}
