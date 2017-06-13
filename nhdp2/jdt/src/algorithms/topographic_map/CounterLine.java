package algorithms.topographic_map;


import java.util.ArrayList;
import java.util.Iterator;

import delaunay_triangulation.Point_dt;

/**
 * The CounterLine class describes an counter line.
 * The CounterLine is defined by a list of points, height and an argument that describes 
 * if the counter line is closed (Polygon) or not (Path). 
 * @version 1.0 12 December 2009
 * @author Omri Gutman
 *
 */
public class CounterLine {
	private boolean _isClosed;
	private double _height;

	private ArrayList<Point_tp> _points;
	public CounterLine(ArrayList<Point_tp> points, double height, boolean isClosed){
		this._isClosed = isClosed;
		this._height = height;
		this._points = points;
	}
	
	
	/**
	 * @return the counter line height.
	 */
	public double getHeight(){
		return _height;
	}
	/**
	 * 
	 * @return An Iterator object that iterates over the counter line points. converts Point_tp to Point_dt
	 * @see Point_dt
	 * @see Iterator
	 */
	public Iterator<Point_dt> getPointsListIterator(){
		return new PointDtIterator(_points.listIterator());
	}
	/**
	 * 
	 * @return the counter line points list.
	 * @see Point_dt
	 * @see ArrayList
	 */
	public ArrayList<Point_tp> getPointsList(){
		return _points;
	}
	
	
	/**
	 *
	 * @return true is the counter line is closed counter line(Polygon) or not (Path)
	 */
	public boolean isClosed(){
		return _isClosed;
	}
	
	
	/**
	 * @return the number of points in this CounterLine.
	 */
	public int getNumberOfPoints(){
		return _points.size();
	}
	
	
	
	private class PointDtIterator implements Iterator<Point_dt> {

		private Iterator<Point_tp>_pointTPItr;
		public PointDtIterator(Iterator<Point_tp> pointsTPItr){
			this._pointTPItr = pointsTPItr;
		}
		
		@Override
		public boolean hasNext() {
			return _pointTPItr.hasNext();			
		}

		@Override
		public Point_dt next() {
			
			Point_tp tpPoint = _pointTPItr.next();
			return new Point_dt(tpPoint.getX().doubleValue(),tpPoint.getY().doubleValue(),tpPoint.getZ());
		}

		@Override
		public void remove() {
			_pointTPItr.remove();
		}
		
	}

}
