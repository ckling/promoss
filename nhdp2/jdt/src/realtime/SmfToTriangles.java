package realtime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

/**
 * This class is responsible converting the data from a .smf file
 * into an array to Triangle_dt objects, that represents a triangulation.
 * 
 * @author NS
 *
 */
public class SmfToTriangles {
	
	/**
	 * Constructor.
	 */
	public SmfToTriangles() {
		// Empty constructor.
	}
	
	/**
	 * Extracts an array of triangles out of a .smf file.
	 * 
	 * @param smfFile The name of the input .smf file.
	 * @return An array of triangles representing the triangulation.
	 * @throws Exception
	 */

	public static Vector<Triangle_dt> getTriangles(String smfFile) throws Exception {
		BufferedReader is = new BufferedReader(new FileReader(smfFile));
		
		Vector<Point_dt> points = new Vector<Point_dt>();
		Vector<Triangle_dt> triangles = new Vector<Triangle_dt>();
		
		String s = is.readLine();

		 // Loop over all the points in the file.
		while (s.charAt(0) == 'v') {
			points.add(getPoint(s));
			s = is.readLine();
		}
		
		 // Loop over all the triangles in the file.
		while (s != null) {
			triangles.add(getTriangle(s, points));
			s = is.readLine();
		}
		
		is.close();
		
		return triangles;
	}
	
	/**
	 * Extracts a Point_dt object from a line taken from the smf file.
	 * 
	 * @param currentLine The current line.
	 * @return A Point_dt object representing the point.
	 */
	private static Point_dt getPoint(String currentLine) {
		String[] arr = currentLine.split(" ");
		
		double x = Double.valueOf(arr[1]).doubleValue();
		double y = Double.valueOf(arr[2]).doubleValue();
		double z = Double.valueOf(arr[3]).doubleValue();
		
		return new Point_dt(x, y, z);
	}
	
	/**
	 * Extracts a Triangle_dt object from a line taken from the smf file.
	 * 
	 * @param currentLine The current line.
	 * @param points The array of points extracted from the smf file earlier.
	 * @return A Triangle_dt object representing the triangle.
	 */
	private static Triangle_dt getTriangle(String currentLine, 
			Vector<Point_dt> points) {
		String[] arr = currentLine.split(" ");
		
		int firstIndex = Integer.valueOf(arr[1]).intValue() - 1;
		int secondIndex = Integer.valueOf(arr[2]).intValue() - 1;
		int thirdIndex = Integer.valueOf(arr[3]).intValue() - 1;
		
		return new Triangle_dt(points.get(firstIndex),
				points.get(secondIndex), points.get(thirdIndex));
	}
}
