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
import java.util.Map.Entry;
import java.util.Set;

import org.gesis.promoss.tools.math.BasicMath;
import org.gesis.promoss.tools.probabilistic.ArrayUtils;
import org.gesis.promoss.tools.probabilistic.DirichletEstimation;
import org.gesis.promoss.tools.probabilistic.Gamma;
import org.gesis.promoss.tools.probabilistic.Pair;
import org.gesis.promoss.tools.probabilistic.RandomSamplers;
import org.gesis.promoss.tools.text.HMDP_Corpus;
import org.gesis.promoss.tools.text.Save;

/**
 * This is the practical collapsed stochastic variational inference
 * for the Hierarchical Multi-Dirichlet Process Topic Model (HMDP)
 */
public class HMDP_PCSVB {

	//This class holds the corpus and its properties
	//including metadata
	public HMDP_Corpus c;

	//We have a debugging mode for checking the parameters
	public boolean debug = false;
	//Number of top words returned for the topic file
	public int topk = 100;
	//Number of iterations over the dataset for topic inference
	public int RUNS = 100;
	//Save variables after step SAVE_STEP
	public int SAVE_STEP = 10;
	public int BATCHSIZE = 128;
	public int BATCHSIZE_GROUPS = -1;
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

	//Dirichlet parameter for multinomials over clusters
	public double[] delta;

	//decides if delta is fixed or not.
	public double delta_fix = 0;

	//Dirichlet parameter for multinomial over features
	public double[] epsilon;

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
	private float[][] tempnkt;
	//Estimated number of words in topic k
	public float[] nk;

	//Estimated number of tables for word v in topic k, used to update the topic-prior beta
	public float[][] mkt;
	//Estimated number of tables for word v in topic k, used to update the topic prior beta
	//temporal variable for estimation
	private float[][] tempmkt;
	//Topic "counts" per document
	public float[][] nmk;
	//variational parameters for the stick breaking process - parameters for Beta distributions \hat{a} and \hat{b}
	private double[] ahat;
	private double[] bhat;


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
	//count number of times the group was seen in the batch - FxG
	public int[][] rhot_group;

	/*
	 * Here we define helper variables
	 * Every feature has clusters
	 * Clusters belong to groups of connected clusters (e.g. adjacent clusters).
	 */

	//Sum over log(1-q(k,f,c)). We need this sum for calculating E[n>0] and E[n=0] 
	public double[][][] sumqfck_ge0;
	//Sum table counts for a given cluster; F x Cf x T
	public double[][][] sumqfck;
	//Sum over log(1-q(k,f,c)) for all documents in the given group g and cluster number c. We need this sum for calculating 
	//Sum of E[n>0] and E[n=0] for all documents of the group and cluster. 
	public double[][][] sumqfgc;
	//Sum over 1-q(_,f,_) for document M and feature f (approximated seat counts)
	public double[] sumqf;
	//Batch estimate of sumqf2 for stochastic updates
	//private static double[] sumqf2temp;

	private double[] featureprior;

	//Counter: how many observations do we have per cluster? Dimension: F x |C[f]|
	//We use this for doing batch updates of the cluster parameters and to calculate
	//the update rate \rho
	//private static int[][] rhot_cluster;


	//statistic over gamma, used to do batch-updates of clusters: sum of gamma
	private double[][][][] tempsumqfgc;
	//statistic over gamma, used to do batch-updates of features: sum of gamma
	//private static double[] sumqtemp2_features;
	//statistic over gamma, used to do batch-updates of clusters: prodct of gamma-1
	private double[][][][] sumqtemp;
	//sum over document-topic table estimates \sum_{m=0}^{M} p(n_{mk} > 0)) for batch
	public double sumnmk_ge0=0.0;

	//counts over the feature use
	private double[] tempsumqf;
	//counts over the group use
	double[][] sumq2_groups;

	//group-cluster-topic distributions F x G x C x T excluding feature distribution
	public double[][][][] pi_kfc_noF;

	public double rhostkt_document;
	public double oneminusrhostkt_document;

	//counts, how many documents we observed in the batch to estimate alpha_1
	public int alpha_batch_counter = 0;
	//document lengths in sample
	private double[][] alpha_1_pimk;
	private int[] alpha_1_nm;
	private double[][] alpha_1_nmk;

	private RandomSamplers rs = new RandomSamplers();

	//private double sum_cluster_tables = 0;

	HMDP_PCSVB() {
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
						topic_ge0[k] *= 1.0-sumqfck_ge0[f][d][k];
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

		epsilon = new double[c.F];
		for (int f=0;f<c.F;f++) {
			epsilon[f] = 1.0;
		}

		beta_0V = beta_0 * c.V;

		batch_words = new int[c.V];

		mkt = new float[T][c.V];	
		tempmkt = new float[T][c.V];

		nk = new float[T];
		nkt = new float[T][c.V];	
		tempnkt = new float[T][c.V];	

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

		nmk = new float[c.M][T];

		for (int t=0; t < c.V; t++) {
			for (int k=0;k<T;k++) {

				nkt[k][t]= (float) (Math.random()*INIT_RAND);
				nk[k]+=nkt[k][t];

				tempmkt[k][t] = (float) 0.0;

			}
		}
		pi_0 = new double[T];
		ahat = new double[T];
		bhat = new double[T];

		pi_0[0] = 1.0 / (1.0+gamma);
		for (int i=1;i<T;i++) {
			pi_0[i]=(1.0 / (1.0+gamma)) * (1.0-pi_0[i-1]);
		}
		pi_0 = BasicMath.normalise(pi_0);

		sumqfck_ge0 = new double[c.F][][];
		//rhot_cluster = new int[c.F][];
		//for (int f=0;f<c.F;f++) {
		//	rhot_cluster[f] = new int[c.Cf[f]];
		//}
		sumqfck = new double[c.F][][];
		tempsumqfgc = new double[c.F][][][];
		//sumqtemp2_features = new double[c.F];
		sumqtemp = new double[c.F][][][];
		for (int f=0;f<c.F;f++) {
			sumqfck_ge0[f] = new double[c.Cf[f]][T];
			sumqfck[f] = new double[c.Cf[f]][T];

			tempsumqfgc[f] = new double[c.A[f].length][][];
			sumqtemp[f] = new double[c.A[f].length][][];
			for (int g=0;g<c.A[f].length;g++) {
				tempsumqfgc[f][g]=new double[c.A[f][g].length][T];
				sumqtemp[f][g]=new double[c.A[f][g].length][T];
			}
		}


		sumqf = new double[c.F];

		featureprior = new double[c.F];
		for (int f=0;f<c.F;f++) {
			featureprior[f] = epsilon[f];
		}
		featureprior = BasicMath.normalise(featureprior);

		//sumqf2temp = new double[c.F];
		pi_kfc_noF = new double[c.F][][][];
		for (int f=0;f<c.F;f++) {
			pi_kfc_noF[f] = new double[c.A[f].length][][]; 
			for (int g=0;g<c.A[f].length;g++) {
				pi_kfc_noF[f][g] = new double[c.A[f][g].length][T];
				for (int i=0;i<c.A[f][g].length;i++) {
					for (int k = 0; k < T; k++) {
						//for every group: get topic distribution of clusters and their weight 
						//(the weight of the clusters) for the group
						pi_kfc_noF[f][g][i][k] = pi_0[k]/((double)c.A[f][g].length);
					}
				}
			}
		}

		tempsumqf = new double[c.F];
		sumq2_groups = new double[c.F][];
		for (int f=0;f<c.F;f++) {
			sumq2_groups[f] = new double[c.A[f].length];
		}

		sumqfgc = new double[c.F][][];
		for (int f=0;f<c.F;f++) {
			sumqfgc[f] = new double[c.A[f].length][];
			for (int g=0;g<c.A[f].length;g++) {
				sumqfgc[f][g] = new double[c.A[f][g].length];
			}
		}

		alpha_1_nm = new int[BATCHSIZE_ALPHA];
		alpha_1_nmk = new double[BATCHSIZE_ALPHA][T];
		alpha_1_pimk = new double[BATCHSIZE_ALPHA][T];

		SAMPLE_ALPHA = (int) Math.ceil(c.M / BATCHSIZE_ALPHA);

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
		double[][][] pk_fck = new double[c.F][][];
		for (int f=0;f<c.F;f++) {
			pk_fck[f] = new double[grouplength[f]][];
			for (int i=0;i<grouplength[f];i++) {
				pk_fck[f][i] = new double[T];
			}
		}

		//Prior of the document-topic distribution
		//(This is a mixture of the cluster-topic distributions of the clusters of the document
		double[] topic_prior = new double[T];
		for (int f=0;f<c.F;f++) {
			int g = group[f];
			double sumqfgc_denominator = BasicMath.sum(sumqfgc[f][g]) + grouplength[f]*delta[f];
			for (int i=0;i<grouplength[f];i++) {
				int a= c.A[f][g][i];
				double sumqfck2_denominator = BasicMath.sum(sumqfck[f][a])+ alpha_0;
				//cluster probability in group
				double temp3 = (sumqfgc[f][g][i] + delta[f]) / (sumqfgc_denominator * sumqfck2_denominator);
				for (int k=0;k<T;k++) {
					double temp = 	(sumqfck[f][a][k] + (alpha_0 * pi_0[k])) * temp3;
					double temp4 = temp*featureprior[f];
					topic_prior[k]+=temp4;
					pk_f[k][f]+=temp4;
					pk_fck[f][i][k] = temp4;
				}
			}
		}


		for (int k=0;k<T;k++) {
			pk_f[k]=BasicMath.normalise(pk_f[k]);
		}


		double rhostkt_documentNm = rhostkt_document * c.getN(m);

		int[] termIDs = c.getTermIDs(m);
		short[] termFreqs = c.getTermFreqs(m);

		//Process words of the document
		for (int i=0;i<termIDs.length;i++) {

			//term index
			int t = termIDs[i];
			//How often doas t appear in the document?
			int termfreq = termFreqs[i];


			if (rhot_step>BURNIN) {
				//increase number of words seen in that batch
				batch_words[t]+=termfreq;
			}

			//topic probabilities - q(z)
			double[] q = new double[T];
			//sum for normalisation
			double qsum = 0.0;

			for (int k=0;k<T;k++) {
				//in case the document contains only this word, we do not use nmk
				if (c.getN(m) == termfreq) {
					nmk[m][k] = 0;
				}
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

				if ((Double.isInfinite(q[k]) || q[k]>1 || Double.isNaN(q[k]) || Double.isNaN(nmk[m][k]) ||  Double.isInfinite(nmk[m][k])) && !debug) {
					System.out.println("Error calculating gamma " +
							"first part: " + (nmk[m][k] + alpha_1*topic_prior[k]) + 
							" second part: " + (nkt[k][t] + beta_0) / (nk[k] + beta_0V) + 
							" m " + m+ " " + c.getN(m)+ " " + termfreq + " "+ Math.pow(oneminusrhostkt_document,termfreq) + 
							" sumqmk " + sumqmk[k] + 
							" qk " + q[k] + 
							" nmk " + nmk[m][k] + 
							" prior " + topic_prior[k] + 
							" nkt " + nkt[k][t]+ 
							" a0 " + alpha_0  + 		
							" alpha1 " + alpha_1 +
							" beta " + beta_0 + 
							" betaV " + beta_0V
							);

					for (int f=0;f<c.F;f++) {
						int g = group[f];
						double sumqfgc_denominator = BasicMath.sum(sumqfgc[f]) + grouplength[f]*delta[f];
						System.out.println(" f " + f + " sumqfgc_denominator " + sumqfgc_denominator);
						System.out.println("sumqf[m][f] " + m + " " + f + " " + sumqfgc_denominator + " epsilon " + epsilon[f]);

						for (int h=0;h<grouplength[f];h++) {
							int a= c.A[f][g][h];
							double sumqfck2_denominator = BasicMath.sum(sumqfck[f][a])+ alpha_0;
							System.out.println(" f " + f + " sumqfck2_denominator " + sumqfck2_denominator);

							//cluster probability in group
							System.out.println(" f " + f + " i " + i + " sumqfgc[m][f][i] " + sumqfgc[f][h] + " delta[f] " + delta[f]);

							for (int k2=0;k2<T;k2++) {
								System.out.println(" f " + f + " a "+ a + " k " + k2 + " sumqfck[f][a][k]" + sumqfck[f][a][k] + " pi0[k] " + pi_0[k]);  
							}
						}
					}

					debug = true;
					//Skip this file...
					break;
				}

				//add to batch counts
				if (rhot_step>BURNIN) {
					tempnkt[k][t]+=q[k]*termfreq;
					tempmkt[k][t]+=Math.log(1.0-q[k])*termfreq;
				}

				//update probability of _not_ seeing k in the current document
				sumqmk[k]+=Math.log(1.0-q[k])*termfreq;

				if (c.getN(m) != termfreq) {
					//update document-feature-cluster-topic counts
					if (termfreq==1) {
						nmk[m][k] = (float) (oneminusrhostkt_document * nmk[m][k] + rhostkt_documentNm * q[k]);
					}
					else {
						double temp = Math.pow(oneminusrhostkt_document,termfreq);
						nmk[m][k] = (float) (temp * nmk[m][k] + (1.0-temp) * c.getN(m) * q[k]);
					}
				}
				else {
					nmk[m][k]=(float) (q[k]*termfreq);
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
			updateTopicWordCounts();
		}

		for (int f=0;f<c.F;f++) {
			for (int k=0;k<T;k++) {
				//TODO add feature probability here?
				tempsumqf[f] += topic_ge_0[k] * pk_f[k][f];
			}
		}
		//TODO 2x?
		if (rhot_step>BURNIN_DOCUMENTS) {



			for (int f=0;f<c.F;f++) {

				int g = group[f];
				//increase count for that group
				rhot_group[f][g]++;

				//update feature-counter
				for (int i=0;i<grouplength[f];i++) {
					for (int k=0;k<T;k++) {

						//gives the probability that a table of a topic was drawn from the given cluster
						double topicProbInCluster = pk_fck[f][i][k]/topic_prior[k];

						//p(not_seeing_fik)
						sumqtemp[f][g][i][k] += Math.log(1.0 - (topic_ge_0[k] * topicProbInCluster));
						tempsumqfgc[f][g][i][k]+= topic_ge_0[k] * topicProbInCluster;

						//if ((1.0 - Math.exp(sumqtemp[f][g][i][k])) >= tempsumqfgc[f][g][i][k])
						//	System.out.println((1.0 - Math.exp(sumqtemp[f][g][i][k])) + " " + tempsumqfgc[f][g][i][k] );
					}
				}

				updateClusterTopicDistribution(f,g);	

			}

		}

		//take 10000 samples to estimate alpha_1
		//we have to have at least one run before we estimate alpha!
		if (rhot_step>BURNIN_DOCUMENTS+1) {

			//ignore documents containing only one word.
			if (rhot%SAMPLE_ALPHA == 0 && c.getN(m)>1) {
				alpha_1_nm[alpha_batch_counter] = c.getN(m);
				for (int k=0;k<T;k++) {
					alpha_1_nmk[alpha_batch_counter][k] = nmk[m][k];
					alpha_1_pimk[alpha_batch_counter][k] = topic_prior[k];
				}
				
				alpha_batch_counter++;
				
				if (alpha_batch_counter>=BATCHSIZE_ALPHA) {
<<<<<<< HEAD
					
					alpha_1 = DirichletEstimation.estimateAlphaMap(alpha_1_nmk,alpha_1_nm,alpha_1_pimk,alpha_1,1.0,1.0,20);

					//alpha_1 = DirichletEstimation.estimateAlphaNewton(alpha_1_nm,alpha_1_nmk,alpha_1_pimk,alpha_1,1,1);
=======

					alpha_1 = DirichletEstimation.estimateAlphaNewton(alpha_1_nm,alpha_1_nmk,alpha_1_pimk,alpha_1,1,1);
>>>>>>> bfbdba671a773d228fa7fbbc0c653420c2a6a2e8
					alpha_batch_counter=0;
					
				}
			}
		}

	}

	/**
	 * Here we do stochastic updates of the document-topic counts
	 */
	public synchronized void updateTopicWordCounts() {



		double rhostkt = rho(rhos,rhotau,rhokappa,rhot/BATCHSIZE);
		double rhostktnormC = rhostkt * (c.C / Double.valueOf(BasicMath.sum(batch_words)));

		for (int f=0;f<c.F;f++) {
			//TODO: multiply with avg doc length
			sumqf[f]=(1.0-rhostkt) * sumqf[f] +  rhostkt * tempsumqf[f] * (c.M * TRAINING_SHARE / Double.valueOf(BATCHSIZE));
			tempsumqf[f] = 0;
			featureprior[f] = sumqf[f]+epsilon[f];
		}

		featureprior = BasicMath.normalise(featureprior);




		nk = new float[T];
		for (int k=0;k<T;k++) {
			for (int v=0;v<c.V;v++) {
				double oneminusrhostkt = (1.0 - rhostkt);

				nkt[k][v] *= oneminusrhostkt;

				//update word-topic-tables for estimating tau
				mkt[k][v] *= oneminusrhostkt;
				if(!debug && Double.isInfinite(mkt[k][v])) {
					System.out.println("mkt pre " + mkt[k][v] );
					debug = true;
				}

				//we estimate the topic counts as the average q (tempnkt consists of BATCHSIZE observations)
				//and multiply this with the size of the corpus C
				if (tempnkt[k][v]>0) {

					nkt[k][v] += rhostktnormC * tempnkt[k][v];
					//estimate tables in the topic per word, we just assume that the topic-word assignment is 
					//identical for the other words in the corpus.
					mkt[k][v] += rhostkt * ((1.0-Math.exp(tempmkt[k][v]*(c.C / Double.valueOf(BasicMath.sum(batch_words))))));


					if(!debug &&  (Double.isInfinite(tempmkt[k][v]) || Double.isInfinite(mkt[k][v]))) {
						System.out.println("mkt estimate " + tempmkt[k][v] + " " + mkt[k][v] );
						debug = true;
					}

					//reset batch counts
					tempnkt[k][v] = 0;
					//reset word counts in the last topic iteration
					if (k+1==T) {
						batch_words[v] = 0;
					}
				}

				nk[k] += nkt[k][v];

			}
		}

		//reset
		for (int k=0;k<T;k++) {
			for (int t=0;t<c.V;t++) {
				tempmkt[k][t] = (float) 0.0;
			}
		}

	}

	/**
	 * @param f feature of the group
	 * @param g	group id
	 * 
	 *  Stochastic update of the topic counts for a given group of a feature
	 *  
	 */
	public synchronized void updateClusterTopicDistribution(int f, int g) {
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

				double cluster_sum = 0.0;
				//total documents in cluster - remember that this includes documents from other groups
				int cluster_size = c.Cfc[f][a];
				for (int k=0;k<T;k++) {

					//update table counts for the global topic distribution:
					//-> Probability of seeing topic k once in each cluster?


					//update the probability of seeing a table in the cluster: E(m_{f,c,k} > 0)
					sumqfck_ge0[f][a][k] = oneminusrho*sumqfck_ge0[f][a][k] + rhost_group * (1.0 - Math.exp((sumqtemp[f][g][i][k]*cluster_size)/BATCHSIZE_GROUP_MIN));

					//update counts per cluster
					sumqfck[f][a][k] = oneminusrho*sumqfck[f][a][k] + rhost_group * (cluster_size/Double.valueOf(BATCHSIZE_GROUP_MIN)) * tempsumqfgc[f][g][i][k];

					cluster_sum +=tempsumqfgc[f][g][i][k];

					//We have to reset the batch counts 
					tempsumqfgc[f][g][i][k] = 0;
					sumqtemp[f][g][i][k] = 0;
				}
				sumqfgc[f][g][i]  = oneminusrhostkt_document*sumqfgc[f][g][i] + rhostkt_document * cluster_sum * (cluster_size/Double.valueOf(BATCHSIZE_GROUP_MIN));




			}



			if (rhot_step > BURNIN_DOCUMENTS)  {
				//Update global topic distribution
				updateGlobalTopicDistribution();
			}

			//			Iterator<Integer> it = affected_groups.get(f).get(g).iterator();
			//			while (it.hasNext()) {
			//				int ag = it.next();
			//				estimateGroupTopicDistribution(f,ag);
			//			}
		}
	}

	public void updateGlobalTopicDistribution() {

		//sum over tables
		double[] sumfck = new double[T];

		//Start with pseudo-counts from the Beta prior
		for (int k=0;k<T;k++) {
			bhat[k]=gamma;
		}
		//Now add observed estimated counts

		//sum_cluster_tables = 0;
		for (int f=0;f<c.F;f++) {
			//A[f] holds the cluster indices for each cluster of each feature and thus gives us the 
			//number of clusters per feature by A[f].length
			for (int i=0;i< c.Cf[f];i++) {
				for (int k=0;k<T;k++) {
					//We estimate pi_0 by looking at the documents of each cluster of each feature.

					boolean expected_tables = true;
					double tables;
					if (expected_tables) {
						//NEW:
						//Expected table counts like in Teh, Collapsed Variational Inference for HDP (but with 0-order Taylor approximation)
						double a0pik=alpha_0 * pi_0[k];
						tables = (sumqfck_ge0[f][i][k] > 0) ? a0pik * sumqfck_ge0[f][i][k] * (Gamma.digamma0(a0pik + sumqfck[f][i][k] / sumqfck_ge0[f][i][k]) - Gamma.digamma0(a0pik)) : 0;
					}
					else {
						//Sampled number of tables -> better perplexity
						tables = sumqfck_ge0[f][i][k] * rs.randNumTable(pi_0[k], sumqfck[f][i][k]);
					}
					sumfck[k] += tables;
					//sum_cluster_tables += sumfck[k];
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

		int[] index_reverted = ArrayUtils.reverseIndex(index);

		//bhat is the sum over the counts of all topics > k
		for (int k=0;k<T;k++) {
			int sort_index = index_reverted[k];
			for (int k2=sort_index+1;k2<T;k2++) {
				int sort_index_greater = index[k2];
				bhat[k] += sumfck[sort_index_greater];
			}
		}

		double[] pi_ = new double[T];
		//TODO: 1-pi
		for (int k=0;k<T-1;k++) {
			pi_[k]=ahat[k] / (ahat[k]+bhat[k]);
		}
		for (int k=0;k<T-1;k++) {
			pi_0[k]=pi_[k];
			int sort_index = index_reverted[k];
			for (int l=0;l<sort_index;l++) {
				int sort_index_lower = index[l];
				pi_0[k]*=(1.0-pi_[sort_index_lower]);
			}
		}
		//probability of last pi_0 is the rest
		pi_0[T-1]=0.0;
		pi_0[T-1]=1.0 - BasicMath.sum(pi_0);



		//MAP estimation for gamma (Sato (6))
		double gamma_denominator = 0.0;
		for (int k=0;k<T-1;k++) {
			gamma_denominator += Gamma.digamma0(ahat[k] + bhat[k])- Gamma.digamma0(bhat[k]);
		}

		int a = 1;
		int b = 0;
		gamma = (T + a - 2) / (gamma_denominator + b);


	}

	public void updateHyperParameters() {

<<<<<<< HEAD
		//we have to have at least one run for learning the cluster-specific parameters
		if(rhot_step>BURNIN_DOCUMENTS+2) {
=======
		if(rhot_step>BURNIN_DOCUMENTS) {
>>>>>>> bfbdba671a773d228fa7fbbc0c653420c2a6a2e8

			//			double table_sum = 0;
			//			for (int f=0;f<c.F;f++) {
			//				//A[f] holds the cluster indices for each cluster of each feature and thus gives us the 
			//				//number of clusters per feature by A[f].length
			//				for (int i=0;i< c.Cf[f];i++) {
			//					for (int k=0;k<T;k++) {
			//						//NEW:
			//						//Table counts like in Teh, Collapsed Variational Inference for HDP (but with 0-order Taylor approximation)
			//						double a0pik=alpha_0 * pi_0[k];
			//						table_sum+=a0pik * sumqfck_ge0[f][i][k] * (Gamma.digamma0(a0pik + sumqfck[f][i][k]) - Gamma.digamma0(a0pik));
			//					}
			//				}
			//			}

			//			double[] tables_cluster = new double[BasicMath.sum(c.Cf)];
			//			int j=0;
			//			for (int f=0;f<c.F;f++) {
			//				//A[f] holds the cluster indices for each cluster of each feature and thus gives us the 
			//				//number of clusters per feature by A[f].length
			//				for (int i=0;i< c.Cf[f];i++) {
			//					tables_cluster[j++]= (int) Math.ceil(BasicMath.sum(sumqfck[f][i]));
			//				}
			//			}
			//
			//			//System.out.println(sum_cluster_tables + " " + BasicMath.sum(tables_cluster));
			//RandomSamplers rs = new RandomSamplers();
			//			double[] tables_cluster = new double[BasicMath.sum(c.Cf)];
			//			int j=0;
			//			for (int f=0;f<c.F;f++) {
			//				//A[f] holds the cluster indices for each cluster of each feature and thus gives us the 
			//				//number of clusters per feature by A[f].length
			//				for (int i=0;i< c.Cf[f];i++) {
			//					tables_cluster[j++]= (int) Math.ceil(BasicMath.sum(sumqfck[f][i]));
			//				}
			//			}
			//
			//			//System.out.println(sum_cluster_tables + " " + BasicMath.sum(tables_cluster));
			//RandomSamplers rs = new RandomSamplers();
			//alpha_0 = rs.randConParam(alpha_0, tables_cluster, BasicMath.sum(sumqfck_ge0), 1);

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
					if (!debug && (Double.isInfinite(mkt[k][t] ) || Double.isNaN(mkt[k][t] ))) {
						System.out.println("mkt " + k + " " + t + ": " + mkt[k][t] + " nkt: " +  nkt[k][t]);
						debug = true;
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
					delta[f] = DirichletEstimation.estimateAlphaLikChanging(sumqfgc[f], delta[f], 200);
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
					if (BasicMath.sum(sumqfck[f][i])>0) {
						sumfck[i] = new double[T];
						for (int k=0;k<T;k++) {
							sumfck[j][k] = sumqfck[f][i][k];
							sumfc[j]+=sumqfck[f][i][k];
							//System.out.println(sumfck[j][k]);
						}
						j++;
					}
				}
			}
			
<<<<<<< HEAD
			//TODO: estimate!
		
			alpha_0 = DirichletEstimation.estimateAlphaMap(sumfck, sumfc, pi_0, alpha_0,1,1,20);
=======
			alpha_0 = DirichletEstimation.estimateAlphaNewton(sumfc, sumfck, pi_0, alpha_0,1,1);
>>>>>>> bfbdba671a773d228fa7fbbc0c653420c2a6a2e8


		}


	}


	public double rho (int s,int tau, double kappa, int t) {
		return Double.valueOf(s)/Math.pow((tau + t),kappa);
	}


	public void save () {

		String output_base_folder = c.directory + "output_HMDP/";

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
									doc_topic[me][k]+=pi_kfc_noF[f][g][i][k];
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
									doc_topic[me][k]+=pi_kfc_noF[f][g][i][k];
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
								doc_topic[m][k]+=pi_kfc_noF[f][g][i][k];
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
				double sumqfck2_denominator = BasicMath.sum(sumqfck[f][a]) + alpha_0;
				for (int k=0;k<T;k++) {
					feature_cluster_topics[f][a][k]=(sumqfck[f][a][k] + alpha_0 * pi_0[k]) / sumqfck2_denominator;
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
					sumfck[k]+=a0pik * sumqfck_ge0[f][i][k] * (Gamma.digamma0(a0pik + sumqfck[f][i][k]) - Gamma.digamma0(a0pik));
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
					topic_ge0[k] *= 1.0-sumqfck_ge0[f][i][k];
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
					double sumqfgc_denominator = BasicMath.sum(sumqfgc[f][g]) + c.A[f][g].length*delta[f];
					double temp2 = (sumqf[f] + epsilon[f]);
					for (int i=0;i<grouplength[f];i++) {
						int a= c.A[f][g][i];
						double sumqfck2_denominator = BasicMath.sum(sumqfck[f][a])+ alpha_0;
						//cluster probability in group
						double temp3 = (sumqfgc[f][g][i] + delta[f]) / (sumqfgc_denominator * sumqfck2_denominator);
						for (int k=0;k<T;k++) {
							double temp = 	(sumqfck[f][a][k] + alpha_0 * pi_0[k])	* temp3;
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
