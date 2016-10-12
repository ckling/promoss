package org.gesis.promoss.tools.math;

import java.math.BigDecimal;
import java.math.BigInteger;

public class FunctionsMath {

	//Gamma approx
	public static double gamma(double x)
	{
	    final double C0 = 1.0;
	    final double C1 = 1.0/12.0;
	    final double C2 = 1.0/288.0;
	    final double C3 = -139.0/51840.0;
	    final double C4 = -571.0/2488320.0;
	    final double C5 = 163879.0/209018880.0;
	    final double C6 = 5246819.0/75246796800.0;
	    final double C7 = -534703531.0/902961561600.0;
	    final double C8 = -4483131259.0/86684309913600.0;
	    final double C9 = 432261921612371.0/514904800886784000.0;
	 
	    double offset = 1;
	 
	    x -= 1;
	    while (x <= 15)
	    {
	        x += 1;
	        offset *= x;
	    }
	 
	    double z = 1/x;
	 
	    return Math.sqrt(2*Math.PI*x)*Math.pow(x/Math.E, x)
	    *(((((((((C9*z + C8)*z + C7)*z + C6)*z + C5)*z + C4)*z + C3)*z + C2)*z + C1)*z + C0)/offset;
	}
	
	/**
	 * Compute the natural logarithm of x to a given scale, x > 0.
	 * Use Newton's algorithm.
	 */
	public static BigDecimal lnNewton(BigDecimal x, int scale)
	{
	    int        sp1 = scale + 1;
	    BigDecimal n   = x;
	    BigDecimal term;

	    // Convergence tolerance = 5*(10^-(scale+1))
	    BigDecimal tolerance = BigDecimal.valueOf(5)
	                                        .movePointLeft(sp1);

	    // Loop until the approximations converge
	    // (two successive approximations are within the tolerance).
	    do {

	        // e^x
	        BigDecimal eToX = new BigDecimal(Math.E).pow(x.intValue());

	        // (e^x - n)/e^x
	        term = eToX.subtract(n)
	                    .divide(eToX, sp1, BigDecimal.ROUND_DOWN);

	        System.out.println(term);
	        
	        // x - (e^x - n)/e^x
	        x = x.subtract(term);

	        Thread.yield();
	    } while (term.compareTo(tolerance) > 0);

	    return x.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
	}
	
	public static double besseli (double v, double z) {
		
		//DANGER: INCOMPLETE!
		
		double result = Math.pow((z / 2),v);
			
		return result;
		
	}
	
		  //----------------
		  // Get square root
		  //----------------

		  public static BigDecimal sqrt (BigInteger n) {
		    return sqrt (new BigDecimal (n));
		  }

		  public static BigDecimal sqrt(BigDecimal n) {


			  BigDecimal ZERO = new BigDecimal ("0");
			  BigDecimal ONE = new BigDecimal ("1");
			  BigDecimal TWO = new BigDecimal ("2");
			  int maxIterations = 50;
			  int scale = 1000;
			  
		    // Make sure n is a positive number

		    if (n.compareTo (ZERO) <= 0) {
		      throw new IllegalArgumentException ();
		    }
		    
		    BigInteger integerPart = n.toBigInteger ();
		    int length = integerPart.toString ().length ();
		    if ((length % 2) == 0) {
		      length--;
		    }
		    length /= 2;
		    BigDecimal guess = ONE.movePointRight (length);

		    BigDecimal initialGuess = guess;
		    BigDecimal lastGuess = ZERO;
		    guess = new BigDecimal (initialGuess.toString ());

		    // Iterate

		    int iterations = 0;

		    while (true) {
		      lastGuess = guess;
		      guess = n.divide(guess, scale, BigDecimal.ROUND_HALF_EVEN);
		      guess = guess.add(lastGuess);
		      guess = guess.divide (TWO, scale, BigDecimal.ROUND_HALF_EVEN);
			if (++iterations >= maxIterations || lastGuess.equals (guess)) {
				return guess;
		      }
		    }

		  }
	
}
