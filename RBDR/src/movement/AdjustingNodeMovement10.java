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
public class AdjustingNodeMovement10
extends MovementModel {

	public static final String AdjNo = "adjNo";

	private int adjNo;

	private Coord lastWaypoint;

	private double rx;
	private double ry;
	private double x_r1;
	private double y_r1;
	private double angle;

	//========================================================================//
	// MovementModel implementation
	//========================================================================//
	public AdjustingNodeMovement10( Settings settings ) {
		super( settings );
		int aNs[];

		aNs = settings.getCsvInts(AdjNo,1);

		this.adjNo = aNs[0];


	}

	public AdjustingNodeMovement10( AdjustingNodeMovement10 other ) {
		super( other );
		this.adjNo = other.adjNo;
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
		System.out.println("test:"+adjNo);
		rx = 440;
		ry = 845;
		
		switch(adjNo) {
		case 1:
			x_r1 = 450;
			y_r1 = 850;
			break;
		case 2:
			x_r1 = 1350;
			y_r1 = 850;
			
			break;
		case 3:
			x_r1 = 2250;
			y_r1 = 850;
			break;
		case 4:
			x_r1 = 3150;
			y_r1 = 850;
			break;
		case 5:
			x_r1 = 4050;
			y_r1 = 850;
			break;
		case 6:
			x_r1 = 450;
			y_r1 = 2550;
			break;
		case 7:
			x_r1 = 1350;
			y_r1 = 2550;
			
			break;
		case 8:
			x_r1 = 2250;
			y_r1 = 2550;
			break;
		case 9:
			x_r1 = 3150;
			y_r1 = 2550;
			break;
		case 10:
			x_r1 = 4050;
			y_r1 = 2550;
			break;
		}

		angle = 0;

		Coord c
		= new Coord(x_r1 + rx,y_r1);
		this.lastWaypoint = c;

		return c;
	}

	@Override
	public MovementModel replicate() {
		return new AdjustingNodeMovement10( this );
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
