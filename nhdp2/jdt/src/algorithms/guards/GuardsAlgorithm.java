package algorithms.guards;

import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Triangle_dt;

import java.util.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Created by IntelliJ IDEA.
 * User: Lior Talker
 * Date: 02/12/2009
 * Time: 18:15:44
 * To change this template use File | Settings | File Templates.
 */
public class GuardsAlgorithm {

    // this function selects guards to watch diamonds - the minumum amount of guards that can watch all the diamonds.
    // the implementation is greedy.
    public int[] selectGuardsToWatchDiamonds(Delaunay_Triangulation dt, Point_dt[] guards, Point_dt[] diamonds) {
		int alreadyCovered = 0;
		int currentMaxGuardIndex = -1;
		int resultSize = 0;
		//temporary result array
		int[] tempArray = new int[guards.length];
		//result array
		int[] resultArray;

		PolygonGuardMatrix matrix = new PolygonGuardMatrix(guards.length, diamonds.length);
		
		//set matrix with visible values
		for(int i = 0; i < matrix.m_numGuards; ++i)
		{
			for(int j = 0; j < matrix.m_numElements; ++j)
			{
				matrix.m_indicesMatrix[i][j] =
					visible(guards[i].x(), guards[i].y(), diamonds[j].x(), diamonds[j].y(), dt);
			}
		}

		//main loop
		while(alreadyCovered != diamonds.length && resultSize < guards.length)
		{
			currentMaxGuardIndex = matrix.MaxGuardIndex();
			if(currentMaxGuardIndex >= 0)
			{
				tempArray[resultSize] = currentMaxGuardIndex;
				resultSize++;
				alreadyCovered += matrix.NumElementsGuarded(currentMaxGuardIndex);
				matrix.Update(currentMaxGuardIndex);
			}
			else
			{
				break;
			}
		}

		//create result array with actual size
		resultArray = new int[resultSize];
		System.arraycopy(tempArray, 0, resultArray, 0, resultSize);

		return resultArray;
    }

   // this is the main visibility function
    private boolean visible(double guardX, double guardY, double diamondX, double diamondY, Delaunay_Triangulation dt) {

        List<Line2D.Double> onSightLines;
        // get the 2D lines in the line between the guard and the diamond
        onSightLines = getOnSightLines(guardX,guardY,diamondX,diamondY,dt);
        // guard's height
        double guardZ = dt.z(guardX,guardY);
        //diamond's height
        double diamondZ = dt.z(diamondX,diamondY);
        // diffrential of a line
        double diff = (guardZ - diamondZ)/(Math.sqrt(Math.pow((guardX-diamondX),2) + Math.pow((guardY-diamondY),2)));
        Point2D intrest1, intrest2;
        double lineOfSightZ1, lineOfSightZ2,triangleZ1,triangleZ2;

       // for each 2D line check if intersection exists
       for (Line2D line : onSightLines) {
            intrest1 = line.getP1();
            intrest2 = line.getP2();
            lineOfSightZ1 = guardZ + diff*(Math.sqrt(Math.pow((guardX-intrest1.getX()),2) + Math.pow((guardY-intrest1.getY()),2)));
            lineOfSightZ2 = guardZ + diff*(Math.sqrt(Math.pow((guardX-intrest2.getX()),2) + Math.pow((guardY-intrest2.getY()),2)));
            triangleZ1 = dt.z(intrest1.getX(),intrest1.getY());
            triangleZ2 = dt.z(intrest2.getX(),intrest2.getY());

            if (Math.max(lineOfSightZ1,lineOfSightZ2) < Math.max(triangleZ1,triangleZ2)) {
                return false;
            }
        }

        // if no intersection found
        return true;

    }

    // look for the next triangle to "work with" after advancing in the line of sight
    private Triangle_dt chooseNextTriangle(Line2D sightLine, Triangle_dt nextTriangle, Delaunay_Triangulation dt) {
        Triangle_dt neigbour1 = nextTriangle.next_12();
        Triangle_dt neigbour2 = nextTriangle.next_23();
        Triangle_dt neigbour3 = nextTriangle.next_31();
        Line2D.Double n1l1, n1l2, n1l3 ,n2l1 , n2l2  ,n2l3 ,n3l1 ,n3l2  ,n3l3;
        Point2D p11;
        Point2D p12;
        Point2D p13;
        Point2D p21;
        Point2D p22;
        Point2D p23;
        Point2D p31;
        Point2D p32;
        Point2D p33;
        Point2D farPoint = sightLine.getP1();
        Triangle_dt farTriangle = null;

        // check the neighbours
        if (!neigbour1.isHalfplane()) {
                n1l1 = new Line2D.Double(neigbour1.p1().x(),neigbour1.p1().y(),neigbour1.p2().x(),neigbour1.p2().y());
                if ((p11 = lineIntersection(sightLine,n1l1)) != null) {
                    // handle the special case of a line contained in another
                    if (p11.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        // get the beginning or the end of the edge
                        if (sightLine.getP1().distance(n1l1.getP1()) > sightLine.getP1().distance(n1l1.getP2())) {
                            p11 = n1l1.getP1();
                        }
                        else {
                            p11 = n1l1.getP2();
                        }
                    }
                    // we look for the farest point from the first point of the sight line
                    if (farPoint.distance(sightLine.getP1()) < p11.distance(sightLine.getP1())) {
                        farPoint = p11;
                        farTriangle = neigbour1;
                    }
                }
                n1l2 = new Line2D.Double(neigbour1.p2().x(),neigbour1.p2().y(),neigbour1.p3().x(),neigbour1.p3().y());
                if ((p12 = lineIntersection(sightLine,n1l2)) != null) {
                    // handle the special case of a line contained in another
                    if (p12.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        // get the beginning or the end of the edge
                        if (sightLine.getP1().distance(n1l2.getP1()) > sightLine.getP1().distance(n1l2.getP2())) {
                            p12 = n1l2.getP1();
                        }
                        else {
                            p12 = n1l2.getP2();
                        }
                    }
                    // we look for the farest point from the first point of the sight line
                    if (farPoint.distance(sightLine.getP1()) < p12.distance(sightLine.getP1())) {
                        farPoint = p12;
                        farTriangle = neigbour1;
                    }
                }
                n1l3 = new Line2D.Double(neigbour1.p3().x(),neigbour1.p3().y(),neigbour1.p1().x(),neigbour1.p1().y());
                if ((p13 = lineIntersection(sightLine,n1l3)) != null) {
                    // handle the special case of a line contained in another
                    if (p13.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(n1l3.getP1()) > sightLine.getP1().distance(n1l3.getP2())) {
                            p13 = n1l3.getP1();
                        }
                        else {
                            p13 = n1l3.getP2();
                        }
                    }
                    if (farPoint.distance(sightLine.getP1()) < p13.distance(sightLine.getP1())) {
                        farPoint = p13;
                        farTriangle = neigbour1;
                    }
                }
            }

            if (!neigbour2.isHalfplane()) {
                n2l1 = new Line2D.Double(neigbour2.p1().x(),neigbour2.p1().y(),neigbour2.p2().x(),neigbour2.p2().y());
                if ((p21 = lineIntersection(sightLine,n2l1)) != null) {
                    // handle the special case of a line contained in another
                    if (p21.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(n2l1.getP1()) > sightLine.getP1().distance(n2l1.getP2())) {
                            p21 = n2l1.getP1();
                        }
                        else {
                            p21 = n2l1.getP2();
                        }
                    }
                    if (farPoint.distance(sightLine.getP1()) < p21.distance(sightLine.getP1())) {
                        farPoint = p21;
                        farTriangle = neigbour2;
                    }
                }
                n2l2 = new Line2D.Double(neigbour2.p2().x(),neigbour2.p2().y(),neigbour2.p3().x(),neigbour2.p3().y());
                if ((p22 = lineIntersection(sightLine,n2l2)) != null) {
                    // handle the special case of a line contained in another
                    if (p22.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(n2l2.getP1()) > sightLine.getP1().distance(n2l2.getP2())) {
                            p22 = n2l2.getP1();
                        }
                        else {
                            p22 = n2l2.getP2();
                        }
                    }
                    if (farPoint.distance(sightLine.getP1()) < p22.distance(sightLine.getP1())) {
                        farPoint = p22;
                        farTriangle = neigbour2;
                    }
                }
                n2l3 = new Line2D.Double(neigbour2.p3().x(),neigbour2.p3().y(),neigbour2.p1().x(),neigbour2.p1().y());
                if ((p23 = lineIntersection(sightLine,n2l3)) != null) {
                    // handle the special case of a line contained in another
                    if (p23.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(n2l3.getP1()) > sightLine.getP1().distance(n2l3.getP2())) {
                            p23 = n2l3.getP1();
                        }
                        else {
                            p23 = n2l3.getP2();
                        }
                    }
                    if (farPoint.distance(sightLine.getP1()) < p23.distance(sightLine.getP1())) {
                        farPoint = p23;
                        farTriangle = neigbour2;
                    }
                }
            }

            if (!neigbour3.isHalfplane()) {
                n3l1 = new Line2D.Double(neigbour3.p1().x(),neigbour3.p1().y(),neigbour3.p2().x(),neigbour3.p2().y());
                if ((p31 = lineIntersection(sightLine,n3l1)) != null) {
                    // handle the special case of a line contained in another
                    if (p31.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(n3l1.getP1()) > sightLine.getP1().distance(n3l1.getP2())) {
                            p31 = n3l1.getP1();
                        }
                        else {
                            p31 = n3l1.getP2();
                        }
                    }
                    if (farPoint.distance(sightLine.getP1()) < p31.distance(sightLine.getP1())) {
                        farPoint = p31;
                        farTriangle = neigbour3;
                    }
                }
                n3l2 = new Line2D.Double(neigbour3.p2().x(),neigbour3.p2().y(),neigbour3.p3().x(),neigbour3.p3().y());
                if ((p32 = lineIntersection(sightLine,n3l2)) != null) {
                    // handle the special case of a line contained in another
                    if (p32.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(n3l2.getP1()) > sightLine.getP1().distance(n3l2.getP2())) {
                            p32 = n3l2.getP1();
                        }
                        else {
                            p32 = n3l2.getP2();
                        }
                    }
                    if (farPoint.distance(sightLine.getP1()) < p32.distance(sightLine.getP1())) {
                        farPoint = p32;
                        farTriangle = neigbour3;
                    }
                }
                n3l3 = new Line2D.Double(neigbour3.p3().x(),neigbour3.p3().y(),neigbour3.p1().x(),neigbour3.p1().y());
                if ((p33 = lineIntersection(sightLine,n3l3)) != null) {
                    // handle the special case of a line contained in another
                    if (p33.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(n3l3.getP1()) > sightLine.getP1().distance(n3l3.getP2())) {
                            p33 = n3l3.getP1();
                        }
                        else {
                            p33 = n3l3.getP2();
                        }
                    }
                    if (farPoint.distance(sightLine.getP1()) < p33.distance(sightLine.getP1())) {
                        farPoint = p33;
                        farTriangle = neigbour3;
                    }
                }
            }

        // if we couldn't find the next triangle in our neighbours region, we search in the whold triangulation
        if (farPoint.equals(sightLine.getP1())) {
            // search the triangle in all the triangulation
            Iterator<Triangle_dt> iterator = dt.trianglesIterator();
            Triangle_dt triangle;
            Line2D line1, line2, line3;
            Point2D p1,p2,p3;
            int count;
            while (iterator.hasNext()) {
                count = 0;
                triangle = iterator.next();
                if (triangle.isHalfplane()) {
                    continue;
                }
                line1 = new Line2D.Double(triangle.p1().x(),triangle.p1().y(),triangle.p2().x(),triangle.p2().y());
                line2 = new Line2D.Double(triangle.p2().x(),triangle.p2().y(),triangle.p3().x(),triangle.p3().y());
                line3 = new Line2D.Double(triangle.p3().x(),triangle.p3().y(),triangle.p1().x(),triangle.p1().y());
                // check the intersection of the sightLine with the first edge
                if ((p1 = lineIntersection(sightLine,line1)) != null) {
                    // if we found that an edge is contained in the sight line we declare it as the next triangle
                    if (p1.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        nextTriangle = triangle;
                        break;
                    }
                    count++;
                }
                if ((p2 = lineIntersection(sightLine,line2)) != null) {
                    if (p2.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        nextTriangle = triangle;
                        break;
                    }
                    count++;
                }
                if ((p3 = lineIntersection(sightLine,line3)) != null) {
                    if (p3.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        nextTriangle = triangle;
                        break;
                    }
                    count++;
                }
                // if there are more than one intersections
                if (count > 1) {
                    if (p1 != null && p2 != null) {
                        // check if the intersections are "real"
                        if (!p1.equals(p2)) {
                            return triangle;
                        }
                    }
                    if (p2 != null && p3 != null) {
                        if (!p2.equals(p3)) {
                            return triangle;
                        }
                    }
                    if (p3 != null && p1 != null) {
                        if (!p3.equals(p1)) {
                            return triangle;
                        }
                    }
                }
            }
        }
        // we found the next neighbour - it is one of our neighbours
        else {
            return farTriangle;
        }
        return farTriangle;
    }

    // this function checks if this intersection is a valid one
    private boolean isRealIntersection(Point2D firstInter,Point2D secondInter,Point2D thirdInter) {
        if (firstInter != null && secondInter != null) {
            // check if the intersections are "real"
            if (!firstInter.equals(secondInter)) {
                return true;
            }
        }
        if (secondInter != null && thirdInter != null) {
            if (!secondInter.equals(thirdInter)) {
                return true;
            }
        }
        if (thirdInter != null && firstInter != null) {
            if (!thirdInter.equals(firstInter)) {
                return true;
            }
        }
        return false;
    }

    // find the 2D lines in the way between the guard and the diamond
    private List<Line2D.Double> getOnSightLines(double guardX, double guardY, double diamondX, double diamondY, Delaunay_Triangulation dt) throws IllegalStateException {
        Point_dt guardP = new Point_dt(guardX,guardY);
        Triangle_dt guardTriangle = dt.find(guardP);
        Triangle_dt nextTriangle = guardTriangle;
        Point2D guard2DP = new Point2D.Double(guardX,guardY);
        Point2D diamond2DP = new Point2D.Double(diamondX,diamondY);
        Line2D sightLine = new Line2D.Double(guardX,guardY,diamondX,diamondY);
        List<Line2D.Double> listOfLines = new ArrayList<Line2D.Double>();
        Point2D firstPoint = guard2DP;
        Point2D secondPoint = guard2DP;
        Line2D.Double firstEdge,secondEdge,thirdEdge;
        Point2D firstInter,secondInter,thirdInter;
        Point2D containedInter = null;
        Point2D notContainedInter = null;

        // loop until you get to the diamond
        while (!diamond2DP.equals(secondPoint)) {

            if (!nextTriangle.isHalfplane()) {
                // get all the edges of the current triangle
                firstEdge =   new Line2D.Double(nextTriangle.p1().x(),nextTriangle.p1().y(),nextTriangle.p2().x(),nextTriangle.p2().y());
                secondEdge =   new Line2D.Double(nextTriangle.p2().x(),nextTriangle.p2().y(),nextTriangle.p3().x(),nextTriangle.p3().y());
                thirdEdge =   new Line2D.Double(nextTriangle.p1().x(),nextTriangle.p1().y(),nextTriangle.p3().x(),nextTriangle.p3().y());
                // get the sight line and the edges intersections
                firstInter = lineIntersection(sightLine,firstEdge);
                secondInter = lineIntersection(sightLine,secondEdge);
                thirdInter = lineIntersection(sightLine,thirdEdge);
                if (!isRealIntersection(firstInter,secondInter,thirdInter)) {
                    listOfLines.add(new Line2D.Double(firstPoint,diamond2DP));
                    return listOfLines;
                }
                if (firstInter != null) {
                    // if the lines are contained in each other
                    if (firstInter.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(firstEdge.getP1()) > sightLine.getP1().distance(firstEdge.getP2())) {
                            firstInter = firstEdge.getP1();
                        }
                        else {
                            firstInter = firstEdge.getP2();
                        }
                        // save the intersection of contained lines
                        containedInter = firstInter;
                    }
                    else {
                        // save the regular intersection
                        notContainedInter = firstInter;
                    }
                }
                if (secondInter != null) {
                    if (secondInter.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        if (sightLine.getP1().distance(secondEdge.getP1()) > sightLine.getP1().distance(secondEdge.getP2())) {
                            secondInter = secondEdge.getP1();
                        }
                        else {
                            secondInter = secondEdge.getP2();
                        }
                        // save the intersection of contained lines
                        containedInter = secondInter;
                    }
                    else {
                        // save the regular intersection
                        notContainedInter = secondInter;
                    }
                }
                if (thirdInter != null) {
                    if (thirdInter.equals(new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE))) {
                        // look for the farest point
                        if (sightLine.getP1().distance(thirdEdge.getP1()) > sightLine.getP1().distance(thirdEdge.getP2())) {
                            thirdInter = thirdEdge.getP1();
                        }
                        else {
                            thirdInter = thirdEdge.getP2();
                        }
                        containedInter = thirdInter;
                    }
                    else {
                        notContainedInter = thirdInter;
                    }
                }
                if (containedInter != null) {
                    listOfLines.add(new Line2D.Double(firstPoint,containedInter));
                }
                else {
                    if (notContainedInter != null) {
                        listOfLines.add(new Line2D.Double(firstPoint,notContainedInter));    
                    }
                    else {
                        throw new IllegalStateException("not intersecting in any way, even though of the check");
                    }
                }
                secondPoint = listOfLines.get(listOfLines.size()-1).getP2();
                sightLine.setLine(secondPoint,sightLine.getP2());
                nextTriangle = chooseNextTriangle(sightLine,nextTriangle,dt);
                firstPoint = secondPoint;
            }
            else {
                throw new IllegalStateException("got to the edge of the triangulation");
            }
       }
       return listOfLines;
}

// get the lines intersection. return -Double.Max_Value, -Double.Max_Value
private Point2D lineIntersection(Line2D line1, Line2D line2) {
    Point2D cp = null;
    double a1,b1,c1,a2,b2,c2,denom;
    a1 = line1.getY2()-line1.getY1();
    b1 = line1.getX1()-line1.getX2();
    c1 = line1.getX2()*line1.getY1()-line1.getX1()*line1.getY2();
    // a1x + b1y + c1 = 0 line1 eq
    a2 = line2.getY2()-line2.getY1();
    b2 = line2.getX1()-line2.getX2();
    c2 = line2.getX2()*line2.getY1()-line2.getX1()*line2.getY2();
    // a2x + b2y + c2 = 0 line2 eq
    denom = a1*b2 - a2*b1;
    if(denom != 0) {
        cp = new Point2D.Double((b1*c2 - b2*c1)/denom ,((a2*c1) - (a1*c2))/denom);
        if (cp.getX() < Math.min(line1.getP1().getX(), line1.getP2().getX()) || cp.getX() >Math.max(line1.getP1().getX(), line1.getP2().getX())
              || cp.getY() < Math.min(line1.getP1().getY(), line1.getP2().getY()) || cp.getY() >Math.max(line1.getP1().getY(), line1.getP2().getY())
              || cp.getX() < Math.min(line2.getP1().getX(), line2.getP2().getX()) || cp.getX() >Math.max(line2.getP1().getX(), line2.getP2().getX())
              || cp.getY() < Math.min(line2.getP1().getY(), line2.getP2().getY()) || cp.getY() >Math.max(line2.getP1().getY(), line2.getP2().getY())) {

            cp = null;
        }
    }
    else {
        // sight line is a point
        if (a1 == 0 && b1 == 0) {
            if (a2*line1.getX1() + b2*line1.getY1() + c2 == 0) {
                cp =  new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE);
            }
        }
        else {
            // check if the lines are the same (in the infinite way)
            if (a1/c1 == a2/c2 && b1/c1 == b2/c2) {
                cp = new Point2D.Double(-Double.MAX_VALUE,-Double.MAX_VALUE);
            }
            else {  // case the lines are parallel but not intersecting
                cp = null;
            }
        }
    }
    return cp;
    }
}
