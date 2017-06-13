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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import text.Text;

import db.Database;
import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;
import math.*;
import geo.*;

public class KMeans {

	//Index over neighbour clusters
	public ArrayList<CopyOnWriteArraySet<Integer>> qqN;
	//index over documents for clusters
	public ArrayList<CopyOnWriteArraySet<Integer>> qd;

	public Document[] docs = null;

	public double[] mx;	
	public double[] my;	
	public double[] mz;	
	public double[] k;
	//assignment of q to documents
	public int[] q;
	//array with all triangles, useful for creating the map
	public int[][] triangles;
	//likelihood of every point for reassigning empty cluster centroids
	private double[] likelihood;
	int[] countQ = null;	
	boolean skip = false;
	
	private static int nrThreads = 16;
	private static double kstart = 1000;
	
	private static double maxKGlobal = kstart;

	public KMeans (Document[] docs, int n, String dir) {

		this.docs = docs;

		qqN = new ArrayList<CopyOnWriteArraySet<Integer>>();
		qd = new ArrayList<CopyOnWriteArraySet<Integer>>();
		for (int i = 0; i < n; i++) {
			qqN.add(i,new CopyOnWriteArraySet<Integer>());
			qd.add(i,new CopyOnWriteArraySet<Integer>());
		}


		double[] lats = new double[docs.length];
		double[] lons = new double[docs.length];

		for (int i = 0; i < docs.length; i++) {

			lats[i] = docs[i].lat;
			lons[i] = docs[i].lon;

		}

		double[][] toc = geo.Coordinates.toCart(lats,lons);
		double[] x = toc[0];
		double[] y = toc[1];
		double[] z = toc[2];

		//System.out.println(x[0] + " " + y[0] + " " + z[0]);

		q = new int[docs.length];
		likelihood  = new double[docs.length];
		mx =  new double[n];	
		my =  new double[n];	
		mz =  new double[n];
		k =  new double[n];

		//initialize k
		for (int i = 0; i <n; i++) {
			k[i] = kstart;
		}
		
		for (int i = 0; i < n; i++) {

			int randPoint = (int) Math.floor(Math.random() * (docs.length-1));
			mx[i] = x[randPoint]; 
			my[i] = y[randPoint];
			mz[i] = z[randPoint]; 

			//System.out.println(mx[i] + " " + my[i] + " " + mz[i]);

		}
		
		for (int i = 0; i <docs.length; i++) {
			q[i] = (int) Math.round(Math.random()*(n-1));
		}


		//initialize q
		String clusterFile = dir  + File.separator + "cluster"+n+".csv";
		try {
			File cluster = new File(clusterFile);
			if (cluster.exists()) {

				BufferedReader br  = new BufferedReader(new FileReader(clusterFile));
				String strLine;
				//Read File Line By Line
				int countLines = 0;
				while ((strLine = br.readLine()) != null &&  countLines <docs.length )   {
					q[countLines] = Integer.valueOf(strLine);
					countLines++;
				}
				
				
				//M-step: set new parameters for every q

				double[] sumx = new double[n];
				double[] sumy = new double[n];
				double[] sumz = new double[n];
				//number of points in Q
				countQ = new int[n];

				for (int i = 0; i < docs.length; i++) {
					sumx[q[i]]+=x[i];
					sumy[q[i]]+=y[i];
					sumz[q[i]]+=z[i];
					countQ[q[i]]++;
				}

				for (int j=0; j < n; j++) {

					if (countQ[j] > 0) {
						double sumx2 =  sumx[j]*sumx[j];
						double sumy2 =  sumy[j]*sumy[j];
						double sumz2 =  sumz[j]*sumz[j];
						double rl2 = FunctionsMath.sqrt(new BigDecimal(sumx2 + sumy2 + sumz2)).doubleValue();
						double r_ = rl2 / countQ[j];
						
						if (r_ >= 1) {
							r_ =  2-r_;
						}
						
						mx[j] = sumx[j] / rl2;
						my[j] = sumy[j] / rl2;
						mz[j] = sumz[j] / rl2;

						BigDecimal r_big = new BigDecimal(r_);
						
						if (r_ == 1.0) {
							r_big = r_big.subtract(new BigDecimal(Double.MIN_VALUE));
						}

						
						k[j] = r_big.multiply(new BigDecimal(3)).subtract(r_big.pow(3)).divide(BigDecimal.ONE.subtract(r_big.pow(2)),BigDecimal.ROUND_HALF_EVEN).longValue();
						//k[j] = 3r * (3/(3+2
						//k[j] = 2 / (2 - 2 * r_);
					}
					//no points assigned to q: assignment of a bad point
					else {
						int randPoint = (int) Math.floor(Math.random() * (docs.length-1));
						k[j] = Math.pow(10, 15);
						//set it to a place where nothing is
						mx[j] = 1; 
						my[j] = 0;
						mz[j] = 0; 
					}

					
					System.out.println(mx[j] + " " + my[j] + " " + mz[j] + " | k =  " + k[j] + " (" + countQ[j] +")");
					
				}
						
				skip = true;
				
			}

		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int count = 0;

		//skip EM?
		if (!skip) {

			int change;
			do {
				change = 0;
				
				//E-step: assign region q to points (hard assignment)			

				EstQ[] estq = new EstQ[nrThreads];

				//split Tasks, last thread gets rest size of division
				int partSize, partSizeLast;

				for (int l = 0; l < nrThreads; l++) {

					if (l == nrThreads-1) {
						partSize =  (int) Math.floor(docs.length / nrThreads);
						partSizeLast = partSize + docs.length % nrThreads;
					}
					else {
						partSize = (int) Math.floor(docs.length / nrThreads);		
						partSizeLast = partSize;
					}

					//copies of array x/y/z/q in length partsize
					double[] xC = new double[partSizeLast];
					double[] yC = new double[partSizeLast];
					double[] zC = new double[partSizeLast];
					int[] qC = new int[partSizeLast];

					System.arraycopy(x, l * partSize, xC, 0, partSizeLast);
					System.arraycopy(y, l * partSize, yC, 0, partSizeLast);
					System.arraycopy(z, l * partSize, zC, 0, partSizeLast);
					System.arraycopy(q, l * partSize, qC, 0, partSizeLast);

					estq[l] = new EstQ(n, mx,my,mz,k,xC,yC,zC, qC,l);

				}

				
				try {
					for (int l = 0; l < nrThreads; l++) {

						//task split length, last thread has rest size of division
						if (l == nrThreads-1) {
							partSize =  (int) Math.floor(docs.length / nrThreads);
							partSizeLast = partSize + docs.length % nrThreads;
						}
						else {
							partSize = (int) Math.floor(docs.length / nrThreads);		
							partSizeLast = partSize;
						}

						//wait till thread l terminates
						estq[l].t.join();

						System.arraycopy(estq[l].q, 0, q, l*partSize, partSizeLast);
						
						System.arraycopy(estq[l].likelihood, 0, likelihood, l*partSize, partSizeLast);

						//check for changes in thread
							change += estq[l].change;

					}

				} catch (InterruptedException e) {}

				//M-step: set new parameters for every q

				double[] sumx = new double[n];
				double[] sumy = new double[n];
				double[] sumz = new double[n];
				//number of points in Q
				countQ = new int[n];

				for (int i = 0; i < docs.length; i++) {
					sumx[q[i]]+=x[i];
					sumy[q[i]]+=y[i];
					sumz[q[i]]+=z[i];
					countQ[q[i]]++;
				}

				for (int j=0; j < n; j++) {

					double sumx2 = 0;
					double sumy2 = 0;
					double sumz2 = 0;
					double rl2 = 0;
					double r_ = 0;
					
					if (countQ[j] > 0) {
						sumx2 =  sumx[j]*sumx[j];
						sumy2 =  sumy[j]*sumy[j];
						sumz2 =  sumz[j]*sumz[j];
						rl2 = Math.sqrt(sumx2 + sumy2 + sumz2);
						r_ = rl2 / countQ[j];
						
						if (r_ > 1) {
							r_ = 2 - r_;
						}
						if (r_ == 1) {
							r_ = 1-Double.MIN_VALUE;
						}

						mx[j] = sumx[j] / rl2;
						my[j] = sumy[j] / rl2;
						mz[j] = sumz[j] / rl2;

						k[j] = (r_ * 3 - r_ * r_ * r_) / (1 - r_*r_);
						//k[j] = 3r * (3/(3+2
						//k[j] = 2 / (2 - 2 * r_);
					}
					//or: no element in cluster j, then assign point with lowest likelihood
					else {
						
						//one previous change was by our outlier which now gets his own cluster
						change--;
												
						double min = Double.MAX_VALUE;
						int minIndex = -1;
						double max = Double.MIN_VALUE;
						for (int i=0; i<docs.length;i++) {
							if (min > likelihood[i]) {
								min = likelihood[i];
								minIndex = i;
							}
							if (max < likelihood[i]) {
								max = likelihood[i];
							}
						}
						if (max > maxKGlobal) {
							maxKGlobal = max;
						}
						
						//set cluster centroid to point with min likelihood
						mx[j] = x[minIndex];
						my[j] = y[minIndex];
						mz[j] = z[minIndex];
						//set k to average k
						k[j] = BasicMath.mean(k);
						countQ[q[minIndex]]--;
						q[minIndex] = j; 
						countQ[j] = 1;
						
						//set likelihood of the point to max for not reusing 
						//this point for another cluster centroid
						likelihood[minIndex] = max;
						
					}
					if (Double.isInfinite(k[j])) {
						k[j] = BasicMath.max(k);
					}
					
					//K-Means-Mode
					//k[j] = 100;
					//max k = 10^10
					//if (k[j] > Math.pow(10, 10)) {
					//	k[j]=Math.pow(10, 10);
					//}
					
					
					System.out.println(r_ + " " + mx[j] + " " + my[j] + " " + mz[j] + " | k =  " + k[j] + " (" + countQ[j] +")");

				}
				System.out.println(count++);

			} while (change > 0);

			File cluster = new File(clusterFile);
			if (!cluster.exists()) {
			//values converged!
			System.out.println("success, writing file");
			//save clusters:
			File file = new File(clusterFile);
			FileWriter writer;
			try {
				writer = new FileWriter(file ,true);

				for (int i = 0; i <docs.length; i++) {
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


		//create index for docs in q if done
		for (int i = 0; i < docs.length; i++) {
			//qNd[q[i]].add(i);
			qd = addElement(qd,q[i],i);
		}			

		//save q for every document
		for (int i = 0; i < docs.length; i++) {
			docs[i].q = q[i];
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

				/*
				System.out.println(triangle_dt.p1().x() + " " + triangle_dt.p1().y() + " " + triangle_dt.p1().z());
				System.out.println(triangle_dt.p2().x() + " " + triangle_dt.p2().y() + " " + triangle_dt.p2().z());
				System.out.println(triangle_dt.p3().x() + " " + triangle_dt.p3().y() + " " + triangle_dt.p3().z());
				System.out.println();
				System.out.println();
				 */

			}
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



}

class EstQ implements Runnable {
	public int change;
	public int[] q;
	public double[] likelihood;
	//calculate new avg
	private int n;
	private double[] x;
	private double[] y;
	private double[] z;
	private double[] mx;
	private double[] my;
	private double[] mz;
	private double[] k;
	
	Thread t;

	EstQ (int nI, double[] mxI, double[] myI, double[] mzI,double[] kI, double[] xI, double[] yI, double[] zI, int[] qI, int lI) {
		String name = "Thread expectation q" + String.valueOf(lI);
		t = new Thread(this, name);
		n = nI;
		mx = mxI;
		my = myI;
		mz = mzI;
		k = kI;
		x= xI;
		y= yI;
		z= zI;	
		q=qI;
		t.start();
	}

	public void run() {

		for (int i=0; i < x.length; i++) {

			//Set pQ to minimum, Q to -1
			double logpQ =  -Double.MAX_VALUE;
			int newQ = -1;
			likelihood = new double[x.length];
			double logpQnew = 0.0;
			
			for (int j=0; j < n; j++) {

				double mytx = mx[j] * x[i] + my[j] * y[i] + mz[j] * z[i];
							
					logpQnew = Math.log(k[j]) + k[j] * mytx - k[j];
					//proportional to
					//logpQnew = k[j] * mytx;
					
					likelihood[i] += logpQnew;
					
					
				
				//update to q with maximum log likelihood
				if (logpQnew >= logpQ) {
					logpQ = logpQnew;
					newQ = j;
				}

			}
			
			if (newQ == -1) {
				System.out.println(likelihood[i]);
			}
			
			
			if (q[i] != newQ) {
				q[i] = newQ;
				//inform about change
				change++;		
			}
			


		}

	}
}