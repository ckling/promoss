using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace JDT_NET.Delaunay_triangulation
{
    public class Point_dt
    {
        double X, Y, Z;

        /**
         * Default Constructor.
         * constructs a 3D point at (0,0,0).
         */
        public Point_dt()
            : this(0, 0)
        {

        }

        /** 
         * constructs a 3D point 
         */
        public Point_dt(double x, double y, double z)
        {
            this.X = x;
            this.Y = y;
            this.Z = z;
        }

        /** constructs a 3D point with a z value of 0. */
        public Point_dt(double x, double y)
            : this(x, y, 0)
        {

        }

        /** simple copy constructor */
        public Point_dt(Point_dt p)
        {
            X = p.X;
            Y = p.Y;
            Z = p.Z;
        }

        /** returns the x-coordinate of this point. */
        public double x
        {
            get
            {
                return X;
            }
            set
            {
                this.X = value;
            }
        }

        /** returns the y-coordinate of this point. */
        public double y
        {
            get
            {
                return Y;
            }
            set
            {
                Y = value;
            }
        }

        /** returns the z-coordinate of this point. */
        public double z
        {
            get
            {
                return Z;
            }
            set
            {
                Z = value;
            }
        }

        public double distance2(Point_dt p)
        {
            return (p.X - X) * (p.X - X) + (p.Y - Y) * (p.Y - Y);
        }

        public double distance2(double px, double py)
        {
            return (px - X) * (px - X) + (py - Y) * (py - Y);
        }

        internal bool isLess(Point_dt p)
        {
            return (X < p.X) || ((X == p.X) && (Y < p.Y));
        }

        internal bool isGreater(Point_dt p)
        {
            return (X > p.X) || ((X == p.X) && (Y > p.Y));
        }

        /**
         * return true iff this point [x,y] coordinates are the same as p [x,y]
         * coordinates. (the z value is ignored).
         */
        public bool Equals(Point_dt p)
        {
            return (X == p.X) && (Y == p.Y);
        }

        public override bool Equals(object obj)
        {
            var p = (Point_dt) obj;
            return Equals(p);
        }

        public override int GetHashCode()
        {
            return X.GetHashCode() + Y.GetHashCode() + Z.GetHashCode();
        }

        /** return a String in the [x,y,z] format */
        public String toString()
        {
            return " Pt[" + X + "," + Y + "," + Z + "]";
        }

        public override string ToString()
        {
            return " Pt[" + X + "," + Y + "," + Z + "]";
        }

        /** @return the L2 distanse NOTE: 2D only!!! */
        public double distance(Point_dt p)
        {
            double temp = Math.Pow(p.x - X, 2) + Math.Pow(p.y - Y, 2);
            return Math.Sqrt(temp);
        }

        /** @return the L2 distanse NOTE: 2D only!!! */
        public double distance3D(Point_dt p)
        {
            double temp = Math.Pow(p.x - X, 2) + Math.Pow(p.y - Y, 2)
                    + Math.Pow(p.z - Z, 2);

            return Math.Sqrt(temp);
        }

        /** return a String: x y z (used by the save to file - write_tsin method). */
        public String toFile()
        {
            return ("" + X + " " + Y + " " + Z);
        }

        String toFileXY()
        {
            return ("" + X + " " + Y);
        }

        // pointLineTest
        // ===============
        // simple geometry to make things easy!
        /** �����a----+----b������ */
        public const int ONSEGMENT = 0;

        /**
         * + <br>
         * �����a---------b������
         * */
        public const int LEFT = 1;

        /**
         * �����a---------b������ <br>
         * +
         * */
        public const int RIGHT = 2;
        /** ��+��a---------b������ */
        public const int INFRONTOFA = 3;
        /** ������a---------b���+��� */
        public const int BEHINDB = 4;
        public const int ERROR = 5;

        /**
         * tests the relation between this point (as a 2D [x,y] point) and a 2D
         * segment a,b (the Z values are ignored), returns one of the following:
         * LEFT, RIGHT, INFRONTOFA, BEHINDB, ONSEGMENT
         * 
         * @param a
         *            the first point of the segment.
         * @param b
         *            the second point of the segment.
         * @return the value (flag) of the relation between this point and the a,b
         *         line-segment.
         */
        public int pointLineTest(Point_dt a, Point_dt b)
        {

            double dx = b.X - a.X;
            double dy = b.Y - a.Y;
            double res = dy * (X - a.X) - dx * (Y - a.Y);

            if (res < 0)
                return LEFT;
            if (res > 0)
                return RIGHT;

            if (dx > 0)
            {
                if (X < a.X)
                    return INFRONTOFA;
                if (b.X < X)
                    return BEHINDB;
                return ONSEGMENT;
            }
            if (dx < 0)
            {
                if (X > a.X)
                    return INFRONTOFA;
                if (b.X > X)
                    return BEHINDB;
                return ONSEGMENT;
            }
            if (dy > 0)
            {
                if (Y < a.Y)
                    return INFRONTOFA;
                if (b.Y < Y)
                    return BEHINDB;
                return ONSEGMENT;
            }
            if (dy < 0)
            {
                if (Y > a.Y)
                    return INFRONTOFA;
                if (b.Y > Y)
                    return BEHINDB;
                return ONSEGMENT;
            }
            Console.WriteLine("Error, pointLineTest with a=b");

            return ERROR;
        }

        bool areCollinear(Point_dt a, Point_dt b)
        {
            double dx = b.X - a.X;
            double dy = b.Y - a.Y;
            double res = dy * (X - a.X) - dx * (Y - a.Y);
            return res == 0;
        }

        /*
         * public ajSegment Bisector( ajPoint b) { double sx = (x+b.x)/2; double sy
         * = (y+b.y)/2; double dx = b.x-x; double dy = b.y-y; ajPoint p1 = new
         * ajPoint(sx-dy,sy+dx); ajPoint p2 = new ajPoint(sx+dy,sy-dx); return new
         * ajSegment( p1,p2 ); }
         */

        Point_dt circumcenter(Point_dt a, Point_dt b)
        {

            double u = ((a.X - b.X) * (a.X + b.X) + (a.Y - b.Y) * (a.Y + b.Y)) / 2.0f;
            double v = ((b.X - X) * (b.X + X) + (b.Y - Y) * (b.Y + Y)) / 2.0f;
            double den = (a.X - b.X) * (b.Y - Y) - (b.X - X) * (a.Y - b.Y);
            if (den == 0) // oops
                Console.WriteLine("circumcenter, degenerate case");
            return new Point_dt((u * (b.Y - Y) - v * (a.Y - b.Y)) / den, (v
                    * (a.X - b.X) - u * (b.X - X))
                    / den);
        }

        public static IComparer<Point_dt> getComparator(int flag)
        {
            return new CCompare(flag);
        }

        public static IComparer<Point_dt> getComparator()
        {
            return new CCompare(0);
        }
    }

    class CCompare : IComparer<Point_dt>
    {
        private int _flag;

        public CCompare(int i)
        {
            _flag = i;
        }

        /** compare between two points. */
        public int compare(object o1, object o2)
        {
            //object o1 = this;

            int ans = 0;
            if (o1 != null && o2 != null && o1 is Point_dt && o2 is Point_dt)
            {
                Point_dt d1 = (Point_dt)o1;
                Point_dt d2 = (Point_dt)o2;
                if (_flag == 0)
                {
                    if (d1.x > d2.x)
                        return 1;
                    if (d1.x < d2.x)
                        return -1;
                    // x1 == x2
                    if (d1.y > d2.y)
                        return 1;
                    if (d1.y < d2.y)
                        return -1;
                }
                else if (_flag == 1)
                {
                    if (d1.x > d2.x)
                        return -1;
                    if (d1.x < d2.x)
                        return 1;
                    // x1 == x2
                    if (d1.y > d2.y)
                        return -1;
                    if (d1.y < d2.y)
                        return 1;
                }
                else if (_flag == 2)
                {
                    if (d1.y > d2.y)
                        return 1;
                    if (d1.y < d2.y)
                        return -1;
                    // y1 == y2
                    if (d1.x > d2.x)
                        return 1;
                    if (d1.x < d2.x)
                        return -1;

                }
                else if (_flag == 3)
                {
                    if (d1.y > d2.y)
                        return -1;
                    if (d1.y < d2.y)
                        return 1;
                    // y1 == y2
                    if (d1.x > d2.x)
                        return -1;
                    if (d1.x < d2.x)
                        return 1;
                }
                else
                {
                    if (o1 == null && o2 == null)
                        return 0;
                    if (o1 == null && o2 != null)
                        return 1;
                    if (o1 != null && o2 == null)
                        return -1;
                }
            }

            return ans;
        }

        #region IComparer<Point_dt> Members

        public int Compare(Point_dt o1, Point_dt o2)
        {

            int ans = 0;
            if (o1 != null && o2 != null && o1 is Point_dt && o2 is Point_dt)
            {
                Point_dt d1 = (Point_dt)o1;
                Point_dt d2 = (Point_dt)o2;
                if (_flag == 0)
                {
                    if (d1.x > d2.x)
                        return 1;
                    if (d1.x < d2.x)
                        return -1;
                    // x1 == x2
                    if (d1.y > d2.y)
                        return 1;
                    if (d1.y < d2.y)
                        return -1;
                }
                else if (_flag == 1)
                {
                    if (d1.x > d2.x)
                        return -1;
                    if (d1.x < d2.x)
                        return 1;
                    // x1 == x2
                    if (d1.y > d2.y)
                        return -1;
                    if (d1.y < d2.y)
                        return 1;
                }
                else if (_flag == 2)
                {
                    if (d1.y > d2.y)
                        return 1;
                    if (d1.y < d2.y)
                        return -1;
                    // y1 == y2
                    if (d1.x > d2.x)
                        return 1;
                    if (d1.x < d2.x)
                        return -1;

                }
                else if (_flag == 3)
                {
                    if (d1.y > d2.y)
                        return -1;
                    if (d1.y < d2.y)
                        return 1;
                    // y1 == y2
                    if (d1.x > d2.x)
                        return -1;
                    if (d1.x < d2.x)
                        return 1;
                }
                else
                {
                    if (o1 == null && o2 == null)
                        return 0;
                    if (o1 == null && o2 != null)
                        return 1;
                    if (o1 != null && o2 == null)
                        return -1;
                }
            }

            return ans;
        }

        #endregion
    }
}
