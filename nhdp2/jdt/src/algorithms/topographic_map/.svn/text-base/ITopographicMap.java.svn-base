package algorithms.topographic_map;
import java.util.ArrayList;
import java.util.Iterator;
import delaunay_triangulation.Triangle_dt;



/**
 * A service that creates a topographic map from list of Delaunay triangles.
 * @version 1.0 12 December 2009
 * @author Omri Gutman
 *
 */
public interface ITopographicMap {	
	
	
	/**
	 * Returns a list of counter line that represents a topographic map. 
	 * @param triangles - a list of triangles that was created by Delaunay triangulation algorithm.
	 * @param heightDelta - each counter line will be in the height x*heightDelta
	 * @return a list of counters lines. Each counter line is in the height of x*heightDelta
	 * @see CounterLine
	 * @see Triangle_dt
	 */
	public ArrayList<CounterLine> createCounterLines(Iterator<Triangle_dt> triangles, int heightDelta);
}