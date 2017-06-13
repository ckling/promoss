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

package jgibblda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;


import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;
import ckling.math.*;

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
	private double likelihood;
	double[] countQ = null;	
	private boolean skip = false;
	private boolean change;
	//number of clusters
	private int J;

	private static int nrThreads = 1;
	private static double kstart = 1;
	private boolean useFile = true;

//	private static double maxKGlobal = kstart;
	
	public KMeans (Document[] docs, int n, String dir, double trainingSize) {

		int trainingLength =  (int) (trainingSize*docs.length);
		
		this.docs = docs;
		J = n;
		
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
		
//		for (int i=0;i<x.length;i++) {
//			if (i%1==0) {
//				System.out.println(x[i] + " " + y[i] + " " + z[i]);
//			}
//		}

		//System.out.println(x[0] + " " + y[0] + " " + z[0]);

		q = new int[docs.length];
		likelihood  = 0;
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

//			double u = -1 + Math.random()*2;
//			double theta = Math.random()*2*Math.PI;
//			mx[i] = Math.cos(theta)* Math.sqrt(1 - (u*u)); 
//			my[i] = Math.sin(theta) * Math.sqrt(1 - (u*u));
//			mz[i] = u; 

		}

		for (int i = 0; i <docs.length; i++) {
			q[i] = (int) Math.round(Math.random()*(n-1));
		}


		//initialize q
		String clusterFile = dir  + File.separator + "cluster"+n+".csv";
		try {
			File cluster = new File(clusterFile);
			if (useFile && cluster.exists()) {

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
				countQ = new double[n];

				for (int i = 0; i < docs.length; i++) {
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



						k[j] = (r_ * 3 - r_ * r_ * r_) / (1 - r_*r_);
						//k[j] = 3r * (3/(3+2
						//k[j] = 2 / (2 - 2 * r_);
					
				}
				
				for (int j=0; j < J; j++) {
				System.out.println(mx[j] + " " + my[j] + " " + mz[j] + " | k =  " + k[j] + " (" + countQ[j] +")");
				}

				skip = true;

			}
			else {
				pQ = new double[docs.length][J];
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
				partSize = docs.length / nrThreads;		
				
				for (int l = 0; l < nrThreads; l++) {
					
					if (l == nrThreads-1) {
						limit = partSize + docs.length % nrThreads;
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

				for (int i = 0; i < trainingLength; i++) {
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

						//debug for numerical problems
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

						//dont stop sampling till every cluster is non empty
						
//						//one previous change was by an outlier which now gets his own cluster
						int minIndex = (int) (Math.random()*docs.length);
						
						//if there is a min likelihood
//						if (false && !Double.isNaN(min)) {
//						
						//set cluster centroid to point with min likelihood
						mx[j] = x[minIndex];
						my[j] = y[minIndex];
						mz[j] = z[minIndex];
						//set k to average k
						k[j] = kstart;
						countQ[q[minIndex]]--;
						q[minIndex] = j; 
						countQ[j] = 1;

						//set likelihood of the point to max for not reusing 
						//this point for another cluster centroid
//						
//						}
//						else {

//							double u = -1 + Math.random()*2;
//							double theta = Math.random()*Math.PI;
//							mx[j] = Math.cos(theta)* Math.sqrt(1 - (u*u)); 
//							my[j] = Math.sin(theta) * Math.sqrt(1 - (u*u));
//							mz[j] = u; 
							
//							int randPoint = (int) Math.floor(Math.random() * (docs.length-1));
//							mx[j] = x[randPoint]; 
//							my[j] = y[randPoint];
//							mz[j] = z[randPoint]; 
//							
//							k[j] = kstart;

//						}

					}
					double max = Math.pow(10,15);
					if (Double.isInfinite(k[j]) || k[j]>max) {
						//k[j] = BasicMath.max(k);
						k[j] = max;
					}

					//K-Means-Mode
					//k[j] = 100;
					//max k = 10^10
					//if (k[j] > Math.pow(10, 10)) {
					//	k[j]=Math.pow(10, 10);
					//}


					System.out.println(r_ + " " + mx[j] + " " + my[j] + " " + mz[j] + " | k =  " + k[j] + " (" + countQ[j] +")");

				}
				
				

				double likelihoodSum = 1.0;
				if (count > 0) {				
					
					likelihoodSum = likelihood;
					
					double ratio = (likelihoodSum / oldLikelihood) -1;
					System.out.println(ratio);
					//&& ratio >= 0
					if (count > 20 && ratio < 0.001) {
						//stop sampling
						change = false;
					}
				}
				oldLikelihood = likelihoodSum;
				likelihood = 0.0;
				
				System.out.println(count++);

			} while (change);

			if (useFile) {
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
			if (!Double.isNaN(mx[i])) {
			p[i] = new Point_dt(mx[i],my[i],mz[i],i);
			dt.insertPoint(p[i]);
			}
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
//				
//				double cosAlpha = (b*b + c*c - a*a) / (2 * b * c); 
//				double cosBeta = (a*a + c*c - b*b) / (2 * a * c); 
//				double cosGamma = (b*b + a*a - c*c) / (2 * b * a);
//				
//				//calculate angle in degrees
//				double alpha = Math.acos(cosAlpha);
//				double beta = Math.acos(cosBeta);
//				double gamma = Math.acos(cosGamma);
//				double minAngle = Math.min(alpha, Math.min(beta,gamma));
//				
//				//atan(1/4) is minimum angle, as in  "NON-OBTUSE TRIANGULATION OF A POLYGON"BS BAKER, E GROSSE, 1985
//				if (minAngle >= Math.atan(1./6.)) {
				
				double maxdist = Math.sqrt(1.0/Math.pow(2, 7));
				
				if (Math.max(Math.max(a,b),c)<maxdist) {
				
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
//				}
			}
			
			}
			System.out.println(i + " triangles");

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

				double mytx = mx[j] * x[i] + my[j] * y[i] + mz[j] * z[i];

				logpQnew[j] = Math.log(k[j]) + k[j] * (mytx - 1);
				if (soft) {
					pQnew[j] = Math.exp(logpQnew[j]);
					if (Double.isInfinite(pQnew[j])) {
						soft = false;
					}
				}				
				likelihood += logpQnew[j] + Math.log(1./J) ;

				//probability P(q) for every cluster is 1/J (that is why P(q|x) ~ P(x|q) / P(x) as seen above )
				
				//update to q with maximum log likelihood
				if (logpQnew[j] >= logpQ) {
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