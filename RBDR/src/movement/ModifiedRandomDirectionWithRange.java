package movement;

import core.Coord;
import core.Settings;

/**
 * <p>
 * Modified Random Direction movement model as described in:
 * Elizabeth M. Royer, P. Michael Melliar-Smith, and Louise E. Moser,
 * "An Analysis of the Optimum Node Density for Ad hoc Mobile Networks"
 * </p>
 *
 * <p>
 * Similar to {@link RandomDirection}, except nodes will not move all the way
 * to the edge. Instead they will pick a random direction and move in that
 * direction for a random distance before pausing and picking another
 * direction.
 * </p>
 *
 * @author teemuk
 */
public class ModifiedRandomDirectionWithRange
extends RandomDirectionWithRange {
	public static final String LOCATION_S = "nodeLocation";

	private Coord loc; /** The location of the nodes */

    public ModifiedRandomDirectionWithRange( Settings settings ) {
        super( settings );
        int coords[];

        coords = settings.getCsvInts(LOCATION_S,2);
        this.loc = new Coord(coords[0],coords[1]);   
    }

    public ModifiedRandomDirectionWithRange( ModifiedRandomDirectionWithRange other ) {
        super( other );
        this.loc = other.loc;
    }
    
	@Override
	public Coord getInitialLocation() {
        Coord c
        = new Coord( MovementModel.rng.nextDouble() * loc.getX(),
                     MovementModel.rng.nextDouble() * loc.getY());
    setLastWaypoint(c);
    return c;
	}

    @Override
    protected double getTravelFraction() {
        // Move a random fraction in the picked direction instead of all the
        // way to the edge.
        return MovementModel.rng.nextDouble();
    }
    
    @Override
    public MovementModel replicate() {
        return new ModifiedRandomDirectionWithRange( this );
    }


}
