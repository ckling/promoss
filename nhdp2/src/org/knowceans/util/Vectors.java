/*
 * Created on Jun 18, 2004 To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
/*
 * Copyright (c) 2004-2006 Gregor Heinrich. All rights reserved. Redistribution and
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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Vector;

/**
 * Static vector manipulation routines for Matlab porting and other numeric
 * operations. The routines work for int and double partly; the class is
 * extended as needed.
 * <p>
 * TODO: The question remains whether it makes sense to have a formal IntVector,
 * analoguous to IntMatrix that allows performing search and indexing
 * operations, such as views, etc.
 * 
 * @author heinrich
 */
public class Vectors {

    protected static int ndigits = 0;
    protected static int colwidth = 0;

    /**
     * Set the format of the following double print calls to the column width
     * and number of digits. Note that this is a static setting, it can be
     * undone using unsetFormat.
     * 
     * @param colwidth
     * @param ndigits
     */
    public static final void setFormat(int colwidth, int ndigits) {
        Vectors.colwidth = colwidth;
        Vectors.ndigits = ndigits;
    }

    /**
     * Get the format settings for doubles.
     * 
     * @return
     */
    public static final int[] getFormat() {
        return new int[] { colwidth, ndigits };
    }

    /**
     * Unset the format settings for doubles.
     */
    public static final void unsetFormat() {
        Vectors.colwidth = 0;
        Vectors.ndigits = 0;
    }

    /**
     * @param start
     * @param end
     * @param step
     * @return [start : step : end]
     */
    public static int[] range(int start, int end, int step) {

        int[] out = new int[(int) Math.floor((end - start) / step) + 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = start + step * i;
        }
        return out;
    }

    /**
     * @param start
     * @param end
     * @return [start : end]
     */
    public static int[] range(int start, int end) {
        return range(start, end, end - start > 0 ? 1 : -1);
    }

    /**
     * create sequence [start : step : end] of double values. TODO: check
     * precision.
     * 
     * @param start
     *            double value of start, if integer, use "1.0" notation.
     * @param end
     *            double value of end, if integer, use "1.0" notation.
     * @param step
     *            double value of step size
     * @return
     */
    public static double[] range(double start, double end, double step) {

        double[] out = new double[(int) Math.floor((end - start) / step) + 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = start + step * i;
            // System.out.println(step * i + " " + i);
        }
        return out;
    }

    /**
     * @param start
     * @param end
     * @return [start : end]
     */
    public static double[] range(double start, double end) {
        return range(start, end, end - start > 0 ? 1 : -1);
    }

    /**
     * sum the elements of vec
     * 
     * @param vec
     * @return
     */
    public static double sum(double[] vec) {
        double sum = 0;
        for (int i = 0; i < vec.length; i++) {
            sum += vec[i];
        }
        return sum;

    }

    /**
     * product of the values in vec
     * 
     * @param vec
     * @return
     */
    public static double product(double[] vec) {
        double prod = 1;
        for (int i = 0; i < vec.length; i++) {
            prod *= vec[i];
        }
        return prod;
    }

    /**
     * sum the elements of vec
     * 
     * @param vec
     * @return
     */
    public static double sum(float[] vec) {
        double sum = 0;
        for (int i = 0; i < vec.length; i++) {
            sum += vec[i];
        }
        return sum;

    }

    /**
     * sum the elements of vec
     * 
     * @param vec
     * @return
     */
    public static int sum(int vec[]) {
        int sum = 0;
        for (int i = 0; i < vec.length; i++)
            sum += vec[i];

        return sum;
    }

    /**
     * cumulative sum of the elements, starting at element 0.
     * 
     * @param vec
     * @return vector containing the cumulative sum of the elements of vec
     */
    public static double[] cumsum(double[] vec) {
        double[] x = new double[vec.length];
        x[0] = vec[0];
        for (int i = 1; i < vec.length; i++) {
            x[i] = vec[i] + x[i - 1];
        }
        return x;
    }

    public static float[] cumsum(float[] vec) {
        float[] x = new float[vec.length];
        x[0] = vec[0];
        for (int i = 1; i < vec.length; i++) {
            x[i] = vec[i] + x[i - 1];
        }
        return x;
    }

    /**
     * maximum value in vec
     * 
     * @param vec
     * @return
     */
    public static int max(int[] vec) {
        int max = vec[0];
        for (int i = 1; i < vec.length; i++) {
            if (vec[i] > max)
                max = vec[i];
        }
        return max;
    }

    /**
     * maximum value in vec
     * 
     * @param vec
     * @return
     */
    public static double max(double[] vec) {
        double max = vec[0];
        for (int i = 1; i < vec.length; i++) {
            if (vec[i] > max)
                max = vec[i];
        }
        return max;
    }

    public static double max(float[] vec) {
        float max = vec[0];
        for (int i = 1; i < vec.length; i++) {
            if (vec[i] > max)
                max = vec[i];
        }
        return max;
    }

    /**
     * minimum value in vec
     * 
     * @param vec
     * @return
     */
    public static int min(int[] vec) {
        int min = vec[0];
        for (int i = 1; i < vec.length; i++) {
            if (vec[i] < min)
                min = vec[i];
        }
        return min;
    }

    /**
     * minimum value in vec
     * 
     * @param vec
     * @return
     */
    public static double min(double[] vec) {
        double min = vec[0];
        for (int i = 1; i < vec.length; i++) {
            if (vec[i] < min)
                min = vec[i];
        }
        return min;
    }

    public static float min(float[] vec) {
        float min = vec[0];
        for (int i = 1; i < vec.length; i++) {
            if (vec[i] < min)
                min = vec[i];
        }
        return min;
    }

    /**
     * rounds matrix and converts to integer
     * 
     * @param x
     * @return
     */
    public static int[][] round(double[][] x) {
        int[][] y = new int[x.length][];
        for (int i = 0; i < y.length; i++) {
            y[i] = round(x[i]);
        }
        return y;
    }

    public static int[][] round(float[][] x) {
        int[][] y = new int[x.length][];
        for (int i = 0; i < y.length; i++) {
            y[i] = round(x[i]);
        }
        return y;
    }

    /**
     * rounds vector and converts to integer
     * 
     * @param x
     * @return
     */
    public static int[] round(double[] x) {
        int[] y = new int[x.length];
        for (int i = 0; i < y.length; i++) {
            y[i] = (int) Math.round(x[i]);
        }
        return y;
    }

    public static int[] round(float[] x) {
        int[] y = new int[x.length];
        for (int i = 0; i < y.length; i++) {
            y[i] = Math.round(x[i]);
        }
        return y;
    }

    /**
     * @param x
     * @param y
     * @return [x y]
     */
    public static double[] concat(double[] x, double[] y) {
        double[] z = new double[x.length + y.length];
        System.arraycopy(x, 0, z, 0, x.length);
        System.arraycopy(y, 0, z, x.length, y.length);
        return z;
    }

    /**
     * @param x
     * @param y
     * @return [x y]
     */
    public static int[] concat(int[] x, int[] y) {
        int[] z = new int[x.length + y.length];
        System.arraycopy(x, 0, z, 0, x.length);
        System.arraycopy(y, 0, z, x.length, y.length);
        return z;
    }

    /**
     * @param x
     * @param y
     * @param rowwise
     *            ? [x; y] : [x, y]
     * @return [x {;,} y]
     */
    public static double[][] concat(double[][] x, double[] y, boolean rowwise) {

        if (rowwise) {
            double[][] a = new double[1][];
            a[1] = y;
            x = concat(x, a, true);
            return x;

        } else {
            if (x.length != y.length) {
                throw new IllegalArgumentException(
                        "[x; y] needs x and y to have equal column dimensions.");
            }
            double[][] z = new double[x.length][];
            for (int i = 0; i < x.length; i++) {
                z[i] = new double[x[i].length + y.length];
                z[i][z[i].length - 1] = y[i];
                System.arraycopy(x[i], 0, z[i], 0, x[i].length);
            }
            return z;
        }
    }

    /**
     * @param x
     * @param y
     * @param rowwise
     *            ? [x; y] : [x, y]
     * @return [x {;,} y]
     */
    public static double[][] concat(double[][] x, double[][] y, boolean rowwise) {
        if (rowwise) {
            double[][] z = new double[x.length + y.length][];
            System.arraycopy(x, 0, z, 0, x.length);
            System.arraycopy(y, 0, z, x.length, y.length);
            return z;
        } else {
            if (x.length != y.length) {
                throw new IllegalArgumentException(
                        "[x; y] needs x and y to have equal column dimensions.");
            }
            double[][] z = new double[x.length][];
            for (int i = 0; i < x.length; i++) {
                z[i] = new double[x[i].length + y[i].length];
                System.arraycopy(x[i], 0, z[i], 0, x[i].length);
                System.arraycopy(y[i], 0, z[i], x[i].length, y[i].length);
            }
            return z;
        }
    }

    /**
     * @param x
     * @param y
     * @param rowwise
     *            ? [x; y] : [x, y]
     * @return [x {;,} y]
     */
    public static int[][] concat(int[][] x, int[][] y, boolean rowwise) {
        if (rowwise) {
            int[][] z = new int[x.length + y.length][];
            System.arraycopy(x, 0, z, 0, x.length);
            System.arraycopy(y, 0, z, x.length, y.length);
            return z;
        } else {
            if (x.length != y.length) {
                throw new IllegalArgumentException(
                        "[x; y] needs x and y to have equal column dimensions.");
            }
            int[][] z = new int[x.length][];
            for (int i = 0; i < x.length; i++) {
                z[i] = new int[x[i].length + y[i].length];
                System.arraycopy(x[i], 0, z[i], 0, x[i].length);
                System.arraycopy(y[i], 0, z[i], x[i].length, y[i].length);
            }
            return z;
        }
    }

    /**
     * create matroid with matrix a as element repeated for rows and cols
     * 
     * @param a
     * @param rows
     * @param cols
     * @return
     */
    public static double[][] repmat(double[][] a, int rows, int cols) {
        double[][] b = (double[][]) ArrayUtils.copy(a);
        for (int i = 0; i < cols; i++) {
            b = concat(b, a, false);
        }
        double[][] c = (double[][]) ArrayUtils.copy(b);
        for (int i = 0; i < rows; i++) {
            c = concat(c, b, true);
        }
        return c;
    }

    /**
     * create matroid with matrix a as element repeated for rows and cols
     * 
     * @param a
     * @param rows
     * @param cols
     * @return
     */
    public static int[][] repmat(int[][] a, int rows, int cols) {
        int[][] b = (int[][]) ArrayUtils.copy(a);
        for (int i = 0; i < cols; i++) {
            b = concat(b, a, false);
        }
        int[][] c = (int[][]) ArrayUtils.copy(b);
        for (int i = 0; i < rows; i++) {
            c = concat(c, b, true);
        }
        return c;
    }

    /**
     * w = [x y z]
     * 
     * @param x
     * @param y
     * @return [x y z]
     */
    public static double[] concat(double[] x, double[] y, double[] z) {
        double[] w = new double[x.length + y.length + z.length];
        System.arraycopy(x, 0, w, 0, x.length);
        System.arraycopy(y, 0, w, x.length, y.length);
        System.arraycopy(y, 0, w, x.length + y.length, z.length);
        return w;
    }

    /**
     * Create new vector of larger size and data of the argument.
     * 
     * @param vector
     *            source array
     * @param moreelements
     *            number of elements to add
     * @return larger vector
     */
    public static double[] increaseSize(final double[] vector, int moreelements) {
        double[] longer = new double[vector.length + moreelements];
        System.arraycopy(vector, 0, longer, 0, vector.length);
        return longer;
    }

    /**
     * Create new matrix of larger size and data of the argument.
     * 
     * @param matrix
     * @param more
     *            rows
     * @param more
     *            cols
     * @return larger matrix
     */
    public static double[][] increaseSize(final double[][] matrix,
            int morerows, int morecols) {

        double[][] array2 = new double[matrix.length + morerows][];
        for (int i = 0; i < matrix.length; i++) {
            array2[i] = (morecols > 0) ? increaseSize(matrix[i], morecols)
                    : matrix[i];
        }
        for (int i = matrix.length; i < array2.length; i++) {
            array2[i] = new double[matrix[0].length + morecols];
        }

        return array2;
    }

    /**
     * Create new vector with data of the argument and removed element.
     * 
     * @param vector
     * @param element
     * @return shorter vector
     */
    public static double[] removeElement(final double[] vector, int element) {
        double[] shorter = new double[vector.length - 1];
        System.arraycopy(vector, 0, shorter, 0, element);
        System.arraycopy(vector, element + 1, shorter, element, vector.length
                - element - 1);
        return shorter;
    }

    /**
     * Create new matrix with data of the argument and removed rows and columns.
     * 
     * @param matrix
     * @param rows
     *            ordered vector of rows to remove
     * @param cols
     *            ordered vector of cols to remove
     * @return smaller matrix
     */
    public static double[][] removeElements(final double[][] matrix,
            int[] rows, int[] cols) {
        return chooseElements(matrix, rangeComplement(rows, matrix.length),
            rangeComplement(cols, matrix[0].length));
    }

    /**
     * Create new vector with data of the argument and removed elements.
     * 
     * @param vector
     * @param elements
     *            ordered elements to remove
     * @return smaller vector
     */
    public static double[] removeElements(final double[] vector, int[] elements) {
        return chooseElements(vector, rangeComplement(elements, vector.length));
    }

    /**
     * return the complement of the sorted subset of the set 0:length-1 in
     * Matlab notation
     * 
     * @param set
     *            sorted set of elements < length
     * @param length
     *            of superset of set and its returned complement
     * @return
     */
    public static int[] rangeComplement(int[] set, int length) {
        int[] complement = new int[length - set.length];
        int sindex = 0;
        int cindex = 0;
        for (int i = 0; i < length; i++) {
            if (sindex >= set.length || set[sindex] != i) {
                complement[cindex] = i;
                cindex++;
            } else {
                sindex++;
            }
        }
        return complement;
    }

    /** get the column of the matrix */
    public static int[] getColumn(int[][] matrix, int col) {
        int[] a = new int[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            a[i] = matrix[i][col];
        }
        return a;
    }

    /** get the column of the matrix */
    public static double[] getColumn(double[][] matrix, int col) {
        double[] a = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            a[i] = matrix[i][col];
        }
        return a;
    }

    /**
     * Create a matrix that contains the rows and columns of the argument matrix
     * in the order given by rows and cols
     * 
     * @param matrix
     * @param rows
     * @param cols
     * @return
     */
    public static double[][] chooseElements(double[][] matrix, int[] rows,
            int[] cols) {

        double[][] matrix2 = new double[rows.length][cols.length];

        for (int i = 0; i < rows.length; i++) {
            matrix2[i] = chooseElements(matrix[rows[i]], cols);
        }

        return matrix2;
    }

    /**
     * Create vector that contains the elements of the argument in the order as
     * given by keep
     * 
     * @param vector
     * @param keep
     * @return
     */
    public static double[] chooseElements(double[] vector, int[] keep) {
        double[] vector2 = new double[keep.length];

        for (int i = 0; i < keep.length; i++) {
            vector2[i] = vector[keep[i]];
        }
        return vector2;
    }

    /**
     * Extract the column from the 2-dim matrix / array (stored row-wise in
     * Java)
     * 
     * @param matrix
     * @param col
     *            column number to choose (must exist in each row of the matrix)
     * @return
     */
    public static double[] chooseColumn(double[][] matrix, int col) {
        double[] column = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            column[i] = matrix[i][col];
        }
        return column;
    }

    /**
     * Create new vector of larger size and data of the argument.
     * 
     * @param vector
     *            source array
     * @param moreelements
     *            number of elements to add
     * @return larger vector
     */
    public static int[] increaseSize(final int[] vector, int moreelements) {
        int[] longer = new int[vector.length + moreelements];
        System.arraycopy(vector, 0, longer, 0, vector.length);
        return longer;
    }

    /**
     * Create new matrix of larger size and data of the argument.
     * 
     * @param matrix
     * @param more
     *            rows
     * @param more
     *            cols
     * @return larger matrix
     */
    public static int[][] increaseSize(final int[][] matrix, int morerows,
            int morecols) {

        int[][] array2 = new int[matrix.length + morerows][];
        for (int i = 0; i < matrix.length; i++) {
            array2[i] = (morecols > 0) ? increaseSize(matrix[i], morecols)
                    : matrix[i];
        }
        for (int i = matrix.length; i < array2.length; i++) {
            array2[i] = new int[matrix[0].length + morecols];
        }

        return array2;
    }

    /**
     * Create new vector with data of the argument and removed element.
     * 
     * @param vector
     * @param element
     * @return shorter vector
     */
    public static int[] removeElement(final int[] vector, int element) {
        int[] shorter = new int[vector.length - 1];
        System.arraycopy(vector, 0, shorter, 0, element);
        System.arraycopy(vector, element + 1, shorter, element, vector.length
                - element - 1);
        return shorter;
    }

    /**
     * Create new matrix with data of the argument and removed rows and columns.
     * 
     * @param matrix
     * @param rows
     *            ordered vector of rows to remove
     * @param cols
     *            ordered vector of cols to remove
     * @return smaller matrix
     */
    public static int[][] removeElements(final int[][] matrix, int[] rows,
            int[] cols) {
        return chooseElements(matrix, rangeComplement(rows, matrix.length),
            rangeComplement(cols, matrix[0].length));
    }

    /**
     * Create new vector with data of the argument and removed elements.
     * 
     * @param vector
     * @param elements
     *            ordered elements to remove
     * @return smaller vector
     */
    public static int[] removeElements(final int[] vector, int[] elements) {
        return chooseElements(vector, rangeComplement(elements, vector.length));
    }

    /**
     * Create a matrix that contains the rows and columns of the argument matrix
     * in the order given by rows and cols
     * 
     * @param matrix
     * @param rows
     * @param cols
     * @return
     */
    public static int[][] chooseElements(int[][] matrix, int[] rows, int[] cols) {

        int[][] matrix2 = new int[rows.length][cols.length];

        for (int i = 0; i < rows.length; i++) {
            matrix2[i] = chooseElements(matrix[rows[i]], cols);
        }

        return matrix2;
    }

    /**
     * Create a matrix that contains the rows of the argument matrix in the
     * order given by rows. This method does not copy the rows.
     * 
     * @param matrix
     * @param rows
     * @return
     */
    public static int[][] chooseElements(int[][] matrix, int[] rows) {

        int[][] matrix2 = new int[rows.length][];

        for (int i = 0; i < rows.length; i++) {
            matrix2[i] = matrix[rows[i]];
        }

        return matrix2;
    }

    /**
     * Create vector that contains the elements of the argument in the order as
     * given by keep
     * 
     * @param vector
     * @param keep
     * @return
     */
    public static int[] chooseElements(int[] vector, int[] keep) {
        int[] vector2 = new int[keep.length];

        for (int i = 0; i < keep.length; i++) {
            vector2[i] = vector[keep[i]];
        }
        return vector2;
    }

    /**
     * prints a double representation of the vector.
     * 
     * @param x
     * @return
     */
    public static String print(double[] x) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < x.length - 1; i++) {
            b.append(format(x[i])).append(" ");
        }
        if (x.length > 0)
            b.append(format(x[x.length - 1]));
        return b.toString();
    }

    public static String print(float[] x) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < x.length - 1; i++) {
            b.append(format(x[i])).append(" ");
        }
        if (x.length > 0)
            b.append(format(x[x.length - 1]));
        return b.toString();
    }

    public static String print(Object x) {
        if (x instanceof Object[]) {
            return print((Object[]) x);
        } else if (x instanceof int[]) {
            return print((int[]) x);
        } else if (x instanceof double[]) {
            return print((double[]) x);
        } else if (x instanceof float[]) {
            return print((float[]) x);
        } else
            return null;
    }

    /**
     * Print the array of objects via their toString() methods.
     * 
     * @param x
     * @return
     */
    public static String print(Object[] x) {
        return print(x, " ");
    }

    /**
     * Print the array of objects via their toString() methods, using the
     * delimiter.
     * 
     * @param x
     * @param delim
     * @return
     */
    public static String print(Object[] x, String delim) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < x.length - 1; i++) {
            b.append(x[i]).append(delim);
        }
        if (x.length > 0)
            b.append(x[x.length - 1]);
        return b.toString();
    }

    /**
     * Print the double array of objects via their toString() methods, using the
     * delimiter.
     * 
     * @param x
     * @param delim
     * @return
     */
    public static <T> String printf(T x, String format, String delim) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        int len = Array.getLength(x);
        for (int i = 0; i < Array.getLength(x) - 1; i++) {
            b.append(String.format(format, Array.get(x, i))).append(delim);
        }
        if (len > 0)
            b.append(String.format(format, Array.get(x, len - 1)));
        return b.toString();
    }

    private static String format(double x) {
        if (ndigits > 0) {
            return DoubleFormat.format(x, ndigits, colwidth);
        } else {
            return Double.toString(x);
        }
    }

    /**
     * prints a double representation of an array.
     * 
     * @param x
     * @return
     */
    public static String print(double[][] x) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < x.length - 1; i++) {
            b.append(print(x[i])).append("\n");
        }
        if (x.length > 0)
            b.append(print(x[x.length - 1]));
        return b.toString();
    }

    public static String print(float[][] x) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < x.length - 1; i++) {
            b.append(print(x[i])).append("\n");
        }
        if (x.length > 0)
            b.append(print(x[x.length - 1]));
        return b.toString();
    }

    /**
     * prints a double representation of the vector.
     * 
     * @param x
     * @return
     */
    public static String print(int[] x) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < x.length - 1; i++) {
            b.append(x[i]).append(" ");
        }
        if (x.length > 0)
            b.append(x[x.length - 1]);
        return b.toString();
    }

    /**
     * prints a double representation of an array.
     * 
     * @param x
     * @return
     */
    public static String print(int[][] x) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < x.length - 1; i++) {
            b.append(print(x[i])).append("\n");
        }
        if (x.length > 0)
            b.append(print(x[x.length - 1]));
        return b.toString();
    }

    /**
     * prints a double representation of an array.
     * 
     * @param x
     * @return
     */
    public static String print(int[][] x, String delim) {
        if (x == null)
            return "null";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < x.length - 1; i++) {
            b.append(print(x[i])).append(delim);
        }
        if (x.length > 0)
            b.append(print(x[x.length - 1]));
        return b.toString();
    }

    /**
     * @param len
     * @param factor
     * @return factor * ones(1, len);
     */
    public static double[] ones(int len, double factor) {
        double[] x = new double[len];
        for (int i = 0; i < x.length; i++) {
            x[i] = factor;
        }
        return x;
    }

    /**
     * @param len
     * @param factor
     * @return factor * ones(1, len);
     */
    public static int[] ones(int len, int factor) {
        int[] x = new int[len];
        for (int i = 0; i < x.length; i++) {
            x[i] = factor;
        }
        return x;
    }

    /**
     * @param len
     * @return zeros(1, len)
     */
    public static double[] zeros(int len) {
        return new double[len];
    }

    /**
     * @param len
     * @return ones(1, len)
     */
    public static int[] ones(int len) {
        return ones(len, 1);
    }

    /**
     * cast a double[] to an int[]
     * 
     * @param vec
     * @return
     */
    public static int[] cast(double[] vec) {
        int[] ivec = new int[vec.length];
        for (int i = 0; i < ivec.length; i++) {
            ivec[i] = (int) vec[i];
        }
        return ivec;
    }

    /**
     * cast a double[] to an int[]
     * 
     * @param vec
     * @return
     */
    public static double[] cast(int[] vec) {
        double[] dvec = new double[vec.length];
        for (int i = 0; i < dvec.length; i++) {
            dvec[i] = (double) vec[i];
        }
        return dvec;
    }

    /**
     * find indices with val
     * 
     * @param vec
     * @param val
     * @return vector with 0-based indices.
     */
    public static int[] find(int[] vec, int val) {
        Vector<Integer> v = new Vector<Integer>();
        for (int i = 0; i < vec.length; i++) {
            if (vec[i] == val) {
                v.add(new Integer(i));
            }
        }
        int[] vv = new int[v.size()];
        for (int i = 0; i < vv.length; i++) {
            vv[i] = ((Integer) v.get(i)).intValue();
        }
        return vv;
    }

    /**
     * returns a copy of the vector elements with the given indices in the
     * original vector.
     * 
     * @param indices
     * @return
     */
    public static double[] subVector(double[] vec, int[] indices) {
        double[] x = new double[indices.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = vec[indices[i]];
        }
        return x;
    }

    /**
     * returns a copy of the vector elements with the given indices in the
     * original vector.
     * 
     * @param cols
     * @return
     */
    public static int[] subVector(int[] vec, int[] indices) {
        int[] x = new int[indices.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = vec[indices[i]];
        }
        return x;
    }

    /**
     * @param weights
     * @param i
     * @param j
     * @return
     */
    public static double[] subVector(double[] vec, int start, int end) {
        double[] x = new double[end - start + 1];
        for (int i = 0; i <= end - start; i++) {
            x[i] = vec[start + i];
        }
        return x;
    }

    /**
     * @param weights
     * @param i
     * @param j
     * @return
     */
    public static double[] sub(double[] vec, int start, int length) {
        double[] x = new double[length];
        for (int i = 0; i < length; i++) {
            x[i] = vec[start + i];
        }
        return x;
    }

    /**
     * @param weights
     * @param i
     * @param j
     * @return
     */
    public static int[] sub(int[] vec, int start, int length) {
        int[] x = new int[length];
        for (int i = 0; i < length; i++) {
            x[i] = vec[start + i];
        }
        return x;
    }

    /**
     * set the elements of vec at indices with the respective replacements.
     * TODO: implement views as in the colt library
     * 
     * @param vec
     * @param indices
     * @param replacements
     * @return
     */
    public static void setSubVector(int[] vec, int[] indices, int[] replacements) {
        for (int i = 0; i < indices.length; i++) {
            vec[indices[i]] = replacements[i];
        }
    }

    /**
     * set the elements of vec at indices with the replacement. TODO: implement
     * views as in the colt library
     * 
     * @param vec
     * @param indices
     * @param replacement
     * @return
     */
    public static void setSubVector(int[] vec, int[] indices, int replacement) {
        for (int i = 0; i < indices.length; i++) {
            vec[indices[i]] = replacement;
        }
    }

    /**
     * add a scalar to the vector
     * 
     * @param vec
     * @param scalar
     */
    public static void add(int[] vec, int scalar) {
        for (int i = 0; i < vec.length; i++) {
            vec[i] += scalar;
        }
    }

    /**
     * add a scalar to the vector. This creates a new double vector.
     * 
     * @param vec
     * @param scalar
     */
    public static double[] add(int[] vec, double scalar) {
        double[] aa = new double[vec.length];
        for (int i = 0; i < vec.length; i++) {
            aa[i] = scalar + vec[i];
        }
        return aa;
    }

    /**
     * add a scalar to the matrix
     * 
     * @param vec
     * @param scalar
     */
    public static void add(double[][] mat, double scalar) {
        for (int i = 0; i < mat.length; i++) {
            add(mat[i], scalar);
        }
    }

    /**
     * add a scalar to the vector
     * 
     * @param vec
     * @param scalar
     */
    public static void add(double[] vec, double scalar) {
        for (int i = 0; i < vec.length; i++) {
            vec[i] += scalar;
        }
    }

    /**
     * a+=b
     * 
     * @param a
     * @param b
     */
    public static void add(double[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] += b[i];
        }
    }

    /**
     * a+=b
     * 
     * @param a
     * @param b
     */
    public static void add(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] += b[i];
        }
    }

    /**
     * a+=b
     * 
     * @param a
     * @param b
     */
    public static void add(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] += b[i];
        }
    }

    /**
     * a-=b
     * 
     * @param a
     * @param b
     */
    public static void subtract(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] -= b[i];
        }
    }

    /**
     * a-=b
     * 
     * @param a
     * @param b
     */
    public static void subtract(double[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] -= b[i];
        }
    }

    /**
     * a-=b
     * 
     * @param a
     * @param b
     */
    public static void subtract(int[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] -= b[i];
        }
    }

    /**
     * a-=b
     * 
     * @param a
     * @param b
     */
    public static void subtract(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] -= b[i];
        }
    }

    /**
     * squared euclidean distance between vectors.
     * 
     * @param a
     * @param b
     * @return
     */
    public static double sqdist(double[] a, double[] b) {
        double dist = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            dist += diff * diff;
        }
        return dist;
    }

    /**
     * squared absolute value of vector
     * 
     * @param a
     * @return
     */
    public static double sqabs(double[] a) {
        double mag = 0;
        for (int i = 0; i < a.length; i++) {
            mag += a[i] * a[i];
        }
        return mag;
    }

    /**
     * set the elements of a copy of vec at indices with the respective
     * replacements. TODO: implement views as in the colt library
     * 
     * @param vec
     * @param indices
     * @param replacements
     * @return the copied vector with the replacements;
     */
    public static int[] setSubVectorCopy(int[] vec, int[] indices,
            int[] replacements) {
        int[] x = new int[vec.length];
        for (int i = 0; i < indices.length; i++) {
            x[indices[i]] = replacements[i];
        }
        return x;
    }

    /**
     * copies the vector of int or double
     * 
     * @param source
     * @return
     */
    public static Object copy(Object source) {
        if (source instanceof double[]) {
            return copy((double[]) source);
        } else if (source instanceof float[]) {
            return copy((float[]) source);
        } else if (source instanceof int[]) {
            return copy((int[]) source);
        }
        return null;
    }

    /**
     * copies a the source to the destination
     * 
     * @param alpha
     * @return
     */
    public static double[] copy(double[] source) {
        if (source == null)
            return null;
        double[] dest = new double[source.length];
        // System.arraycopy(source, 0, dest, 0, source.length);
        for (int i = 0; i < dest.length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }

    public static float[] copy(float[] source) {
        if (source == null)
            return null;
        float[] dest = new float[source.length];
        // System.arraycopy(source, 0, dest, 0, source.length);
        for (int i = 0; i < dest.length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }

    /**
     * copies a the source to the destination
     * 
     * @param alpha
     * @return
     */
    public static int[] copy(int[] source) {
        if (source == null)
            return null;
        int[] dest = new int[source.length];
        System.arraycopy(source, 0, dest, 0, source.length);
        return dest;
    }

    /**
     * copies a the source to the destination
     * 
     * @param alpha
     * @return
     */
    public static double[] copyDouble(int[] source) {
        if (source == null)
            return null;
        double[] dest = new double[source.length];
        for (int i = 0; i < source.length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }

    /**
     * copy source to dest (no generics to keep performant)
     * 
     * @param source
     * @param dest
     */
    public static void copy(int[] source, double[] dest) {
        if (source == null)
            return;
        for (int i = 0; i < source.length; i++) {
            dest[i] = source[i];
        }
    }

    /**
     * copy source to dest (no generics to keep performant)
     * 
     * @param source
     * @param dest
     */
    public static void copy(double[] source, int[] dest) {
        if (source == null)
            return;
        for (int i = 0; i < source.length; i++) {
            dest[i] = (int) source[i];
        }
    }

    /**
     * copy source to dest (no generics to keep performant)
     * 
     * @param source
     * @param dest
     */
    public static void copy(int[] source, int[] dest) {
        if (source == null)
            return;
        for (int i = 0; i < source.length; i++) {
            dest[i] = source[i];
        }
    }

    /**
     * copy source to dest (no generics to keep performant)
     * 
     * @param source
     * @param dest
     */
    public static void copy(double[] source, double[] dest) {
        if (source == null)
            return;
        for (int i = 0; i < source.length; i++) {
            dest[i] = source[i];
        }
    }

    /**
     * multiplicates the vector with a scalar. The argument is modified.
     * 
     * @param ds
     * @param d
     * @return
     */
    public static void mult(double[] ds, double d) {
        for (int i = 0; i < ds.length; i++) {
            ds[i] *= d;
        }
    }

    /**
     * a *= b
     * 
     * @param a
     * @param scalar
     */
    public static void dotmult(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] *= b[i];
        }
    }

    /**
     * multiplicates the vector with a vector (inner product). The argument is
     * not modified.
     * 
     * @param ds
     * @param d
     * @return
     */
    public static double mult(double[] ds, double[] dt) {
        if (ds.length != dt.length)
            throw new IllegalArgumentException("Vector dimensions must agree.");
        double s = 0;
        for (int i = 0; i < ds.length; i++) {
            s += ds[i] * dt[i];
        }
        return s;
    }

    /**
     * transpose the matrix
     * 
     * @param mat
     * @return
     */
    public static double[][] transpose(double[][] mat) {
        double[][] a = new double[mat[0].length][mat.length];
        for (int i = 0; i < mat[0].length; i++) {
            for (int j = 0; j < mat.length; j++) {
                a[i][j] = mat[j][i];
            }
        }
        return a;
    }

    /**
     * transpose the matrix
     * 
     * @param mat
     * @return
     */
    public static int[][] transpose(int[][] mat) {
        int[][] a = new int[mat[0].length][mat.length];
        for (int i = 0; i < mat[0].length; i++) {
            for (int j = 0; j < mat.length; j++) {
                a[i][j] = mat[j][i];
            }
        }
        return a;
    }

    /**
     * check if argument is nan or infinity
     * 
     * @param alpha
     * @return
     */
    public static boolean isDegenerate(double alpha) {
        return Double.isInfinite(alpha) || Double.isNaN(alpha);
    }

    /**
     * check if argument contains nan or infinity
     * 
     * @param pp
     * @return
     */
    public static boolean isDegenerate(double[] ds) {
        for (int i = 0; i < ds.length; i++) {
            if (isDegenerate(ds[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if argument contains nan or infinity
     * 
     * @param pp
     * @return
     */
    public static boolean isDegenerate(double[][] pp) {
        for (int i = 0; i < pp.length; i++) {
            if (isDegenerate(pp[i])) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean isPositive(T ds) {
        if (ds instanceof double[][]) {
            return isPositive((double[][]) ds);
        } else if (ds instanceof float[][]) {
            return isPositive((float[][]) ds);
        } else if (ds instanceof int[][]) {
            return isPositive((int[][]) ds);
        } else if (ds instanceof double[]) {
            return isPositive((double[]) ds);
        } else if (ds instanceof float[]) {
            return isPositive((float[]) ds);
        } else {
            return isPositive((int[]) ds);
        }
    }

    /**
     * check if argument contains positive
     * 
     * @param pp
     * @return
     */
    public static boolean isPositive(double[] ds) {
        for (int i = 0; i < ds.length; i++) {
            if (ds[i] > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if argument positive
     * 
     * @param pp
     * @return
     */
    public static boolean isPositive(double[][] pp) {
        for (int i = 0; i < pp.length; i++) {
            if (isPositive(pp[i])) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean isNegative(T ds) {
        if (ds instanceof double[][]) {
            return isNegative((double[][]) ds);
        } else if (ds instanceof float[][]) {
            return isNegative((float[][]) ds);
        } else if (ds instanceof int[][]) {
            return isNegative((int[][]) ds);
        } else if (ds instanceof double[]) {
            return isNegative((double[]) ds);
        } else if (ds instanceof float[]) {
            return isNegative((float[]) ds);
        } else {
            return isNegative((int[]) ds);
        }
    }

    /**
     * check if argument contains positive
     * 
     * @param pp
     * @return
     */
    public static boolean isNegative(double[] ds) {
        for (int i = 0; i < ds.length; i++) {
            if (ds[i] < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if argument positive
     * 
     * @param pp
     * @return
     */
    public static boolean isNegative(double[][] pp) {
        for (int i = 0; i < pp.length; i++) {
            if (isNegative(pp[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if argument contains positive
     * 
     * @param pp
     * @return
     */
    public static boolean isPositive(int[] ds) {
        for (int i = 0; i < ds.length; i++) {
            if (ds[i] > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if argument positive
     * 
     * @param pp
     * @return
     */
    public static boolean isPositive(int[][] pp) {
        for (int i = 0; i < pp.length; i++) {
            if (isPositive(pp[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if argument contains positive
     * 
     * @param pp
     * @return
     */
    public static boolean isNegative(int[] ds) {
        for (int i = 0; i < ds.length; i++) {
            if (ds[i] < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if argument positive
     * 
     * @param pp
     * @return
     */
    public static boolean isNegative(int[][] pp) {
        for (int i = 0; i < pp.length; i++) {
            if (isNegative(pp[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks equality
     * 
     * @param a
     * @param b
     * @return
     */
    public static boolean isEqual(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * remove first element of the array that has value val by shifting all
     * subsequent elements of the vector up to (old size) len, setting the final
     * element to gapval.
     * 
     * @param x
     * @param val
     * @param gapval
     */
    public static void removeFirst(int[] x, int val, int len, int gapval) {
        // find element in array
        for (int k = 0; k < x.length; k++) {
            if (x[k] == val) {
                System.arraycopy(x, k + 1, x, k, len - k - 1);
                x[len - 1] = -1;
            }
        }
    }

    /**
     * remove first element of the array that has value val by shifting all
     * subsequent elements of the vector up to (old size) len, setting the final
     * element to gapval.
     * 
     * @param x
     * @param val
     * @param gapval
     */
    public static void removeFirst(double[] x, double val, int len,
            double gapval) {
        // find element in array
        for (int k = 0; k < x.length; k++) {
            if (x[k] == val) {
                System.arraycopy(x, k + 1, x, k, len - k - 1);
                x[len - 1] = gapval;
            }
        }
    }

    /**
     * remove first element of the array that has value val by shifting all
     * subsequent elements of the vector up to (old size) len, setting the final
     * element to gapval.
     * 
     * @param x
     * @param val
     * @param gapval
     */
    public static <T> void removeFirst(T[] x, T val, int len, T gapval) {
        // find element in array
        for (int k = 0; k < x.length; k++) {
            if (x[k] == val) {
                System.arraycopy(x, k + 1, x, k, len - k - 1);
                x[len - 1] = gapval;
            }
        }
    }

    /**
     * resize the array by copying it into a new array of size K. Changes the
     * pointer of the array.
     * 
     * @param x
     *            original array
     * @param newlen
     *            new length
     * @param fillval
     *            value to be filled into added elements
     * @return resized array
     */
    public static int[] resize(int[] x, int newlen, int fillval) {
        int[] u = new int[newlen];
        System.arraycopy(x, 0, u, 0, Math.min(newlen, x.length));
        for (int i = x.length; i < u.length; i++) {
            u[i] = fillval;
        }
        x = null;
        return u;
    }

    /**
     * resize the array by copying it into a new array of size K. Changes the
     * pointer of the array.
     * 
     * @param x
     *            original array
     * @param newlen
     *            new length
     * @param fillval
     *            value to be filled into added elements
     * @return resized array
     */
    public static double[] resize(double[] x, int newlen, double fillval) {
        double[] u = new double[newlen];
        System.arraycopy(x, 0, u, 0, Math.min(newlen, x.length));
        for (int i = x.length; i < u.length; i++) {
            u[i] = fillval;
        }
        return u;
    }

    /**
     * resize the array by copying it into a new array of size newlen and
     * filling it with null if grown. Changes the pointer of the array.
     * 
     * @param x
     *            original array
     * @param newlen
     *            new length
     * @param elementType
     *            type of one element, so zero-length arrays can be robustly
     *            handled. Can be null if x.length > 0 can be guaranteed.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] resize(T[] x, int newlen, T elementType) {
        Class<T> clazz;
        if (x.length > 0) {
            clazz = (Class<T>) x[0].getClass();
        } else {
            clazz = (Class<T>) elementType.getClass();
        }
        T[] u = (T[]) Array.newInstance(clazz, newlen);
        System.arraycopy(x, 0, u, 0, Math.min(newlen, x.length));
        return u;
    }
}
