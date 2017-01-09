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

	public double[][] alpha0;
	public double[][] alpha1;
	public double[][] alpha2;

	//Dirichlet concentration parameter for topic-word distributions
	public double beta = 0.01;
	public double beta2 = 0.01;

	//helping variable beta*V
	private double beta_V;
	private double beta2_V;

	//Store some zeros for empty documents in the doc_topic matrix?
	public Boolean store_empty = true;

	//Estimated number of times term t appeared in topic k
	public float[][] nkt;
	//Estimated number of words in topic k
	public float[] nk;
	//Estimated number of times term t appeared in topic k
	public float[][] nkt2;
	//Estimated number of words in topic k
	public float[] nk2;
	//Topic counts per document
	public float[][] nmk;
	public float[][] nmk2;
	//table counts per comment
	public float[][] mmk2;
	
	//Zc: D x Cd x Nm x (K x K' + 1)
	private float[][][][][] zc;
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
		alpha1 = new double[c.G];
		alpha2 = new double[c.G][K2];


		beta_V = beta * c.V;

		nk = new float[K];
		nkt = new float[K][c.V];	

		//read corpus size and initialise nkt / nk
		c.readCorpusSize();

		nmk = new float[c.M][K];

		for (int t=0; t < c.V; t++) {
			for (int k=0;k<K;k++) {

				nkt[k][t]= (float) (Math.random()*INIT_RAND);
				nk[k]+=nkt[k][t];

			}
		}
		
		zc = new float[c.D][][][][];
		for (int d=0;d<c.D;d++) {
			zc[d] = new float[c.Cd[d]][][][];
			for (int cd = 0; cd < c.Cd[d]; cd++) {
				zc[d][cd] = new float[c.Nm[m]]][][][];
			}
		}
		for (int m=0;m<c.M;m++) {
			int d = c.meta[m][1];
			int cd = c.meta[m][2];
			if (cd > 0) {
				zc[d][cd]=new float[c.getN(m)][K+1][K2];
//TODO: change dimensionality of alpha2 in plate
			}
		}
		
		
		z = new float[c.D][K];

		System.out.println("Initialising count variables...");

	}




	public void inferenceDoc(int m) {

		int[] termIDs = c.getTermIDs(m);
		short[] termFreqs = c.getTermFreqs(m);
		
		int g = c.meta[m][0];
		int d = c.meta[m][1];
		int commentID = c.meta[m][2];
		
		//Normal docs
		if (commentID == 0) {

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
					nmk[m][k]-=z[d][i][k];
				}
				
				q[k] = 	//probability of topic given feature & group
						(nmk[m][k] + alpha0[g][k])
						//probability of topic given word w
						* (nkt[k][t] + beta) 
						/ (nk[k] + beta_V);

				qsum+=q[k];

			}
		}

		}


	}


	public void updateHyperParameters() {

		if(rhot_step>BURNIN_DOCUMENTS) {

			

			alpha = DirichletEstimation.estimateAlphaLik(nmk,alpha);


			
			
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
