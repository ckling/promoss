package tests;

import delaunay_triangulation.*;

/**
 * This class demonstrates a simple usage of the 
 * JDT Find process.
 */
public class SimpleFindUsage {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Delaunay_Triangulation dt = new Delaunay_Triangulation();
		Point_dt pointA = new Point_dt(0, 0);
		Point_dt pointB = new Point_dt(2, 2);
		Point_dt pointC = new Point_dt(4, 0);

		dt.insertPoint(pointA);
		dt.insertPoint(pointB);
		dt.insertPoint(pointC);

		Point_dt pointX = new Point_dt(1.3, 1);
		Point_dt pointY = new Point_dt(4, 2);
		
		FindAndPrint(dt, pointX);
		FindAndPrint(dt, pointY);
	}

	private static void FindAndPrint(Delaunay_Triangulation triangulation,
			Point_dt point) {
		Triangle_dt triangle = triangulation.find(point);
		if (triangle.isHalfplane()){
			System.out.println("Point " + point + " is not in the triangle.");
		} else {
			System.out.println("Point " + point + " is in the triangle.");
		}
	}

}
