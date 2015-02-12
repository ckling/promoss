/*
 * Created on 07.07.2006
 */
/*
 * Copyright (c) 2005-2006 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knowceans.util;

import static org.knowceans.util.Gamma.digamma;
import static org.knowceans.util.Gamma.invdigamma;
import static org.knowceans.util.Gamma.lgamma;
import static org.knowceans.util.Gamma.trigamma;

/**
 * DirichletEstimation provides a number of methods to estimate parameters of a
 * Dirichlet distribution and the Dirichlet-multinomial (Polya) distribution.
 * Most of the algorithms described in Minka (2003) Estimating a Dirichlet
 * distribution, but some home-grown extensions.
 * 
 * @author gregor
 */
public class DirichletEstimation {

	/**
	 * Estimator for the Dirichlet parameters.
	 * 
	 * @param multinomial
	 *            parameters p
	 * @return ML estimate of the corresponding parameters alpha
	 */
	public static double[] estimateAlpha(double[][] pp) {

		// sufficient statistics
		double[] suffstats = suffStats(pp);

		double[] pmean = guessMean(pp);

		// initial guess for alpha
		double[] alpha = guessAlpha(pp, pmean);

		boolean newton = false;
		if (newton) {
			alphaNewton(pp.length, suffstats, alpha);
		} else {
			alphaFixedPoint(suffstats, alpha);
		}
		return alpha;
	}

	/**
	 * estimate mean and precision of the observations separately.
	 * 
	 * @param pp
	 *            input data with vectors in rows
	 * @return a vector of the Dirichlet mean in the elements 0..K-2 (the last
	 *         element is the difference between the others and 1) and Dirichlet
	 *         precision in the element K-1, where K is the dimensionality of
	 *         the data, pp[0].length.
	 */
	public static double[] estimateMeanPrec(double[][] pp) {
		double[] mean = guessMean(pp);
		double[] meansq = colMoments(pp, 2);
		double prec = guessPrecision(mean, meansq);
		double[] suffstats = suffStats(pp);

		for (int i = 0; i < 5; i++) {
			prec = precFixedPoint(suffstats, pp.length, mean, prec);
			meanGenNewton(suffstats, mean, prec);
		}
		double[] retval = new double[mean.length];
		System.arraycopy(mean, 0, retval, 0, mean.length - 1);
		retval[mean.length - 1] = prec;
		return retval;
	}

	/**
	 * Estimator for the Dirichlet parameters from counts. This corresponds to
	 * the estimation of the Polya distribution but is done via Dirichlet
	 * parameter estimation.
	 * 
	 * @param nn
	 *            counts in each multinomial experiment
	 * @return ML estimate of the corresponding parameters alpha
	 */
	public static double[] estimateAlpha(int[][] nmk) {
		double[][] pmk = new double[nmk.length][];
		int K = nmk[0].length;
		// calculate sum
		for (int m = 0; m < pmk.length; m++) {
			int nm = Vectors.sum(nmk[m]);
			pmk[m] = new double[K];
			for (int k = 0; k < K; k++) {
				pmk[m][k] = nmk[m][k] / (double) nm;
			}
		}
		// System.out.println(Vectors.print(pmk));
		// estimate dirichlet distributions
		return estimateAlpha(pmk);
	}

	/**
	 * Polya estimation using the fixed point iteration of the leave-one-out
	 * likelihood, after Minka 2003. TODO: Gibbs sampler for Polya distribution.
	 * 
	 * @param alpha
	 *            [in/out] Dirichlet parameter with element for each k
	 * @param nmk
	 *            count matrix with individual observations in rows
	 *            ("documents") and categories in columns ("topics")
	 * @return number of iterations
	 */
	public static int estimateAlphaLoo(double[] alpha, int[][] nmk) {
		int[] nm = new int[nmk.length];
		// int[] nk = new int[alpha.length];
		double limdist = 0.000000001;
		int iter = 20000;
		double[] alphanew = new double[alpha.length];
		alphanew = Vectors.copy(alpha);
		double diffalpha;
		double sumalpha;

		// calculate sum
		for (int m = 0; m < nm.length; m++) {
			nm[m] = Vectors.sum(nmk[m]);
		}
		// Eq. 65: ak_new =
		// ak sum_m (nmk / (nmk - 1 + ak))
		// / sum_m (nm / (nm - 1 + sum_k ak))
		for (int i = 0; i < iter; i++) {
			sumalpha = Vectors.sum(alpha);
			diffalpha = 0;
			double den = 0;
			for (int m = 0; m < nm.length; m++) {
				den += nm[m] / (nm[m] - 1 + sumalpha);
			}
			for (int k = 0; k < alpha.length; k++) {
				double num = 0;
				for (int m = 0; m < nm.length; m++) {
					num += nmk[m][k] / (nmk[m][k] - 1 + alpha[k]);
				}
				alphanew[k] = alpha[k] * num / den;
				diffalpha += Math.abs(alpha[k] - alphanew[k]);
			}
			// System.out.println(Vectors.print(aa));
			if (diffalpha < limdist) {
				return i;
			}
			alpha = Vectors.copy(alphanew);
		}
		return iter;
	}

	/**
	 * Estimate mean and precision of the observations separately from counts.
	 * This corresponds to estimation of the Polya distribution.
	 * 
	 * @param nn
	 *            input data with vectors in rows
	 * @return a vector of the Dirichlet mean in the elements 0..K-2 (the last
	 *         element is the difference between the others and 1) and Dirichlet
	 *         precision in the element K-1, where K is the dimensionality of
	 *         the data, pp[0].length.
	 */
	public static double[] estimateMeanPrec(int[][] nn) {
		double[] retval = null;
		return retval;
	}

	/**
	 * Get the precision out of a mean precision combined vector
	 * 
	 * @param meanPrec
	 * @return
	 */
	public static double getPrec(double[] meanPrec) {
		return meanPrec[meanPrec.length - 1];
	}

	/**
	 * Get the mean out of a mean precision combined vector. The mean vector is
	 * copied.
	 * 
	 * @param meanPrec
	 * @return
	 */
	public static double[] getMean(double[] meanPrec) {
		double[] retval = new double[meanPrec.length];
		System.arraycopy(meanPrec, 0, retval, 0, meanPrec.length - 1);
		double sum = 0;
		for (int k = 0; k < meanPrec.length - 1; k++) {
			sum += meanPrec[k];
		}
		retval[meanPrec.length - 1] = 1 - sum;
		return retval;
	}

	/**
	 * Get the alpha vector out of a mean precision combined vector. The vector
	 * is copied.
	 * 
	 * @param meanPrec
	 * @return
	 */
	public static double[] getAlpha(double[] meanPrec) {
		double[] aa = getMean(meanPrec);
		Vectors.mult(aa, getPrec(meanPrec));
		return aa;
	}

	/**
	 * Estimate the dirichlet parameters using the moments method
	 * 
	 * @param pp
	 *            data with items in rows and dimensions in cols
	 * @param pmean
	 *            first moment of pp
	 * @return
	 */
	public static double[] guessAlpha(double[][] pp, double[] pmean) {
		// first and second moments of the columns of p

		int K = pp[0].length;
		double[] pmeansq = colMoments(pp, 2);

		// init alpha_k using moments method (19-21)
		double[] alpha = Vectors.copy(pmean);
		double precision = guessPrecision(pmean, pmeansq);
		precision /= K;
		// System.out.println("precision = " + precision);
		// alpha_k = mean_k * precision
		for (int k = 0; k < K; k++) {
			alpha[k] = pmean[k] * precision;
		}
		return alpha;
	}

	/**
	 * Estimate the Dirichlet mean of the data along columns
	 * 
	 * @param pp
	 * @return
	 */
	public static double[] guessMean(double[][] pp) {
		return colMoments(pp, 1);
	}

	/**
	 * Estimate the mean given the data and a guess of the mean and precision.
	 * This uses the gradient ascent method described in Minka (2003) and Huang
	 * (2004).
	 * 
	 * @param suffstats
	 * @param mean
	 *            [in / out]
	 * @param prec
	 */
	private static void meanGenNewton(double[] suffstats, double[] mean,
			double prec) {

		double[] alpha = new double[mean.length];

		for (int i = 0; i < 100; i++) {

			for (int k = 0; k < mean.length; k++) {
				for (int j = 0; j < alpha.length; j++) {
					alpha[k] += mean[j]
							* (suffstats[j] - digamma(prec * mean[j]));
				}
				alpha[k] = invdigamma(suffstats[k] - alpha[k]);
			}
			double sumalpha = Vectors.sum(alpha);
			for (int k = 0; k < alpha.length; k++) {
				mean[k] = alpha[k] / sumalpha;
			}
		}
	}

	/**
	 * Estimate the precision given the data and a guesses of the mean and
	 * precision. This uses the gradient ascent method described in Minka (2003)
	 * and Huang (2004).
	 * 
	 * @param suffstats
	 * @param N
	 * @param mean
	 * @param prec
	 */
	private static double precFixedPoint(double[] suffstats, int N,
			double[] mean, double prec) {
		double dloglik = 0;
		for (int k = 0; k < mean.length; k++) {
			dloglik += mean[k] * (digamma(prec * mean[k]) + suffstats[k]);
		}
		dloglik = N * (digamma(prec) - dloglik);
		double ddloglik = 0;
		for (int k = 0; k < mean.length; k++) {
			ddloglik += mean[k] * mean[k] * trigamma(prec * mean[k]);
		}
		ddloglik = N * (trigamma(prec) - dloglik);
		double precinv = 1 / prec + dloglik / (prec * prec * ddloglik);
		return 1 / precinv;

	}

	/**
	 * guess alpha via Dirichlet parameter point estimate and Dirichlet moment
	 * matching.
	 * 
	 * @param nmk
	 * @return
	 */
	public static double[] guessAlpha(int[][] nmk) {

		double[][] pmk = new double[nmk.length][];
		double[] pk;
		int K = nmk[0].length;
		// calculate sum
		for (int m = 0; m < pmk.length; m++) {
			int nm = Vectors.sum(nmk[m]);
			pmk[m] = new double[K];
			for (int k = 0; k < K; k++) {
				pmk[m][k] = nmk[m][k] / (double) nm;
			}
		}
		pk = guessMean(pmk);
		return guessAlpha(pmk, pk);
	}

	/**
	 * guess alpha via "direct" moment matching on the Polya distribution (which
	 * is just Dirichlet moment matching in disguise). After Minka's (2003)
	 * Equation (19ff).
	 * 
	 * @param nmk
	 * @param nm
	 *            sums of observations for all categories (eg document lengths,
	 *            ndsum)
	 */
	public static double[] guessAlphaDirect(int[][] nmk, int[] nm) {
		double[] pmean, pmk;
		double pvark, prec;
		int K = nmk[0].length;
		int M = nm.length;

		// all computations inline to easier port to C

		// meank = 1/M sum_m nmk / nm
		// vark = 1/M sum_m (nmk / nm - meank)^2
		// mk = meank (1 - meank) / vark - 1
		// alphak = prec * meank
		// prec = exp( 1 / (K - 1) sum_(k=1..K-1) log mk

		pmean = new double[K];
		pmk = new double[M];
		prec = 0;

		// calculate pmean and pvar
		for (int k = 0; k < K; k++) {
			for (int m = 0; m < M; m++) {
				// calculations more expensive than memory
				pmk[m] = nmk[m][k] / (double) nm[m];
				pmean[k] += pmk[m];
			}
			pmean[k] /= M;
			// need variance for K-1 components
			if (k < K - 1) {
				pvark = 0;
				for (int m = 0; m < M; m++) {
					double diff = pmk[m] - pmean[k];
					pvark += diff * diff;
				}
				pvark /= M;
				prec += Math.log(pmean[k] * (1 - pmean[k]) / pvark - 1);
			}
		}
		prec = Math.exp(1 / (K - 1) * prec);
		// alpha = pmean * prec
		Vectors.mult(pmean, prec);
		return pmean;
	}

	/**
	 * Estimate the Dirichlet precision using moment matching method.
	 * 
	 * @param pmean
	 * @param pmeansq
	 * @return
	 */
	public static double guessPrecision(double[] pmean, double[] pmeansq) {
		double precision = 0;

		int K = pmean.length;

		// estimate s for each dimension (21) and take the mean
		for (int k = 0; k < K; k++) {
			precision += (pmean[k] - pmeansq[k])
					/ (pmeansq[k] - pmean[k] * pmean[k]);
		}
		return precision / pmean.length;
	}

	/**
	 * Moment of each column in an element of the returned vector
	 * 
	 * @param xx
	 * @param order
	 * @return
	 */
	private static double[] colMoments(double[][] xx, int order) {
		int K = xx[0].length;
		int N = xx.length;

		double[] pmean2 = new double[K];
		for (int i = 0; i < N; i++) {
			for (int k = 0; k < K; k++) {
				double element = xx[i][k];
				for (int d = 1; d < order; d++) {
					element *= element;
				}
				pmean2[k] += element;
			}
		}
		for (int k = 0; k < K; k++) {
			pmean2[k] /= N;
		}
		return pmean2;
	}

	/**
	 * Dirichlet sufficient statistics 1/N sum log p
	 * 
	 * @param pp
	 * @return
	 */
	private static double[] suffStats(double[][] pp) {
		int K = pp[0].length;
		int N = pp.length;
		double eps = 1e-6;

		double[] suffstats = new double[K];

		for (int i = 0; i < N; i++) {
			for (int k = 0; k < K; k++) {
				suffstats[k] += Math.log(pp[i][k] + eps);
			}
		}
		for (int k = 0; k < K; k++) {
			suffstats[k] /= N;
		}
		return suffstats;
	}

	// FIXME: doesn't work yet.
	public static void alphaNewton(int N, double[] suffstats, double[] alpha) {
		int K = alpha.length;

		// initial likelihood (4)
		double loglik = 0;
		double loglikold = 0;
		double[] grad = new double[K];
		double alphasum = Vectors.sum(alpha);
		double[] alphaold = new double[K];
		double lgasum = 0;
		double asssum = 0;
		int iterations = 1000;
		double epsilon = 1e-6;
		for (int i = 0; i < iterations; i++) {
			System.arraycopy(alpha, 0, alphaold, 0, K);

			for (int k = 0; k < K; k++) {
				lgasum += lgamma(alpha[k]);
				asssum += (alpha[k] - 1) * suffstats[k];
				grad[k] = N
						* (digamma(alphasum) - digamma(alpha[k]) + suffstats[k]);
			}
			loglik = N * (lgamma(alphasum) - lgasum + asssum);
			// System.out.println(loglik);
			if (Math.abs(loglikold - loglik) < epsilon) {
				break;
			}
			loglikold = loglik;

			// invhessian x grad and diag Q (could be omitted by calculating 17
			// and 15 below inline)
			double[] hinvg = new double[K];
			double[] qdiag = new double[K];
			double bnum = 0;
			double bden = 0;

			// (14)
			double z = N * trigamma(alphasum);

			// (18)
			for (int k = 0; k < K; k++) {
				qdiag[k] = -N * trigamma(alpha[k]);
				bnum += grad[k] / qdiag[k];
				bden += 1 / qdiag[k];
			}
			double b = bnum / (1 / z + bden);

			for (int k = 0; k < K; k++) {
				// (17)
				hinvg[k] = (grad[k] - b) / qdiag[k];
				// (15)
				alpha[k] -= hinvg[k];
			}
			// System.out.println("hinv g = " + Vectors.print(hinvg));
			// System.out.println("alpha = " + Vectors.print(alpha));
		}
	}

	/**
	 * fixpoint iteration on alpha.
	 * 
	 * @param suffstats
	 * @param alpha
	 *            [in/out]
	 */
	public static int alphaFixedPoint(double[] suffstats, double[] alpha) {
		int K = alpha.length;
		double maxdiff = 1e-4;
		int maxiter = 100;
		double alphadiff;

		// TODO: update alpha element-wise correct ?
		// using (9)
		for (int i = 0; i < maxiter; i++) {
			alphadiff = 0;
			double sumalpha = Vectors.sum(alpha);
			for (int k = 0; k < K; k++) {
				alpha[k] = invdigamma(digamma(sumalpha) + suffstats[k]);
				alphadiff = Math.max(alphadiff, Math.abs(alpha[k] - alphadiff));
			}
			if (alphadiff < maxdiff) {
				return i;
			}
		}
		return maxiter;
	}

	public static double estimateAlphaMomentMatch(int[][] nmk, int[] nm) {
		int k, m;
		double precision = 0;
		double pmk;
		int K = nmk[0].length;
		int M = nmk.length;
		double eps = 1e-6;
		double pmeank = 0, pmeansqk = 0;

		for (k = 0; k < K; k++) {
			for (m = 0; m < M; m++) {
				pmk = (double) nmk[m][k] / (double) nm[m];
				pmeank += pmk;
				pmeansqk += pmk * pmk;
			}
			pmeank /= M;
			pmeansqk /= M;
			precision += (pmeank - pmeansqk)
					/ (pmeansqk - pmeank * pmeank + eps);
		}
		return precision / (K * K);
	}

	/**
	 * fixpoint iteration on alpha using counts as input and estimating by Polya
	 * distribution directly. Eq. 55 in Minka (2003)
	 * 
	 * @param nmk
	 *            count data (documents in rows, topic associations in cols)
	 * @param nm
	 *            total counts across rows
	 * @param alpha
	 * @param alpha
	 */
	public static double estimateAlphaMap(int[][] nmk, int[] nm, double alpha,
			double a, double b) {
		int i, m, k, iter = 200;
		double summk, summ;
		int M = nmk.length;
		int K = nmk[0].length;
		double alpha0 = 0;
		double prec = 1e-5;

		// alpha = ( a - 1 + alpha * [sum_m sum_k digamma(alpha + mnk) -
		// digamma(alpha)] ) /
		// ( b + K * [sum_m digamma(K * alpha + nm) - digamma(K * alpha)] )

		for (i = 0; i < iter; i++) {
			summk = 0;
			summ = 0;
			for (m = 0; m < M; m++) {
				summ += digamma(K * alpha + nm[m]);
				for (k = 0; k < K; k++) {
					summk += digamma(alpha + nmk[m][k]);
				}
			}
			summ -= M * digamma(K * alpha);
			summk -= M * K * digamma(alpha);
			alpha = (a - 1 + alpha * summk) / (b + K * summ);
			// System.out.println(alpha);
			// System.out.println(Math.abs(alpha - alpha0));
			if (Math.abs(alpha - alpha0) < prec) {
				return alpha;
			}
			alpha0 = alpha;
		}
		return alpha;
	}

	/**
	 * fixpoint iteration on alpha using counts as input and estimating by Polya
	 * distribution directly. Eq. 55 in Minka (2003). This version uses a subset
	 * of rows in nmk, indexed by mrows.
	 * 
	 * @param nmk
	 *            count data (documents in rows, topic associations in cols)
	 * @param nm
	 *            total counts across rows
	 * @param mrows
	 *            set of rows to be used for estimation
	 * @param alpha
	 * @param alpha
	 */
	public static double estimateAlphaMapSub(int[][] nmk, int[] nm,
			int[] mrows, double alpha, double a, double b) {
		int i, m, k, iter = 200;
		double summk, summ;
		int M = mrows.length;
		int K = nmk[0].length;
		double alpha0 = 0;
		double prec = 1e-5;

		// alpha = ( a - 1 + alpha * [sum_m sum_k digamma(alpha + mnk) -
		// digamma(alpha)] ) /
		// ( b + K * [sum_m digamma(K * alpha + nm) - digamma(K * alpha)] )

		for (i = 0; i < iter; i++) {
			summk = 0;
			summ = 0;
			for (m = 0; m < M; m++) {
				summ += digamma(K * alpha + nm[mrows[m]]);
				for (k = 0; k < K; k++) {
					summk += digamma(alpha + nmk[mrows[m]][k]);
				}
			}
			summ -= M * digamma(K * alpha);
			summk -= M * K * digamma(alpha);
			alpha = (a - 1 + alpha * summk) / (b + K * summ);
			// System.out.println(alpha);
			// System.out.println(Math.abs(alpha - alpha0));
			if (Math.abs(alpha - alpha0) < prec) {
				return alpha;
			}
			alpha0 = alpha;
		}
		return alpha;
	}

	/**
	 * estimate several alphas based on subsets of rows
	 * 
	 * @param nmk
	 *            full array
	 * @param nm
	 *            full sums array
	 * @param m2j
	 *            row-wise association with set j (max j - 1 = alphajk.length)
	 * @param alphajk
	 *            set-wise hyperparameter
	 * @param a
	 * @param b
	 * @return
	 */
	public static double[][] estimateAlphaMapSub(int[][] nmk, int[] nm,
			int[] m2j, double[][] alphajk, double a, double b) {
		int i, m, M, k, K, iter = 200;
		double sumalpha, summk, summ;
		double prec = 1e-5;
		M = nmk.length;
		int J = alphajk.length;
		K = alphajk[0].length;
		double[][] alphanew = new double[J][K];
		for (int j = 0; j < J; j++) {

			// alpha_k = alpha_k * ( [sum_m digamma(nmk + alpha_k) -
			// digamma(alpha_k)] ) /
			// ( [sum_m digamma(nm + sum_k alpha_k) - digamma(sum_k * alpha_k)]
			// )

			for (i = 0; i < iter; i++) {
				sumalpha = Vectors.sum(alphajk[j]);
				for (k = 0; k < K; k++) {
					summk = 0;
					summ = 0;
					for (m = 0; m < M; m++) {
						// filter by subset j
						if (m2j[m] == j) {
							summk += digamma(nmk[m][k] + alphajk[j][k]);
							summ += digamma(nm[m] + sumalpha);
						}
					}
					summk -= M * digamma(alphajk[j][k]);
					summ -= M * digamma(sumalpha);
					// MAP version
					alphanew[j][k] = alphajk[j][k] * (a + summk)
							/ (b / K + summ);
					// ML version
					// alphanew[k] = alpha[k] * summk / summ;
				}
				if (Vectors.sqdist(alphanew[j], alphajk[j]) < prec) {
					break;
				}
				// System.out.println(Vectors.print(alphanew));
				// update alpha to new values
				alphajk[j] = Vectors.copy(alphanew[j]);
			}
		}
		return alphajk;
	}

	/**
	 * fixpoint iteration on alpha using counts as input and estimating by Polya
	 * distribution directly. Eq. 55 in Minka (2003)
	 * 
	 * @param nmk
	 *            count data (documents in rows, topic associations in cols)
	 * @param nm
	 *            total counts across rows
	 * @param alpha
	 *            [in/out]
	 */
	public static double[] estimateAlphaMap(int[][] nmk, int[] nm,
			double[] alpha, double a, double b) {

		double[] alphanew;
		double sumalpha, summk, summ;
		int i, m, M, k, K, iter = 200;
		double prec = 1e-5;

		M = nmk.length;
		K = alpha.length;

		alphanew = new double[K];

		// alpha_k = alpha_k * ( [sum_m digamma(nmk + alpha_k) -
		// digamma(alpha_k)] ) /
		// ( [sum_m digamma(nm + sum_k alpha_k) - digamma(sum_k * alpha_k)] )

		for (i = 0; i < iter; i++) {
			sumalpha = Vectors.sum(alpha);
			for (k = 0; k < K; k++) {
				summk = 0;
				summ = 0;
				for (m = 0; m < M; m++) {
					summk += digamma(nmk[m][k] + alpha[k]);
					summ += digamma(nm[m] + sumalpha);
				}
				summk -= M * digamma(alpha[k]);
				summ -= M * digamma(sumalpha);
				// MAP version
				alphanew[k] = alpha[k] * (a + summk) / (b / K + summ);
				// ML version
				// alphanew[k] = alpha[k] * summk / summ;
			}
			if (Vectors.sqdist(alphanew, alpha) < prec) {
				return alphanew;
			}
			// System.out.println(Vectors.print(alphanew));
			// update alpha to new values
			alpha = Vectors.copy(alphanew);
		}
		return alpha;
	}

	// //////////////////////////////

	/**
	 * sampling function for adaptive rejection sampler
	 * 
	 * @param nmk
	 *            count matrix (rows for observations)
	 * @param a
	 *            Gamma prior shape
	 * @param b
	 *            Gamma prior scale
	 * @throws Exception
	 */
	private static double[] sampleAlphaArms(int[][] nmk, double a, double b,
			int n) throws Exception {
		GammaPolyaArms arms;
		GammaPolyaParams params = new GammaPolyaParams();
		params.nmk = nmk;
		params.a = a;
		params.b = b;
		arms = new GammaPolyaArms();
		double[] xprev = new double[] { 5 };
		double[] xl = new double[] { .00001 };
		double[] xr = new double[] { 10.0 };
		double[] alpha = new double[n];
		arms.hist(params, 0.1, 2.8, 200);
		for (int i = 0; i < n; i++) {
			alpha[i] = arms.armsSimple(params, 25, xl, xr, true, xprev);
		}
		return alpha;
	}

	static class GammaPolyaParams {
		int[][] nmk;
		double a;
		double b;
	}

	static class GammaPolyaArms extends ArmSampler {

		@Override
		public double logpdf(double alpha, Object params) {
			double logpaz = 0;
			GammaPolyaParams gpp = (GammaPolyaParams) params;
			// p(a | z) = 1/Z prod_m [fdelta(n[m][.], a) / fdelta(a)] p(a);
			for (int m = 0; m < gpp.nmk.length; m++) {
				logpaz += Gamma.ldelta(gpp.nmk[m], alpha);
				logpaz -= Gamma.ldelta(gpp.nmk[m].length, alpha);
			}
			// logpaz += Math.log(Densities.pdfGamma(alpha, gpp.a, gpp.b));
			return logpaz;
		}

		/**
		 * print a histogram of the likelihood
		 * 
		 * @param alpha
		 * @param params
		 * @param min
		 * @param max
		 */
		public void hist(Object params, double amin, double amax, int n) {
			double adiff = (amax - amin) / (n + 1);
			GammaPolyaParams gpp = (GammaPolyaParams) params;
			double a = amin;
			for (int i = 0; i < n; i++) {
				a += adiff;
				double pa = logpdf(a, params);
				System.out.println(a + "\t" + pa);
			}
		}

	}

	// ////////////////////////////

	public static void main(String[] args) throws Exception {
		// testing estimation of alpha from p
		testPolya();
		// testDirichlet();
	}

	public static void testPolya() {

		RandomSamplers rs = new RandomSamplers();
		int M = 10000;
		int K = 5;
		int N = 0;
		int N0 = 100;
		// FIXME: ARMS only works for higher alpha

		double[] alpha = { 0.35, 0.35, 0.05, 0.24, 0.31 };

		System.out.println("original alpha");
		System.out.println(Vectors.print(alpha));
		int[][] nmk = new int[M][K];
		int[] nm = new int[M];
		int W = 0;
		for (int m = 0; m < nmk.length; m++) {
			nm[m] = (int) (N0 + N * Math.random());
			nmk[m] = rs.randMultFreqs(rs.randDir(alpha), nm[m]);
			W += nm[m];
		}
		int[] nk = new int[K];
		for (int k = 0; k < nk.length; k++) {
			for (int m = 0; m < M; m++) {
				nk[k] += nmk[m][k];
			}
		}
		System.out.println("sample counts in categories");
		System.out.println(Vectors.print(nk));

		alpha = guessAlpha(nmk);
		System.out.println("estimated alpha from counts (moments matching)");
		System.out.println(Vectors.print(alpha));

		// System.out.println(Vectors.print(nmk));
		alpha = estimateAlpha(nmk);
		System.out
				.println("estimated alpha from counts (fixed point via Dirichlet Eq. 9)");
		System.out.println(Vectors.print(alpha));

		System.out.println("guess alpha via Polya moment match");
		alpha = guessAlphaDirect(nmk, nm);
		System.out.println(Vectors.print(alpha));

		System.out
				.println("estimated scalar alpha from counts via precision (moment matching)");
		System.out.println(estimateAlphaMomentMatch(nmk, nm));
		System.out
				.println("estimated scalar alpha from counts via MAP estimator");
		System.out.println(estimateAlphaMap(nmk, nm, 0.1, 0.5, 0.5));

		System.out
				.println("estimated vector alpha from counts via MAP estimator");
		double[] astart = Vectors.ones(K, 0.1);
		System.out.println(Vectors.print(estimateAlphaMap(nmk, nm, astart, 0.5,
				0.5)));
	}

	public static void testDirichlet() {
		// testing estimation of alpha from p
		double[] alpha = { 1, 1.9, 1.9 };
		System.out.println("original alpha");
		System.out.println(Vectors.print(alpha));
		RandomSamplers rs = new RandomSamplers();
		double[][] pp = rs.randDir(alpha, 100000);
		// System.out.println(Vectors.print(pp));
		double[] alphaguess = estimateAlpha(pp);
		System.out.println("estimated alpha");
		System.out.println(Vectors.print(alphaguess));

		System.out.println("guessed mean");
		double[] mean = guessMean(pp);
		double[] suffstats = suffStats(pp);
		System.out.println(Vectors.print(mean));

		System.out.println("guessed mean (Newton)");
		meanGenNewton(suffstats, mean, 2.5);
		System.out.println(Vectors.print(mean));

		System.out.println("estimated precision, mean and alpha");
		double[] mp = estimateMeanPrec(pp);
		System.out.println(getPrec(mp));
		System.out.println(Vectors.print(getMean(mp)));
		System.out.println(Vectors.print(getAlpha(mp)));
	}

}
