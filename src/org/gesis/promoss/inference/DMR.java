package org.gesis.promoss.inference;


import org.gesis.promoss.inference.LBFGS.Function;
import org.gesis.promoss.inference.LBFGS.Params;
import org.gesis.promoss.inference.LBFGS.ProgressCallback;
import org.gesis.promoss.inference.LBFGS.Result;
import org.gesis.promoss.inference.LBFGS.Status;
import org.gesis.promoss.tools.math.BasicMath;
import org.gesis.promoss.tools.probabilistic.Gamma;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import cc.mallet.optimize.OptimizationException;
import cc.mallet.topics.DMROptimizable;




/**
 * @author c
 * Code taken from Mallet
 * All contributions are also licensed under
 * Common Public License, version 1.0 (CPL-1.0)
 * 
 * This class implements a Dirichlet-Multinomial Regression
 * on an array of probabilities given an array of double
 * input values. 
 */
public class DMR implements Optimizable.ByGradientValue {

	public int F; //Dimensionality of input +1
	//0 has the default feature -> the intercept / offset of the regression
	public int K; //Dimensionality of output
	public int M; //Number of observations



	//public double gaussianPriorMean = 0;
	public double gaussianPriorVariance = 100;

	public double[][] metadata;
	public double[][] observations;
	
	public double[] parameters;
	
	
	
	public static void main(String[] args) {

		//double[][] X2 = {{1,3},{1,4},{1,5},{1,6},{1,7},{1,8}};
		//double[] Y2 = {1,2,3,4,5,6};
		//linreg_test(X2,Y2);

		//System.out.println("-----------------");

		//double[][] X = {{0},{0},{0},{1},{1},{1}};
		//double[][] Y = {{32,123,31},{132,115,31},{32,117,32},{131,31,222},{32,31,223},{131,31,222+Math.random()}};
//		double[][] X = {{0},{0},{1},{1}};
		double[][] X = {{0},{0},{1},{1}};
		double[][] Y = {{32,923,100,100},{32,1203,100,100},{112,34,100,100},{123,35,100,100}};
		//double[][] X = {{1},{1},{1},{1}};
		//double[][] Y = {{1000,1000},{1000,1000},{1000,1000},{1000,1000}};
//		double[] parameters = dmr_test(X,Y);
//		
//		DMR dmr = new DMR(X,Y);
//		for (int i=0;i<dmr.M;i++) {
//			double observation = Y[i][0] / BasicMath.sum(Y[i]);
//			double prediction = dmr.predict(i, parameters)[0] / BasicMath.sum(dmr.predict(i, parameters));
//			System.out.println("pred: "+prediction + " obs: "+ observation);
//		}
		

		
		  DMR dmr = new DMR(X, Y);
		  dmr.dmr_test(X, Y);
		  
		  //if (1==1)return;
		  System.out.println("-----------------");

			LimitedMemoryBFGS optimizer = new LimitedMemoryBFGS(dmr);

			// Optimize once
			try {
				optimizer.optimize();
			} catch (OptimizationException e) {
				// step size too small
			}
			double[] params = new double[dmr.getNumParameters()];
			dmr.getParameters(params);
			int K = dmr.K;
			int F = dmr.F;
			for (int k=0;k<K;k++) {
				for (int f=0;f<F;f++) {
					System.out.println ("K: " + k + " F: "+f + "  " + params[k*F+f]);
				}
			}
		
	}
	

	/**
	 * We predict the observations using the observed input
	 * @param metadata: Input for the regression, MxF 
	 * @param probabilities: Observed probabilities, MxK
	 */
	DMR (double[][] metadata_in, double[][] observations) {

		this.M = observations.length;
		//+ intercept!
		this.F = metadata_in[0].length+1;
		this.K = observations[0].length;

		metadata = new double[M][];

		double[] metadata_mean = new double[F-1];
		double[] metadata_var = new double[F-1];

		
		//Add one feature for the intercept?
		for (int m=0;m<M;m++) {
			metadata[m] = new double[F];
			//intercept
			metadata[m][0] = 1;
			//Object src, int srcPos, Object dest, int destPos, int length)
			System.arraycopy(metadata_in[m], 0, metadata[m], 1, F-1);
			
			for (int f=0;f<F-1;f++) {
				metadata_mean[f] += metadata_in[m][f];
			}
			//observations[m] = BasicMath.normalise(observations[m]);
		}
		
		

		for (int f=0;f<F-1;f++) {
			metadata_mean[f] /= Double.valueOf(M);
		}
		
		for (int m=0;m<M;m++) {
			for (int f=0;f<F-1;f++) {
				metadata[m][f+1] -= metadata_mean[f];
				metadata_var[f]+= Math.pow(metadata[m][f+1], 2);
			}
		}
		
		for (int f=0;f<F-1;f++) {
			metadata_var[f] /= Double.valueOf(M);
		}
		
		for (int m=0;m<M;m++) {
			for (int f=0;f<F-1;f++) {
				metadata[m][f+1] /= metadata_var[f];
			}
		}
		
		this.parameters = new double[K * F];
	


		//System.out.println("num features: " + numFeatures + " numLabels: " + numLabels);

		this.observations = observations;


	}


	/**
	 *  Use only the default features to set the topic prior (use no document features)
	 */



	//predict for a given document
	public double[] predict(int m, double[] parameters) {
		//int F = metadata.length;
		//int K = parameters.length / F;
		double[] output = new double[K];
		for (int k=0;k<K;k++) {
			double sum = 0;
			for (int f=0;f<F;f++) {
				sum+=metadata[m][f] * parameters[k*F+f];
			}
			output[k] = Math.exp(sum);
		}
		return output;
	}
	
	//predict for a given document
	public double[] predict(int m) {
		//int F = metadata.length;
		//int K = parameters.length / F;
		double[] output = new double[K];
		for (int k=0;k<K;k++) {
			double sum = 0;
			for (int f=0;f<F;f++) {
				sum+=metadata[m][f] * parameters[k*F+f];
			}
			output[k] = Math.exp(sum);
		}
		return output;
	}


	/** The log probability of the observed counts (e.g. topic counts) given the features. */
	public double getValue () {
		//int F = metadata[0].length;
		//int K = parameters.length / F;
		// Incorporate likelihood of data
		double logLik = 0.0;

		for (int m=0;m<M;m++) {
			double[] scores = predict(m);
			double sumPrediction = BasicMath.sum(scores);
			double gamma_diff_global = Gamma.lgamma0(sumPrediction) - Gamma.lgamma0(sumPrediction + BasicMath.sum(observations[m]));
			if (!Double.isNaN(gamma_diff_global)) {
				logLik+=gamma_diff_global;
			}
			for (int i = 0; i < K; i++) {
				double gamma_diff_local = Gamma.lgamma0(scores[i] + observations[m][i]) - Gamma.lgamma0(scores[i]);
				if (!Double.isNaN(gamma_diff_local)) {
					logLik+=gamma_diff_local;
				}
			}
		}

		// Incorporate prior on parameters
		double prior = 0;

		// The log of a gaussian prior is x^2 / -2sigma^2

		for (int k = 0; k < K; k++) {
			for (int f = 0; f < F; f++) {
				double param = parameters[k*F+f];
				prior -= (param) * (param) /
						(2*gaussianPriorVariance) - Math.log(2 * Math.sqrt(2 * Math.PI * gaussianPriorVariance));
			}
		}

		logLik += prior;
		
		if (Double.isInfinite(logLik)) {
			return -logLik;
		}

		
		System.out.println("lik: " + logLik);
		
		return logLik;


	}



	public void getValueGradient (double[] buffer) {
		//int F = metadata[0].length;
		//int K = parameters.length / F;
		//int M = metadata.length;

		//gradient dimensions: K * F
		double[] gradient = new double[parameters.length];
		//for (int i = 0;i<gradient.length;i++) gradient[i] = 0;

		for (int m=0;m<M;m++) {

			double[] scores = predict(m);

			double sumScores = BasicMath.sum(scores);


			double totalLength = BasicMath.sum(observations[m]);

//			double digammaDifferenceForSums = 
//					Gamma.digamma0(sumScores) -
//					Gamma.digamma0(sumScores+ totalLength);
			double digammaDifferenceForSums =
					Gamma.digamma0(sumScores) -
					Gamma.digamma0(sumScores+ totalLength);
			//if (m==0) System.out.println("dfs "+digammaDifferenceForSums);

			for (int k=0;k<K;k++) {
				for (int f=0;f<F;f++) {
					double diff = 0;
						diff += Gamma.digamma0(scores[k]+observations[m][k] 
								-  Gamma.digamma0(scores[k]));
						
					if (m==0) System.out.println("diff " + k + " " + f + " " + metadata[m][f] + " " +  parameters[k*F+f] + " " + scores[k]+ " " + observations[m][k] + " " + digammaDifferenceForSums +  " " + diff);

					
					gradient[k*F+f]+= (digammaDifferenceForSums + diff)
									* metadata[m][f] * scores[k];
				}
			}
		}

		for (int k=0;k<K;k++) {
			for (int f=0;f<F;f++) {
				gradient[k*F+f] -=  (parameters[k*F+f]) / gaussianPriorVariance;
				
				if (Double.isInfinite(gradient[k*F+f])) {
					gradient[k*F+f] = 0;
				}
				gradient[k*F+f] = gradient[k*F+f];
				
				//System.out.println(k*F+f + " " + gradient[k*F+f]);
			}
		}

	
		System.arraycopy(gradient, 0, buffer, 0, gradient.length);
	}

	static double[] dmr_test(final double[][] X, final double[][] Y) {

		final DMR dmr = new DMR(X,Y);

		Function f = new Function() {
			@Override
			public double evaluate(double[] lambda, double[] g, int n, double step) {
				//g = dmr.getValueGradient(lambda);
				dmr.setParameters(lambda);
				dmr.getValueGradient(g);
			
				System.out.println("Likelihood " + dmr.getValue());
				return dmr.getValue();
			}
		};
		
		Params p = new Params();
		p.m = 4;
		ProgressCallback cb = new ProgressCallback() {
			@Override
			public int apply(double[] x, double[] g, double fx, double xnorm,
					double gnorm, double step, int n, int k, Status ls) {
				return 0;
			}
		};
		double[] coef = new double[(X[0].length+1) * Y[0].length];
		for (int i=0;i<coef.length;i++) {
			coef[i]=0;
		}
		Result r = LBFGS.lbfgs(coef, f, cb, p);

		for (int i=0;i<coef.length;i++) {
			System.out.println(coef[i]);
		}
		
		return coef;
	}

	static void linreg_test(final double[][] X, final double[] Y) {
		final int Nfeat = X[0].length;
		Function f = new Function() {
			@Override
			public double evaluate(double[] beta, double[] g, int n, double step) {
				double sqloss = 0;
				for (int j=0; j<Nfeat; j++) {
					g[j]=0;
				}
				for (int i=0; i<n; i++) {
					double pred = BasicMath.innerProduct(beta, X[i]);
					double resid = pred - Y[i];
					sqloss += Math.pow(resid, 2);
					for (int j=0; j<Nfeat; j++) {
						g[j] += X[i][j] * resid;
					}
					for (int j=0; j<Nfeat; j++) {
						System.out.println("gradient "+j+": "+g[j] + " " + beta[j]);
					}
				}

				for (int j=0; j<g.length; j++) {
					System.out.println(beta[j]);
				}

				System.out.println("lik:"+sqloss);

				return sqloss;
			}
		};
		Params p = new Params();
		ProgressCallback cb = new ProgressCallback() {
			@Override
			public int apply(double[] x, double[] g, double fx, double xnorm,
					double gnorm, double step, int n, int k, Status ls) {
				return 0;
			}
		};
		double[] coef = new double[Nfeat];
		LBFGS.lbfgs(coef, f, cb, p);

		for (int i=0;i<coef.length;i++) {
			System.out.println(coef[i]);
		}
	}

	static void mean_test() {
		final int target = 3;
		Function f = new Function() {
			@Override
			public double evaluate(double[] x, double[] g, int n, double step) {
				g[0] = x[0] - target; 
				return Math.pow(x[0] - target, 2);
			}
		};
		Params p = new Params();
		ProgressCallback cb = new ProgressCallback() {
			@Override
			public int apply(double[] x, double[] g, double fx, double xnorm,
					double gnorm, double step, int n, int k, Status ls) {
				return 0;
			}
		};
		double[] sol = new double[1];
		Result r = LBFGS.lbfgs(sol, f, cb, p);
		for (int i=0;i<sol.length;i++) {
			System.out.println(sol[i]);
		}
	}



	@Override
	public int getNumParameters() {
		return K*F;
	}


	@Override
	public void getParameters(double[] buffer) {
		System.arraycopy(parameters, 0, buffer, 0, K*F);
	}


	@Override
	public double getParameter(int index) {
		return parameters[index];
	}


	@Override
	public void setParameters(double[] buff) {
		System.arraycopy (buff, 0, parameters, 0, buff.length);
	}


	@Override
	public void setParameter(int index, double value) {
		this.parameters[index] = value;
	}






}
