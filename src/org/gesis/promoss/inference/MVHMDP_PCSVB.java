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
import org.gesis.promoss.tools.probabilistic.ArrayUtils;
import org.gesis.promoss.tools.probabilistic.DirichletEstimation;
import org.gesis.promoss.tools.probabilistic.Gamma;
import org.gesis.promoss.tools.probabilistic.Pair;
import org.gesis.promoss.tools.text.HMDP_Corpus;
import org.gesis.promoss.tools.text.Save;

/**
 * This is the practical collapsed stochastic variational inference
 * for the Hierarchical Multi-Dirichlet Process Topic Model (HMDP)
 */
public class MVHMDP_PCSVB {

	//This class holds the corpus and its properties
	//including metadata
	public HMDP_Corpus c;

	//We have a debugging mode for checking the parameters
	public boolean debug = true;
	//Number of top words returned for the topic file
	public int topk = 100;
	//Number of iterations over the dataset for topic inference
	public int RUNS = 100;
	//Save variables after step SAVE_STEP
	public int SAVE_STEP = 10;
	public int BATCHSIZE = 128;
	public int BATCHSIZE_GROUPS = -1;
	public int BATCHSIZE_CLUSTERS = -1;

	//How many observations do we take before updating alpha_1
	public int BATCHSIZE_ALPHA = 1000;
	//After how many steps a sample is taken to estimate alpha
	public int SAMPLE_ALPHA = 1;
	//Burn in phase: how long to wait till updating nkt?
	public int BURNIN = 0;
	//Burn in phase for documents: How long till we update the
	//topic distributions of context-clusters?
	public int BURNIN_DOCUMENTS = 0;
	//Should the topics be randomly initialised?
	public double INIT_RAND = 1;

	//Relative size of the training set
	public double TRAINING_SHARE = 1.0;

	public String save_prefix = "";

	public int T = 8; //Number of truncated topics

	public double alpha_0 = 1;

	public double alpha_1 = 1;

	//Dirichlet parameter for multinomials over cluster-topic distributions 
	//(indicating similarity of adjacent clusters) per feature
	public double[] delta;

	//Dirichlet parameter for multinomials over topic-word multinomials
	//(indicating of similarity of adjacent cluster-topic-word distributions)
	public double[] delta2;

	//decides if delta is fixed or not.
	public double delta_fix = 0;

	//Dirichlet parameter for multinomial over features for cluster-topic priors
	public double[] epsilon;

	//Dirichlet parameter for multinomial over features for topic-word multinomials
	public double[] epsilon2;

	public double gamma = 1;

	//Dirichlet concentration parameter for topic-word distributions
	public double beta_0 = 0.01;

	//helping variable beta_0*V
	private double beta_0V;

	//Global topic weight (estimated)
	public double[] pi_0;

	//Store some zeros for empty documents in the doc_topic matrix?
	public Boolean store_empty = true;

	//sum of counts over all documents for cluster c in feature f for topic k
	public float[][][] nkfc;
	//Estimated number of times term t appeared in topic k
	public float[][] nkt;
	//Estimated number of times term t appeared in topic k in the batch
	private float[][] batch_nkt;
	//Estimated number of words in topic k
	public float[] nk;

	//Estimated number of tables for word v in topic k, used to update the topic-prior beta
	public float[][] mkt;
	//Estimated number of tables for word v in topic k, used to update the topic prior beta
	//temporal variable for estimation
	private float[][] batch_mkt;
	//Context-cluster-topic "counts" per document M x T
	public float[][] nmk;



	//rho: Learning rate; rho = s / ((tau + t)^kappa);
	//recommended values: See "Online learning for latent dirichlet allocation" paper by Hoffman
	//tau = 64, K = 0.5; S = 1; Batchsize = 4096

	public int rhos = 1;
	public double rhokappa = 0.5;
	public int rhotau = 64;

	//public int rhos = 1;
	//public double rhokappa = 0.5;
	//public int rhotau = 2000;

	public int rhos_document = 1;
	public double rhokappa_document = 0.5;
	public int rhotau_document = 64;

	public int rhos_group = 1;
	public double rhokappa_group = 0.5;
	public int rhotau_group = 64;

	//tells the number of processed words
	public int rhot = 0;
	//tells the number of the current run)
	public int rhot_step = 0;


	//count number of words seen in the batch
	//remember that rhot counts the number of documents, not words
	private int[] batch_words;
	//count number of times the group was seen in the batch - FxAf
	public int[][] rhot_group;
	//count number of documents seen in a cluster - FxAf
	public int[][] rhot_cluster;
	//count number of words seen in a cluster - FxAf
	public int[][] batch_cluster_words;

	/*
	 * Every context space has clusters
	 * Clusters belong to groups of connected clusters (e.g. adjacent clusters).
	 */

	//Convention: 
	//Normal counts on the lowest level start with n (e.g. nkt)
	//Tables counts on the cluster level start with m
	//Table counts on the global (first) level of the 3-level HMDP start with l

	//Sum over log(1-q(k,f,c)). We need this sum for calculating E[n>0] and E[n=0] 
	public double[][][] lfck;
	//Sum table counts for a given cluster; F x Cf x T
	public double[][][] mfck;
	//Sum over customers for a given cluster; F x Cf
	public double[][] mfc;
	//Sum over 1-q(_,f,_) for document M and feature f (approximated seat counts)
	public double[] sumqf;
	//Batch estimate of sumqf2 for stochastic updates
	//private static double[] sumqf2temp;

	private double[] zeta;
	private double[] zeta2;



	//Counter: how many observations do we have per cluster? Dimension: F x |C[f]|
	//We use this for doing batch updates of the cluster parameters and to calculate
	//the update rate \rho
	//private static int[][] rhot_cluster;

	//statistic over gamma, used to do batch-updates of clusters: sum of gamma
	private double[][][] mfgi;
	private double[][][] batch_mfgi;
	//statistic over gamma, used to do batch-updates of features: sum of gamma
	//private static double[] sumqtemp2_features;


	private double[][][] batch_logmfck;

	//counts over the feature use
	private double[] batchmf;
	//counts over the group use
	double[][] batch_mfg;

	//group-cluster-topic distributions F x G x C x T excluding feature distribution
	public double[][][][] eta_pic_kfc;

	public double rhostkt_document;
	public double oneminusrhostkt_document;

	//counts, how many documents we observed in the batch to estimate alpha_1
	public int alpha_batch_counter = 0;
	//document lengths in sample
	private double[][] alpha_1_pimk;
	private int[] alpha_1_nm;
	private double[][] alpha_1_nmk;

	//private RandomSamplers rs = new RandomSamplers();

	private double kappa,mu = 1;
	//number of words for cluster-specific topics
	private float[][][][] nfckt;
	//number of words for cluster-specific topics - batch data
	private float[][][][] batch_nfckt;
	//sum of words for cluster-specific topics
	private float[][][] nfck;
	//counts for parent cluster-specific topics 
	private float[][][] nxfc;
	//counts for parent cluster-specific topics 
	private float[][][] batch_nxfc;
	//counts for cluster-specific background-models 
	private float[][][] nfct;
	//counts for cluster-specific background-models 
	private float[][][] batch_nfct;
	//sum of counts for cluster-specific background-models 
	private float[][] nfc;
	//counts for non-background topics per topic
	private float[][] nky;
	//counts for non-background topics per topic
	private float[][] batch_nky;
	//sum of counts for non-background topics
	private float ng;
	//counts for parent cluster-specific topics per group FxA_fxP_fj)
	private float[][][] nfji;
	//counts for parent cluster-specific topics per group FxA_fxP_fj)
	private float[][][] batch_nfji;
	//sum of counts for parent cluster-specific topics per group FxA_f)
	private float[][] nfj;
	//Mixing proportions of regions for the topic prior (FxA_fxP_fj)
	private float[][][] eta;
	//Mixing proportions of regions for the topic-word distributions
	//(calculated from nfik)
	private float[][][] eta2;	
	//Probability for global topics (calculated from ngk and ng)
	private float[] lambda;
	//Probability for background-topics (FxC_f)
	private float[][] nu;

	//private double sum_cluster_tables = 0;

	MVHMDP_PCSVB() {
		c = new HMDP_Corpus();
	}

	public void initialise () {

		// Folder names, files etc. 
		c.dictfile = c.directory+"words.txt";
		//textfile contains the group-IDs for each feature dimension of the document
		//and the words of the document, all seperated by space (example line: a1,a2,a3,...,aF word1 word2 word3 ... wordNm)
		c.documentfile = c.directory+"texts.txt";
		//groupfile contains clus
		//cluster-IDs for each group.separated by space
		c.groupfile = c.directory+"groups.txt";

		System.out.println("Creating dictionary...");
		c.readDict();	
		System.out.println("Initialising parameters...");
		initParameters();
		System.out.println("Processing documents...");
		c.readDocs();
		c.readClusterSizeWords();
		System.out.println("Estimating topics...");



	}

	public void run () {
		for (int i=0;i<RUNS;i++) {

			int topicsUsed = 0;
			double[] topic_ge0 = new double[T];
			for (int k=0;k<T;k++) {
				topic_ge0[k] = 1.0;
			}
			for (int f=0;f<c.F;f++) {
				for (int d=0;d< c.Cf[f];d++) {
					for (int k=0;k<T;k++) {
						topic_ge0[k] *= 1.0-lfck[f][d][k];
					}
				}
			}
			for (int k=0;k<T;k++) {
				topicsUsed += (1.0-topic_ge0[k]) > 0.5? 1 : 0;
			}

			System.out.println(c.directory + " run " + i + " (Topics "+ topicsUsed + " alpha_0 "+alpha_0+" alpha_1 "+ alpha_1+ " beta_0 " + beta_0 + " gamma "+gamma + " delta " + delta[0]+ " epsilon " + epsilon[0]);

			onePass();

			if (rhot_step%SAVE_STEP==0 || rhot_step == RUNS) {
				//store inferred variables
				System.out.println("Storing variables...");
				save();
			}

		}
	}

	public void onePass() {
		rhot_step++;
		//get step size
		rhostkt_document = rho(rhos_document,rhotau_document,rhokappa_document,rhot_step);
		oneminusrhostkt_document = (1.0 - rhostkt_document);

		int progress = c.M / 50;
		if (progress==0) progress = 1;
		for (int m=0;m<Double.valueOf(c.M)*TRAINING_SHARE;m++) {
			if(m%progress == 0) {
				System.out.print(".");
			}

			inferenceDoc(m);
		}
		System.out.println();

		updateHyperParameters();
	}


	//set Parameters
	public void initParameters() {

		beta_0V = beta_0 * c.V;


		if (rhos_document < 0) 
			rhos_document = rhos;
		if (rhos_group < 0) 
			rhos_group = rhos;

		if (rhotau_document < 0) 
			rhotau_document = rhotau;
		if (rhotau_group < 0) 
			rhotau_group = rhotau;

		if (rhokappa_document < 0) 
			rhokappa_document = rhokappa;
		if (rhokappa_group < 0) 
			rhokappa_group = rhokappa;

		if (BATCHSIZE_GROUPS < 0)
			BATCHSIZE_GROUPS = BATCHSIZE;

		if (BATCHSIZE_CLUSTERS < 0)
			BATCHSIZE_CLUSTERS = BATCHSIZE;



		c.readFfromTextfile();

		System.out.println("Reading groups...");
		c.readGroups(); //if there is an unseen Group mentioned

		c.V = c.dict.length();

		delta = new double[c.F];
		for (int f=0;f<c.F;f++) {
			delta[f]=1.0;
			if (delta_fix != 0) {
				delta[f]=delta_fix;
			}
		}

		delta2 = new double[c.F];
		for (int f=0;f<c.F;f++) {
			delta2[f]=1.0;
			if (delta_fix != 0) {
				delta2[f]=delta_fix;
			}
		}


		epsilon = new double[c.F];
		for (int f=0;f<c.F;f++) {
			epsilon[f] = 1.0;
		}

		epsilon2 = new double[c.F];
		for (int f=0;f<c.F;f++) {
			epsilon2[f] = 1.0;
		}

		beta_0V = beta_0 * c.V;

		batch_words = new int[c.V];

		mkt = new float[T][c.V];	
		batch_mkt = new float[T][c.V];

		nk = new float[T];
		nkt = new float[T][c.V];	
		batch_nkt = new float[T][c.V];	

		//count the number of documents in each group
		c.Cfg = new int[c.F][];
		for (int f=0;f<c.F;f++) {
			c.Cfg[f]=new int[c.A[f].length];
		}

		//count the number of documents in each feature
		c.Cfd=new int[c.F];
		//count the number of documents in each cluster
		c.Cfc=new int[c.F][];
		for (int f=0;f<c.F;f++) {
			c.Cfc[f]=new int[c.Cf[f]];
		}


		//read corpus size and initialise nkt / nk
		c.readCorpusSize();

		rhot_group = new int[c.F][];
		for (int f=0;f<c.F;f++) {
			rhot_group[f]=new int[c.A[f].length];
		}
		rhot_cluster = new int[c.F][];
		batch_cluster_words = new int[c.F][];
		for (int f=0;f<c.F;f++) {
			batch_cluster_words[f]=new int[c.Cf[f]];
			rhot_cluster[f]=new int[c.Cf[f]];
		}

		nmk = new float[c.M][T];



		for (int t=0; t < c.V; t++) {
			for (int k=0;k<T;k++) {

				nkt[k][t]= (float) (Math.random()*INIT_RAND);
				nk[k]+=nkt[k][t];

				batch_mkt[k][t] = (float) 0.0;

			}
		}
		pi_0 = new double[T];


		pi_0[0] = 1.0 / (1.0+gamma);
		for (int i=1;i<T;i++) {
			pi_0[i]=(1.0 / (1.0+gamma)) * (1.0-pi_0[i-1]);
		}
		pi_0 = BasicMath.normalise(pi_0);

		lfck = new double[c.F][][];
		//rhot_cluster = new int[c.F][];
		//for (int f=0;f<c.F;f++) {
		//	rhot_cluster[f] = new int[c.Cf[f]];
		//}
		mfck = new double[c.F][][];
		batch_logmfck = new double[c.F][][];
		mfc = new double[c.F][];
		mfgi = new double[c.F][][];
		batch_mfgi = new double[c.F][][];
		//sumqtemp2_features = new double[c.F];
		for (int f=0;f<c.F;f++) {
			lfck[f] = new double[c.Cf[f]][T];
			mfck[f] = new double[c.Cf[f]][T];
			batch_logmfck[f] = new double[c.Cf[f]][T];
			mfc[f] = new double[c.Cf[f]];
			mfgi[f] = new double[c.A[f].length][];
			batch_mfgi[f] = new double[c.A[f].length][];
			for (int g=0;g<c.A[f].length;g++) {
				mfgi[f][g]=new double[c.A[f][g].length];
				batch_mfgi[f][g]=new double[c.A[f][g].length];
			}
		}


		sumqf = new double[c.F];

		zeta = new double[c.F];
		for (int f=0;f<c.F;f++) {
			zeta[f] = epsilon[f];
		}
		zeta = BasicMath.normalise(zeta);

		zeta2 = new double[c.F];
		for (int f=0;f<c.F;f++) {
			zeta2[f] = epsilon2[f];
		}
		zeta2 = BasicMath.normalise(zeta);


		eta = new float[c.F][][];
		for (int f=0;f<c.F;f++) {
			eta[f] = new float[c.A[f].length][];
			for (int g=0;g<c.A[f].length;g++) {
				eta[f][g] = new float[c.A[f][g].length];
				for (int i=0;i<c.A[f][g].length;i++) {
					eta[f][g][i]=(float) (1.0 / c.A[f][g].length);
				}
			}
		}

		//sumqf2temp = new double[c.F];
		eta_pic_kfc = new double[c.F][][][];
		for (int f=0;f<c.F;f++) {
			eta_pic_kfc[f] = new double[c.A[f].length][][]; 
			for (int g=0;g<c.A[f].length;g++) {
				eta_pic_kfc[f][g] = new double[c.A[f][g].length][T];
				for (int i=0;i<c.A[f][g].length;i++) {
					for (int k = 0; k < T; k++) {
						//for every group: get topic distribution of clusters and their weight 
						//(the weight of the clusters) for the group
						eta_pic_kfc[f][g][i][k] = pi_0[k]/((double)c.A[f][g].length);
					}
				}
			}
		}

		batchmf = new double[c.F];
		batch_mfg = new double[c.F][];
		for (int f=0;f<c.F;f++) {
			batch_mfg[f] = new double[c.A[f].length];
		}

		if (SAMPLE_ALPHA <= 0) {
			SAMPLE_ALPHA = (int) (c.M*TRAINING_SHARE);
		}
		
		alpha_1_nm = new int[BATCHSIZE_ALPHA];
		alpha_1_nmk = new double[BATCHSIZE_ALPHA][T];
		alpha_1_pimk = new double[BATCHSIZE_ALPHA][T];

		SAMPLE_ALPHA = Math.max(1, (int) Math.ceil(c.M*TRAINING_SHARE / Double.valueOf(BATCHSIZE_ALPHA)));

		//number of words for cluster-specific topics
		nfckt = new float[c.F][][][];
		for (int f=0;f<c.F;f++) {
			nfckt[f] = new float[c.Cf[f]][T][c.V];
		}
		//number of words for cluster-specific topics - batch data
		batch_nfckt = new float[c.F][][][];
		for (int f=0;f<c.F;f++) {
			batch_nfckt[f] = new float[c.Cf[f]][T][c.V];
		}
		//sum of words for cluster-specific topics
		nfck = new float[c.F][][];
		for (int f=0;f<c.F;f++) {
			nfck[f] = new float[c.Cf[f]][T];
		}
		//counts for parent cluster-specific topics 
		nxfc = new float[2][c.F][];
		for (int x=0;x<2;x++) {
			for (int f=0;f<c.F;f++) {
				nxfc[x][f] =  new float[c.Cf[f]];
			}
		}
		//counts for parent cluster-specific topics 
		batch_nxfc = new float[2][c.F][];
		for (int x=0;x<2;x++) {
			for (int f=0;f<c.F;f++) {
				batch_nxfc[x][f] = new float[c.Cf[f]];
			}
		}
		//counts for cluster-specific background-models 
		nfct = new float[c.F][][];
		for (int f=0;f<c.F;f++) {
			nfct[f] = new float[c.Cf[f]][c.V];
		}
		//counts for cluster-specific background-models 
		batch_nfct = new float[c.F][][];
		for (int f=0;f<c.F;f++) {
			batch_nfct[f] = new float[c.Cf[f]][c.V];
		}
		//sum of counts for cluster-specific background-models 
		nfc = new float[c.F][];
		for (int f=0;f<c.F;f++) {
			nfc[f] = new float[c.Cf[f]];
		}
		//counts for non-background topics per topic
		nky = new float[T][2];
		batch_nky = new float[T][2];
		//sum of counts for non-background topics
		ng = 0;
		//counts for parent cluster-specific topics per group FxA_fxP_fj)
		nfji = new float[c.F][][];
		for (int f=0;f<c.F;f++) {
			nfji[f] = new float[c.A[f].length][];
			for (int g=0;g<c.A[f].length;g++) {
				nfji[f][g] = new float[c.A[f][g].length];
			}
		}
		batch_nfji = new float[c.F][][];
		for (int f=0;f<c.F;f++) {
			batch_nfji[f] = new float[c.A[f].length][];
			for (int g=0;g<c.A[f].length;g++) {
				batch_nfji[f][g] = new float[c.A[f][g].length];
			}
		}
		//sum of counts for parent cluster-specific topics per group FxA_f)
		nfj = new float[c.F][];
		for (int f=0;f<c.F;f++) {
			nfj[f] = new float[c.A[f].length];
		}
		//Mixing proportions of regions for the topic prior (FxA_fxP_fj)
		float[][][] eta = new float[c.F][][];
		for (int f=0;f<c.F;f++) {
			eta[f] = new float[c.A[f].length][];
			for (int g=0;g<c.A[f].length;g++) {
				eta[f][g] = new float[c.A[f][g].length];
			}
		}
		//Mixing proportions of regions for the topic-word distributions
		//(calculated from nfik)
		eta2 = new float[c.F][][];
		for (int f=0;f<c.F;f++) {
			eta2[f] = new float[c.A[f].length][];
			for (int g=0;g<c.A[f].length;g++) {
				eta2[f][g] = new float[c.A[f][g].length];
				for (int i=0;i<c.A[f][g].length;i++) {
					eta2[f][g][i]=(float) (1.0 / c.A[f][g].length);
				}
			}
		}
		//Probability for global topics (calculated from ngk and ng)
		lambda = new float[T];
		for (int k=0;k<T;k++) {
			lambda[k]= 0.5f;
		}

		//Probability for background-topics (FxC_f)
		nu = new float[c.F][];
		for (int f=0;f<c.F;f++) {
			nu[f] = new float[c.Cf[f]];
			for (int i=0;i<c.Cf[f];i++) {
				nu[f][i]=0.5f;
			}
		}
	}




	public void inferenceDoc(int m) {

		//increase counter of documents seen
		rhot++;

		int[] grouplength = new int[c.F];
		int[] group = c.groups[m];

		//Expectation(number of tables)
		double[] sumqmk = new double[T];

		//Stochastic cluster updates: tmkfc unkown (tables!)
		//-> get table counts per cluster (or estimate it)
		//Stochastic group updates: tmkfg unknown (tables in group, tells how often cluster X was chosen in group g)
		for (int f=0;f<c.F;f++) {

			//Number of clusters of the group
			grouplength[f] = c.A[f][group[f]].length;

			//Helping variable: sum log(1-qkfc) for this document, (don't mix with sumqkfc, which is the global count variable!)
			//Tells the expected total number of times topic k was _not_ seen for feature f in cluster c in the currect document

		}

		//probability of feature f given k
		double[][] pk_f = new double[T][c.F];
		//probability of feature x cluster x topic
		double[][][] pifik = new double[c.F][][];
		for (int f=0;f<c.F;f++) {
			pifik[f] = new double[grouplength[f]][];
			for (int i=0;i<grouplength[f];i++) {
				pifik[f][i] = new double[T];
			}
		}

		double[] topic_prior = new double[T];
		double nm_alpha = c.getN(m) + alpha_1;
		double[][][] n_alpha_fi = new double[c.F][][];
		for (int f=0;f<c.F;f++) {
			n_alpha_fi[f]=new double[grouplength[f]][T];
			int g = group[f];
			for (int i=0;i<grouplength[f];i++) {
				int a= c.A[f][g][i];
				double alpha_fi = alpha_1* zeta[f] * eta[f][g][i];
				for (int k=0;k<T;k++) {
					n_alpha_fi[f][i][k] = (nmk[m][k] + (alpha_fi * 
							(mfck[f][a][k] + 
									alpha_0 * pi_0[k]) /
									(mfc[f][a] + alpha_0))) / nm_alpha;

					if (debug && Double.isNaN(n_alpha_fi[f][i][k])) {
						System.out.println(
								"nmk " + nmk[m][k] 
										+"\n mfck " + mfck[f][a][k] 
												+"\n pi0k " + pi_0[k] 
														+"\n mfc" + mfc[f][a]
																+"\n nm_alpha "+nm_alpha);
						System.exit(0);

					}


					topic_prior[k]+=n_alpha_fi[f][i][k];
				}
			}
		}



		double[][] p_fi2 = new double[c.F][];
		for (int f=0;f<c.F;f++) {
			p_fi2[f]=new double[grouplength[f]];
			int g = group[f];
			for (int i=0;i<grouplength[f];i++) {
				int a= c.A[f][g][i];
				p_fi2[f][i] = zeta2[f] * eta2[f][g][i];
			}
		}


		double rhostkt_documentNm = rhostkt_document * c.getN(m);

		int[] termIDs = c.getTermIDs(m);
		short[] termFreqs = c.getTermFreqs(m);

		//Process words of the document
		for (int wordIndex=0;wordIndex<termIDs.length;wordIndex++) {



			//term index
			int t = termIDs[wordIndex];
			//How often doas t appear in the document?
			int termfreq = termFreqs[wordIndex];


			if (rhot_step>BURNIN) {
				//increase number of words seen in that batch
				batch_words[t]+=termfreq;
			}


			//probability of word under topic
			double[] topic_word = new double[T];

			for (int k=0;k<T;k++) {
				topic_word[k] = lambda[k] * (nkt[k][t] + beta_0) / (nk[k] +beta_0V);


			}

			//probability of word under topic
			double[][] background_word = new double[c.F][];

			//topic probabilities - q(z)
			double[][][][] q = new double[2][c.F][][];


			for (int f=0;f<c.F;f++) {
				int g = group[f];
				background_word[f] = new double[grouplength[f]];
				q[0][f]=new double[grouplength[f]][T];
				q[1][f]=new double[grouplength[f]][T];
				for (int i=0;i<grouplength[f];i++) {
					int a= c.A[f][g][i];
					background_word[f][i] = nu[f][a] * (nfct[f][a][t] + beta_0)/(nfc[f][a] + c.V*beta_0);
				}
			}

			//sum 
			double qsum = 0.0;
			//we need a statistics about how often a topic was assigned
			//because under the practical approximation a topic is only
			//assigned ONCE per document
			double[] qk = new double[T];

			for (int y=0;y<2;y++) {

				for (int f=0;f<c.F;f++) {
					int g = group[f];
					for (int i=0;i<grouplength[f];i++) {
						int a= c.A[f][g][i];
						for (int k=0;k<T;k++) {
							q[y][f][i][k]=n_alpha_fi[f][i][k] * nu[f][a];

							if (y==0) {
								q[y][f][i][k]*=(1.0-lambda[k]) * p_fi2[f][i] * (nfckt[f][i][k][t] + beta_0) / (nfck[f][i][k] + c.V * beta_0);
								if (debug && (q[y][f][i][k]==0 || Double.isNaN(q[y][f][i][k])) ) {
									System.out.println(
											"nu "+ nu[f][a]
													+"\n n_alpha_fi "+n_alpha_fi[f][i][k]
															+"\n lambda "+lambda[k]
																	+"\n p_fi2 "+ p_fi2[f][i]
																			+"\n nfckt "+nfckt[f][i][k][t]
																					+"\n beta_0 " +beta_0
																					+"\n nfck "+nfck[f][i][k]
											);		
									System.exit(0);
								}
							}
							else {
								q[y][f][i][k]*=topic_word[k];

							}
							qk[k]+=q[y][f][i][k];

							qsum+=q[y][f][i][k];
						}
					}
				}
			}

			double[][] qx = new double[c.F][];
			for (int f=0;f<c.F;f++) {
				qx[f] = new double[grouplength[f]];
				int g = group[f];
				for (int i=0;i<grouplength[f];i++) {
					int a= c.A[f][g][i];
					qx[f][i] = nu[f][a] *(nfct[f][a][t] + beta_0) / (nfc[f][a] + c.V * beta_0);
					qsum += qx[f][i];
				}
			}

			//normalise q



			qk = BasicMath.normalise(qk);

			for (int f=0;f<c.F;f++) {
				int g = group[f];
				for (int i=0;i<grouplength[f];i++) {
					int a= c.A[f][g][i];
					qx[f][i] /= qsum;
					batch_nfct[f][a][t]+=qx[f][i];
					batch_nxfc[1][f][a]+=qx[f][i];

				}
			}			
			for (int y=0;y<2;y++) {
				for (int f=0;f<c.F;f++) {
					int g = group[f];
					for (int i=0;i<grouplength[f];i++) {
						int a= c.A[f][g][i];
						for (int k=0;k<T;k++) {
							q[y][f][i][k]/=qsum;
							batch_nfckt[f][a][k][t]+=q[y][f][i][k];
							batch_nky[k][y]+=q[y][f][i][k];
							batch_nfji[f][g][i]+=q[y][f][i][k];
							batch_nxfc[0][f][a]+=q[y][f][i][k];
						}
					}
				}
			}




			for (int k=0;k<T;k++) {

				//add to batch counts
				if (rhot_step>BURNIN) {
					batch_nkt[k][t]+=qk[k]*termfreq;
					batch_mkt[k][t]+=Math.log(1.0-qk[k])*termfreq;
				}

				//update probability of _not_ seeing k in the current document
				sumqmk[k]+=Math.log(1.0-qk[k])*termfreq;

				if (c.getN(m) != termfreq) {
					//update document-feature-cluster-topic counts
					if (termfreq==1) {
						nmk[m][k] = (float) (oneminusrhostkt_document * nmk[m][k] + rhostkt_documentNm * qk[k]);
					}
					else {
						double temp = Math.pow(oneminusrhostkt_document,termfreq);
						nmk[m][k] = (float) (temp * nmk[m][k] + (1.0-temp) * c.getN(m) * qk[k]);
					}
				}
				else {
					nmk[m][k]=(float) (qk[k]*termfreq);
				}


			}

		}
		//End of loop over document words


		//get probability for NOT seeing topic f to update delta
		//double[] tables_per_feature = new double[c.F];
		double[] topic_ge_0 = new double[T];
		for (int k=0;k<T;k++) {
			//Probability that we saw the given topic
			topic_ge_0[k] = (1.0 - Math.exp(sumqmk[k]));
		}

		//We update global topic-word counts in batches (mini-batches lead to local optima)
		//after a burn-in phase
		if (rhot%BATCHSIZE == 0 && rhot_step>BURNIN) {
			updateGlobalTopicParameters();
		}

		for (int f=0;f<c.F;f++) {
			for (int k=0;k<T;k++) {
				batchmf[f] += topic_ge_0[k] * pk_f[k][f];
			}
		}

		if (rhot_step>BURNIN_DOCUMENTS) {



			for (int f=0;f<c.F;f++) {

				int g = group[f];
				//increase count for that group
				rhot_group[f][g]++;

				//update feature-counter
				for (int i=0;i<grouplength[f];i++) {
					int a = c.A[f][g][i];
					rhot_cluster[f][a]++;
					batch_cluster_words[f][a]+=c.getN(m);

					for (int k=0;k<T;k++) {

						//gives the probability that a table of a topic was drawn from the given cluster



						//p(not_seeing_fik)
						batch_logmfck[f][a][k] += Math.log(1.0 - (topic_ge_0[k] * n_alpha_fi[f][i][k]));
						if (debug&& (batch_logmfck[f][a][k]==0 || Double.isNaN(batch_logmfck[f][a][k]))) {
							System.out.println(
									"batch_logmfck "+batch_logmfck[f][a][k]
											+"\n topic_ge_0 " + topic_ge_0[k]
													+"\n n_alpha_fi " + n_alpha_fi[f][i][k]
									);
						}

						//if ((1.0 - Math.exp(sumqtemp[f][g][i][k])) >= tempsumqfgc[f][g][i][k])
						//	System.out.println((1.0 - Math.exp(sumqtemp[f][g][i][k])) + " " + tempsumqfgc[f][g][i][k] );
					}
					updateClusterParameters(f,a);	

				}

				updateGroupParameters(f,g);	

			}

		}

		//take e.g. 10000 samples to estimate alpha_1
		if (rhot_step>BURNIN_DOCUMENTS) {

			//ignore documents containing only one word.
			if (rhot%SAMPLE_ALPHA == 0 && c.getN(m)>1) {
				alpha_1_nm[alpha_batch_counter] = c.getN(m);
				for (int k=0;k<T;k++) {
					alpha_1_nmk[alpha_batch_counter][k] = nmk[m][k];
					alpha_1_pimk[alpha_batch_counter][k] = topic_prior[k];
				}

				alpha_batch_counter++;

				if (alpha_batch_counter>=BATCHSIZE_ALPHA) {
					//TODO: Fix alpha_1 estimate
					//System.out.println("estimating alpha1");
					alpha_1 = DirichletEstimation.estimateAlphaMap(alpha_1_nmk,alpha_1_nm,alpha_1_pimk,alpha_1,1,1,20);
					alpha_batch_counter=0;

				}
			}
		}

	}

	/**
	 * Here we do stochastic updates of the global parameters
	 */
	public synchronized void updateGlobalTopicParameters() {

		int sum_batch_words = BasicMath.sum(batch_words);

		double rhostkt = rho(rhos,rhotau,rhokappa,rhot/BATCHSIZE);
		double rhostktnormC = rhostkt * (c.C / Double.valueOf(BasicMath.sum(batch_words)));
		double oneminusrhostkt = (1.0 - rhostkt);


		for (int f=0;f<c.F;f++) {
			sumqf[f]=(1.0-rhostkt) * sumqf[f] +  rhostkt * batchmf[f] * (c.M * TRAINING_SHARE / Double.valueOf(BATCHSIZE));
			batchmf[f] = 0;
			zeta[f] = sumqf[f]+epsilon[f];
		}

		zeta = BasicMath.normalise(zeta);




		nk = new float[T];
		for (int k=0;k<T;k++) {
			for (int t=0;t<c.V;t++) {

				nkt[k][t] *= oneminusrhostkt;

				//update word-topic-tables for estimating tau
				mkt[k][t] *= oneminusrhostkt;
				if(!debug && Double.isInfinite(mkt[k][t])) {
					System.out.println("mkt pre " + mkt[k][t] );
					debug = true;
				}

				//we estimate the topic counts as the average q (tempnkt consists of BATCHSIZE observations)
				//and multiply this with the size of the corpus C
				if (batch_nkt[k][t]>0) {

					nkt[k][t] += rhostktnormC * batch_nkt[k][t];
					//estimate tables in the topic per word, we just assume that the topic-word assignment is 
					//identical for the other words in the corpus.
					mkt[k][t] += rhostkt * (1.0-Math.exp(batch_mkt[k][t]*(c.C / Double.valueOf(BasicMath.sum(batch_words)))));
					if(!debug &&  (Double.isInfinite(batch_mkt[k][t]) || Double.isInfinite(mkt[k][t]))) {
						System.out.println("mkt estimate " + batch_mkt[k][t] + " " + mkt[k][t] );
						debug = true;
					}

					//reset batch counts
					batch_nkt[k][t] = 0;
					//reset word counts in the last topic iteration

				}

				nk[k] += nkt[k][t];

			}
		}

		//reset
		for (int k=0;k<T;k++) {
			for (int t=0;t<c.V;t++) {
				batch_mkt[k][t] = (float) 0.0;
			}
		}

		//		
		//		this.batch_nct
		//		this.batch_nfckt
		//		this.batch_nfgik
		//		this.batch_nfji
		//		this.batch_nxfc
		//		this.batch_nyk
		//

		for (int k=0;k<T;k++) {
			for (int y=0;y<2;y++) {
				nky[k][y] = (float)(oneminusrhostkt * nky[k][y] + rhostkt * batch_nky[k][y] * (c.C / Double.valueOf(sum_batch_words)));
			}
			lambda[k] = (float) ((nky[k][0] + kappa)/(nky[k][0]+nky[k][1]+2*kappa));
			if (debug && lambda[k]==0) {
				System.out.println("nky0 "+nky[k][0] + " nky1 " +nky[k][1] + " kappa " +kappa);
			}

		}

		for (int t=0;t<c.V;t++) {
			batch_words[t] = 0;
		}

	}

	private synchronized void updateClusterParameters(int f, int a) {

		int BATCHSIZE_CLUSTER_MIN = Math.min(c.Cfc[f][a],BATCHSIZE_GROUPS);

		//System.out.println(rhot_cluster[f][a]);

		if (rhot_cluster[f][a] % BATCHSIZE_CLUSTER_MIN == 0) {

			//TODO: Cluster-specific learning rate
			double rhost_cluster = rho(rhos_group,rhotau_group,rhokappa_group,rhot_cluster[f][a]/BATCHSIZE_CLUSTER_MIN);
			double oneminusrho = 1.0-rhost_cluster;


			//total documents in cluster - remember that this includes documents from other groups
			int cluster_size = c.Cfc[f][a];

			mfc[f][a] = 0;
			nfc[f][a] = 0;
			for (int k=0;k<T;k++) {

				//update table counts for the global topic distribution:
				//-> Probability of seeing topic k once in each cluster?

				//System.out.println("update lfck");

				//update the probability of seeing a table in the cluster: E(m_{f,c,k} > 0)
				lfck[f][a][k] = oneminusrho*lfck[f][a][k] + rhost_cluster * (1.0 - Math.exp((batch_logmfck[f][a][k]*cluster_size)/BATCHSIZE_CLUSTER_MIN));


				if (debug && (Double.isNaN(lfck[f][a][k]) || lfck[f][a][k]<=0 || cluster_size==0)) {
					System.out.println(
							"batch_logmfck "+ batch_logmfck[f][a][k]
									+"\n cluster_size "+cluster_size
									+"\n BATCHSIZE_CLUSTER_MIN " + BATCHSIZE_CLUSTER_MIN
									+"\n oneminusrho "+ oneminusrho
							);
				}

				//update counts per cluster
				mfck[f][a][k] = oneminusrho*mfck[f][a][k] + rhost_cluster * Double.valueOf(cluster_size)/Double.valueOf(BATCHSIZE_CLUSTER_MIN) * Math.exp(batch_logmfck[f][a][k]);			

				mfc[f][a]+=mfck[f][a][k];

				nfck[f][a][k]=0;
				for (int t=0;t<c.V;t++) {
					nfckt[f][a][k][t]*=oneminusrho;
					if (batch_nfckt[f][a][k][t]>0) {
						float temp = (float) (rhost_cluster * c.Cfcw[f][a]/Double.valueOf(batch_cluster_words[f][a]) * batch_nfckt[f][a][k][t]);
						nfckt[f][a][k][t]+=temp;
						nfck[f][a][k]+=nfckt[f][a][k][t];
						batch_nfckt[f][a][k][t]=0;
					}
				}
				nfc[f][a]+=nfck[f][a][k];

			}


			nxfc[0][f][a] = (float) (oneminusrho * nxfc[0][f][a] + (rhost_cluster*batch_nxfc[0][f][a])*c.Cfcw[f][a]/batch_cluster_words[f][a]);
			nxfc[1][f][a] = (float) (oneminusrho * nxfc[1][f][a] + (rhost_cluster*batch_nxfc[1][f][a])*c.Cfcw[f][a]/batch_cluster_words[f][a]);

			batch_nxfc[0][f][a]=0;
			batch_nxfc[1][f][a]=0;

			for (int t=0;t<c.V;t++) {

				nfct[f][a][t]*=oneminusrho;
				if (batch_nfct[f][a][t]>0) {
					float temp = (float) ((rhost_cluster*batch_nfct[f][a][t])*c.Cfcw[f][a]/Double.valueOf(batch_cluster_words[f][a]));
					nfct[f][a][t]+=temp;			
					batch_nfct[f][a][t]=0;
				}
			}
			nfc[f][a]=(float) BasicMath.sum(nfct[f][a]);


			batch_cluster_words[f][a]=0;
		}

	}


	/**
	 * @param f feature of the group
	 * @param g	group id
	 * 
	 *  Stochastic update of the topic counts for a given group of a feature
	 *  
	 */
	private synchronized void updateGroupParameters(int f, int g) {
		//These are the global variables...
		//sumqkfc2[f][a][k] ok
		//sumqfgc[f][group[f]][i] ok 
		//sumqfg[f][group[f]] 


		//we update after we saw all documents from the group 
		//OR if we saw BATCHSIZE_GROUPS documents
		int BATCHSIZE_GROUP_MIN = Math.min(c.Cfg[f][g],BATCHSIZE_GROUPS);
		if (rhot_group[f][g] % BATCHSIZE_GROUP_MIN == 0) {

			//calculate update rate
			double rhost_group = rho(rhos_group,rhotau_group,rhokappa_group,rhot_group[f][g]);
			double oneminusrho = 1.0-rhost_group;



			//sum over table counts per cluster


			int groupsize = c.A[f][g].length;
			for (int i=0;i<groupsize;i++) {
				int a = c.A[f][g][i];

				//update group-cluster-counts: how many tables do we expect to see for group i?
				//Double.valueOf(Cfg[f][g])/Double.valueOf(BATCHSIZE_GROUPS) is the proportion of observed words!



				//TODO: nfgik??
				mfgi[f][g][i] = (float) (oneminusrho*mfgi[f][g][i] + rhost_group * (c.Cfc[f][a]/Double.valueOf(BATCHSIZE_GROUP_MIN)) * batch_mfgi[f][g][i]);

				batch_mfgi[f][g][i]=0;



				eta[f][g][i] = (float) (mfgi[f][g][i] +delta[f]);

			}

			eta[f][g] = BasicMath.normalise(eta[f][g]);



			//Update global topic distribution
			updateGlobalTopicDistribution();



		}
	}

	private synchronized void updateGlobalTopicDistribution() {

		//we have to do one run where the cluster parameters are learned!
		if (rhot_step > BURNIN_DOCUMENTS+1)  {
			//sum over tables
			double[] sumfck = new double[T];
			double[] ahat = new double[T];
			double[] bhat = new double[T];

			
			//Start with pseudo-counts from the Beta prior
			for (int k=0;k<T;k++) {
				bhat[k]=gamma;
				if (debug && (Double.isNaN(bhat[k]) || bhat[k]<=0)){
					System.out.println( "gamma " + gamma
					);
					System.exit(0);
		}
			}
			//Now add observed estimated counts

			//sum_cluster_tables = 0;
			for (int f=0;f<c.F;f++) {
				//A[f] holds the cluster indices for each cluster of each feature and thus gives us the 
				//number of clusters per feature by A[f].length
				for (int i=0;i< c.Cf[f];i++) {
					if (BasicMath.sum(lfck[f][i])>0) {
						for (int k=0;k<T;k++) {
							//We estimate pi_0 by looking at the documents of each cluster of each feature.

							//boolean expected_tables = true;
							double tables;
							//if (expected_tables) {
							//NEW:
							//Expected table counts like in Teh, Collapsed Variational Inference for HDP (but with 0-order Taylor approximation)
							double a0pik=alpha_0 * pi_0[k];
							if (debug && (Double.isNaN(lfck[f][i][k]) || lfck[f][i][k]<=0)){
								System.out.println( "lfck f " + f + " c " + i + " k " + k + " | "+lfck[f][i][k]);
							}
							tables = (lfck[f][i][k] > 0) ? a0pik * lfck[f][i][k] * (Gamma.digamma0(a0pik + mfck[f][i][k] / lfck[f][i][k]) - Gamma.digamma0(a0pik)) : 0;
							
							if (debug && (Double.isNaN(tables) || tables<0)){
										System.out.println( "lfck f " + f + " c " + i + " k " + k + " | "+lfck[f][i][k]
												+"\n mfck" + mfck[f][i][k]
												+"\n a0pik " + a0pik
												+"\n alpha_0 "+ alpha_0
												+"\n pi_0[k] "+ pi_0[k]
										);
										System.exit(0);
							}
							
							//}
							//else {
							//Sampled number of tables -> better perplexity
							//	tables = sumqfck_ge0[f][i][k] * rs.randNumTable(pi_0[k], sumqfck[f][i][k]);
							//}
							sumfck[k] += tables;
							//sum_cluster_tables += sumfck[k];
						}
					}
				}
			}

			//now add this sum to ahat
			for (int k=0;k<T;k++) {
				ahat[k]=1.0+sumfck[k];
			}
			double[] ahat_copy = new double[T];
			System.arraycopy(ahat, 0, ahat_copy, 0, ahat.length);
			//get indices of sticks ordered by size (given by ahat)
			int[] index = ArrayUtils.sortArray(ahat_copy,"desc");
			//on which position in the list is the given index
			//int[] index_reverted = ArrayUtils.reverseIndex(index);
			
//			for (int k=0;k<T;k++) {
//				System.out.println("k " + k + " " + index[k] + " " + ahat[k] + " " + ahat[index[k]]);//+ " " + index_reverted[k]);
//			}

			//bhat is the sum over the counts of all topics > k		
			int sum = 0;
			for (int k2=T-1;k2>=0;k2--) {
				int i = index[k2];
				if (k2<T-1) {
					bhat[i]=sum;
				}
				sum += (double) sumfck[i];
			}
			
//			for (int k2=0;k2<T;k2++) {
//				int i = index_reverted[k2];
//				System.out.println(bhat[i]);
//			}
				

			double[] pi_ = new double[T];

			for (int k=0;k<T;k++) {
				pi_[k]=ahat[k] / (ahat[k]+bhat[k]);
			}
//			for (int k=0;k<T-1;k++) {
//				pi_0[k]=pi_[k];
//			}
//			for (int k=0;k<T-1;k++) {
//
//				int sort_index = index_reverted[k];
//				System.out.println();
//
//				for (int l=0;l<sort_index;l++) {
//					int sort_index_lower = index[l];
//										System.out.print(sort_index_lower + " ");
//					pi_0[k]*=(1.0-pi_[sort_index_lower]);
//				}
//				System.out.println();
//
//				if (debug && (Double.isNaN(pi_0[k]) || pi_0[k]<0 || pi_0[k] > 1 || Double.isNaN(ahat[k]) || ahat[k] <=0 || Double.isNaN(bhat[k])|| bhat[k] <=0)){
//					System.out.println(
//							"ahatk " + ahat[k]
//							+"\n bhatk " + bhat[k]
//							+"\n sumfck "+ sumfck[k]
//							);
//					System.exit(0);
//				}
//			}
//			//probability of last pi_0 is the rest

			
			//rest is the remaining stick length
			double rest = 1.0;
			int i0 = index[0];
			pi_0[i0] = pi_[i0];
			rest -= pi_0[i0];
			
			for (int k2=1;k2<T-1;k2++) {
				int i = index[k2];
				pi_0[i] = pi_[i]*rest;
				rest -= pi_0[i];
			}
			
			//probability of last pi_0 is the rest (truncation)
			int last_index = index[T-1];
			pi_0[last_index]=rest;
//			for (int k2=0;k2<T;k2++) {
//				System.out.println("k2 " + k2 + " " + pi_0[k2]);
//			}


			//MAP estimation for gamma (Sato (6))
			double gamma_denominator = 0.0;
			for (int k=0;k<T-1;k++) {
				gamma_denominator += Gamma.digamma0(ahat[k] + bhat[k])- Gamma.digamma0(bhat[k]);
				if (debug && (Double.isNaN(pi_0[k]) || pi_0[k]<0 || pi_0[k] > 1 || Double.isNaN(ahat[k]) || ahat[k] <=0 || Double.isNaN(bhat[k])|| bhat[k] <=0)){
				System.out.println(
						"ahatk " + ahat[k]
						+"\n bhatk " + bhat[k]
						+"\n sumfck "+ sumfck[k]
						);
				System.exit(0);
				}
			}

			int a = 1;
			int b = 0;
			gamma = (T + a - 2) / (gamma_denominator + b);
			
			if (debug && (Double.isNaN(gamma) || gamma<=0)){
				System.out.println(
						"\n gamma_denominator "+ gamma_denominator
						);
				System.exit(0);

			}
			
		}

	}

	public void updateHyperParameters() {

		if(rhot_step>BURNIN_DOCUMENTS+2) {


			//TODO: update beta_0
			if (1==0) {
				double beta_0_denominator = 0.0;
				for (int k=0; k < T; k++) {
					//log(x-0.5) for approximating the digamma function, x >> 1 (Beal03)
					beta_0_denominator += Gamma.digamma0(nk[k]+beta_0*c.V);
				}
				beta_0_denominator -= T * Gamma.digamma0(beta_0*c.V);
				beta_0 = 0;
				for (int k=0;k<T;k++) {
					for (int t = 0; t < c.V; t++) {
						beta_0 += mkt[k][t];
						if (debug && (Double.isInfinite(mkt[k][t] ) || Double.isNaN(mkt[k][t] ))) {
							System.out.println("mkt " + k + " " + t + ": " + mkt[k][t] + " nkt: " +  nkt[k][t]);
						}
					}
				}

				beta_0 /= beta_0_denominator;

				//prevent too small values of beta_0
				//beta_0 = Math.min(0.01, beta_0);

				//at this point, beta is multiplied by V
				beta_0V = beta_0;

				//Correct value of beta
				beta_0 /= c.V;
			}


			//		beta_0 = DirichletEstimation.estimateAlphaLikChanging(nkt,beta_0,200);
			//
			//		beta_0V = beta_0 * V;


			//gamma prior Gamma(1,1), Minka
			//beta_0 = DirichletEstimation.estimateAlphaMap(nkt,nk,beta_0,1.0,1.0);

			//epsilon = DirichletEstimation.estimateAlphaLik(sumqf,epsilon);

			//		for (int i=0;i<sumqfgc.length;i++) {
			//			for (int j=0;j<sumqfgc[i].length;j++) {
			//				if (BasicMath.sum(sumqfgc[i][j])> 1) {
			//					System.out.println(i + " "+ j + ": ");
			//					for (int c=0;c<sumqfgc[i][j].length;c++) {
			//						System.out.println(" "+ sumqfgc[i][j][c]);
			//					}
			//					System.out.println();
			//				}
			//			}
			//		}

			if (delta_fix == 0) {
				for (int f=0;f<c.F;f++) {
					System.out.println("estimating delta");
					delta[f] = DirichletEstimation.estimateAlphaLikChanging(mfgi[f], delta[f], 200);
				}
			}

			//sum over tables
			int clusters = BasicMath.sum(c.Cf);
			double[][] sumfck = new double[clusters][T];
			double[] sumfc = new double[clusters];


			//Now add observed estimated counts

			int j=0;
			//sum_cluster_tables = 0;
			for (int f=0;f<c.F;f++) {
				for (int i=0;i< c.Cf[f];i++) {
					//Check for empty clusters
					if (BasicMath.sum(mfck[f][i])>0) {
						sumfck[i] = new double[T];
						for (int k=0;k<T;k++) {
							sumfck[j][k] = mfck[f][i][k];
							sumfc[j]+=mfck[f][i][k];
							//System.out.println(sumfck[j][k]);
						}
						j++;
					}
				}
			}
			//TODO: fix StackOverflowError for digamma function
			//System.out.println("estimating alpha0");
			alpha_0 = DirichletEstimation.estimateAlphaMap(sumfck, sumfc, pi_0, alpha_0,1,1,20);


		}


	}


	public double rho (int s,int tau, double kappa, int t) {
		return Double.valueOf(s)/Math.pow((tau + t),kappa);
	}


	public void save () {

		String output_base_folder = c.directory + "output_MVHMDP/";

		File output_base_folder_file = new File(output_base_folder);
		if (!output_base_folder_file.exists()) output_base_folder_file.mkdir();

		String output_folder = output_base_folder + rhot_step + "/";

		File file = new File(output_folder);
		if (!file.exists()) file.mkdir();

		Save save = new Save();
		save.saveVar(nkt, output_folder+save_prefix+"nkt");
		save.close();
		save.saveVar(pi_0, output_folder+save_prefix+"pi0");
		save.close();
		save.saveVar(sumqf, output_folder+save_prefix+"sumqf");
		save.close();
		save.saveVar(alpha_0, output_folder+save_prefix+"alpha_0");
		save.close();
		save.saveVar(alpha_1, output_folder+save_prefix+"alpha_1");
		save.close();

		//We save the large document-topic file every 10 save steps, together with the perplexity
		if ((rhot_step % (SAVE_STEP *10)) == 0) {

			save.saveVar(perplexity(), output_folder+save_prefix+"perplexity");

		}
		if (rhot_step == RUNS) {

			float[][] doc_topic;
			if (store_empty) {

				//index counter for empty documents
				int empty_counter = 0;
				//#documents including empty documents
				int Me = c.M + c.empty_documents.size();
				doc_topic = new float[Me][T];
				for (int m=0;m<Me;m++) {
					for (int k=0;k<T;k++) {
						doc_topic[m][k]  = 0;
					}
				}
				int m = 0;
				for (int me=0;me<Me;me++) {
					if (c.empty_documents.contains(me)) {
						doc_topic[me]  = new float[T];
						int[] group = c.groups[c.M + empty_counter];
						int[] grouplength = new int[c.F]; 
						for (int f =0; f<c.F;f++) {
							int g = group[f];
							grouplength[f] = c.A[f][g].length;
							for (int i=0;i<grouplength[f];i++) {
								for (int k=0;k<T;k++) {
									doc_topic[me][k]+=eta_pic_kfc[f][g][i][k];
								}
							}
						}
						double sum = BasicMath.sum(doc_topic[me]);
						for (int k=0;k<T;k++) {
							doc_topic[me][k]/=sum;
						}
						empty_counter++;
					}
					else {			
						//if doc is in the training set
						if (m<c.M) {
							doc_topic[me] = nmk[m];
						}
						int[] group = c.groups[m];
						int[] grouplength = new int[c.F]; 
						for (int f =0; f<c.F;f++) {
							int g = group[f];
							grouplength[f] = c.A[f][g].length;
							for (int i=0;i<grouplength[f];i++) {
								for (int k=0;k<T;k++) {
									doc_topic[me][k]+=eta_pic_kfc[f][g][i][k];
								}
							}
						}
						double sum = BasicMath.sum(doc_topic[me]);
						for (int k=0;k<T;k++) {
							doc_topic[me][k]/=sum;
						}
						m++;
					}
				}

			}
			else {
				doc_topic = new float[c.M][T];
				for (int m=0;m < c.M;m++) {
					for (int k=0;k<T;k++) {
						doc_topic[m][k]  = 0;
					}
				}
				for (int m=0;m < c.M;m++) {
					doc_topic[m]  = nmk[m];
					int[] group = c.groups[m];
					int[] grouplength = new int[c.F]; 
					for (int f =0; f<c.F;f++) {
						int g = group[f];
						grouplength[f] = c.A[f][g].length;
						for (int i=0;i<grouplength[f];i++) {
							for (int k=0;k<T;k++) {
								doc_topic[m][k]+=eta_pic_kfc[f][g][i][k];
							}
						}
					}
					double sum = BasicMath.sum(doc_topic[m]);
					for (int k=0;k<T;k++) {
						doc_topic[m][k]/=sum;
					}						
				}
			}

			save.saveVar(doc_topic, output_folder+save_prefix+"doc_topic");
			save.close();
		}

		double[][][] feature_cluster_topics = new double[c.F][][];

		for (int f=0; f<c.F;f++) {
			feature_cluster_topics[f] = new double[c.Cf[f]][T];
			for (int a=0;a< c.Cf[f];a++) {
				double sumqfck2_denominator = BasicMath.sum(mfck[f][a]) + alpha_0;
				for (int k=0;k<T;k++) {
					feature_cluster_topics[f][a][k]=(mfck[f][a][k] + alpha_0 * pi_0[k]) / sumqfck2_denominator;
				}
			}

			save.saveVar(feature_cluster_topics[f], output_folder+save_prefix+"clusters_"+f+"");
			save.close();
		}

		if (topk > c.V) {
			topk = c.V;
		}


		String[][] topktopics = new String[T*2][topk];

		for (int k=0;k<T;k++) {

			List<Pair> wordprob = new ArrayList<Pair>(); 
			for (int v = 0; v < c.V; v++){
				wordprob.add(new Pair(c.dict.getWord(v), (nkt[k][v]+beta_0)/(nk[k]+beta_0V), false));
			}
			Collections.sort(wordprob);

			for (int i=0;i<topk;i++) {
				topktopics[k*2][i] = (String) wordprob.get(i).first;
				topktopics[k*2+1][i] = String.valueOf(wordprob.get(i).second);
			}

		}
		save.saveVar(topktopics, output_folder+save_prefix+"topktopics");

		int n_clusters = BasicMath.sum(c.Cf);
		String[][] topktopics_cluster_background = new String[n_clusters*3][];

		int line = 0;
		for (int f=0;f<c.F;f++) {
			for (int i=0;i<c.Cf[f];i++) {
				topktopics_cluster_background[line*3] = new String[1]; 

				List<Pair> wordprob = new ArrayList<Pair>(); 
				for (int v = 0; v < c.V; v++){
					wordprob.add(new Pair(c.dict.getWord(v), (nfct[f][i][v]+beta_0)/(nfc[f][i]+beta_0V), false));
				}
				Collections.sort(wordprob);

				topktopics_cluster_background[line*3][0]=f + " " + i;
				topktopics_cluster_background[line*3+1]=new String[topk];
				topktopics_cluster_background[line*3+2]=new String[topk];
				for (int j=0;j<topk;j++) {
					topktopics_cluster_background[line*3+1][j] = (String) wordprob.get(j).first;
					topktopics_cluster_background[line*3+2][j] = String.valueOf(wordprob.get(j).second);
				}

				line++;
			}
		}
		save.saveVar(topktopics_cluster_background, output_folder+save_prefix+"topktopics_cluster_background");

		String[][] topktopics_cluster = new String[n_clusters*T*3][];

		line = 0;
		for (int f=0;f<c.F;f++) {
			for (int i=0;i<c.Cf[f];i++) {
				for (int k=0;k<T;k++) {

					topktopics_cluster[line*3] = new String[1]; 

					List<Pair> wordprob = new ArrayList<Pair>(); 
					for (int v = 0; v < c.V; v++){
						wordprob.add(new Pair(c.dict.getWord(v), (nfckt[f][i][k][v]+beta_0)/(nfck[f][i][k]+beta_0V), false));
					}
					Collections.sort(wordprob);

					topktopics_cluster[line*3][0]=f + " " + i + " " + k;
					topktopics_cluster[line*3+1]=new String[topk];
					topktopics_cluster[line*3+2]=new String[topk];
					for (int j=0;j<topk;j++) {
						topktopics_cluster[line*3+1][j] = (String) wordprob.get(j).first;
						topktopics_cluster[line*3+2][j] = String.valueOf(wordprob.get(j).second);
					}

					line++;
				}
			}
		}
		save.saveVar(topktopics_cluster, output_folder+save_prefix+"topktopics_cluster");


		save.saveVar(delta, output_folder+save_prefix+"delta");
		save.saveVar(epsilon, output_folder+save_prefix+"epsilon");


		save.saveVar("alpha_0 "+alpha_0+
				"\nalpha_1 "+ alpha_1+
				"\nbeta_0 " + beta_0 +
				"\ngamma "+gamma+
				"\nrhos "+rhos+
				"\nrhotau "+rhotau+
				"\nrhokappa "+rhokappa+
				"\nBATCHSIZE "+BATCHSIZE+
				"\nBATCHSIZE_GROUPS "+BATCHSIZE_GROUPS+
				"\nBURNIN "+BURNIN+
				"\nBURNIN_DOCUMENTS "+BURNIN_DOCUMENTS+
				"\nMIN_DICT_WORDS "+c.MIN_DICT_WORDS
				,output_folder+save_prefix+"others");


		//save counts for tables in clusters
		double[] sumfck = new double[T];
		for (int f=0;f<c.F;f++) {
			for (int i=0;i< c.Cf[f];i++) {
				for (int k=0;k<T;k++) {
					//NEW:
					//Table counts like in Teh, Collapsed Variational Inference for HDP (but with 0-order Taylor approximation)
					double a0pik=alpha_0 * pi_0[k];
					sumfck[k]+=a0pik * lfck[f][i][k] * (Gamma.digamma0(a0pik + mfck[f][i][k]) - Gamma.digamma0(a0pik));
				}
			}
		}
		save.saveVar(sumfck, output_folder+save_prefix+"sumfck");

		double[] topic_ge0 = new double[T];
		for (int k=0;k<T;k++) {
			topic_ge0[k] = 1.0;
		}
		for (int f=0;f<c.F;f++) {
			for (int i=0;i< c.Cf[f];i++) {
				for (int k=0;k<T;k++) {
					topic_ge0[k] *= 1.0-lfck[f][i][k];
				}
			}
		}
		for (int k=0;k<T;k++) {
			topic_ge0[k] = 1.0-topic_ge0[k];
		}

		save.saveVar(topic_ge0, output_folder+save_prefix+"topic_ge0");

	}

	public double perplexity () {

		int testsize = (int) Math.floor(TRAINING_SHARE * c.M);
		if (testsize == 0) return 0;

		int totalLength = 0;
		double likelihood = 0;

		for (int m = testsize; m < c.M; m++) {
			totalLength+=c.getN(m);
		}

		int runmax = 20;

		for (int m = testsize; m < c.M; m++) {

			int[] termIDs = c.getTermIDs(m);
			short[] termFreqs = c.getTermFreqs(m);

			int termlength = termIDs.length;
			double[][] z = new double[termlength][T];

			//sample for 200 runs
			for (int RUN=0;RUN<runmax;RUN++) {

				int[] grouplength = new int[c.F];
				int[] group = c.groups[m];


				for (int f=0;f<c.F;f++) {
					//Number of clusters of the group
					grouplength[f] = c.A[f][group[f]].length;
				}

				//Prior of the document-topic distribution
				//(This is a mixture of the cluster-topic distributions of the clusters of the document
				double[] topic_prior = new double[T];
				for (int f=0;f<c.F;f++) {
					int g = group[f];
					double sumqfgc_denominator = BasicMath.sum(mfgi[f][g]) + c.A[f][g].length*delta[f];
					double temp2 = (sumqf[f] + epsilon[f]);
					for (int i=0;i<grouplength[f];i++) {
						int a= c.A[f][g][i];
						double sumqfck2_denominator = BasicMath.sum(mfck[f][a])+ alpha_0;
						//cluster probability in group
						double temp3 = (mfgi[f][g][i] + delta[f]) / (sumqfgc_denominator * sumqfck2_denominator);
						for (int k=0;k<T;k++) {
							double temp = 	(mfck[f][a][k] + alpha_0 * pi_0[k])	* temp3;
							topic_prior[k]+=temp*temp2;

						}
					}
				}

				//word index
				int n = 0;

				//Process words of the document
				for (int i=0;i<termIDs.length;i++) {

					//term index
					int t = termIDs[i];
					//How often doas t appear in the document?
					int termfreq = termFreqs[i];

					//remove old counts 
					for (int k=0;k<T;k++) {
						nmk[m][k] -= termfreq * z[n][k];
					}

					//topic probabilities - q(z)
					double[] q = new double[T];
					//sum for normalisation
					double qsum = 0.0;

					for (int k=0;k<T;k++) {

						q[k] = 	//probability of topic given feature & group
								(nmk[m][k] + alpha_1*topic_prior[k])
								//probability of topic given word w
								* (nkt[k][t] + beta_0) 
								/ (nk[k] + beta_0V);


						qsum+=q[k];

					}

					//Normalise gamma (sum=1), update counts and probabilities
					for (int k=0;k<T;k++) {
						//normalise
						q[k]/=qsum;
						z[n][k]=q[k];
						nmk[m][k]+=termfreq*q[k];
					}

					n++;
				}
			}

			int n=0;
			for (int i=0;i<termIDs.length;i++) {

				//term index
				int t = termIDs[i];
				//How often doas t appear in the document?
				int termfreq = termFreqs[i];

				double lik = 0;

				for (int k=0;k<T;k++) {
					lik +=   z[n][k] * (nkt[k][t] + beta_0) / (nk[k] + beta_0V);	
				}


				//				for (int k=0;k<T;k++) {
				//					lik +=  z[mt][n][k] * (nkt[k][t] + beta_0) / (nk[k] + beta_0V);
				//				}
				likelihood+=termfreq * Math.log(lik);



				n++;
			}

			for (int k=0;k<T;k++) nmk[m][k] = 0;
		}


		//get perplexity
		double perplexity = Math.exp(- likelihood / Double.valueOf(totalLength));

		System.out.println("Perplexity: " + perplexity);

		return (perplexity);


	}


}
