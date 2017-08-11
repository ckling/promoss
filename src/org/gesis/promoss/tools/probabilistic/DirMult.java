package org.gesis.promoss.tools.probabilistic;

<<<<<<< HEAD
import org.gesis.promoss.tools.math.BasicMath;

=======
>>>>>>> 70b5662ad9b8fa2a07e6477243204ad8b19c49c8
/**
 * @author Christoph Carl Kling
 *
 * This class has all typical variables needed in probabilistic
 * models involving (mixtures of) hierarchical multinomial distributions with Dirichlet priors.
 * This class also can be used for hierarchical multi-Dirichlet processes.
 * 
 * The variables are based on the Chinese restaurant metaphor (customers and tables).
 * 
 * Additionally, I give standard functions for parameter estimation.
 *
 */
public class DirMult {
	
	//Number of categories / truncation level
<<<<<<< HEAD
	public int K;
	//Number of multinomial distributions
	public int M;	
	//Number of parent Dirichlet distributions / DPs
	public int P = 1;
	//prior distributions PxK
	public double[][] priors;
	//Category counts
	public float[][] nmk;
	//Table counts
	public float[][] mmk;
	//Log probability that counts are 0
	private float[] zero_nmk;
	//Log probability that tables are 0
	private float[] zero_mmk;
	
	public double getnGreater0(int k) {
		return 1.0-Math.exp(zero_nmk[k]);
	}
	public double getmGreater0(int k) {
		return 1.0-Math.exp(zero_mmk[k]);
	}
	
	public double[] getPrior() {
		double[] prior = priors[0];
		for (int p=1;p<P;p++) {
			for (int k=0;k<K;k++) {
			prior[k] += priors[p][k];
			}
		}
		return(prior);
	}
	
	/**
	 * @param m Index of the multinomial
	 * @return Geometric expectation of multinomial parameters (zeroth order)
	 */
	public double[] geometricP(int m) {
		
		double[] geometricP = new double[K];
		double[] prior = getPrior();
		double nsum = BasicMath.sum(nmk[m]);
		double priorsum = BasicMath.sum(prior);
		double sum = nsum + priorsum;
		
		for (int k=0;k<K;k++) {
			geometricP[k] = Math.exp(Gamma.digamma(nmk[m][k] + prior[k])/Gamma.digamma(sum));
		}
		return geometricP;
	}
	
	/**
	 * @param m Index of the multinomial
	 * @return Expectation of multinomial parameters (zeroth order)
	 */
	public double[] p(int m) {
		
		double[] p = new double[K];
		double[] prior = getPrior();
		double nsum = BasicMath.sum(nmk[m]);
		double priorsum = BasicMath.sum(prior);
		double sum = nsum + priorsum;
		
		for (int k=0;k<K;k++) {
			p[k] = (nmk[m][k] + prior[k])/sum;
		}
		return p;
	}
=======
	public double K;
	//Number of multinomial distributions
	public double M;	
	//Number of parent Dirichlet distributions / DPs
	public int P = 1;
	
	
	
>>>>>>> 70b5662ad9b8fa2a07e6477243204ad8b19c49c8

}
