package tests;

import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

import java.util.Iterator;

import algorithms.guards.GuardsAlgorithm;

/**
 * Created by IntelliJ IDEA.
 * User: Lior Talker
 * Date: 09/12/2009
 * Time: 21:02:10
 * To change this template use File | Settings | File Templates.
 */
public class GuardsAlgorithmUsage {

    public static void main(String[] args) {
		Delaunay_Triangulation dt = new Delaunay_Triangulation();
		Point_dt pointA = new Point_dt(0, 1,2);
		Point_dt pointB = new Point_dt(2, 0,8);
		Point_dt pointC = new Point_dt(2, 2,6);
		Point_dt pointD = new Point_dt(3, 1,4);
        Point_dt pointE = new Point_dt(2,1,5);

		dt.insertPoint(pointA);
		dt.insertPoint(pointB);
		dt.insertPoint(pointC);
		dt.insertPoint(pointD);
        dt.insertPoint(pointE);

        Point_dt guardA = new Point_dt(1.5, 1.5,dt.z(1.5, 1.5));
		Point_dt guardB = new Point_dt(2.9,0.9, dt.z(2.9,0.9));
		Point_dt diamondA = new Point_dt(0.9, 1.2, dt.z(0.9, 1.2));
		Point_dt diamondB = new Point_dt(0.9, 1,dt.z(0.9,1));
        Point_dt diamondC = new Point_dt(1.7, 1.2,dt.z(1.7, 1.2));

        Point_dt guardsArr[] = new Point_dt[2];
        Point_dt diamondArr[] = new Point_dt[3];

        guardsArr[0] = guardA;
        guardsArr[1] = guardB;
        diamondArr[0] = diamondA;
        diamondArr[1] = diamondB;
        diamondArr[2] = diamondC;


        GuardsAlgorithm guardsAlgorithm= new GuardsAlgorithm();
        int guardsIndices [] = guardsAlgorithm.selectGuardsToWatchDiamonds(dt,guardsArr,diamondArr);

        System.out.print("this is the selected guards indices: ");
        for (int i=0 ; i < guardsIndices.length; i++) {
            System.out.print(guardsIndices[i]);
            System.out.print(" ");
        }
	}
}
