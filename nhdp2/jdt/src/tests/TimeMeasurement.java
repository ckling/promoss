package tests;

import java.util.Date;

import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

public class TimeMeasurement {

	/**
	 * @param args
	 *            Not Used
	 */
	public static void main(String[] args) {
		int size = 100000, size2 = size;
		double delta = 1000, delta2 = delta / 2;
		double[] xx = new double[size], yy = new double[size];
		Point_dt[] ps = new Point_dt[size];
		double[] xx2 = new double[size2], yy2 = new double[size2];

		long start = new Date().getTime();
		Delaunay_Triangulation ad = new Delaunay_Triangulation();

		for (int i = 0; i < size; i++) {
			xx[i] = (Math.random() * delta - (delta * 0.1));
			yy[i] = (Math.random() * delta - (delta * 0.1));

			ps[i] = new Point_dt(xx[i], yy[i]);
			ad.insertPoint(ps[i]);
		}
		long mid = new Date().getTime();

		for (int i = 0; i < size2; i++) {
			xx2[i] = (Math.random() * delta2);
			yy2[i] = (Math.random() * delta2);
		}
		long m1 = new Date().getTime();
		for (int i = 0; i < size2; i++) {
			Point_dt p = new Point_dt(xx2[i], yy2[i]);
			Triangle_dt t1 = ad.find(p);
			if (!t1.contains(p)) {
				System.out.println(i + ") **ERR: find *** T: " + t1);
			}
		}
		long e1 = new Date().getTime();

		System.out.println("delaunay_triangulation " + ad.size() + " points, "
				+ ad.trianglesSize() + " triangles,  Triangles_td: "
				+ Triangle_dt._counter + "  ,c2: " + Triangle_dt._c2);
		System.out.println("Constructing time: " + (mid - start));
		System.out.println("*** E3 find:  time: " + (e1 - m1));
		System.out.println("delaunay_triangulation " + ad.size() + " points, "
				+ ad.trianglesSize() + " triangles,  Triangles_td: "
				+ Triangle_dt._counter + "  ,c2: " + Triangle_dt._c2);
	}
}
