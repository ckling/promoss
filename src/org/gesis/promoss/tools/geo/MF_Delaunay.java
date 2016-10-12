/*
q * Copyright (C) 2007 by
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

package org.gesis.promoss.tools.geo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

import org.gesis.promoss.tools.math.*;

import quickhull3d.Point3d;
import quickhull3d.QuickHull3D;


//import delaunay_triangulation.Delaunay_Triangulation;
//import delaunay_triangulation.Point_dt;
//import delaunay_triangulation.Triangle_dt;

class Coord {

	double lat;
	double lon;

	public Coord (double lat, double lon) {
		this.lat=lat;
		this.lon=lon;
	}
	@Override
	public boolean equals(Object obj)
	{
		boolean isEqual = false;
		if (this.getClass() == obj.getClass())
		{
			Coord myValueObject = (Coord) obj;
			if (myValueObject.lat == this.lat && myValueObject.lon == this.lon)
			{
				isEqual = true;
			}
		}

		return isEqual;
	}

}


public class MF_Delaunay {

	//Index over neighbour clusters
	public ArrayList<CopyOnWriteArraySet<Integer>> qqN;
	//index over documents for clusters
	public ArrayList<CopyOnWriteArraySet<Integer>> qd;


	private double[] mx;	
	private double[] my;	
	private double[] mz;	
	private double[] k;
	private double[] x;
	private double[] y;
	private double[] z;
	//hard assignment of documents to q
	private int[] q;
	//soft assignment of documents to q
	private double[][] pQ;
	//array with all triangles, useful for creating the map
	private int[][] triangles;
	//likelihood of every point for reassigning empty cluster centroids
	private double likelihood;
	double[] countQ = null;	
	private boolean skip = false;
	private boolean change;
	//number of clusters
	private int J;

	private static int nrThreads = 1;
	private static double kstart = 1;
	private boolean useFile = true;

	//mininum value for k
	private double mink = 0.001;
	//get maximum k of all clusters
	private double maxk = mink;

	private HashSet<Coord> distinct = new HashSet<Coord>();

	public String clusterFile = null;


	public MF_Delaunay (Double[] lats, Double[] lons, int n, String dir, int metadata_index) {

		if (clusterFile == null) {
			clusterFile = dir  + File.separator + "cluster_"+metadata_index+"_"+n+".csv";
		}

		int corpusSize = lats.length;

		J = n;

		qqN = new ArrayList<CopyOnWriteArraySet<Integer>>();
		qd = new ArrayList<CopyOnWriteArraySet<Integer>>();
		for (int i = 0; i < n; i++) {
			qqN.add(i,new CopyOnWriteArraySet<Integer>());
			qd.add(i,new CopyOnWriteArraySet<Integer>());
		}





		for (int i = 0; i < corpusSize; i++) {

			if (distinct.size()<n) {
				distinct.add(new Coord(lats[i],lons[i]));
			}

		}

		//System.out.println(count1 +" " + count2 + " " + distinct.size());

		double[][] toc = org.gesis.promoss.tools.geo.Coordinates.toCart(lats,lons);
		x = toc[0];
		y = toc[1];
		z = toc[2];

		q = new int[corpusSize];
		likelihood  = 0;
		mx =  new double[n];	
		my =  new double[n];	
		mz =  new double[n];
		k =  new double[n];

		//initialize k
		for (int i = 0; i <n; i++) {
			k[i] = kstart;
		}

		for (int i = 0; i <corpusSize; i++) {
			q[i] = (int) Math.round(Math.random()*(n-1));
		}


		//initialize q
		try {
			File cluster = new File(clusterFile);
			if (useFile && cluster.exists()) {

				BufferedReader br  = new BufferedReader(new FileReader(clusterFile));
				String strLine;
				//Read File Line By Line
				int countLines = 0;
				while ((strLine = br.readLine()) != null &&  countLines <corpusSize )   {
					q[countLines++] = Integer.valueOf(strLine);
					//System.out.println(countLines-1 + " " +  Integer.valueOf(strLine));
				}

				br.close();

				//M-step: set new parameters for every q

				double[] sumx = new double[n];
				double[] sumy = new double[n];
				double[] sumz = new double[n];
				//number of points in Q
				countQ = new double[n];

				for (int i = 0; i < corpusSize; i++) {
					//System.out.println(q[i]);
					sumx[q[i]]+=x[i];
					sumy[q[i]]+=y[i];
					sumz[q[i]]+=z[i];
					countQ[q[i]]++;
				}

				for (int j=0; j < J; j++) {


					double sumx2 =  sumx[j]*sumx[j];
					double sumy2 =  sumy[j]*sumy[j];
					double sumz2 =  sumz[j]*sumz[j];
					double rl2 = Math.sqrt(sumx2 + sumy2 + sumz2);
					double r_ = rl2 / countQ[j];

					if (r_ >= 1) {
						r_ =  2-r_;
					}

					mx[j] = sumx[j] / rl2;
					my[j] = sumy[j] / rl2;
					mz[j] = sumz[j] / rl2;



					if (countQ[j] == 1 ||  r_ == 1) {
						k[j] = -1;
					}
					else {

						k[j] = (r_ * 3.0 - r_ * r_ * r_) / (1.0 - r_*r_);
						if (k[j] == 0) {
							k[j] = mink;
						}
						if (k[j] > maxk) {
							maxk = k[j];
						}

					}

					k[j] = (r_ * 3.0 - r_ * r_ * r_) / (1.0 - r_*r_);

				}

				for (int j=0; j < J; j++) {
					if (k[j] == -1) {
						k[j] = maxk;
					}
				}

				/*
				for (int j=0; j < J; j++) {
				System.out.println(j + " " +mx[j] + " " + my[j] + " " + mz[j] + " | k =  " + k[j] + " (" + countQ[j] +")");
				}
				 */

				skip = true;

			}
			else {
				pQ = new double[corpusSize][J];
			}

		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int count = 0;

		double oldLikelihood = 1;

		//skip EM?
		if (!skip) {

			do {
				change = true;

				//E-step: assign region q to points (hard assignment)			

				EstQ[] estq = new EstQ[nrThreads];

				//split Tasks, last thread gets rest size of division
				int partSize, limit;
				partSize = corpusSize / nrThreads;		

				for (int l = 0; l < nrThreads; l++) {

					if (l == nrThreads-1) {
						limit = partSize + corpusSize % nrThreads;
					}
					else {
						limit = partSize;
					}

					estq[l] = new EstQ(l*partSize,l*partSize + limit);

				}


				try {
					for (int l = 0; l < nrThreads; l++) {

						//wait till thread l terminates
						estq[l].t.join();

					}

				} catch (InterruptedException e) {}

				//M-step: set new parameters for every q

				double[] sumx = new double[n];
				double[] sumy = new double[n];
				double[] sumz = new double[n];
				//number of points in Q
				countQ = new double[n];

				for (int i = 0; i < corpusSize; i++) {
					for (int j=0; j < J; j++) {
						sumx[j]+=pQ[i][j]*x[i];
						sumy[j]+=pQ[i][j]*y[i];
						sumz[j]+=pQ[i][j]*z[i];
						countQ[j]+=pQ[i][j];
					}
				}


				for (int j=0; j < n; j++) {

					double sumx2 = 0;
					double sumy2 = 0;
					double sumz2 = 0;
					double rl2 = 0;
					double r_ = 0;

					//empty clusters are possible if pQ == 0
					if (countQ[j] > 0) {
						sumx2 =  sumx[j]*sumx[j];
						sumy2 =  sumy[j]*sumy[j];
						sumz2 =  sumz[j]*sumz[j];
						rl2 = Math.sqrt(sumx2 + sumy2 + sumz2);
						r_ = rl2 / countQ[j];

						mx[j] = sumx[j] / rl2;
						my[j] = sumy[j] / rl2;
						mz[j] = sumz[j] / rl2;


						if (countQ[j] == 1 ||  r_ >= 0.99999999) {
							k[j] = -1;
						}
						else {
							k[j] = (3 * r_ - Math.pow(r_,3)) / (1 - Math.pow(r_,2));
						}
						if (k[j] == 0) {
							k[j] = mink;
						}
						else if (k[j] > maxk) {
							maxk = k[j];
						}

						k[j] = (r_ * 3 - r_ * r_ * r_) / (1 - r_*r_);

					}
					//or: no element in cluster j, then assign point with lowest likelihood
					else {


						//dont stop sampling till every cluster is non empty

						//						//one previous change was by an outlier which now gets his own cluster

						if (!distinct.isEmpty()) {
							Coord coord = distinct.iterator().next();
							distinct.remove(coord);
							double[]xyz = org.gesis.promoss.tools.geo.Coordinates.toCart(coord.lat,coord.lon);
							mx[j] = xyz[0];
							my[j] = xyz[1];
							mz[j] = xyz[2];
						}
						else {
							//System.out.println("random");

							int newIndex = (int) (Math.random()*corpusSize);
							mx[j] = x[newIndex];
							my[j] = y[newIndex];
							mz[j] = z[newIndex];
							countQ[q[newIndex]]--;
							q[newIndex] = j; 
						}											

						//set k to average k
						k[j] = kstart;
						countQ[j] = 1;

						count = 0;


					}
					double max = Math.pow(10,15);
					//double max = 1000;
					if (Double.isInfinite(k[j]) || k[j]>max) {
						//k[j] = BasicMath.max(k);
						k[j] = max;
					}

					//System.out.println(r_ + " " + mx[j] + " " + my[j] + " " + mz[j] + " | k =  " + k[j] + " (" + countQ[j] +")");

				}

				for (int j=0; j < J; j++) {
					if (k[j] < 0) {
						k[j] = maxk;
					}
				}



				double likelihoodSum = 1.0;
				if (count > 0) {				

					likelihoodSum = likelihood;

					double ratio = (likelihoodSum / oldLikelihood) -1;
					//System.out.println(ratio);
					if (Double.isNaN(ratio)) break;
					//&& ratio >= 0
					if (count > 1 && ratio < 0.01) {
						//stop sampling
						change = false;
					}
				}
				oldLikelihood = likelihoodSum;
				likelihood = 0.0;

				System.out.println("Geographical clustering step "+ count++ + " (Likelihood: " + oldLikelihood+")");

			} while (change);

			if (useFile) {
				File cluster = new File(clusterFile);
				if (!cluster.exists()) {
					//values converged!
					//System.out.println("success, writing file");
					//save clusters:
					File file = new File(clusterFile);
					FileWriter writer;
					try {
						writer = new FileWriter(file ,true);

						for (int i = 0; i <corpusSize; i++) {
							writer.write(String.valueOf(q[i]) );
							writer.write("\n");
						}
						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		}


		//create index for docs in q if done
		for (int i = 0; i < corpusSize; i++) {
			//qNd[q[i]].add(i);
			qd = addElement(qd,q[i],i);
		}			

		if (J>1) {

			Point3d[] points = new Point3d[n];

			for (int i = 0; i < n; i++) {
				if (!Double.isNaN(mx[i])) {
					points[i] = new Point3d(mx[i]+Math.random()*0.00001,my[i],mz[i]);
					//+Math.random()*0.00001?
				}
			}
			QuickHull3D hull = new QuickHull3D();
			hull.build (points);
			int[][] faceIndices = hull.getFaces();
			hull.triangulate();
			int[] point_indices = hull.getVertexPointIndices();

			int triangle_count = 0;
			triangles = new int[faceIndices.length][3];

			for (int i=0;i<faceIndices.length;i++) {

				int[] triangle = faceIndices[i];

				if (triangle.length == 3) {

					triangle_count++;

					//translate to orginial point indices
					for (int j=0;j<3;j++) {
						triangle[j] = point_indices[triangle[j]];
					}
					//System.out.println(triangle[0] + " " + triangle[1] + " " + triangle[2] );

					//check for triangles with small angle as in  "NON-OBTUSE TRIANGULATION OF A POLYGON"BS BAKER, E GROSSE, 1985

					//calculate length of triangle sides
					double a = 
							Math.sqrt(
									Math.pow(points[triangle[0]].x -  points[triangle[1]].x,2) +
									Math.pow(points[triangle[0]].y -  points[triangle[1]].y,2) +
									Math.pow(points[triangle[0]].z -  points[triangle[1]].z,2)
									);
					double b = 
							Math.sqrt(
									Math.pow(points[triangle[2]].x -  points[triangle[1]].x,2) +
									Math.pow(points[triangle[2]].y -  points[triangle[1]].y,2) +
									Math.pow(points[triangle[2]].z -  points[triangle[1]].z,2)
									);

					double c = 
							Math.sqrt(
									Math.pow(points[triangle[0]].x -  points[triangle[2]].x,2) +
									Math.pow(points[triangle[0]].y -  points[triangle[2]].y,2) +
									Math.pow(points[triangle[0]].z -  points[triangle[2]].z,2)
									);
					//				
					double cosAlpha = (b*b + c*c - a*a) / (2 * b * c); 
					double cosBeta = (a*a + c*c - b*b) / (2 * a * c); 
					double cosGamma = (b*b + a*a - c*c) / (2 * b * a);

					//calculate angle in degrees
					double alpha = Math.acos(cosAlpha);
					double beta = Math.acos(cosBeta);
					double gamma = Math.acos(cosGamma);
					double minAngle = Math.min(alpha, Math.min(beta,gamma));

					//atan(1/4) is minimum angle, as in  "NON-OBTUSE TRIANGULATION OF A POLYGON"BS BAKER, E GROSSE, 1985
					//if (minAngle >= Math.atan(1./6.)) {

						//double maxdist = Math.sqrt(1.0/Math.pow(2, 5));
						
						//if (Math.max(Math.max(a,b),c)<maxdist) {

							triangles[i] = triangle;


							//set index for q->qN
							/*
					qqN[p1id].add(p2id);
					qqN[p1id].add(p3id);
					qqN[p2id].add(p1id);
					qqN[p2id].add(p3id);
					qqN[p3id].add(p1id);
					qqN[p3id].add(p2id);
							 */
							
							qqN = addElement(qqN,triangle[0],triangle[1]);
							qqN = addElement(qqN,triangle[0],triangle[2]);
							qqN = addElement(qqN,triangle[1],triangle[0]);
							qqN = addElement(qqN,triangle[1],triangle[2]);
							qqN = addElement(qqN,triangle[2],triangle[0]);
							qqN = addElement(qqN,triangle[2],triangle[1]);


							//GNUplot:
							//splot "/home/c/ownCloud/files/voro.gnu" with lines


							/*
				System.out.println(triangle_dt.p1().x() + " " + triangle_dt.p1().y() + " " + triangle_dt.p1().z());
				System.out.println(triangle_dt.p2().x() + " " + triangle_dt.p2().y() + " " + triangle_dt.p2().z());
				System.out.println(triangle_dt.p3().x() + " " + triangle_dt.p3().y() + " " + triangle_dt.p3().z());
				System.out.println(triangle_dt.p1().x() + " " + triangle_dt.p1().y() + " " + triangle_dt.p1().z());
				System.out.println();
				System.out.println();
							 */
						//}
					//}

				}

			}

			//System.out.println(i + " triangles");

			//		for (int i1 =0;i1<mx.length;i1++) {
			//			System.out.println(mx[i1] + " " + my[i1]+ " " + mz[i1]);
			//		}

				//fit triangles size to used triangles
				int[][] trianglesOld = triangles;
				triangles = new int[triangle_count][3];
				System.arraycopy(trianglesOld, 0, triangles, 0, triangle_count);
			
		}
		else {
			triangles=new int[0][3];
		}

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

		double[][] ret = new double[J][3];
		for (int j=0;j<J;j++) {
			ret[j][0]=mx[j];
			ret[j][1]=my[j];
			ret[j][2]=mz[j];
		}
		return ret;
	}

	public double[][] getqmxyzk () {
		double[][] result = {mx,my,mz,k};
		return result;
	}

	public double[] getqk () {
		return k;
	}

	public int[] getq () {
		return q;
	}

	public ArrayList<CopyOnWriteArraySet<Integer>> getqqN () {
		return qqN;
	}


	public double[][] getPq() {
		//		double[][] ret = new double[docs.length][k.length];
		//		for (int i=0;i < ret.length;i++) {
		//			for (int j= 0;j<ret[0].length;j++) {
		//				//all points get equal probability => ignore distances
		//				ret[i][j] = 1.0;
		//			}
		//		}
		//		return ret;
		return pQ;

	}

	public void clearPq() {
		pQ = null;
	}


	class EstQ implements Runnable {

		Thread t;
		private int start;
		private int limit;

		EstQ (int start, int limit) {

			this.start = start;
			this.limit = limit;

			String name = "Thread expectation q" + String.valueOf(start);
			t = new Thread(this, name);		
			t.start();
		}

		public void run() {

			double log2pi = Math.log(2 * Math.PI);

			for (int i=start; i < limit; i++) {

				//Set pQ to minimum, Q to -1
				double logpQ =  -Double.MAX_VALUE;
				int newQ = -1;
				double[] logpQnew = new double[J];
				double[] pQnew =new double[J];

				//soft assignment of elements to clusters? if possible (numerical problems with big k's)
				boolean soft = false;
				//is the result too big for double?
				//			boolean bigNumber = false;

				for (int j=0; j < J; j++) {

					double mytx = (mx[j] * x[i]) + (my[j] * y[i]) + (mz[j] * z[i]);

					if (mx[j] == x[i] && my[j] == y[i] && mz[j] == z[i]) {
						newQ = j;
						q[i] = newQ;
						logpQ = 0;
						break;
					}
					else {
						// k/(2pi e^k - e^-k) * k^(mx) => log k - Math k
						if (k[j] > 10) {
							logpQnew[j] = Math.log(k[j]) - log2pi +  k[j] * (mytx - 1);
						}
						else {
							double denominator = Math.log(2 * Math.PI * (Math.exp(k[j])-Math.exp(-k[j]))); 
							logpQnew[j] = Math.log(k[j]) - denominator + k[j] * mytx;
						}
					}

					if (soft) {
						pQnew[j] = Math.exp(logpQnew[j]);
						if (Double.isInfinite(pQnew[j])) {
							soft = false;
						}
					}				
					likelihood += logpQnew[j] + Math.log(1./J) ;

					//probability P(q) for every cluster is 1/J (that is why P(q|x) ~ P(x|q) / P(x) as seen above )

					//update to q with maximum log likelihood
					if (logpQnew[j] > logpQ) {
						logpQ = logpQnew[j];
						newQ = j;
					}
				}


				if (q[i] != newQ) {
					q[i] = newQ;
					//inform about change
					//change=true;		
				}

				if (soft) {
					double sum = BasicMath.sum(pQnew);
					for (int j=0; j < J; j++) {
						pQ[i][j]=pQnew[j]/sum;
					}
				}
				else {
					for (int j=0; j < J; j++) {
						pQ[i][j]=0.0;
					}
					pQ[i][q[i]]=1.0;
				}

			}

		}

	}

}