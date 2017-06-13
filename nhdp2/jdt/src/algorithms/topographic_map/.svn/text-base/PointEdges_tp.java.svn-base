package algorithms.topographic_map;



/**
 * Represents a point and line two line that are connected to this point. 
 * Only two connected line are permitted.
 * @author Omri
 *
 */
public class PointEdges_tp {
	boolean _touched = false;
	
	Point_tp _point;
	Line_tp _aLine;
	Line_tp _bLine;
	
	
	/**
	 * Construct a new object with p as the base point.
	 * @param p
	 */
	public PointEdges_tp(Point_tp p){
		this._point = p;
	}
	
	
	/**
	 * Adds a line. if LineA is null the line will the added as LineA else
	 * if LineB is null the line will be added as LineB else the line will be ignored
	 * @param line
	 */
	public void addLine(Line_tp line) {
		if(line.equals(_aLine) || line.equals(_bLine))
			return;
		if(_aLine == null)
			_aLine = line;
		else if(_bLine == null)
			_bLine = line;
		//else
		//	throw new Exception("More then two lines connected to one point");
	}
	
	public Point_tp getPoint(){
		return _point;
	}
	
	public Line_tp getALine(){
		return _aLine;
	}
	public Line_tp getBLine(){
		return _bLine;
	}
	/**
	 * Used by topographic map algorithm - any PointEdges_tp that is in use by one of the counter lines will be marked as touched
	 */
	public void touch(){
		_touched = true;
	}
	
	/**
	 * @return true the object was touched during the topographic map algorithm.
	 */
	public boolean isTouched(){
		return _touched;
	}
}


