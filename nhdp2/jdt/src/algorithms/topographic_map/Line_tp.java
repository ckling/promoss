package algorithms.topographic_map;


/**
 * Class that represents a 3D line. used by the topographic map algorithm.
 * @author Omri
 *
 */
public class Line_tp {


	/**
	 * Construct a line between p1 to p2 in the height z
	 * @param p1
	 * @param p2
	 * @param z
	 */
	public Line_tp(Point_tp p1, Point_tp p2,
			double z) {

		_p1 = p1;
		_p2 = p2;
		if(_p1.compareTo(_p2) < 0){
			Point_tp temp = _p1;
			_p1 = _p2;
			_p2 = temp;
		}			
		_z = z;		
	}
	private Point_tp _p1;
	private Point_tp _p2;
	private double _z;

	/**
	 * return true iff this._p1.equal(other._p1) and  
	 * this._p2.equal(other._p2) z is ignored.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Line_tp other = (Line_tp) obj;
		if (_p1 == null) {
			if (other._p1 != null)
				return false;
		} else if (!_p1.equals(other._p1))
			return false;
		if (_p2 == null) {
			if (other._p2 != null)
				return false;
		} else if (!_p2.equals(other._p2))
			return false;
		return true;
	}
	
	public Point_tp getP1(){
		return _p1;
	}
	public Point_tp getP2(){
		return _p2;
	}
	public double getZ(){
		return _z;
	}
}

