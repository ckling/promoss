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

import ckling.geo.Map;
import ckling.math.BasicMath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArraySet;


public class Model {	

	//---------------------------------------------------------------
	//	Class Variables
	//---------------------------------------------------------------

	public static String tassignSuffix;	//suffix for topic assignment file
	public static String sigmaSuffix;		//suffix for sigma file (cluster - topic distribution) file
	public static String rhoSuffix;		//suffix for rho file (region - topic distribution) file
	public static String thetaSuffix;		//suffix for document specific theta (topic - document distribution) file
	public static String phiSuffix;		//suffix for phi file (topic - word distribution) file
	public static String othersSuffix; 	//suffix for containing other parameters
	public static String twordsSuffix;		//suffix for file containing words-per-topics
	public static String mapSuffix;		//suffix for file containing words-per-topics
	public static String mapRegionSuffix;		//suffix for file containing words-per-topics

	//---------------------------------------------------------------
	//	Model Parameters and Variables
	//---------------------------------------------------------------

	public String wordMapFile; 		//file that contain word to id map
	public String trainlogFile; 	//training log file	

	public String dir;
	public String dfile;
	public String modelName;
	public int modelStatus; 		//see Constants class for status of model
	public LDADataset data;			// link to a dataset

	public int M; //dataset size (i.e., number of docs)
	public int V; //vocabulary size
	public int K; //number of topics
	public int J; //number of clusters q
	public int R; //number of regions


	//use distances between neighbours in order to decide where to go
	public boolean CONSIDER_DIST=false;
	public int NR_THREADS =1;

	//set size of the training data, if 1.0 then no perplexity is calculated
	double trainingSize = 0.8;

	public int[][] nmk; // M x K

	public int[][] nkt; //K x V
	public int[] nk;
	public int[][] qd; //documents in cluster q
	public double[] tau;  // K+1
	public double[][] rho; //(für Gr) // R x K+1
	public double[][] sigma; //(für Gq) // J x K+1
	public double[][] pi; //(für Gj) // K+1
	public int[][] z; // M x N
	public int[] y; // J region assignment for clusters
	public double gamma;
	public double[] alphar; //alpha for each region
//	public double[] alphaq; //alpha for each cluster
	public double alphaq;
	public double alpha0; //
	public double beta;
	public double[][] phi; //K x V
	public double gammaa; 
	public double gammab;
	public double alpha0a; 
	public double alpha0b; 	  
	public double alphara; 
	public double alpharb; 	  
	public double alphaqa; 
	public double alphaqb;
	public double betaa; 
	public double betab;

	public SortedSet<Integer> kgaps;
	public List<Integer> kactive;

	public int niters; //number of Gibbs sampling iteration
	public int liter; //the iteration at which the model was saved	
	public int savestep; //saving period
	public int twords; //print out top words per each topic
	public int rtopics; //print out top topics per each region
	public int withrawdata;

	// variables for spatial clusters
	public int[][] qqN;
	public double[][] pQ; //probability for a point to belong to its neighbours 

	// Temp variables while sampling
	protected int [] length; //length[i] of document i

	//Topic description for understanding results
	protected String[] topicWords;

	//additional temp variables for spatial clusters
	protected int[] q; //spatial cluster index
	protected int[][] triangles; // array containing the cluster delaunay triangles
	protected double[][] qm; //cluster centroids in spherical coordinates, size 2 x L
	protected double[][] qmxyzk;
	
	//words and probabilities for map
	protected String[][] words;
	protected String[][] probabilities;
	
	//---------------------------------------------------------------
	//	Constructors
	//---------------------------------------------------------------	

	public Model(){
		setDefaultValues();	
	}

	/**
	 * Set default values for variables
	 */
	public void setDefaultValues(){
		wordMapFile = "wordmap4.txt";
		trainlogFile = "trainlog4.txt";
		tassignSuffix = ".tassign4";
		thetaSuffix = ".theta4";
		phiSuffix = ".phi4";
		sigmaSuffix = ".sigma4";
		rhoSuffix = ".rho4";
		othersSuffix = ".others4";
		twordsSuffix = ".twords4";
		mapSuffix = ".mapTopic4.html";
		mapRegionSuffix = ".mapRegion4.html";

		dir = "./";
		dfile = "trndocs.dat";
		modelName = "model-final";
		modelStatus = Constants.MODEL_STATUS_UNKNOWN;		

		M = 0;
		V = 0;
		liter = 0;
		kactive = new ArrayList<Integer>();
		kgaps = new TreeSet<Integer>();

		z = null;
		phi = null;

		//new variables;
		q = null;
		triangles = null;
		qm = null;
		qmxyzk = null;

		topicWords = null;

	}

	//---------------------------------------------------------------
	//	I/O Methods
	//---------------------------------------------------------------
	/**
	 * read other file to get parameters
	 */
	protected boolean readOthersFile(String otherFile){
		//open file <model>.others to read:

		try {
			BufferedReader reader = new BufferedReader(new FileReader(otherFile));
			String line;
			while((line = reader.readLine()) != null){
				StringTokenizer tknr = new StringTokenizer(line,"= \t\r\n");

				int count = tknr.countTokens();
				if (count != 2)
					continue;

				String optstr = tknr.nextToken();
				String optval = tknr.nextToken();

				if (optstr.equalsIgnoreCase("liter")){
					liter = Integer.parseInt(optval);
				}
				else if (optstr.equalsIgnoreCase("nwords")){
					V = Integer.parseInt(optval);
				}
				else if (optstr.equalsIgnoreCase("ndocs")){
					M = Integer.parseInt(optval);
				}
				else {
					// any more?
				}
			}

			reader.close();
		}
		catch (Exception e){
			System.out.println("Error while reading other file:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	protected boolean readTAssignFile(String tassignFile){
		try {
			int i,j;
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(tassignFile), "UTF-8"));

			String line;
			y = new int[J];	
			z = new int[M][];			
			data = new LDADataset(M);
			data.V = V;			
			for (i = 0; i < M; i++){
				line = reader.readLine();
				StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");

				int length = tknr.countTokens();

				Vector<Integer> words = new Vector<Integer>();
				Vector<Integer> topics = new Vector<Integer>();

				for (j = 0; j < length; j++){
					String token = tknr.nextToken();

					StringTokenizer tknr2 = new StringTokenizer(token, ":");
					if (tknr2.countTokens() != 2){
						System.out.println("Invalid word-topic assignment line\n");
						return false;
					}

					words.add(Integer.parseInt(tknr2.nextToken()));
					topics.add(Integer.parseInt(tknr2.nextToken()));
				}//end for each topic assignment

				//allocate and add new document to the corpus
				Document doc = new Document(words);
				data.setDoc(doc, i);

				//assign values for z
				z[i] = new int[length];
				for (j = 0; j < topics.size(); j++){
					z[i][j]=topics.get(j);
				}

			}//end for each doc

			reader.close();
		}
		catch (Exception e){
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * load saved model
	 */
	public boolean loadModel(){
		if (!readOthersFile(dir + File.separator + modelName + othersSuffix))
			return false;

		if (!readTAssignFile(dir + File.separator + modelName + tassignSuffix))
			return false;

		// read dictionary
		Dictionary dict = new Dictionary();
		if (!dict.readWordMap(dir + File.separator + wordMapFile))
			return false;

		data.dict = dict;

		return true;
	}

	/**
	 * Save word-topic assignments for this model
	 */
	public boolean saveModelTAssign(String filename){
		int i, j;

		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			//write docs with topic assignments for words
			for (i = 0; i < data.M; i++){
				for (j = 0; j < data.docs[i].length;++j){
					writer.write(data.docs[i].words[j] + ":" + z[i][j] + " ");					
				}
				writer.write("\n");
			}

			writer.close();
		}
		catch (Exception e){
			System.out.println("Error while saving model tassign: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save theta (topic distribution) for this model
	 */
	public boolean saveModelThetaD(String filename){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for (int i = 0; i < M; i++){
				for (int j = 0; j < K; j++){

					writer.write(pi[i][j] + " ");
				}
				writer.write("\n");
			}
			writer.close();
		}
		catch (Exception e){
			System.out.println("Error while saving topic distribution file for this model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save word-topic distribution
	 */

	public boolean saveModelPhiD(String filename){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			for (int i = 0; i < K; i++){
				for (int j = 0; j < V; j++){
					writer.write(phi[i][j] + " ");
				}
				writer.write("\n");
			}
			writer.close();
		}
		catch (Exception e){
			System.out.println("Error while saving word-topic distribution:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}



	/**
	 * Save other information of this model
	 */
	public boolean saveModelOthers(String filename){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			writer.write("gamma=" + gamma + "\n");
			writer.write("alpha0=" + alpha0 + "\n");
			writer.write("alphaq=" + alphaq + "\n");
//			for (int i =0;i < J;i++) {
//				writer.write("alphaq["+i+"]=" + alphaq[i] + "\n");
//			}
			writer.write("beta=" + beta + "\n");
			writer.write("ntopics=" + K + "\n");
			writer.write("nclusters=" + J + "\n");
			writer.write("ndocs=" + M + "\n");
			writer.write("nwords=" + V + "\n");
			writer.write("liters=" + liter + "\n");

			writer.close();
		}
		catch(Exception e){
			System.out.println("Error while saving model others:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveModelMapRegion(String filename) {

		Map map = new Map(51.501904, -0.146301, 10,R);
		map.setFile(filename);

		map.size(3);

		//avgP = 0.001; //experimental - set minimum of p to a small fixed value instead of avg


		for (int i=0;i<triangles.length;i++) {				

			if (y[triangles[i][0]] == y[triangles[i][1]] && y[triangles[i][0]] == y[triangles[i][2]]) {

				//read region
				int r = y[triangles[i][0]];

				double[] lat = new double[3];
				double[] lon = new double[3];
				lat[0] =  qm[0][triangles[i][0]];
				lon[0] =  qm[1][triangles[i][0]];
				lat[1] =  qm[0][triangles[i][1]];
				lon[1] =  qm[1][triangles[i][1]];
				lat[2] =    qm[0][triangles[i][2]];
				lon[2] =    qm[1][triangles[i][2]];


				//draw polygon	
				int red = 0,green = 0,blue = 0;

				if (r%3==0) {
					red = 255;
					green =  (int) Math.floor(255.0 * ((double) r / (double)K));
					blue = 0;
				}
				else if (r%3==1) {
					red = 0;
					green =  255;
					blue =  (int) Math.floor(255.0 * ((double) r / (double)K));
				}
				if (r%3==2) {
					red = (int) Math.floor(255.0 * ((double) r / (double)K));
					green =  0;
					blue =  255;
				}

				//double avgEta =  (eta[l][nextQN] +  eta[l][nextQ] + eta[l][j]) / 3.0;
				//max opacity = 0.5
				//double opacity = (avgEta/etaMax[l]) * 0.5;

				double opacity = 0.35;


				//map.addPolygon(lat,lon,red,green,blue,opacity, r);


				//System.out.println(l + " " + j + " " + nextQ + " " + nextQN);

			}
		}





		map.getMap(words,probabilities);

		return true;		
	}
	
	private double[] getTopicP (double lat, double lon) {
		double[] p_topic = new double[this.K];
		
		double[] xyz = ckling.geo.Coordinates.toCart(lat, lon);
		double x = xyz[0];
		double y = xyz[1];
		double z = xyz[2];
		
		double[] p_region = new double[J];
		for (int j=0;j<J;j++) {		
		 double mytx = qmxyzk[0][j] * x + qmxyzk[1][j] * y + qmxyzk[2][j] * z;
		 p_region[j] = (qmxyzk[3][j] / (2 * Math.PI * (Math.exp(qmxyzk[3][j]) - Math.exp(-qmxyzk[3][j]) ))) * Math.exp(mytx - 1);
		}
		double sum = ckling.math.BasicMath.sum(p_region);
		if (sum > 0) {
		for (int j=0;j<J;j++) {		
			p_region[j]/=sum;
		}
		}
		else {
			for (int j=0;j<J;j++) {		
				p_region[j]=1.0/Double.valueOf(J);
			}
		}
		
		for (int k=0;k<K;k++) {	
		for (int j=0;j<J;j++) {	
		p_topic[k] = sigma[j][k] * p_region[j];
		}
		}
		
		return p_topic;
	}
	
		
	/**
	 * Save model the most likely words for each topic
	 */
	public boolean saveModelTwordsD(String filename){
		try{

			words = new String[K][twords];
			probabilities = new String[K][twords];
			topicWords = new String[K];

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));

			if (twords > V){
				twords = V;
			}

			for (int k = 0; k < K; k++){
				List<Pair> wordsProbsList = new ArrayList<Pair>(); 
				for (int w = 0; w < V; w++){
					Pair p = new Pair(w, phi[k][w], false);

					wordsProbsList.add(p);
				}//end foreach word

				//print topic				
				writer.write("Topic " + k + "th:\n");
				Collections.sort(wordsProbsList);

				//reset topic description
				topicWords[k] = "";
				for (int i = 0; i < twords; i++){
					if (data.dict.contains((Integer)wordsProbsList.get(i).first)){
						String word = data.dict.getWord((Integer)wordsProbsList.get(i).first);

						topicWords[k] = topicWords[k] + ", " + word;
						writer.write("\t" + word + " " + wordsProbsList.get(i).second + "\n");
						words[k][i]=word;
						probabilities[k][i]=String.valueOf(wordsProbsList.get(i).second);
					}
				}
			} //end foreach topic			

			writer.close();
		}
		catch(Exception e){
			System.out.println("Error while saving model twords: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save cluster-region distribution
	 */

	public boolean saveModelRho(String filename){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			for (int r = 0; r < R; r++){
				for (int k = 0; k < K; k++){
					writer.write(rho[r][k] + " ");
				}
				writer.write("\n");
			}
			writer.close();
		}
		catch (Exception e){
			System.out.println("Error while saving region-topic distribution:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save cluster-region distribution
	 */

	public boolean saveModelSigma(String filename){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			for (int j = 0; j < J; j++){
				for (int k = 0; k < K; k++){
					writer.write(sigma[j][k] + " ");
				}
				writer.write("\n");
			}
			writer.close();
		}
		catch (Exception e){
			System.out.println("Error while saving cluster-topic distribution:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save model
	 */
	public boolean saveModel(String modelName){
		if (!saveModelTAssign(dir + File.separator + modelName + tassignSuffix)){
			return false;
		}
		
		if (!saveModelOthers(dir + File.separator + modelName + othersSuffix)){			
			return false;
		}

		if (twords > 0){
			if (!saveModelTwordsD(dir + File.separator + modelName + twordsSuffix))
				return false;

		}				
		
		if (modelName.equals("model-final")) {
			if (!saveModelThetaD(dir + File.separator + modelName + thetaSuffix)){
				return false;
			}

			if (!saveModelPhiD(dir + File.separator + modelName + phiSuffix)){
				return false;
			}

		}

		if (!saveModelSigma(dir + File.separator + modelName + sigmaSuffix)){
			return false;
		}


		//if (!saveModelMapRegion(dir + File.separator + modelName + mapRegionSuffix)){
		//		return false;
		//}


		return true;
	}

	//---------------------------------------------------------------
	//	Init Methods
	//---------------------------------------------------------------
	/**
	 * initialize the model
	 */
	protected boolean init(Option option){		
		if (option == null)
			return false;

		modelName = "model";
		K = 0;
		J =option.J;
		R = option.R;

		kactive = new ArrayList<Integer>();
		kgaps = new TreeSet<Integer>();

		if (option.beta >= 0)
			beta = option.beta;

		niters = option.niters;

		dir = option.dir;
		if (dir.endsWith(File.separator))
			dir = dir.substring(0, dir.length() - 1);

		dfile = option.dfile;
		twords = option.twords;
		rtopics = option.rtopics;
		wordMapFile = option.wordMapFileName;

		return true;
	}

	/**
	 * Init parameters for estimation
	 */
	public boolean initNewModel(Option option){
		if (!init(option))
			return false;


		data = LDADataset.readDataSet(dir + File.separator + dfile);
		if (data == null){
			System.out.println("Fail to read training data!\n");
			return false;
		}

		
//		Cluster1 kmeans = new Cluster1(data.docs, J, dir);


		int maxGroup = 0;
		for (int i=0; i < data.docs.length;i++) {
			if (data.docs[i].group > maxGroup) {
				maxGroup = data.docs[i].group;
			}
		}
		
		int[] groupSize = new int[maxGroup+1];
		for (int i=0; i < data.docs.length;i++) {
			groupSize[data.docs[i].group]++;
		}
				
		qd = new int[maxGroup][];
		for (int q = 0; q < groupSize.length;q++) {
			qd[q] = new int[groupSize[q]];
		}

		int[] count_q = new int[maxGroup];
		for (int i=0; i < data.docs.length;i++) {
			int q = data.docs[i].group;
			qd[q][count_q[q]++]=i;
		}
    	    	
//		//normalize pQ to sum to 1 over all neighbours
//		for (int j = 0;j<J;j++) {
//			//neighbours of q_j
//			int[] neighbour = qqN[j];
//			for(Iterator<Integer> iterator = qd.get(j).iterator();iterator.hasNext();){	
//				int m = iterator.next();
//				double sum = 0;
//				//first, add probability for own cluster
//				sum += pQ[m][j];
//				//next, add neighbours
//				for (int k=0;k<neighbour.length;k++) {
//					sum+=pQ[m][k];
//				}
//				//now normalize
//				pQ[m][j]=pQ[m][j]/sum;
//				if (sum == 0.0) {
//					for (int k=0;k<neighbour.length;k++) {
//						pQ[m][k]=1.0/neighbour.length;
//					}
//				}
//				else {
//					for (int k=0;k<neighbour.length;k++) {
//						pQ[m][k]=pQ[m][k]/sum;
//						//System.out.println(pQ[m][k]);
//					}
//				}
//			}
//		}

		//+ allocate memory and assign values for variables		
		M = data.M;
		V = data.V;
		dir = option.dir;
		savestep = option.savestep;

		length = new int[M];
		for (int m = 0; m < M; m++){
			length[m] = data.docs[m].length;
		}

		// K: from command line or default value
		// alpha, beta: from command line or default values
		// niters, savestep: from command line or default values

		nmk = new int[M][K];
		nkt = new int[K][V];
		nk = new int[K];

		kactive = new ArrayList<Integer>();
		kgaps = new TreeSet<Integer>();
		//region assignments for clusters
		y = new int[J];
		//topic assignments for words
		z = new int[M][];
		for (int m = 0; m < data.M; m++){
			z[m] = new int[length[m]];				
		}

		rho = new double[R][K+1];

		sigma = new double[J][K+1];

		//initialize delta



		tau = new double[K+1];		
		pi = new double[M][K+1];		
		phi = new double[K][V+1];

		return true;
	}

	/**
	 * Init parameters for inference
	 * @param newData DataSet for which we do inference
	 */
	public boolean initNewModel(Option option, LDADataset newData, Model trnModel){
		if (!init(option))
			return false;

		int m;

		K = trnModel.K;
		J = trnModel.J;
		R = trnModel.R;
		alpha0 = trnModel.alpha0;
//		for (int i = 0; i < J;i++) {
//			alphaq[i] = 1.;
//		}
		alphaq = 1;
		beta = trnModel.beta;

		kactive = new ArrayList<Integer>();
		kgaps = new TreeSet<Integer>();

		System.out.println("K:" + K);

		data = newData;

		//+ allocate memory and assign values for variables		
		M = data.M;
		V = data.V;
		dir = option.dir;
		savestep = option.savestep;
		System.out.println("M:" + M);
		System.out.println("V:" + V);

		// K: from command line or default value
		// alpha, beta: from command line or default values
		// niters, savestep: from command line or default values

		nkt = new int[V][K];


		nk = new int[K];

		y = new int[J];
		z = new int[M][];
		for (m = 0; m < data.M; m++){
			int N = data.docs[m].length;
			z[m] = new int[N];

			// total number of words in document i
			length[m] = N;
		}


		return true;
	}

	/**
	 * Init parameters for inference
	 * reading new dataset from file
	 */
	public boolean initNewModel(Option option, Model trnModel){
		if (!init(option))
			return false;

		LDADataset dataset = LDADataset.readDataSet(dir + File.separator + dfile, trnModel.data.dict);
		if (dataset == null){
			System.out.println("Fail to read dataset!\n");
			return false;
		}

		return initNewModel(option, dataset , trnModel);
	}

	/**
	 * init parameter for continue estimating or for later inference
	 */
	public boolean initEstimatedModel(Option option){
		if (!init(option))
			return false;

		int m, n, w;

		// load model, i.e., read z and trndata
		if (!loadModel()){
			System.out.println("Fail to load word-topic assignment file of the model!\n");
			return false;
		}

		System.out.println("Model loaded:");
		System.out.println("\talpha0:" + alpha0);
//		System.out.println("\talphaq:" + alphaq.toString());
		System.out.println("\talphaq:" + alphaq);
		System.out.println("\tbeta:" + beta);
		System.out.println("\tM:" + M);
		System.out.println("\tV:" + V);		

		nkt = new int[V][K];
		nk = new int[M];

		y = new int[J];

		for (m = 0; m < data.M; m++){
			int N = data.docs[m].length;

			// assign values for nw, nd, nwsum, and ndsum
			for (n = 0; n < N; n++){
				w = data.docs[m].words[n];
				int topic = z[m][n];

				// number of instances of word i assigned to topic j
				nkt[topic][w]++;
				// number of words in document i assigned to topic j
				nmk[m][topic]++;
				// total number of words assigned to topic j
				nk[topic]++;	    		
			}
			// total number of words in document i
			length[m] = N;
		}

		tau = new double[K+1];		
		rho = new double[R][K+1];		
		sigma = new double[J][K+1];		
		pi = new double[M][K+1];		
		phi = new double[K][V];
		dir = option.dir;
		savestep = option.savestep;

		return true;
	}

}
