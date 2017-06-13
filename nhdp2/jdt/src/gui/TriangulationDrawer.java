package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;

import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

/**
 * This class is responsible for drawing a triangulation.
 * NOTE: Most of this code is taken from MyFrame.java.
 * 
 * @author NS
 *
 */
public class TriangulationDrawer {

	private Point_dt _dx_f, _dy_f, _dx_map, _dy_map;
	private Point_dt m_boundingBoxMin, m_boundingBoxMax;
	
	
	/**
	 * Constructor.
	 */
	public TriangulationDrawer(int width, int height) {
		int startX = width / 4;
		int startY = height / 8;
		_dx_f = new Point_dt(startX, width - startX + 30);
		_dy_f = new Point_dt(startY, height);
	}
	
	/**
	 * Draws the triangulation.
	 * 
	 * @param g The Graphics object used to draw the triangulation on.
	 * @param triangulation A Vector objects that contains multiple
	 * Triangle_dt objects that represent the triangulation.
	 */
	public void drawTriangulation(Graphics g, 
			Vector<Triangle_dt> triangulation) {
		setBoundingBox(triangulation);
		
		_dx_map = new Point_dt(m_boundingBoxMax.x(), m_boundingBoxMin.x());
		_dy_map = new Point_dt(m_boundingBoxMax.y(), m_boundingBoxMin.y());
		
		for (Integer i = 0; i < triangulation.size(); i++) {
			Triangle_dt currentTriangle = triangulation.get(i);
			drawTriangle(g, currentTriangle, null);
		}
	}
	
	/**
	 * Sets the bounding box.
	 * @param triangulation The Vector of Triangle_dt objects that represents
	 * the triangulation.
	 */
	private void setBoundingBox(Vector<Triangle_dt> triangulation) {
		for (Integer i = 0; i < triangulation.size(); i++) {
			Triangle_dt currentTriangle = triangulation.get(i);
			Point_dt p1 = currentTriangle.p1();
			Point_dt p2 = currentTriangle.p2();
			Point_dt p3 = currentTriangle.p3();
			
			updateBoundingBox(p1);
			updateBoundingBox(p2);
			updateBoundingBox(p3);
		}
	}
	
	/**
	 * Updates the bounding box.
	 * @param p The current box to compare to the bounding box.
	 */
	private void updateBoundingBox(Point_dt p) {
		double x = p.x(), y = p.y(), z = p.z();
		if (m_boundingBoxMin == null) {
			m_boundingBoxMin = new Point_dt(p);
			m_boundingBoxMax = new Point_dt(p);
		} else {
			if (x < m_boundingBoxMin.x())
				m_boundingBoxMin.setX(x);
			else if (x > m_boundingBoxMax.x())
				m_boundingBoxMax.setX(x);
			if (y < m_boundingBoxMin.y())
				m_boundingBoxMin.setY(y);
			else if (y > m_boundingBoxMax.y())
				m_boundingBoxMax.setY(y);
			if (z < m_boundingBoxMin.z())
				m_boundingBoxMin.setZ(z);
			else if (z > m_boundingBoxMax.z())
				m_boundingBoxMax.setZ(z);
		}
	}
	
	/**
	 * Draws a triangle.
	 * 
	 * @param g The Graphics object used to draw the triangulation on.
	 * @param t The current Triangle_dt object to draw
	 * @param c The color of the triangle
	 * NOTE: Taken from MyFrame.java.
	 */
	private void drawTriangle(Graphics g, 
			Triangle_dt t, 
			Color c) {
			if (c != null)
				g.setColor(c);
			if (t.isHalfplane()) {
				if (c == null)
					g.setColor(Color.blue);
				drawLine(g, t.p1(), t.p2());
			} else {
				if (c == null)
					g.setColor(Color.black);
				drawLine(g, t.p1(), t.p2());
				drawLine(g, t.p2(), t.p3());
				drawLine(g, t.p3(), t.p1());
			}
	}
	
	/**
	 * Draws a line.
	 * 
	 * @param g The Graphics object used to draw the triangulation on.
	 * @param p1 The first point on the line.
	 * @param p2 The second point on the line.
	 * NOTE: Taken from MyFrame.java.
	 */
	private void drawLine(Graphics g, Point_dt p1, Point_dt p2) {
		Point_dt t1 = this.world2screen(p1);
		Point_dt t2 = this.world2screen(p2);
		g.drawLine((int) t1.x(), (int) t1.y(), (int) t2.x(), (int) t2.y());
	}
	
	/**
	 * Taken from MyFrame.java.
	 */
	private Point_dt world2screen(Point_dt p) {
		double x = transform(_dx_map, p.x(), _dx_f);
		double y = transformY(_dy_map, p.y(), _dy_f);
		return new Point_dt(x, y);
	}

	/**
	 * Transforms the point p from the Rectangle th into this Rectangle.
	 * NOTE: r.contains(p) must be true! assume p.x < p.y.
	 * NOTE: Taken from MyFrame.java.
	 **/
	private double transform(Point_dt range, double x, Point_dt new_range) {
		double dx1 = range.y() - range.x();
		double dx2 = new_range.y() - new_range.x();

		double scale = (x - range.x()) / dx1;
		double ans = new_range.x() + dx2 * scale;
		return ans;
	}
	
	/**
	 * Transform the point p from the Rectangle into this Rectangle.
	 * NOTE: flips the Y coordination for frame!.
	 * NOTE: r.contains(p) must be true! assume p.x < p.y.
	 * NOTE: Taken from MyFrame.java.
	 * */
	private double transformY(Point_dt range, double x, Point_dt new_range) {
		double dy1 = range.y() - range.x();
		double dy2 = new_range.y() - new_range.x();

		double scale = (x - range.x()) / dy1;
		double ans = new_range.y() - dy2 * scale;
		return ans;
	}
}