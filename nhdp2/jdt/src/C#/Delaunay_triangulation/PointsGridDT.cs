/*************************************************
 * Written by:  Tal Shargal
 * Date:        25/12/09
 *************************************************/

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
//using C5;

namespace JDT_NET.Delaunay_triangulation
{
    public class PointsGridDT
    {
        /*********************  members      **********************/        
        private Delaunay_Triangulation _dt;
        private int _dtMc;

        private Dictionary<Point_dt,Triangle_dt> _points2Triangles;        
        private const int DefaultMatrixSize = 5;
        private readonly int _matrixSize;

        private bool _preCalculated = false;    // flag whether a precalculation was done
        private Point_dt _maxPoint;
        private decimal _xInterval;
        private decimal _yInterval;

        public Delaunay_Triangulation DelaunayTriangulation
        {
            get { return _dt; }
            set
            {
                _dt = value;
                _dtMc = _dt != null ? _dt.getModeCounter() : 0;
            }
        }

        /*********************  public methods **********************/        

        public PointsGridDT() : this(DefaultMatrixSize)
        {            
        }

        /// <summary>
        /// Constructor of Points grid. Doesn't call Precalculate!
        /// </summary>
        /// <param name="matrixSize">at least 2</param>
        public PointsGridDT(int matrixSize)
        {
            if (matrixSize<2)
            {
                throw new ArgumentException("matrixSize must be greater than 1");
            }
            _matrixSize = matrixSize;
            _points2Triangles = new Dictionary<Point_dt, Triangle_dt>(_matrixSize * _matrixSize);            
            DelaunayTriangulation = null;            
        }

        /// <summary>
        /// Constructor of Points grid. Doesn't call Precalculate!
        /// </summary>
        /// <param name="matrixSize">at least 2</param>
        /// <param name="delaunayTriangulation">triangulation to work on</param>
        public PointsGridDT(int matrixSize, Delaunay_Triangulation delaunayTriangulation)
        {
            _matrixSize = matrixSize;
            _points2Triangles = new Dictionary<Point_dt, Triangle_dt>(_matrixSize * _matrixSize);            
            DelaunayTriangulation = delaunayTriangulation;
        }

        /// <summary>
        /// Returns the closest triangle to the target point according the grid.
        /// Use this method to find a start triangle for the complete find method of DT
        /// </summary>
        /// <param name="p">the target point</param>
        /// <returns>Closest triangle from the grid</returns>
        public Triangle_dt FindClosestTriangle(Point_dt p)
        {
            if (!_preCalculated) // if no grid was build
            {
                PreCalculate();
            }

            if (_dtMc != _dt.getModeCounter())  // if the grid isn't updated
            {
                UpdateGrid();
            }

            return _points2Triangles[FindClosestPoint(p)];
        }

        /*********************  private methods **********************/
        private void PreCalculate()
        {
            if (_dt == null)
            {
                throw new InvalidOperationException("Delaunay_Triangulation must be set before calling this method");
            }

            _points2Triangles.Clear();            

            _maxPoint = _dt.PotentialBbMax;

            _xInterval = (decimal)_maxPoint.x / (_matrixSize - 1);
            _yInterval = (decimal)_maxPoint.y / (_matrixSize - 1);

            // build grid of points - triangle for each point
            for (decimal xAxis = 0; xAxis <= (int)Math.Floor(_maxPoint.x); xAxis += _xInterval)            
            {
                for (decimal yAxis = 0; yAxis <= (int)Math.Floor(_maxPoint.y); yAxis += _yInterval)
                {
                    var anchorPoint = new Point_dt((double) xAxis, (double) yAxis);
                    var correspondTriangle = _dt.find(anchorPoint);
                    _points2Triangles[anchorPoint] = correspondTriangle;                    
                }
            }

            _preCalculated = true;
            _dtMc = _dt.getModeCounter();
        }

        /// <summary>
        /// updates each point on the grid
        /// </summary>
        private void UpdateGrid()
        {            
            for (decimal xAxis = 0; xAxis <= (int)Math.Floor(_maxPoint.x); xAxis += _xInterval)
            {
                for (decimal yAxis = 0; yAxis <= (int)Math.Floor(_maxPoint.y); yAxis += _yInterval)
                {
                    var anchorPoint = new Point_dt((double)xAxis, (double)yAxis);
                    if (!IsRelativeTriangleStillExists(anchorPoint))    // if the triangle that match to this point isn't in the current dt - update to the new one
                    {                       
                        var correspondTriangle = _dt.find(anchorPoint);                        
                        _points2Triangles[anchorPoint] = correspondTriangle;                        
                    }
                }
            }
            
            _dtMc = _dt.getModeCounter();
        }

        /// <summary>
        /// </summary>        
        /// <returns>true iff the corresonding triangle is exist in the current DT</returns>
        private bool IsRelativeTriangleStillExists(Point_dt p)
        {
            if (!_points2Triangles.ContainsKey(p))
                return false;
            
            return _dt.trianglesIterator().Contains(_points2Triangles[p]);
        }        

        /// <summary>
        /// calculates the "lower left" point of the cell contains point p
        /// </summary>        
        private Point_dt FindClosestPoint(Point_dt p)
        {            
            var basePointX = Math.Floor((decimal)p.x / _xInterval) * _xInterval;
            var basePointY = Math.Floor((decimal)p.y / _yInterval) * _yInterval;
            var basePoint = new Point_dt((double) basePointX, (double) basePointY);
            return basePoint;
        }
    }
}

