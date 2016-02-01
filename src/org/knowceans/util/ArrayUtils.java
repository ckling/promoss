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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * ArrayUtils provides functionality for conversion between primitive and object
 * arrays and lists as well as a copying method for arrays of arbitrary type and
 * dimensions. For array arguments, reflection and Object arguments are chosen
 * as an alternative to overloading. Hopefully, this class will be obsolete in
 * Java 1.6...
 * 
 * @author gregor heinrich
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
}
