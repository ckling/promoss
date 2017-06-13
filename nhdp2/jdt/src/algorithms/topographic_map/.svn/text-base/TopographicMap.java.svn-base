package algorithms.topographic_map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import delaunay_triangulation.Triangle_dt;

/**
 *	Creates a topographic map from list of Delaunay triangles.
 *	
 * @version 1.0 19 December 2009
 * @author Omri Gutman
 */	
public class TopographicMap implements ITopographicMap {


	/** 
	 * Creates a counter lines list from a list of Delaunay triangles:
	 * <br>
	 * 1. Creates a list of the triangles crossing lines. i.e all the lines that
	 * connects triangle edges in the height k * heightDelta. 
	 * <br>
	 * 2. Insert all the points (crossing line points) into an hash map: the hash map maps between 
	 * the point and an object that represent the lines that connected to this point.
	 * <br>
	 * 3. Creates CounterLines list.for each untouched point from the hash map do:
	 * <br>
	 * 3.a. Create the path that starting at this point (using the crossing lines and the hash map data structure).
	 * <br>
	 * 3.b. Add the created path to the counter lines list.
	 * <br>
	 * Complexity:
	 * <br>
	 * Let say that the number of triangles is N and the number of counter lines that cross an triangle is K by average.
	 * The time complexity is O(N*K):
	 * <br>
	 * @param triangles
	 * @param heightDelta
	 * @see ITopographicMap#createCounterLines(Iterator, int)
	 * @see Triangle_dt
	 * @see CounterLine
	 */
	@Override
	public ArrayList<CounterLine> createCounterLines(
			Iterator<Triangle_dt> triangles, int heightDelta){
		try{
			ArrayList<CounterLine> counterLines = new ArrayList<CounterLine>();
			ArrayList<Line_tp> lines = new ArrayList<Line_tp>();
			while(triangles.hasNext()){
				Triangle_dt triangle = triangles.next();
				if(!triangle.isHalfplane()){
					lines.addAll(TriangleCrossingLinesCalculator.getLines(triangle, heightDelta));	
				}
			}
			HashMap<Point_tp,PointEdges_tp> pointsHash = new HashMap<Point_tp,PointEdges_tp>(lines.size());
			ArrayList<Point_tp> points = new ArrayList<Point_tp>(lines.size());	

			for(Line_tp line : lines){
				Point_tp pa = line.getP1();
				Point_tp pb = line.getP2();

				PointEdges_tp pea = pointsHash.get(pa);
				if(pea == null){
					pea = new PointEdges_tp(pa);
					pointsHash.put(pa, pea);
					points.add(pa);
				}
				pea.addLine(line);
				PointEdges_tp pe2 = pointsHash.get(pb);
				if(pe2 == null){
					pe2 = new PointEdges_tp(pb);
					pointsHash.put(pb, pe2);
					points.add(pb);
				}
				pe2.addLine(line);	
			}
			for(Point_tp key : points){
				PointEdges_tp pointEdges = pointsHash.get(key);
				if(!pointEdges.isTouched()){
					counterLines.add(generateCounterLine(pointsHash, pointEdges));
				}
			}
			return counterLines;
		}
		catch(Exception e){
			System.out.println("Error. Can't calculate topography map counter line for the triangulation");
			e.printStackTrace();
			return null;

		}
	}

	/**
	 * Generates the counter line that starts from firstPointEdges
	 * @param linesHash
	 * @param firstPointEdges
	 * @return the CounterLine
	 * @throws Exception
	 */
	private CounterLine generateCounterLine(HashMap<Point_tp,PointEdges_tp> linesHash, PointEdges_tp firstPointEdges) throws Exception {
		firstPointEdges.touch();	
		Line_tp lineA = firstPointEdges.getALine();
		Line_tp lineB = firstPointEdges.getBLine();
		CounterLine counterLineA = findCounterPoints(linesHash, firstPointEdges, lineA);
		if(counterLineA.isClosed())
			return counterLineA;
		else{
			CounterLine counterLineB = findCounterPoints(linesHash, firstPointEdges, lineB);
			if(counterLineB.isClosed()){
				return counterLineB;
			}
			else{
				if(counterLineA.getPointsList().get(0) != counterLineB.getPointsList().get(0))
					throw new Exception();

				ArrayList<Point_tp> unionLinePoints = new ArrayList<Point_tp>();
				for(Point_tp point : counterLineA.getPointsList()){
					unionLinePoints.add(0, point);
				}
				ArrayList<Point_tp> counterBPoints = counterLineB.getPointsList();
				unionLinePoints.addAll(counterBPoints.subList(1, counterBPoints.size()));

				CounterLine unionLine = new CounterLine(unionLinePoints,counterLineA.getHeight(),false);
				return unionLine;
			}
		}

	}

	
	/**
	 * Generates the path that starts from pe and continue with line.
	 * pe.getPoint() must be equal to one of line end points (line.getP1() or line.getP2()).
	 * @param linesHash
	 * @param firstPointEdges
	 * @return the CounterLine
	 * @throws TopographicMapException - this exception will be thrown when pe.getPoint() != line.getP2()
	 * and pe.getPoint() != line.getP1()
	 */
	private CounterLine  findCounterPoints(HashMap<Point_tp,PointEdges_tp> linesHash,PointEdges_tp pe, Line_tp line) throws TopographicMapException{
		ArrayList<Point_tp> points = new ArrayList<Point_tp>();
		boolean closeCounter = false;
		Line_tp curLine = line;
		Point_tp curPoint = pe.getPoint();
		double height = curPoint.getZ();
		points.add(curPoint);

		while(curLine != null){
			if(curLine.getP1().equals(curPoint))
				curPoint = curLine.getP2();
			else if(curLine.getP2().equals(curPoint))
				curPoint = curLine.getP1();
			else{
				throw new TopographicMapException("InValid behavior in topographic map colculations");
			}
			if(curPoint.equals(pe.getPoint())){
				closeCounter = true;
				curPoint = null;	
				curLine = null;
			}
			else{
				points.add(curPoint);
				PointEdges_tp curPointLines = linesHash.get(curPoint);
				curPointLines.touch();
				if(curPointLines.getALine().equals(curLine))
					curLine = curPointLines.getBLine();
				else if(curPointLines.getBLine().equals(curLine))
					curLine = curPointLines.getALine();
				else{
					curLine = null;
				
				}			
			}
		}
		CounterLine counterLine = new CounterLine(points, height, closeCounter);
		return counterLine;
	}
}
