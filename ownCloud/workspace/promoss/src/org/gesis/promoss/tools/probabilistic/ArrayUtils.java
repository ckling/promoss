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
package org.gesis.promoss.tools.probabilistic;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ArrayUtils provides functionality for conversion between primitive and object
 * arrays and lists as well as a copying method for arrays of arbitrary type and
 * dimensions. For array arguments, reflection and Object arguments are chosen
 * as an alternative to overloading. Hopefully, this class will be obsolete in
 * Java 1.6...
 * 
 * @author gregor heinrich
 * 
 * 
 * @author christoph carl kling
 * 
 * I added some functions for re-ordering array elements
 */
public class ArrayUtils {

	public static void main(String[] args) {
		Integer[] a = new Integer[] { 1, 2, 3 };
		Object b = convert(a);
		System.out.println(Vectors.print((int[]) b) + " "
				+ b.getClass().getComponentType());
		Object[] c = convert(b);
		System.out.println(Vectors.print(c) + " "
				+ c.getClass().getComponentType());
		int[] d = new int[0];
		Object[] e = convert(d);
		System.out.println(Vectors.print(e) + " "
				+ e.getClass().getComponentType());
		Object f = convert(e);
		System.out.println(Vectors.print((int[]) f) + " "
				+ f.getClass().getComponentType());
		List<?> h = asList(b);
		System.out.println(h);
		int[] i = (int[]) asPrimitiveArray(h);
		System.out.println(Vectors.print(i));
		int[] j = (int[]) copy(i);
		i[2] = 45;
		System.out.println(Vectors.print(i));
		System.out.println(Vectors.print(j));
		int[][] k = new int[][] { i, j };
		int[][] l = (int[][]) copy(k);
		k[1][1] = 123;
		System.out.println(Vectors.print(k));
		System.out.println(Vectors.print(l));
		double[][][] m = new double[0][1][];
		double[][][] n = (double[][][]) copy(m);
		System.out.println(n.getClass().getComponentType().getComponentType()
				.getComponentType());
		double[][][] o = new double[][][] { { { 2.3, 2.4 } } };
		double[][][] p = (double[][][]) copy(o);
		o[0][0][1] *= 5;
		System.out.println(Vectors.print(o[0]));
		System.out.println(Vectors.print(p[0]));

		double[][][] q = new double[1][1][2];
		copy(q, o);
		o[0][0][1] *= 5;
		System.out.println(Vectors.print(o[0]));
		System.out.println(Vectors.print(q[0]));

	}

	@SuppressWarnings("rawtypes")
	public static final Class[][] types = {
			{ Boolean.class, Byte.class, Double.class, Float.class,
					Integer.class, Long.class, Short.class },
			{ Boolean.TYPE, Byte.TYPE, Double.TYPE, Float.TYPE, Integer.TYPE,
					Long.TYPE, Short.TYPE } };

	/**
	 * Get an array of the number type corresponding to the element type of the
	 * argument. The element type is determined from the runtime type of the
	 * first element, which, even if the array was initialised as Object[], can
	 * be a subclass of Object wrapping a primitive.
	 * <p>
	 * The contract is that the element runtime type is equal for all elements
	 * in the array and no null element is included, otherwise runtime
	 * exceptions will be thrown. If the array has size 0, the class type is
	 * determined by the component type, resulting in null return value if the
	 * array was not initialised as an Wrapper[] where wrapper is one of the
	 * primitive wrapper object types excluding Void.
	 * 
	 * @param objects
	 *            object array with elements of primitive wrapper classes
	 *            excluding Void.
	 * @return an array of the relevant primitive type or null if size = 0 and
	 *         or invalid type.
	 */
	public static Object convert(Object[] objects) {
		Class<? extends Object> c = null;
		if (objects.length == 0) {
			c = objects.getClass().getComponentType();
		} else {
			c = objects[0].getClass();
		}
		// no standard method found to get primitive types from their wrappers
		for (int i = 0; i < types[0].length; i++) {
			if (c.equals(types[0][i])) {
				Object array = Array.newInstance(types[1][i], objects.length);
				for (int j = 0; j < objects.length; j++) {
					Array.set(array, j, objects[j]);
				}
				return array;
			}
		}
		return null;
	}

	/**
	 * Get an array of the wrapper type corresponding to the primitive element
	 * type of the argument.
	 * 
	 * @param array
	 *            array of a primitive type
	 * @return array of the object type corresponding to the primitive type or
	 *         null if invalid element type or no array in argument.
	 */
	@SuppressWarnings("unchecked")
	public static Object[] convert(Object array) {
		Class<? extends Object> c = array.getClass();
		if (c.isArray() && c.getComponentType().isPrimitive()) {
			int len = Array.getLength(array);
			// handle zero-length arrays
			if (len == 0) {
				// automatic wrapping to object type in native Array.get()
				c = array.getClass().getComponentType();
				for (int i = 0; i < types[1].length; i++) {
					if (c.equals(types[1][i])) {
						c = types[0][i];
						return (Object[]) Array.newInstance(c, 0);
					}
				}
			}
			// automatic wrapping to object type in native Array.get()
			c = Array.get(array, 0).getClass();
			Object[] objects = (Object[]) Array.newInstance(c, len);
			for (int i = 0; i < len; i++) {
				objects[i] = Array.get(array, i);
			}
			return objects;
		}
		return null;
	}

	/**
	 * Convert an array of primitive-type elements into a list of its
	 * wrapper-type elements.
	 * 
	 * @param array
	 * @return the array or null if invalid
	 */
	public static List<?> asList(Object array) {
		Object[] a = convert(array);
		if (a == null) {
			return null;
		}
		return Arrays.asList(a);
	}

	/**
	 * Convert an list of objects into an array of primitive types. This extends
	 * the functionality of the List.toArray() method to primitive types. If the
	 * list does not consist of wrapper-type elements or if it has zero length,
	 * null is returned (in the second case because the element type cannot be
	 * determined).
	 * 
	 * @param objects
	 * @return array of primitive types or null if invalid.
	 */
	public static Object asPrimitiveArray(List<? extends Object> objects) {
		Class<? extends Object> c = null;
		if (objects.size() == 0) {
			return null;
		} else {
			c = objects.get(0).getClass();
		}
		// no standard method found to get primitive types from their wrappers
		for (int i = 0; i < types[0].length; i++) {
			if (c.equals(types[0][i])) {
				Object array = Array.newInstance(types[1][i], objects.size());
				for (int j = 0; j < objects.size(); j++) {
					Array.set(array, j, objects.get(j));
				}
				return array;
			}
		}
		return null;
	}

	/**
	 * Convert an list of objects into an array of primitive types. This extends
	 * the functionality of the List.toArray() method to primitive types. If the
	 * list does not consist of wrapper-type elements or if it has zero length,
	 * null is returned (in the second case because the element type cannot be
	 * determined).
	 * 
	 * @param objects
	 * @param sample
	 *            object (instantiate primitive array[0] if objects.size = 0)
	 * @return array of primitive types or null if invalid.
	 */
	public static Object asPrimitiveArray(List<? extends Object> objects,
			Class<?> type) {
		if (objects.size() == 0) {
			Object x = Array.newInstance(type, 0);
			return x;
		}
		return asPrimitiveArray(objects);
	}

	/**
	 * Create a copy of the argument array. There are almost no restrictions on
	 * the type of array to be copied: The array can be of object or primitive
	 * element type and can have any number of dimensions (a[], a[][], a[][][]
	 * etc.). For primitive element types, this is a deep copy.
	 * 
	 * @param array
	 * @return the copied array or null if the argument was not an array type.
	 */
	public static Object copy(Object array) {
		Class<? extends Object> c = array.getClass();
		if (c.isArray()) {
			int len = Array.getLength(array);
			c = array.getClass().getComponentType();
			// handle zero-length arrays
			if (len == 0) {
				return Array.newInstance(c, 0);
			}
			Object newArray = Array.newInstance(c, len);
			// check if this is a nested array
			c = Array.get(array, 0).getClass().getComponentType();
			if (c != null) {
				for (int i = 0; i < len; i++) {
					Array.set(newArray, i, copy(Array.get(array, i)));
				}
			} else {
				for (int i = 0; i < len; i++) {
					Array.set(newArray, i, Array.get(array, i));
				}
			}
			return newArray;
		}
		return null;
	}

	/**
	 * Create a copy of the argument array in the pointer provided, which is
	 * assumed to be of the same dimensions than array. There are almost no
	 * restrictions on the type of array to be copied: The arrays can be of
	 * object or primitive element type and can have any number of dimensions
	 * (a[], a[][], a[][][] etc.). For primitive element types, this is a deep
	 * copy.
	 * <p>
	 * Note: This method is rather slow. Use System.arraycopy for scalability.
	 * 
	 * @param array
	 *            whose content is copied
	 * @param target
	 *            array whose content is overwritten (same size as array).
	 */
	public static void copy(Object target, Object array) {
		Class<? extends Object> c = array.getClass();
		if (c.isArray()) {
			int len = Array.getLength(array);
			c = array.getClass().getComponentType();
			// handle zero-length arrays
			if (len == 0) {
				return;
			}
			// check if this is a nested array
			c = Array.get(array, 0).getClass().getComponentType();
			if (c != null) {
				for (int i = 0; i < len; i++) {
					copy(Array.get(target, i), Array.get(array, i));
				}
			} else {
				for (int i = 0; i < len; i++) {
					Array.set(target, i, Array.get(array, i));
				}
			}
		}
	}

	/**
	 * determines whether an object is an array and has nonzero length
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isArray(Object s) {
		try {
			// native access to first element
			Array.get(s, 0);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
	
	


	public static String[] getDistinct(String[] input) {

		Set<String> distinct = new HashSet<String>();
		for(String element : input) {
			distinct.add(element);
		}

		return distinct.toArray(new String[0]);
	}
	public static int[] getDistinct(int[] input) {

		Set<Integer> distinct = new HashSet<Integer>();
		for(int element : input) {
			distinct.add(element);
		}

		int[] ret = new int [distinct.size()];

		int i=0;
		Iterator<Integer> iterator = distinct.iterator();
		while (iterator.hasNext()) {
			ret[i++]=iterator.next();
		}

		return ret;
	}

	public static int[] sortArray(double[] array) {


		int[] index = new int[array.length];
		ArrayElement[] arrayElement = new ArrayElement[array.length];
		for (int i = 0; i < array.length;i++) {
			index[i]=i;
			arrayElement[i] = new ArrayElement(i,array[i]);			  
		}
		Arrays.sort(arrayElement);
		for (int i = 0; i < array.length;i++) {
			index[i]=arrayElement[i].getIndex();
			array[i]=arrayElement[i].getValue();
		}

		return(index);

	}

	/**
	 * @param array Array to be sorted
	 * @param order asc or desc, default is asc
	 * @return Array of indices
	 */
	public static int[] sortArray(double[] array, String order) {

		int[] index = new int[array.length];
		ArrayElement[] arrayElement = new ArrayElement[array.length];
		for (int i = 0; i < array.length;i++) {
			index[i]=i;
			arrayElement[i] = new ArrayElement(i,array[i]);			  
		}
		Arrays.sort(arrayElement);

		if (!order.equals("desc")) {
			for (int i = 0; i < array.length;i++) {
				index[i]=arrayElement[i].getIndex();
				array[i]=arrayElement[i].getValue();
			}
		}
		else {
			for (int i = 0; i < array.length;i++) {
				index[array.length-i-1]=arrayElement[i].getIndex();
				array[array.length-i-1]=arrayElement[i].getValue();
			}
		}

		return(index);

	}

	public static int[] sortArray(int[] array) {


		int[] index = new int[array.length];
		ArrayElementInt[] arrayElementInt = new ArrayElementInt[array.length];
		for (int i = 0; i < array.length;i++) {
			index[i]=i;
			arrayElementInt[i] = new ArrayElementInt(i,array[i]);			  
		}
		Arrays.sort(arrayElementInt);
		for (int i = 0; i < array.length;i++) {
			index[i]=arrayElementInt[i].getIndex();
			array[i]=arrayElementInt[i].getValue();
		}

		return(index);

	}

	/**
	 * @param array Array to be sorted
	 * @param index Indices after which to sort
	 * @return original positions of the elements in the array, starting with 0
	 */
	public static int[] sortArray(int[] array, int[] index) {

		int length = array.length;
		int[] array_new = new int[length];
		for (int i=0; i<length; i++) {
			array_new[i] = array[index[i]];
		}

		return array_new;
	}

	/**
	 * @param array Array to be sorted
	 * @param index Indices after which to sort
	 * @return original positions of the elements in the array, starting with 0
	 */
	public static double[] sortArray(double[] array, int[] index) {

		int length = array.length;
		double[] array_new = new double[length];
		for (int i=0; i<length; i++) {
			array_new[i] = array[index[i]];
		}

		return array_new;
	}

	public static int[] reverseIndex(int[] index) {

		int length = index.length;
		int[] index_new = new int[length];
		for (int i=0; i<length; i++) {
			index_new[index[i]] = i;
		}

		return index_new;
	}


	public int[] concat(int[] a, int[] b) {
		int aLen = a.length;
		int bLen = b.length;
		int[] c= new int[aLen+bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

	public double[] concat(double[] a, double[] b) {
		int aLen = a.length;
		int bLen = b.length;
		double[] c= new double[aLen+bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}
	
	public double[] serialise(double[][] a) {
		
		int total_length = 0;
		for (int i=0;i<a.length;i++) {
			total_length += a[i].length;
		}
		double[] ret = new double[total_length];
		int offset = 0;
		for (int i=0;i<a.length;i++) {
			for (int j=0;i<a.length;i++) {
				ret[offset + j] = a[i][j];
				offset += a[i].length;
			}
		}
		return ret;
		
	}
	
}


class ArrayElement implements Comparable<Object> {

	private int index;
	private double value;

	ArrayElement(int index, double value) {
		this.index = index;
		this.value = value;
	}

	public double getValue() {
		return value;
	}
	public int getIndex() {
		return index;
	}

	public int compareTo(Object other) {
		if (this.getValue() == ((ArrayElement) other).getValue()) {
			return 0;
		} else if (this.getValue() > ((ArrayElement) other).getValue()) {
			return 1;
		} else {
			return -1;
		}
	}

}

class ArrayElementInt implements Comparable<Object> {
	private int index;
	private int value;

	ArrayElementInt(int index, int value) {
		this.index = index;
		this.value = value;
	}

	public int getValue() {
		return value;
	}
	public int getIndex() {
		return index;
	}

	public int compareTo(Object other) {
		if (this.getValue() == ((ArrayElementInt) other).getValue()) {
			return 0;
		} else if (this.getValue() > ((ArrayElementInt) other).getValue()) {
			return 1;
		} else {
			return -1;
		}
	}

}