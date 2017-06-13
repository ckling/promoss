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

import org.knowceans.util.RandomSamplers;

public class VisCorpus {

	/**
	 * this generates a corpus with M documents and Nm words each, using 2K
	 * topics and defining the topic distributions as rows and columns of a K^2
	 * term matrix. This re-enacts the synthesised data set in Griffiths and
	 * Steyvers (2004).
	 * 
	 * @param M
	 * @param Nm
	 * @return
	 */
	public static NumCorpus generateLdaCorpus(int K, int M, int Nm) {
		NumCorpus nc = new NumCorpus();
		nc.docs = new Document[M];
		int K2 = K >> 1;
		nc.numTerms = K2 * K2;
		nc.numDocs = M;
		nc.numWords = M * Nm;

		// LDA generative process with trivial parameters
		// create phi
		double[][] phi = new double[K][nc.numTerms];
		for (int k = 0; k < K2; k++) {
			for (int i = 0; i < K2; i++) {
				// rows
				phi[k][i + k * K2] = 1. / K2;
				// columns
				phi[k + K2][i * K2 + k] = 1. / K2;
			}
		}
		RandomSamplers samp = new RandomSamplers();
		for (int m = 0; m < M; m++) {
			double[] theta = samp.randDir(1., K);
			int[] doc = new int[Nm];
			for (int n = 0; n < Nm; n++) {
				int z = samp.randMult(theta);
				doc[n] = samp.randMult(phi[z]);
			}
			Document d = new Document();
			d.setWords(doc);
			d.compile();
			nc.docs[m] = d;
		}
		return nc;
	}
}
