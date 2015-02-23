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

	public static int BATCHSIZE_GROUPS = 100;

	//Burn in phase: how long to wait till updating nkt?
	public static int BURNIN = 0;


	private static String basedirectory = "/home/c/work/topicmodels/wiki/";

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
	public static double[][][][] nmfck;
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
	private static double[][][] sumqfck;
	//Sum table counts for a given cluster; F x Cf x T
	private static double[][][] sumqkfc2;
	//Sum of sumqkfc2[f][c][k] for all k; F x Cf
	private static double[][] sumqkfc2_denominator;
	//Sum over log(1-q(k,f,c)) for all documents in the given group g and cluster number c. We need this sum for calculating 
	//Sum of E[n>0] and E[n=0] for all documents of the group and cluster. 
	private static double[][][] sumqfgc;
	//E[n>0] and E[n=0] for all documents of the group. 
	private static double[][] sumqfgctemp;
	//The same but summed over all clusters of the group
	private static double[][] sumqfg;

	//Sum over log(1-q(_,f,_)) for feature f (approximated table counts)
	private static double[] sumqf;
	//Batch estimate of sumqf for stochastic updates
	private static double[] sumqftemp;


	//Sum over log(1-q(_,_,_))
	private static double sumq;
	//Counter: how many observations do we have per cluster? Dimension: F x |C[f]|
	//We use this for doing batch updates of the cluster parameters and to calculate
	//the update rate \rho
	private static int[][] rhot_cluster;


	//statistic over gamma, used to do batch-updates of clusters
	private static double[][][][] sumqtemp;


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
				inferenceGlobalTopicDistribution();
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
		q = new double[F][][];

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

		nmfck = new double[M][][][];

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

		sumqfck = new double[F][][];
		rhot_cluster = new int[F][];
		for (int f=0;f<F;f++) {
			rhot_cluster[f] = new int[Cf[f]];
		}
		sumqkfc2 = new double[F][][];
		sumqkfc2_denominator = new double[F][];
		sumqtemp = new double[F][][][];
		for (int f=0;f<F;f++) {
			sumqfck[f] = new double[Cf[f]][T];
			sumqkfc2[f] = new double[Cf[f]][T];
			sumqkfc2_denominator[f] = new double[Cf[f]];
			sumqtemp[f] = new double[A[f].length][][];
			for (int g=0;g<A[f].length;g++) {
				sumqtemp[f][g]=new double[A[f][g].length][T];
			}
		}
		

		sumqfgc = new double[F][][];
		sumqfg = new double[F][];
		sumqf = new double[F];
		sumqftemp = new double[F];
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
		double[][][] sumqmfck = new double[F][][];

		for (int f=0;f<F;f++) {
			sumqmfck[f]=new double[A[f][group[f]].length][T];
			for (int i=0;i<A[f][group[f]].length;i++) {
				for (int k=0;k<T;k++) {
					sumqmfck[f][i][k]=1.0;
				}
			}
		}





		//For the first run...
		if (rhot_document == 1) {

			nmfck[m] = new double[F][][];
			for (int f=0;f<F;f++) {
				nmfck[m][f] = new double[A[f].length][T];
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


			for (int f=0;f<F;f++) {
				for (int i=0;i<nmfck[m][f].length;i++) {
					for (int k=0;k<T;k++) {
						//This just assigns equal weight to every topic/feature/cluster  for now, non-optimal solution
						//TODO: sample numbers from cluster-prior instead!
						nmfck[m][f][i][k] = 1.0 / F * 1.0/A[f][group[f]].length * 1.0/T;
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
		double[][][] pi_kfc = new double[F][][];


		for (int f=0;f<F;f++) {
			int grouplength = A[f][group[f]].length;
			pi_kfc[f] = new double[grouplength][T];
			for (int i=0;i<grouplength;i++) {

				int g = group[f];
				int a=A[f][g][i];

				//here we do things once which do not depend on topic k
				//increase counter of seen documents for that cluster
				rhot_cluster[f][a]++;
				//..and for that group
				rhot_group[f][g]++;

				for (int k=0;k<T;k++) {
					pi_kfc[f][i][k] = 				
							//Topic probability for this group
							(sumqkfc2[f][a][k] + alpha_0 * pi0[k]) / sumqkfc2_denominator[f][a]
							//cluster probability in group
							* (sumqfgc[f][group[f]][i] + delta) 
							/ (sumqfg[f][group[f]] + grouplength*delta)
							//feature probability
							* (sumqf[f] + epsilon);
				}

			}

		}


		//get words in random order
		//Collections.shuffle(wordset);

		for (Entry<Integer,Integer> e : wordset) {

			int t = e.getKey();
			int termfreq = e.getValue();

			//increase number of words seen in that batch
			batch_words+=termfreq;
			n+=termfreq;

			if(debug)
				System.out.println(t);

			//sum of gamma, for normalisation
			double qsum = 0.0;

			for (int f=0;f<F;f++) {

				//Number of clusters of the group
				int grouplength = A[f][group[f]].length;

				q[f] = new double[grouplength][T];
				for (int i=0;i<grouplength;i++) {
					int a=A[f][group[f]][i];

					//OLD INFERENCE					
					//a is the global cluster index, do not mix with c which just counts the clusters of the group of the document
					//Remember that c is the cth cluster for document m -- the index is stored in a 
					//prodqmkfc gives the expected probability that we did _not_ see topic k in feature f, cluster c
					//						gamma[f][c][k] = 
					//								(1.0 - Math.exp(sumqmkfc[k][f][c])) * nmkfc[m][k][f][c]
					//										+ ((Math.exp(sumqmkfc[k][f][c]) * (1.0 - Math.exp(sumqkfc[k][f][a]))*alpha_1) 
					//												* (sumqkfc2[k][f][a]) / (alpha_0 + (sumqkfc2[k][f][a]))
					//												+ (Math.exp(sumqkfc[k][f][a])) * alpha_1 * alpha_0 * pi0[k])
					//												* (sumqfgc[f][group[f]][c] + delta) 
					//												/ (sumqfg[f][group[f]] + grouplength*delta)
					//												* (sumqf[f] + epsilon) 
					//												/ (sumq + F * epsilon)
					//												* (nkt[k][t] + beta0*tau[t]) / (nk[k] + beta0);


					for (int k=0;k<T;k++) {
						q[f][i][k] = 
								//probability of topic given feature & group
								(nmfck[m][f][i][k] + alpha_1 * pi_kfc[f][i][k])
								//probability of topic given word w
								* (nkt[k][t] + beta0*tau[t]) / (nk[k] + beta0);

						qsum+=q[f][i][k];
					}
				}
			}


			//Normalise gamma (sum=1), update counts and probabilities
			for (int f=0;f<F;f++) {
				//System.out.println("test " + f + " " + group[f] + " " + A[f][group[f]].length);

				//Number of clusters of the group
				int grouplength = A[f][group[f]].length;
				for (int i=0;i<grouplength;i++) {
					//a is the global cluster index, do not mix with c which just counts the clusters of the document
					//int a=A[f][group[f]][c];
					for (int k=0;k<T;k++) {
						//normalise
						q[f][i][k]/=qsum;

						//add to batch counts
						if (rhot_document>BURNIN) {
							tempnkt[k][t]+=q[f][i][k]*termfreq;
						}

						//update probability of _not_ seeing kfc in the current document
						if (termfreq>1) {
							sumqmfck[f][i][k]*=Math.pow(1.0-q[f][i][k],termfreq);
						}
						else {
							sumqmfck[f][i][k]*=1.0-q[f][i][k];
						}

						//update document-feature-cluster-topic counts
						if (termfreq==1) {
							nmfck[m][f][i][k] = oneminusrhostkt_document * nmfck[m][f][i][k] + rhostkt_documentNm * q[f][i][k];
						}
						else {
							double temp = Math.pow(oneminusrhostkt_document,termfreq);
							nmfck[m][f][i][k] = temp * nmfck[m][f][i][k] + (1.0-temp) * N[m] * q[f][i][k];
						}

					}

				}
			}



		}
		//Ending of loop over document words

		//after we processed all terms, update global counts
		//sum gamma over features and cluster for each topic k 
		for (int f=0;f<F;f++) {
			int g = group[f];
			//Number of clusters of the group
			int grouplength = A[f][g].length;
			for (int i=0;i<grouplength;i++) {								
				//how often did we see this cluster already?
				rhot_cluster[f][i]++;
				for (int k=0;k<T;k++) {
					sumqtemp[f][g][i][k]
							+= 1.0-sumqmfck[f][i][k];
					sumqftemp[f]+=1.0-sumqmfck[f][i][k];
				}
			}
		}


		
		


		//We update global topic-word counts in batches (mini-batches lead to local optima)
		if (rhot%BATCHSIZE == 0) {
			//only update if we are beyond burn in phase
			if (rhot_document>BURNIN) {
				stochasticDocumentFeatureUpdate();
			}
		}

		//Stochastic cluster updates: tmkfc unkown (tables!)
		//-> get table counts per cluster (or estimate it)
		//Stochastic group updates: tmkfg unknown (tables in group, tells how often cluster X was chosen in group g)
		for (int f=0;f<F;f++) {
			int g = group[f];
			if (rhot_group[f][g] % BATCHSIZE_GROUPS == 0) {
				stochasticClusterUpdate(f,g);	
			}			
		}
	}


	/**
	 * Here we do stochastic updates of the document-topic and the global feature counts
	 */
	private static void stochasticDocumentFeatureUpdate() {
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
			//reset
			sumqftemp[f]=0;
		}

		//TODO: use tempsumq to update the cluster-topic distributions

		batch_words = 0;
	}

	/**
	 * @param f feature of the group
	 * @param g	group id
	 * 
	 *  Stochastic update of the topic counts for a given group of a feature
	 *  
	 */
	private static void stochasticClusterUpdate(int f, int g) {

		//These are the global variables...
		//sumqkfc2[f][a][k] ok
		//sumqfgc[f][group[f]][i] ok 
		//sumqfg[f][group[f]] 

		//calculate update rate
		double rhost_group = rho(rhos,rhotau,rhokappa,rhot_group[f][g]);
		double oneminusrho = 1.0-rhost_group;
	
		
		int groupsize = A[f][g].length;
		for (int i=0;i<groupsize;i++) {
			int a = A[f][g][i];

			//sum over all topics
			double topicsum = 0;
			for (int k=0;k<T;k++) {
				sumqkfc2[f][a][k] = oneminusrho*sumqkfc2[f][a][k] + rhost_group * Cfc[f][a] * (sumqtemp[f][g][i][k]/BATCHSIZE_GROUPS);
				topicsum+=sumqtemp[f][g][i][k];
				
				//We have to reset the batch counts 
				sumqtemp[f][g][i][k] = 0;
			}
			//We calculate the denominator for topic inference to save time
			sumqkfc2_denominator[f][a] = BasicMath.sum(sumqkfc2[f][a])+ alpha_0;

			sumqfgc[f][g][i]  = oneminusrho*sumqfgc[f][g][i] + rhost_group * Cfg[f][g] * (topicsum/BATCHSIZE_GROUPS);

		}
		sumqfg[f][g] = BasicMath.sum(sumqfgc[f][g]);
				
		

	}

	private static void inferenceGlobalTopicDistribution() {

		double[] sumfck = new double[T];

		//Start with pseudo-counts from the Beta prior
		for (int k=0;k<T;k++) {
			ahat[k] = 1.0;
			bhat[k]=gamma;
		}
		//Now add observed estimated counts

		for (int f=0;f<F;f++) {
			for (int a=0;a<Cf[f];a++) {
				for (int k=0;k<T;k++) {
					//We estimate pi_0 by looking at the documents of each cluster of each feature.
					//For each cluster, we calculate the probability that we saw topic k in one of its documents.
					//We then calculate the expected number of clusters where we saw topic k.
					sumfck[k]+= sumqfck[f][a][k];
					ahat[k] += sumfck[k];
				}
			}
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
