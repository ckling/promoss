/*
 * Copyright (C) 2016 by
 * 
 * 	Christoph Carl Kling
 *	pcfst ät c-kling.de
 *  Institute for Web Science and Technologies (WeST)
 *  University of Koblenz-Landau
 *  west.uni-koblenz.de
 *
 * HMDP is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * HMDP is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCFST; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.gesis.promoss.inference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gesis.promoss.tools.math.BasicMath;
import org.gesis.promoss.tools.probabilistic.DirichletEstimation;
import org.gesis.promoss.tools.probabilistic.Gamma;
import org.gesis.promoss.tools.probabilistic.Pair;
import org.gesis.promoss.tools.text.Corpus;
import org.gesis.promoss.tools.text.DCTM_Corpus;
import org.gesis.promoss.tools.text.Save;
import org.knowceans.util.Vectors;

/**
 * This is the practical collapsed stochastic variational inference
 * for the Hierarchical Multi-Dirichlet Process Topic Model (HMDP)
 */
public class DCTM2_CVB_old {

	//This class holds the corpus and its properties
	//including metadata
	public DCTM_Corpus c;

	//We have a debugging mode for checking the parameters
	public boolean debug = false;
	//number of top words returned for the topic file
	public int topk = 100;
	//Number of read docs (might repeat with the same docs)
	public int RUNS = 500;
	//Save variables after step SAVE_STEP
	public int SAVE_STEP = 10;

	//How many observations do we take before updating alpha
	public int BATCHSIZE_ALPHA = 1000;
	//After how many steps a sample is taken to estimate alpha
	public int SAMPLE_ALPHA = 1;
	//Burn in phase: how long to wait till updating nkt?
	public int BURNIN = 0;
	//Burn in phase for documents: How long till we update hyperparameters
	public int BURNIN_DOCUMENTS = 1;
	//should the topics be randomly initialised?
	public double INIT_RAND = 1;


	public String save_prefix = "";

	public int K = 100; //Number of topics
	public int K2 = K; //Number of comment topics

	//global prior: G x K 
	public double[] alpha0;
	//concentration: GxK
	public double[][] alpha1;

	//Dirichlet concentration parameter for topic-word distributions
	public double beta = 0.01;
	public double beta2 = 0.01;

	//helping variable beta*V
	private double beta_V;
	private double beta2_V;

	//Store some zeros for empty documents in the doc_topic matrix?
	public Boolean store_empty = true;

	//Estimated number of times term t appeared in topic k: K x V
	private float[][] nkt;
	//Estimated number of words in topic k: K 
	private float[] nk;
	//Estimated number of times term t appeared in topic k: K' x V
	private float[][] nkt2;
	//Estimated number of words in topic k: K'
	private float[] nk2;
	//Topic counts per document: G x Gd x K
	private float[][][] nmk1;
	//Per document: Is K>0? G x Gd x K
	private float[][][] nmk1ge0;
	//Per document: Variance of every topic probability. G x Gd x K
	private float[][][] nmk1var;
	//private int[][] nm;
	//Topic counts per comment: G x Gd x Cd x K2
	private float[][][][] nmk2;
	//mmk table counts for the comments of document m: G x Gd x K
	private float[][][] mmk;
	//table counts per comment: G x Gd x Cd x K' x (K + 1)
	private float[][][][][] mmik;
	//table counts for the transition matrix: G x K x K'
	private double[][][] mgkk;
	//table sums for the transition matrix: G x K x K'
	private double[][] mgkSum;
	//tables of documents
	private double[][] mgk;
	//global topic weights
	private double[][] pi0gk;
	//Geometric expecation of alpha0*pik
	private double[][] gapk;


	//helper variables for estimating alpha1, one for each group
	//G x K x Cd x K
	private float[][][][] prioralpha1;
	private int[][][] nmkalpha1;
	private int[][] nmalpha1;
	//helper variables for estimating alpha2, one for each group
	//G x Gc x K2
	//private float[][][] nmkalpha2;
	//private float[][] nmalpha2;
	//index of current comment
	private int[] comment_counter;

	//global prior for each group
	//private double[] eta;


	//Zc: G x Gd x Cd x Nm x K'
	private float[][][][][] z2;
	//z: D x Nm x K
	private float[][][][] z;

	//counts, how many documents we observed in the batch to estimate alpha
	public int alpha_batch_counter = 0;

	public int rhot_step = 0;

	public double a0 = 1;
	public double b0 = 1;
	

	DCTM2_CVB_old() {
		c = new DCTM_Corpus();
	}

	public void initialise () {

		// Folder names, files etc. 
		c.dictfile = c.directory+"words.txt";
		//textfile contains the words of the document, all seperated by space (example line: word1 word2 word3 ... wordNm)
		c.documentfile = c.directory+"corpus.txt";
		c.metafile= c.directory+"meta.txt";
		//groupfile contains clus


		System.out.println("Creating dictionary...");
		c.readDict();	
		System.out.println("Initialising parameters...");
		initParameters();
		System.out.println("Processing documents...");
		c.readDocs();
		System.out.println("Estimating topics...");
		initParametersMeta();


	}

	public void run () {
		for (int i=0;i<RUNS;i++) {

			System.out.println(c.directory + " run " + i + " (avg. alpha "+ " beta " + beta + ")");

			onePass();

			if (rhot_step == RUNS) {
				//store inferred variables
				System.out.println("Storing variables...");
				save();
			}

		}
	}

	public void onePass() {
		rhot_step++;
		//get step size

		
		//reset variance and ge0
		for (int g=0;g<c.G;g++) {
			for (int d=0;d<c.Gd[g];d++) {
		for (int k=0;k<K;k++) {
			nmk1ge0[g][d][k]=0;
		}
			}
		}
		
		int progress = c.M / 50;
		if (progress==0) progress = 1;
		for (int m=0;m<Double.valueOf(c.M);m++) {
			if(m%progress == 0) {
				System.out.print(".");
			}

			inferenceDoc(m);
		}
		System.out.println();

		updateHyperParameters();

		for (int g=0; g<c.G;g++) {
			comment_counter[g]=0;
		}
	}

	//set Parameters
	public void initParameters() {

		beta_V = beta * c.V;



		c.V = c.dict.length();


		beta2_V = beta2 * c.V;

		nk = new float[K];
		nkt = new float[K][c.V];
		nk2 = new float[K2];
		nkt2 = new float[K2][c.V];


		//		//create initial imbalance
		//		for (int k=0;k<K;k++) {
		//			for (int t=0;t<c.V;t++) {
		//				nkt[k][t]=(float) Math.random();
		//				nk[k]+=nkt[k][t];
		//			}			
		//		}
		//
		//
		//		for (int k2=0;k2<K2;k2++) {
		//			for (int t=0;t<c.V;t++) {
		//				nkt2[k2][t]=(float) Math.random();
		//				nk2[k2]+=nkt2[k2][t];
		//			}			
		//		}


		//read corpus size and initialise nkt / nk
		c.readCorpusSize();

		System.out.println("Initialising count variables...");

	}

	public void initParametersMeta()  {

		//eta = new double[c.G];
		//for (int g=0;g<c.G;g++) {
		//	eta[g] = 0.5;
		//}

		mgk = new double[c.G][K];
		gapk= new double[c.G][K];
		pi0gk= new double[c.G][K];
		for (int g=0;g<c.G;g++) {
			for (int k=0;k<K;k++) {
				pi0gk[g][k]=1.0/K;
				gapk[g][k]=0.1;
			}
		}
		mgkk = new double[c.G][K][K2];
		mgkSum = new double[c.G][K];

		for (int g=0;g<c.G;g++) {
			for (int k=0;k<K;k++) {
				for (int k2=0;k2<K2;k2++) {

					mgkSum[g][k]+=mgkk[g][k][k2];
				}
			}
		}

		comment_counter = new int[c.G];

		z = new float[c.G][][][];
		z2 = new float[c.G][][][][];


		mmik = new float[c.G][][][][];
		for (int g=0;g<c.G;g++) {
			z[g] = new float[c.Gd[g]][][];
			z2[g] = new float[c.Gd[g]][][][];

			mmik[g] = new float[c.Gd[g]][][][];

			for (int d=0;d<c.Gd[g];d++) {
				//System.out.println(g + " " + d + " " + c.Cd[g][d]);

				z2[g][d] = new float[c.Cd[g][d]][][];
				mmik[g][d] = new float[c.Cd[g][d]][][];
			}
		}

		prioralpha1 = new float[c.G][][][];
		nmalpha1 = new int[c.G][];
		nmkalpha1 = new int[c.G][][];


		for (int g=0;g<c.G;g++) {
			for (int k=0;k<K;k++) {
				nmkalpha1[g] = new int[c.Gc[g]][K2];
				nmalpha1[g] = new int[c.Gc[g]];
				prioralpha1[g] = new float[c.Gc[g]][K][K2];

			}

			//nmkalpha2[g] = new float[c.Gc[g]][K];
			//nmalpha2 = new float[c.G][c.Cd[g]];

		}


		for (int g=0;g<c.G;g++) {
			comment_counter[g]=0;
		}



		for (int m=0;m<c.M;m++) {
			int g = c.meta[m][0];
			int ci = c.meta[m][2];
			if (ci > 0) {
				nmalpha1[g][comment_counter[g]] = c.getN(m);
				comment_counter[g]++;
			}

		}
		for (int g=0;g<c.G;g++) {
			comment_counter[g]=0;
		}

		for (int m=0;m<c.M;m++) {
			int g = c.meta[m][0];
			int d = c.meta[m][1];
			int ci = c.meta[m][2];
			if (ci > 0) {

				//System.out.println(d + " " + cd + " " + c.Cd[d]);
				//comment
				z2[g][d][ci-1]=new float[c.getTermIDs(m).length][K2];
				//System.out.println(m + " " + g + " " + d  + " " + ci + " "  + c.getTermIDs(m).length);
				//System.out.println(z2[g][d][ci-1].length);
				mmik[g][d][ci-1] = new float[K2][K]; 
			}
			else {
				//document
				z[g][d] =	new float[c.getTermIDs(m).length][K];
			}
		}


		alpha0 = new double[c.G];
		alpha1 = new double[c.G][K];

		for (int g=0;g<c.G;g++) {
			alpha0[g] = 1;
			for (int k=0;k<K;k++) {
				//alpha0[g][k]=0.01;
			}
			for (int k=0;k<K;k++) {
				alpha1[g][k]=K2*0.1;
			}
		}

		nmk1 = new float[c.G][][];

		nmk1ge0= new float[c.G][][];
		nmk1var= new float[c.G][][];

		//nm = new int[c.G][];
		for (int g=0;g<c.G;g++) {
			nmk1[g] = new float[c.Gd[g]][K];
			nmk1ge0[g] = new float[c.Gd[g]][K];
			nmk1var[g] = new float[c.Gd[g]][K];
			//nm[g] = new int[c.Gd[g]];
		}
		//		for (int m=0;m<c.M;m++) {
		//			int g = c.meta[m][0];
		//			int d = c.meta[m][1];
		//			int ci = c.meta[m][2];
		//			if (ci == 0) {
		//				//nm[g][d]=c.getN(m);
		//			}
		//		}

		nmk2 = new float[c.G][][][];
		for (int g=0;g<c.G;g++) {
			nmk2[g] = new float[c.Gd[g]][][];
			for (int d = 0; d < c.Gd[g]; d++) {
				nmk2[g][d] = new float[c.Cd[g][d]][K2];
			}

		}

		for (int t=0; t < c.V; t++) {
			for (int k=0;k<K;k++) {

				nkt[k][t]= (float) (Math.random()*INIT_RAND);
				nk[k]+=nkt[k][t];

			}
		}


		mmk = new float[c.G][][];
		for (int g=0;g<c.G;g++) {
			mmk[g] = new float[c.Gd[g]][K];
		}

	}




	public void inferenceDoc(int m) {

		int[] termIDs = c.getTermIDs(m);
		short[] termFreqs = c.getTermFreqs(m);

		int g = c.meta[m][0];
		int d = c.meta[m][1];
		int ci = c.meta[m][2];



		//Document inference

		if (ci == 0) {


			//Process words of the document
			for (int i=0;i<termIDs.length;i++) {

				//term index
				int t = termIDs[i];
				//How often doas t appear in the document?
				int termfreq = termFreqs[i];

				
				for (int k=0;k<K;k++) {
					if (g >= z.length || d >= z[g].length || i >= z[g][d].length || k >= z[g][d][i].length) {
						System.out.println(m +" " + g + " " + d + " " + i + " " + k);
					}
					nmk1[g][d][k]-=
							termfreq*z[g][d][i][k];
					nkt[k][t]-=termfreq*z[g][d][i][k];
					nk[k]-=termfreq*z[g][d][i][k];
					nmk1var[g][d][k]-=termfreq*(z[g][d][i][k]*(1.0-z[g][d][i][k]));

				}
				
				//topic probabilities - q(z)
				double[] q = new double[K];
				//sum for normalisation
				double qsum = 0.0;

				for (int k=0;k<K;k++) {


					if (rhot_step==1) {
						q[k]=10+Math.random();
					}else {
						q[k] = 	//probability of topic given feature & group
								(nmk1[g][d][k] + mmk[g][d][k] + gapk[g][k])
								//probability of topic given word w
								* (nkt[k][t] + beta) 
								/ (nk[k] + beta_V)
								* Math.exp(-(nmk1var[g][d][k]/(2*Math.pow(nmk1[g][d][k] + mmk[g][d][k] + gapk[g][k],2))))
								;
					}

					qsum+=q[k];


				}
				for (int k=0;k<K;k++) {
					q[k]/=qsum;
					z[g][d][i][k] = (float) q[k];
					nmk1[g][d][k]+=termfreq*q[k];
					nkt[k][t]+=termfreq*q[k];
					nk[k]+=termfreq*q[k];
					
					nmk1var[g][d][k]+=termfreq*(q[k]*(1.0-q[k]));
					nmk1ge0[g][d][k]+=termfreq*Math.log(1.0-q[k]);
					if (q[k]<0||q[k]>1 || Math.exp(nmk1ge0[g][d][k])>1) {
						System.out.println("qmap error in document: "+q[k] + " " + nmk1ge0[g][d][k] + " " + Math.exp(nmk1ge0[g][d][k]));
						System.exit(0);
					}

					
				}		


			}



		}
		//Comment inference
		else {

			//infer tables
			double[][] ge0 = new double[K][K2];
			for (int k=0;k<K;k++) {
				for (int k2=0;k2<K2;k2++) {
					ge0[k][k2]=1;
				}
			}

			//index of the commented document
			//int document_m = m-ci;

			//calculate prior
			double[] prior = new double[K2];
			//calculate sources of prior
			double[][] prior_sources = new double[K2][K];

			//the last index is for the global prior alpha2
			for (int k2=0;k2<K2;k2++) {
				for (int k=0;k<K;k++) {
					prioralpha1[g][comment_counter[g]][k][k2] = 0;
					nmkalpha1[g][comment_counter[g]][k2]=0;
				}
			}

			double[] theta = new double[K];
			double theta_sum = 0;

			for (int k=0;k<K;k++) {
				theta[k] = (nmk1[g][d][k] + mmk[g][d][k] + alpha0[g]);
				theta_sum+=theta[k];
			}
			for (int k=0;k<K;k++) {
				theta[k]/=theta_sum;
				//System.out.println(mmk[g][d][k]);
			}


			for (int k=0;k<K;k++) {

				for (int k2=0;k2<K2;k2++) {
					double temp = theta[k] 
							* (mgkk[g][k][k2] +1) / (mgkSum[g][k] +K2);
					double temp2 = temp * alpha1[g][k];
					prior[k2]+=  temp2;
					prior_sources[k2][k] =   temp2;
					//if (g==0 && d==0 && ci==1) {						
					//		System.out.println("first" + k + " " + k2 + " rhot: " + rhot_step + " "  + theta[k] +" " + nmk1[g][d][k] + " of " + BasicMath.sum(nmk1[g][d]) + " " + mmk[g][d][k]  + " "  + temp + " " + temp2 + " " + alpha1[g][k] + " " + mgkk[g][k][k2]);					
					//}
					//if (m<=10) {
					//System.out.println( k2 + " : " + prior[k2]);
					//}

					//System.out.println(prioralpha1[g].length + " " + c.Gc[g] + " "+ comment_counter[g]);
					prioralpha1[g][comment_counter[g]][k][k2]=(float) (temp);
				}
			}

			//prioralpha1[g][comment_counter[g]] = BasicMath.normalise(prioralpha1[g][comment_counter[g]]);

			for (int k2=0;k2<K2;k2++) {
				if (g==0 && d==0) {
					for (int k=0;k<K;k++) {
						//		System.out.println("unnorm " + k + " " + k2 + " " + prior_sources[k2][k]);
					}
				}
				//if (Double.isNaN(BasicMath.normalise(prior_sources[k2])[0])) {
				//	System.out.println("mist" + prior_sources[k2][0]);
				//}
				prior_sources[k2] = BasicMath.normalise(prior_sources[k2]);


				if (g==0 && d==0) {
					for (int k=0;k<K;k++) {
						//	System.out.println("norm " +k + " " + k2 + " " + prior_sources[k2][k]);
					}
				}
				//if (m<=10) {
				//System.out.println(alpha2[g][k2] + " " + (prior[k2]-alpha2[g][k2]));
				//}
			}

			//Process words of the document
			for (int i=0;i<termIDs.length;i++) {

				//term index
				int t = termIDs[i];
				//How often doas t appear in the document?
				int termfreq = termFreqs[i];

				//topic probabilities - q(z)
				double[] q = new double[K2];
				//sum for normalisation
				double qsum = 0.0;

				for (int k2=0;k2<K2;k2++) {
					if (c.getN(m) == termfreq) {
						nmk2[g][d][ci-1][k2] = 0;
					}
					else {

						nmk2[g][d][ci-1][k2]-=termfreq*z2[g][d][ci-1][i][k2];
						nkt2[k2][t]-=termfreq*z2[g][d][ci-1][i][k2];
						nk2[k2]-=termfreq*z2[g][d][ci-1][i][k2];
					}
				}
				for (int k2=0;k2<K2;k2++) {

					if (rhot_step==1) {
						q[k2]=10+Math.random();
					}else {
						q[k2] = 	//probability of topic given feature & group
								(nmk2[g][d][ci-1][k2] + prior[k2])
								//probability of topic given word w
								* (nkt2[k2][t] + beta) 
								/ (nk2[k2] + beta_V);
					}

					qsum+=q[k2];


				}
				
				//probability that source K was selected
				double[] qmapK = new double[K];
				
				
				for (int k2=0;k2<K2;k2++) {
					q[k2]/=qsum;
					z2[g][d][ci-1][i][k2] = (float) q[k2];
					nmk2[g][d][ci-1][k2]+=termfreq*q[k2];
					nkt2[k2][t]+=termfreq*q[k2];
					nk2[k2]+=termfreq*q[k2];
					
					
					//System.out.println(g + " "  + d + " " + ci + " " + i + " " + k2);
					//System.out.println(z2[g].length + " " + z2[g][d].length );
					//System.out.println(z2[g].length + " " + z2[g][d].length +" " + z2[g][d][ci-1].length );
					z2[g][d][ci-1][i][k2]=(float) q[k2];
					for (int k=0;k<K;k++) {
						//map probability of k2 to probability for source k
						double qmap = q[k2]*prior_sources[k2][k];
						ge0[k][k2]*=(1.0-(qmap));
						qmapK[k]+=qmap;
						
						if (Double.isNaN(ge0[k][k2])) {
							System.out.println(rhot_step + " " +qsum + " " + q[k2] + " " + prior_sources[k2][k]);
							System.exit(0);
						}
					}
				}	
				

				
				for (int k=0;k<K;k++) {
					//TODO: store q(k,k2) in z, not only q(k2) -> update var correctly
					//nmk1var[g][d][k]+=termfreq*(qmapK[k]*(1.0-qmapK[k]));
					//nmk1ge0[g][d][k]+=termfreq*Math.log(1.0-qmapK[k]);
				
				
				if (qmapK[k]<0 || qmapK[k]>1 || Math.exp(nmk1ge0[g][d][k])>1) {
					System.out.println("qmap error in comment: "+qmapK[k] + " " + nmk1ge0[g][d][k] + " " + Math.exp(nmk1ge0[g][d][k]));
					System.exit(0);
				}
				}
				
				double u = Math.random();
				double[] cumq = new double[K2];
				int sample_k = 0;
				for (int k2=0;k2<K2;k2++) {
					cumq[k2]+=q[k2];
					if (k2>0) {
						cumq[k2]+=cumq[k2-1];
					}
				}
				while (u>cumq[sample_k]) {
					sample_k++;
				}

				nmkalpha1[g][comment_counter[g]][sample_k]+=termfreq;


			}

			//double[] table = new double[K2];
			for (int k2=0;k2<K2;k2++) {

				//remove tables
				for (int k=0;k<K;k++) {	
					if (mmik[g][d][ci-1][k2][k]>0) {
						mmk[g][d][k] -= mmik[g][d][ci-1][k2][k];
						mgkk[g][k][k2] -= mmik[g][d][ci-1][k2][k];			
						mgkSum[g][k] -= mmik[g][d][ci-1][k2][k];	
						if (mmk[g][d][k]<0) {
							//System.out.println(mmik[g][d][ci-1][k2][k] + " " + mmk[g][d][k] + " " + mgkk[g][k][k2] + " " + mgkSum[g][k]);
							mmk[g][d][k]=0;
						}
					}

				}
			}
			for (int k2=0;k2<K2;k2++) {
				//calculate new expectation of tables as in Teh 06: CVB for DP
				for (int k=0;k<K;k++) {
					ge0[k][k2] = 1.0 - ge0[k][k2];	
				}
				//System.out.println(prior[k2] + nmk2[g][d][ci-1][k2]);
				if (nmk2[g][d][ci-1][k2]<= 1) {
					//	table[k2] = ge0[k2];
				}
				else {
					//	table[k2] = ge0[k2]; //TODO: prior[k2] * ge0[k2] * Gamma.digamma0(prior[k2] + nmk2[g][d][ci-1][k2]) - Gamma.digamma0(prior[k2]);
				}
				//if (table[k2]<0 || Double.isNaN(table[k2])) {
				//	table[k2] = 0;
				//}
				//				if (table[k2] > nmk2[g][d][ci-1][k2]) {
				//					System.out.println("mist " + ge0[k2] + " " +  nmk2[g][d][ci-1][k2]);
				//					table[k2] =   nmk2[g][d][ci-1][k2];
				//				}

				for (int k=0;k<K;k++) {					
					mmik[g][d][ci-1][k2][k]=(float) (ge0[k][k2]);
					if (Double.isNaN(mmik[g][d][ci-1][k2][k])) {
						System.out.println(ge0[k][k2]);
						System.exit(0);
					}
				}
				//update counts from global group prior alpha2



			}

			for (int k2=0;k2<K2;k2++) {


				for (int k=0;k<K;k++) {	
					mmk[g][d][k]+=mmik[g][d][ci-1][k2][k];		
					mgkk[g][k][k2]+=mmik[g][d][ci-1][k2][k];
					mgkSum[g][k]+=mmik[g][d][ci-1][k2][k];	
					//System.out.println(table[k2]);


				}






			}

			for (int k=0;k<K;k++) {	
				for (int k2=0;k2<K2;k2++) {
					//nmkalpha1[g][comment_counter[g]][k2]=(float) (nmk2[g][d][ci-1][k2] );
				}
			}
			//for (int k2=0;k2<K2;k2++) {
			//	nmkalpha2[g][comment_counter[g]][k2]=(float) (nmk2[g][d][ci-1][k2]*prior_sources[k2][K]);
			//}

			comment_counter[g]++;

		}


	}


	public void updateHyperParameters() {

		//System.out.println("stop");System.exit(0);

		if(rhot_step>BURNIN_DOCUMENTS && (rhot_step-1)%1 == 0) {


			//for (int g=0;g<c.G;g++) {

			//double a = BasicMath.sum(nmkalpha1[g])+ 1;
			//eta[g] = a / (a+ (BasicMath.sum(nmkalpha2[g]) + 1));
			//}

			//use counts of documents -> own variable!
			//TODO get transition matrix, calculate alpha1 based on tables and prior probabilities (asymmetric prior)
			System.out.println("Estimating alpha0...");

			for (int g=0;g<c.G;g++) {
				
				
				
				//System.out.println(BasicMath.sum(nmk1[g]) + " total words: " + c.C);
				double[][] nmk  = new double[c.Gd[g]][K];
				double[] nm = new double[c.Gd[g]];
				for (int d = 0;d<c.Gd[g];d++) {
					//nm[d]=0.00001;
					for (int k=0;k<K;k++) {
						nmk[d][k] = nmk1[g][d][k] + mmk[g][d][k];
						nm[d] += nmk[d][k];
						//System.out.println(d + " " + k + " " + sum[d][k]);
					}
				}
				if (BasicMath.sum(nm) > 1000) {
				
					//System.out.println(BasicMath.sum(nmk[0]) + " " + nm[0]);
					//alpha0[g] = DirichletEstimation.estimateAlphaMap(nmk,nm,alpha0[g],1.0,1.0);
					//alpha0[g] = DirichletEstimation.estimateAlphaMap(nmk,nm,alpha0[g],1,1);
				double eta = 0;

				for (int d=0;d<c.Gd[g];d++) {
					eta += Gamma.digamma(alpha0[g])-Gamma.digamma(alpha0[g]+BasicMath.sum(nmk1[g][d]));
				}
				
				double summ = BasicMath.sum(mgk[g]);
				if (summ == 0) {
					for (int d = 0;d<c.Gd[g];d++) {
						for (int k=0;k<K;k++) {
							double ge0 = 1.0-Math.exp(nmk1ge0[g][d][k]);
							mgk[g][k]+=ge0;
							summ+=ge0;
						}
					}
					//System.out.println(summ + " / "+BasicMath.sum(nm));
				}
				
				double galpha = Math.exp(Gamma.digamma(a0 + summ)) / (b0 - eta);
				for (int k=0;k<K;k++) {
					gapk[g][k]=galpha;
					//pik ~ Dir(m1+1,...,mK+1)
					gapk[g][k]*=Math.exp(Gamma.digamma(1 + mgk[g][k])) / Math.exp(Gamma.digamma(K + summ));
					//System.out.println("gap: " +gapk[k]);

				}
				
				for (int k=0;k<K;k++) {
					mgk[g][k]=0;
				}
				
				//Get tables like in Teh06 (second order Taylor for m>0)
				for (int d = 0;d<c.Gd[g];d++) {
				for (int k=0;k<K;k++) {
					double ge0 = 1.0-Math.exp(nmk1ge0[g][d][k]);
					//System.out.println(ge0);
					
					 
					//mmk[g][d][k]=(float) (ge0 * gapk[k] * Gamma.digamma(gapk[k] + nmk1[g][d][k]/ge0) - Gamma.digamma(gapk[k]) 
					//		+(nmk1var[g][d][k] * Gamma.trigamma(gapk[k] + nmk1[g][d][k]/ge0))/2 );
					
					double tables = 0;
					
					if (nmk1[g][d][k]<=1) {
						tables = ge0;
					}
					else {
					tables=(float) (ge0 * gapk[g][k] * 
									(Gamma.digamma(gapk[g][k] + nmk1[g][d][k]/ge0) - Gamma.digamma(gapk[g][k]) 
									+((((nmk1var[g][d][k]/ge0 -Math.exp(nmk1ge0[g][d][k])) * nmk1[g][d][k]/ge0) * Gamma.trigamma(gapk[g][k] + nmk1[g][d][k]/ge0))/2) ));
					}
					
					if (1==2 && tables > nmk1[g][d][k]) {
						double first = ge0 * gapk[g][k] *(Gamma.digamma(gapk[g][k] + nmk1[g][d][k]/ge0) - Gamma.digamma(gapk[g][k]));
						System.out.println("0-order: " + first + " " + nmk1[g][d][k]/ge0 + "  "+ nmk1[g][d][k] + " " + ge0  + " " + gapk[g][k]);
						double second = ((((nmk1var[g][d][k]/ge0 -Math.exp(nmk1ge0[g][d][k])) * nmk1[g][d][k]/ge0) * Gamma.trigamma(gapk[g][k] + nmk1[g][d][k]/ge0))/2) ;
						System.out.println("2-order: " + second);
						System.exit(0);
					}
					
					mgk[g][k]+=tables;
				}
				}
				summ = BasicMath.sum(mgk[g]);

				//pik ~ Dir(m1+1,...,mK+1)
				for (int k=0;k<K;k++) {
				pi0gk[g][k]= (mgk[g][k]+1)/(summ+K);
				}
				
				alpha0[g] = (a0 + summ) / (b0 - eta);
				System.out.println(alpha0[g] + " " + summ + " " + eta + " " + BasicMath.sum(nm) + " " + c.Gd[g] + " " + c.Gc[g]);

				}
			}
			System.out.println("Estimating alpha1...");


			//double[][] nmalpha2 = new double[c.G][];
			for (int g=0;g<c.G;g++) {
				//nmalpha2[g]=new double[c.Gc[g]];
				for (int j=0;j<c.Gc[g];j++) {
					//nmalpha2[g][j] = BasicMath.sum(nmkalpha2[g][j]);
				}
			}

			for (int g=0;g<c.G;g++) {

				//for (int k=0;k<K;k++) {	
				//TODO: fix estimator

				//System.out.println(BasicMath.sum(nmalpha1[g][k]) 
				//		+ " " + BasicMath.sum(nmkalpha1[g][k]) 
				//		+ " " + BasicMath.sum(prioralpha1[g][k]) 
				//		+ " " +alpha1[g][k]);
				//System.out.print("prior: ");
				//for (int k2=0;k2<K2;k2++) {
				//	for (int k=0;k<K;k++) {
				//		System.out.print(prioralpha1[g][0][k][k2] + " ");
				//	}
				//}
				//System.out.println();

				//if (c.Gd[g] > 1000) {

				alpha1[g] = DirichletEstimation.estimateAlphaLBFGS(nmalpha1[g],nmkalpha1[g],prioralpha1[g],alpha1[g]);
				//}
				//alpha1[g][k]=DirichletEstimation.estimateAlphaNewton(nmalpha1[g][k],nmkalpha1[g][k],prioralpha1[g][k],alpha1[g][k]);
				//System.out.println(BasicMath.sum(alpha1[g]));
				//}
			}
			//System.out.println("Estimating alpha2...");


			for (int g=0;g<c.G;g++) {
				//System.out.println(BasicMath.sum(nmkalpha2[g]));

				//alpha2[g]=DirichletEstimation.estimateAlphaLik(nmkalpha2[g],alpha2[g]);

			}

		}


	}


	public void save () {

		String output_base_folder = c.directory + "output_DCTM2/";

		File output_base_folder_file = new File(output_base_folder);
		if (!output_base_folder_file.exists()) output_base_folder_file.mkdir();

		String output_folder = output_base_folder + rhot_step + "/";

		File file = new File(output_folder);
		if (!file.exists()) file.mkdir();

		Save save = new Save();
		save.saveVar(nkt, output_folder+save_prefix+"nkt");
		save.close();
		save.saveVar(nkt2, output_folder+save_prefix+"nkt2");
		save.close();
		for (int g=0;g<c.G;g++) {
			save.saveVar(nmk1[g], output_folder+save_prefix+"nmk_"+g);
			save.close();
		}

		save.close();
		save.saveVar(alpha0, output_folder+save_prefix+"alpha0");
		save.close();
		save.saveVar(alpha1, output_folder+save_prefix+"alpha1");
		save.close();

		//alpha1[g] = DirichletEstimation.estimateAlphaLBFGS(nmalpha1[g],nmkalpha1[g],prioralpha1[g],alpha1[g]);
		for (int g=0;g<c.G;g++) {
			save.saveVar(nmalpha1[g], output_folder+save_prefix+"nmalpha1_"+g);
			save.close();
			save.saveVar(nmkalpha1[g], output_folder+save_prefix+"nmkalpha1_"+g);
			save.close();
			save.saveVar(prioralpha1[g][0], output_folder+save_prefix+"prioralpha0_"+g);
			save.close();
		}


		//save.saveVar(eta, output_folder+save_prefix+"eta");
		//save.close();

		for (int g=0;g<c.G;g++) {

			double[][] pi = new double[K][K2];

			for (int k=0;k<K;k++) {
				for (int k2=0;k2<K2;k2++) {
					pi[k][k2] = (mgkk[g][k][k2] +1)/(mgkSum[g][k] + K2);
				}
			}

			save.saveVar(pi, output_folder+save_prefix+"pikk2_"+g);
			save.close();

		}

		//We save the large document-topic file every 10 save steps, together with the perplexity
		if ((rhot_step % (SAVE_STEP *10)) == 0) {

			//save.saveVar(perplexity(), output_folder+save_prefix+"perplexity");

		}

		if (topk > c.V) {
			topk = c.V;
		}


		String[][] topktopics = new String[K*2][topk];

		for (int k=0;k<K;k++) {

			List<Pair> wordprob = new ArrayList<Pair>(); 
			for (int v = 0; v < c.V; v++){
				wordprob.add(new Pair(c.dict.getWord(v), (nkt[k][v]+beta)/(nk[k]+beta_V), false));
			}
			Collections.sort(wordprob);

			for (int i=0;i<topk;i++) {
				topktopics[k*2][i] = (String) wordprob.get(i).first;
				topktopics[k*2+1][i] = String.valueOf(wordprob.get(i).second);
			}

		}
		save.saveVar(topktopics, output_folder+save_prefix+"topktopics");



		String[][] topktopicsc = new String[K2*2][topk];

		for (int k2=0;k2<K2;k2++) {

			List<Pair> wordprob = new ArrayList<Pair>(); 
			for (int v = 0; v < c.V; v++){
				wordprob.add(new Pair(c.dict.getWord(v), (nkt2[k2][v]+beta2)/(nk2[k2]+beta2_V), false));
			}
			Collections.sort(wordprob);

			for (int i=0;i<topk;i++) {
				topktopicsc[k2*2][i] = (String) wordprob.get(i).first;
				topktopicsc[k2*2+1][i] = String.valueOf(wordprob.get(i).second);
			}

		}
		save.saveVar(topktopicsc, output_folder+save_prefix+"topktopicsc");

	}



}
