using System;
using System.Collections.Generic;
using System.Text;
using System.Linq;
using JDT_NET.Delaunay_triangulation;

namespace JDT_NET.GUI
{
    public class Visibility
    {
        internal List<Point_dt> _section;
        internal List<Triangle_dt> _tr;
        Point_dt _p1, _p2, last = null;
        Delaunay_Triangulation _dt;

        public Visibility(Delaunay_Triangulation dt)
        {
            _dt = dt;
            _section = new List<Point_dt>();
        }

        /**
         * this method checke if there is a Line Of Site between p1 and p2, note the
         * z value of the points is the Hieght ABOVE ground!!
         * 
         * @param p1
         *            "guard",
         * @param p2
         *            "client"
         * @return true iff both points are valid (not null && inside the
         *         triangulation)
         */
        public bool los(Point_dt p1, Point_dt p2)
        {
            bool ans = true;
            if (_dt == null)
                throw new Exception(
                        "** ERR: null pointer triangulation (LOS) **");
            if (p1 == null || p2 == null)
                throw new Exception("** ERR: null pointer points (LOS) **");
            if (!_dt.contains(p1.x, p1.y))
                throw new Exception("** ERR: p1:" + p1
                        + " is NOT contained in the triangulation **");
            if (!_dt.contains(p2.x, p2.y))
                throw new Exception("** ERR: p2:" + p2
                        + " is NOT contained in the triangulation **");

            this.computeSection(p1, p2);
            ans = this.isVisible(p1.z, p2.z);
            this.computeSection(p2, p1);
            ans = ans && this.isVisible(p2.z, p1.z);
            printSection(p1, p2);
            return ans;
        }

        void printSection(Point_dt p1, Point_dt p2) {
		// System.out.println("G: "+p1);
		// System.out.println("C: "+p2);
		double dx = p1.distance(p2);
		double z1 = p1.z + this._dt.z(p1).z;
		double z2 = p2.z + this._dt.z(p2).z;
		double dz = z2 - z1;
		double gap = 100000;
		for (int i = 0; i < _section.Count; i++) {
			Point_dt curr = _section.ElementAt(i);
			double d = curr.distance(p1);
			double z = z1 + dz * (d / dx);
			double dzz = z - curr.z;
			if (gap > dzz)
				gap = dzz;
			// System.out.println(i+")  dist1:"+curr.distance(p1)+"   dist2:"+curr.distance(p2)+"  c.z:"+curr.z+"  Z:"+z+"  block:"+(curr.z>z));
			Console.WriteLine(i + ") dist1:" + curr.distance(p1) + "  dist2:"
					+ curr.distance(p2) + "  Curr: " + curr + "  DZ:"
					+ (z - curr.z) + "  block:" + (curr.z > z));
		}
		Console.WriteLine("Triangle size: " + _tr.Count + "  min Gap:" + gap);

	}

        internal void computeSection(Point_dt p1, Point_dt p2)
        {

            Triangle_dt t1 = _dt.find(p1);
            Triangle_dt t2 = _dt.find(p2);
            _p1 = t1.z(p1);
            _p2 = t2.z(p2);
            if (_tr == null)
                _tr = new List<Triangle_dt>();
            else
                _tr.Clear();
            if (_section == null)
                _section = new List<Point_dt>();
            else
                _section.Clear();
            Triangle_dt curr_t = t1;
            while (curr_t != t2 && curr_t != null)
            {
                _tr.Add(curr_t);
                cut(curr_t);
                curr_t = next_t(p1, p2, curr_t, _tr);
            }
            _tr.Add(t2);
        }

        Triangle_dt next_t(Point_dt pp1, Point_dt pp2, Triangle_dt curr,
                List<Triangle_dt> tr)
        {
            Triangle_dt ans = null, t12, t23, t31;
            t12 = curr.next_12();
            t23 = curr.next_23();
            t31 = curr.next_31();
            if (t12 != null && cut(pp1, pp2, t12) && !tr.Contains(t12))
                ans = t12;
            else if (t23 != null && cut(pp1, pp2, t23) && !tr.Contains(t23))
                ans = t23;
            else if (t31 != null && cut(pp1, pp2, t31) && !tr.Contains(t31))
                ans = t31;
            return ans;
        }

        /** return true iff the segment _p1,_p2 is cutting t */
        bool cut(Point_dt pp1, Point_dt pp2, Triangle_dt t)
        {
            bool ans = false;
            if (t.isHalfplane())
                return false;
            Point_dt p1 = t.p1(), p2 = t.p2(), p3 = t.p3();
            int f1 = p1.pointLineTest(pp1, pp2);
            int f2 = p2.pointLineTest(pp1, pp2);
            int f3 = p3.pointLineTest(pp1, pp2);

            if ((f1 == Point_dt.LEFT | f1 == Point_dt.RIGHT)
                    && (f1 == f2 && f1 == f3))
                return false;

            if (f1 != f2 && pp1.pointLineTest(p1, p2) != pp2.pointLineTest(p1, p2))
                return true;
            if (f2 != f3 && pp1.pointLineTest(p2, p3) != pp2.pointLineTest(p2, p3))
                return true;
            if (f3 != f1 && pp1.pointLineTest(p3, p1) != pp2.pointLineTest(p3, p1))
                return true;

            return ans;
        }

        /**
         * Add the intersections of triangle t with the section to the list of
         * intersection (set)
         */
        void cut(Triangle_dt t)
        {
            if (t.isHalfplane())
                return;
            Point_dt p1 = t.p1(), p2 = t.p2(), p3 = t.p3();
            int f1 = p1.pointLineTest(_p1, _p2);
            int f2 = p2.pointLineTest(_p1, _p2);
            int f3 = p3.pointLineTest(_p1, _p2);

            if ((f1 == Point_dt.LEFT | f1 == Point_dt.RIGHT)
                    && (f1 == f2 && f1 == f3))
                return;
            if (f1 != f2 && _p1.pointLineTest(p1, p2) != _p2.pointLineTest(p1, p2))
                Add(intersection(p1, p2));
            if (f2 != f3 && _p1.pointLineTest(p2, p3) != _p2.pointLineTest(p2, p3))
                Add(intersection(p2, p3));
            if (f3 != f1 && _p1.pointLineTest(p3, p1) != _p2.pointLineTest(p3, p1))
                Add(intersection(p3, p1));
        }

        void Add(Point_dt p)
        {
            int len = _section.Count;
            if (p != null
                    && (len == 0 || _p1.distance(p) > _p1.distance(_section
                            .ElementAt(len - 1))))
                _section.Add(p);
        }

        Point_dt intersection(Point_dt q1, Point_dt q2)
        {
            Point_dt ans = null;
            double x1 = _p1.x, x2 = _p2.x;
            double xx1 = q1.x, xx2 = q2.x;
            double dx = x2 - x1, dxx = xx2 - xx1;
            if (dx == 0 && dxx == 0)
            {
                ans = q1;
                if (q2.distance(_p1) < q1.distance(_p1))
                    ans = q2;
            }
            else if (dxx == 0)
            {
                ans = new Point_dt(q1.x, f(_p1, _p2, q1.x),
                        fz(_p1, _p2, q1.x));
            }
            else if (dx == 0)
            {
                ans = new Point_dt(_p1.x, f(q1, q2, _p1.x), fz(q1, q1, _p1.x));
            }
            else
            {
                double x = (k(_p1, _p2) - k(q1, q2)) / (m(q1, q2) - m(_p1, _p2));
                double y = m(_p1, _p2) * x + k(_p1, _p2);
                double z = mz(q1, q2) * x + kz(q1, q2);
                ans = new Point_dt(x, y, z);
            }
            return ans;
        }

        /** assume z = m*x + k (as a 2D XZ!! linear function) */
        private double mz(Point_dt p1, Point_dt p2)
        {
            double ans = 0;
            double dx = p2.x - p1.x, dz = p2.z - p1.z;
            if (dx != 0)
                ans = dz / dx;
            return ans;
        }

        private double kz(Point_dt p1, Point_dt p2)
        {
            double k = p1.z - mz(p1, p2) * p1.x;
            return k;
        }

        private double f(Point_dt p1, Point_dt p2, double x)
        {
            return m(p1, p2) * x + k(p1, p2);
        }

        private double fz(Point_dt p1, Point_dt p2, double x)
        {
            return mz(p1, p2) * x + kz(p1, p2);
        }

        /** assume y = m*x + k (as a 2D XY !! linear function) */
        private double m(Point_dt p1, Point_dt p2)
        {
            double ans = 0;
            double dx = p2.x - p1.x, dy = p2.y - p1.y;
            if (dx != 0)
                ans = dy / dx;
            return ans;
        }

        private double k(Point_dt p1, Point_dt p2)
        {
            double k = p1.y - m(p1, p2) * p1.x;
            return k;
        }

        /**
         * checks if a tower of height h1 at _p1 can see the tip of a tower of size
         * h2 at _p2
         */
        bool isVisible(double h1, double h2)
        {
            bool ans = false;
            if (_section != null)
            {
                ans = true;
                double z1 = _p1.z + h1, z2 = _p2.z + h2, dz = z2 - z1, dist = _p1
                        .distance(_p2);
                int len = _section.Count;
                for (int i = 0; i < len && ans; i++)
                {
                    Point_dt curr_p = _section.ElementAt(i);
                    double d = _p1.distance(curr_p);
                    // System.out.println(i+")  curr Z: "+(int)curr_p.z+"    sec z: "+(int)(z1+dz*(d/dist)));
                    if (curr_p.z > z1 + dz * (d / dist))
                        ans = false;
                }
            }
            return ans;
        }
    }
}
