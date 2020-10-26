/* $Id: RoundFrame.java 782 2015-01-05 18:05:25Z Michael $
 *
 * Mack
 *
 * Released under Gnu Public License
 * Copyright © 2011 Michael G. Binz
 */
package de.michab.swingx;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.JFrame;

import org.smack.util.MathUtil;
import org.smack.util.StringUtil;




/**
 * A round frame.  Use to pimp up your app.
 *
 * @version $Rev: 782 $
 * @author Michael Binz
 *
 * TODO convert content of the n, e, s, w cells to the adjusted raster. Keep
 * center component, this is not adjusted.
 *
 * TODO ensure that the window is actually round even in case the pixel
 * xy size is not equivalent.
 */
@SuppressWarnings("serial")
public class RoundFrame extends JFrame
{
    // TODO(michab) Clip a texture so that we have a title bar.
    /**
     * Create an instance.
     */
    public RoundFrame()
    {
        this( StringUtil.EMPTY_STRING );
    }



    /**
     * Create an instance.
     */
    public RoundFrame( String title )
    {
        super( title );

//        JXPanel contentPane = new JXPanel();
//
//        BufferedImage bi =
//                iconToImage( new MetalBumps( 20, 20, Color.GRAY, Color.BLUE, Color.DARK_GRAY ) );
//
//        ImagePainter ip = new ImagePainter( bi );
//        ip.setHorizontalRepeat( true );
//        ip.setVerticalRepeat( true );
//        contentPane.setBackgroundPainter( ip );
//        setContentPane( contentPane );
//
//        // Compute the bounds for the maximized window state.
//        Dimension screenSize =
//            Toolkit.getDefaultToolkit().getScreenSize();
//        int minDimension =
//            Math.min( screenSize.width, screenSize.height );
//        setMaximizedBounds( new Rectangle(
//                0,
//                0,
//                minDimension,
//                minDimension ) );
//
//        // The mouse listeners care for move and resize.
//        addMouseListener( _mouseListener );
//        addMouseMotionListener( _mouseListener );
//        // The window state listener handles maximize events.
//        addWindowStateListener( _windowStateListener );
//
//        setUndecorated( true );
//
//        setLayout( new RoundBorderLayout() );
//
//        setLocationRelativeTo( null );
    }



    /**
     * Lays out the component.  If the diameter is zero, then it is set to 100.
     */
    @Override
    public void pack()
    {
        super.pack();

        if ( getDiameter() == 0 )
            setDiameter( 100 );
    }



    /**
     * Get the window diameter.
     *
     * @return The window's diameter.
     */
    public int getDiameter()
    {
        return getHeight();
    }



    /**
     * Set the window's diameter.  This is a bound property.

     * @param The new diameter.
     */
    public void setDiameter( int diameter )
    {
        int oldDiameter = getDiameter();
        if ( diameter == oldDiameter )
            return;

        int oldRadius = oldDiameter / 2;
        int newRadius = diameter / 2;
        int radiusDelta = newRadius - oldRadius;

        Rectangle bounds = getBounds();

        bounds.x -= radiusDelta;
        bounds.y -= radiusDelta;
        bounds.width = diameter;
        bounds.height = diameter;

        setBounds( bounds );

        firePropertyChange( PROP_DIAMETER, oldDiameter, diameter );
    }



    /**
     * Performs additional validation.  Ensures that width and height
     * of the passed rectangle are the same.
     *
     * @throws IllegalArgumentException If width is not equal to height.
     */
    @Override
    public void setSize( int w, int h )
    {
        if ( w != h )
            throw new IllegalArgumentException( "w != h" );

        super.setSize( w, h );
    }



    /**
     * Performs additional validation.  Ensures that width and height
     * of the passed rectangle are the same.
     *
     * @throws IllegalArgumentException If width is not equal to height.
     */
    @Override
    public void setSize( Dimension d )
    {
        setSize( d.width, d.height );
    }



    /**
     * Performs additional validation.  Ensures that width and height
     * of the passed rectangle are the same.
     *
     * @throws IllegalArgumentException If width is not equal to height.
     */
    @Override
    public void setBounds( int x, int y, int width, int height )
    {
        if ( width != height )
            throw new IllegalArgumentException( "w != h" );

        Shape shape = new Ellipse2D.Float(
                0,
                0,
                width,
                height );

        setShape( shape );

        super.setBounds( x, y, width, height );
    }



    /**
     * Set the minimum diameter.
     *
     * @param minimumDiameter The minimum diameter.
     */
    public void setMinimumDiameter( int minimumDiameter )
    {
        // TODO make bound.
        setMinimumSize(
            new Dimension( minimumDiameter, minimumDiameter ) );
    }



    /**
     * Set the maximum diameter.
     *
     * @param maximumDiameter The maximum diameter.
     */
    public void setMaximumDiameter( int maximumDiameter )
    {
        // TODO make bound.
        setMaximumSize(
            new Dimension( maximumDiameter, maximumDiameter ) );
    }



    /**
     * Get the window's radius.
     *
     * @return The window's radius.
     */
    public int getRadius()
    {
        return getDiameter() / 2;
    }



    /**
     * This listener is responsible for correctly setting the window bounds
     * when the window is maximized.
     */
    private final WindowStateListener _windowStateListener  =
        new WindowStateListener()
    {
        @Override
        public void windowStateChanged( WindowEvent e )
        {
            if ( (e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH )
                setBounds( getMaximizedBounds() );

        }
    };



    private enum MouseMode { NEUTRAL, RESIZE, MOVE };

    private MouseAdapter _mouseListener = new MouseAdapter()
    {
        private static final int resizeDetection = 5;

        private Point _mousePositionDelta = null;

        /**
         * Null means that the mouse is outside of the window.  Check if this
         * helps or hinders.
         */
        private MouseMode _mouseMode = null;



        /**
         * Checks whether the passed mouse event qualifies as resize.
         *
         * @param e The event to be tested.
         * @return {@code true} if the event initiates a resize.
         */
        private boolean isResizeGesture( MouseEvent e )
        {
            int radius = getRadius();

            int distance = MathUtil.distanceInt(
                    radius,
                    radius,
                    e.getX(),
                    e.getY() );

            return distance > radius-resizeDetection && distance <= radius;
        }



        /**
         * Checks whether the passed mouse event qualifies as move.
         *
         * @param e The event to be tested.
         * @return {@code true} if the event initiates a move.
         */
        private boolean isMoveGesture( MouseEvent e )
        {
            int radius = getRadius();

            int distance = MathUtil.distanceInt(
                radius,
                radius,
                e.getX(),
                e.getY() );

            return distance < radius-resizeDetection;
        }



        /**
         *
         * @param e
         */
        private void doMove( MouseEvent e )
        {
            setLocation(
                    e.getXOnScreen() - _mousePositionDelta.x,
                    e.getYOnScreen() - _mousePositionDelta.y );
        }



        /**
         *
         * @param e
         */
        private void doResize( MouseEvent e )
        {
            int radius = getRadius();

            int newradius = MathUtil.distanceInt(
                    radius,
                    radius,
                    e.getX(),
                    e.getY() );

            setDiameter( 2 * newradius );
        }



        @Override
        public void mouseMoved( MouseEvent e )
        {
            if ( _mouseMode == null )
                return;

            if ( isResizeGesture( e ) )
            {
                RoundFrame.this.setCursor(
                        getResizeCursor( e.getPoint() ) );
            }
            else if ( isMoveGesture( e ) )
            {
                RoundFrame.this.setCursor(
                        Cursor.getPredefinedCursor( Cursor.MOVE_CURSOR ) );
            }
            else
            {
                Cursor c = Cursor.getDefaultCursor();

                if ( ! c.equals( RoundFrame.this.getCursor() ) )
                    RoundFrame.this.setCursor( c );
            }
        }

        @Override
        public void mousePressed( MouseEvent e )
        {
            if ( isResizeGesture( e ) )
                _mouseMode = MouseMode.RESIZE;
            else if ( isMoveGesture( e ) )
            {
                _mouseMode = MouseMode.MOVE;

                _mousePositionDelta = new Point( e.getLocationOnScreen() );
                _mousePositionDelta.x -= getX();
                _mousePositionDelta.y -= getY();
            }
            else
                _mouseMode = MouseMode.NEUTRAL;
        }



        @Override
        public void mouseReleased( MouseEvent e )
        {
            _mouseMode = MouseMode.NEUTRAL;
        }



        @Override
        public void mouseDragged( MouseEvent e )
        {
            if ( _mouseMode == MouseMode.MOVE )
                doMove( e );
            else if ( _mouseMode == MouseMode.RESIZE )
                doResize( e );
        }



        @Override
        public void mouseExited( MouseEvent me )
        {
            RoundFrame.this.setCursor(
                    Cursor.getDefaultCursor() );
        }



        /**
         * Compute the right cursor for the passed point relative to the
         * window center point.
         *
         * @param location
         * @return One of the eight possible resize cursors.
         */
        private Cursor getResizeCursor( Point location )
        {
            // Normalize the coordinate. (px,py) are in a coordinate system
            // with the center of the round window at (0,0) and an y-axis
            // growing upwards.
            int radius = getRadius();
            int px = location.x - radius;
            int py = radius - location.y;

            int quadrant;
            float kat;
            float hyp = MathUtil.distanceInt(
                0,
                0,
                px,
                py );

            if ( px >= 0 && py >= 0 )
            {
                quadrant = 0;
                kat = MathUtil.distanceInt(
                    0,
                    0,
                    px,
                    0 );
            }
            else if ( px >= 0 && py < 0)
            {
                quadrant = 1;
                kat = MathUtil.distanceInt(
                    0,
                    0,
                    0,
                    py );
            }
            else if ( px < 0 && py < 0)
            {
                quadrant = 2;
                kat = MathUtil.distanceInt(
                    0,
                    0,
                    px,
                    0 );
            }
            else
            {
                quadrant = 3;
                kat = MathUtil.distanceInt(
                    0,
                    0,
                    0,
                    py );
            }

            // The angle is clockwise [0..360[
            double angle = Math.toDegrees(
                    Math.asin(  kat / hyp ) ) + quadrant * 90.0;

            // Sort the location into the eight possible sectors.
            int cursor;

            if ( angle < 22.5 )
                cursor = Cursor.N_RESIZE_CURSOR;
            else if ( angle < (22.5+1*45) )
                cursor = Cursor.NE_RESIZE_CURSOR;
            else if ( angle < (22.5+2*45) )
                cursor = Cursor.E_RESIZE_CURSOR;
            else if ( angle < (22.5+3*45) )
                cursor = Cursor.SE_RESIZE_CURSOR;
            else if ( angle < (22.5+4*45 ) )
                cursor = Cursor.S_RESIZE_CURSOR;
            else if ( angle < (22.5+5*45) )
                cursor = Cursor.SW_RESIZE_CURSOR;
            else if ( angle < (22.5+6*45) )
                cursor = Cursor.W_RESIZE_CURSOR;
            else if ( angle < (22.5+7*45) )
                cursor = Cursor.NW_RESIZE_CURSOR;
            else
                cursor = Cursor.N_RESIZE_CURSOR;

            return Cursor.getPredefinedCursor(
                        cursor );
        }
    };



    public static final String PROP_DIAMETER = "diameter";
    public static final String PROP_DIAMETER_MIN = "minimumDiameter";
    public static final String PROP_DIAMETER_MAX = "maximumDiameter";

    /**
     * Move to common lib.
     *
     * @param icon
     * @return
     */
    private BufferedImage iconToImage(Icon icon)
    {
            BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
            icon.paintIcon(null, image.getGraphics(), 0, 0);
            return image;
    }
}
