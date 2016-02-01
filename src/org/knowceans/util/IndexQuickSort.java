/*
 * Created on Sep 20, 2009
 */
package org.knowceans.util;

import java.util.Comparator;
import java.util.List;

/**
 * IndexQuickSort sorts indices of an array without changing its values. There
 * are additional functions to reverse the order and to get the inverse of the
 * mapping from the array to the sorted index. Types supported include primitive
 * double and int arrays as well as objects and associated Comparators.
 * 
 * @author gregor
 * @author used quicksort algorithm at
 *         http://www.cs.princeton.edu/introcs/42sort/QuickSort.java.html
 */
public class IndexQuickSort {

    public static void main(String[] args) {
        double[] weights = new double[] { 0.1, 0.1, 0.001, 0.05, 0.2, 0.3, 0.5,
                0.03, 0.02, 0.1 };
        Double[] weights2 = new Double[] { 0.1, 0.1, 0.001, 0.05, 0.2, 0.3,
                0.5, 0.03, 0.02, 0.1 };
        Comparator<Double> cmp = new Comparator<Double>() {
            // @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(o1, o2);
            }
        };

        for (int i = 0; i < weights.length; i++) {
            System.out.println(i + "\t" + weights[i]);
        }
        System.out.println("sorted");
        int[] index = sort(weights);
        for (int i = 0; i < index.length; i++) {
            System.out.println(i + "\t" + weights[index[i]] + "\t" + index[i]);
        }
        System.out.println("reversed");
        reverse(index);
        for (int i = 0; i < index.length; i++) {
            System.out.println(i + "\t" + weights[index[i]] + "\t" + index[i]);
        }
        System.out.println("inverse");
        int[] index2 = inverse(index);
        for (int i = 0; i < index2.length; i++) {
            System.out.println(i + "\t" + weights[index2[index[i]]] + "\t"
                    + index2[i]);
        }

        System.out.println("now with objects");
        for (int i = 0; i < weights.length; i++) {
            System.out.println(i + "\t" + weights[i]);
        }
        System.out.println("sorted");
        index = sort(weights2, cmp);
        for (int i = 0; i < index.length; i++) {
            System.out.println(i + "\t" + weights2[index[i]] + "\t" + index[i]);
        }
        System.out.println("reversed");
        reverse(index);
        for (int i = 0; i < index.length; i++) {
            System.out.println(i + "\t" + weights2[index[i]] + "\t" + index[i]);
        }

        int[] xx = new int[] { 100, 101, 102, 103, 104, 105, 106, 107, 108,
                109, 110, 111, 112 };
        int[] order = new int[] { 0, 11, 10, 9, 5, 7, 6, 3, 4, 8, 2, 1, 12 };
        reorder(xx, order);
        System.out.println(Vectors.print(xx));

    }

    /**
     * sort indices
     * 
     * @param fixedArray
     *            values to be sorted
     * @param index
     *            range of indices into fixedArray
     */
    public static void sort(double[] fixedArray, int[] index) {
        sort(fixedArray, index, 0, index.length - 1);
    }

    /**
     * sort indices
     * 
     * @param fixedArray
     *            values to be sorted
     * @return index range of indices into fixedArray
     */
    public static int[] sort(double[] fixedArray) {
        int[] index = Vectors.range(0, fixedArray.length - 1);
        sort(fixedArray, index, 0, index.length - 1);
        return index;
    }

    /**
     * inverse of the index, i.e., the ordered index of the argument
     * 
     * @param index
     */
    public static int[] inverse(int[] index) {
        return sort(index);
    }

    /**
     * inverse of the index, i.e., the ordered index of the argument
     * 
     * @param index
     * @param invindex
     *            the inverse mapping of index
     */
    public static void inverse(int[] index, int[] invindex) {
        sort(index, invindex);
    }

    /**
     * reverse ordering
     * 
     * @param main
     * @param index
     */
    public static void reverse(int[] index) {
        int N = index.length;
        for (int i = 0; i < (N / 2); i++) {
            swap(index, i, N - 1 - i);
        }
    }

    // /////////////////

    // quicksort a[left] to a[right]
    public static void sort(double[] a, int[] index, int left, int right) {
        if (right <= left)
            return;
        int i = part(a, index, left, right);
        sort(a, index, left, i - 1);
        sort(a, index, i + 1, right);
    }

    // partition a[left] to a[right], assumes left < right
    private static int part(double[] a, int[] index, int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (a[index[++i]] < a[index[right]])
                // find item on left to swap
                // a[right] acts as sentinel
                ;
            while (a[index[right]] < a[index[--j]])
                // find item on right to swap
                if (j == left)
                    // don't go out-of-bounds
                    break;
            if (i >= j)
                // check if pointers cross
                break;
            // swap two elements into place
            swap(index, i, j);
        }
        // swap with partition element
        swap(index, i, right);
        return i;
    }

    // swap indices i and j
    public static void swap(int[] index, int i, int j) {
        int b = index[i];
        index[i] = index[j];
        index[j] = b;
    }

    // swap indices i and j
    public static void swap(double[] index, int i, int j) {
        double b = index[i];
        index[i] = index[j];
        index[j] = b;
    }

    // swap indices i and j
    public static <T> void swap(T[] index, int i, int j) {
        T b = index[i];
        index[i] = index[j];
        index[j] = b;
    }

    // ////////////

    /**
     * sort indices
     * 
     * @param fixedArray
     *            values to be sorted
     * @param index
     *            range of indices into fixedArray
     */
    public static void sort(int[] fixedArray, int[] index) {
        sort(fixedArray, index, 0, index.length - 1);
    }

    /**
     * sort indices
     * 
     * @param fixedArray
     *            values to be sorted
     * @return index range of indices into fixedArray
     */
    public static int[] sort(int[] fixedArray) {
        int[] index = Vectors.range(0, fixedArray.length - 1);
        sort(fixedArray, index, 0, index.length - 1);
        return index;
    }

    /**
     * sort indices.
     * 
     * 
     * @param <T>
     *            a list of Comparables. Cast problem with <T extends
     *            Comparable<T>>, so the argument has Comparable elements by
     *            contract.
     * 
     * @param fixedList
     *            values to be sorted should be Comparable
     * @return index range of indices into fixedArray
     * 
     */
    @SuppressWarnings("unchecked")
    public static <T> int[] sort(List<T> fixedList) {
        T[] array = (T[]) fixedList.toArray();
        return sort(array, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return ((Comparable) o1).compareTo(o2);
            }
        });
    }

    // quicksort a[left] to a[right]
    public static void sort(int[] a, int[] index, int left, int right) {
        if (right <= left)
            return;
        int i = part(a, index, left, right);
        sort(a, index, left, i - 1);
        sort(a, index, i + 1, right);
    }

    // partition a[left] to a[right], assumes left < right
    private static int part(int[] a, int[] index, int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (a[index[++i]] < a[index[right]])
                // find item on left to swap
                // a[right] acts as sentinel
                ;
            while (a[index[right]] < a[index[--j]])
                // find item on right to swap
                if (j == left)
                    // don't go out-of-bounds
                    break;
            if (i >= j)
                // check if pointers cross
                break;
            // swap two elements into place
            swap(index, i, j);
        }
        // swap with partition element
        swap(index, i, right);
        return i;
    }

    // ////////////

    /**
     * sort indices
     * 
     * @param fixedArray
     *            values to be sorted
     * @param index
     *            range of indices into fixedArray
     */
    public static <T> void sort(T[] fixedArray, Comparator<T> cmp, int[] index) {
        sort(fixedArray, cmp, index, 0, index.length - 1);
    }

    /**
     * sort indices
     * 
     * @param fixedArray
     *            values to be sorted
     * @return index range of indices into fixedArray
     */
    public static <T> int[] sort(T[] fixedArray, Comparator<T> cmp) {
        int[] index = Vectors.range(0, fixedArray.length - 1);
        sort(fixedArray, cmp, index, 0, index.length - 1);
        return index;
    }

    public static <T> void sort(T[] a, Comparator<T> cmp, int[] index,
            int left, int right) {
        if (right <= left)
            return;
        int i = part(a, cmp, index, left, right);
        sort(a, cmp, index, left, i - 1);
        sort(a, cmp, index, i + 1, right);
    }

    private static <T> int part(T[] a, Comparator<T> cmp, int[] index,
            int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (cmp.compare(a[index[++i]], a[index[right]]) == -1)
                ;
            while (cmp.compare(a[index[right]], a[index[--j]]) == -1)
                if (j == left)
                    break;
            if (i >= j)
                break;
            swap(index, i, j);
        }
        swap(index, i, right);
        return i;
    }

    // ////////////

    /**
     * re-sort index of x after x[inc] has increased in value
     * 
     * @param x
     * @param sort2k
     * @param invidx
     * @param inc
     */
    public static void resortinc(int[] x, int[] idx, int[] invidx, int inc) {
        int tmp;
        inc = invidx[inc];
        while (inc > 0 && x[idx[inc]] > x[idx[inc - 1]]) {
            tmp = idx[inc];
            idx[inc] = idx[inc - 1];
            idx[inc - 1] = tmp;
            tmp = invidx[idx[inc]];
            invidx[idx[inc]] = invidx[idx[inc - 1]];
            invidx[idx[inc - 1]] = tmp;
            inc--;
        }
    }

    /**
     * re-sort index of x after x[dec] has reduced in value
     * 
     * @param x
     * @param sort2k
     * @param invidx
     * @param dec
     */
    public static void resortdec(int[] x, int[] idx, int[] invidx, int dec) {
        int tmp;
        dec = invidx[dec];
        while (dec < x.length - 1 && x[idx[dec]] < x[idx[dec + 1]]) {
            tmp = idx[dec];
            idx[dec] = idx[dec + 1];
            idx[dec + 1] = tmp;
            tmp = invidx[idx[dec]];
            invidx[idx[dec]] = invidx[idx[dec + 1]];
            invidx[idx[dec + 1]] = tmp;
            dec++;
        }
    }

    /**
     * reorder the array according to the sorting. This uses bubblesort to
     * reorder in-place.
     * 
     * @param ds
     * @param order
     */
    public static void reorder(double[] x, int[] order) {
        int[] inv = inverse(order);
        for (int j = x.length - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (inv[i] > inv[i + 1]) {
                    swap(inv, i, i + 1);
                    swap(x, i, i + 1);
                }
            }
        }
    }

//    /**
//     * reordering without a sort
//     * 
//     * @param <T>
//     * @param x
//     * @param order
//     */
//    public static void reorder(double[] x, int[] order) {
//        int i, j;
//        for (i = 1; i < order.length; ++i) {
//            j = order[i];
//            while (j < i) {
//                j = order[j];
//            }
//            if (j == i) {
//                while ((j = order[j]) != i) {
//                    swap(x, i, j);
//                }
//            }
//        }
//    }

    /**
     * reorder the array according to the sorting. This uses bubblesort to
     * reorder in-place.
     * 
     * @param x
     * @param order
     *            sorting index new, element old
     */
    public static void reorder(int[] x, int[] order) {
        int[] inv = inverse(order);
        for (int j = x.length - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (inv[i] > inv[i + 1]) {
                    swap(inv, i, i + 1);
                    swap(x, i, i + 1);
                }
            }
        }
    }

//    /**
//     * reordering without a sort.
//     * 
//     * @param <T>
//     * @param x
//     * @param order
//     */
//    public static void reorder(int[] x, int[] order) {
//        int i, j;
//        for (i = 1; i < order.length; ++i) {
//            j = order[i];
//            while (j < i) {
//                j = order[j];
//            }
//            if (j == i) {
//                while ((j = order[j]) != i) {
//                    swap(x, i, j);
//                }
//            }
//        }
//    }

    /**
     * reorder the array according to the sorting. This uses bubblesort to
     * reorder in-place.
     * 
     * @param x
     * @param order
     */
    public static <T> void reorder(T[] x, int[] order) {
        int[] inv = inverse(order);
        for (int j = x.length - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (inv[i] > inv[i + 1]) {
                    swap(inv, i, i + 1);
                    swap(x, i, i + 1);
                }
            }
        }
    }

//    /**
//     * reordering without a sort
//     * 
//     * @param <T>
//     * @param x
//     * @param order
//     */
//    public static <T> void reorder(T[] x, int[] order) {
//        int i, j;
//        for (i = 1; i < order.length; ++i) {
//            j = order[i];
//            while (j < i) {
//                j = order[j];
//            }
//            if (j == i) {
//                while ((j = order[j]) != i) {
//                    swap(x, i, j);
//                }
//            }
//        }
//    }

    /**
     * reordering for lists
     * 
     * @param <T>
     * @param x
     * @param order
     */
    public static <T> void reorder(List<T> x, int[] order) {
        int[] inv = inverse(order);
        for (int j = x.size() - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (inv[i] > inv[i + 1]) {
                    swap(inv, i, i + 1);
                    T temp = x.get(i);
                    x.set(i, x.get(i + 1));
                    x.set(i + 1, temp);
                }
            }
        }
    }

//    /**
//     * reordering without a sort
//     * 
//     * @param <T>
//     * @param x
//     * @param order
//     */
//    public static <T> void reorder(List<T> x, int[] order) {
//        int i, j;
//        for (i = 1; i < order.length; ++i) {
//            j = order[i];
//            while (j < i) {
//                j = order[j];
//            }
//            if (j == i) {
//                while ((j = order[j]) != i) {
//                    T temp = x.get(i);
//                    x.set(i, x.get(i + 1));
//                    x.set(i + 1, temp);
//                }
//            }
//        }
//    }

}
