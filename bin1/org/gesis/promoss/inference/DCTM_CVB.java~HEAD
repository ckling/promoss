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

/**
 * This is the practical collapsed stochastic variational inference
 * for the Hierarchical Multi-Dirichlet Process Topic Model (HMDP)
 */
public class DCTM_CVB {

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

	//relative size of the training set
	public double TRAINING_SHARE = 1.0;

	public String save_prefix = "";

	public int K = 100; //Number of topics
	public int K2 = K; //Number of comment topics

	//global prior: G x K 
	public double[][] alpha0;
	//concentration: GxK'
	public double[][] alpha1;
	//global comment prior: G x K'
	public double[][] alpha2;

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
	//Topic counts per document
	private float[][] nmk;
	//mmk table counts for the comments of document m
	private float[][] mmk;
	//table counts per comment: D x Cd x K' x (K + 1)
	private float[][][][] mmik;
	//table counts for the transition matrix: G x K x K'
	private float[][][] mgkk;

	
	//Zc: D x Cd x Nm x K'
	private float[][][][] z2;
	//z: D x Nm x K
	private float[][][] z;

	//counts, how many documents we observed in the batch to estimate alpha
	public int alpha_batch_counter = 0;
	
	public int rhot_step = 0;

	DCTM_CVB() {
		c = new DCTM_Corpus();
	}

	public void initialise () {

		// Folder names, files etc. 
		c.dictfile = c.directory+"words.txt";
		//textfile contains the words of the document, all seperated by space (example line: word1 word2 word3 ... wordNm)
		c.documentfile = c.directory+"corpus.txt";
		//groupfile contains clus


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

		beta_V = beta * c.V;



		c.V = c.dict.length();

		alpha0 = new double[c.G][K];
		alpha1 = new double[c.G][K2];
		alpha2 = new double[c.G][K2];


		beta_V = beta * c.V;

		nk = new float[K];
		nkt = new float[K][c.V];
		nk2 = new float[K];
		nkt2 = new float[K][c.V];
		
		mgkk = new float[c.G][K][K2];
		
		//create initial imbalance
		for (int k=0;k<K;k++) {
			for (int t=0;t<c.V;t++) {
				nkt[k][t]=(float) Math.random();
				nk[k]+=nkt[k][t];
			}			
		}
		

		for (int k2=0;k2<K2;k2++) {
			for (int t=0;t<c.V;t++) {
				nkt2[k2][t]=(float) Math.random();
				nk2[k2]+=nkt[k2][t];
			}			
		}
		
	
		//read corpus size and initialise nkt / nk
		c.readCorpusSize();

		nmk = new float[c.M][];
		
		for (int t=0; t < c.V; t++) {
			for (int k=0;k<K;k++) {

				nkt[k][t]= (float) (Math.random()*INIT_RAND);
				nk[k]+=nkt[k][t];

			}
		}
		
		mmk = new float[c.D][K];
		
		z = new float[c.D][][];
		z2 = new float[c.D][][][];
		mmik = new float[c.D][][][];
		for (int d=0;d<c.D;d++) {
			z2[d] = new float[c.Cd[d]][][];
			mmik[d] = new float[c.Cd[d]][][];
		}
		for (int m=0;m<c.M;m++) {
			int d = c.meta[m][1];
			int cd = c.meta[m][2];
			if (cd > 0) {
				//comment
				nmk[m] = new float[K2];
				z2[d][cd]=new float[c.getN(m)][K2];
				mmik[d][cd] = new float[K2][K+1]; 
			}
			else {
				//document
				nmk[m] = new float[K];
				z[cd] = new float[c.getN(m)][K];
			}
		}
		
		


		System.out.println("Initialising count variables...");

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

			//topic probabilities - q(z)
			double[] q = new double[K];
			//sum for normalisation
			double qsum = 0.0;

			for (int k=0;k<K;k++) {
				if (c.getN(m) == termfreq) {
					nmk[m][k] = 0;
				}
				else {
					nmk[m][k]-=termfreq*z[d][i][k];
					nkt[k][t]-=termfreq*z[d][i][k];
					nk[k]-=termfreq*z[d][i][k];
				}
				
				q[k] = 	//probability of topic given feature & group
						(nmk[m][k] + alpha0[g][k])
						//probability of topic given word w
						* (nkt[k][t] + beta) 
						/ (nk[k] + beta_V);

				qsum+=q[k];
				

			}
			for (int k=0;k<K;k++) {
				q[k]/=qsum;
				nmk[m][k]+=termfreq*q[k];
				nkt[k][t]+=termfreq*q[k];
				nk[k]+=termfreq*q[k];
			}		
			
			
		}

		}
		//Comment inference
		else {
			
			//infer tables
			double[] ge0 = new double[K2];
			for (int k2=0;k2<K2;k2++) {
				ge0[k2]=1;
			}
			
			//index of the commented document
			int document_m = m-ci;
			
			//calculate prior
			double[] prior = alpha2[g];
			//calculate sources of prior
			double[][] prior_sources = new double[K2][K+1];
			//the last index is for the global prior alpha2
			for (int k2=0;k2<K2;k2++) {
				prior_sources[k2][K] = alpha2[g][k2];
			}
			
			double[] theta = new double[K];
			double theta_sum = 0;

			for (int k=0;k<K;k++) {
				theta[k] = alpha1[g][k] * (nmk[document_m][k] + mmk[document_m][k] + alpha0[g][k]);
				theta_sum+=theta[k];
			}
			for (int k=0;k<K;k++) {
				theta[k]/=theta_sum;
				for (int k2=0;k2<K2;k2++) {
					double temp = alpha1[g][k] * theta[k] * (mgkk[g][k][k2]);
					prior[k2]+=temp;
					prior_sources[k2][k] = temp;
				}
			}
			for (int k2=0;k2<K2;k2++) {
				prior_sources[k2] = BasicMath.normalise(prior_sources[k2]);
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
						nmk[m][k2] = 0;
					}
					else {
						nmk[m][k2]-=termfreq*z2[d][ci][i][k2];
						nkt2[k2][t]-=termfreq*z2[d][ci][i][k2];
						nk2[k2]-=termfreq*z2[d][ci][i][k2];
					}


							
					q[k2] = 	//probability of topic given feature & group
							(nmk[m][k2] + prior[k2])
							//probability of topic given word w
							* (nkt[k2][t] + beta) 
							/ (nk[k2] + beta_V);

					qsum+=q[k2];
					

				}
				for (int k2=0;k2<K2;k2++) {
					q[k2]/=qsum;
					nmk[m][k2]+=termfreq*q[k2];
					nkt2[k2][t]+=termfreq*q[k2];
					nk2[k2]+=termfreq*q[k2];
					z2[d][ci][i][k2]=(float) q[k2];
					ge0[k2]*=(1.0-q[k2]);
				}		
				
				
			}
			
			double[] table = new double[K2];
			for (int k2=0;k2<K2;k2++) {
				
				//remove tables
				for (int k=0;k<K;k++) {	
					mmk[d][k]-=mmik[d][ci][k2][k];
					mgkk[g][k][k2]-=mmik[d][ci][k2][k];													
				}
				
				//calculate new expectation of tables as in Teh 06: CVB for DP
				ge0[k2] = 1.0 - ge0[k2];				
				table[k2] = prior[k2] * ge0[k2] * Gamma.digamma(prior[k2] + nmk[m][k2]) - Gamma.digamma(prior[k2]);
				
				for (int k=0;k<K+1;k++) {	
					mmik[d][ci][k2][k]=(float) (prior_sources[k2][k]*table[k2]);
					mmk[d][k]+=mmik[d][ci][k2][k];
					mgkk[g][k][k2]+=mmik[d][ci][k2][k];			
				}
				
				
			}
			
			
			
			
		}


	}


	public void updateHyperParameters() {

		if(rhot_step>BURNIN_DOCUMENTS) {

			
			//TODO filter for documents -> sample! (asymmetric prior)
			alpha0 = DirichletEstimation.estimateAlphaLik(nmk,alpha);
			//TODO get transition matrix, calculate alpha1 based on tables and prior probabilities (asymmetric prior)
			alpha1
			//TODO use tables for estimate (asymmetric prior)
			alpha2


			
			
		}


	}


	public void save () {

		String output_base_folder = c.directory + "output_LDA/";

		File output_base_folder_file = new File(output_base_folder);
		if (!output_base_folder_file.exists()) output_base_folder_file.mkdir();

		String output_folder = output_base_folder + rhot_step + "/";

		File file = new File(output_folder);
		if (!file.exists()) file.mkdir();

		Save save = new Save();
		save.saveVar(nkt, output_folder+save_prefix+"nkt");
		save.close();
		save.saveVar(alpha, output_folder+save_prefix+"alpha");
		save.close();

		//We save the large document-topic file every 10 save steps, together with the perplexity
		if ((rhot_step % (SAVE_STEP *10)) == 0) {

			save.saveVar(perplexity(), output_folder+save_prefix+"perplexity");

		}
		if (rhot_step == RUNS) {

			float[][] doc_topic;
			if (store_empty) {

				//#documents including empty documents
				int Me = c.M + c.empty_documents.size();
				doc_topic = new float[Me][K];
				for (int m=0;m<Me;m++) {
					for (int k=0;k<K;k++) {
						doc_topic[m][k]  = 0;
					}
				}
				int m = 0;
				for (int me=0;me<Me;me++) {
					if (c.empty_documents.contains(me)) {
						doc_topic[me]  = new float[K];
						for (int k=0;k<K;k++) {
							doc_topic[me][k] = (float) (1.0 / K);
						}
					}
					else {				
						doc_topic[me]  = nmk[m];
						doc_topic[me] = BasicMath.normalise(doc_topic[me]);
						m++;
					}
				}

			}
			else {
				doc_topic = new float[c.M][K];
				for (int m=0;m < c.M;m++) {
					for (int k=0;k<K;k++) {
						doc_topic[m][k]  = 0;
					}
				}
				for (int m=0;m < c.M;m++) {
					doc_topic[m]  = nmk[m];
					doc_topic[m] = BasicMath.normalise(doc_topic[m]);						
				}
			}

			save.saveVar(doc_topic, output_folder+save_prefix+"doc_topic");
			save.close();
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

		save.saveVar(
				"\nalpha "+ alpha+
				"\nbeta " + beta +
				"\nrhos "+rhos+
				"\nrhotau "+rhotau+
				"\nrhokappa "+rhokappa+
				"\nBATCHSIZE "+BATCHSIZE+
				"\nBURNIN "+BURNIN+
				"\nBURNIN_DOCUMENTS "+BURNIN_DOCUMENTS+
				"\nMIN_DICT_WORDS "+c.MIN_DICT_WORDS
				,output_folder+save_prefix+"others");


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
			double[][] z = new double[termlength][K];

			//sample for 200 runs
			for (int RUN=0;RUN<runmax;RUN++) {

				//word index
				int n = 0;
				//Process words of the document
				for (int i=0;i<termIDs.length;i++) {

					//term index
					int t = termIDs[i];
					//How often doas t appear in the document?
					int termfreq = termFreqs[i];

					//remove old counts 
					for (int k=0;k<K;k++) {
						nmk[m][k] -= termfreq * z[n][k];
					}

					//topic probabilities - q(z)
					double[] q = new double[K];
					//sum for normalisation
					double qsum = 0.0;

					for (int k=0;k<K;k++) {

						q[k] = 	//probability of topic given feature & group
								(nmk[m][k] + alpha[k])
								//probability of topic given word w
								* (nkt[k][t] + beta) 
								/ (nk[k] + beta_V);


						qsum+=q[k];

					}

					//Normalise gamma (sum=1), update counts and probabilities
					for (int k=0;k<K;k++) {
						//normalise
						q[k]/=qsum;
						z[n][k]=q[k];
						nmk[m][k]+=termfreq*q[k];
					}

					n++;
				}
			}


			int n=0;
			for (int i=0;i<termFreqs.length;i++) {

				//term index
				int t = termIDs[i];
				int termFreq = termFreqs[i];

				double lik = 0;

				for (int k=0;k<K;k++) {
					lik +=   z[n][k] * (nkt[k][t] + beta) / (nk[k] + beta_V);				
				}

				likelihood+=termFreq * Math.log(lik);

				n++;
			}

			for (int k=0;k<K;k++) nmk[m][k] = 0;

		}

		//sampling of topic-word distribution finished - now calculate the likelihood and normalise by totalLength



		double perplexity = Math.exp(- likelihood / Double.valueOf(totalLength));

		System.out.println("Perplexity: " + perplexity);
		
		//get perplexity
		return (perplexity);


	}


}
