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

package trash.WithRegionLevel;

import static java.lang.Math.log;

import java.io.File;
import java.util.Iterator;
import math.BasicMath;
import org.knowceans.util.DirichletEstimation;
import org.knowceans.util.Gamma;
import org.knowceans.util.RandomSamplers;

public class Estimator {

	// output model
	protected Model trnModel;

	//constant variables
	protected double Kalpha;
	protected double Vbeta;
	protected double Jzeta;
	protected Option option;
	protected int[] qLength;
	protected int[] randQ; //cluster assignments, randomly changing between q and its neighbours

	protected int T; //number of tables, calculated by updateTauSigmaPi()
	protected int[] Tr; //R , number of tables in region, calculated by updateTauSigmaPi()
	protected int[] Tj; //J , number of tables in cluster, calculated by updateTauSigmaPi()
	protected double[] nj;
	protected int[] mk;
	protected int[][] mrk;
	protected int[][] mjk;
	protected int sampleCount;
	protected boolean initialized;
	
	
	private RandomSamplers samp;
	private double[] p;


	public boolean init(Option option){

		this.option = option;
		trnModel = new Model();

		if (option.est){
			if (!trnModel.initNewModel(option))
				return false;
			trnModel.data.localDict.writeWordMap(option.dir + File.separator + trnModel.wordMapFile);
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

		trnModel.tau[trnModel.K] = 1.0;
		for (int r=0;r<trnModel.R;r++) {
			trnModel.rho[r][trnModel.K] = 1.0;
		}
		for (int j=0;j<trnModel.J;j++) {
			trnModel.sigma[j][trnModel.K] = 1.0;
		}

		trnModel.gamma = 10.;
		trnModel.alpha0 = 10.;
		trnModel.alphar = new double[trnModel.R];
		for (int i = 0; i < trnModel.R;i++) {
			trnModel.alphar[i] = 1.;
		}
		trnModel.alphaq = new double[trnModel.J];
		for (int i = 0; i < trnModel.J;i++) {
			trnModel.alphaq[i] = 1.;
		}
		//set random cluster assignment to cluster from q
		randQ = trnModel.q;

		trnModel.gammaa = 10.0;
		trnModel.gammab = 0.1;
		trnModel.alpha0a = 10.0;
		trnModel.alpha0b = 0.1;
		trnModel.alphara = 10.0;
		trnModel.alpharb = 0.1;
		trnModel.alphaqa = 0.1;
		trnModel.alphaqb = 0.1;

		trnModel.betaa = 0.1;
		trnModel.betab = 0.1;
		
		Tr = new int[trnModel.R];
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


		int lastIter = trnModel.liter;
		for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters; trnModel.liter++){
			System.out.println("Iteration " + trnModel.liter + " ");
			
			//randomly assign cluster, choosing q or one of its neighbours
			setCluster();

			// for all z_i
			for (int m = 0; m < trnModel.M; m++){			
				for (int n = 0; n < trnModel.length[m]; n++){
					// sample from p(z_i|z_-i, w)
					sampling(m, n);

				}// end for each word
			}// end for each document
			System.out.print(".");

			updatePrior();

			System.out.print(".");

			if (trnModel.liter > 10) {
				updateHyper();
				//assign regions to clusters
				sampleRegions();
			}

			System.out.print(".");

			initialized = true;

		}// end iterations		

		System.out.println("Gibbs sampling completed!\n");
		System.out.println("Saving the final model!\n");
		sampleCount++;
		computePhi();
		updatePrior();
		trnModel.liter--;
		trnModel.saveModel("model-final");
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
		int cluster = randQ[m];
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
			p[k] = (trnModel.alphaq[cluster]*trnModel.sigma[cluster][k] + trnModel.nmk[m][kk]) * 
					((trnModel.nkt[kk][w] + trnModel.beta)/(trnModel.nk[kk] + Vbeta));
		}
		p[trnModel.K]=trnModel.alphaq[cluster]*
				(trnModel.sigma[cluster][trnModel.K]/trnModel.V);

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
				System.out.println("k " + topicNew + " kk " + kk + " p " + p[topicNew] + " alphaq " + trnModel.alphaq[cluster] + " sigma " + trnModel.sigma[cluster][topicNew] +" nmk " + trnModel.nmk[m][kk] + " nkt " +trnModel.nkt[kk][w] + " nk " +trnModel.nk[kk]);
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
			double[][] rhoOld = trnModel.rho;
			trnModel.rho=new double[trnModel.R][trnModel.K+1];
			for(int r=0;r<trnModel.R;r++) {
				System.arraycopy(rhoOld[r], 0, trnModel.rho[r], 0,trnModel.K);
			}
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

		// gamma: root level (Escobar+West95) with n = T
		trnModel.gamma=samp.randConParam(trnModel.gamma, T, trnModel.K, trnModel.gammaa, trnModel.gammab, 20);


		//sample 20 times
		for (int iter = 0; iter < 20; iter++) {


			// alpha: document level (Teh+06)
			double qs = 0;
			double qw = 0;

			double[] nr = new double[trnModel.R];
			for (int m = 0; m < trnModel.M; m++) {				
				int cluster = randQ[m];
				int region = trnModel.y[cluster];
				for (int k=0;k<trnModel.K;k++) {
					nr[region]+= trnModel.length[m];
				}								
			}
			for (int r = 0; r < trnModel.R; r++) {			

				// (49) (corrected)
				qs += samp.randBernoulli(nr[r] / (nr[r] + trnModel.alpha0));
				// (48)
				qw += log(samp.randBeta(trnModel.alpha0 + 1, nr[r]));

			}
			// (47)
			trnModel.alpha0 = samp.randGamma(trnModel.alpha0a + T - qs, 1. / (trnModel.alpha0b - qw));
		}

		//alphar  region level (Escobar+West95) with n = T
		for (int r=0;r<trnModel.R;r++) {
			trnModel.alphar[r]=samp.randConParam(trnModel.alphar[r], Tr[r], trnModel.K, trnModel.alphara, trnModel.alpharb, 20);
		}
		
		//alphaq cluster level (Escobar+West95) with n = T
		for (int j=0;j<trnModel.J;j++) {
			trnModel.alphaq[j]=samp.randConParam(trnModel.alphaq[j], Tj[j], trnModel.K, trnModel.alphaqa, trnModel.alphaqb, 20);
		}


		trnModel.beta = DirichletEstimation
				.estimateAlphaMap(trnModel.nkt, trnModel.nk, trnModel.beta, trnModel.betaa, trnModel.betab);

		System.out.println();
		System.out.println("hyper:");
		System.out.println(trnModel.gamma);
		System.out.println(trnModel.alpha0);
		System.out.println(trnModel.alphar[0]);
		System.out.println(trnModel.alphaq[0]);


	}

	public void sampleRegions() { 

		int[] countR = new int[trnModel.R];
		
		for (int j = 0; j < trnModel.J; j++) {
			int[] njk = new int[trnModel.K];
			for(Iterator<Integer> iterator = trnModel.qd.get(j).iterator();iterator.hasNext();){	
				int m = iterator.next();
				for (int k = 0; k < trnModel.K; k++){
					njk[k]+=trnModel.nmk[m][k];
				}
			}

			double maxLik = -Double.MAX_VALUE;
			double[] lik = new double[trnModel.R];
			for (int r = 0; r < trnModel.R; r++) {
				for (int k = 0; k < trnModel.K; k++){
					lik[r] += mjk[j][k] * Math.log(trnModel.alphar[r]*trnModel.rho[r][k]) ;
				}
				maxLik = Math.max(maxLik, lik[r]);
			}
			
			double logsum = 0.;
			
			for (int r = 0; r < trnModel.R; r++) {
				System.out.println(lik[r] - maxLik);
				logsum = Math.exp(lik[r]-maxLik) + logsum;
				lik[r] = Math.log(maxLik + Math.log(logsum));
			}
			

			double rand = Math.log(Math.random() * lik[trnModel.R-1]);
			for (int r = 0; r < trnModel.R; r++){
				if (lik[r] > rand) { //sample region r w.r.t distribution lik
					trnModel.y[j]=r;
					countR[r]++;
					break;
				}
					
			}
			
		}

		
		for (int i=0;i< trnModel.R;i++) {
			System.out.println("Region " + i + ": " + countR[i]);
		}


	}


	public void updatePrior(){
		//sample tables for all levels in the model
		sampleTables();

		updateTau();
		updateRho();
		updateSigma();
		//updatePi();
	}

	public void sampleTables() {

		T=0;
		//sample mk using method by teh

		mk = new int[trnModel.K];
		mrk = new int[trnModel.R][trnModel.K + 1];
		mjk = new int[trnModel.J][trnModel.K + 1];

		for (int j = 0; j < trnModel.J; j++) {

			double[] weight = new double[trnModel.K];
			for (int k = 0; k < trnModel.K; k++){
				weight[k] = trnModel.sigma[j][k]*trnModel.alphaq[j];
			}

			for(Iterator<Integer> iterator = trnModel.qd.get(j).iterator();iterator.hasNext();){	
				int m = iterator.next();

				for (int k = 0; k < trnModel.K; k++){
					int kk =  trnModel.kactive.get(k);
					mjk[j][k]+=samp.randNumTable(weight[k],trnModel.nmk[m][kk]);
				}
			}
			Tj[j] = BasicMath.sum(mjk[j]);
		}


		for (int j = 0; j < trnModel.J; j++) {
			int region = trnModel.y[j];
			for (int k = 0; k < trnModel.K; k++){
				mrk[region][k]+=samp.randNumTable(trnModel.rho[region][k]*trnModel.alphar[region],mjk[j][k]);
			}
		}
		//sum over tables for region in next loop

		for (int r = 0; r < trnModel.R; r++) {
			Tr[r] = BasicMath.sum(mrk[r]);
			
			for (int k = 0; k < trnModel.K; k++){
				mk[k]+=samp.randNumTable(trnModel.tau[k]*trnModel.alpha0,mrk[r][k]);
			}
		}
		T = BasicMath.sum(mk);
		

		//System.out.println(T + " " + Tr[0] + " " + Tj[0]);
		
	}

	public void updateTau(){


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

	public void updateRho(){

		double[][] alpha0TauMrk = new double[trnModel.R][trnModel.K + 1];

		for (int r = 0; r < trnModel.R; r++) {
			for (int k = 0; k < trnModel.K; k++) {
				alpha0TauMrk[r][k]=trnModel.tau[k]*trnModel.alpha0 + mrk[r][k];
			}
			alpha0TauMrk[r][trnModel.K] = trnModel.tau[trnModel.K]*trnModel.alpha0;
		}

		for (int r = 0; r < trnModel.R; r++) {
			//sample sigma 			
			trnModel.rho[r] = samp.randDir(alpha0TauMrk[r]);
		}

	}

	public void updateSigma(){

		double[][] alpharRhoMjk = new double[trnModel.J][trnModel.K + 1];

		for (int j = 0; j < trnModel.J; j++) {
			int region = trnModel.y[j];
			for (int k = 0; k < trnModel.K; k++) {
				alpharRhoMjk[j][k]=trnModel.rho[region][k]*trnModel.alphar[region] + mjk[j][k];
			}
			alpharRhoMjk[j][trnModel.K] = trnModel.rho[region][trnModel.K]*trnModel.alphar[region];
		}

		for (int j = 0; j < trnModel.J; j++) {
			//sample sigma 			
			trnModel.sigma[j] = samp.randDir( alpharRhoMjk[j]);
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


	//set cluster for each document m to q[m] or one of its neighbours
	public void setCluster() {

		for (int j = 0;j<trnModel.J;j++) {
			//chose neighbours of q or q itself
			int[] neighbour = trnModel.qqN[j];
			int size = neighbour.length;

			for(Iterator<Integer> iterator = trnModel.qd.get(j).iterator();iterator.hasNext();){	
				int m = iterator.next();
				//chose one of the neighbours or q
				int choice = (int) Math.floor(Math.random()*(size+1));
				if (choice == size) {
					randQ[m]=trnModel.q[m];
				}
				else {
					randQ[m]=neighbour[choice];
				}
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

}
