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

import ckling.geo.Coordinates;

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

public class GaussEM {

	//Index over neighbour clusters
	public ArrayList<CopyOnWriteArraySet<Integer>> qqN;
	//index over documents for clusters
	public ArrayList<CopyOnWriteArraySet<Integer>> qd;

	public Document[] docs = null;

	private double[] lons;
	private double[] lats;

	private boolean change;

	//two dimensions, x and y, K x 2
	public double[][] m;	
	//two dimensions, x and y, K x 2
	public double[][] sigma;
	public double[] rho;
	//assignment of faactor q to documents
	public double[][] q;
	public double[] p;
	private double[] pAvgQ;
	private double[] avgQ;

	private double likelihood;
	//array with all triangles, useful for creating the map
	public int[][] triangles;
	int[] countQ = null;
	//number of clusters
	public int Q;
	boolean skip = false;

	private int THREADS = 16;
	private boolean HARD_ASSIGNMENT = false;
	private double MAX_DIST = 99999; //kilometres
	private boolean useFile = false;


	public GaussEM (Document[] docs, int n, String dir) {

		Q = n;

		this.docs = docs;

		qqN = new ArrayList<CopyOnWriteArraySet<Integer>>();
		qd = new ArrayList<CopyOnWriteArraySet<Integer>>();
		for (int i = 0; i < Q; i++) {
			qqN.add(i,new CopyOnWriteArraySet<Integer>());
			qd.add(i,new CopyOnWriteArraySet<Integer>());
		}

		lats = new double[docs.length];
		lons = new double[docs.length];

		for (int i = 0; i < docs.length; i++) {

			lats[i] = docs[i].lat;
			lons[i] = docs[i].lon;

		}





		//System.out.println(x[0] + " " + y[0] + " " + z[0]);

		q = new double[docs.length][Q];
		m =  new double[Q][2];
		sigma =  new double[Q][4];
		rho = new double[Q];
		pAvgQ = new double[Q];

		for (int i = 0; i < Q; i++) {
			//set probabilities for each cluster to 1 / Q
			pAvgQ[i] = 1.0/Q;
			int randomPointId = (int) Math.floor(Math.random() * docs.length);

			m[i][0] = lats[randomPointId]+(Math.random()-0.5)*0.01; 
			m[i][1] = lons[randomPointId]+(Math.random()-0.5)*0.01; 

			
			sigma[i][0]=10;
			sigma[i][1]=0;
			sigma[i][2]=0;
			sigma[i][3]=10;
			rho[i] = -1.0 + Math.random()*2;

			//System.out.println(mx[i] + " " + my[i] + " " + mz[i]);

		}

		for (int i = 0; i <docs.length; i++) {
			//int randomCluster = (int) Math.floor(Math.random()*(Q));
			//q[i][randomCluster] = 1.0;
			if (HARD_ASSIGNMENT) {
				int randomCluster = (int) Math.floor(Math.random()*(Q));
				q[i][randomCluster]=1.0;
			}
			else {
				for (int j = 0; j < Q; j++) {
					q[i][j]=1.0/Q;
				}
			}
		}


		//initialize q
		String clusterFile = dir  + File.separator + "clusterGauss"+Q+".csv";
		try {
			File cluster = new File(clusterFile);
			if (useFile && cluster.exists()) {

				BufferedReader br  = new BufferedReader(new FileReader(clusterFile));
				String strLine;
				//Read File Line By Line
				int countLines = 0;
				while ((strLine = br.readLine()) != null &&  countLines <docs.length )   {
					int clusterId = Integer.valueOf(strLine);
					q[countLines][clusterId] = 1.0;
					countLines++;
				}


				maximization();




				//skip = true;

			}

		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int count = 0;

		//skip EM?
		if (!skip) {

			do {

				change = false;

				expectation();

				//debug: print stats
				System.out.println(count++);
				if (count%1 == 0) {
					int[] qCount = new int[Q];
					double[] qCount2 = new double[Q];
					int[] qMaxP;
					qMaxP = getq();
					for (int i=0; i < docs.length; i++) {
						qCount[qMaxP[i]]++;
						for (int j=0; j < Q; j++) {
							qCount2[j]+=q[i][j];
						}
					}
					for (int i=0; i < Q; i++) {
						System.out.println(i +" " + qCount[i] + " " + qCount2[i] + " " + m[i][0] + " " + m[i][1] + " " + sigma[i][0] + " " + sigma[i][3] + " " + rho[i]);
						//System.out.println(i +" " + qCount[i] + " " + qCount2[i]);
					}
				}
				
				maximization();
				
				
				
			} while (change);

		}

		int[] qMaxP;
		qMaxP = getq();

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
					writer.write(String.valueOf(qMaxP[i]) );
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
			qd = addElement(qd,qMaxP[i],i);
		}			

		//save q for every document
		for (int i = 0; i < docs.length; i++) {
			docs[i].q = qMaxP[i];
		}


		Delaunay_Triangulation dt = new Delaunay_Triangulation();

		Point_dt[] p = new Point_dt[Q];

		for (int i = 0; i < Q; i++) {
			p[i] = new Point_dt(m[i][0],m[i][1],0,i);
			dt.insertPoint(p[i]);
		}

		triangles = new int[dt.trianglesSize()][3];
		int i = 0;

		for (Iterator<Triangle_dt> iterator_dt = dt.trianglesIterator();iterator_dt.hasNext();) {

			Triangle_dt triangle_dt = iterator_dt.next();

			if (!triangle_dt.isHalfplane()) {
				
				int [] pid = new int[3];
				
				pid[0] = triangle_dt.p1().id();
				pid[1] = triangle_dt.p2().id();
				pid[2] = triangle_dt.p3().id();
							
				//check for too long distances (maxdist) where no information should be exchanged
				boolean longDistance = false;
				for (int j=0;j<3;j++) {
					for (int k=0;k<3;k++) {
						if (k!=j) {
							double dist = Coordinates.distFrom(lats[pid[j]], lons[pid[j]], lats[pid[k]], lons[pid[k]]);
							if (dist > MAX_DIST) {
								longDistance = true;
								break;
							}
						}
						if (longDistance) {
							break;
						}
					}
				}
				
				if (!longDistance) {
				
				for (int j=0; j<3; j++) {
					triangles[i][j]=pid[j]; 
				}
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

				for (int j=0;j<3;j++) {
					for (int k=0;k<3;k++) {
						if (k!=j) {
							qqN = addElement(qqN,pid[j],pid[k]);
						}
					}
				}
				

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


	private void expectation() {
		//assign factors

		double oldLikelihood = likelihood;
		likelihood = 0.0;
		avgQ = new double[Q];

		EstQ[] estq = new EstQ[THREADS];		

		int blockSize = docs.length / THREADS;
		int restSize = docs.length % THREADS;

		for (int i = 0; i < THREADS; i++) {
			int rest = 0;
			if (i == THREADS-1) {
				rest = restSize;
			}
			estq[i] = new EstQ(i*blockSize,(i+1)*blockSize + rest);
		}


		for (int thread=0;thread<THREADS;thread++) {
			try {
				estq[thread].t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		likelihood = Math.exp(likelihood / docs.length);
		if (oldLikelihood != 0) {
			System.out.println((likelihood/oldLikelihood)-1);
		}
		
		if (Math.abs((likelihood/oldLikelihood)-1) <= 0.0001) {
			//set change to false and break EM loop
			change = false;
		}

		//all clusters of equal size!
		for (int i = 0; i < Q; i++) {
			//pAvgQ[i] = avgQ[i] / docs.length;
			pAvgQ[i] = 1.0 / Q;
		}


	}

	private void maximization() {
		//calculate parameters

		double[][] mSum = new double[Q][2];
		double[] weightSum = new double[Q];


		for (int i = 0; i < docs.length; i++) {
			for (int j=0; j<Q; j++) {
				mSum[j][0]+=q[i][j] * lats[i];
				mSum[j][1]+=q[i][j] * lons[i];
				weightSum[j] += q[i][j];
			}
		}

		for (int j=0; j<Q; j++) {
			m[j][0] = mSum[j][0] / weightSum[j]; 
			m[j][1] = mSum[j][1] / weightSum[j]; 
		}

		double[] numeratorSum = new double[Q];
		double[] denominatorSumX = new double[Q];
		double[] denominatorSumY = new double[Q];


		//calculate variance
		for (int i = 0; i < docs.length; i++) {
			for (int j=0; j<Q; j++) {
				numeratorSum[j]+=q[i][j] * (lats[i]-m[j][0])*(lons[i]-m[j][1]);
				denominatorSumX[j]+=q[i][j] * (lats[i]-m[j][0])* (lats[i]-m[j][0]);
				denominatorSumY[j]+=q[i][j] * (lons[i]-m[j][1])* (lons[i]-m[j][1]);
			}
		}
		for (int j=0; j<Q; j++) {
			rho[j]=(numeratorSum[j]/weightSum[j]) / 
					(Math.sqrt(denominatorSumX[j]/weightSum[j]) * Math.sqrt(denominatorSumY[j]/weightSum[j]));

			//debug for inprecise double numbers. rho is correlation coefficient -1 <= rho <= 1
			if (rho[j] >=1.0) {
				rho[j]= 0.999999;
			}
			if (rho[j] <= -1.0) {
				rho[j]= -0.999999;
			}

			//adding a tiny variance as in WWW2011 code
			sigma[j][0] = denominatorSumX[j]/weightSum[j] + 0.00001;
			//sigma[j][1] = denominatorSumX[j]/weightSum[j];
			//sigma[j][2] = denominatorSumX[j]/weightSum[j];
			sigma[j][3] = denominatorSumY[j]/weightSum[j] + 0.00001;

			//simga 1 and 2 are rho*SQRT(sigma0)*SQRT(sigma1);
		}

	}

	class EstQ implements Runnable {
		private int start;
		private int limit;

		Thread t;

		//calculate q from document start to document limit
		EstQ (int start, int limit) {

			String name = "Thread expectation q" + String.valueOf(start);
			t = new Thread(this, name);
			this.start = start;
			this.limit = limit;
			t.start();
		}

		public void run() {

			for (int i = start; i < limit; i++) {
				double[] qNew = new double[Q];
				double pQsum = 0.;
				double likelihoodSum = 0.0;
				for (int j=0; j<Q; j++) {
					//pAvgQ[j] *  for different cluster weights
					double pQ = pAvgQ[j] *
							(1. / (Math.sqrt(sigma[j][0]) * Math.sqrt(sigma[j][3]) * Math.sqrt(1 - (rho[j]*rho[j])))) *
							Math.exp(
									(-1. / (2*(1 - rho[j]*rho[j]))) * (
											(Math.pow((lats[i]-m[j][0]),2) / sigma[j][0]) +
											(Math.pow((lons[i]-m[j][1]),2) / sigma[j][3]) -
											((2 * rho[j] * (lats[i]-m[j][0]) * (lons[i]-m[j][1])) / (Math.sqrt(sigma[j][0]) * Math.sqrt(sigma[j][3])))
											)
									)
									;

					//debug
					if (Double.isNaN(pQ)) {

						System.out.println("ERROR: pQ is NaN!!!");
						System.out.println(lats[i] + " " + lons[i] + " " +m[j][0] + " " + m[j][1] +" "+sigma[j][0] + " " + sigma[j][3]+ " " + rho[j]);
						System.out.println();

					}
					//if (Double.isInfinite(pQ)) pQ = 9999999999999.0;

					//double diffX = lats[i]-m[j][0];
					//double diffY = lons[i]-m[j][1];
					//if (i==0) System.out.println(pQ + " ("+ diffX +" | " + diffY +")");
					likelihoodSum += pQ;
					qNew[j] = pQ;
					pQsum+=pQ;
				}
				if (likelihoodSum == 0) {
					int j = 0;
					System.out.println("ERROR: likelihood == 0!!!");
					System.out.println(lats[i] + " " + lons[i] + " " +m[j][0] + " " + m[j][1] +" "+sigma[j][0] + " " + sigma[j][3]+ " " + rho[j]);
					System.out.println();

				}
				likelihood += Math.log(likelihoodSum);

				if (HARD_ASSIGNMENT) {
					double max = -1;
					int maxQold = -1;
					int maxQ = -1;
					for (int j=0; j<Q; j++) {
						if (q[i][j]==1.0) {
							maxQold = j;
						}
						if (qNew[j] > max) {
							max = qNew[j];
							maxQ = j;
						}						
					}
					if (maxQ != maxQold) {
						change = true;
						q[i][maxQold] = 0.0;
					} 

					q[i][maxQ] = 1.0;
					avgQ[maxQ]+=1.0;

				}
				else {				
					for (int j=0; j<Q; j++) {
						//normalize
						qNew[j]/=pQsum;
						
						if (!change && qNew[j]!=q[i][j]) {
							change = true;
						}

						q[i][j]=qNew[j];
						avgQ[j]+=q[i][j];
						//if (i==0) System.out.println(q[i][j]);

					}
				}

			}
		}
	}


	public double[][] getqm () {
		double[][] latLon =  {lats,lons};
		return latLon;
	}

	public double[][] getPq () {
		return q;
	}
	
	public int[] getq () {
		int[] qMaxP = new int[docs.length];
		for (int i = 0; i < q.length; i++) {
			double maxP = .0;
			for (int j = 0; j < q[i].length; j++) {
				if (i==1) {
					//System.out.println(q[i][j]);
				}
				if (q[i][j] > maxP) {
					qMaxP[i] = j;
					maxP = q[i][j];
				}
			}			
		}
		return qMaxP;
	}

}
