package movement;

import core.Coord;
import core.Settings;

/**
 * <p>
 * Adjusting Node's movement model as described in:
 * Kazuya MATSUTANI.
 * </p>
 *
 * <p>
 * Nodes will start at a assigned place on the simulation area and move around
 * assigned area.
 *
 * @author teemuk
 */
public class AdjustingNodeMovement
extends MovementModel {

	public static final String NrofAdj = "nrofHosts";
	
	private int n;
	
    private Coord lastWaypoint;
    
    private double rx;
    private double ry;
    private double x_r1;
    private double y_r1;
    private double angle;
    
    //========================================================================//
    // MovementModel implementation
    //========================================================================//
    public AdjustingNodeMovement( Settings settings ) {
        super( settings );
		int ns[];
	
		ns = settings.getCsvInts(NrofAdj, 1);
		this.n = ns[0];
		System.out.println("---------------------------------------------------");	
		System.out.println(this.n+" adjusting Nodes are here.");
		System.out.println("---------------------------------------------------");	
		
    }

    public AdjustingNodeMovement( AdjustingNodeMovement other ) {
        super( other );
        this.n = other.n;
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path( super.generateSpeed() );
        p.addWaypoint( this.lastWaypoint.clone() );
        
        Coord next = this.getWayPoint( this.lastWaypoint.getX(),
                                             this.lastWaypoint.getY() );
        p.addWaypoint( next );

        this.lastWaypoint = next;

        return p;
    }

    @Override
    public Coord getInitialLocation() {
    	rx = 2245;
    	ry = 1695;
    	x_r1 = 2250;
    	y_r1 = 1700;
    	angle = 0;
    	
        Coord c
            = new Coord(x_r1 + rx,y_r1);
        this.lastWaypoint = c;

        return c;
    }

    @Override
    public MovementModel replicate() {
        return new AdjustingNodeMovement( this );
    }
    //========================================================================//


    //========================================================================//
    // Sub-classable
    //========================================================================//
    /**
     * Returns the fraction of the path to follow towards the edge. This is
     * called after the direction of travel has been picked and the path
     * towards the edge calculated. Returning {@code 1.0} causes the node to
     * travel all the way to the edge, returning {@code 0.5} would cause the
     * node to travel half way, etc.
     *
     * @return  the fraction of the path to follow towards the edge of the
     *          simulation area
     */
    protected double getTravelFraction() {
        return 1.0;
    }
    //========================================================================//

    private Coord getWayPoint( double x0, double y0 ) {

        double x = x_r1 + rx*Math.cos(Math.PI/180*angle);
        double y = y_r1 + ry*Math.sin(Math.PI/180*angle);
        angle += 5;
        if(angle > 360) {
        	angle = 0;
        }
 
        return new Coord( x, y );
    }
}
