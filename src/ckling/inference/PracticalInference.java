package ckling.inference;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.knowceans.util.DirichletEstimation;
import org.knowceans.util.Gamma;

import jgibblda.Dictionary;
import jgibblda.Pair;

import ckling.functions.ArrayTool;
import ckling.math.BasicMath;
import ckling.text.Read;
import ckling.text.Save;
import ckling.text.Text;

public class PracticalInference {

	//We have a debugging mode for checking the parameters
	public static boolean debug = false;

	//number of top words returned for the topic file
	public static int topk = 100;

	//Number of read docs (might repeat with the same docs)
	public static int RUNS = 1000;

	//Save variables after step SAVE_STEP
	public static int SAVE_STEP = 100;

	public static int BATCHSIZE = 100;

	public static int BATCHSIZE_GROUPS = 10;

	//Tells after how many steps we update alpha
	public static int STEP_ALPHA_UPDATE = 10000;

	//Burn in phase: how long to wait till updating nkt?
	public static int BURNIN = 10;


	//private static String dir = "/export/ckling/jgibblda/models/";
	private static String dir = "/home/c/work/topicmodels/";

	private static String basedirectory = dir+"porn_full/";



	public static int M = 0; //Number of Documents
	public static int C = 0; //Number of words in the corpus
	public static int V = 0; //Number of distinct words in the corpus, read from dictfile
	public static int F = 0; //Number of features
	public static int[] Cf; //Number of clusters for each feature
	public static int[] Cfd; //Number of documents for each feature
	public static int[][] Cfc; //Number of documents for each cluster (FxCf)
	public static int[][] Cfg; //Number of documents for each group (FxCg)
	public static int[] N; //Number of Words per document
	public static int T = 20; //Number of truncated topics
	public static HashMap<Integer,Integer> Ncounts; //Number of documents with N words


	public static int[][][] A; //Groups and their clusters

	public static ArrayList<ArrayList<Set<Integer>>> affected_groups; //Which clusters are affected by changes in group g; F x G x ...

	public static double alpha_0 = 0.5;

	public static double alpha_1 = 0.5;

	//Dirichlet parameter for multinomials over clusters

	public static double delta = 1;

	//Dirichlet parameter for multinomial over features
	public static double epsilon = 1;

	public static double gamma = 0.5;

	//Dirichlet concentration parameter for topic-word distributions
	public static double beta_0 = 0.1;

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

	//Estimated number of tables for word v in topic k, used to update tau
	public static double[][] mkt;
	//Estimated number of tables for word v in topic k, used to update tau
	//temporal variable for estimation
	public static double[][] tempmkt;


	//Topic/feature/cluster "counts" per document
	public static double[][][][] nmfck;
	//Topic-table "counts" for each document per cluster
	public static double[][][][] tmkfc;

	//variational parameters for the stick breaking process - parameters for Beta distributions \hat{a} and \hat{b}
	private static double[] ahat;
	private static double[] bhat;


	//rho: Learning rate; rho = s / ((tau + t)^kappa);
	//recommended values: See "Online learning for latent dirichlet allocation" paper by Hoffman
	//tau = 64, K = 0.5; S = 1; Batchsize = 4096

	private static int rhos = 10;
	private static double rhokappa = 0.9;
	private static int rhotau = 100;

	private static int rhos_document = 1;
	private static double rhokappa_document = 0.5;
	private static int rhotau_document = 64;

	//	private static double rhokappa_hyper = 0.9;
	//	private static int rhos_hyper = 5;
	//	private static int rhotau_hyper = 100;
	//tells the number of processed words
	private static int rhot = 0;
	//tells the number of the current run)
	private static int rhot_step = 0;
	//tells the number of words seen in this document
	private static int[] rhot_words_doc;

	//count number of words seen in the batch
	//remember that rhot counts the number of documents, not words
	private static int[] batch_words;
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
	private static double[][][] sumqfck2;
	//Sum of sumqkfc2[f][c][k] for all k; F x Cf
	private static double[][] sumqfck2_denominator;
	//Sum over log(1-q(k,f,c)) for all documents in the given group g and cluster number c. We need this sum for calculating 
	//Sum of E[n>0] and E[n=0] for all documents of the group and cluster. 
	private static double[][][] sumqfgc;
	//The same but summed over all clusters of the group
	private static double[][] sumqfgc_denominator;

	//helping variable: counts for document
	private static double[][][] sumqmfck;

	//Sum over 1-q(_,f,_) for feature f (approximated seat counts)
	private static double[] sumqf;
	//Batch estimate of sumqf2 for stochastic updates
	//private static double[] sumqf2temp;


	//Counter: how many observations do we have per cluster? Dimension: F x |C[f]|
	//We use this for doing batch updates of the cluster parameters and to calculate
	//the update rate \rho
	//private static int[][] rhot_cluster;


	//statistic over gamma, used to do batch-updates of clusters: sum of gamma
	private static double[][][][] sumqtemp2_clusters;
	//statistic over gamma, used to do batch-updates of features: sum of gamma
	private static double[] sumqtemp2_features;
	//statistic over gamma, used to do batch-updates of clusters: prodct of gamma-1
	private static double[][][][] sumqtemp;


	//Document-Table counts per topic, added: for updating hyper parameters. Dimension: M.
	//private static double sumqk;


	//Helper variable: word frequencies
	public static int[] wordfreq;

	//document-words (M x doclength)
	private static Set<Entry<Integer, Integer>>[] wordsets;
	//document groups (M x F)
	private static int[][] groups;

	private static double[][][] q;

	//group-cluster-topic distributions F x G x C x T
	private static double[][][][] pi_kfc;


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

			System.out.println("Run " + i + " (alpha_0 "+alpha_0+" alpha_1 "+ alpha_1+ " beta_0 " + beta_0 + " gamma "+gamma);



			rhot_step++;



			for (int m=0;m<M;m++) {
				inferenceDoc(m);
			}
			updateHyperParameters();


			if (rhot_step%SAVE_STEP==0) {
				//store inferred variables
				System.out.println("Storing variables...");
				save();
			}

		}



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
				for (int c=0;c<A[f][g].length;c++) {
					int a = A[f][g][c];
					Cfc[f][a]++;
					Cfd[f]++;
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

		affected_groups = new ArrayList<ArrayList<Set<Integer>>>();
		for (int f=0;f<F;f++) {
			affected_groups.add(f, new ArrayList<Set<Integer>>());
			for (int g=0;g<A[f].length;g++) {
				affected_groups.get(f).add(g, new HashSet<Integer>());
				for (int i = 0;i<A[f][g].length;i++) {
					for (int g2=0;g2<A[f].length;g2++) {
						for (int i2 = 0;i2<A[f][g].length;i2++) {
							if (i2==i) {
								affected_groups.get(f).get(g).add(g2);
							}
						}
					}
				}
			}
		}


	}

	//set Parameters
	private static void getParameters() {
		readFfromTextfile();
		q = new double[F][][];
		System.out.println("Reading groups...");

		readGroups(); //if there is an unseen Group mentioned

		V = dict.length();

		alpha_0 = 0.5;
		alpha_1 = 0.5;
		delta = 1;
		epsilon = 1;
		beta_0 = 0.1;
		//T = 2;

		batch_words = new int[V];

		mkt = new double[T][V];	
		tempmkt = new double[T][V];

		nk = new double[T];
		nkt = new double[T][V];	
		tempnkt = new double[T][V];	

		//count the number of documents in each group
		Cfg = new int[F][];
		for (int f=0;f<F;f++) {
			Cfg[f]=new int[A[f].length];
		}
		//count the number of documents in each feature
		Cfd=new int[F];
		//count the number of documents in each cluster
		Cfc=new int[F][];
		for (int f=0;f<F;f++) {
			Cfc[f]=new int[Cf[f]];
		}

		//read corpus size and initialise nkt / nk
		readCorpusSize();

		rhot_words_doc=new int[M];
		rhot_group = new int[F][];
		for (int f=0;f<F;f++) {
			rhot_group[f]=new int[A[f].length];
		}

		N = new int[M];

		nmfck = new double[M][][][];

		for (int k=0;k<T;k++) {
			for (int t=0; t < V; t++) {
				nkt[k][t] = (Double.valueOf(wordfreq[t])/Double.valueOf(T)) * (0.1 + 0.9 * Math.random() * 2.0);
				//nkt[k][t] = (Double.valueOf(C)/V * 1.0/ Double.valueOf(T)) * 0.9 + 0.1 * (0.5-Math.random()) * C/Double.valueOf(T);
				nk[k]+=nkt[k][t];

				mkt[k][t] = 1.0;
				tempmkt[k][t] = 1.0;
			}
		}



		pi0 = new double[T];
		ahat = new double[T];
		bhat = new double[T];

		for (int i=0;i<T;i++) {
			pi0[i]=1.0/(double)T;
		}

		System.out.println("Initialising count variables...");

		sumqfck = new double[F][][];
		//rhot_cluster = new int[F][];
		//for (int f=0;f<F;f++) {
		//	rhot_cluster[f] = new int[Cf[f]];
		//}
		sumqfck2 = new double[F][][];
		sumqfck2_denominator = new double[F][];
		sumqtemp2_clusters = new double[F][][][];
		sumqtemp2_features = new double[F];
		sumqtemp = new double[F][][][];
		for (int f=0;f<F;f++) {
			sumqfck[f] = new double[Cf[f]][T];
			sumqfck2[f] = new double[Cf[f]][T];
			sumqfck2_denominator[f] = new double[Cf[f]];
			sumqtemp2_clusters[f] = new double[A[f].length][][];
			sumqtemp[f] = new double[A[f].length][][];
			for (int g=0;g<A[f].length;g++) {
				sumqtemp2_clusters[f][g]=new double[A[f][g].length][T];
				sumqtemp[f][g]=new double[A[f][g].length][T];
				for (int a=0; a<A[f][g].length;a++) {
					for (int k=0;k<T;k++) {
						sumqtemp[f][g][a][k] = 1.0;
					}
				}
			}
		}


		sumqfgc = new double[F][][];
		sumqfgc_denominator = new double[F][];
		sumqf = new double[F];

		//sumqf2temp = new double[F];
		pi_kfc = new double[F][][][];
		for (int f=0;f<F;f++) {
			sumqfgc[f] = new double[A[f].length][];
			sumqfgc_denominator[f] = new double[A[f].length];
			pi_kfc[f] = new double[A[f].length][][]; 
			for (int g=0;g<A[f].length;g++) {
				//System.out.println(A[f].length + " " + a + " " + A[f][a]);
				sumqfgc[f][g] = new double[A[f][g].length];
				pi_kfc[f][g] = new double[A[f][g].length][T];
				for (int i=0;i<A[f][g].length;i++) {
					for (int k = 0; k < T; k++) {
						//for every group: get topic distribution of clusters and their weight 
						//(the weight of the clusters) for the group
						pi_kfc[f][g][i][k] = 1.0/(double)T;
					}
				}
			}
		}

		sumqmfck = new double[F][][];

	}


	/**
	 * Reads the number of features F from by counting the
	 * number of groups in the first line of the textfile
	 */
	private static void readFfromTextfile() {
		String firstLine = Text.readLineStatic(documentfile);
		//File e.g. looks like groupID1,groupID2,groupID3,groupID4 word1 word2 word3
		F = firstLine.split(" ")[0].split(",").length;
	}

	private static void readDocs() {

		//Try to read parsed documents
		Read read = new Read();
		wordsets = read.readVarSet(basedirectory+"wordsets");
		groups = read.readVarInt(basedirectory+"groups");
		if (wordsets!=null && groups != null) {
			return ;
		}
		else {
			wordsets = new Set[M];
			groups = new int[M][F];
		}

		if (documentText == null) {
			documentText = new Text();
			documentText.setLang("en");
			documentText.setStopwords(false);
			documentText.setStem(false);
		}
		String line = ""; 
		int m=0;
		while ((line = documentText.readLine(documentfile))!=null) {
			HashMap<Integer,Integer> distinctWords = new HashMap<Integer, Integer>();

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

				wordsets[m]=wordset;
				groups[m]=group;
				m++;
			}


		}
		Save save = new Save();
		save.saveVar(wordsets, basedirectory+"wordsets");
		save.saveVar(groups, basedirectory+"groups");

		return;



	}

	private static void inferenceDoc(int m) {

		Set<Entry<Integer, Integer>> wordset = wordsets[m];
		if (wordset == null || wordset.isEmpty()) return;

		//increase counter of documents seen
		rhot++;

		int[] grouplength = new int[F];
		int[] group = groups[m];

		//Expectation(number of tables)
		double[] qmk = new double[T];
		for (int k=0;k<T;k++) {
			qmk[k]=1.0;
		}

		
		//Stochastic cluster updates: tmkfc unkown (tables!)
		//-> get table counts per cluster (or estimate it)
		//Stochastic group updates: tmkfg unknown (tables in group, tells how often cluster X was chosen in group g)
		for (int f=0;f<F;f++) {

			//Number of clusters of the group
			grouplength[f] = A[f][group[f]].length;

			//Helping variable: sum log(1-qkfc) for this document, (don't mix with sumqkfc, which is the global count variable!)
			//Tells the expected total number of times topic k was _not_ seen for feature f in cluster c in the currect document
			sumqmfck[f]=new double[grouplength[f]][T];

			q[f] = new double[grouplength[f]][T];

		}
		//get words in random order - slightly improves model performance. In theory, all words and documents should be drawn randomly
		//Collections.shuffle(wordset);


		for (int f=0;f<F;f++) {
			for (int i=0;i<grouplength[f];i++) {
				for (int k=0;k<T;k++) {
					sumqmfck[f][i][k] = 1.0;
				}
			}
		}
		
		//For the first run...
		if (rhot_step == 1) {
			//in case we do not know the length of the doc yet
			if (N[m]==0) {
				for (Entry<Integer,Integer> e : wordset) {
					N[m]+=e.getValue();
				}
			}


			nmfck[m] = new double[F][][];
			for (int f=0;f<F;f++) {
				nmfck[m][f] = new double[grouplength[f]][T];
			}

			int ft = F * T;
			double sum = 0;
			for (int f=0;f<F;f++) {
				for (int i=0;i<A[f][group[f]].length;i++) {
					for (int k=0;k<T;k++) {
						//This just assigns equal weight to every topic/feature/cluster  for now, non-optimal solution
						//TODO: sample numbers from cluster-prior instead?
						nmfck[m][f][i][k] = (1.0 / (ft  * grouplength[f]))+(0.1 + 0.9 * Math.random() * 2.0);
						sum += nmfck[m][f][i][k];
						//nmfck[m][f][i][k] = 0;
					}
				}
			}
			for (int f=0;f<F;f++) {
				for (int i=0;i<A[f][group[f]].length;i++) {
					for (int k=0;k<T;k++) {
						nmfck[m][f][i][k] *= N[m]/sum;
					}
				}
			}

		}

		//get cluster-specific topic distributions
		//we do not re-estimate them after every word as we do not expect big changes		

		//We update global topic-word counts in batches (mini-batches lead to local optima)
		if (rhot%BATCHSIZE == 0) {
			//only update if we are beyond burn in phase
			if (rhot_step>BURNIN) {
				updateTopicAndFeatureDistribution();
			}
		}




		for (Entry<Integer,Integer> e : wordset) {

			int t = e.getKey();
			int termfreq = e.getValue();

			//update number of words seen
			rhot_words_doc[m]+=termfreq;
			if (rhot_step>BURNIN) {
				//increase number of words seen in that batch
				batch_words[t]+=termfreq;
			}

			//sum of gamma, for normalisation
			double qsum = 0.0;

			double Vbeta0 = V*beta_0;

			double[] topic_term = new double[T];
			for (int k=0;k<T;k++) {
				topic_term[k] =
						(nkt[k][t] + beta_0) 
						/ (nk[k] + Vbeta0);
			}


			//Get probabilities of features
			double[] feature_p = new double[F];
			//Get probabilities of clusters
			double[][] cluster_p = new double[F][];

			for (int f=0;f<F;f++) {
				cluster_p[f] = new double[grouplength[f]];

				for (int i=0;i<grouplength[f];i++) {

					for (int k=0;k<T;k++) {

						q[f][i][k] = 
								//probability of topic given feature & group
								(nmfck[m][f][i][k] + pi_kfc[f][group[f]][i][k])
								//probability of topic given word w
								* topic_term[k];

						if ((q[f][i][k] == 0 || Double.isNaN(q[f][i][k])) && !debug) {
							System.out.println("q is " + q[f][i][k] + " " + nmfck[m][f][i][k] + " " + pi_kfc[f][group[f]][i][k] + " " +  topic_term[k]);
							debug = true;
						}

						qsum+=q[f][i][k];

					}
				}
			}


			double rhostkt_document = rho(rhos_document,rhotau_document,rhokappa_document,rhot_words_doc[m]);
			double oneminusrhostkt_document = (1.0 - rhostkt_document);
			double rhostkt_documentNm = rhostkt_document * N[m];

			double[] topic_probability = new double[T];


			if (qsum == 0 && !debug) {
				System.out.println("qsum is 0!");
			}

			//Normalise gamma (sum=1), update counts and probabilities
			for (int f=0;f<F;f++) {


				for (int i=0;i<grouplength[f];i++) {
					//a is the global cluster index, do not mix with c which just counts the clusters of the document
					//int a=A[f][group[f]][c];
					for (int k=0;k<T;k++) {
						//normalise
						q[f][i][k]/=qsum;
						feature_p[f]+=q[f][i][k];
						cluster_p[f][i]+=q[f][i][k];						

						//topic_probability[k]+=q[f][i][k];

						//						if (m == 0 && t==1494) {
						//							System.out.println(
						//									"Step: "+rhot_step+ "; Documentlength: "+N[m]+"; feature: " + f + "; cluster: "+i+ "; "+
						//											"topic "+ k + "; " +
						//													"q=" + q[f][i][k] + "; " +
						//															"nmfck = " + nmfck[m][f][i][k] + "; " +
						//																	"topic_term = "+topic_term[k]+ "; pikfc: " + pi_kfc[f][group[f]][i][k] + "; " +
						//																			"update rho: " +rhostkt_document
						//																					
						//									);
						//						}

						//add to batch counts
						if (rhot_step>BURNIN) {
							tempnkt[k][t]+=q[f][i][k]*termfreq;
							topic_probability[k]+=q[f][i][k];
						}

						//update probability of _not_ seeing kfc in the current document
						if (termfreq>1) {
							sumqmfck[f][i][k]*=Math.pow(1.0-q[f][i][k],termfreq);
						}
						else {
							sumqmfck[f][i][k]*=1.0-q[f][i][k];
							//System.out.println("q = " + sumqmfck[f][i][k]);
						}


						//update document-feature-cluster-topic counts
						if (termfreq==1) {
							nmfck[m][f][i][k] = oneminusrhostkt_document * nmfck[m][f][i][k] + rhostkt_documentNm * q[f][i][k];
						}
						else {
							double temp = Math.pow(oneminusrhostkt_document,termfreq);
							nmfck[m][f][i][k] = temp * nmfck[m][f][i][k] + (1.0-temp) * N[m] * q[f][i][k];
						}
						//if (m%1000 == 1)
						//System.out.println(m + " "+ k + ": " + nmfck[m][f][i][k]);

					}

				}
			}


			for (int k=0;k<T;k++) {
				qmk[k]*= Math.pow(1.0-topic_probability[k],termfreq);
			}

			if (rhot_step>BURNIN) {

				for (int k=0;k<T;k++) {
					if (termfreq>1) {
						tempmkt[k][t]*=Math.pow(1.0-topic_probability[k],termfreq);
					}
					else {
						tempmkt[k][t]*=1.0-topic_probability[k];
					}
				}
			}



		}
		//End of loop over document words


		//get probability for NOT seeing topic f to update delta
		//double[] tables_per_feature = new double[F];

		for (int f=0;f<F;f++) {

			int g = group[f];
			//increase count for that group
			rhot_group[f][g]++;

			//update feature-counter
			for (int i=0;i<grouplength[f];i++) {

				//how often did we see this cluster already?
				//rhot_cluster[f][i]++;
				for (int k=0;k<T;k++) {
					//p(not_seeing_fik)
					sumqtemp[f][g][i][k] *= sumqmfck[f][i][k];
					//System.out.println(sumqmfck[f][i][k] + " " + qsum);
					double temp = 1.0 - sumqmfck[f][i][k];
					sumqtemp2_clusters[f][g][i][k]+= temp;
					sumqtemp2_features[f]+=temp;
				}
			}
			if (rhot_group[f][g] % BATCHSIZE_GROUPS == 0) {
				updateClusterTopicDistribution(f,g);	
			}
		}

		//sumqk+=tables_per_topic_sum;


		if (rhot_step>BURNIN) {
			//count INVERSE tables per topic to update alpha_1

			double tables_per_topic_sum = Double.valueOf(T);
			for (int k=0;k<T;k++) {
				tables_per_topic_sum -= qmk[k];
			}
			//we use the learning function for global distributions
			double rhostkt = rho(rhos,rhotau,rhokappa,rhot);
			alpha_1 = (1.0-rhostkt) * alpha_1 + rhostkt * tables_per_topic_sum / (Gamma.digamma0(N[m]+alpha_1) - Gamma.digamma0(alpha_1));
			//System.out.println(BasicMath.sum(qmk) + " " +alpha_1 + " "+ tables_per_topic_sum / (Gamma.digamma0(N[m]+alpha_1) - Gamma.digamma0(alpha_1)));
		}

	}


	private static void estimateGroupTopicDistribution(int f, int g) {

		int grouplength = A[f][g].length;
		for (int i=0;i<grouplength;i++) {

			int a=A[f][g][i];

			//We calculate the denominator for topic inference to save time
			sumqfgc_denominator[f][g] = BasicMath.sum(sumqfgc[f][g]) + A[f][g].length*delta;
			//We calculate the denominator for topic inference to save time
			sumqfck2_denominator[f][a] = BasicMath.sum(sumqfck2[f][a])+ alpha_0;

			double sumqf_denominator = BasicMath.sum(sumqf);

			for (int k=0;k<T;k++) {

				//we already multiply with alpha_1 to save time
				pi_kfc[f][g][i][k] = 	alpha_1 *			
						//Topic probability for this group
						(sumqfck2[f][a][k] + alpha_0 * pi0[k]) / sumqfck2_denominator[f][a]
								//cluster probability in group
								* (sumqfgc[f][g][i] + delta) 
								/ (sumqfgc_denominator[f][g])
								//feature probability
								* (sumqf[f] + epsilon)
								/ (sumqf_denominator + F*epsilon);

				if ((Double.isNaN(pi_kfc[f][g][i][k]) || Double.isInfinite(pi_kfc[f][g][i][k])) && !debug) {
					System.out.println("pi " +pi_kfc[f][g][i][k] + " " + alpha_1 + " " + sumqfck2[f][a][k] + " " + sumqfgc[f][g][i] + " " +sumqf[f]);
					debug = true;
				}


			}

		}
	}

	/**
	 * Here we do stochastic updates of the document-topic and the global feature counts
	 */
	private static void updateTopicAndFeatureDistribution() {
		//if (rhot%1000 == 0)
		//System.out.println("Updating topics... (Step "+rhot+")");
		double batchsum = BasicMath.sum(batch_words);

		//learning rate for the topics
		double rhostkt = rho(rhos,rhotau,rhokappa,rhot);
		double rhostktnormC =  rhostkt / batchsum * C;


		for (int k=0;k<T;k++) {
			for (int v=0;v<V;v++) {
				//update topic-word counts
				nk[k] -= nkt[k][v];
				double oneminusrhostkt = (1.0 - rhostkt);

				nkt[k][v] *= oneminusrhostkt;

				//update word-topic-tables for estimating tau
				mkt[k][v] *= oneminusrhostkt;
				if(!debug && Double.isInfinite(mkt[k][v])) {
					System.out.println("mkt pre " + Double.valueOf(wordfreq[v])/batch_words[v] + " " + mkt[k][v] + " " + Double.valueOf(wordfreq[v])/batch_words[v]);
					debug = true;
				}

				//we estimate the topic counts as the average q (tempnkt consists of BATCHSIZE observations)
				//and multiply this with the size of the corpus C
				if (tempnkt[k][v]>0) {
					//System.out.println("Topic-word " +k + " " + v + " " + tempnkt[k][v]);
					nkt[k][v] += rhostktnormC * tempnkt[k][v];

					//estimate tables in the topic per word, we just assume that the topic-word assignment is 
					//identical for the other words in the corpus.
					mkt[k][v] += rhostkt * (1.0-(Math.pow(tempmkt[k][v],Double.valueOf(wordfreq[v])/batch_words[v])));
					if(!debug &&  (Double.isInfinite(tempmkt[k][v]) || Double.isInfinite(mkt[k][v]))) {
						System.out.println("mkt estimate " + tempmkt[k][v] + " " + mkt[k][v] + " " + Double.valueOf(wordfreq[v])/batch_words[v]);
						debug = true;
					}
					//System.out.println((1.0-(Math.pow((Double.valueOf(batch_words[v]) - Double.valueOf(tempnkt[k][v]))/Double.valueOf(batch_words[v]),wordfreq[v]))));		

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
		//System.out.println(BasicMath.sum(mkt));
		//reset
		for (int k=0;k<T;k++) {
			for (int t=0;t<V;t++) {
				tempmkt[k][t] = 1.0;
			}
		}

		for (int f=0;f<F;f++) {

			sumqf[f]=(1.0-rhostkt) * sumqf[f] +  rhostkt * ( Double.valueOf(M) / BATCHSIZE) * sumqtemp2_features[f];
			//System.out.println("test " + sumqtemp2_features[f]);

			sumqtemp2_features[f]=0;
			//use average probability for not seeing f to estimate the probability that it was chosen at least once
			//sumqf2[f]=(1.0-rhostkt) * sumqf2[f] + rhostkt * 1.0-Math.pow(sumqf2temp[f]/Double.valueOf(BATCHSIZE),Cfd[f]);

		}


	}

	/**
	 * @param f feature of the group
	 * @param g	group id
	 * 
	 *  Stochastic update of the topic counts for a given group of a feature
	 *  
	 */
	private static void updateClusterTopicDistribution(int f, int g) {

		//if (rhot_group[f][g]%10000 == 0)
		//System.out.println("Stochastic cluster update (feature " + f + ", group " + g + ")");

		//These are the global variables...
		//sumqkfc2[f][a][k] ok
		//sumqfgc[f][group[f]][i] ok 
		//sumqfg[f][group[f]] 

		//calculate update rate
		double rhost_group = rho(rhos,rhotau,rhokappa,rhot_group[f][g]);
		double oneminusrho = 1.0-rhost_group;



		//sum over table counts per cluster


		int groupsize = A[f][g].length;
		for (int i=0;i<groupsize;i++) {
			int a = A[f][g][i];

			//update group-cluster-counts: how many tables do we expect to see for group i?
			sumqfgc[f][g][i]  = oneminusrho*sumqfgc[f][g][i] + rhost_group * BasicMath.sum(sumqtemp2_clusters[f][g][i]) * Double.valueOf(Cfg[f][g])/Double.valueOf(BATCHSIZE_GROUPS);

			for (int k=0;k<T;k++) {

				//update table counts for the global topic distribution:
				//-> Probability of seeing topic k once in each cluster?

				//total documents in cluster - remember that this includes documents from other groups
				int cluster_size = Cfc[f][a];
				//update the probability of seeing a table in the cluster: E(m_{f,c,k} > 0)
				sumqfck[f][a][k] = oneminusrho*sumqfck[f][a][k] + rhost_group * (1.0- Math.pow(sumqtemp[f][g][i][k],cluster_size/Double.valueOf(BATCHSIZE_GROUPS)));

				//update counts per cluster
				sumqfck2[f][a][k] = oneminusrho*sumqfck2[f][a][k] + rhost_group * cluster_size * (sumqtemp2_clusters[f][g][i][k]/Double.valueOf(BATCHSIZE_GROUPS));

				//System.out.println(f + " " + g + " " + i + " " +k + " " +sumqtemp[f][g][i][k] + " " + sumqtemp[f][g][i][k]/Double.valueOf(BATCHSIZE_GROUPS) + " " + cluster_size );

				//We have to reset the batch counts 
				sumqtemp2_clusters[f][g][i][k] = 0;
				sumqtemp[f][g][i][k] = 1.0;
			}



		}


		//System.out.println("sum: " +BasicMath.sum(sumqftemp)/batchsum + " ; sumqfsum: " + BasicMath.sum(sumqf));

		if (rhot_step > BURNIN)  {
			//Update global topic distribution
			updateGlobalTopicDistribution();
		}

		Iterator<Integer> it = affected_groups.get(f).get(g).iterator();
		while (it.hasNext()) {
			int ag = it.next();
			estimateGroupTopicDistribution(f,ag);
		}
	}

	private static void updateGlobalTopicDistribution() {


		double[] sumfck = new double[T];

		//Start with pseudo-counts from the Beta prior
		for (int k=0;k<T;k++) {
			ahat[k] = 1.0;
			bhat[k]=gamma;
		}
		//Now add observed estimated counts

		for (int f=0;f<F;f++) {
			//A[f] holds the cluster indices for each cluster of each feature and thus gives us the 
			//number of clusters per feature by A[f].length
			for (int i=0;i<A[f].length;i++) {
				for (int k=0;k<T;k++) {
					//We estimate pi_0 by looking at the documents of each cluster of each feature.
					//For each cluster, we calculate the probability that we saw topic k in one of its documents.
					//We then calculate the expected number of clusters where we saw topic k.
					sumfck[k]+= sumqfck[f][i][k];
				}
			}
		}

		//now add this sum to ahat
		for (int k=0;k<T;k++) {
			ahat[k]+=sumfck[k];
		}


		double[] ahat_copy = new double[T];
		System.arraycopy(ahat, 0, ahat_copy, 0, ahat.length);
		//get indices of sticks ordered by size (given by ahat)
		int[] index = ArrayTool.sortArray(ahat_copy);
		//large sticks come first, so reverse order
		ArrayUtils.reverse(index);

		int[] index_reverted = ArrayTool.reverseIndex(index);

		//bhat is the sum over the counts of all topics > k
		for (int k=0;k<T-1;k++) {
			int sort_index = index_reverted[k];
			for (int k2=sort_index+1;k2<T;k2++) {
				int sort_index_lower = index[k2];
				bhat[k] += sumfck[sort_index_lower];
			}
		}

		for (int k=0;k<T;k++) {
			pi0[k]=ahat[k] / (ahat[k]+bhat[k]);
			int sort_index = index_reverted[k];
			for (int l=0;l<sort_index;l++) {
				int sort_index_lower = index[l];
				pi0[k]*=bhat[sort_index_lower] / (ahat[sort_index_lower]+bhat[sort_index_lower]);
			}
			//System.out.println("pi0["+k + "] = " + pi0[k] + "(" + sumqfck[0][0][k] + ")");
		}

		//MAP estimation for gamma (Sato (6))
		double gamma_denominator = 0.0;
		for (int k=0;k<T-1;k++) {
			gamma_denominator += Gamma.digamma0(ahat[k] + bhat[k])- Gamma.digamma0(bhat[k]);
		}

		gamma = (T -1) / gamma_denominator;


	}

	public static void updateHyperParameters() {

		if(! (rhot_step>BURNIN)) { return; }

		//System.out.println("Updating hyperparameters...");

		//Update alpha_0 using the table counts per cluster
		//Cf is the number of clusters per feature
		double alpha_0_denominator = 0;
		for (int f = 0; f < F; f++) {
			for (int i = 0; i < Cf[f]; i++) {
				alpha_0_denominator += Gamma.digamma0(Cfc[f][i] + alpha_0);
			}
		}
		alpha_0_denominator -= BasicMath.sum(Cf) * Gamma.digamma0(alpha_0);
		alpha_0 = BasicMath.sum(sumqfck) / alpha_0_denominator;
		//System.out.println(BasicMath.sum(sumqfck) + " "+ alpha_0_denominator);


		double beta_0_denominator = 0.0;
		for (int k=0; k < T; k++) {
			//log(x-0.5) for approximating the digamma function, x >> 1 (Beal03)
			beta_0_denominator += Gamma.digamma0(nk[k]+beta_0);
		}
		beta_0_denominator -= T * Gamma.digamma0(beta_0);
		//System.out.println("sum " + BasicMath.sum(mkt) + " beta_0 " +beta_0_denominator );
		beta_0 = 0;
		for (int k=0;k<T;k++) {
			for (int t = 0; t < V; t++) {
				beta_0 += mkt[k][t];
				if (!debug && (mkt[k][t] == 0 || Double.isInfinite(mkt[k][t] ) || Double.isNaN(mkt[k][t] ))) {
					System.out.println("mkt " + k + " " + t + ": " + mkt[k][t] + " nkt: " +  nkt[k][t]);
					debug = true;
				}
			}
		}

		beta_0 /= beta_0_denominator;


		//gamma prior Gamma(1,1), Minka
		//beta_0 = DirichletEstimation.estimateAlphaMap(nkt,nk,beta_0,1.0,1.0);

		//TODO zeta, delta: feature-choice (f), group-choice (delta)
		//For now: uniform prior!







	}


	public static double rho (int s,int tau, double kappa, int t) {

		//System.out.println(Double.valueOf(s)/Math.pow((tau+t),kappa));
		return Double.valueOf(s)/Math.pow((tau+t),kappa);

	}


	public static void save () {

		Save save = new Save();
		save.saveVar(nkt, basedirectory+"nkt_"+rhot_step);
		save.close();
		save.saveVar(pi0, basedirectory+"pi0_"+rhot_step);
		save.close();
		double fdenominator = BasicMath.sum(sumqf) + F * epsilon;
		double[] fprob = new double[F];
		for (int f = 0; f < F; f++) {
			fprob[f]=(sumqf[f] + epsilon) / fdenominator;
		}
		save.saveVar(fprob, basedirectory+"sumqf_"+rhot_step);
		save.close();
		save.saveVar(alpha_0, basedirectory+"alpha_0_"+rhot_step);
		save.close();
		save.saveVar(alpha_1, basedirectory+"alpha_1_"+rhot_step);
		save.close();

		//TODO: save document-topic distribution
		double[][] doc_topic = new double[M][T];
		for (int m=0;m<M;m++) {
			for (int k=0;k<T;k++) {
				doc_topic[m][k]  = 0;
			}
		}
		for (int m=0;m<M;m++) {
			for (int k=0;k<T;k++) {
				doc_topic[m][k]  = 0;
			}
			double sum = 0;
			int[] group = groups[m];
			int[] grouplength = new int[F]; 
			for (int f =0; f<F;f++) {
				int g = group[f];
				grouplength[f] = A[f][group[f]].length;
				for (int i=0;i<grouplength[f];i++) {
					for (int k=0;k<T;k++) {
						//Dimension nmfck: M x F x A[f][group[f]].length x T
						if (N[m]>0) {
						doc_topic[m][k]+=
								nmfck[m][f][i][k] +	
								pi_kfc[f][group[f]][i][k];
						sum += nmfck[m][f][i][k] + pi_kfc[f][group[f]][i][k];
						}
						else {
							doc_topic[m][k]+=
									
									pi_kfc[f][group[f]][i][k];
							sum += pi_kfc[f][group[f]][i][k];
						}
					}
				}
			}
			for (int k=0;k<T;k++) {
				doc_topic[m][k]/=sum;
			}
		}
		save.saveVar(doc_topic, basedirectory+"doc_topic_"+rhot_step);
		save.close();

		double[][][] feature_cluster_topics = new double[F][][];

		for (int f=0; f<F;f++) {
			feature_cluster_topics[f] = new double[Cf[f]][T];
			for (int a=0;a<Cf[f];a++) {
				sumqfck2_denominator[f][a] = BasicMath.sum(sumqfck2[f][a]) + alpha_0;
				for (int k=0;k<T;k++) {
					//System.out.println(sumqfck2[f][a][k]);
					feature_cluster_topics[f][a][k]=(sumqfck2[f][a][k] + alpha_0 * pi0[k]) / sumqfck2_denominator[f][a];
				}
			}

			save.saveVar(feature_cluster_topics[f], basedirectory+"clusters_"+f+"_"+rhot_step);
			save.close();
		}

		if (topk > V) {
			topk = V;
		}


		String[][] topktopics = new String[T*2][topk];

		for (int k=0;k<T;k++) {

			List<Pair> wordprob = new ArrayList<Pair>(); 
			for (int v = 0; v < V; v++){
				wordprob.add(new Pair(dict.getWord(v), (nkt[k][v]+beta_0/V)/(nk[k]+beta_0), false));
			}
			Collections.sort(wordprob);

			for (int i=0;i<topk;i++) {
				topktopics[k*2][i] = (String) wordprob.get(i).first;
				topktopics[k*2+1][i] = String.valueOf(wordprob.get(i).second);
			}

		}
		save.saveVar(topktopics, basedirectory+"topktopics_"+rhot_step);

	}



}
