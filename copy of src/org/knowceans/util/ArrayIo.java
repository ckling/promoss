/*
 * Copyright (c) 2006 Gregor Heinrich. All rights reserved. Redistribution and
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * MatrixIo provides methods to load and save matrices and vectors to file,
 * which automatic compression if the file extension given is ".zip". The
 * standard methods are load/saveBinaryMatrix that load/save a double[][] with
 * the protocol rows,cols,foreach(row): foreach(col): double[row][col] :end
 * :end.
 * <p>
 * Custom protocols for more complex data can be easily constructed by opening a
 * stream using one of the open*Stream() methods, using the methods of the
 * Data*Stream classes and read/write* methods of this class for matrices and
 * vectors and then closing the stream using the close*Stream methods in this
 * class (provided for symmetry) or the close() methods in the Data*Stream
 * classes.
 * <p>
 * TODO: The binary methods could be considered for change to a subclass of
 * DataInputStream and DataOutputStream.
 * 
 * @author gregor
 */
public class ArrayIo {

    /**
     * Loads an integer matrix from a binary file, optionally a zip file. The
     * method actually reads a float matrix.
     * 
     * @param filename
     * @return
     */
    public static int[][] loadBinaryIntMatrix(String filename) {
        int m, n;
        int[][] a = null;
        int i = 0, j = 0;
        try {

            DataInputStream dis = openInputStream(filename);
            m = dis.readInt();
            n = dis.readInt();
            a = new int[m][n];
            for (i = 0; i < m; i++) {
                for (j = 0; j < n; j++) {
                    a[i][j] = dis.readInt();
                }
            }
            closeInputStream(dis);

        } catch (IOException e) {
            System.err.println(i + " " + j);
            e.printStackTrace();
        }
        return a;
    }

    /**
     * Loads a matrix from a binary file, optionally a zip file. The method
     * actually reads a float matrix.
     * 
     * @param filename
     * @return
     */
    public static double[][] loadBinaryMatrix(String filename) {
        int m, n;
        double[][] a = null;
        int i = 0, j = 0;
        try {

            DataInputStream dis = openInputStream(filename);
            m = dis.readInt();
            n = dis.readInt();
            a = new double[m][n];
            for (i = 0; i < m; i++) {
                for (j = 0; j < n; j++) {
                    a[i][j] = dis.readFloat();
                }
            }
            closeInputStream(dis);

        } catch (IOException e) {
            System.err.println(i + " " + j);
            e.printStackTrace();
        }
        return a;
    }

    // compatibility matrix r/w

    /**
     * Writes integer matrix to binary file. If the file name ends with zip, the
     * output is zipped. Note: The method actually saves float values.
     * 
     * @param filename
     * @param a
     */
    public static void saveBinaryIntMatrix(String filename, int[][] a) {
        int i = 0, j = 0;

        try {
            DataOutputStream dos = openOutputStream(filename);
            dos.writeInt(a.length);
            dos.writeInt(a[0].length);
            for (i = 0; i < a.length; i++) {
                for (j = 0; j < a[0].length; j++) {
                    dos.writeInt(a[i][j]);
                }
            }
            closeOutputStream(dos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(i + " " + j);
            e.printStackTrace();
        }
    }

    /**
     * Writes matrix to binary file. If the file name ends with zip, the output
     * is zipped. Note: The method actually saves float values.
     * 
     * @param filename
     * @param a
     */
    public static void saveBinaryMatrix(String filename, double[][] a) {
        int i = 0, j = 0;

        try {
            DataOutputStream dos = openOutputStream(filename);
            dos.writeInt(a.length);
            dos.writeInt(a[0].length);
            for (i = 0; i < a.length; i++) {
                for (j = 0; j < a[0].length; j++) {
                    dos.writeFloat((float) a[i][j]);
                }
            }
            closeOutputStream(dos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(i + " " + j);
            e.printStackTrace();
        }
    }

    // read methods

    /**
     * generic method that calls the methods according to the type of the
     * argument Object
     * 
     * @param is
     * @param type
     * @return
     * @throws IOException
     */
    public static Object read(DataInputStream is, Object type)
        throws IOException {
        if (type instanceof double[][][]) {
            return readDoubleTensor(is);
        } else if (type instanceof double[][]) {
            return readDoubleMatrix(is);
        } else if (type instanceof double[]) {
            return readDoubleVector(is);
        } else if (type instanceof float[][]) {
            return readFloatMatrix(is);
        } else if (type instanceof float[]) {
            return readFloatVector(is);
        } else if (type instanceof int[][]) {
            return readIntMatrix(is);
        } else if (type instanceof int[]) {
            return readIntVector(is);
        }
        return null;
    }

    /**
     * Read matrix from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static int[][] readIntMatrix(DataInputStream bw) throws IOException {
        int rows = bw.readInt();
        int[][] matrix = new int[rows][];
        for (int i = 0; i < rows; i++) {
            matrix[i] = readIntVector(bw);
        }
        return matrix;
    }

    /**
     * Read vector from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static int[] readIntVector(DataInputStream bw) throws IOException {
        int length = bw.readInt();
        int[] vector = new int[length];
        for (int i = 0; i < length; i++) {
            vector[i] = bw.readInt();
        }
        return vector;
    }

    public static double[][][] readDoubleTensor(DataInputStream bw)
        throws IOException {
        int slices = bw.readInt();
        double[][][] tensor = new double[slices][][];
        for (int i = 0; i < slices; i++) {
            tensor[i] = readDoubleMatrix(bw);
        }
        return tensor;
    }

    /**
     * Read matrix from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static double[][] readDoubleMatrix(DataInputStream bw)
        throws IOException {
        int rows = bw.readInt();
        double[][] matrix = new double[rows][];
        for (int i = 0; i < rows; i++) {
            matrix[i] = readDoubleVector(bw);
        }
        return matrix;
    }

    /**
     * Read vector from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static double[] readDoubleVector(DataInputStream bw)
        throws IOException {
        int length = bw.readInt();
        double[] vector = new double[length];
        for (int i = 0; i < length; i++) {
            vector[i] = bw.readDouble();
        }
        return vector;
    }

    /**
     * Read matrix from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static float[][] readFloatMatrix(DataInputStream bw)
        throws IOException {
        int rows = bw.readInt();
        float[][] matrix = new float[rows][];
        for (int i = 0; i < rows; i++) {
            matrix[i] = readFloatVector(bw);
        }
        return matrix;
    }

    /**
     * Read vector from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static float[] readFloatVector(DataInputStream bw)
        throws IOException {
        int length = bw.readInt();
        float[] vector = new float[length];
        for (int i = 0; i < length; i++) {
            vector[i] = bw.readFloat();
        }
        return vector;
    }

    // write methods

    /**
     * generic write method that calls the respective method of the type of data
     * 
     * @param writer
     * @param data object
     */
    public static void write(DataOutputStream os, Object data)
        throws IOException {
        if (data instanceof double[][][]) {
            writeDoubleTensor(os, (double[][][]) data);
        } else if (data instanceof double[][]) {
            writeDoubleMatrix(os, (double[][]) data);
        } else if (data instanceof double[]) {
            writeDoubleVector(os, (double[]) data);
        } else if (data instanceof float[][]) {
            writeFloatMatrix(os, (float[][]) data);
        } else if (data instanceof float[]) {
            writeFloatVector(os, (float[]) data);
        } else if (data instanceof int[][]) {
            writeIntMatrix(os, (int[][]) data);
        } else if (data instanceof int[]) {
            writeIntVector(os, (int[]) data);
        }
    }

    /**
     * Writes an integer matrix in the format
     * rows,cols1,a11,a12,a1...,cols2,a21,... This way, matrices can be stored
     * that have variable row lengths.
     * 
     * @param bw
     * @param matrix
     * @throws IOException
     */
    public static void writeIntMatrix(DataOutputStream bw, int[][] matrix)
        throws IOException {
        bw.writeInt(matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            writeIntVector(bw, matrix[i]);
        }
    }

    /**
     * Writes an integer vector in the format size,v1,v2,...
     * 
     * @param bw
     * @param vector
     * @throws IOException
     */
    public static void writeIntVector(DataOutputStream bw, int[] vector)
        throws IOException {
        bw.writeInt(vector.length);
        for (int i = 0; i < vector.length; i++) {
            bw.writeInt(vector[i]);
        }
    }

    /**
     * Writes a double tensor (here = 3d matrix).
     * 
     * @param bw
     * @param tensor
     * @throws IOException
     */
    public static void writeDoubleTensor(DataOutputStream bw,
        double[][][] tensor) throws IOException {
        bw.writeInt(tensor.length);
        for (int r = 0; r < tensor.length; r++) {
            writeDoubleMatrix(bw, tensor[r]);
        }
    }

    /**
     * Writes a double matrix in the format
     * rows,cols1,a11,a12,a1...,cols2,a21,...
     * 
     * @param bw
     * @param matrix
     * @throws IOException
     */
    public static void writeDoubleMatrix(DataOutputStream bw, double[][] matrix)
        throws IOException {
        bw.writeInt(matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            writeDoubleVector(bw, matrix[i]);
        }
    }

    /**
     * Writes a double vector in the format size,v1,v2,...
     * 
     * @param bw
     * @param vector
     * @throws IOException
     */
    public static void writeDoubleVector(DataOutputStream bw, double[] vector)
        throws IOException {
        bw.writeInt(vector.length);
        for (int i = 0; i < vector.length; i++) {
            bw.writeDouble(vector[i]);
        }
    }

    /**
     * Writes a float matrix in the format rows,cols,a11,a12,a1...,a21,...
     * 
     * @param bw
     * @param matrix
     * @throws IOException
     */
    public static void writeFloatMatrix(DataOutputStream bw, float[][] matrix)
        throws IOException {
        bw.writeInt(matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            writeFloatVector(bw, matrix[i]);
        }
    }

    /**
     * Writes a float vector in the format size,v1,v2,...
     * 
     * @param bw
     * @param vector
     * @throws IOException
     */
    public static void writeFloatVector(DataOutputStream bw, float[] vector)
        throws IOException {
        bw.writeInt(vector.length);
        for (int i = 0; i < vector.length; i++) {
            bw.writeFloat(vector[i]);
        }
    }

    // ascii methods

    public static String padSpace(String s, int length) {
        if (s == null)
            s = "[null]";
        StringBuffer b = new StringBuffer(s);
        for (int i = 0; i < length - s.length(); i++) {
            b.append(' ');
        }
        return b.substring(0, length);
    }

    static NumberFormat nf = new DecimalFormat("0.00000");

    /**
     * @param d
     * @return
     */
    public static String formatDouble(double d) {
        String x = nf.format(d);
        // String x = shadeDouble(d, 1);
        return x;

    }

    public static double[][] loadAscii(String filename) {
        Vector<double[]> a = new Vector<double[]>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.trim().split(" ");
                double[] row = new double[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    row[i] = Double.parseDouble(fields[i]);
                }
                a.add(row);
            }
            br.close();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return a.toArray(new double[0][0]);
    }

    /**
     * @param filename
     * @param a
     */
    public static void saveAscii(String filename, double[][] a) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            int row, col;
            for (row = 0; row < a.length; row++) {
                for (col = 0; col < a[0].length; col++) {
                    if (col > 0)
                        bw.write(' ');
                    bw.write(formatDouble(a[row][col]));
                }
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param filename
     * @param a matrix (with equal columns for each row)
     */
    public static void saveTransposedAscii(String filename, double[][] a) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            int col, row;
            for (col = 0; col < a[0].length; col++) {
                for (row = 0; row < a.length; row++) {
                    if (row > 0)
                        bw.write(' ');
                    bw.write(formatDouble(a[row][col]));
                }
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens a data output stream with optional zip compression. The returned
     * DataOutputStream can be written to and must be closed using
     * closeStream(DataOutputStream dos) or dos.close().
     * 
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static DataOutputStream openOutputStream(String filename)
        throws FileNotFoundException, IOException {
        DataOutputStream dos = null;
        if (filename.endsWith(".zip")) {
            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(
                filename));
            String name = new File(filename).getName();
            zip.putNextEntry(new ZipEntry(name.substring(0, name.length() - 3)
                + "bin"));
            dos = new DataOutputStream(new BufferedOutputStream(zip));
        } else {
            dos = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(filename)));
        }
        return dos;
    }

    /**
     * Close the data output, which results in flushing the write buffer and
     * closing the file.
     * 
     * @param dos
     * @throws IOException
     */
    public static void closeOutputStream(DataOutputStream dos)
        throws IOException {
        dos.close();
    }

    /**
     * Opens a data input stream with optional zip compression. The returned
     * DataInputStream can be read from and must be closed using
     * closeStream(DataOutputStream dos) or dos.close().
     * 
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static DataInputStream openInputStream(String filename)
        throws IOException, FileNotFoundException {
        DataInputStream dis = null;

        if (filename.endsWith(".zip")) {

            ZipFile f = new ZipFile(filename);
            String name = new File(filename).getName();
            dis = new DataInputStream(new BufferedInputStream(f
                .getInputStream(f.getEntry(name.substring(0, name.length() - 3)
                    + "bin"))));
        } else {
            dis = new DataInputStream(new BufferedInputStream(
                new FileInputStream(filename)));
        }
        return dis;
    }

    /**
     * Close the input stream
     * 
     * @param dis
     * @throws IOException
     */
    public static void closeInputStream(DataInputStream dis) throws IOException {
        dis.close();
    }

    /**
     * Save a matrix as shaded values using MayaShades. The values are
     * normalised on the maximum.
     * 
     * @param filename
     * @param a
     */
    public static void saveShades(String filename, double[][] a) {
        double[] maxs = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            maxs[i] = Vectors.max(a[i]);
        }
        double maxx = Vectors.max(maxs);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            int row;
            bw.write("# [:::::] = " + maxx + ", [.    ] = " + maxx / 10.);
            bw.newLine();
            for (row = 0; row < a.length; row++) {
                for (int col = 0; col < a[row].length; col++) {
                    if (col > 0) {
                        bw.write(' ');
                    }
                    bw.write(MayaShades.shadeDouble(a[row][col], maxx));
                }
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save a transposed matrix as shaded values using MayaShades. The values
     * are normalised on the maximum.
     * 
     * @param filename
     * @param a matrix with equal cols in each row
     */
    public static void saveTransposedShades(String filename, double[][] a) {
        double[] maxs = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            maxs[i] = Vectors.max(a[i]);
        }
        double maxx = Vectors.max(maxs);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            int col;
            bw.write("# [:::::] = " + maxx + ", [.    ] = " + maxx / 10.);
            bw.newLine();
            for (col = 0; col < a[0].length; col++) {
                for (int row = 0; row < a.length; row++) {
                    if (row > 0) {
                        bw.write(' ');
                    }
                    bw.write(MayaShades.shadeDouble(a[row][col], maxx));
                }
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
