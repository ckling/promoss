package tests;

import java.util.Vector;

import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

public class MemoryUsage {

	/**
	 * this method tests the number of Triangle_dt can be constructed and
	 * inserted to a vector, before a java.lang.OutOfMemoryError is thrown
	 * 
	 * @param args
	 *            Not Used.
	 */
	public static void main(String[] args) {
		int i = 0;
		try {
			Point_dt p1 = new Point_dt(1, 1);
			Point_dt p3 = new Point_dt(1, 0);
			Point_dt p2 = new Point_dt(0, 0);
			Vector<Triangle_dt> vec = new Vector<Triangle_dt>();
			while (true) {
				Triangle_dt t = new Triangle_dt(p1, p2, p3);
				vec.add(t);
				i++;
				if (i % 10000 == 0)
					System.out.println(i);
			}
		} catch (Exception e) {
			System.out.println("out of MEMORY: points: " + i);
		} catch (OutOfMemoryError oome) {
			System.out.println("out of MEMORY: points: " + i);
		}
	}
}
