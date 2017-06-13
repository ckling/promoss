package tests;

import java.util.Iterator;
import delaunay_triangulation.*;

/**
 * This class demonstrates a simple usage of the 
 * JDT Triangulation process.
 */
public class SimpleTriangulationUsage {

	public static void main(String[] args) {
		Delaunay_Triangulation dt = new Delaunay_Triangulation();
		Point_dt pointA = new Point_dt(0, 1, 2);
		Point_dt pointB = new Point_dt(2, 0, 3);
		Point_dt pointC = new Point_dt(2, 2, 4);
		Point_dt pointD = new Point_dt(4, 1, 5);
		Point_dt pointE = new Point_dt(5, 3, 5);
		dt.insertPoint(pointA);
		dt.insertPoint(pointB);
		dt.insertPoint(pointC);
		dt.insertPoint(pointD);
		dt.insertPoint(pointE);

		Iterator<Triangle_dt> iterator = dt.trianglesIterator();

		while (iterator.hasNext()) {
			Triangle_dt curr = iterator.next();
			if (!curr.isHalfplane()) {
				System.out.println(curr.p1() + ", " + curr.p2() + ", "
						+ curr.p3());
			}
		}
	}

}
