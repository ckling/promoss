package algorithms.topographic_map;

import java.math.BigDecimal;

import delaunay_triangulation.Point_dt;


/**
 * Represents a 3D point. used by the topographic map algorithm.
 * In order to get good accuracy big decimal where used.
 * @author Omri
 *
 */
public class Point_tp {
	


	/**
	 * Construct Point_tp from Point_dt
	 * @param pointDt
	 */
	public Point_tp(Point_dt pointDt){
		_x = new BigDecimal(pointDt.x());
		_y = new BigDecimal(pointDt.y());
		_z = pointDt.z();
	}


	public Point_tp(BigDecimal x, BigDecimal y, double z) {

		_x = x;
		_y = y;
		_z = z;
	}
	private BigDecimal _x;
	private BigDecimal _y;
	private double _z;
	
	public BigDecimal getX(){
		return _x;
	}
	public BigDecimal getY(){
		return _y;
	}
	public double getZ(){
		return _z;
	}
	




	/**
	 * Only x and y coordinates are being used for the hash code calculations
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = _x.hashCode();
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = _y.hashCode();
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	/**
	 * return true iff this point [x,y] coordinates are the same as obj [x,y]
	 * coordinates. (the z value is ignored).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point_tp other = (Point_tp) obj;
		if (_x.compareTo(other._x) != 0)
			return false;
		if (_y.compareTo(other._y) != 0)
			return false;
		return true;
	}

	
	/**
	 *Compare between two points
	 */
	public int compareTo(Point_tp other) {
		if(this._x.compareTo(other._x) != 0)
			return this._x.compareTo(other._x);

		return this._y.compareTo(other._y);

	}
}


