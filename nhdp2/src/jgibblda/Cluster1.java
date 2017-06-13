/*
 * Copyright (C) 2007 by
 * 
 * 	Xuan-Hieu Phan
 *	hieuxuan@ecei.tohoku.ac.jp or pxhieu@gmail.com
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 * 
 *  Cam-Tu Nguyen
 *  ncamtu@gmail.com
 *  College of Technology
 *  Vietnam National University, Hanoi
 *
 * JGibbsLDA is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JGibbsLDA is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGibbsLDA; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package jgibblda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;


import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;
import ckling.math.*;

public class Cluster1 {

	//Index over neighbour clusters
	public ArrayList<CopyOnWriteArraySet<Integer>> qqN;
	//index over documents for clusters
	public ArrayList<CopyOnWriteArraySet<Integer>> qd;

	public Document[] docs = null;

	public double[] mx;	
	public double[] my;	
	public double[] mz;	
	public double[] k;
	public double[] x;
	public double[] y;
	public double[] z;
	//hard assignment of documents to q
	public int[] q;
	//soft assignment of documents to q
	public double[][] pQ;
	//array with all triangles, useful for creating the map
	public int[][] triangles;
	//likelihood of every point for reassigning empty cluster centroids
	private double[] likelihood;
	double[] countQ = null;	
	private boolean skip = false;
	private boolean change;
	//number of clusters
	private int J;

	private static int nrThreads = 16;
	private static double kstart = 1;

	private static double maxKGlobal = kstart;

	public Cluster1 (Document[] docs, int n, String dir) {

		this.docs = docs;
		J = docs.length;

		
		qqN = new ArrayList<CopyOnWriteArraySet<Integer>>();
		qd = new ArrayList<CopyOnWriteArraySet<Integer>>();
		for (int i = 0; i < n; i++) {
			qqN.add(i,new CopyOnWriteArraySet<Integer>());
			qd.add(i,new CopyOnWriteArraySet<Integer>());
		}


		double[] lats = new double[docs.length];
		double[] lons = new double[docs.length];

		int count1=0,count2=0;
		
		for (int i = 0; i < docs.length; i++) {

			lats[i] =  docs[i].lat;
			lons[i] =  docs[i].lon;
			
		}
		
		System.out.println(count1 +" " + count2);

		double[][] toc = ckling.geo.Coordinates.toCart(lats,lons);
		x = toc[0];
		y = toc[1];
		z = toc[2];

		//System.out.println(x[0] + " " + y[0] + " " + z[0]);

		q = new int[docs.length];
		
		mx =  new double[n];	
		my =  new double[n];	
		mz =  new double[n];
		
		//create index for docs in q if done
		for (int i = 0; i < docs.length; i++) {
			//qNd[q[i]].add(i);
			qd = addElement(qd,i,i);
			mx[i]=x[i];
			my[i]=y[i];
			mz[i]=z[i];
		}			

		//save q for every document
		for (int i = 0; i < docs.length; i++) {
			docs[i].q = i;
		}


		Delaunay_Triangulation dt = new Delaunay_Triangulation();

		Point_dt[] p = new Point_dt[n];

		for (int i = 0; i < n; i++) {
			p[i] = new Point_dt(mx[i],my[i],mz[i],i);
			dt.insertPoint(p[i]);
		}

		triangles = new int[dt.trianglesSize()][3];
		int i = 0;

		for (Iterator<Triangle_dt> iterator_dt = dt.trianglesIterator();iterator_dt.hasNext();) {

			Triangle_dt triangle_dt = iterator_dt.next();

			if (!triangle_dt.isHalfplane()) {
				
				//check for triangles with small angle as in  "NON-OBTUSE TRIANGULATION OF A POLYGON"BS BAKER, E GROSSE, 1985
				
				//calculate length of triangle sides
				double a = 
							Math.sqrt(
							Math.pow(triangle_dt.p1().x() -  triangle_dt.p2().x(),2) +
							Math.pow(triangle_dt.p1().y() -  triangle_dt.p2().y(),2) +
							Math.pow(triangle_dt.p1().z() -  triangle_dt.p2().z(),2)
							);
				double b = 
						Math.sqrt(
						Math.pow(triangle_dt.p2().x() -  triangle_dt.p3().x(),2) +
						Math.pow(triangle_dt.p2().y() -  triangle_dt.p3().y(),2) +
						Math.pow(triangle_dt.p2().z() -  triangle_dt.p3().z(),2)
						);
				
				double c = 
						Math.sqrt(
						Math.pow(triangle_dt.p1().x() -  triangle_dt.p3().x(),2) +
						Math.pow(triangle_dt.p1().y() -  triangle_dt.p3().y(),2) +
						Math.pow(triangle_dt.p1().z() -  triangle_dt.p3().z(),2)
						);
				
				double cosAlpha = (b*b + c*c - a*a) / (2 * b * c); 
				double cosBeta = (a*a + c*c - b*b) / (2 * a * c); 
				double cosGamma = (b*b + a*a - c*c) / (2 * b * a);
				
				//calculate angle in degrees
				double alpha = Math.acos(cosAlpha);
				double beta = Math.acos(cosBeta);
				double gamma = Math.acos(cosGamma);
				
				double minAngle = Math.min(alpha, Math.min(beta,gamma));
				

				//atan(1/4) is minimum angle, as in  "NON-OBTUSE TRIANGULATION OF A POLYGON"BS BAKER, E GROSSE, 1985
				if (true || minAngle >= Math.atan(1./4.)) {
				
				
				
				int p1id = triangle_dt.p1().id();
				int p2id = triangle_dt.p2().id();
				int p3id = triangle_dt.p3().id();
				
				triangles[i][0]=p1id; 
				triangles[i][1]=p2id; 
				triangles[i][2]=p3id; 
								
				i++;

				//set index for q->qN
				/*
					qqN[p1id].add(p2id);
					qqN[p1id].add(p3id);
					qqN[p2id].add(p1id);
					qqN[p2id].add(p3id);
					qqN[p3id].add(p1id);
					qqN[p3id].add(p2id);
				 */

				qqN = addElement(qqN,p1id,p2id);
				qqN = addElement(qqN,p1id,p3id);
				qqN = addElement(qqN,p2id,p1id);
				qqN = addElement(qqN,p2id,p3id);
				qqN = addElement(qqN,p3id,p1id);
				qqN = addElement(qqN,p3id,p2id);


				//GNUplot:
				//splot "/home/c/ownCloud/files/voro.gnu" with lines


				System.out.println(triangle_dt.p1().x() + " " + triangle_dt.p1().y() + " " + triangle_dt.p1().z());
				System.out.println(triangle_dt.p2().x() + " " + triangle_dt.p2().y() + " " + triangle_dt.p2().z());
				System.out.println(triangle_dt.p3().x() + " " + triangle_dt.p3().y() + " " + triangle_dt.p3().z());
				System.out.println();
				System.out.println();


				}
			}

		}


		//fit triangles size to used triangles
		int[][] trianglesOld = triangles;
		triangles = new int[i][3];
		System.arraycopy(trianglesOld, 0, triangles, 0, i);
		
	}


	/*
	 * Add data to array in array list
	 */
	private ArrayList<CopyOnWriteArraySet<Integer>> addElement(ArrayList<CopyOnWriteArraySet<Integer>> a, int index, int value) {
		CopyOnWriteArraySet<Integer> cowas = a.remove(index);
		cowas.add(value);
		a.add(index,cowas);
		return a;
	}


	public double[][] getqm () {
		return ckling.geo.Coordinates.toSpherical(mx, my,mz);
	}

	public int[] getq () {
		return q;
	}


	public double[][] getPq() {
		double[][] ret = new double[docs.length][docs.length];
		for (int i=0;i < ret.length;i++) {
			for (int j= 0;j<ret[0].length;j++) {
				//all points get equal probability => ignore distances
				ret[i][j] = 1.0;
			}
		}
		return ret;

	}

}