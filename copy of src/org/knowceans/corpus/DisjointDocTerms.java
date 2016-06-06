/*
 * (C) Copyright 2005-2011, Gregor Heinrich (gregor :: arbylon : net) \
 * (This file is part of the knowceans-ilda experimental software package
 */
/*
 * knowceans-ilda is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) 
 * any later version.
 */
/*
 * knowceans-ilda is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.knowceans.corpus;

import java.util.ArrayList;
import java.util.Random;

import org.knowceans.util.ArrayUtils;
import org.knowceans.util.IndexQuickSort;
import org.knowceans.util.Vectors;

/**
 * DisjointDocTerms partitions the a given document-term matrix into P mutually
 * exclusive document subsets and term subsets. Based on these, index sets can
 * be retrieved into the original corpus for parallel access to mutually
 * exclusive documents and terms.
 * 
 * @author gregor
 */
public class DisjointDocTerms {

	public static void main(String[] args) {
		int P = 2;
		// ParNumCorpus nc = new ParNumCorpus("./nips/nips.corpus");
		NumCorpus nc = new NumCorpus("./berry95/berry95.corpus");
		int[][] w = nc.getDocWords(new Random());
		DisjointDocTerms dj = new DisjointDocTerms(w, nc.getNumTerms());
		int[][] mm = dj.getDisjointDocuments(P);
		System.out.println("disjoint document sets");
		for (int p = 0; p < P; p++) {
			System.out.println(Vectors.print(mm[p]));
		}
		System.out.println("disjoint term sets");
		int[][] tt = dj.getDisjointTerms(P);
		for (int p = 0; p < P; p++) {
			System.out.println(Vectors.print(tt[p]));
		}
		System.out.println("disjoint token sequences");
		int[][][] ww = dj.getDisjointTokens(P);
		for (int p = 0; p < P; p++) {
			System.out.println("corpus " + p);
			for (int m = 0; m < nc.getNumDocs(); m++) {
				System.out.println(Vectors.print(ww[p][m]));
			}
		}
	}

	/**
	 * original corpus
	 */
	private int[][] w;

	/**
	 * number of terms
	 */
	private int V;

	// //////////////////////

	public DisjointDocTerms(int[][] w, int V) {
		this.w = w;
		this.V = V;
	}

	/**
	 * get the set of disjoint m for each threadd.
	 * 
	 * @param P
	 * @return m = int[p][i]
	 */
	public int[][] getDisjointDocuments(int P) {
		int[][] pm = new int[P][];
		int[] dd = alignDocs(P);
		int M = w.length;
		int size = M / P;
		int plus = M % P;
		for (int p = 0; p < P; p++) {
			pm[p] = new int[size + (p < plus ? 1 : 0)];
		}
		int[] ii = new int[P];
		for (int m = 0; m < M; m++) {
			int p = dd[m];
			pm[p][ii[p]] = m;
			ii[p]++;
		}
		return pm;
	}

	/**
	 * get the set of disjoint terms
	 * 
	 * @param P
	 * @return int[q][i] -> t
	 */
	public int[][] getDisjointTerms(int P) {
		int[][] qt = new int[P][];
		int size = V / P;
		int plus = V % P;
		for (int q = 0; q < P; q++) {
			qt[q] = new int[size + (q < plus ? 1 : 0)];
			for (int i = 0; i < qt[q].length; i++) {
				qt[q][i] = q + i * P;
			}
		}
		return qt;
	}

	/**
	 * get the set of disjoint words for a corpus
	 * 
	 * @param w
	 *            int[m][n] -> t
	 * @return int[q][m][i] -> n
	 */
	@SuppressWarnings("unchecked")
	public int[][][] getDisjointTokens(int P) {
		int M = w.length;
		int[] pt = alignTerms(P);
		// processor-specific sampling sequences
		ArrayList[][] wpm = new ArrayList[P][M];

		for (int m = 0; m < M; m++) {
			for (int q = 0; q < P; q++) {
				wpm[q][m] = new ArrayList<Integer>();
			}
			for (int n = 0; n < w[m].length; n++) {
				wpm[pt[w[m][n]]][m].add(n);
			}
		}

		int[][][] pp = new int[P][M][];
		for (int m = 0; m < M; m++) {
			for (int q = 0; q < P; q++) {
				if (wpm[q][m].size() > 0) {
					pp[q][m] = (int[]) ArrayUtils.asPrimitiveArray(wpm[q][m]);
				} else {
					pp[q][m] = new int[0];
				}
			}
		}
		return pp;

	}

	/**
	 * partition corpus into sets that with mutually exclusive words and topics
	 * 
	 * @param P
	 */
	private int[] alignTerms(int P) {
		int[] pt = new int[V];
		// sorted term frequencies: use ids from NumCorpus
		for (int t = 0; t < pt.length; t++) {
			pt[t] = t % P;
		}
		return pt;
	}

	/**
	 * partition corpus into sets that with mutually exclusive words and topics
	 * 
	 * @param nc
	 *            corpus
	 * @param pt
	 *            term association int pt[V] -> [0, P)
	 * @param pm
	 *            document association int pm[M] -> [0, P)
	 * @param P
	 * @return
	 */
	private int[] alignDocs(int P) {
		int M = w.length;
		int[] pm = new int[M];
		// calculate document lengths
		int[] nm = new int[M];
		for (int m = 0; m < M; m++) {
			nm[m] = w[m].length;
		}
		// sort document lengths
		int[] idxm = IndexQuickSort.sort(nm);
		IndexQuickSort.reverse(idxm);
		// associate documents with p
		for (int m = 0; m < M; m++) {
			pm[idxm[m]] = m % P;
		}
		return pm;
	}
}
