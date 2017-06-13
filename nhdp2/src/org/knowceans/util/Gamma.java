/*
 * Created on Aug 1, 2005
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

import static java.lang.Math.log;

/**
 * Gamma represents the Gamma function and its derivatives
 * 
 * @author heinrich
 */

public class Gamma {

	/**
	 * <a href="http://en.wikipedia.org/wiki/Euler-Mascheroni_constant">Euler-
	 * Mascheroni constant</a>
	 * 
	 * @since 2.0
	 */
	public static final double GAMMA = 0.577215664901532860606512090082;

	/** Lanczos coefficients */
	private static final double[] lanczos = { 0.99999999999999709182,
			57.156235665862923517, -59.597960355475491248,
			14.136097974741747174, -0.49191381609762019978,
			.33994649984811888699e-4, .46523628927048575665e-4,
			-.98374475304879564677e-4, .15808870322491248884e-3,
			-.21026444172410488319e-3, .21743961811521264320e-3,
			-.16431810653676389022e-3, .84418223983852743293e-4,
			-.26190838401581408670e-4, .36899182659531622704e-5, };

	/** Avoid repeated computation of log of 2 PI in logGamma */
	private static final double HALF_LOG_2_PI = 0.5 * Math.log(2.0 * Math.PI);

	/**
	 * clamp value for small gamma and digamma values
	 */
	private static final double GAMMA_MINX = 1.e-12;
	private static final double DIGAMMA_MINNEGX = -1250;

	/**
	 * Returns the natural logarithm of the gamma function &#915;(x). The
	 * implementation of this method is based on:
	 * <ul>
	 * <li><a href="http://mathworld.wolfram.com/GammaFunction.html"> Gamma
	 * Function</a>, equation (28).</li>
	 * <li><a href="http://mathworld.wolfram.com/LanczosApproximation.html">
	 * Lanczos Approximation</a>, equations (1) through (5).</li>
	 * <li><a href="http://my.fit.edu/~gabdo/gamma.txt">Paul Godfrey, A note on
	 * the computation of the convergent Lanczos complex Gamma approximation
	 * </a></li>
	 * </ul>
	 * gregh: this implementation sets 0 argument to a very small double (1e-15)
	 * in order to cope with degenerate Dirichlet distributions
	 * 
	 * @param x
	 *            the value.
	 * @return log(&#915;(x))
	 */
	// this is from Apache Commons Math
	public static double lgamma(double x) {
		double ret;

		if (Double.isNaN(x)) {
			return x;
		}

		if (x >= 0 && x <= GAMMA_MINX) {
			x = GAMMA_MINX;
		}
		double g = 607.0 / 128.0;

		double sum = 0.0;
		for (int i = lanczos.length - 1; i > 0; --i) {
			sum = sum + (lanczos[i] / (x + i));
		}
		sum = sum + lanczos[0];

		double tmp = x + g + .5;
		ret = ((x + .5) * Math.log(tmp)) - tmp + HALF_LOG_2_PI
				+ Math.log(sum / x);
		return ret;
	}

	/**
	 * truncated Taylor series of log Gamma(x). From lda-c
	 * 
	 * @param x
	 * @return
	 */
	public static double lgamma0(double x) {
		double z;
		assert x > 0 : "lgamma(" + x + ")";
		z = 1. / (x * x);

		x = x + 6;
		z = (((-0.000595238095238 * z + 0.000793650793651) * z - 0.002777777777778)
				* z + 0.083333333333333)
				/ x;
		z = (x - 0.5) * log(x) - x + 0.918938533204673 + z - log(x - 1)
				- log(x - 2) - log(x - 3) - log(x - 4) - log(x - 5)
				- log(x - 6);
		return z;
	}

	/**
	 * gamma function
	 * 
	 * @param x
	 * @return
	 */
	public static double fgamma(double x) {
		return Math.exp(lgamma(x));
	}

	/**
	 * faculty of an integer.
	 * 
	 * @param n
	 * @return
	 */
	public static int factorial(int n) {
		return (int) Math.exp(lgamma(n + 1));
	}

	/**
	 * "Dirichlet delta function" is the partition function of the Dirichlet
	 * distribution and the k-dimensional generalisation of the beta function.
	 * fdelta(a) = prod_k fgamma(a_k) / fgamma( sum_k a_k ) = int_(sum x = 1)
	 * prod_k x_k^(a_k-1) dx. See G. Heinrich: Parameter estimation for text
	 * analysis (http://www.arbylon.net/publications/text-est.pdf)
	 * 
	 * @param x
	 * @return
	 */
	public static double ldelta(double[] x) {
		double lognum = 0;
		double den = 0;
		for (int i = 0; i < x.length; i++) {
			lognum += lgamma(x[i]);
			den += x[i];
		}
		return lognum - lgamma(den);
	}

	public static double fdelta(double[] x) {
		return Math.exp(ldelta(x));
	}

	public static double ldelta(int[] x) {
		double lognum = 0;
		double den = 0;
		for (int i = 0; i < x.length; i++) {
			lognum += lgamma(x[i]);
			den += x[i];
		}
		return lognum - lgamma(den);
	}

	public static double fdelta(int[] x) {
		return Math.exp(ldelta(x));
	}

	/**
	 * log Delta function with a symmetric concentration parameter alpha that is
	 * added to every element in the vector.
	 * 
	 * @param x
	 * @param alpha
	 * @return
	 */
	public static double ldelta(int[] x, double alpha) {
		double lognum = 0;
		double den = 0;
		for (int i = 0; i < x.length; i++) {
			lognum += lgamma(x[i] + alpha);
			den += x[i];
		}
		den += alpha * x.length;
		return lognum - lgamma(den);
	}

	/**
	 * log Delta function with a concentration parameter alpha that is added to
	 * every element in the vector.
	 * 
	 * @param x
	 * @param alpha
	 * @return
	 */
	public static double ldelta(int[] x, double[] alpha) {
		double lognum = 0;
		double den = 0;
		for (int i = 0; i < x.length; i++) {
			lognum += lgamma(x[i] + alpha[i]);
			den += x[i];
		}
		den += Vectors.sum(alpha) * x.length;
		return lognum - lgamma(den);
	}

	/**
	 * "standard" fdelta function
	 * 
	 * @param x
	 * @param alpha
	 * @return
	 */
	public static double fdelta(int[] x, double alpha) {
		return Math.exp(ldelta(x, alpha));
	}

	/**
	 * Symmetric version of the log Dirichlet delta function
	 * 
	 * @param K
	 * @param x
	 * @return
	 */
	public static double ldelta(int K, double x) {
		double delta;
		delta = K * lgamma(x) - lgamma(K * x);
		return delta;
	}

	public static double fdelta(int K, double x) {
		return Math.exp(ldelta(K, x));
	}

	/**
	 * compute the ratio of the dirichlet partition functions
	 * <p>
	 * delta(n(k) + alpha(k)) / delta(n(k) - nless(k) + alpha(k))
	 * 
	 * @param nk
	 *            [K] numerator counts / integer part
	 * @param alphak
	 *            [K] numerator hyperparameter / fractional part
	 * @param nkless
	 *            [K] count vector that denominator is less than numerator
	 * @param temp
	 *            [K] buffer used for computing
	 * @return
	 */
	public static double fdeltaRatio(int[] nk, double[] alphak, int[] nkless,
			double[] tempk) {
		double rat = 0;
		Vectors.copy(nk, tempk);
		Vectors.add(tempk, alphak);
		rat = Gamma.ldelta(tempk);
		Vectors.subtract(tempk, nkless);
		rat -= Gamma.ldelta(tempk);
		return Math.exp(rat);
	}

	// limits for switching algorithm in digamma
	/** C limit */
	private static final double C_LIMIT = 49;
	/** S limit */
	private static final double S_LIMIT = 1e-5;

	/**
	 * <p>
	 * Computes the digamma function of x.
	 * </p>
	 * <p>
	 * This is an independently written implementation of the algorithm
	 * described in Jose Bernardo, Algorithm AS 103: Psi (Digamma) Function,
	 * Applied Statistics, 1976.
	 * </p>
	 * <p>
	 * Some of the constants have been changed to increase accuracy at the
	 * moderate expense of run-time. The result should be accurate to within
	 * 10^-8 absolute tolerance for x >= 10^-5 and within 10^-8 relative
	 * tolerance for x > 0.
	 * </p>
	 * <p>
	 * Performance for large negative values of x will be quite expensive
	 * (proportional to |x|). Accuracy for negative values of x should be about
	 * 10^-8 absolute for results less than 10^5 and 10^-8 relative for results
	 * larger than that.
	 * </p>
	 * gregh: this implementation sets 0 argument to a very small double (1e-12)
	 * in order to cope with degenerate Dirichlet distributions. Furthermore,
	 * small values of the result (for negative arguments) will be set to the
	 * result for that bound.
	 * 
	 * @param x
	 *            the argument
	 * @return digamma(x) to within 10-8 relative or absolute error whichever is
	 *         smaller
	 * @see <a href="http://en.wikipedia.org/wiki/Digamma_function"> Digamma at
	 *      wikipedia </a>
	 * @see <a href="http://www.uv.es/~bernardo/1976AppStatist.pdf"> Bernardo's
	 *      original article </a>
	 * @since 2.0
	 */
	// from Apache Commons Math
	public static double digamma(double x) {
		// double y = digamma(x, 0);
		// System.out.println(y + " " + x);
		// return y;
		// }
		//
		// private static double digamma(double x, int level) {
		if (x >= 0 && x < GAMMA_MINX) {
			x = GAMMA_MINX;
		}
		if (x < DIGAMMA_MINNEGX) {
			// System.out.println("UNDERFLOW: level " + level + " x " + x);
			return digamma(DIGAMMA_MINNEGX + GAMMA_MINX);
		}
		if (x > 0 && x <= S_LIMIT) {
			// System.out.println("S_LIMIT: level " + level + " x " + x);
			// use method 5 from Bernardo AS103
			// accurate to O(x)
			return -GAMMA - 1 / x;
		}

		if (x >= C_LIMIT) {
			// System.out.println("C_LIMIT: level " + level + " x " + x);
			// use method 4 (accurate to O(1/x^8)
			double inv = 1 / (x * x);
			// 1 1 1 1
			// log(x) - --- - ------ + ------- - -------
			// 2 x 12 x^2 120 x^4 252 x^6
			return Math.log(x) - 0.5 / x - inv
					* ((1.0 / 12) + inv * (1.0 / 120 - inv / 252));
		}
		// if (level > 12000) {
		// System.out.println("recursion level " + level + " x " + x);
		// }
		// return digamma(x + 1, level + 1) - 1 / x;
		return digamma(x + 1) - 1 / x;
	}

	/**
	 * Nonrecursive version, truncated Taylor series of Psi(x) = d/dx Gamma(x).
	 * From lda-c
	 * 
	 * @param x
	 * @return
	 */
	public static double digamma0(double x) {
		double p;
		assert x > 0 : "digamma(" + x + ")";
		x = x + 6;
		p = 1 / (x * x);
		p = (((0.004166666666667 * p - 0.003968253986254) * p + 0.008333333333333)
				* p - 0.083333333333333)
				* p;
		p = p + log(x) - 0.5 / x - 1 / (x - 1) - 1 / (x - 2) - 1 / (x - 3) - 1
				/ (x - 4) - 1 / (x - 5) - 1 / (x - 6);
		return p;
	}

	/**
	 * coarse approximation of the inverse of the digamma function (after Eqs.
	 * 132-135 in Minka (2003), Estimating a Dirichlet distribution)
	 */
	public static double invdigamma(double y) {
		double x = 0;
		// initialisation (135)
		if (y >= -2.22) {
			x = Math.exp(y) + .5;
		} else {
			// gamma = - digamma(1)
			x = -1 / (y + GAMMA);
		}
		// Newton's method (132)
		for (int i = 0; i < 5; i++) {
			x = x - (digamma(x) - y) / trigamma(x);
		}
		return x;
	}

	/**
	 * <p>
	 * Computes the trigamma function of x. This function is derived by taking
	 * the derivative of the implementation of digamma.
	 * </p>
	 * 
	 * @param x
	 *            the argument
	 * @return trigamma(x) to within 10-8 relative or absolute error whichever
	 *         is smaller
	 * @see <a href="http://en.wikipedia.org/wiki/Trigamma_function"> Trigamma
	 *      at wikipedia </a>
	 * @see Gamma1#digamma(double)
	 * @since 2.0
	 */
	// from Apache Commons Math
	public static double trigamma(double x) {
		if (x > 0 && x <= S_LIMIT) {
			return 1 / (x * x);
		}

		if (x >= C_LIMIT) {
			double inv = 1 / (x * x);
			// 1 1 1 1 1
			// - + ---- + ---- - ----- + -----
			// x 2 3 5 7
			// 2 x 6 x 30 x 42 x
			return 1 / x + inv / 2 + inv / x
					* (1.0 / 6 - inv * (1.0 / 30 + inv / 42));
		}

		return trigamma(x + 1) + 1 / (x * x);
	}

	/**
	 * Non-recursive version, truncated Taylor series of d/dx Psi(x) = d^2/dx^2
	 * Gamma(x). From lda-c
	 * 
	 * @param x
	 * @return
	 */
	public static double trigamma0(double x) {
		double p;
		int i;
		assert x > 0 : "trigamma(" + x + ")";

		x = x + 6;
		p = 1 / (x * x);
		p = (((((0.075757575757576 * p - 0.033333333333333) * p + 0.0238095238095238)
				* p - 0.033333333333333)
				* p + 0.166666666666667)
				* p + 1)
				/ x + 0.5 * p;
		for (i = 0; i < 6; i++) {
			x = x - 1;
			p = 1 / (x * x) + p;
		}
		return (p);
	}

	/**
	 * Recursive implementation of the factorial.
	 * 
	 * @param i
	 * @return
	 */
	public static long fak(int i) {
		if (i > 1) {
			return i * fak(i - 1);
		}
		return 1;
	}

	/**
	 * Pochhammer function
	 * 
	 * @author Gregor Heinrich (after Thomas Minka's fastfit implementation)
	 * @param x
	 * @param n
	 * @return
	 */
	double pochhammer(double x, int n) {
		double result;
		if (n == 0)
			return 0;
		if (n <= 20) {
			int i;
			double xi = x;
			/* this assumes x is not too large */
			result = xi;
			for (i = n - 1; i > 0; i--) {
				xi = xi + 1;
				result *= xi;
			}
			result = log(result);
		} else if (x >= 1.e4 * n) {
			result = log(x) + (n - 1) * log(x + n / 2);
		} else
			result = lgamma(x + n) - lgamma(x);
		return result;
	}

	/**
	 * Pochhammer digamma function
	 * 
	 * @author Gregor Heinrich (after Thomas Minka's fastfit implementation)
	 * @param x
	 * @param n
	 * @return
	 */
	public static double diPochhammer(double x, int n) {
		double result;
		if (n == 0)
			return 0;
		if (n <= 20) {
			int i;
			double xi = x;
			result = 1 / xi;
			for (i = n - 1; i > 0; i--) {
				xi = xi + 1;
				result += 1 / xi;
			}
		} else
			result = digamma(x + n) - digamma(x);
		return result;
	}

	/**
	 * Pochhammer trigamma function.
	 * 
	 * @author Gregor Heinrich (after Thomas Minka's fastfit implementation)
	 * @param x
	 * @param n
	 * @return
	 */
	public static double triPochhammer(double x, int n) {
		double result;
		if (n == 0)
			return 0;
		if (n <= 20) {
			result = -1 / (x * x);
			n--;
			while (n > 0) {
				x = x + 1;
				result -= 1 / (x * x);
				n--;
			}
			return result;
		}
		return trigamma(x + n) - trigamma(x);
	}
}
