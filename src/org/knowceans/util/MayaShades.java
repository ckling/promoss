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

import static java.lang.Math.floor;

/**
 * MayaShades creates Maya-esque visualisations of numeric data -- Maya-esqe
 * because the plotted values resemble the Maya numeric symbols. For
 * presentation of numerical data, the output has several advantages: (1) over
 * numeric data: because the eye directly recognises where in a matrix the large
 * values are located, (2) over visualised Hinton diagrams: because simple
 * console or text file output can be used to present the data. (In a way,
 * MayaShades is similar to the presentation in Hinton diagrams).
 * <p>
 * In addition to purely "shaded" numerical output, if data are over a specified
 * maximum value, the numeric value is shown instead, resulting in a "heavier"
 * shade that makes it consistent with the symbolic shades. This allows
 * presentation of a specific middle range of data with "analogue" shading,
 * which has proven very useful to visualise discrete probability distributions.
 * <p>
 * To layout the numeric data in an appropriate width, specific number
 * formatting (unavailable in the NumberFormat framework in the the Java API) is
 * provided for this class, which can be used independent of the shading
 * facility.
 * 
 * @author heinrich
 */
public class MayaShades {

    static String[] shades = {"     ", ".    ", ":    ", ":.   ", "::   ",
        "::.  ", ":::  ", ":::. ", ":::: ", "::::.", ":::::"};

    /**
     * Create a string representation of the vector.
     * 
     * @param dd
     * @param max
     * @return
     */
    public static String shadeDouble(double[] dd, double max) {
        StringBuffer b = new StringBuffer();
        for (double d : dd) {
            b.append(shadeDouble(d, max));
            b.append(" ");
        }
        return b.toString();
    }
    
    /**
     * create a string representation whose gray value appears as an indicator
     * of magnitude, cf. Hinton diagrams in statistics. A name for this kind of
     * presentation could be Quertzal from the Maya word Quetzal.
     * 
     * @param d
     *            value
     * @param max
     *            maximum value
     * @return
     */
    public static String shadeDouble(double d, double max) {
        int a = (int) floor(d * 10 / max + 0.5);
        if (a > 10 || a < 0) {
            String x = format(d, 3, 5);
            a = 5 - x.length();
            for (int i = 0; i < a; i++) {
                x += " ";
            }
            return "<" + x + ">";
        }
        return "[" + shades[a] + "]";
    }

    /**
     * formats a number to the specified number of digits
     * 
     * @param number
     * @param digits
     *            number of active digits
     * @param maxlength
     *            maximum length of string
     * @return
     */
    public static String format(double number, int digits, int maxlength) {
        String s = new String();
        s = format(number, digits);
        int padding = maxlength - s.length();
        if (padding < 0) {
            return s.substring(0, maxlength - 1) + "#";
        }
        for (int i = 0; i < padding; i++) {
            s = " " + s;
        }
        return s;
    }

    /**
     * formats a number to the specified number of digits
     * 
     * @param number
     * @param digits
     *            number of active digits
     * @return
     */
    public static String format(double number, int digits) {
        String s = new String();

        int savedigits = digits;

        if (number < 0)
            digits++;
        if (Math.abs(number) < 1) {
            s = Double.toString(number).substring(0, digits + 2);
        } else {
            int exponent = (int) (Math.log(number) / Math.log(10));
            // too large for
            if (exponent > digits - 1) {
                digits = digits - Integer.toString(exponent).length();
                number /= Math.pow(10, exponent);
                if (digits > 0) {
                    s = Double.toString(number).substring(0, digits + 1);
                    if (s.charAt(s.length() - 1) == '.') {
                        s = s.substring(0, s.length() - 1);
                    }
                    s += "e" + exponent;
                } else {
                    throw new NumberFormatException("Number too large for "
                        + savedigits + " digits.");
                }
            } else {
                s = Double.toString(number).substring(0, digits + 1);
                if (!s.contains(".") || s.endsWith(".")) {
                    s = s.substring(0, s.length() - 1);
                }
            }
        }
        return s;
    }

}
