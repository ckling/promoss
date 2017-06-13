package tests;

import delaunay_triangulation.*;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Aviad Segev
 * Date: 22/11/2009
 * Time: 22:14:42
 *
 * A Test class used to test the performance of using a spatial grid index.
 */
public class GridIndexMeasurement
{
	static final int FIND_COUNT = 100000;
	static final int INDEX_SIZE = 169;
	static final int HORIZONTAL_CELL_COUNT = (int)Math.sqrt(INDEX_SIZE);
	static final int VERTICAL_CELL_COUNT =  (int)Math.sqrt(INDEX_SIZE);

	public static void main(String[] args) {
		try {

			// Load a triangulation
			String dir = "data/";
			String file = dir+"terra_13000.tsin";
			Delaunay_Triangulation dt = new Delaunay_Triangulation(file);

			// Prepre search points
			BoundingBox dt_box = dt.getBoundingBox();
			Point_dt[] searchedPoints = new Point_dt[FIND_COUNT];

			for (int i = 0; i < FIND_COUNT; i++) {
				searchedPoints[i] = new Point_dt(dt_box.minX() + Math.random() *  dt_box.getWidth(),
						dt_box.minY() + Math.random() * dt_box.getHeight());
			}

			// Perform regular point location search
			long regualrFindStartTime = new Date().getTime();

			for (int i = 0; i < FIND_COUNT; i++) {
				Point_dt p = searchedPoints[i];
				Triangle_dt t1 = dt.find(p);
				if (!t1.contains(p)) {
					System.out.println(i + ") **ERR: regular find *** T: " + t1);
				}
			}

			long regularFindEndTime = new Date().getTime();

			// compute grid index
			dt.IndexData(HORIZONTAL_CELL_COUNT, VERTICAL_CELL_COUNT);

			long indexBuildEndTime = new Date().getTime();

			// Perform indexed point location search
			for (int i = 0; i < FIND_COUNT; i++) {
				Point_dt p = searchedPoints[i];
				Triangle_dt t1 = dt.find(p);
				if (!t1.contains(p)) {
					System.out.println(i + ") **ERR: indexed find *** T: " + t1);
				}
			}
			long indexedFindEndTime = new Date().getTime();

			System.out.println("Regular search took: " + (regularFindEndTime - regualrFindStartTime));
			System.out.println("Index building took: " + (indexBuildEndTime - regularFindEndTime));
			System.out.println("Indexed search took: " + (indexedFindEndTime - indexBuildEndTime));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
