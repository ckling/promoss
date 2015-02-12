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

import java.io.File;
import java.util.Iterator;
import ckling.math.BasicMath;

import org.knowceans.util.DirichletEstimation;
import org.knowceans.util.RandomSamplers;

import jgibblda.Model;

public class Estimator {

	// output model
	protected Model trnModel;

	//constant variables
	protected double Kalpha;
	protected double Vbeta;
	protected double Jzeta;
	protected Option option;
	protected int[] qLength;
	protected int[] clusterm; //cluster assignments, randomly changing between q and its neighbours

	protected int T; //number of tables, calculated by updateTauSigmaPi()
	protected int[] Tj; //J , number of tables in cluster, calculated by updateTauSigmaPi()
	protected double[] nj;
	protected int[][] mjk; //cluster table count
	protected int[][] mdk; //doc table count
	protected int[] nmkNew; //for testing phase, save counts for new topic
	protected int[][] lengthj;
	protected int[] countj;
	protected int sampleCount;
	protected boolean initialized;
	protected int trainingLength;

	private RandomSamplers samp;
	private double[] p;

	public double perplexity=0.0;
	public double perplexityLoc=0.0;
	public int K;


	public boolean init(Option option){

		this.option = option;
		trnModel = new Model();

		if (option.est){
			if (!trnModel.initNewModel(option))
				return false;
			trnModel.data.dict.writeWordMap(option.dir + File.separator + trnModel.wordMapFile);
		}
		else if (option.estc){
			if (!trnModel.initEstimatedModel(option))
				return false;
		}

		return true;
	}

	public void estimate(){
		System.out.println("Sampling " + trnModel.niters + " iteration!");

		//load sampler
		samp = new RandomSamplers();

		//setting constants
		Vbeta = trnModel.V * trnModel.beta;

		trnModel.tau[trnModel.K] = 1;
		for (int j=0;j<trnModel.J;j++) {
			trnModel.sigma[j][trnModel.K] = 1;
		}

		//		double factor = 1./10.;
		//		double factorBeta = trnModel.beta/0.1;


		trnModel.gamma = 1.0;
		trnModel.alpha0 = 1.0;
		//		trnModel.alphaq = new double[trnModel.J];
		//		for (int i = 0; i < trnModel.J;i++) {
		//			trnModel.alphaq[i] = 1.0;
		//		}
		trnModel.alphaq = 1.0;
		//set cluster assignment to generate a random choice between home cluster and neighbour clusters
		clusterm=trnModel.q;

		//		trnModel.gammaa = 5.0;
		//		trnModel.gammab = 0.1;
		//		trnModel.alpha0a = 5.0;
		//		trnModel.alpha0b = 0.1;
		//		trnModel.alphaqa = 0.1;
		//		trnModel.alphaqb = 0.1;

		trnModel.gammaa = 1.0;
		trnModel.gammab = 0.1;
		trnModel.alpha0a = 1.0;
		trnModel.alpha0b = 1.0;
		trnModel.alphaqa = 1.0;
		trnModel.alphaqb = 1.0;

		//attention, high values for car dataset only, for other cases 0.1 (sparse)
		trnModel.betaa = 0.1;
		trnModel.betab = 0.1;

		Tj = new int[trnModel.J];

		double[] nj = new double[trnModel.J];
		for (int m = 0; m < trnModel.M; m++) {
			for (int k=0;k<trnModel.K;k++) {
				nj[trnModel.q[m]]+= trnModel.length[m];
			}
		}


		p = new double[trnModel.K+1];

		//setting temporary variables for sampling the estimate
		sampleCount = 0;

		qLength = new int[trnModel.J];
		for (int m = 0; m < trnModel.M; m++){			
			for (int n = 0; n < trnModel.length[m]; n++){
				qLength[trnModel.q[m]]++;
			}
		}

		//sample training data - might be the whole data
		trainingLength =  (int) (trnModel.trainingSize*trnModel.M);

		int lastIter = trnModel.liter;
		for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters; trnModel.liter++){
			System.out.println("Iteration " + trnModel.liter + " ");

			//randomly assign cluster, choosing q or one of its neighbours
			setCluster();

			// for all z_i
			for (int m = 0; m < trainingLength; m++){			
				for (int n = 0; n < trnModel.length[m]; n++){
					// sample from p(z_i|z_-i, w)
					sampling(m, n);

				}// end for each word
			}// end for each document
			System.out.print(".");

			updatePrior();

			System.out.print(".");

			if (trnModel.liter > 10 && trnModel.liter%1 == 0) {
				updateHyper();
			}
			//			if (trnModel.liter > 10 && trnModel.liter%10 == 0) {
			//				computePhi();
			//				getPerplexity();
			//			}

			System.out.print(".");

			initialized = true;

		}// end iterations		

		System.out.println("Gibbs sampling completed!\n");
		
		if (1==1) {
		System.out.println("Saving the final model!\n");
		sampleCount++;
		//setBaseCluster();
		computePhi();
		updatePrior();
		trnModel.liter--;
		trnModel.saveModel("model-final");
//
//		//if training size < 100%, calculate perplexite with rest
//		if (trnModel.trainingSize != 1.0) {
//			getPerplexityPlaceKnown();
//			getPerplexity();
//		}
		
		}

	}

	/**
	 * Do sampling
	 * @param m document number
	 * @param n word number
	 * @return topic id
	 */
	public void sampling(int m, int n){
		// remove z_i from the count variable
		int w = trnModel.data.docs[m].words[n];
		int cluster = clusterm[m];
		int topic;

		if (initialized) {
			topic = trnModel.z[m][n];
			trnModel.nmk[m][topic]--;
			trnModel.nkt[topic][w]--;
			trnModel.nk[topic]--;
		}
		else {
			topic = -1;
		}


		//sampling for document topics
		for (int k = 0; k < trnModel.K; k++){
			int kk = trnModel.kactive.get(k);
			p[k] = (trnModel.alphaq*trnModel.sigma[cluster][k] + trnModel.nmk[m][kk]) * 
					((trnModel.nkt[kk][w] + trnModel.beta)/(trnModel.nk[kk] + Vbeta));
		}
		p[trnModel.K]=trnModel.alphaq*trnModel.sigma[cluster][trnModel.K] / trnModel.V;

		//accumulate
		for (int k = 1; k < trnModel.K+1; k++){
			p[k] += p[k-1];
		}



		// scaled sample because of unnormalized p[]
		double u = Math.random() * p[trnModel.K];

		int topicNew = -1;
		for (topicNew = 0; topicNew < trnModel.K+1; topicNew++){
			if (p[topicNew] > u) //sample topic w.r.t distribution p
				break;
		}

		//Debug
		if (topicNew > trnModel.K) {
			System.out.println("error in sampling topic, printing p");
			for (topicNew = 0; topicNew < trnModel.K+1; topicNew++){
				int kk = trnModel.kactive.get(topicNew);
				System.out.println("k " + topicNew + " kk " + kk + " p " + p[topicNew] + " alphaq " + trnModel.alphaq + " sigma " + trnModel.sigma[cluster][topicNew] +" nmk " + trnModel.nmk[m][kk] + " nkt " +trnModel.nkt[kk][w] + " nk " +trnModel.nk[kk]);
			}
		}

		if (topicNew == trnModel.K) {
			addTopic(m,n,w);
		}
		else {
			int kk = trnModel.kactive.get(topicNew);
			trnModel.nmk[m][kk]++;
			trnModel.nkt[kk][w]++;
			trnModel.nk[kk]++;
			trnModel.z[m][n]=kk;
		}

		if (initialized && trnModel.nk[topic] == 0) {
			rmTopic(topic);
		}




		//System.out.println(m + " "+ trnModel.z.length + " " + n  + " " + trnModel.z[m].size());
		//update z vector

		// add newly estimated z_i to count variables

		//trnModel.nqz[cluster][topicNew]++;
		//trnModel.nqzsum[topicNew]++;

		return;
	}

	/**
	 * Do sampling in test phase
	 * @param m document number
	 * @param n word number
	 * @return topic id
	 */
	public void samplingTest(int m, int n){
		// remove z_i from the count variable
		int w = trnModel.data.docs[m].words[n];
		int cluster = clusterm[m];
		int topic;

		if (initialized) {
			topic = trnModel.z[m][n];
			if(topic == -1) {
				nmkNew[m]--;
			}
			else {
				trnModel.nmk[m][topic]--;
			}
		}
		else {
			topic = -1;
		}


		//sampling for testing document topics
		for (int k = 0; k < trnModel.K; k++){
			int kk = trnModel.kactive.get(k);
			p[k] = (trnModel.alphaq*trnModel.sigma[cluster][k] + trnModel.nmk[m][kk]) * 
					((trnModel.nkt[kk][w] + trnModel.beta)/(trnModel.nk[kk] + Vbeta));
		}
		p[trnModel.K]= (trnModel.alphaq*trnModel.sigma[cluster][trnModel.K] + nmkNew[m]) / trnModel.V;

		//accumulate
		for (int k = 1; k < trnModel.K+1; k++){
			p[k] += p[k-1];
		}



		// scaled sample because of unnormalized p[]
		double u = Math.random() * p[trnModel.K];

		int topicNew = -1;
		for (topicNew = 0; topicNew < trnModel.K+1; topicNew++){
			if (p[topicNew] > u) //sample topic w.r.t distribution p
				break;
		}

		//Debug
		if (topicNew > trnModel.K) {
			System.out.println("error in sampling topic, printing p");
			for (topicNew = 0; topicNew < trnModel.K+1; topicNew++){
				int kk = trnModel.kactive.get(topicNew);
				System.out.println("k " + topicNew + " kk " + kk + " p " + p[topicNew] + " alphaq " + trnModel.alphaq + " sigma " + trnModel.sigma[cluster][topicNew] +" nmk " + trnModel.nmk[m][kk] + " nkt " +trnModel.nkt[kk][w] + " nk " +trnModel.nk[kk]);
			}
		}

		if (topicNew == trnModel.K) {
			nmkNew[m]++;
			trnModel.z[m][n]=-1;
		}
		else {
			int kk = trnModel.kactive.get(topicNew);
			trnModel.nmk[m][kk]++;
			trnModel.z[m][n]=kk;
		}

		//System.out.println(m + " "+ trnModel.z.length + " " + n  + " " + trnModel.z[m].size());
		//update z vector

		// add newly estimated z_i to count variables

		//trnModel.nqz[cluster][topicNew]++;
		//trnModel.nqzsum[topicNew]++;

		return;
	}

	/*
	 * add topic, increase count for document m and term t
	 */
	public void addTopic(int m, int n, int t) {

		System.out.println("adding topic " + trnModel.K + " for doc " + m);

		if (trnModel.kgaps.isEmpty()) {
			trnModel.K++;
			//create new topic with number K / index K-1
			trnModel.kactive.add(trnModel.K-1);
			trnModel.z[m][n]=trnModel.K-1;

			p=new double[trnModel.K+1];

			//expand phi,pi,sigma,tau,nkt,nk,nmk
			int[][] nmkOld = trnModel.nmk;
			trnModel.nmk=new int[trnModel.M][trnModel.K];
			for (int mm=0;mm<trnModel.M;mm++) {
				System.arraycopy(nmkOld[mm], 0, trnModel.nmk[mm], 0,trnModel.K-1);	
			}
			int[] nkOld = trnModel.nk;
			trnModel.nk=new int[trnModel.K];
			System.arraycopy(nkOld, 0, trnModel.nk, 0,trnModel.K-1);	
			int[][] nktOld = trnModel.nkt;
			trnModel.nkt=new int[trnModel.K][trnModel.V];
			System.arraycopy(nktOld, 0, trnModel.nkt, 0,trnModel.K-1);

			trnModel.nmk[m][trnModel.K-1]=1;
			trnModel.nkt[trnModel.K-1][t]=1;
			trnModel.nk[trnModel.K-1]=1;		

			double[] tauOld = trnModel.tau;
			trnModel.tau=new double[trnModel.K+1];
			System.arraycopy(tauOld, 0, trnModel.tau, 0,trnModel.K);
			double[][] sigmaOld = trnModel.sigma;
			trnModel.sigma=new double[trnModel.J][trnModel.K+1];
			for(int j=0;j<trnModel.J;j++) {
				System.arraycopy(sigmaOld[j], 0, trnModel.sigma[j], 0,trnModel.K);
			}
			double[][] piOld = trnModel.pi;
			trnModel.pi=new double[trnModel.M][trnModel.K+1];
			for(int mm=0;mm<trnModel.M;mm++) {
				System.arraycopy(piOld[mm], 0, trnModel.pi[mm], 0,trnModel.K);
			}

		}
		else {
			int newk =  trnModel.kgaps.first();
			trnModel.kgaps.remove(newk);
			trnModel.kactive.add(newk);
			trnModel.nmk[m][newk]=1;
			trnModel.nkt[newk][t]=1;
			trnModel.nk[newk]=1;
			trnModel.K++;
			trnModel.z[m][n]=newk;
		}

		updatePrior();

	}

	public void rmTopic(int kk) {

		System.out.println("removing topic " + kk + " from " +  trnModel.K + " topics");

		int k;
		for (k = 0; k < trnModel.K; k++){
			if (trnModel.kactive.get(k)==kk) break;
		}
		trnModel.kgaps.add(kk);
		trnModel.kactive.remove(k);
		trnModel.K--;
		updatePrior();
	}

	public void updateHyper() {

		/**das muss 
		 * TODO
		 * das muss für alpha0 sein, für alphaq: length und mjk!!
		 */

		lengthj = new int[trnModel.J][];

		for (int j = 0; j < trnModel.J; j++) {
			lengthj[j] = new int[countj[j]];
		}

		//index counter for each cluster
		int[] docIndex = new int[trnModel.J];

		for (int m=0; m < trainingLength; m++) {
			int cluster = clusterm[m];
			lengthj[cluster][docIndex[cluster]++] = trnModel.length[m];
		}

		//wir brauchen: Summe aller tische in allen regionen, außerdem anzahl der untertische aus dokumenten.

		trnModel.alphaq= samp.randConParam(trnModel.alphaq,Tj, T, trnModel.alphaqa, trnModel.alphaqb, 20);



		int[] nk = new int[trnModel.J];
		for (int j = 0; j < trnModel.J; j++) {	
			for (int k = 0; k < trnModel.K; k++){
				nk[j]+= mjk[j][k];
			}
		}


		trnModel.alpha0=samp.randConParam(trnModel.alpha0, nk, T, trnModel.alpha0a, trnModel.alpha0b, 20);

		// gamma: root level (Escobar+West95) with n = T
		trnModel.gamma=samp.randConParam(trnModel.gamma, T, trnModel.K, trnModel.gammaa, trnModel.gammab, 20);

		//		for (int r = 0; r < 20; r++) {
		//		double eta = samp.randBeta(trnModel.gamma + 1, T);
		//		double bloge = trnModel.gammab - log(eta);
		//		// (13')
		//		double pie = 1. / (1. + (T * bloge / (trnModel.gammaa + trnModel.K - 1)));
		//		// (13)
		//		int u = samp.randBernoulli(pie);
		//		trnModel.gamma = samp.randGamma(trnModel.gammaa + trnModel.K - 1 + u, 1. / bloge);
		//		}
		//		
//		trnModel.beta = DirichletEstimation
//				.estimateAlphaMap(trnModel.nkt, trnModel.nk, trnModel.beta, trnModel.betaa, trnModel.betab);

		System.out.println();
		System.out.println("K: " + trnModel.K);
		System.out.println("hyper:");
		System.out.println(trnModel.gamma);
		System.out.println(trnModel.alpha0);
		System.out.println(trnModel.alphaq);
		System.out.println(trnModel.beta);


	}


	public void updatePrior(){
		updateTau();
		updateSigma();
		//updatePi();
	}

	public void updateTau(){

		//sample mk using method by teh

		int[] mk = new int[trnModel.K];
		mjk = new int[trnModel.J][trnModel.K];
		mdk = new int[trnModel.M][trnModel.K];

		double[][] weight = new double[trnModel.J][trnModel.K];
		for (int j = 0; j < trnModel.J; j++) {
			for (int k = 0; k < trnModel.K; k++){
				weight[j][k] = trnModel.sigma[j][k]*trnModel.alphaq;
			}
		}

		for(int m=0;m<trainingLength;m++) {
			int cluster = clusterm[m];
			for (int k = 0; k < trnModel.K; k++){
				int kk =  trnModel.kactive.get(k);
				int tables = samp.randNumTable(weight[cluster][k],trnModel.nmk[m][kk]);
				mjk[cluster][k]+=tables;
				mdk[m][k]+=tables;
			}
		}

		for (int j = 0; j < trnModel.J; j++) {

			Tj[j] = BasicMath.sum(mjk[j]);			

			for (int k = 0; k < trnModel.K; k++){
				mk[k]+=samp.randNumTable(trnModel.tau[k]*trnModel.alpha0,mjk[j][k]);
			}
		}
		T = BasicMath.sum(mk);



		/*
		// (40) sample mk
		double[] mk = new double[trnModel.K + 1];
		mjk = new double[trnModel.J][trnModel.K + 1];
		for (int j = 0; j < trnModel.J; j++) {
			for(Iterator<Integer> iterator = trnModel.qd.get(j).iterator();iterator.hasNext();){
				int m = iterator.next();
				for (int k = 0; k < trnModel.K; k++) {
					int kk =  trnModel.kactive.get(k);
					if (trnModel.nmk[m][kk] > 1) {
						mjk[j][k] += samp.randAntoniak(trnModel.alphaq[j] * trnModel.tau[k], trnModel.nmk[m][kk]);
					}
					else{
						if (trnModel.nmk[m][kk] == 1) 
							mjk[j][k] += trnModel.nmk[m][kk];
					}
				}
			}
			for (int k = 0; k < trnModel.K; k++) {
				mk[k]+=mjk[j][k];
			}
			// number of tables in cluster j
			Tj[j] = BasicMath.sum(mjk[j]);
			// number of tables
			T+= Tj[j];
		}
		 */



		double[] sampleMkGamma = new double[trnModel.K+1];
		for (int k = 0; k < trnModel.K; k++){
			sampleMkGamma[k]=mk[k];
		}
		sampleMkGamma[trnModel.K] = trnModel.gamma;

		// (36) sample tau
		trnModel.tau = samp.randDir(sampleMkGamma);				

		/*
				for (int k = 0; k < trnModel.K+1; k++) {
					System.out.println("table " +k +": "+mk[k]);
				} 
				//Debug: print tau
				System.out.print("\n tau ");
				for(int i = 0; i < trnModel.K+1;i++) {
					System.out.print(trnModel.tau[i]+" ");
				}
				System.out.println();
		 */



	}

	public void updateSigma(){

		double[][] alpha0TauMjk = new double[trnModel.J][trnModel.K + 1];

		for (int j = 0; j < trnModel.J; j++) {
			for (int k = 0; k < trnModel.K; k++) {
				alpha0TauMjk[j][k]=trnModel.tau[k]*trnModel.alpha0 + mjk[j][k];
				if (!initialized) {
					alpha0TauMjk[j][k]++;
				}
			}
			alpha0TauMjk[j][trnModel.K] = trnModel.tau[trnModel.K]*trnModel.alpha0;
		}

		for (int j = 0; j < trnModel.J; j++) {
			//sample sigma 			
			trnModel.sigma[j] = samp.randDir(alpha0TauMjk[j]);
			//check if sigma is 0 for some k
			boolean isZero = false;
			for (int k = 0; k < trnModel.K; k++) {
				if (trnModel.sigma[j][k]==0) {
					isZero = true;
					break;
				}
			}
			//repair sigma with 0 values, add a tiny bit and normalise
			if (isZero) {
				double sum = BasicMath.sum(trnModel.sigma[j]) + trnModel.K * Double.MIN_VALUE;
				for (int k = 0; k < trnModel.K; k++) {
					trnModel.sigma[j][k]=(trnModel.sigma[j][k]+Double.MIN_VALUE)/sum;
				}
			}
		}


		/*
				//Debug: print tau
				System.out.print("\n tau ");
				for(int i = 0; i < trnModel.K+1;i++) {
					System.out.print(trnModel.tau[i]+" ");
				}
				System.out.print("\n sigma ");
				for (int j = 0; j < trnModel.J; j++) {
					for (int k = 0; k < trnModel.K; k++) {
						if (trnModel.sigma[j][k] < 0)
							System.out.print(trnModel.sigma[j][k] + " ");
					}
				}
				System.out.println();
		 */



	}

	public void updatePi(){
		//not neccessary for sampling, ~Dir(n_j+alpha_q*sigma_q)
	}

	public void setCluster () {
		setBaseCluster();
	}

	//set cluster assignment for all documents to base cluster
	public void setBaseCluster () {	
		countj = new int[trnModel.J];
		for (int j = 0;j<trnModel.J;j++) {
			int[] docs = trnModel.qd[j];
			for (int doc:docs) {
				clusterm[doc]=j;
				countj[j]++;
			}
		}

	}



	public void computePhi(){
		trnModel.phi = new double[trnModel.K][trnModel.V];
		for (int k = 0; k < trnModel.K; k++){
			for (int w = 0; w < trnModel.V; w++){
				int kk =  trnModel.kactive.get(k);
				trnModel.phi[k][w] =(trnModel.nkt[kk][w] + trnModel.beta) 
						/ (trnModel.nk[kk] + trnModel.V * trnModel.beta);
			}
		}
	}


	//p(r|l) = 1/(N+1) for base and neighbour clusters
	public void getPerplexityPlaceKnown() {

		//sample test data starting after training data
		trainingLength =  (int) (trnModel.trainingSize*trnModel.M);

		//initialise variable for new topic counts
		nmkNew = new int[trnModel.M];
		//reset topic counts for training set to 0 (in case there is another perplexity calculated before)
		for (int m = trainingLength; m < trnModel.M; m++){
			for (int k = 0; k < trnModel.K; k++){
				int kk = trnModel.kactive.get(k);
				trnModel.nmk[m][kk]=0;
			}
		}

		//first, sample topics for the test documents, leaving all other parameters unchanged!
		//sample as often as training data
		initialized = false;
		for (int r = 0; r < trnModel.niters; r++){

			setCluster();
			// for all z_i
			for (int m = trainingLength; m < trnModel.M; m++){			
				for (int n = 0; n < trnModel.length[m]; n++){
					// sample from p(z_i|z_-i, w)
					samplingTest(m, n);
				}// end for each word
			}// end for each document
			initialized = true;
		}

		double logLik = 0;
		int wordCount = 0;

		for (int m = trainingLength; m < trnModel.M; m++){

			wordCount += trnModel.length[m];

			int cluster = clusterm[m];
			//set no neighbours, cluster is just the base cluster
			//int[] neighbour = new int[0];
			double[] rho = new double[trnModel.K+1];

			//calculate rho

			//rho is the topic distribution for a document given cluster j

			//calculate theta, the topic distribution, for each topic
			for (int k = 0; k < trnModel.K; k++){
				int kk = trnModel.kactive.get(k);
				rho[k] = (trnModel.alphaq*trnModel.sigma[cluster][k] + trnModel.nmk[m][kk]);
			}
			rho[trnModel.K]=trnModel.alphaq*trnModel.sigma[cluster][trnModel.K] + nmkNew[m];

			//normalise tau
			double rhoSum = BasicMath.sum(rho);
			for (int k = 0; k < trnModel.K+1; k++){ 
				rho[k] /= rhoSum;
			}


			//for each cluster (base or neighbours)


			//			int j= neighbour.length;
			for (int n = 0; n < trnModel.length[m]; n++){
				//get probability for each word
				int w = trnModel.data.docs[m].words[n];


				double sum = 0;
				for (int k = 0; k < trnModel.K+1; k++){
					if (k < trnModel.K) {
						sum += rho[k] * trnModel.phi[k][w];
					}
					else {
						//phi is 1/V for new topics
						sum += rho[k] * (1.0 / trnModel.V);
					}
				}

				logLik += Math.log(sum);

			}// end for each word



		}//end for each document

		perplexityLoc = Math.exp(-(logLik / wordCount));
		K = trnModel.K;

		System.out.println("Perplexity place known: "+perplexityLoc);

	}

	public void getPerplexity() {

		perplexity = 0;
		K = trnModel.K;

		
		//
		//sample test data starting after training data
		trainingLength =  (int) (trnModel.trainingSize*trnModel.M);

		//initialise variable for new topic counts
		nmkNew = new int[trnModel.M];
		//reset topic counts for training set to 0 (in case there is another perplexity calculated before)
		for (int m = trainingLength; m < trnModel.M; m++){
			for (int k = 0; k < trnModel.K; k++){
				int kk = trnModel.kactive.get(k);
				trnModel.nmk[m][kk]=0;
			}
		}

		//first, sample topics for the test documents, leaving all other parameters unchanged!
		//sample as often as training data
		initialized = false;
		for (int r = 0; r < trnModel.niters; r++){
			// for all z_i
			for (int m = trainingLength; m < trnModel.M; m++){			
				//set cluster to random cluster
				clusterm[m]=(int) (Math.random() * trnModel.J);
				for (int n = 0; n < trnModel.length[m]; n++){
					// sample from p(z_i|z_-i, w)
					samplingTest(m, n);
				}// end for each word
			}// end for each document
			initialized = true;
		}

		double logLik = 0;
		int wordCount = 0;

		for (int m = trainingLength; m < trnModel.M; m++){

			wordCount += trnModel.length[m];

			int cluster = clusterm[m];
			//set no neighbours, cluster is just the base cluster
			//int[] neighbour = new int[0];
			double[] rho = new double[trnModel.K+1];

			//calculate rho

			//rho is the topic distribution for a document given cluster j

			//calculate theta, the topic distribution, for each topic
			for (int k = 0; k < trnModel.K; k++){
				int kk = trnModel.kactive.get(k);
				rho[k] = (trnModel.alphaq*trnModel.sigma[cluster][k] + trnModel.nmk[m][kk]);
			}
			rho[trnModel.K]=trnModel.alphaq*trnModel.sigma[cluster][trnModel.K] + nmkNew[m];

			//normalise tau
			double rhoSum = BasicMath.sum(rho);
			for (int k = 0; k < trnModel.K+1; k++){ 
				rho[k] /= rhoSum;
			}


			//for each cluster (base or neighbours)


			//			int j= neighbour.length;
			for (int n = 0; n < trnModel.length[m]; n++){
				//get probability for each word
				int w = trnModel.data.docs[m].words[n];


				double sum = 0;
				for (int k = 0; k < trnModel.K+1; k++){
					if (k < trnModel.K) {
						sum += rho[k] * trnModel.phi[k][w];
					}
					else {
						//phi is 1/V for new topics
						sum += rho[k] * (1.0 / trnModel.V);
					}
				}

				logLik += Math.log(sum);

			}// end for each word



		}//end for each document

		perplexity = Math.exp(-(logLik / wordCount));
		K = trnModel.K;

		System.out.println("Perplexity place unknown: "+perplexity);
	}


}
