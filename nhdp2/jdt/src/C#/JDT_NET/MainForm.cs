using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using JDT_NET.Delaunay_triangulation;

namespace JDT_NET.GUI
{
    public partial class MainForm : Form
    {
        private static long serialVersionUID = 1L;

        // *** private data ***
        public const int POINT = 1, FIND = 2, VIEW1 = 3, VIEW2 = 4,
                VIEW3 = 5, VIEW4 = 6, SECTION1 = 7, SECTION2 = 8, GUARD = 9,
                CLIENT = 10;
        private int _stage, _view_flag = VIEW1, _mc = 0;
        private Triangle_dt _t1, _t2; // tmp triangle for find testing for selection
        private Delaunay_Triangulation _ajd = null;
        protected List<Point_dt> _clients, _guards;
        protected Point_dt _dx_f, _dy_f, _dx_map, _dy_map, _p1, _p2;// ,_guard=null,
        // _client=null;
        protected bool _visible = false;
        private double _topo_dz = 100.0, GH = 30, CH = 5;
        // private List<Triangle_dt> _tr = null;//new List<Triangle_dt>();
        private Visibility _los;// , _section2;


        // *** text area ***
        public MainForm()
        {
            InitializeComponent();

            this.Text = "Delaunay GUI tester";
            this.Size = new Size(500, 500);
            this.BackColor = Color.White;
            _stage = 0;
            _ajd = new Delaunay_Triangulation();
            _ajd.PotentialBbMax = new Point_dt(this.Width, this.Height);

            _dx_f = new Point_dt(10, this.Width - 10);
            _dy_f = new Point_dt(55, this.Height - 10);
            _dx_map = new Point_dt(_dx_f);
            _dy_map = new Point_dt(_dy_f);

            //addWindowListener(new WindowAdapter() {
            //    public void windowClosing(WindowEvent e) {
            //        System.exit(0);
            //    }
            //});
        }

        public MainForm(Delaunay_Triangulation aj)
        {
            this.Text = "ajDelaunay GUI tester";
            this.Size = new Size(550, 550);
            _stage = 0;
            _ajd = aj;
            _dx_f = new Point_dt(5, this.Width - 5);
            _dy_f = new Point_dt(5, this.Height - 5);
            _dx_map = new Point_dt(aj.maxBoundingBox().x, aj.minBoundingBox().x);
            _dy_map = new Point_dt(aj.maxBoundingBox().y, aj.minBoundingBox().y);
            _clients = null;
            _guards = null;
            //addWindowListener(new WindowAdapter() {
            //    public void windowClosing(WindowEvent e) {
            //        System.exit(0);
            //    }
            //});
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            base.OnPaint(e);

            Graphics g = e.Graphics;

            // _ajd.initTriangle();
            // ajTriangle[] tt = _ajd._triangles;
            if (_ajd == null || _ajd.size() == 0)
                return;
            _dx_f = new Point_dt(5, this.Width - 5);
            _dy_f = new Point_dt(5, this.Height - 5);

            List<Triangle_dt> it = _ajd.trianglesIterator();
            foreach (Triangle_dt curr in it)
            {
                if (!curr.isHalfplane())
                    drawTriangle(g, curr, Color.Empty);
            }
            it = _ajd.trianglesIterator();
            foreach(Triangle_dt curr in it)
            {
                if (curr.isHalfplane())
                    drawTriangle(g, curr, Color.Empty);
            }
            if (_t2 != null)
                drawTriangle(g, _t2, Color.Red);
            if (_t1 != null && _stage == FIND)
                drawTriangle(g, _t1, Color.YellowGreen);
            if (this._view_flag == VIEW3)
                drawTopo(g);

            // debug
            if (_mc < _ajd.getModeCounter())
            {
                _mc = _ajd.getModeCounter();

                List<Triangle_dt> it2 = _ajd.getLastUpdatedTriangles();
                for (int i = 0; i < it2.Count; i++)
                {
                    drawTriangle(g, it2[i], Color.Cyan);
                }

                Console.WriteLine("   MC: " + _mc + "  number of triangles updated: " + it2.Count);

            }

            if (_los != null && (_stage == SECTION1 || _stage == SECTION2))
            {
                if (_los != null && _los._tr != null)
                {
                    foreach (Triangle_dt curr in _los._tr)
                    {
                        if (!curr.isHalfplane())
                            drawTriangle(g, curr, Color.Red);
                    }
                }

                for(int i = 0; i < _los._section.Count; i++)
                {
                    Point_dt curr_p = _los._section[i];

                    if (curr_p != null)
                    {
                        drawPoint(g, curr_p, Color.Blue);
                        Console.WriteLine(i + ") " + curr_p + "  dist _p1: " + _p1.distance(curr_p));
                    }
                }

                drawLine(g, _p1, _p2, Color.Blue);
            }
            /*
             * if(_stage == GUARD | _stage == CLIENT) { if(_p1!=null)
             * drawPoint(g,_p1,6,Color.ORANGE); if(_p2!=null) { if(_visible)
             * drawPoint(g,_p2,6,Color.BLUE); else drawPoint(g,_p2,6, Color.RED); }
             * }
             */
            if (_los == null)
                _los = new Visibility(_ajd);
            if (_stage == GUARD || _stage == CLIENT)
            {
                int[] ccc = new int[0];
                if (_clients != null)
                    ccc = new int[_clients.Count];
                for (int gr = 0; _guards != null && gr < _guards.Count; gr++)
                {
                    Point_dt gg = _guards.ElementAt(gr);
                    drawPoint(g, gg, 8, Color.Orange);

                    for (int c = 0; _clients != null && c < _clients.Count; c++)
                    {
                        Point_dt cc = _clients.ElementAt(c);
                        drawPoint(g, cc, 6, Color.White);
                        // Color cl = Color.RED;
                        if (_los.los(gg, cc))
                        {
                            this.drawLine(g, gg, cc, Color.Red);
                            ccc[c]++;
                        }
                    }
                }

                int c1 = 0, c2 = 0, c3 = 0;
                for (int i = 0; i < ccc.Length; i++)
                {
                    if (ccc[i] > 0)
                    {
                        c1++;
                        c2 += ccc[i];
                    }
                }
                if (c1 > 0)
                    Console.WriteLine("clients:" + ccc.Length + "  visible c:" + c1 + "   ave:" + c2 / c1);
            }

        }

        void drawTopo(Graphics g)
        {
            List<Triangle_dt> it = _ajd.trianglesIterator();
            
            foreach (Triangle_dt curr in it)
            {
                if (!curr.isHalfplane())
                    drawTriangleTopoLines(g, curr, this._topo_dz, Color.Red);
            }
        }

        void drawTriangleTopoLines(Graphics g, Triangle_dt t, double dz, Color cl)
        {
            if (t.p1().z < 0 | t.p2().z < 0 | t.p3().z < 0)
                return;

            Point_dt[] p12 = computePoints(t.p1(), t.p2(), dz);
            Point_dt[] p23 = computePoints(t.p2(), t.p3(), dz);
            Point_dt[] p31 = computePoints(t.p3(), t.p1(), dz);

            int i12 = 0, i23 = 0, i31 = 0;
            bool cont = true;
            while (cont)
            {
                cont = false;

                if (i12 < p12.Length && i23 < p23.Length && p12[i12].z == p23[i23].z)
                {
                    if (p12[i12].z % 200 > 100)
                        drawLine(g, p12[i12], p23[i23], Color.Red);
                    else
                        drawLine(g, p12[i12], p23[i23], Color.Yellow);
                    i12++;
                    i23++;
                    cont = true;
                }
                if (i23 < p23.Length && i31 < p31.Length && p23[i23].z == p31[i31].z)
                {
                    if (p23[i23].z % 200 > 100)
                        drawLine(g, p23[i23], p31[i31], Color.Red);
                    else
                        drawLine(g, p23[i23], p31[i31], Color.Yellow);

                    i23++;
                    i31++;
                    cont = true;
                }
                if (i12 < p12.Length && i31 < p31.Length && p12[i12].z == p31[i31].z)
                {
                    if (p12[i12].z % 200 > 100)
                        drawLine(g, p12[i12], p31[i31], Color.Red);
                    else
                        drawLine(g, p12[i12], p31[i31], Color.Yellow);

                    i12++;
                    i31++;
                    cont = true;
                }
            }
        }

        Point_dt[] computePoints(Point_dt p1, Point_dt p2, double dz)
        {
            Point_dt[] ans = new Point_dt[0];
            double z1 = Math.Min(p1.z, p2.z), z2 = Math.Max(p1.z, p2.z);
            if (z1 == z2)
                return ans;
            double zz1 = ((int)(z1 / dz)) * dz;
            if (zz1 < z1)
                zz1 += dz;
            double zz2 = ((int)(z2 / dz)) * dz;
            int len = (int)((zz2 - zz1) / dz) + 1, i = 0;
            ans = new Point_dt[len];
            double DZ = p2.z - p1.z, DX = p2.x - p1.x, DY = p2.y - p1.y;
            for (double z = zz1; z <= zz2; z += dz)
            {
                double scale = (z - p1.z) / DZ;
                double x = p1.x + DX * scale;
                double y = p1.y + DY * scale;
                ans[i] = new Point_dt(x, y, z);
                i++;
            }
            return ans;
        }

        public void drawTriangle(Graphics g, Triangle_dt t, Color cl)
        {
            if (_view_flag == VIEW1 || t.isHalfplane())
            {
                if (t.isHalfplane())
                {
                    if (cl == Color.Empty)
                        drawLine(g, t.p1(), t.p2(), Color.Blue);
                    else
                        drawLine(g, t.p1(), t.p2(), cl);
                }
                else
                {
                    if (cl == Color.Empty)
                    {
                        drawLine(g, t.p1(), t.p2(), Color.Black);
                        drawLine(g, t.p2(), t.p3(), Color.Black);
                        drawLine(g, t.p3(), t.p1(), Color.Black);
                    }
                    
                    {
                        drawLine(g, t.p1(), t.p2(), cl);
                        drawLine(g, t.p2(), t.p3(), cl);
                        drawLine(g, t.p3(), t.p1(), cl);
                    }
                }
            }
            else
            {
                // //////////////////////////////////////////////////////////////////
                double maxZ = _ajd.maxBoundingBox().z;
                double minZ = _ajd.minBoundingBox().z;
                double z = (t.p1().z + t.p2().z + t.p3().z) / 3.0;
                double dz = maxZ - minZ;
                if (dz == 0)
                    dz = 1;
                int co = 30 + (int)(220 * ((z - minZ) / dz));
                if (cl == Color.Empty)
                    cl = Color.FromArgb(co, co, co);

                int[] xx = new int[3], yy = new int[3];
                // double f = 0;
                // double dx_map = _dx_map.y- _dx_map.x;
                // double dy_map = _dy_map.y- _dy_map.x;

                // f = (t.p1().x -_dx_map.x)/dx_map;
                Point_dt p1 = world2screen(t.p1());
                xx[0] = (int)p1.x;
                yy[0] = (int)p1.y;
                Point_dt p2 = world2screen(t.p2());
                xx[1] = (int)p2.x;
                yy[1] = (int)p2.y;
                Point_dt p3 = world2screen(t.p3());
                xx[2] = (int)p3.x;
                yy[2] = (int)p3.y;

                Brush b = new SolidBrush(cl);
                g.FillPolygon(b, ToPoints(xx, yy));
                // ////////////////////////////////////
            }
        }

        private Point[] ToPoints(int[] x, int[] y)
        {
            if (x == null || y == null || (x.Length != y.Length))
                throw new Exception("ToPoints");

            Point[] t = new Point[x.Length];
            for (int i = 0; i < x.Length; i++)
                t[i] = new Point(x[i], y[i]);

            return t;
        }

        public void drawLine(Graphics g, Point_dt p1, Point_dt p2, Color cl)
        {
            // g.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            Point_dt t1 = this.world2screen(p1);
            Point_dt t2 = this.world2screen(p2);

            g.DrawLine(new Pen(cl), (int)t1.x, (int)t1.y, (int)t2.x, (int)t2.y);
        }

        public void drawPoint(Graphics g, Point_dt p1, Color cl)
        {
            drawPoint(g, p1, 4, cl);
        }

        public void drawPoint(Graphics g, Point_dt p1, int r, Color cl)
        {
            Point_dt t1 = this.world2screen(p1);
            Brush b = new SolidBrush(cl);
            g.FillEllipse(b, (int)t1.x - r / 2, (int)t1.y - r / 2, r, r);
        }

        protected override void OnLoad(EventArgs e)
        {
            Dialog();
        }

        public void Dialog()
        {
            MainMenu menu = new MainMenu();

            MenuItem m = new MenuItem("File");
            MenuItem m1;
            m1 = new MenuItem("Open");
            m1.Click += MainMenuClick;
            m.MenuItems.Add(m1);
            m1 = new MenuItem("Save tsin");
            m1.Click += MainMenuClick;
            m.MenuItems.Add(m1);
            m1 = new MenuItem("Save smf");
            m1.Click += MainMenuClick;
            m.MenuItems.Add(m1);
            MenuItem m6 = new MenuItem("Clear");
            m6.Click += MainMenuClick;
            m.MenuItems.Add(m6);
            MenuItem m2 = new MenuItem("Exit");
            m2.Click += MainMenuClick;
            m.MenuItems.Add(m2);
            menu.MenuItems.Add(m);

            m = new MenuItem("Input");
            MenuItem m3 = new MenuItem("Point");
            m3.Click += MainMenuClick;
            m.MenuItems.Add(m3);
            m3 = new MenuItem("100-rand-ps");
            m3.Click += MainMenuClick;
            m.MenuItems.Add(m3);
            m3 = new MenuItem("Guard-30m");
            m3.Click += MainMenuClick;
            m.MenuItems.Add(m3);
            m3 = new MenuItem("Client-5m");
            m3.Click += MainMenuClick;
            m.MenuItems.Add(m3);
            menu.MenuItems.Add(m);

            m = new MenuItem("View");
            m3 = new MenuItem("Lines");
            m3.Click += MainMenuClick;
            m.MenuItems.Add(m3);
            m3 = new MenuItem("Triangles");
            m3.Click += MainMenuClick;
            m.MenuItems.Add(m3);
            m3 = new MenuItem("Topo");
            m3.Click += MainMenuClick;
            m.MenuItems.Add(m3);
            MenuItem m4 = new MenuItem("Find");
            m4.Click += MainMenuClick;
            m.MenuItems.Add(m4);
            m4 = new MenuItem("Section");
            m4.Click += MainMenuClick;
            m.MenuItems.Add(m4);
            m4 = new MenuItem("Info");
            m4.Click += MainMenuClick;
            m.MenuItems.Add(m4);
            m4 = new MenuItem("CH");
            m4.Click += MainMenuClick;
            m.MenuItems.Add(m4);
            menu.MenuItems.Add(m);

            this.Menu = menu;
            this.MouseClick += MainForm_Click;
        }

        void MainForm_Click(object sender, MouseEventArgs e)
        {
            int xx = e.X;
            int yy = e.Y;

            switch (_stage)
            {
                case 0:
                    {
                        Console.WriteLine("[" + xx + "," + yy + "]");
                        break;
                    }
                case POINT:
                    {
                        Point_dt q = new Point_dt(xx, yy);
                        Point_dt p = screen2world(q);
                        _ajd.insertPoint(p);
                        Refresh();
                        break;
                    }
                case FIND:
                    {
                        Point_dt q = new Point_dt(xx, yy);
                        Point_dt p = screen2world(q);
                        //_t1 = _ajd.find(p);
                        _t1 = _ajd.FastFind(p);                        
                        Refresh();
                        break;
                    }
                case SECTION1:
                    {
                        Point_dt q = new Point_dt(xx, yy);
                        _p1 = screen2world(q);
                        // _p1 = new Point_dt(99792.03,1073355.0,30.0);

                        // _t1 = _ajd.find(_p1);
                        _stage = SECTION2;
                        break;
                    }
                case SECTION2:
                    {
                        Point_dt q = new Point_dt(xx, yy);
                        _p2 = screen2world(q);
                        // _p2 = new Point_dt(149587.055,1040477.0,5.0);

                        // _t2 = _ajd.find(_p2);
                        _los = new Visibility(_ajd);
                        _los.computeSection(_p1, _p2);
                        Refresh();
                        _stage = SECTION1;
                        break;
                    }
                case GUARD:
                    {
                        Point_dt q = new Point_dt(xx, yy);
                        _p1 = screen2world(q);
                        if (_guards == null)
                            _guards = new List<Point_dt>();
                        _guards.Add(new Point_dt(_p1.x, _p1.y, GH));
                        /*
                         * if(_p2!=null) { _los = new Visibility(_ajd);
                         * _los.computeSection(_p1,_p2); _visible =
                         * _los.isVisible(30,5); }
                         */
                        Refresh();
                        break;
                    }
                case CLIENT:
                    {
                        Point_dt q = new Point_dt(xx, yy);
                        _p2 = screen2world(q);
                        if (_clients == null)
                            _clients = new List<Point_dt>();
                        _clients.Add(new Point_dt(_p2.x, _p2.y, CH));
                        /*
                         * if(_p1!=null) { _los = new Visibility(_ajd);
                         * _los.computeSection(_p1,_p2); _visible =
                         * _los.isVisible(30,5); }
                         */
                        Refresh();
                        break;
                    }
            }
        }

        void MainMenuClick(object sender, EventArgs e)
        {
            MenuItem mi = sender as MenuItem;
            if (mi == null)
                throw new Exception("MainMenuClick");

            string arg = mi.Text;

            if (arg.Equals("Open"))
                openTextFile();
            else if (arg.Equals("Save tsin"))
                saveTextFile();
            else if (arg.Equals("Save smf"))
                saveTextFile2();
            else if (arg.Equals("Lines"))
            {
                this._view_flag = VIEW1;
                Refresh();
            }
            else if (arg.Equals("Triangles"))
            {
                this._view_flag = VIEW2;
                Refresh();
            }
            else if (arg.Equals("Topo"))
            {
                this._view_flag = VIEW3;
                Refresh();
            }
            else if (arg.Equals("Clear"))
            {
                _ajd = new Delaunay_Triangulation();
                _dx_map = new Point_dt(_dx_f);
                _dy_map = new Point_dt(_dy_f);
                _clients = null;
                _guards = null;
                _mc = 0;
                Refresh();

            }
            else if (arg.Equals("Exit"))
            {
                Application.Exit();
            }

            else if (arg.Equals("Point"))
            {
                _stage = POINT;
            }
            else if (arg.Equals("CH"))
            {
                _ajd.CH_vertices_Iterator();
            }
            else if (arg.Equals("100-rand-ps"))
            {
                Random r = new Random();
                double x0 = 10, y0 = 60, dx = this.Width - x0 - 10, dy = this.Height - y0 - 10;
                for (int i = 0; i < 100; i++)
                {
                    double x = r.NextDouble() * dx + x0;
                    double y = r.NextDouble() * dy + y0;
                    Point_dt q = new Point_dt(x, y);
                    Point_dt p = screen2world(q);
                    _ajd.insertPoint(p);
                }

                Refresh();
            }
            else if (arg.Equals("Find"))
            {
                _stage = MainForm.FIND;
            }
            else if (arg.Equals("Section"))
            {
                _stage = MainForm.SECTION1;
            }
            else if (arg.Equals("Client-5m"))
            {
                // Console.WriteLine("CL!");
                _stage = MainForm.CLIENT;

            }
            else if (arg.Equals("Guard-30m"))
            {
                _stage = MainForm.GUARD;
            }
            else if (arg.Equals("Info"))
            {
                string ans = "" + _ajd.GetType().FullName
                        + "  # vertices:" + _ajd.size() + "  # triangles:"
                        + _ajd.trianglesSize();
                ans += "   min BB:" + _ajd.minBoundingBox() + "   max BB:"
                        + _ajd.maxBoundingBox();
                Console.WriteLine(ans);
                Console.WriteLine();
            }
        }

        // *** private methodes - random points obs ****

        // ********** Private methodes (open,save...) ********

        private void openTextFile()
        {
            _stage = 0;
            OpenFileDialog d = new OpenFileDialog();
            DialogResult dr = d.ShowDialog();
            if (dr != DialogResult.OK)
                return;

            string fi = d.FileName;
            _clients = null;
            _guards = null;

            if (!string.IsNullOrEmpty(fi)) // the user actualy choose a file.
            {
                try
                {
                    _ajd = new Delaunay_Triangulation(fi);
                    _dx_map = new Point_dt(_ajd.minBoundingBox().x, _ajd.maxBoundingBox().x);
                    _dy_map = new Point_dt(_ajd.minBoundingBox().y, _ajd.maxBoundingBox().y);

                    Refresh();
                }
                catch (Exception e)  // in case something went wrong.
                {
                    Console.WriteLine("** Error while reading text file **");
                    Console.WriteLine(e);
                }
            }
        }

        private void saveTextFile()
        {
            _stage = 0;
            SaveFileDialog d = new SaveFileDialog();

            if (d.ShowDialog() != DialogResult.OK)
                return;

            string fi = d.FileName;
            if (fi != null)
            {
                try
                {
                    _ajd.write_tsin(fi);
                }
                catch (Exception e)
                {
                    Console.WriteLine("ERR cant save to text file: " + fi);
                    Console.WriteLine(e.StackTrace);
                }
            }
        }

        public void saveTextFile2()
        {
            _stage = 0;
            SaveFileDialog d = new SaveFileDialog();
            if (d.ShowDialog() != DialogResult.OK)
                return;

            string fi = d.FileName;
            if (fi != null)
            {
                try
                {
                    _ajd.write_smf(fi);
                }
                catch (Exception e)
                {
                    Console.WriteLine("ERR cant save to text file: " + fi);
                    Console.WriteLine(e.StackTrace);
                }
            }
        }

        Point_dt screen2world(Point_dt p)
        {
            double x = transform(_dx_f, p.x, _dx_map);
            double y = transformY(_dy_f, p.y, _dy_map);
            return new Point_dt(x, y);
        }

        Point_dt world2screen(Point_dt p)
        {
            double x = transform(_dx_map, p.x, _dx_f);
            double y = transformY(_dy_map, p.y, _dy_f);
            return new Point_dt(x, y);
        }

        /**
         * transforms the point p from the Rectangle th into this Rectangle, Note:
         * r.contains(p) must be true! assume p.x
         * < p
         * .y
         * 
         * */

        static double transform(Point_dt range, double x, Point_dt new_range)
        {
            double dx1 = range.y - range.x;
            double dx2 = new_range.y - new_range.x;

            double scale = (x - range.x) / dx1;
            double ans = new_range.x + dx2 * scale;
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

        static double transformY(Point_dt range, double x, Point_dt new_range)
        {
            double dy1 = range.y - range.x;
            double dy2 = new_range.y - new_range.x;

            double scale = (x - range.x) / dy1;
            double ans = new_range.y - dy2 * scale;
            return ans;
        }
    }
}


