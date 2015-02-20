package ckling.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import jgibblda.Dictionary;
import jgibblda.Pair;

import ckling.math.BasicMath;
import ckling.math.Save;
import ckling.text.Text;

public class PracticalInference {

	//We have a debugging mode for checking the parameters
	public static boolean debug = false;

	//number of top words returned for the topic file
	public static int topk = 25;

	//Number of read docs (might repeat with the same docs)
	public static int RUNS = 100000;

	public static int BATCHSIZE = 100;

	public static int BATCHSIZE_CLUSTERS = 100;

	//Burn in phase: how long to wait till updating nkt?
	public static int BURNIN = 0;


	private static String basedirectory = "/home/c/work/topicmodels/test/";

	public static int M = 0; //Number of Documents
	public static int C = 0; //Number of words in the corpus
	public static int V = 0; //Number of distinct words in the corpus, read from dictfile
	public static int F = 0; //Number of features
	public static int[] Cf; //Number of clusters for each feature
	public static int[][] Cfc; //Number of documents for each cluster (FxCf)
	public static int[][] Cfg; //Number of documents for each group (FxCg)
	public static int[] N; //Number of Words per document
	public static int T = 6; //Number of truncated topics


	public static int[][][] A; //Groups and their clusters

	public static double alpha_0 = 1;

	public static double alpha_1 = 1;

	//Dirichlet parameter for multinomials over clusters

	public static double delta = 1;

	//Dirichlet parameter for multinomial over features
	public static double epsilon = 1;

	public static double gamma = 1;

	//Dirichlet concentration parameter for topic-word distributions
	public static double beta0 = 0.1;
	//Word-specific weight for the dirichlet prior beta, length: V (number of Words)
	public static double[] tau;

	//Global topic weight (estimated)
	public static double[] pi0;

	public static String dictfile;
	public static String documentfile;
	public static String groupfile;

	public static Dictionary dict;

	private static Text documentText;

	//sum of counts over all documents for cluster c in feature f for topic k
	public static double[][][] nkfc;
	//Estimated number of times term t appeared in topic k
	public static double[][] nkt;
	//Estimated number of times term t appeared in topic k in the batch
	public static double[][] tempnkt;
	//Estimated number of words in topic k
	public static double[] nk;
	//Total number of words in the corpus
	public static int n = 0;

	//Topic/feature/cluster "counts" per document
	public static double[][][][] nmkfc;
	//Topic-table "counts" for each document per cluster
	public static double[][][][] tmkfc;

	//variational parameters for the stick breaking process - parameters for Beta distributions \hat{a} and \hat{b}
	private static double[] ahat;
	private static double[] bhat;


	//rho: Learning rate; rho = s / ((tau + t)^kappa);
	//recommended values: See "Online learning for latent dirichlet allocation" paper by Hoffman
	//tau = 64, K = 0.5; S = 4096
	//rho for document-f-c-topic updates
	private static double rhod = 0;
	//rho for topic-word count updates
	private static double rhoc = 0;
	//rho for fc-topic count updates
	private static double rhofh = 0;
	//rho for global-topic count updates
	private static double rhoh = 0;

	private static double rhokappa = 0.9;
	private static int rhos = 10;
	private static int rhotau = 1000;
	//tells the number of processed words
	private static int rhot = 0;
	//tells the step for each document (the number of the current run)
	private static int rhot_document = 0;
	
	//count number of words seen in the batch
	//remember that rhot counts the number of documents, not words
	private static int batch_words = 0;
	//count number of times the group was seen in the batch - FxG
	private static int[][] rhot_group;

	/*
	 * Here we define helper variables
	 * Every feature has clusters
	 * Clusters belong to groups of connected clusters (e.g. adjacent clusters).
	 */

	//Sum over log(1-q(k,f,c)). We need this sum for calculating E[n>0] and E[n=0] 
	private static double[][][] sumqkfc;
	//Sum over q(k,f,c). We need this sum for calculating SUM(n>0) 
	private static double[][][] sumqkfc2;
	//Sum over log(1-q(k,f,c)) for all documents in the given group g and cluster number c. We need this sum for calculating 
	//E[n>0] and E[n=0] for all documents of the group. 
	private static double[][][] sumqfgc;
	//The same but summed over all clusters of the group
	private static double[][] sumqfg;
	//The same but summed over all clusters of the group
	private static double[][] sumqfgtemp;

	//Sum over log(1-q(_,f,_)) for feature f (approximated table counts)
	private static double[] sumqf;
	//Sum over log(1-q(_,f,_)) for feature f - for batch updates
	private static double[] sumqftemp;

	//Sum over log(1-q(_,_,_))
	private static double sumq;
	//Counter: how many observations do we have per cluster? Dimension: F x |C[f]|
	//We use this for doing batch updates of the cluster parameters and to calculate
	//the update rate \rho
	private static int[][] rhot_cluster;


	//statistic over gamma, used to do batch-updates
	private static double[][][] tempsumq;


	//Helper variable: word frequencies
	public static int[] wordfreq;

	//document-words (M x doclength)
	private static Set<Entry<Integer, Integer>>[] wordsets;
	//document groups (M x F)
	private static int[][] groups;

	private static double[][][] q;

	
	@SuppressWarnings("unchecked")//for ArrayList type conversion
	public static void main (String[] args) {

		readSettings();

		System.out.println("Reading dictionary...");
		readDict();		

		System.out.println("Initialise parameters...");
		getParameters();

		System.out.println("Processing documents...");

		readDocs();

		System.out.println("Estimating topics...");

		for (int i=0;i<RUNS;i++) {
			rhot_document++;
			for (int m=0;m<M;m++) {
				System.out.println("Inference " + m);
				inferenceDoc(m);
				//TODO: Create parameter: How often should pi_0 be updated?
				System.out.println("Inference Global");
				inferenceGlobal();
			}
		}

		//store inferred variables
		System.out.println("Storing variables...");
		save();

		//inferenceDoc(null, null); //do update parameters using the new document

	}

	private static void readSettings() {
		// Folder names, files etc. - TODO should be set via args later
		dictfile = basedirectory+"words.txt";
		//textfile contains the group-IDs for each feature dimension of the document
		//and the words of the document, all seperated by space (example line: a1,a2,a3,...,aF word1 word2 word3 ... wordNm)
		documentfile = basedirectory+"texts.txt";
		//groupfile contains cluster-IDs for each group.separated by space
		groupfile = basedirectory+"groups.txt";
	}


	private static void readCorpusSize() {
		// Read dictionary from file
		// The file contains words, one in each row

		wordfreq = new int[V];
		Text dictText = new Text();

		String line;
		while((line = dictText.readLine(documentfile)) != null) {
			M++;
			//System.out.println(line);

			String[] lineSplit = line.split(" ");

			String groupString = lineSplit[0];
			String[] groupSplit = groupString.split(",");
			for (int f=0;f<F;f++) {
				int g = Integer.valueOf(groupSplit[f]);
				Cfg[f][g]++;
				int[] clusters = A[f][g];
				for (int c=0;c<clusters.length;c++) {
					Cfc[f][c]++;
				}
			}

			for (int i=1;i<lineSplit.length;i++) {
				if (dict.contains(lineSplit[i])) {
					int wordid = dict.getID(lineSplit[i]);
					wordfreq[Integer.valueOf(wordid)]++;
					C++;
				}
			}

		}

	}

	private static void readDict() {
		// Read dictionary from file
		// The file contains words, one in each row

		dict = new Dictionary();
		Text dictText = new Text();
		String line;
		while((line = dictText.readLine(dictfile)) != null) {

			dict.addWord(line);

		}

	}

	@SuppressWarnings("unchecked")
	private static void readGroups() {
		//initialise if not yet done
		if (A==null) {
			A = new int[F][][];
			for (int f = 0; f < F; f++) {
				A[f]=new int[0][];
			}
		}

		//initialise variable which stores the number of clusters for each feature
		Cf = new int[F];

		//get text of the groupfile
		Text grouptext = new Text();
		String line;
		while ((line = grouptext.readLine(groupfile))!= null) {

			//System.out.println("Line: "+line);

			String[] lineSplit = line.split(" ");

			int f = Integer.valueOf(lineSplit[0]);
			int groupID = Integer.valueOf(lineSplit[1]);

			int[] cluster = new int[lineSplit.length - 2];
			for (int i=2;i<lineSplit.length;i++) {
				cluster[i-2] = Integer.valueOf(lineSplit[i]);
				//System.out.println("test: "+cluster[i-2]);
				//Find out about the maximum cluster ID. The number of clusters is this ID +1
				Cf[f] = Math.max(Cf[f],cluster[i-2] + 1);
			}
			//System.out.println("test ending");

			//System.out.println(f + " " + groupID);
			if(A[f].length - 1 < groupID) {
				int[][] Afold = A[f];
				A[f] = new int[groupID+1][];
				System.arraycopy(Afold, 0, A[f], 0, Afold.length);
			}
			//System.out.println(cluster.length);
			A[f][groupID] = cluster;			
		}

		grouptext.close();

		//fill undefined groups with null
		//TODO: we have to add a mechanism for adding new groups...
		for (int f=0;f<F;f++) {
			for (int g=0;g<A[f].length;g++) {
				if (A[f][g] == null) {
					A[f][g] = new int[0];
				}
			}
		}


	}

	//set Parameters
	private static void getParameters() {
		// TODO Auto-generated method stub
		readFfromTextfile();
		q = new double[T][F][];
		
		System.out.println("Reading groups...");

		readGroups(); //if there is an unseen Group mentioned

		V = dict.length();

		alpha_0 = 0.5;
		alpha_1 = 0.5;
		delta = 1;
		epsilon = 1;
		gamma = 1;
		beta0 = 0.1*V;
		//T = 2;

		nk = new double[T];
		nkt = new double[T][V];	
		tempnkt = new double[T][V];	

		//count the number of documents in each group
		Cfg = new int[F][];
		for (int f=0;f<F;f++) {
			Cfg[f]=new int[A[f].length];
		}
		//count the number of documents in each cluster
		Cfc=new int[F][];
		for (int f=0;f<F;f++) {
			Cfc[f]=new int[Cf[f]];
		}
		
		rhot_group = new int[F][];
		for (int f=0;f<F;f++) {
			rhot_group[f]=new int[A[f].length];
		}
		
		//read corpus size and initialise nkt / nk
		readCorpusSize();
		
		N = new int[M];

		nmkfc = new double[M][][][];

		groups = new int[M][F];
		wordsets = new Set[M];
		

		for (int k=0;k<T;k++) {
			for (int t=0; t < V; t++) {
				nkt[k][t] += wordfreq[t]/Double.valueOf(T) + (0.5-Math.random()) * wordfreq[t]/Double.valueOf(T) *0.1;
				nk[k]+=nkt[k][t];
			}
		}


		tau = new double[V];
		for (int i=0;i<V;i++) {
			tau[i]=1.0/(double)V;
		}

		pi0 = new double[T];
		ahat = new double[T];
		bhat = new double[T];

		for (int i=0;i<T;i++) {
			pi0[i]=1.0/T;
		}

		System.out.println("Initialising count variables...");

		sumqkfc = new double[T][F][];
		rhot_cluster = new int[F][];
		for (int f=0;f<F;f++) {
			rhot_cluster[f] = new int[Cf[f]];
		}
		sumqkfc2 = new double[T][F][];
		tempsumq = new double[T][F][];
		for (int k=0;k<T;k++) {
			for (int f=0;f<F;f++) {
				sumqkfc[k][f] = new double[Cf[f]];
				sumqkfc2[k][f] = new double[Cf[f]];
				tempsumq[k][f] = new double[Cf[f]];
			}
		}

		sumqfgc = new double[F][][];
		sumqfg = new double[F][];
		sumqf = new double[F];
		for (int f=0;f<F;f++) {
			sumqfgc[f] = new double[A[f].length][];
			sumqfg[f] = new double[A[f].length];
			for (int a=0;a<A[f].length;a++) {
				//System.out.println(A[f].length + " " + a + " " + A[f][a]);
				sumqfgc[f][a] = new double[A[f][a].length];
			}
		}

		

	}


	/**
	 * Reads the number of features F from by counting the
	 * number of groups in the first line of the textfile
	 */
	private static void readFfromTextfile() {
		// TODO Auto-generated method stub
		String firstLine = Text.readLineStatic(documentfile);
		//File e.g. looks like groupID1,groupID2,groupID3,groupID4 word1 word2 word3
		F = firstLine.split(" ")[0].split(",").length;
	}

	private static boolean readDocs() {

		HashMap<Integer,Integer> distinctWords = new HashMap<Integer, Integer>();
		if (documentText == null) {
			documentText = new Text();
			documentText.setLang("en");
			documentText.setStopwords(false);
			documentText.setStem(false);
		}
		String line = ""; 
		int m=0;
		while ((line = documentText.readLine(documentfile))!=null) {

			//System.out.println(line);
			String[] docSplit = line.split(" ",2);
			String[] groupString = docSplit[0].split(",");

			int[] group = new int[F];
			for (int f=0; f<F; f++) {
				group[f] = Integer.valueOf(groupString[f]);
			}

			if (docSplit.length>1) {
				documentText.setText(docSplit[1]);
				Iterator<String> words = documentText.getTerms();

				while(words.hasNext()) {
					
					N[m]++;

					String word = words.next();
					if (dict.contains(word)) {
						int wordID = dict.getID(word);
						if (distinctWords.containsKey(wordID)) {
							int count = distinctWords.get(wordID);
							distinctWords.put(wordID, count+1);
						}
						else {
							distinctWords.put(wordID, 1);
						}
					}
				}

				Set<Entry<Integer, Integer>> wordset = distinctWords.entrySet();

				if (m%100 == 0)
					System.out.println("Reading " + m);

				//inferenceDoc(wordset,group,linecount++);
				wordsets[m]=wordset;
				groups[m]=group;
				m++;
			}


		}
		return true;



	}

	private static void inferenceDoc(int m) {

		//increase counter of documents seen
		rhot++;
		Set<Entry<Integer, Integer>> wordset = wordsets[m];
		int[] group = groups[m];

		//Helping variable: sum log(1-qkfc) for this document, (don't mix with sumqkfc, which is the global count variable!)
		//Tells the expected total number of times topic k was _not_ seen for feature f in cluster c in the currect document
		double[][][] sumqmkfc = new double[T][F][];
		//Expectation E[nmkfc > 0]
		double[][][] enmkfcg0 = new double[T][F][];
		for (int k=0; k<T; k++) {
			for (int f=0;f<F;f++) {
				sumqmkfc[k][f]=new double[A[f][group[f]].length];
				for (int i=0;i<A[f][group[f]].length;i++) {
					sumqmkfc[k][f][i]=1.0;
				}
			}
		}



		
		//this is not necessary
		//double[][][] gamma = new double[T][F][];


		//For the first run...
		if (rhot_document == 1) {
			
			nmkfc[m] = new double[T][F][];
			for (int k=0;k<T;k++) {
				for (int f=0;f<F;f++) {
					nmkfc[m][k][f] = new double[A[f].length];
				}
			}
			
			//set initial random topic weights
			double[] initrandk = new double[T];
			for (int i=0;i<initrandk.length;i++) {
				//we use a random number between 0 and 1 and add the DP parameter alpha_0 for smoothing
				initrandk[i] = Math.random()+alpha_0;
			}
			//normalisation
			initrandk = BasicMath.normalise(initrandk);

			//set initial random feature weights
			double[] initrandf = new double[F];
			for (int i=0;i<initrandf.length;i++) {
				//we use a random number between 0 and 1 and add the DP parameter alpha_0 for smoothing
				initrandf[i] = Math.random()+alpha_0;
			}
			//normalisation
			initrandf = BasicMath.normalise(initrandf);

			for (int k=0;k<T;k++) {
				for (int f=0;f<F;f++) {
					for (int i=0;i<nmkfc[m][k][f].length;i++) {
						//This just assigns equal weight to every topic/feature/cluster  for now, non-optimal solution
						//TODO: sample numbers from cluster-prior instead!
						nmkfc[m][k][f][i] = 1.0 / F * 1.0/A[f][group[f]].length * 1.0/T;
					}
				}
			}
		}

		//learning rate for the document - based on the current step (how often did we process the document?)
		double rhostkt_document = rho(rhos,rhotau,rhokappa,rhot_document);
		double oneminusrhostkt_document = (1.0 - rhostkt_document);
		double rhostkt_documentNm = rhostkt_document * N[m];

		//get cluster-specific topic distributions
		//we do not re-estimate them after every word as we do not expect big changes
		double[][][] pi_kfc = new double[T][F][];

		for (int k=0;k<T;k++) {

			for (int f=0;f<F;f++) {

				int grouplength = A[f][group[f]].length;

				pi_kfc[k][f] = new double[grouplength];

				for (int c=0;c<grouplength;c++) {

					int g = group[f];
					int a=A[f][g][c];

					//here we do things once which do not depend on topic k
					if (k==0) {
						//increase counter of seen documents for that cluster
						rhot_cluster[f][a]++;
						rhot_group[f][g]++;
					}

					pi_kfc[k][f][c] = 				
							//Topic probability for this group
							//TODO: Check denominator
							(sumqkfc2[k][f][a] + alpha_0 * pi0[k]) / (sumqfg[f][group[f]] + alpha_0)
							//cluster probability in group
							* (sumqfgc[f][group[f]][c] + delta) 
							/ (sumqfg[f][group[f]] + grouplength*delta)
							//feature probability
							* (sumqf[f] + epsilon) / (sumq + F * epsilon);

				}

			}

		}

		//get words in random order
		//Collections.shuffle(wordset);

		for (Entry<Integer,Integer> e : wordset) {

;
			int t = e.getKey();
			int termfreq = e.getValue();

			//increase number of words seen in that batch
			batch_words+=termfreq;
			n+=termfreq;

			if(debug)
				System.out.println(t);

			//sum of gamma, for normalisation
			double qsum = 0.0;



			for (int k=0;k<T;k++) {

				//TODO: calculate estimate using ahat and bhat
				pi0[k]=1.0/T;

				for (int f=0;f<F;f++) {

					//Number of clusters of the group
					int grouplength = A[f][group[f]].length;

					q[k][f] = new double[grouplength];
					for (int c=0;c<grouplength;c++) {

						//TODO: sumqfg is a sum of the exp not the exp

						//a is the global cluster index, do not mix with c which just counts the clusters of the group of the document
						int a=A[f][group[f]][c];
						//Remember that c is the cth cluster for document m -- the index is stored in a 
						//prodqmkfc gives the expected probability that we did _not_ see topic k in feature f, cluster c
						//						gamma[k][f][c] = 
						//								(1.0 - Math.exp(sumqmkfc[k][f][c])) * nmkfc[m][k][f][c]
						//										+ ((Math.exp(sumqmkfc[k][f][c]) * (1.0 - Math.exp(sumqkfc[k][f][a]))*alpha_1) 
						//												* (sumqkfc2[k][f][a]) / (alpha_0 + (sumqkfc2[k][f][a]))
						//												+ (Math.exp(sumqkfc[k][f][a])) * alpha_1 * alpha_0 * pi0[k])
						//												* (sumqfgc[f][group[f]][c] + delta) 
						//												/ (sumqfg[f][group[f]] + grouplength*delta)
						//												* (sumqf[f] + epsilon) 
						//												/ (sumq + F * epsilon)
						//												* (nkt[k][t] + beta0*tau[t]) / (nk[k] + beta0);



						//System.out.println(nmkfc[k][f][c]);

						q[k][f][c] = 
								//probability of topic given feature & group
								(nmkfc[m][k][f][c] + alpha_1 * pi_kfc[k][f][c])
								//probability of topic given word w
								* (nkt[k][t] + beta0*tau[t]) / (nk[k] + beta0);

						qsum+=q[k][f][c];

						//Extensive debugging goes here
						if (debug) {

							boolean weirdResult = true;
							if (Double.isNaN(q[k][f][c]) || q[k][f][c] <= 0.0) weirdResult = true;

							if (weirdResult) {
								System.out.println("Gamma:"+q[k][f][c]);
								System.out.println("Part 1:"+(1.0 - sumqmkfc[k][f][c]) * nmkfc[m][k][f][c]);
								System.out.println("Part 2:"+(Math.exp(sumqmkfc[k][f][c]) * (1.0 - Math.exp(sumqkfc[k][f][a]))*alpha_1) 
										* (sumqkfc2[k][f][a]) / (alpha_0 + (sumqkfc2[k][f][a])));
								System.out.println("Part 3:"+(Math.exp(sumqkfc[k][f][a])) * alpha_1 * alpha_0 * pi0[k]);
								System.out.println("Part 4:"+ (sumqfgc[f][group[f]][c] + delta) 
										/ (sumqfg[f][group[f]] + grouplength*delta)
										* (sumqf[f] + epsilon) 
										/ (sumq + F * epsilon));
								System.out.println("Part 5:"+(nkt[k][t] + beta0*tau[t]) / (nk[k] + beta0));

								//This is because of a negative gamma
								System.out.println("Detail 1:" + nmkfc[m][k][f][c] );
								System.out.println("Detail 2:" + Math.exp(sumqmkfc[k][f][c]));

							}

						}


					}
				}
			}

			//Normalise gamma (sum=1), update counts and probabilities




			for (int k=0;k<T;k++) {
				for (int f=0;f<F;f++) {
					//System.out.println("test " + f + " " + group[f] + " " + A[f][group[f]].length);

					//Number of clusters of the group
					int grouplength = A[f][group[f]].length;
					for (int c=0;c<grouplength;c++) {
						//a is the global cluster index, do not mix with c which just counts the clusters of the document
						//int a=A[f][group[f]][c];
						//normalise
						q[k][f][c]/=qsum;

						//add to batch counts
						if (rhot_document>BURNIN) {
							tempnkt[k][t]+=q[k][f][c]*termfreq;
						}

						//update probability of _not_ seeing kfc in the current document
						if (termfreq>1) {
							sumqmkfc[k][f][c]*=Math.pow(1.0-q[k][f][c],termfreq);
						}
						else {
							sumqmkfc[k][f][c]*=1.0-q[k][f][c];
						}

						//update document-feature-cluster-topic counts
						if (termfreq==1) {
							nmkfc[m][k][f][c] = oneminusrhostkt_document * nmkfc[m][k][f][c] + rhostkt_documentNm * q[k][f][c];
						}
						else {
							double temp = Math.pow(oneminusrhostkt_document,termfreq);
							nmkfc[m][k][f][c] = temp * nmkfc[m][k][f][c] + (1.0-temp) * N[m] * q[k][f][c];
						}

						//System.out.println(nmkfc[m][k][f][c]);

					}
				}

			}
			
			
		}






		//after we processed all terms, update global counts
		
		
		
		//We update global topic-word counts in batches (mini-batches lead to local optima)
		if (rhot%BATCHSIZE == 0) {

			//only update if we are beyond burnin
			if (rhot_document>BURNIN) {
				
				System.out.println("Updating topics... (Step"+rhot+")");

				//learning rate for the topics
				double rhostkt = rho(rhos,rhotau,rhokappa,rhot);
				double rhostktnormC =  rhostkt / Double.valueOf(batch_words) * C;

				for (int k=0;k<T;k++) {
				for (int v=0;v<V;v++) {
					//update topic-word counts
					nk[k] -= nkt[k][v];
					nkt[k][v] *= (1.0 - rhostkt);

					//we estimate the topic counts as the average q (tempnkt consists of BATCHSIZE observations)
					//and multiply this with the size of the corpus C
					if (tempnkt[k][v]>0) {
						//System.out.println("Topic-word " +k + " " + v + " " + tempnkt[k][v]);
						nkt[k][v] += rhostktnormC * tempnkt[k][v];
						//reset
						tempnkt[k][v] = 0;
					}
					nk[k] += nkt[k][v];
				}
				}
				
				//update feature-table counts
				//we re-use the learning rate for topics for now
				double rhostktnormC_batch = rhostkt / BATCHSIZE * C;
				for (int f=0;f<F;f++) {
					sumqf[f]=(1.0-rhostkt) * sumqf[f] +  rhostktnormC_batch * sumqftemp[f];
					sumqftemp[f] = 0;
				}

				//TODO: use tempsumq to update the cluster-topic distributions

				batch_words = 0;
			}

			//System.out.println("after " + nkt[k][t]);


		}
		
		

		//probability of  topics and clusters for feature f
		double[] sumfeature = new double[F];
		for (int k=0;k<T;k++) {
			//sum gamma over features and cluster for each topic k 
			for (int f=0;f<F;f++) {
				//Number of clusters of the group
				int grouplength = A[f][group[f]].length;
				for (int c=0;c<grouplength;c++) {
					int a=A[f][group[f]][c];
					//counts of expectation of seeing f in a document
					sumfeature[f]+=1.0-sumqmkfc[k][f][c];
					//Update global probability of seeing topic k in feature f and cluster c
					//sumqkfc[k][f][a] += 1.0-sumqmkfc[k][f][c];
					//Update global _counts_ of seeing topic k in feature f and cluster c
					//OLD: sumqkfc2[k][f][a] += 1.0-sumqmkfc[k][f][c];
					
					//how often did we see this cluster already?
					if (k==0) {
						rhot_cluster[f][a]++;
					}
					tempsumq[k][f][a]+= 1.0-sumqmkfc[k][f][c];
					//Update global _count_ of seeing cluster c of the group of the document for feature f
					//Should we do batch updates for this, too? yes!
					tempsumqfgc[f][group[f]][c] += 1.0-sumqmkfc[k][f][c];
				}
			}
		}

		//Stochastic cluster updates: tmkfc unkown (tables!)
		//-> get table counts per cluster (or estimate it)
		//Stochastic group updates: tmkfg unknown (tables in group, tells how often cluster X was chosen in group g)
		for (int f=0;f<F;f++) {
			
			int g = group[f];
			if (rhot_group[f][g] % BATCHSIZE_GROUPS == 0) {
				
				for (int i=0;i<A[f][g].length;i++) {
					
					sumqfgc[f][g][i] = oneminusrho*sumqfgc[f][g][i] + rho * Cfg[f][g] * (tempsumqfgc[f][g][i]/BATCHSIZE_GROUPS);

				}				
			}
			
			for (int i=0;i<A[f][g].length;i++) {
				int a = A[f][g][i];
					

				if (rhot_cluster[f][a] % BATCHSIZE_CLUSTERS == 0) {
					//reset batch cluster-topic counts if batch was processed

					//TODD get rhot_cluster and update the number of customers in the restaurant
					//table count
					//sumqkfc2[k][f][c] =


					double rhostk_cluster = rho(rhos,rhotau,rhokappa,rhot_cluster[f][a]);

					for (int k=0;k<T;k++) {

						sumqkfc2[k][f][a] = (1.0 - rhostk_cluster) *  sumqkfc2[k][f][a] + 
								rhostk_cluster * 
								//Average expected table count per topic and document
								(tempsumq[k][f][a] / BATCHSIZE_CLUSTERS) 
								* Cfc[f][a];

					}


					tempsumq = new double[T][F][];
					for (int k=0;k<T;k++) {
						//TODO check if we really do not want to reset those...?
						//sumqkfc[k][f] = new double[Cf[f]];
						//sumqkfc2[k][f] = new double[Cf[f]];
						tempsumq[k][f][a] = 0;
					}
				}

			}
		}
		



		for (int f=0;f<F;f++) {

			//Sum over log(1-q(k,f,c)) for all documents in the given group. We need this sum for calculating 
			//E[n>0] and E[n=0] for all clusters of the group.
			//We only loop over f as the document belongs only to one group per feature
			sumqfgtemp[f][group[f]] += sumfeature[f];
			sumqftemp[f]+=sumfeature[f];

		}
		sumq+=BasicMath.sum(sumfeature);





	}

	private static void inferenceGlobal() {

		double[] sumfck = new double[T];
		//save sums for ahat and bhat for normalisation
		double sumahat;
		double sumbhat;

		for (int k=0;k<T;k++) {
			//Start with pseudo-counts from the Beta prior
			ahat[k] = 1.0;
			bhat[k]=gamma;
			//Now add observed estimated counts

			for (int f=0;f<F;f++) {
				for (int a=0;a<Cf[f];a++) {
					//We estimate pi_0 by looking at the documents of each cluster of each feature.
					//For each cluster, we calculate the probability that we saw topic k in one of its documents.
					//We then calculate the expected number of clusters where we saw topic k.
					sumfck[k]+= sumqkfc[k][f][a];
				}
			}

			ahat[k] += sumfck[k];

		}

		//bhat is the sum over the counts of all topics > k
		for (int k=0;k<T;k++) {
			for (int k2=k+1;k2<T;k2++) {
				bhat[k] += sumfck[k2];
			}
		}

		for (int k=0;k<T;k++) {
			pi0[k]=ahat[k] / (ahat[k]+bhat[k]);
			for (int l=0;l<k-1;l++) {
				pi0[k]*=bhat[l] / (ahat[l]+bhat[l]);
			}
		}


	}


	public static double rho (int s,int tau, double kappa, int t) {

		//System.out.println(Double.valueOf(s)/Math.pow((tau+t),kappa));
		return Double.valueOf(s)/Math.pow((tau+t),kappa);

	}


	public static void save () {

		Save save = new Save();
		save.saveVar(nkt, basedirectory+"nkt");
		save.close();
		save.saveVar(pi0, basedirectory+"pi0");
		save.close();
		for (int f=0; f<F;f++) {
			save.saveVar(sumqf[f], basedirectory+"sumqf"+f);
			save.close();
		}

		if (topk > V) {
			topk = V;
		}


		String[][] topktopics = new String[T][topk];

		for (int k=0;k<T;k++) {

			List<Pair> wordprob = new ArrayList<Pair>(); 
			for (int v = 0; v < V; v++){
				wordprob.add(new Pair(dict.getWord(v), nkt[k][v]/nk[k], false));
			}
			Collections.sort(wordprob);

			for (int i=0;i<topk;i++) {
				topktopics[k][i] = (String) wordprob.get(i).first;
			}

		}
		save.saveVar(topktopics, basedirectory+"topktopics");


	}



}
