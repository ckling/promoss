using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using JDT_NET.Delaunay_triangulation;

namespace JDT_NET.TESTING
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("Starting 3 time tests");
            for(int i = 0; i < 3; i++)
                DoTimeTest();
            Console.WriteLine("Starting 3 memory test");
            DoMemoryTest();
            Console.ReadLine();
        }

        static void DoMemoryTest()
        {
            int i = 0;
		    try
            {
			    Point_dt p1 = new Point_dt(1, 1);
			    Point_dt p3 = new Point_dt(1, 0);
			    Point_dt p2 = new Point_dt(0, 0);
			    List<Triangle_dt> vec = new List<Triangle_dt>();
			    while (true)
                {
				    Triangle_dt t = new Triangle_dt(p1, p2, p3);
				    vec.Add(t);
				    i++;
				    if (i % 10000 == 0)
					    Console.WriteLine(i);
		        }

		    }
            catch (OutOfMemoryException oome)
            {
                Console.WriteLine("out of MEMORY: points: " + i);
            }
            catch (Exception e)
            {
			    Console.WriteLine("out of MEMORY: points: " + i);
		    }
            
        }

        static void DoTimeTest()
        {
            int size = 100000, size2 = size;
		    double delta = 1000, delta2 = delta / 2;
		    double[] xx = new double[size], yy = new double[size];
		    Point_dt[] ps = new Point_dt[size];
		    double[] xx2 = new double[size2], yy2 = new double[size2];

		    DateTime start = DateTime.Now;
		    Delaunay_Triangulation ad = new Delaunay_Triangulation();

            Random r = new Random();
		    for (int i = 0; i < size; i++) {
			    xx[i] = (r.NextDouble() * delta - (delta * 0.1));
                yy[i] = (r.NextDouble() * delta - (delta * 0.1));

			    ps[i] = new Point_dt(xx[i], yy[i]);
			    ad.insertPoint(ps[i]);
		    }
		    DateTime mid = DateTime.Now;

		    for (int i = 0; i < size2; i++) {
                xx2[i] = (r.NextDouble() * delta2);
                yy2[i] = (r.NextDouble() * delta2);
		    }
		    DateTime m1 = DateTime.Now;
		    for (int i = 0; i < size2; i++) {
			    Point_dt p = new Point_dt(xx2[i], yy2[i]);
			    Triangle_dt t1 = ad.find(p);
			    if (!t1.contains(p)) {
                    Console.WriteLine(i + ") **ERR: find *** T: " + t1);
			    }
		    }
		    DateTime e1 = DateTime.Now;

		    Console.WriteLine("delaunay_triangulation " + ad.size() + " points, "
				    + ad.trianglesSize() + " triangles,  Triangles_td: "
				    + Triangle_dt._counter + "  ,c2: " + Triangle_dt._c2);
		    Console.WriteLine("Constructing time: " + (mid - start).TotalSeconds);
		    Console.WriteLine("*** E3 find:  time: " + (e1 - m1).TotalSeconds);
		    Console.WriteLine("delaunay_triangulation " + ad.size() + " points, "
				    + ad.trianglesSize() + " triangles,  Triangles_td: "
				    + Triangle_dt._counter + "  ,c2: " + Triangle_dt._c2);
        }
    }
}
