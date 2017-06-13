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
public class DCTM6_CVB {


	//This class holds the corpus and its properties
	//including metadata
	public DCTM_Corpus c;

	//do we calculate the perplexity?
	public boolean test = false;

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

	//global prior: G x K 
	public double[] alpha0;
	//concentration: G
	public double[] alpha1;

	//Dirichlet concentration parameter for topic-word distributions
	public double beta = 0.01;
	public double Gbeta = 0.01;


	public double gamma = 1;
	public double Ggamma = 1;

	//helping variable beta*V
	private double beta_V;

	//Store some zeros for empty documents in the doc_topic matrix?
	public Boolean store_empty = true;

	//Estimated number of times term t appeared in topic k: K x V
	private double[][] nkt;
	//Probability that term t ever was assigned to topic k: K x V
	private double[][] nkt0;
	//Var of nkt: K x V
	private double[][] nktvar;
	//Var of nk: K
	private double[] nkvar;


	private double[][][] mgkk0;
	private double[][][] mgkkvar;

	//Estimated number of words in topic k: K 
	private double[] nk;
	//Topic counts per document: G x Gd x K
	private float[][][] nmk1;
	//Per document: Is K>0? G x Gd x K
	private float[][][] nmk10;
	//Per document: Variance of every topic probability. G x Gd x K
	private float[][][] nmk1var;
	//Per document: Variance of every topic probability in comments. G x Gd x Cd x (K)*K2
	private float[][][][] nmk2var;
	//private int[][] nm;
	//Topic counts per comment: G x Gd x Cd x (K)*K2
	private float[][][][] nmk2;
	//mmk table counts for the comments of document m: G x Gd x K
	private float[][][] mdk;
	//varmmk var of table counts for the comments of document m: G x Gd x K
	private float[][][] varmdk;
	//are tables positive? for the comments of document m: G x Gd x K
	private float[][][] mdkequal0;
	//table counts per comment: G x Gd x Cd x (K' x (K + 1))
	private float[][][][] mdc;
	//table counts for the transition matrix: G x K x K'
	private double[][][] mgkk;
	//table sums for the transition matrix: G x K x K'
	private double[][] mgkSum;
	//tables of groups
	private double[] mg;
	//geometric expectation of alphag*delta G x (K' x (K + 1))
	private double[] Galpha1;

	//tables of documents
	private double[][] mgk;
	//global topic weights
	private double[][] pi0gk;
	//Geometric expecation of alpha0*pik
	private double[][] gapk;
	//tables for topics
	private double mbeta;
	//tables for delta
	private double mgamma;

	//index of current comment
	private int[] comment_counter;

	//auxiliary variables
	private double[][][] eta;
	private double[] eta_sum;


	//Zc: G x Gd x Cd x Nm x K'
	private float[][][][][] z2;
	private float[][][][][] m20;
	private float[][][][][] m2var;
	//z: D x Nm x K
	private float[][][][] z;

	//counts, how many documents we observed in the batch to estimate alpha
	public int alpha_batch_counter = 0;

	public int rhot_step = 0;

	public double a0 = 1;
	public double b0 = 1;


	DCTM6_CVB() {
		c = new DCTM_Corpus();
	}

	public void initialise () {

		// Folder names, files etc. 
		c.dictfile = c.directory+"words.txt";
		//textfile contains the words of the document, all seperated by floatspace (example line: word1 word2 word3 ... wordNm)
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

		int progress = c.M / 50;
		if (progress==0) progress = 1;
		for (int m=0;m<Double.valueOf(c.M);m++) {
			if(m%progress == 0) {
				System.out.print(".");
			}

			inferenceDoc(m);

		}
		System.out.println();

		if (!test) {
			System.out.print("Updating hyperparameters...");

			updateHyperParameters();

			System.out.println(" done.");
		}

		//reset parameters

		for (int g=0; g<c.G;g++) {
			comment_counter[g]=0;
			//reset information if comment-tables are > 0
			mdkequal0[g]=BasicMath.setTo(mdkequal0[g], 1.0f);
		}

	}

	public double[] perplexity() {

		int sumN = 0;
		int sumNPost = 0;
		int sumNComment = 0;

		double ppx = 0;
		double ppxPost = 0;
		double ppxComment = 0;

		int progress = c.M / 50;
		if (progress==0) progress = 1;
		for (int m=0;m<Double.valueOf(c.M);m++) {
			if(m%progress == 0) {
				System.out.print(".");
			}

			double ppxm = perplexity(m);
			ppx+=ppxm;
			sumN+=c.getN(m);

			//Post
			if (c.meta[m][2]==0) {
				sumNPost+=c.getN(m);
				ppxPost+=ppxm;

			}
			//Comment
			else {
				sumNComment+=c.getN(m);
				ppxComment+=ppxm;
				//System.out.println(ppxComment + " " + sumNComment + " " + Math.exp(-ppxComment/sumNComment));
			}


		}
		System.out.println();


		//reset parameters

		for (int g=0; g<c.G;g++) {
			comment_counter[g]=0;
			//reset information if comment-tables are > 0
			mdkequal0[g]=BasicMath.setTo(mdkequal0[g], 1.0f);
		}




		double result = Math.exp(-ppx/sumN);
		double resultPost = Math.exp(-ppxPost/sumNPost);
		double resultComment = Math.exp(-ppxComment/sumNComment);



		String output_base_folder = c.directory + "output_DCTM6/";

		File output_base_folder_file = new File(output_base_folder);
		if (!output_base_folder_file.exists()) output_base_folder_file.mkdir();

		Save save = new Save();
		save.saveVar(result, output_base_folder+save_prefix+"perplexity");
		save.close();
		save = new Save();
		save.saveVar(resultPost, output_base_folder+save_prefix+"perplexityPost");
		save.close();
		save = new Save();
		save.saveVar(resultComment, output_base_folder+save_prefix+"perplexityComment");
		save.close();

		System.out.println(result + " " + resultPost + " " + resultComment);

		double[] res = {result,resultPost,resultComment};
		return(res);

	}

	//set Parameters
	public void initParameters() {

		c.V = c.dict.length();


		if (!test) {
			beta_V = beta * c.V;

			nk = new double[K];
			nkt = new double[K][c.V];
			nkt0 = new double[K][c.V];
			BasicMath.setTo(nkt0, 1.0);
			nktvar = new double[K][c.V];
			nkvar = new double[K];




		}

		//		//create initial imbalance
		//		for (int k=0;k<K;k++) {
		//			for (int t=0;t<c.V;t++) {
		//				nkt[k][t]=(float) Math.random();
		//				nk[k]+=nkt[k][t];
		//			}			
		//		}
		//
		//
		//		for (int k2=0;k2<K;k2++) {
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

		if (!test) {

			mg = new double[c.G];

			eta = new double[c.G][][];
			for (int g=0;g<c.G;g++) {
				eta[g] = new double[c.Gd[g]][];
				for (int d=0;d<c.Gd[g];d++) {
					eta[g][d] = new double[c.Cd[g][d]];
				}
			}
			eta_sum = new double[c.G];

			mgk = new double[c.G][K];
			gapk= new double[c.G][K];
			pi0gk= new double[c.G][K];
			for (int g=0;g<c.G;g++) {
				for (int k=0;k<K;k++) {
					pi0gk[g][k]=1.0/K;
					gapk[g][k]=0.1;
				}
			}
			mgkk = new double[c.G][K][K];

			mgkSum = new double[c.G][K];

			for (int g=0;g<c.G;g++) {
				for (int k=0;k<K;k++) {
					for (int k2=0;k2<K;k2++) {

						mgkSum[g][k]+=mgkk[g][k][k2];
					}
				}
			}

		}

		comment_counter = new int[c.G];

		z = new float[c.G][][][];
		z2 = new float[c.G][][][][];
		m20 = new float[c.G][][][][];
		m2var = new float[c.G][][][][];

		mdc = new float[c.G][][][];
		for (int g=0;g<c.G;g++) {
			z[g] = new float[c.Gd[g]][][];
			z2[g] = new float[c.Gd[g]][][][];
			m20[g] = new float[c.Gd[g]][][][];
			m2var[g] = new float[c.Gd[g]][][][];

			mdc[g] = new float[c.Gd[g]][][];

			for (int d=0;d<c.Gd[g];d++) {
				//System.out.println(g + " " + d + " " + c.Cd[g][d]);

				z2[g][d] = new float[c.Cd[g][d]][][];
				mdc[g][d] = new float[c.Cd[g][d]][];
				m20[g][d] = new float[c.Cd[g][d]][][];
				m2var[g][d] = new float[c.Cd[g][d]][K][K];
			}
		}



		for (int g=0;g<c.G;g++) {
			comment_counter[g]=0;
		}


		for (int m=0;m<c.M;m++) {
			int g = c.meta[m][0];
			int ci = c.meta[m][2];
			if (ci > 0) {
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
				z2[g][d][ci-1]=new float[c.getTermIDs(m).length][K];
				m20[g][d][ci-1]=new float[K][K];
				//System.out.println(m + " " + g + " " + d  + " " + ci + " "  + c.getTermIDs(m).length);
				//System.out.println(z2[g][d][ci-1].length); 
				mdc[g][d][ci-1] = new float[K*(K)]; 
			}
			else {
				//document
				z[g][d] =	new float[c.getTermIDs(m).length][K];
			}
		}

		if (!test) {

			alpha0 = new double[c.G];
			alpha1 = new double[c.G];
			Galpha1 = new double[c.G];


			for (int g=0;g<c.G;g++) {
				alpha0[g] = 1;
				alpha1[g] = 0.1*K;
				Galpha1[g]=0.1*K;

				for (int k=0;k<K;k++) {
					//alpha0[g][k]=0.01;
				}
			}



			for (int m=0;m<c.M;m++) {


				int g = c.meta[m][0];
				int d = c.meta[m][1];
				int ci = c.meta[m][2];
				if (ci != 0) {
					eta[g][d][ci-1] = 
							alpha1[g]/(c.getN(m)+alpha1[g]);
					eta_sum[g]+= eta[g][d][ci-1];
				}

			}

			for (int g=0;g<c.G;g++) {
				mg[g] = c.Gc[g];

				mg[g]=0;
			}
		}

		nmk1 = new float[c.G][][];

		nmk10= new float[c.G][][];
		nmk1var= new float[c.G][][];

		//nm = new int[c.G][];
		for (int g=0;g<c.G;g++) {
			nmk1[g] = new float[c.Gd[g]][K];
			nmk10[g] = new float[c.Gd[g]][K];
			nmk1var[g] = new float[c.Gd[g]][K];
			//nm[g] = new int[c.Gd[g]];
		}

		nmk2 = new float[c.G][][][];
		for (int g=0;g<c.G;g++) {
			nmk2[g] = new float[c.Gd[g]][][];
			for (int d = 0; d < c.Gd[g]; d++) {
				nmk2[g][d] = new float[c.Cd[g][d]][K];
			}
		}

		nmk2var = new float[c.G][][][];
		for (int g=0;g<c.G;g++) {
			nmk2var[g] = new float[c.Gd[g]][][];
			for (int d = 0; d < c.Gd[g]; d++) {
				nmk2var[g][d] = new float[c.Cd[g][d]][K];
			}
		}


		mgkk0 = new double[c.G][K][K];
		BasicMath.setTo(mgkk0, 1.0);
		mgkkvar = new double[c.G][K][K];

		if (!test) {

			for (int t=0; t < c.V; t++) {
				for (int k=0;k<K;k++) {

					nkt[k][t]= (float) (Math.random()*INIT_RAND);
					nk[k]+=nkt[k][t];

				}
			}

		}

		mdk = new float[c.G][][];
		for (int g=0;g<c.G;g++) {
			mdk[g] = new float[c.Gd[g]][K];
		}
		varmdk = new float[c.G][][];
		for (int g=0;g<c.G;g++) {
			varmdk[g] = new float[c.Gd[g]][K];
		}

		mdkequal0 = new float[c.G][][];
		for (int g=0;g<c.G;g++) {
			mdkequal0[g] = new float[c.Gd[g]][K];
			mdkequal0[g]=BasicMath.setTo(mdkequal0[g], 1.0f);
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
					//if (g >= z.length || d >= z[g].length || i >= z[g][d].length || k >= z[g][d][i].length) {
					//	System.out.println(m +" " + g + " " + d + " " + i + " " + k);
					//}
					nmk1[g][d][k]-=termfreq*z[g][d][i][k];
					if (nmk1[g][d][k]<0)nmk1[g][d][k]=0;

					if (!test) {
						nkt[k][t]-=termfreq*z[g][d][i][k];
						nk[k]-=termfreq*z[g][d][i][k];
					}

					nmk10[g][d][k]-=termfreq*Math.log(1.0-z[g][d][i][k]);
					nmk1var[g][d][k]-=termfreq*(z[g][d][i][k]*(1.0-z[g][d][i][k]));
					if (!test) {
						nkt0[k][t]/=Math.pow((1.0-z[g][d][i][k]),termfreq);
						nktvar[k][t]-=termfreq*z[g][d][i][k]*(1.0-z[g][d][i][k]);
						if (nktvar[k][t]<0) nktvar[k][t]=0;
						nkvar[k]-=termfreq*z[g][d][i][k]*(1.0-z[g][d][i][k]);
						if (nkvar[k]<0) nkvar[k]=0;
					}
				}

				//topic probabilities - q(z)
				double[] q = new double[K];
				//sum for normalisation
				double qsum = 0.0;

				for (int k=0;k<K;k++) {


					if (rhot_step==1) {
						q[k]=Math.random();
					}else {
						q[k] = 	//probability of topic given feature & group
								(nmk1[g][d][k] + mdk[g][d][k] + gapk[g][k])
								//probability of topic given word w
								* (nkt[k][t] + Gbeta) 
								/ (nk[k] + c.V * Gbeta)
								* Math.exp(-(nmk1var[g][d][k]/(2*Math.pow(nmk1[g][d][k] + mdk[g][d][k] + gapk[g][k],2))))
								*Math.exp(-nktvar[k][t]/(2*Math.pow(nkt[k][t] + Gbeta,2)) + nkvar[k]/(2*Math.pow(nk[k] + c.V*Gbeta,2)))
								;
					}

					qsum+=q[k];


				}
				for (int k=0;k<K;k++) {
					q[k]/=qsum;
					z[g][d][i][k] = (float) q[k];
					nmk1[g][d][k]+=termfreq*q[k];
					if (!test) {
						nkt[k][t]+=termfreq*q[k];
						nkt0[k][t]*=Math.pow((1.0-q[k]),termfreq);
						nktvar[k][t]+=termfreq*q[k]*(1.0-q[k]);
						nkvar[k]+=termfreq*q[k]*(1.0-q[k]);
						nk[k]+=termfreq*q[k];
					}

					nmk1var[g][d][k]+=termfreq*(q[k]*(1.0-q[k]));
					nmk10[g][d][k]+=termfreq*Math.log(1.0-q[k]);
					if (Math.exp(nmk10[g][d][k])>1) {
						nmk10[g][d][k]=0;
					}
					if (q[k]<0||q[k]>1) {
						System.out.println("qmap error in document: "+q[k] + " " + nmk10[g][d][k] + " " + Math.exp(nmk10[g][d][k]));
						q[k] = 	//probability of topic given feature & group
								(nmk1[g][d][k] + mdk[g][d][k] + gapk[g][k])
								//probability of topic given word w
								* (nkt[k][t] + Gbeta) 
								/ (nk[k] + c.V * Gbeta);
					}


				}		

				//reset variance of tables after seeing the document
				varmdk[g][d] = new float[K];


			}



		}
		//Comment inference
		else {

			double[] Vndck = new double[K*(K)];

			double[] theta = new double[K];
			double theta_sum = 0;

			double[] Gtheta = new double[K];
			double[][] pi = new double[K][K];
			double[][] Gpi = new double[K][K];

			for (int k=0;k<K;k++) {
				theta[k] = (nmk1[g][d][k] + mdk[g][d][k] + alpha0[g] * pi0gk[g][k]);
				//if (rhot_step > 5)	
				if (debug)System.out.println(k + " " + nmk1[g][d][k] + " " + mdk[g][d][k] + " " + alpha0[g] * pi0gk[g][k]);

				if (theta[k] < 0) {
					if (debug)System.out.println("theta < 0: " + nmk1[g][d][k] + " " + mdk[g][d][k] + " " + alpha0[g]);
				}

				theta_sum+=theta[k];
				if (debug)System.out.println(nmk1[g][d][k] + " " + mdk[g][d][k] + " " + alpha0[g]);
				Gtheta[k] = Gamma.digamma(theta[k]);

				double sum = 0;
				for (int k2=0;k2<K;k2++) {
					double tmp =mgkk[g][k][k2] +gamma;
					Gpi[k][k2] = Gamma.digamma(tmp);
					pi[k][k2]=tmp;
					sum += tmp; 
				}
				double Gsum = Gamma.digamma(sum);
				for (int k2=0;k2<K;k2++) {
					Gpi[k][k2]=Math.exp(Gpi[k][k2]-Gsum);
					pi[k][k2]/=sum;
				}
			}

			for (int k=0;k<K;k++) {
				theta[k]/=theta_sum;
				if (debug)System.out.println("#");

				Gtheta[k]=Math.exp(Gtheta[k]-Gamma.digamma(theta_sum));
			}

			//for every table: p>0
			double[] mequal0 = new double[K];
			BasicMath.setTo(mequal0, 0);

			for (int k=0;k<K;k++) {
				for (int k2=0;k2<K;k2++) {
					mgkk0[g][k2][k]/=1.0-m20[g][d][ci-1][k2][k];
					mgkkvar[g][k2][k]-=m2var[g][d][ci-1][k2][k];
				}
			}


			double[] prior = new double[K];
			double[][] prior_parts = new double[K][K];
			for (int k2=0;k2<K;k2++) {
				for (int k=0;k<K;k++) {
					double denom = Gamma.digamma(mgkSum[g][k] +K*gamma);
					double tmp = Galpha1[g] * Gtheta[k] * Math.exp(Gamma.digamma(mgkk[g][k][k2] + gamma) - denom);
					prior[k2] += tmp ;
					prior_parts[k2][k] = theta[k]*pi[k][k2]*alpha1[g]; 
				}

				if (Double.isInfinite(prior[k2])) {
					System.out.println("Infinite prior!");
					BasicMath.print(Gtheta);
					BasicMath.print(mgkk[g]);

					System.exit(0);

				}

				BasicMath.normaliseDirect(prior_parts[k2]);
			}
			if (debug){
				System.out.println("Document length: "+ c.getN(m));
				for (int i=0;i<c.getTermIDs(m).length;i++) {
					System.out.print(c.dict.getWord(c.getTermIDs(m)[i]) + " ");
				}
				System.out.println();
				BasicMath.print(prior);
				BasicMath.print(nmk2[g][d][ci-1]);
			}

			//Process words of the document
			for (int i=0;i<termIDs.length;i++) {

				//term index
				int t = termIDs[i];
				//How often doas t appear in the document?
				int termfreq = termFreqs[i];

				//topic probabilities - q(z)
				double[] q = new double[K];


				for (int k2=0;k2<K;k2++) {
					//we subtract the old topic assignment(s) from the counts
					//and do sanity checks because of imprecisions (float/double)
					nmk2[g][d][ci-1][k2]-=termfreq*z2[g][d][ci-1][i][k2];
					if (nmk2[g][d][ci-1][k2]<0) nmk2[g][d][ci-1][k2] = 0;
					if (!test) {
						nkt[k2][t]-=termfreq*z2[g][d][ci-1][i][k2];
						if (nkt[k2][t]<0) nkt[k2][t]=0;
						nk[k2]-=termfreq*z2[g][d][ci-1][i][k2];
						if (nk[k2]<0) nk[k2]=0;
						//shouldn't happen... but in case:
						if (z2[g][d][ci-1][i][k2]!=1) {
							nkt0[k2][t]/=Math.pow((1.0-z2[g][d][ci-1][i][k2]),termfreq);

						}
						nktvar[k2][t]-=termfreq*z2[g][d][ci-1][i][k2]*(1.0-z2[g][d][ci-1][i][k2]);
						if (nktvar[k2][t]<=0) nktvar[k2][t]=0;
						nkvar[k2]-=termfreq*z2[g][d][ci-1][i][k2]*(1.0-z2[g][d][ci-1][i][k2]);
						if (nkvar[k2]<=0) nkvar[k2]=0;
					}


				}


				double[] topic_probability = new double[K];
				for (int k2=0;k2<K;k2++) {

					topic_probability[k2] = 
							(nkt[k2][t] + Gbeta) 
							/ (nk[k2] + c.V*Gbeta)
							*Math.exp(-nktvar[k2][t]/(2*Math.pow(nkt[k2][t] + Gbeta,2)) + nkvar[k2]/(2*Math.pow(nk[k2] + c.V*Gbeta,2)))
							;
					if (nkt[k2][t]<0 || nk[k2] <0) {
						System.out.println("nkt2 "+nkt[k2][t] + " "  + nk[k2]);
						for (int k=0;k<K;k++) {	
							System.out.println(z2[g][d][ci-1][i][k*K+k2]);
						}

						System.exit(0);
					}

				}


				//probabilities for topics drawn from the document-topic priors

				for (int k2=0;k2<K;k2++) {			
					//e^second part of the Taylor expansion of the log
					double second = Math.exp(-(nmk2var[g][d][ci-1][k2]/(2*Math.pow(nmk2[g][d][ci-1][k2] + prior[k2],2))));
					if (Double.isNaN(second)) {
						second = 1;
					}
					if (rhot_step==1) {
						q[k2]=Math.random();
					}
					else {
						q[k2]=
								(nmk2[g][d][ci-1][k2] + prior[k2]) 
								* second
								* topic_probability[k2];
					}
					if (q[k2]<0) {// || Double.isNaN(q[k*K+k2])) {
						q[k2]=0;
					}
					if (Double.isNaN(q[k2])) {
						System.out.println(nmk2[g][d][ci-1][k2] + " " + prior[k2]);
						System.out.println(second);
						System.out.println(topic_probability[k2]);
						System.exit(0);
					}
				}


				if (Double.isNaN(BasicMath.sum(q))) {
					BasicMath.print(topic_probability);
					BasicMath.print(q);
					//System.out.println(g + " " + d + " " + ci+ " " + k + " " + k2 +  " " +  alpha1[g] + " " +  theta[k] + " " + (mgkk[g][k][k2] +1) + " " +  denom + " " + nmk2[g][d][ci-1][k*K+k2]);
					//q[k*K+k2]=0;
				}


				BasicMath.normaliseDirect(q);	

				if (Double.isNaN(BasicMath.sum(q))) {
					System.out.println("NAN q");
					System.out.println("topic_probability");
					BasicMath.print(topic_probability);
					System.out.println("q");
					BasicMath.print(q);
					System.out.println("prior");
					BasicMath.print(prior);

					System.out.println(Galpha1[g]);
					//System.out.println(g + " " + d + " " + ci+ " " + k + " " + k2 +  " " +  alpha1[g] + " " +  theta[k] + " " + (mgkk[g][k][k2] +1) + " " +  denom + " " + nmk2[g][d][ci-1][k*K+k2]);
					//q[k*K+k2]=0;

					z2[g][d][ci-1][i]=new float[q.length];

					i=termIDs.length;					
					break;
				}

				for (int k2=0;k2<K;k2++) {
					z2[g][d][ci-1][i][k2] = (float) q[k2];
					mequal0[k2]*=Math.pow(1.0-q[k2],termfreq);

					//if (mgreater0[l]>1) mgreater0[l]=1;

					if (mequal0[k2]>1) {
						mequal0[k2] = 1;
						if (debug) {
							BasicMath.print(q);

							System.out.println("greater0: " + mequal0[k2] + " "  + q[k2]);

							for (int k3=0;k3<K;k3++) {				
								int offset = K*K;
								System.out.println( 
										nmk2[g][d][ci-1][offset+k3] + " " + topic_probability[k2]);
							}


							System.exit(0);
						}
					}

					Vndck[k2]+=termfreq*(q[k2] * (1-q[k2]));
				}


				for (int k2=0;k2<K;k2++) {		
					nmk2[g][d][ci-1][k2]+=termfreq*q[k2];
					nmk2var[g][d][ci-1][k2]+=termfreq*q[k2]*(1.0-q[k2]);

					if (!test) {
						nkt[k2][t]+=termfreq*q[k2];
						nk[k2]+=termfreq*q[k2];
						nkt0[k2][t]*=Math.pow((1.0-q[k2]),termfreq);
						nktvar[k2][t]+=termfreq*q[k2]*(1.0-q[k2]);
						nkvar[k2]+=termfreq*q[k2]*(1.0-q[k2]);
					}
				}


				for (int l=0;l<q.length;l++) {
					z2[g][d][ci-1][i][l]=(float) q[l];

				}					
			}

			//update auxiliary variables
			//m, eta, alpha', delta


			if (debug)System.out.println("+" + alpha1[g]);

			double[] mgreater0 = new double[K];

			for (int k2=0;k2<K;k2++) {
				mgreater0[k2]=1.0-mequal0[k2];
			}

			if (!test) {
				//update eta
				eta_sum[g]-=eta[g][d][ci-1];
				eta[g][d][ci-1] = Gamma.digamma(alpha1[g])-Gamma.digamma(c.getN(m)+alpha1[g]);
				eta_sum[g]+= eta[g][d][ci-1];


			}
			if (rhot_step==1) {
				for (int k=0;k<K;k++) {	
					for (int k2=0;k2<K;k2++) {	
						mdkequal0[g][d][k]*=1.0-(mgreater0[k2]*(prior_parts[k2][k]));
					}
				}
			}
			else {
				for (int k=0;k<K;k++) {	
					for (int k2=0;k2<K;k2++) {	
						double tmp = 1.0-(
								mgreater0[k2]
										*(1.0 - 
												Math.pow(1.0 - prior_parts[k2][k],mdc[g][d][ci-1][k*K+k2]) //p(parent was not chosen for any table)
												)
								);
						mdkequal0[g][d][k]*=tmp;
					}
				}
			}




			//update m
			if (rhot_step==1) {
				//practical approximation 
				for (int k=0;k<K;k++) {	
					for (int k2=0;k2<K;k2++) {
						mdc[g][d][ci-1][k*K+k2]=(float) (mgreater0[k2] * prior_parts[k2][k]);
						if (mdc[g][d][ci-1][k*K+k2]<0) {
							System.out.println("mdc < 0: " + mgreater0[k2]);
						}
					}
				}

				for (int k=0;k<K;k++) {
					for (int k2=0;k2<K;k2++) {
						double tmp = (prior_parts[k2][k]) * (1.0 - (prior_parts[k2][k]));
						varmdk[g][d][k]+=(mgreater0[k2] * (1.0 - mgreater0[k2])) * Math.pow(tmp,c.getN(m));
					}
				}

			}
			else {


				if (rhot_step > 1) {
					for (int k2=0;k2<K;k2++) {

						//remove tables
						for (int k=0;k<K;k++) {	
							if (!test) {
								mg[g] -=mdc[g][d][ci-1][k*K+k2];
								mgkk[g][k][k2] -= mdc[g][d][ci-1][k*K+k2];			
								mgkSum[g][k] -= mdc[g][d][ci-1][k*K+k2];	
							}
							mdk[g][d][k] -= mdc[g][d][ci-1][k*K+k2];
							if (mdk[g][d][k]<0) {
								mdk[g][d][k]=0;
							}
						}
					}
				}



				double[] varmdc = new double[K*K];
				double[] tables = new double[K];
				for (int k2=0;k2<K;k2++) {
					//calculate new expectation of tables as in Teh 06: CVB for DP							



					if (rhot_step>1){
						if (debug)System.out.println(g + " d " + d + " ci " + ci + " k2 "+ k2 + " " + mdc[g][d][ci-1][k2] + "\t" +prior[k2] 
								+ "\t" //+ Gtheta[k]
								+ "\t"+mgreater0[k2]+"\t"+nmk2[g][d][ci-1][k2]);
					}

					if (mgreater0[k2]>0.001) {
						tables[k2]=	(float) (prior[k2] * 	mgreater0[k2]
								*(Gamma.digamma(nmk2[g][d][ci-1][k2]/mgreater0[k2] + prior[k2]) - Gamma.digamma(prior[k2]) 
										+ 0.5 * ((Vndck[k2]/mgreater0[k2] - (1.0-mgreater0[k2]) * nmk2[g][d][ci-1][k2]/mgreater0[k2])
												* Gamma.tetragamma(nmk2[g][d][ci-1][k2]/mgreater0[k2] + prior[k2])
												)) 
								);	

						if (tables[k2] < 0) {
							//System.out.println(nmk2[g][d][ci-1][k2] + " " + tables[k2] + " " + prior[k2] + " " + mgreater0[k2] + " " +Vndck[k2]);

							tables[k2]=mgreater0[k2];
						}


						double varm = (float) (
								prior[k2] * 	mgreater0[k2]
										*(Gamma.digamma(nmk2[g][d][ci-1][k2]/mgreater0[k2] + prior[k2]) - Gamma.digamma(prior[k2]) 
												+ 0.5 * ((Vndck[k2]/mgreater0[k2] - (1.0-mgreater0[k2]) * nmk2[g][d][ci-1][k2]/mgreater0[k2])
														* Gamma.tetragamma(nmk2[g][d][ci-1][k2]/mgreater0[k2] + prior[k2])
														)
														+ prior[k2] * (Gamma.trigamma(nmk2[g][d][ci-1][k2]/mgreater0[k2] + prior[k2]) - Gamma.trigamma(prior[k2]) 
																+ 0.5 * ((Vndck[k2]/mgreater0[k2] - (1.0-mgreater0[k2]) * nmk2[g][d][ci-1][k2]/mgreater0[k2])
																		* Gamma.pentagamma(nmk2[g][d][ci-1][k2]/mgreater0[k2] + prior[k2])
																		)		
																)
												) 
								);	

						for (int k=0;k<K;k++) {
							m2var[g][d][ci-1][k2][k]=(float) (prior_parts[k2][k] * varm);
						}
						for (int k=0;k<K;k++) {
							mdc[g][d][ci-1][k*K+k2] = (float) (tables[k2] * prior_parts[k2][k]);
						}


					}
					else {
						for (int k=0;k<K;k++) {
							mdc[g][d][ci-1][k*K+k2] = (float) (mgreater0[k2]* prior_parts[k2][k]);
						}
					}



					if (mdc[g][d][ci-1][k2] < 0 || Double.isNaN(mdc[g][d][ci-1][k2])) {
						System.out.println( "table problem a " //+
								+mdc[g][d][ci-1][k2] + " " + prior[k2] +" "+nmk2[g][d][ci-1][k2] + " " + mgreater0[k2]
								);
						//							System.out.println(nmk2[g][d][ci-1][k*K+k2]/mgreater0[k*K+k2] + adtp + " " +  mgreater0[k*K+k2]);
						//							System.out.println(								
						//									(nmk2[g][d][ci-1][k*K+k2] + " " + mgreater0[k*K+k2] + " " + adtp) 
						//									);
						//							System.out.println(
						//									0.5 * ((Vndck[k*K+k2]/mgreater0[k*K+k2] - (1.0-mgreater0[k*K+k2]) * nmk2[g][d][ci-1][k*K+k2]/mgreater0[k*K+k2])
						//											* Gamma.trigamma(nmk2[g][d][ci-1][k*K+k2]/mgreater0[k*K+k2] + adtp))
						//									);
						System.exit(0);
					}


				}



				for (int k=0;k<K;k++) {	
					for (int k2=0;k2<K;k2++) {	
						double tmp = 1.0-(
								mgreater0[k2]
										*(1.0 - 
												Math.pow(1.0 - prior_parts[k2][k],mdc[g][d][ci-1][k*K+k2]) //p(parent was not chosen for any table)
												)
								);
						m20[g][d][ci-1][k2][k]=(float) tmp;
					}
				}


				for (int k=0;k<K;k++) {
					for (int k2=0;k2<K;k2++) {
						mgkk0[g][k2][k]*=1.0-m20[g][d][ci-1][k2][k];
						//						if (rhot_step>1 && Math.random()<0.0001) {
						//							System.out.println(mgreater0[k2] + " " + m20[g][d][ci-1][k2][k]);
						//						}
						mgkkvar[g][k2][k]+=m2var[g][d][ci-1][k2][k];
					}
				}


				for (int k=0;k<K;k++) {
					for (int k2=0;k2<K;k2++) {
						varmdk[g][d][k]+=varmdc[k*K+k2];
					}
				}
			}

			for (int k=0;k<K;k++) {
				for (int k2=0;k2<K;k2++) {
					if (!test) {
						mg[g] +=mdc[g][d][ci-1][k*K+k2];			
						mgkk[g][k][k2]+=mdc[g][d][ci-1][k*K+k2];
						mgkSum[g][k]+=mdc[g][d][ci-1][k*K+k2];
					}
					mdk[g][d][k]+=mdc[g][d][ci-1][k*K+k2];		
					//System.out.println(table[k2]);
				}
			}

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

				double eta = 0;

				//gamma priors over alpha
				double a=0;
				double b=1;

				for (int d=0;d<c.Gd[g];d++) {
					double mdkg0= 1.0 - BasicMath.prod(mdkequal0[g][d]);
					if (mdkg0>0) {					
					eta += Gamma.digamma(alpha0[g])
							-Gamma.digamma(alpha0[g]+BasicMath.sum(nmk1[g][d]) + BasicMath.sum(mdk[g][d]))
							- 0.5* BasicMath.sum(varmdk[g][d]) * Gamma.digamma(alpha0[g]+BasicMath.sum(mdk[g][d])/mdkg0);
							;
					}
					else {
						eta += Gamma.digamma(alpha0[g])
								-Gamma.digamma(alpha0[g]+BasicMath.sum(nmk1[g][d]));
					}
				}


				double summ = BasicMath.sum(mgk[g]);
				if (summ == 0) {
					for (int d = 0;d<c.Gd[g];d++) {
						for (int k=0;k<K;k++) {
							double g0 = 1.0-(Math.exp(nmk10[g][d][k]) * mdkequal0[g][d][k]);
							mgk[g][k]+=g0;
							summ+=g0;
						}
					}
					//System.out.println(summ + " / "+BasicMath.sum(nm));
				}
				if (summ == 0) continue;


				double galpha = (a+Math.exp(Gamma.digamma(summ))) / (b - eta);
				for (int k=0;k<K;k++) {
					gapk[g][k]=galpha;
					//pik ~ Dir(m1+1,...,mK)
					gapk[g][k]*=Math.exp(Gamma.digamma(1 + mgk[g][k]) - Gamma.digamma(K + summ));
					//System.out.println("gap: " +gapk[k]);

				}

				BasicMath.setTo(mgk[g],0);


				//Get tables like in Teh06 (second order Taylor for m>0)
				for (int d = 0;d<c.Gd[g];d++) {
					for (int k=0;k<K;k++) {
						double g0 = 1.0-(Math.exp(nmk10[g][d][k]) * mdkequal0[g][d][k]);

						double tables = 0;

						if (nmk1[g][d][k]<=1 || g0 < 0.01 || gapk[g][k] < 0.001) {
							tables = g0;
						}
						else {
							tables=(float) (
									g0 * gapk[g][k] * 
									(
											Gamma.digamma(gapk[g][k] + (nmk1[g][d][k] + mdk[g][d][k])/g0) - Gamma.digamma(gapk[g][k]) 
											+0.5*(((((nmk1var[g][d][k]+mdk[g][d][k])/g0 -Math.exp(nmk10[g][d][k])) * (nmk1[g][d][k]+mdk[g][d][k])/g0) 
													* Gamma.tetragamma(gapk[g][k] + (nmk1[g][d][k]+mdk[g][d][k])/g0))) 
											)
									);

							if (tables <=0 ) {
								tables = g0;
							}
						}


						mgk[g][k]+=tables;
					}
				}
				summ = BasicMath.sum(mgk[g]);

				//pik ~ Dir(m1+1,...,mK)
				for (int k=0;k<K;k++) {
					pi0gk[g][k]= (mgk[g][k]+1.0)/(summ+K);
				}

				alpha0[g] = (a + summ) / (b - eta);

				System.out.println(alpha0[g] + " " + summ + " " + eta + " " + " " + c.Gd[g] + " " + c.Gc[g]);

			}



			System.out.println("Estimating alpha1 + alpha2...");

			//gamma priors over alpha
			double a=0;
			double b=1;

			for (int g=0;g<c.G;g++) {


				//System.out.println("mgkk_sum " + mgkk_sum);
				//System.out.println("mgalpha2_sum " + mgalpha2_sum);


				double alpha = 0,galpha = 0;
				if (mg[g] > 0) {
					alpha = (a+mg[g])/(b-eta_sum[g]);
					System.out.println("alpha' " + g + ":\t" + alpha);
						galpha = Math.exp(Gamma.digamma(a+mg[g]))/(b-eta_sum[g]);
				}
				else {
					galpha = 0;
					System.out.println("alpha' (no observations)");
				}
				//System.out.println("galpha: " + galpha + " " + mg[g] + " " + -eta_sum[g]);

				if (galpha > 0) {
					//Galpha
					Galpha1[g]=galpha;

					alpha1[g] = alpha;
					//System.out.println(alpha1[g]);




				}

			}


			System.out.println("Estimating beta...");

			double sumeta=0;
			for (int k=0;k<K;k++) {
				sumeta+=Gamma.digamma(c.V * beta) - Gamma.digamma( c.V *beta + nk[k]);
			}



			if (mbeta==0) {
				//TODO practical approximation
				for (int k=0;k<K;k++) {

					for (int t=0;t<c.V;t++) {

						mbeta+=(1.0-nkt0[k][t]);
					}
				}

				Gbeta = Math.exp(Gamma.digamma(mbeta))/(-c.V * sumeta);
			}
			else {
				mbeta=0;
			}



			if(debug)System.out.println("b" + Gbeta + " " + mbeta + " " + sumeta);

			if (mbeta==0) {
				for (int k=0;k<K;k++) {

					for (int t=0;t<c.V;t++) {

						if (nkt0[k][t] < 1.0) {
							double tables=(1.0-nkt0[k][t]) * Gbeta * 
									(
											Gamma.digamma(Gbeta + (nkt[k][t]/(1.0-nkt0[k][t]))) - Gamma.digamma(Gbeta) 
											+0.5*((((nktvar[k][t])/(1.0-nkt0[k][t]) - nkt0[k][t] * (nkt[k][t])/(1.0-nkt0[k][t]))) 
													* Gamma.tetragamma(Gbeta + (nkt[k][t]/(1.0-nkt0[k][t]))) 

													));
							if (tables <= 0) {
								tables = (1.0-nkt0[k][t]);
							}
							mbeta+=tables;
						}

					}
				}
			}

			if(debug)System.out.println("c " + mbeta);

			Gbeta = Math.exp(Gamma.digamma(mbeta))/(-c.V * sumeta);
			beta = mbeta / (-c.V * sumeta);
			beta_V = beta * c.V;

			System.out.println("beta\t" + beta);



			System.out.println("Estimating beta'...");


			double sumeta2=0;
			for (int k2=0;k2<K;k2++) {
				sumeta2+=Gamma.digamma( c.V *beta) - Gamma.digamma( c.V *beta + nk[k2]);
			}











			if (c.G > 100) {
				System.out.println("Estimating gamma...");


				double sumetagamma=0;
				for (int g=0;g<c.G;g++) {
					for (int k=0;k<K;k++) {
						sumetagamma+=Gamma.digamma(K * gamma) - Gamma.digamma( K *gamma + BasicMath.sum(mgkk[g][k]));
					}
				}

				if (mgamma==0) {
					for (int g=0;g<c.G;g++) {
						for (int k2=0;k2<K;k2++) {
							for (int k=0;k<K;k++) {
								mgamma+=(1.0-mgkk0[g][k2][k]);
							}
						}
					}

					Ggamma = Math.exp(Gamma.digamma(mgamma))/(-K* sumetagamma);
				}	
				else {
					mgamma=0;
				}


				if(!debug)System.out.println("gamma" + Ggamma + " " + mgamma + " " + sumetagamma);

				for (int g=0;g<c.G;g++) {
					BasicMath.print(mgkk[g]);
				}

				if (mgamma==0) {
					for (int g=0;g<c.G;g++) {
						for (int k2=0;k2<K;k2++) {
							for (int k=0;k<K;k++) {

								if (mgkk0[g][k2][k] < 1.0) {
									if ((1.0-mgkk0[g][k2][k])>0.01 && Ggamma > 0.01 && mgkk[g][k2][k] > 0.01) {

										System.out.println(
												(((mgkkvar[g][k2][k])/(1.0-mgkk0[g][k2][k]) 
														- mgkk0[g][k2][k] * (mgkk[g][k2][k])/(1.0-mgkk0[g][k2][k]))) 
														+ "\t" +  Gamma.tetragamma(Ggamma + (mgkk[g][k2][k]/(1.0-mgkk0[g][k2][k]))) 
														+ "\t" +  Ggamma + "\t" + (mgkk[g][k2][k]/(1.0-mgkk0[g][k2][k])) + "\t" + mgkk[g][k2][k] + "\t" + (1.0-mgkk0[g][k2][k]) 				
												);

										double tables=(1.0-mgkk0[g][k2][k]) * Ggamma * 
												(
														Gamma.digamma(Ggamma + (mgkk[g][k2][k]/(1.0-mgkk0[g][k2][k]))) - Gamma.digamma(Ggamma) 
														+0.5*((((mgkkvar[g][k2][k])/(1.0-mgkk0[g][k2][k]) - mgkk0[g][k2][k] * (mgkk[g][k2][k])/(1.0-mgkk0[g][k2][k]))) 
																* Gamma.tetragamma(Ggamma + (mgkk[g][k2][k]/(1.0-mgkk0[g][k2][k]))) 
																));

										if (tables <= 0) {
											tables = (1.0-mgkk0[g][k2][k]);
										}

										mgamma+=tables;


									}
									else {
										mgamma+=(1.0-mgkk0[g][k2][k]);
									}

								}							

							}
						}
					}
				}

				if(debug)System.out.println("c gamma " + mgamma);

				Ggamma = Math.exp(Gamma.digamma(mgamma))/(-K* sumetagamma);
				gamma = mgamma / (-K * sumetagamma);


				System.out.println("gamma\t" + gamma);

			}
			if(gamma==0) {
				gamma = 1;
				Ggamma = 1;
			}

		}




	}


	public void save () {

		String output_base_folder = c.directory + "output_DCTM6/";

		File output_base_folder_file = new File(output_base_folder);
		if (!output_base_folder_file.exists()) output_base_folder_file.mkdir();

		String output_folder = output_base_folder + rhot_step + "/";

		File file = new File(output_folder);
		if (!file.exists()) file.mkdir();

		Save save = new Save();
		save.saveVar(nkt, output_folder+save_prefix+"nkt");
		save.close();
		for (int g=0;g<c.G;g++) {
			save.saveVar(nmk1[g], output_folder+save_prefix+"nmk_"+g);
			save.close();
		}

		save.close();
		save.saveVar(alpha0, output_folder+save_prefix+"alpha0");
		save.close();
		save.saveVar(pi0gk, output_folder+save_prefix+"pi0gk");
		save.close();
		save.saveVar(alpha1, output_folder+save_prefix+"alpha1");
		save.close();
		save.saveVar(gamma, output_folder+save_prefix+"gamma");
		save.close();
		save.saveVar(beta, output_folder+save_prefix+"beta");
		save.close();


		//save.saveVar(eta, output_folder+save_prefix+"eta");
		//save.close();

		for (int g=0;g<c.G;g++) {

			double[][] pi = new double[K][K];

			for (int k=0;k<K;k++) {
				for (int k2=0;k2<K;k2++) {
					pi[k][k2] = (mgkk[g][k][k2] +gamma)/(mgkSum[g][k] + K*gamma);
				}
			}

			save.saveVar(pi, output_folder+save_prefix+"pikk2_"+g);
			save.saveVar(mgkk[g], output_folder+save_prefix+"mgkk_"+g);

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



		String[][] topktopicsc = new String[K*2][topk];

		for (int k2=0;k2<K;k2++) {

			List<Pair> wordprob = new ArrayList<Pair>(); 
			for (int v = 0; v < c.V; v++){
				wordprob.add(new Pair(c.dict.getWord(v), (nkt[k2][v]+beta)/(nk[k2]+beta_V), false));
			}
			Collections.sort(wordprob);

			for (int i=0;i<topk;i++) {
				topktopicsc[k2*2][i] = (String) wordprob.get(i).first;
				topktopicsc[k2*2+1][i] = String.valueOf(wordprob.get(i).second);
			}

		}
		save.saveVar(topktopicsc, output_folder+save_prefix+"topktopicsc");

	}



	public double perplexity(int m) {

		double ppx = 0;

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


				//topic probabilities - q(z)
				double[] z_theta = new double[K];
				double[] w_phi = new double[K];

				for (int k=0;k<K;k++) {


					z_theta[k] = 	//probability of topic given feature & group
							(nmk1[g][d][k] + mdk[g][d][k] + gapk[g][k])
							* Math.exp(-(nmk1var[g][d][k]/(2*Math.pow(nmk1[g][d][k] + mdk[g][d][k] + gapk[g][k],2))))
							;
					w_phi[k]=
							//probability of topic given word w
							(nkt[k][t] + beta) 
							/ (nk[k] + beta_V)
							//*Math.exp(-nktvar[k][t]/(2*Math.pow(nkt[k][t] + Gbeta,2)) + nkvar[k]/(2*Math.pow(nk[k] + c.V*Gbeta,2)))
							;

				}
				BasicMath.normaliseDirect(z_theta);

				double prob = 0;
				for (int k=0;k<K;k++) {
					prob += z_theta[k]*w_phi[k];
				}

				ppx+=termfreq*Math.log(prob);


			}
			return ppx;

		}
		//Comment inference
		else {

			double[] Vndck = new double[K*(K)];

			double[] theta = new double[K];
			double theta_sum = 0;

			double[] Gtheta = new double[K];
			double[][] Gpi = new double[K][K];

			for (int k=0;k<K;k++) {
				theta[k] = (nmk1[g][d][k] + mdk[g][d][k] + alpha0[g] * pi0gk[g][k]);
				//if (rhot_step > 5)	
				if (debug)System.out.println(k + " " + nmk1[g][d][k] + " " + mdk[g][d][k] + " " + alpha0[g] * pi0gk[g][k]);

				if (theta[k] < 0) {
					if (debug)System.out.println("theta < 0: " + nmk1[g][d][k] + " " + mdk[g][d][k] + " " + alpha0[g]);
				}

				theta_sum+=theta[k];
				if (debug)System.out.println(nmk1[g][d][k] + " " + mdk[g][d][k] + " " + alpha0[g]);
				Gtheta[k] = Gamma.digamma(theta[k]);

				double sum = 0;
				for (int k2=0;k2<K;k2++) {
					double tmp = mgkk[g][k][k2] + gamma;
					Gpi[k][k2] = Gamma.digamma(tmp);
					sum += tmp;
				}
				double Gsum = Gamma.digamma(sum);
				for (int k2=0;k2<K;k2++) {
					Gpi[k][k2]=Math.exp(Gpi[k][k2]-Gsum);
				}
			}

			for (int k=0;k<K;k++) {
				theta[k]/=theta_sum;
				if (debug)System.out.println("#");

				Gtheta[k]=Math.exp(Gtheta[k]-Gamma.digamma(theta_sum));
			}

			//for every table: p>0
			double[] mgreater0 = new double[K*(K)];
			for (int l=0;l<mgreater0.length;l++) {
				mgreater0[l]=1.0;
			}


			//System.out.println("g " + g + " d " + d + " ci " +ci);

			//Process words of the document
			for (int i=0;i<termIDs.length;i++) {

				//term index
				int t = termIDs[i];
				//How often doas t appear in the document?
				int termfreq = termFreqs[i];

				double[] z_theta = new double[K];

				double[] topic_probability = new double[K];
				for (int k2=0;k2<K;k2++) {

					topic_probability[k2] = 
							(nkt[k2][t] + beta) 
							/ (nk[k2] + beta_V)
							//*Math.exp(-nkt2var[k2][t]/(2*Math.pow(nkt2[k2][t] + Gbeta2,2)) + nk2var[k2]/(2*Math.pow(nk2[k2] + c.V*Gbeta2,2)))
							;
				}

				//probabilities for topics drawn from the document-topic priors
				for (int k=0;k<K;k++) {	
					double denom = Gamma.digamma(mgkSum[g][k] +K);

					double prior = Galpha1[g] * Gtheta[k] * Math.exp(Gamma.digamma(mgkk[g][k][k] + gamma) - denom);
					double second = Math.exp(-(nmk2var[g][d][ci-1][k]/(2*Math.pow(nmk2[g][d][ci-1][k] + prior,2))));

					if (Double.isNaN(second) || second == 0) {
						second = 1;
					}
					z_theta[k]=
							(nmk2[g][d][ci-1][k] + prior) 
							* second;

				}


				BasicMath.normaliseDirect(z_theta);	

				double prob = 0;
				for (int k2=0;k2<K;k2++) {			
					prob += z_theta[k2] * topic_probability[k2];			
				}
				
				if (prob <= 0) {

					for (int k=0;k<K;k++) {	

						double denom = Gamma.digamma(mgkSum[g][k] +K);
						double prior = Galpha1[g] * Gtheta[k] * Math.exp(Gamma.digamma(mgkk[g][k][k] + gamma) - denom);
						double second = Math.exp(-(nmk2var[g][d][ci-1][k]/(2*Math.pow(nmk2[g][d][ci-1][k] + prior,2))));

						System.out.println("error ppx:" + prior + " " + second + " " + denom);

					}
					BasicMath.print(z_theta);
					BasicMath.print(nmk2[g][d][ci-1]);
					
				}
				

				ppx+=termfreq*Math.log(prob);
			}


			comment_counter[g]++;

			return(ppx);
		}


	}



}
