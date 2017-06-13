/**
 * 
 */
package tests;

import delaunay_triangulation.Delaunay_Triangulation;

/**
 *
 */
public class ReadWriteTest {

	/**
	 * this method only tests the write_smf method integrity
	 * 
	 * @param args
	 *            Not Used
	 */
	public static void main(String[] args) {
		try {

			 String dir = "data/";
			 String file = dir+"t1-5000.tsin";
			Delaunay_Triangulation dt = new Delaunay_Triangulation(file);
			dt.write_smf(file + "_test.smf");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
