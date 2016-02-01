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

/*
 * Created on Dec 3, 2004
 */
package org.knowceans.corpus;

import java.util.HashMap;
import java.util.Map.Entry;

import org.knowceans.util.Vectors;

/**
 * wrapper for a document in LDA
 * <p>
 * lda-c reference: struct document in lda.h. Here the distinction between term
 * and word is used, changing API nomenclature. TODO automatic length tracking
 * 
 * @author heinrich
 */
public class Document {

	// lda-c: "words"
	protected int[] terms;

	protected int[] counts;

	// lda-c: "length"
	protected int numTerms;

	// lda-c: "total"
	protected int numWords;

	/**
	 * paragraph bounds starting with the length of the first paragraph, ending
	 * with the total term count. if null, no paragraphs in document
	 */
	int[] parBounds;

	/**
     * 
     */
	public Document() {
		numTerms = 0;
		terms = new int[0];
		counts = new int[0];
	}

	/**
     * 
     */
	public Document(int length) {
		terms = new int[length];
		counts = new int[length];
		this.numTerms = length;
	}

	/**
	 * copy constructor
	 * 
	 * @param document
	 */
	public Document(Document d) {
		terms = Vectors.copy(d.terms);
		counts = Vectors.copy(d.counts);
		parBounds = Vectors.copy(d.parBounds);
		numTerms = d.numTerms;
		numWords = d.numWords;
	}

	public void compile() {
		try {
			if (counts.length != terms.length)
				throw new Exception("Document inconsistent.");
			numTerms = counts.length;
			for (int c : counts)
				numWords += c;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	public int[] getCounts() {
		return counts;
	}

	/**
	 * @param index
	 * @return
	 */
	public int getCount(int index) {
		return counts[index];
	}

	/**
	 * @param count
	 * @param index
	 */
	public void setCount(int index, int count) {
		counts[index] = count;
	}

	/**
	 * @return
	 */
	public int getNumTerms() {
		return numTerms;
	}

	/**
	 * @return
	 */
	public int getNumWords() {
		return numWords;
	}

	/**
	 * @return
	 */
	public int[] getTerms() {
		return terms;
	}

	/**
	 * @param index
	 * @return
	 */
	public int getTerm(int index) {
		return terms[index];
	}

	/**
	 * @param term
	 * @param index
	 */
	public void setTerm(int index, int term) {
		terms[index] = term;
	}

	/**
	 * @param is
	 */
	public void setCounts(int[] is) {
		counts = is;
	}

	/**
	 * @param i
	 */
	public void setNumTerms(int i) {
		numTerms = i;
	}

	/**
	 * @param i
	 */
	public void setNumWords(int i) {
		numWords = i;
	}

	/**
	 * @param is
	 */
	public void setTerms(int[] is) {
		terms = is;
	}

	public void setWords(int[] is) {
		if (terms == null) {
			terms = new int[numTerms];
			counts = new int[numTerms];
		}
		// merging is easiest in a hash map
		HashMap<Integer, Integer> termfreqs = new HashMap<Integer, Integer>();
		for (int n = 0; n < numTerms; n++) {
			Integer a = termfreqs.get(terms[n]);
			if (a == 0) {
				termfreqs.put(terms[n], counts[n]);
			} else {
				termfreqs.put(terms[n], counts[n] + a);
			}
		}
		for (int n = 0; n < is.length; n++) {
			Integer count = termfreqs.get(is[n]);
			if (count == null) {
				termfreqs.put(is[n], 1);
			} else {
				termfreqs.put(is[n], count + 1);
			}
		}
		// create merged document
		terms = new int[termfreqs.size()];
		counts = new int[termfreqs.size()];
		int n = 0;
		for (Entry<Integer, Integer> tf : termfreqs.entrySet()) {
			terms[n] = tf.getKey();
			counts[n] = tf.getValue();
			n++;
		}
		numTerms = terms.length;
		numWords = Vectors.sum(counts);
	}

	/**
	 * add all terms to the end of this document, filling the parBounds field.
	 * Paragraphs should be added only this way; if parBounds == null, the
	 * document is emptied before adding new content. Vectors are not copied.
	 * 
	 * @param d
	 */
	public void addDocument(Document d) {
		if (parBounds == null) {
			parBounds = new int[1];
			parBounds[0] = d.numTerms;
			terms = d.terms;
			counts = d.counts;
			numTerms = d.numTerms;
			numWords = d.numWords;
		} else {
			parBounds = Vectors.concat(parBounds, new int[] { numTerms
					+ d.numTerms });
			terms = Vectors.concat(terms, d.terms);
			counts = Vectors.concat(counts, d.counts);
			numTerms += d.numTerms;
			numWords += d.numWords;
		}
	}

	/**
	 * merge all terms and add the document or null.
	 * 
	 * @param d
	 */
	public void mergeDocument(Document d) {
		// merging is easiest in a hash map
		HashMap<Integer, Integer> termfreqs = new HashMap<Integer, Integer>();
		for (int n = 0; n < numTerms; n++) {
			Integer a = termfreqs.get(terms[n]);
			if (a == 0) {
				termfreqs.put(terms[n], counts[n]);
			} else {
				termfreqs.put(terms[n], counts[n] + a);
			}
		}
		if (d != null) {
			for (int n = 0; n < numTerms; n++) {
				Integer a = termfreqs.get(d.terms[n]);
				if (a == 0) {
					termfreqs.put(d.terms[n], d.counts[n]);
				} else {
					termfreqs.put(d.terms[n], d.counts[n] + a);
				}
			}
		}
		// create merged document
		terms = new int[termfreqs.size()];
		counts = new int[termfreqs.size()];
		int n = 0;
		for (Entry<Integer, Integer> tf : termfreqs.entrySet()) {
			terms[n] = tf.getKey();
			counts[n] = tf.getValue();
			n++;
		}
		numTerms = terms.length;
		numWords = Vectors.sum(counts);
	}

	public int[] getParBounds() {
		return parBounds;
	}

	public void setParBounds(int[] parBounds) {
		this.parBounds = parBounds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("Document {numTerms=" + numTerms + " numWords=" + numWords);
		if (parBounds != null) {
			b.append(" numPars=" + parBounds.length + "}");
		} else {
			b.append("}");
		}
		return b.toString();
	}

}
