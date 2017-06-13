package algorithms.topographic_map;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;

import delaunay_triangulation.Triangle_dt;

public class TriangleCrossingLinesCalculator {
	
	
	/**
	 * Computes all the lines that cross the triangle in the heights dz * k 
	 * @param t
	 * @param dz
	 * @return
	 */
	public static ArrayList<Line_tp> getLines(Triangle_dt t, double dz) {

		Point_tp p1 = new Point_tp(t.p1());
		Point_tp p2 = new Point_tp(t.p2());
		Point_tp p3 = new Point_tp(t.p3());
		
		
		

		ArrayList<Line_tp> lines = new ArrayList<Line_tp>();
		if(p1.getZ() == p2.getZ() && p1.getZ() % dz == 0)
		{
			Line_tp  line = new Line_tp(p1, p2, p2.getZ());
			lines.add(line);
		}
		if(p3.getZ() == p2.getZ() && p3.getZ() % dz == 0)
		{
			Line_tp  line = new Line_tp(p3, p2, p2.getZ());
			lines.add(line);
		}
		if(p1.getZ() == p3.getZ() && p3.getZ() % dz == 0)
		{	
			Line_tp  line = new Line_tp(p1, p3, p3.getZ());
			lines.add(line);
			
		}
		
		

		Point_tp[] p12 = computePoints(p1, p2, dz);
		Point_tp[] p23 = computePoints(p2, p3, dz);
		Point_tp[] p31 = computePoints(p3, p1, dz);

		int i12 = 0, i23 = 0, i31 = 0;
		boolean cont = true;
		while (cont) {
			cont = false;
			if(i23 < p23.length && p1.getZ() == p23[i23].getZ())
			{
				Line_tp  line = new Line_tp(p1, p23[i23], p23[i23].getZ());
				lines.add(line);
				i23++;
				cont = true;
			}
			if(i31 < p31.length && p2.getZ() == p31[i31].getZ()){
				Line_tp  line = new Line_tp(p2, p31[i31], p31[i31].getZ());
				lines.add(line);
				i31++;
				cont = true;
			}
			if(i12 < p12.length && p3.getZ() == p12[i12].getZ()){
				Line_tp  line = new Line_tp(p3, p12[i12], p12[i12].getZ());
				lines.add(line);
				i12++;
				cont = true;
			}
			if (i12 < p12.length && i23 < p23.length
					&& p12[i12].getZ() == p23[i23].getZ()) {
				if(p12[i12].compareTo(p23[i23]) != 0)
				{	
					Line_tp line = new Line_tp(p12[i12],p23[i23],p12[i12].getZ());
					lines.add(line);
				}
				i12++;
				i23++;
				cont = true;
			}
			if (i23 < p23.length && i31 < p31.length
					&& p23[i23].getZ() == p31[i31].getZ()) {
				if(p23[i23].compareTo(p31[i31]) != 0){
					Line_tp line = new Line_tp(p23[i23],p31[i31],p23[i23].getZ());
					lines.add(line);
				}
				i23++;
				i31++;
				cont = true;
			}
			if (i12 < p12.length && i31 < p31.length
					&& p12[i12].getZ() == p31[i31].getZ()) {
				if(p12[i12].compareTo(p31[i31]) != 0)
				{	
					Line_tp line = new Line_tp(p12[i12],p31[i31],p12[i12].getZ());
					lines.add(line);
				}
				i12++;
				i31++;
				cont = true;
			}
		}
		if(i12 < p12.length || i23 < p23.length || i31 < p31.length){
			System.out.println("problem");

		}
		return lines;
	}

	/**
	 * Computes all the points in the height dz * k when dz * k is between p1.getZ() and p2.getZ() 
	 * excluding p1.getZ() and p2.getZ();
	 */
	private static Point_tp[] computePoints(Point_tp p1, Point_tp p2, double dz) {
		Point_tp[] ans = new Point_tp[0];
		double z1 = Math.min(p1.getZ(), p2.getZ()), z2 = Math.max(p1.getZ(), p2.getZ());
		if (z1 == z2)
			return ans;
		double k;
		if(z1 % dz == 0){
			k = z1 / dz + 1;
		}
		else{
			k = Math.ceil(z1 / dz);
		}
		double zz1 = k * dz;
		if(z2 % dz == 0){
			k = z2 / dz - 1;
		}
		else{
			k = Math.floor(z2 / dz);
		}
		double zz2 = k * dz;
		int len = (int) ((zz2 - zz1) / dz) + 1, i = 0;
		ans = new Point_tp[len];
		double DZ = p2.getZ() - p1.getZ();
		MathContext context = new MathContext(10,RoundingMode.HALF_EVEN);
		BigDecimal DX = p2.getX().subtract(p1.getX());
		BigDecimal DY = p2.getY().subtract(p1.getY());
		//Big Decimal decleretions:
		BigDecimal bigDecP1z = new BigDecimal(p1.getZ());
		BigDecimal bigDecDZ = new BigDecimal(DZ);
		for (double z = zz1; z <= zz2; z += dz) {
			BigDecimal bigDecZ = new BigDecimal(z);
			BigDecimal scale = bigDecZ.subtract(bigDecP1z).divide(bigDecDZ,context);
			BigDecimal x = DX.multiply(scale).add(p1.getX(),context);
			BigDecimal y = DY.multiply(scale).add(p1.getY(),context);	
			ans[i] = new Point_tp(x, y, z);
			i++;

		}
		return ans;
	}

}
