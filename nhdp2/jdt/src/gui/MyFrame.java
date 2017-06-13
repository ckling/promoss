package gui;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import algorithms.topographic_map.CounterLine;
import algorithms.topographic_map.ITopographicMap;
import algorithms.topographic_map.TopographicMapFactory;
import delaunay_triangulation.Circle_dt;
import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

/**
 * GUI class to test the delaunay_triangulation Triangulation package:
 */

class MyFrame extends Frame implements ActionListener {

	public static void main(String[] args) {
		MyFrame win = new MyFrame();
		win.start();
	}

	private static final long serialVersionUID = 1L;
	// *** private data ***
	public static final int POINT = 1, FIND = 2, VIEW1 = 3, VIEW2 = 4,
			VIEW3 = 5, VIEW4 = 6, SECTION1 = 7, SECTION2 = 8, GUARD = 9,
			CLIENT = 10, DELETE = 11, VORONOI = 12;
	private int _stage, _view_flag = VIEW1, _mc = 0;
	private Triangle_dt _t1, _t2; // tmp triangle for find testing for selection
	private Delaunay_Triangulation _ajd = null;
	private ITopographicMap _topoCreator;
	private ArrayList<CounterLine> _curTopoMap;
	protected Vector<Point_dt> _clients, _guards;
	protected Point_dt _dx_f, _dy_f, _dx_map, _dy_map, _p1, _p2;// ,_guard=null,
																// _client=null;
	protected boolean _visible = false;
	private double GH = 30, CH = 5;
	private int _topo_dz = 100; 
	// private Vector<Triangle_dt> _tr = null;//new Vector<Triangle_dt>();
	private Visibility _los;// , _section2;

	// *** text area ***
	public MyFrame() {
		this.setTitle("Delaunay GUI tester");
		this.setSize(500, 500);
		_stage = 0;
		_ajd = new Delaunay_Triangulation();
		_topoCreator = TopographicMapFactory.createTopographicMap();

		_dx_f = new Point_dt(10, this.getWidth() - 10);
		_dy_f = new Point_dt(55, this.getHeight() - 10);
		_dx_map = new Point_dt(_dx_f);
		_dy_map = new Point_dt(_dy_f);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
	}

	public MyFrame(Delaunay_Triangulation aj) {
		this.setTitle("ajDelaunay GUI tester");
		this.setSize(500, 500);
		_stage = 0;
		_ajd = aj;
		_dx_f = new Point_dt(10, this.getWidth() - 10);
		_dy_f = new Point_dt(55, this.getHeight() - 10);
		_dx_map = new Point_dt(aj.maxBoundingBox().x(), aj.minBoundingBox().x());
		_dy_map = new Point_dt(aj.maxBoundingBox().y(), aj.minBoundingBox().y());
		_clients = null;
		_guards = null;
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
	}

	public void paint(Graphics g) {
		// _ajd.initTriangle();
		// ajTriangle[] tt = _ajd._triangles;
		if (_ajd == null || _ajd.size() == 0)
			return;
		_dx_f = new Point_dt(10, this.getWidth() - 10);
		_dy_f = new Point_dt(55, this.getHeight() - 10);
		
		Triangle_dt curr = null;
		Iterator<Triangle_dt> it = _ajd.trianglesIterator();
		while (it.hasNext()) {
			curr = it.next();
			if (!curr.isHalfplane() && _view_flag != VORONOI)
				drawTriangle(g, curr, null);
		}
		it = _ajd.trianglesIterator();
		while (it.hasNext()) {
			curr = it.next();
			if (curr.isHalfplane() && _view_flag != VORONOI)
				drawTriangle(g, curr, null);
		}
		if (_t2 != null)
			drawTriangle(g, _t2, Color.red);
		if (_t1 != null && _stage == FIND)
			drawTriangle(g, _t1, Color.green);
		if (this._view_flag == VIEW3)
			drawTopographicMap(g,_curTopoMap);

		// debug
		if (_mc < _ajd.getModeCounter() && _view_flag != VORONOI) {
			_mc = _ajd.getModeCounter();
			int i = 0;
			for (Iterator<Triangle_dt> it2 = _ajd.getLastUpdatedTriangles(); it2
					.hasNext();) {
				i++;
				drawTriangle(g, it2.next(), Color.CYAN);
			}
			System.out.println("   MC: " + _mc
					+ "  number of triangles updated: " + i);

		}

		if (_los != null && (_stage == SECTION1 | _stage == SECTION2)) {
			if (_los != null && _los._tr != null) {
				it = _los._tr.iterator();
				while (it.hasNext()) {
					curr = it.next();
					if (!curr.isHalfplane())
						drawTriangle(g, curr, Color.RED);
				}
			}
			Iterator<Point_dt> pit = _los._section.iterator();
			int i = 0;
			while (pit.hasNext()) {
				Point_dt curr_p = pit.next();
				if (curr_p != null && _view_flag != VORONOI) {
					drawPoint(g, curr_p, Color.BLUE);
					System.out.println(i + ") " + curr_p + "  dist _p1: "
							+ _p1.distance(curr_p));
					i++;
				}
			}
			drawLine(g, _p1, _p2);
		}
		/*
		 * if(_stage == GUARD | _stage == CLIENT) { if(_p1!=null)
		 * drawPoint(g,_p1,6,Color.ORANGE); if(_p2!=null) { if(_visible)
		 * drawPoint(g,_p2,6,Color.BLUE); else drawPoint(g,_p2,6, Color.RED); }
		 * }
		 */
		if (_los == null)
			_los = new Visibility(_ajd);
		if (_stage == GUARD | _stage == CLIENT) {
			int[] ccc = new int[0];
			if (_clients != null)
				ccc = new int[_clients.size()];
			for (int gr = 0; _guards != null && gr < _guards.size(); gr++) {
				Point_dt gg = _guards.elementAt(gr);
				drawPoint(g, gg, 8, Color.ORANGE);

				for (int c = 0; _clients != null && c < _clients.size(); c++) {
					Point_dt cc = _clients.elementAt(c);
					drawPoint(g, cc, 6, Color.white);
					// Color cl = Color.RED;
					if (_los.los(gg, cc)) {
						this.drawLine(g, gg, cc);
						ccc[c]++;
					}
				}
			}
			int c1 = 0, c2 = 0, c3 = 0;
			for (int i = 0; i < ccc.length; i++) {
				if (ccc[i] > 0) {
					c1++;
					c2 += ccc[i];
				}
			}
			if (c1 > 0)
				System.out.println("clients:" + ccc.length + "  visible c:"
						+ c1 + "   ave:" + c2 / c1);
		}	
		if (_view_flag == VORONOI) {
			drawVoronoi(g);
		}

	}

	/**
	 * Draws Voronoi diagram based on current triangulation
	 * A Voronoi diagram can be created from a Delaunay triangulation by
	 * connecting the circumcenters of neighboring triangles
	 * 
	 * By Udi Schneider
	 * 
	 * @param g Graphics object
	 */
	void drawVoronoi(Graphics g)
	{
		Iterator<Triangle_dt> it = _ajd.trianglesIterator();
		
		while (it.hasNext()) {
			Triangle_dt curr = it.next();
			Color temp = g.getColor();
            g.setColor(Color.BLACK);
            
            // For a half plane, only one corner is needed
			if (curr.isHalfplane())
			{
				try {
					drawPolygon(g, _ajd.calcVoronoiCell(curr, curr.p1()));
				} catch (NullPointerException e) {}
			}
			// for a full triangle, check every corner
			else
			{
				// if a triangle has no neighbors, a null exception will be caught
				// and no action taken.
				// this is expected, for example when there is only one triangle
				// at the start of the user input
				try {
					drawPolygon(g, _ajd.calcVoronoiCell(curr, curr.p1()));
				} catch (NullPointerException e) {}
				try {
					drawPolygon(g, _ajd.calcVoronoiCell(curr, curr.p2()));
				} catch (NullPointerException e) {}
				try {
					drawPolygon(g, _ajd.calcVoronoiCell(curr, curr.p3()));
				} catch (NullPointerException e) {}
				
				drawPoint(g, curr.p1(), Color.RED);
				drawPoint(g, curr.p2(), Color.RED);
				drawPoint(g, curr.p3(), Color.RED);
			}
			g.setColor(temp);
		}
	}

	public void drawTopographicMap(Graphics g,ArrayList<CounterLine> counterLines){
		if(counterLines != null){
			g.setColor(Color.YELLOW);
			for (CounterLine line : counterLines){
				int[] xPoints = new int[line.getNumberOfPoints()];
				int[] yPoints = new int[line.getNumberOfPoints()];

				Iterator<Point_dt> pointsItr = line.getPointsListIterator();
				int index = 0;
				while (pointsItr.hasNext()){
					Point_dt point = pointsItr.next();
					Point_dt screenPoint = world2screen(point);
					xPoints[index] = (int) screenPoint.x();
					yPoints[index]= (int)screenPoint.y();
					index++;
				}
				if(line.isClosed())
					g.drawPolygon(xPoints,yPoints,xPoints.length);
				else
					g.drawPolyline(xPoints, yPoints, xPoints.length);	
			}
		}
	}

	public void drawTriangle(Graphics g, Triangle_dt t, Color cl) {
		if (_view_flag == VIEW1 | t.isHalfplane()) {
			if (cl != null)
				g.setColor(cl);
			if (t.isHalfplane()) {
				if (cl == null)
					g.setColor(Color.blue);
				drawLine(g, t.p1(), t.p2());
			} else {
				if (cl == null)
					g.setColor(Color.black);
				drawLine(g, t.p1(), t.p2());
				drawLine(g, t.p2(), t.p3());
				drawLine(g, t.p3(), t.p1());
			}
		} else {
			// //////////////////////////////////////////////////////////////////
			double maxZ = _ajd.maxBoundingBox().z();
			double minZ = _ajd.minBoundingBox().z();
			double z = (t.p1().z() + t.p2().z() + t.p3().z()) / 3.0;
			double dz = maxZ - minZ;
			int co = 30 + (int) (220 * ((z - minZ) / dz));
			if (cl == null)
				cl = new Color(co, co, co);
			g.setColor(cl);
			int[] xx = new int[3], yy = new int[3];
			// double f = 0;
			// double dx_map = _dx_map.y()- _dx_map.x();
			// double dy_map = _dy_map.y()- _dy_map.x();

			// f = (t.p1().x() -_dx_map.x())/dx_map;
			Point_dt p1 = world2screen(t.p1());
			xx[0] = (int) p1.x();
			yy[0] = (int) p1.y();
			Point_dt p2 = world2screen(t.p2());
			xx[1] = (int) p2.x();
			yy[1] = (int) p2.y();
			Point_dt p3 = world2screen(t.p3());
			xx[2] = (int) p3.x();
			yy[2] = (int) p3.y();

			g.fillPolygon(xx, yy, 3);

			// ////////////////////////////////////
		}
	}

	/**
	 * Draws a polygon represented by Point_dt points
	 * 
	 * By Udi Schneider 
	 */
	public void drawPolygon(Graphics g, Point_dt[] polygon)
	{
		int[] x = new int[polygon.length];
        int[] y = new int[polygon.length];
        for (int i = 0; i < polygon.length; i++) {
        	polygon[i] = this.world2screen(polygon[i]);
            x[i] = (int) polygon[i].x();
            y[i] = (int) polygon[i].y();
        }
        g.drawPolygon(x, y, polygon.length);
	}
	
	public void drawLine(Graphics g, Point_dt p1, Point_dt p2) {
		// g.drawLine((int)p1.x(), (int)p1.y(), (int)p2.x(), (int)p2.y());
		Point_dt t1 = this.world2screen(p1);
		Point_dt t2 = this.world2screen(p2);
		g.drawLine((int) t1.x(), (int) t1.y(), (int) t2.x(), (int) t2.y());
	}

	public void drawPoint(Graphics g, Point_dt p1, Color cl) {
		drawPoint(g, p1, 4, cl);
	}

	public void drawPoint(Graphics g, Point_dt p1, int r, Color cl) {
		// g.drawLine((int)p1.x(), (int)p1.y(), (int)p2.x(), (int)p2.y());
		Point_dt t1 = this.world2screen(p1);
		g.setColor(cl);
		g.fillOval((int) t1.x() - r / 2, (int) t1.y() - r / 2, r, r);
	}

	public void start() {
		this.show();
		Dialog();
	}

	public void Dialog() {
		MenuBar mbar = new MenuBar();

		Menu m = new Menu("File");
		MenuItem m1;
		m1 = new MenuItem("Open");
		m1.addActionListener(this);
		m.add(m1);
		m1 = new MenuItem("Save tsin");
		m1.addActionListener(this);
		m.add(m1);
		m1 = new MenuItem("Save smf");
		m1.addActionListener(this);
		m.add(m1);

		MenuItem m6 = new MenuItem("Clear");
		m6.addActionListener(this);
		m.add(m6);

		MenuItem m2 = new MenuItem("Exit");
		m2.addActionListener(this);
		m.add(m2);
		mbar.add(m);

		m = new Menu("Input");
		MenuItem m3 = new MenuItem("Point");
		m3.addActionListener(this);
		m.add(m3);
		m3 = new MenuItem("100-rand-ps");
		m3.addActionListener(this);
		m.add(m3);
		m3 = new MenuItem("Guard-30m");
		m3.addActionListener(this);
		m.add(m3);
		m3 = new MenuItem("Client-5m");
		m3.addActionListener(this);
		m.add(m3);

		mbar.add(m);
		
		m = new Menu("Functions");
		MenuItem m5 = new MenuItem("Delete");
		m5.addActionListener(this);
		m.add(m5);
		m5 = new MenuItem("Update Topo");
		m5.addActionListener(this);
		m.add(m5);
		mbar.add(m);
		
		m = new Menu("View");
		m3 = new MenuItem("Lines");
		m3.addActionListener(this);
		m.add(m3);
		m3 = new MenuItem("Triangles");
		m3.addActionListener(this);
		m.add(m3);
		m3 = new MenuItem("Topo");
		m3.addActionListener(this);
		m.add(m3);
		MenuItem m4 = new MenuItem("Find");
		m4.addActionListener(this);
		m.add(m4);
		m4 = new MenuItem("Section");
		m4.addActionListener(this);
		m.add(m4);
		m4 = new MenuItem("Info");
		m4.addActionListener(this);
		m.add(m4);
		m4 = new MenuItem("CH");
		m4.addActionListener(this);
		m.add(m4);
		mbar.add(m);
		m4 = new MenuItem("Voronoi");
		m4.addActionListener(this);
		m.add(m4);
		mbar.add(m);

		setMenuBar(mbar);
		this.addMouseListener(new mouseManeger());
	}

	public void actionPerformed(ActionEvent evt) {
		String arg = evt.getActionCommand();
		if (arg.equals("Open"))
			openTextFile();
		else if (arg.equals("Save tsin"))
			saveTextFile();
		else if (arg.equals("Save smf"))
			saveTextFile2();
		else if (arg.equals("Lines")) {
			this._view_flag = VIEW1;
			repaint();
		} else if (arg.equals("Triangles")) {
			this._view_flag = VIEW2;
			repaint();
		} else if (arg.equals("Topo")) {
			this._view_flag = VIEW3;
			repaint();
		} else if (arg.equals("Clear")) {
			_ajd = new Delaunay_Triangulation();
			_dx_map = new Point_dt(_dx_f);
			_dy_map = new Point_dt(_dy_f);
			_clients = null;
			_guards = null;
			_mc = 0;
			repaint();
		} else if (arg.equals("Exit")) {
			System.exit(209);
		}

		else if (arg.equals("Point")) {
			_stage = POINT;
		} else if (arg.equals("CH")) {
			_ajd.CH_vertices_Iterator();
		} else if (arg.equals("100-rand-ps")) {
			double x0 = 10, y0 = 60, dx = this.getWidth() - x0 - 10, dy = this
					.getHeight()
					- y0 - 10;
			for (int i = 0; i < 100; i++) {
				double x = Math.random() * dx + x0;
				double y = Math.random() * dy + y0;
				Point_dt q = new Point_dt(x, y);
				Point_dt p = screen2world(q);
				_ajd.insertPoint(p);
			}
			repaint();
		} else if (arg.equals("Find")) {
			_stage = FIND;
		} else if (arg.equals("Section")) {
			_stage = SECTION1;
		} else if (arg.equals("Client-5m")) {
			// System.out.println("CL!");
			_stage = this.CLIENT;

		} else if (arg.equals("Guard-30m")) {// System.out.println("GR!");
			_stage = this.GUARD;
		} else if (arg.equals("Info")) {
			String ans = "" + _ajd.getClass().getCanonicalName()
					+ "  # vertices:" + _ajd.size() + "  # triangles:"
					+ _ajd.trianglesSize();
			ans += "   min BB:" + _ajd.minBoundingBox() + "   max BB:"
					+ _ajd.maxBoundingBox();
			System.out.println(ans);
			System.out.println();
		} else if (arg.equals("Delete")){
			_stage = DELETE;
		} else if (arg.equals("Voronoi")){
			_view_flag = VORONOI;
			repaint();
		} else if(arg.equals("Update Topo")){
			_curTopoMap = 
				_topoCreator.createCounterLines(_ajd.trianglesIterator(), _topo_dz);
			_view_flag = VIEW3;
			repaint();
		}
	}

	// *** private methodes - random points obs ****

	// ********** Private methodes (open,save...) ********

	private void openTextFile() {
		_stage = 0;
		FileDialog d = new FileDialog(this, "Open text file", FileDialog.LOAD);
		d.show();
		String dr = d.getDirectory();
		String fi = d.getFile();
		_clients = null;
		_guards = null;
		if (fi != null) { // the user actualy choose a file.
			try {
				_ajd = new Delaunay_Triangulation(dr + fi);
				_dx_map = new Point_dt(_ajd.minBoundingBox().x(), _ajd
						.maxBoundingBox().x());
				_dy_map = new Point_dt(_ajd.minBoundingBox().y(), _ajd
						.maxBoundingBox().y());
				_curTopoMap = 
					_topoCreator.createCounterLines(_ajd.trianglesIterator(), _topo_dz);
				repaint();
			} catch (Exception e) { // in case something went wrong.
				System.out.println("** Error while reading text file **");
				System.out.println(e);
			}

		}
	}

	private void saveTextFile() {
		_stage = 0;
		FileDialog d = new FileDialog(this, "Saving TSIN text file",
				FileDialog.SAVE);
		d.show();
		String dr = d.getDirectory();
		String fi = d.getFile();
		if (fi != null) {
			try {
				// _ajd.write_tsin2(dr+fi);
				// _ajd.write_CH(dr+"CH_"+fi);
				_ajd.write_tsin(dr + fi);
			} catch (Exception e) {
				System.out.println("ERR cant save to text file: " + dr + fi);
				e.printStackTrace();
			}
		}
	}

	public void saveTextFile2() {
		_stage = 0;
		FileDialog d = new FileDialog(this, "Saving SMF text file",
				FileDialog.SAVE);
		d.show();
		String dr = d.getDirectory();
		String fi = d.getFile();
		if (fi != null) {
			try {
				_ajd.write_smf(dr + fi);
			} catch (Exception e) {
				System.out.println("ERR cant save to text file: " + dr + fi);
				e.printStackTrace();
			}
		}
	}

	// ***** inner classes (mouse maneger) *****
	// class mouseManeger1 extends MouseMotionAdapter {
	// public void mouseMoved(MouseEvent e) {
	// m_x = e.getX(); m_y = e.getY();
	// }
	// }

	class mouseManeger extends MouseAdapter { // inner class!!
		public void mousePressed(MouseEvent e) {
			int xx = e.getX();
			int yy = e.getY();
			// System.out.println("_stage: "+_stage+"  selected: "+len);
			switch (_stage) {
			case (0): {
				System.out.println("[" + xx + "," + yy + "]");
				break;
			}
			case (POINT): {
				Point_dt q = new Point_dt(xx, yy);
				Point_dt p = screen2world(q);
				_ajd.insertPoint(p);
				repaint();
				break;
			}
			case (DELETE):{
				Point_dt q = new Point_dt(xx, yy);
                                //finds 
                                Point_dt p = screen2world(q);
                                Point_dt pointToDelete = _ajd.findClosePoint(p);
                                if(pointToDelete==null) {
                                    System.err.println("Error : the point doesn't exists");
                                    return;
                                }
				_ajd.deletePoint(pointToDelete);
				repaint();
				break;
			}
			case (FIND): {
				Point_dt q = new Point_dt(xx, yy);
				Point_dt p = screen2world(q);
				_t1 = _ajd.find(p);
				repaint();
				break;
			}
			case (SECTION1): {
				Point_dt q = new Point_dt(xx, yy);
				_p1 = screen2world(q);
				// _p1 = new Point_dt(99792.03,1073355.0,30.0);

				// _t1 = _ajd.find(_p1);
				_stage = SECTION2;
				break;
			}
			case (SECTION2): {
				Point_dt q = new Point_dt(xx, yy);
				_p2 = screen2world(q);
				// _p2 = new Point_dt(149587.055,1040477.0,5.0);

				// _t2 = _ajd.find(_p2);
				_los = new Visibility(_ajd);
				_los.computeSection(_p1, _p2);
				repaint();
				_stage = SECTION1;
				break;
			}
			case (GUARD): {
				Point_dt q = new Point_dt(xx, yy);
				_p1 = screen2world(q);
				if (_guards == null)
					_guards = new Vector<Point_dt>();
				_guards.add(new Point_dt(_p1.x(), _p1.y(), GH));
				/*
				 * if(_p2!=null) { _los = new Visibility(_ajd);
				 * _los.computeSection(_p1,_p2); _visible =
				 * _los.isVisible(30,5); }
				 */
				repaint();
				break;
			}
			case (CLIENT): {
				Point_dt q = new Point_dt(xx, yy);
				_p2 = screen2world(q);
				if (_clients == null)
					_clients = new Vector<Point_dt>();
				_clients.add(new Point_dt(_p2.x(), _p2.y(), CH));
				/*
				 * if(_p1!=null) { _los = new Visibility(_ajd);
				 * _los.computeSection(_p1,_p2); _visible =
				 * _los.isVisible(30,5); }
				 */
				repaint();
				break;
			}

				// //////////////
			}
		}
	}

	Point_dt screen2world(Point_dt p) {
		double x = transform(_dx_f, p.x(), _dx_map);
		double y = transformY(_dy_f, p.y(), _dy_map);
		return new Point_dt(x, y);
	}

	Point_dt world2screen(Point_dt p) {
		double x = transform(_dx_map, p.x(), _dx_f);
		double y = transformY(_dy_map, p.y(), _dy_f);
		return new Point_dt(x, y);
	}

	/**
	 * transforms the point p from the Rectangle th into this Rectangle, Note:
	 * r.contains(p) must be true! assume p.x
	 * < p
	 * .y
	 * 
	 * */

	static double transform(Point_dt range, double x, Point_dt new_range) {
		double dx1 = range.y() - range.x();
		double dx2 = new_range.y() - new_range.x();

		double scale = (x - range.x()) / dx1;
		double ans = new_range.x() + dx2 * scale;
		return ans;
	}

	/**
	 * transform the point p from the Rectangle th into this Rectangle ,Note:
	 * flips the Y cordination for frame!, Note: r.contains(p) must be true!
	 * assume p.x
	 * < p
	 * .y
	 * 
	 * */

	static double transformY(Point_dt range, double x, Point_dt new_range) {
		double dy1 = range.y() - range.x();
		double dy2 = new_range.y() - new_range.x();

		double scale = (x - range.x()) / dy1;
		double ans = new_range.y() - dy2 * scale;
		return ans;
	}
}
