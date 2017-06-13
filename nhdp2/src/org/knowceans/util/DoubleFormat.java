/*
 * Created on Jun 30, 2005
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

import java.util.Arrays;

/**
 * DoubleFormat formats double numbers into a specified digit format. The
 * difference to NumberFormat and DecimalFormat is that this DigitFormat is
 * based on a strategy to focus on a given number of nonzero digits, for which
 * it rounds numbers if necessary. In addition, it is possible to format a
 * number into a String that has a specified length, with a strategy to first
 * obey the significant-digit rule and then try to either space-pad the string
 * on the left or shorten the string by rounding, converting to
 * mantissa-exponent format or indicating overflow or underflow. Although the
 * class does not yield separator-aligned numbers, it yields a readable output.
 * 
 * @author heinrich
 */
public class DoubleFormat {

    public static void main(String[] args) {

        // test();
        run();
    }

    private static void run() {
        double[] a = new double[] {10229876.3, 1022.9876, 0.9876, 0.00009876,
            0.0987678, 99.987678, 9.87604E9, 9.976E10};

        for (int i = 12; i > 5; i--) {

            System.out.println("\nlength = " + i + " : ");
            for (int j = 0; j < a.length; j++) {
                System.out.println("[" + DoubleFormat.format(a[j], 6, i)
                    + "] <= (" + a[j] + ")");
                System.out.println("[" + DoubleFormat.format(-a[j], 6, i)
                    + "] <= (" + -a[j] + ")");
            }
        }
    }

    /**
     * Format the number x with n significant digits.
     * 
     * @param x
     * @param ndigits
     * @return
     */
    public static double format(double x, int ndigits) {
        int magnitude = ExpDouble.orderOfMagnitude(x);
        double factor = Math.pow(10, ndigits - 1 - magnitude);
        double y = Math.round(x * factor) / factor;

        return y;
    }

    /**
     * Format the number so it becomes exactly as large as the argument strlen.
     * The number of nonzero digits is tried to be ndigits. If strlen does not
     * have enough space for this, this number is reduced or the formatting
     * switched to exponential notation (in that priority order). If this is not
     * successful, null is returned;
     * <p>
     * TODO: debug so that lengths < 5 can be allowed TODO: debug conditions for
     * exponential notation TODO: debug truncation for mixed numbers
     * 
     * @param x the number to be formatted.
     * @param ndigits the maximum number of significant (non-zero) digits
     * @param strlen the exact number of characters in the string returned
     *        (positive for right-aligned, negative for left-aligned)
     * @return the formatted number string or null
     */
    static String format(double x, int ndigits, int strlen) {
        String s = null;
        boolean leftalign = false;
        if (strlen < 0) {
            strlen = -strlen;
            leftalign = true;
        }
        if (strlen < 5)
            throw new IllegalArgumentException("Cannot use yet abs(strlen) < 5");

        int pad = 0;

        ExpDouble d = new ExpDouble(x, ndigits);
        // System.out.println(d.debug());
        int len = d.strlen();
        pad = strlen - len;
        if (pad >= 0) {
            s = d.toString();
        } else {
            // if it has a fraction, it can be right-trucated
            if (d.exponent < 0) {
                // retry rounding if at least one digit can be displayed as
                // 0.00#
                int newdigits = d.digits - -pad;
                // at least one digit necessary
                if (newdigits > 0) {
                    boolean plusone = d.round(newdigits);
                    // if rounding up to next order of magnitude
                    if (plusone && newdigits > 1) {
                        d.round(newdigits - 1);
                    } else {
                        s = exponentialNotation(d, strlen);
                    }
                    s = d.toString();
                } else {
                    s = exponentialNotation(d, strlen);
                }
            } else {
                // try exponential notation (1.2E3)
                s = exponentialNotation(d, strlen);
            }
            pad = strlen - s.length();
        }

        String spc = space(Math.max(0, pad));
        if (leftalign) {
            return s + spc;
        } else {
            return spc + s;
        }
    }

    /**
     * @param d expdouble object
     * @param strlen maximum string length
     * @return number as string in exponential notation
     */
    private static String exponentialNotation(ExpDouble d, int strlen) {
        String s;
        int len;
        len = d.strlenexp();
        int minlen = d.minstrlenexp();
        if (minlen > strlen) {
            char[] cc = new char[strlen];
            if (d.exponent >= 0) {
                Arrays.fill(cc, '>');
            } else {
                Arrays.fill(cc, '<');
            }
            s = new String(cc);
        }

        // do all digits fit?
        int diff = strlen - len;
        if (diff == -1) {
            // at least two digits (with additional point
            d.round(d.digits + diff);
        } else {
            // only one digit (this yields minlen)
            d.round(1);
        }
        s = d.toExpString();
        return s;
    }

    private static String space(int pad) {
        char[] ch = new char[pad];
        Arrays.fill(ch, ' ');
        String spc = new String(ch);
        return spc;
    }

}
