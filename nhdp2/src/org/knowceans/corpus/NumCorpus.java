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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.knowceans.util.ArrayUtils;
import org.knowceans.util.RandomSamplers;
import org.knowceans.util.Vectors;

/**
 * Represents a corpus of documents, using numerical data only.
 * <p>
 * 
 * 
 * @author heinrich
 */
public class NumCorpus implements ICorpus, ITermCorpus, ISplitCorpus {

	protected Document[] docs;

	protected int numTerms;

	protected int numDocs;

	protected int numWords;

	protected boolean debug = false;

	/**
	 * permutation of the corpus used for splitting
	 */
	protected int[] splitperm;

	/**
	 * starting points of the corpus segments
	 */
	protected int[] splitstarts;

	protected NumCorpus trainCorpus;

	protected NumCorpus testCorpus;

	protected int[][] origDocIds;

	/**
	 * term element before which a paragraph ends. Iterating through this allows
	 * to read the corpus by paragraph.
	 * <p>
	 * when paragraph mode is enabled, duplicate term ids in one document's term
	 * vector are allowed. Conversion to docWords is transparent, merging to
	 * unstructured docs using mergeDocuments.
	 */
	int[][] parbounds;

	/**
	 * word before a paragraph end.
	 */
	private int[][] wordparbounds;

	private int readlimit = -1;

	public NumCorpus(String dataFilename) {
		read(dataFilename);
	}

	/**
	 * init the corpus with a reduced set of documents
	 * 
	 * @param dataFilename
	 * @param readlimit
	 */
	public NumCorpus(String dataFilename, int readlimit) {
		this.readlimit = readlimit;
		read(dataFilename);
	}

	public NumCorpus() {

	}

	public NumCorpus(Document[] docs, int numTerms, int numWords) {
		this.numTerms = numTerms;
		this.numWords = numWords;
		numDocs = docs.length;
		this.docs = docs;
	}

	static int OFFSET = 0; // offset for reading data

	/**
	 * read a file in "pseudo-SVMlight" format. The format is extended by a
	 * paragraph-aware version that repeats the pattern
	 * <p>
	 * nterms (term:freq){nterms}
	 * <p>
	 * for each paragraph in the document. This way, each paragraph
	 * 
	 * @param dataFilename
	 */
	public void read(String dataFilename) {
		int length, count = 0, word, n, nd, nt, ns, nw = 0;

		if (debug)
			System.out.println("reading data from " + dataFilename);

		try {
			ArrayList<Document> cdocs = new ArrayList<Document>();
			BufferedReader br = new BufferedReader(new FileReader(dataFilename));
			nd = 0;
			nt = 0;
			String line = "";
			parbounds = null;
			boolean parmode = false;
			while ((line = br.readLine()) != null) {
				// one document per line
				String[] fields = line.trim().split("\\s+");
				if (fields[0].equals("") || fields[0].equals("0"))
					continue;
				length = Integer.parseInt(fields[0]);
				// if single paragraph
				if (length == fields.length - 1) {

					Document d = new Document();
					cdocs.add(d);
					d.setNumTerms(length);
					d.setNumWords(0);
					d.setTerms(new int[length]);
					d.setCounts(new int[length]);

					for (n = 0; n < length; n++) {
						// fscanf(fileptr, "%10d:%10d", &word, &count);
						String[] numbers = fields[n + 1].split(":");
						if (numbers[0].equals("") || numbers[0].equals(""))
							continue;
						word = Integer.parseInt(numbers[0]);
						count = (int) Float.parseFloat(numbers[1]);
						nw += count;
						word = word - OFFSET;
						d.setTerm(n, word);
						d.setCount(n, count);
						d.setNumWords(d.getNumWords() + count);
						if (word >= nt) {
							nt = word + 1;
						}
					}
				} else {
					// more than one paragraph
					// TODO: merge with other case
					parmode = true;
					int nextpar = 0;
					int token = 0;
					Document pd = new Document();
					cdocs.add(pd);
					while (nextpar < fields.length) {
						length = Integer.parseInt(fields[nextpar]);
						nextpar += length + 1;
						token++;
						Document d = new Document();
						d.setNumTerms(length);
						d.setNumWords(0);
						d.setTerms(new int[length]);
						d.setCounts(new int[length]);

						for (n = 0; n < length; n++, token++) {
							String[] numbers = fields[token].split(":");
							if (numbers[0].equals("") || numbers[0].equals(""))
								continue;
							word = Integer.parseInt(numbers[0]);
							count = (int) Float.parseFloat(numbers[1]);
							nw += count;
							word = word - OFFSET;
							d.setTerm(n, word);
							d.setCount(n, count);
							d.setNumWords(d.getNumWords() + count);
							if (word >= nt) {
								nt = word + 1;
							}
						}
						pd.addDocument(d);
					}
				}
				if (nd % 1000 == 0) {
					System.out.println(nd);
				}
				nd++;
				// stop if read limit reached
				if (readlimit >= 0 && nd >= readlimit) {
					break;
				}
			}
			numDocs = nd;
			numTerms = nt;
			numWords = nw;
			docs = cdocs.toArray(new Document[] {});
			// if any document in paragraph mode
			if (parmode) {
				parbounds = new int[docs.length][];
				for (int m = 0; m < docs.length; m++) {
					parbounds[m] = docs[m].getParBounds();
				}
			}
			origDocIds = new int[2][];
			origDocIds[0] = Vectors.range(0, nd - 1);
			if (debug) {
				System.out.println("number of docs    : " + nd);
				System.out.println("number of terms   : " + nt);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	public Document[] getDocs() {
		return docs;
	}

	/**
	 * get array of document terms and frequencies
	 * 
	 * @return docs[0 = terms, 1 = frequencies][m][t]
	 */
	public int[][][] getDocTermsFreqs() {
		// words in documents
		int[][][] documents = new int[2][getNumDocs()][];
		for (int i = 0; i < getNumDocs(); i++) {
			documents[0][i] = getDoc(i).getTerms();
			documents[1][i] = getDoc(i).getCounts();
		}
		return documents;
	}

	/**
	 * get array of paragraph start indices of the documents (term-based)
	 * 
	 * @return
	 */
	public int[][] getDocParBounds() {
		return parbounds;
	}

	/**
	 * get array of paragraph start indices of the documents (word-based)
	 * 
	 * @return
	 */
	public int[][] getDocWordParBounds() {
		if (parbounds == null) {
			return null;
		}
		int[][] psnwords = new int[numDocs][];
		for (int m = 0; m < numDocs; m++) {
			psnwords[m] = getDocWordParBounds(m);
		}
		return psnwords;
	}

	/**
	 * return word-based paragraph starts for doc m
	 * 
	 * @param m
	 * @return
	 */
	private int[] getDocWordParBounds(int m) {
		Document d = docs[m];
		int[] termbounds = d.getParBounds();
		int[] wordbounds = new int[termbounds.length];
		int prevbound = 0;
		int prevwbound = 0;
		for (int j = 0; j < termbounds.length; j++) {
			// sum up frequencies of range corresponding to paragraph
			wordbounds[j] = Vectors.sum(Vectors.sub(d.counts, prevbound,
					termbounds[j] - prevbound)) + prevwbound;
			prevbound = termbounds[j];
			prevwbound = wordbounds[j];
		}
		return wordbounds;
	}

	/**
	 * merge document paragraphs into a single document each.
	 */
	public void mergeDocPars() {
		if (parbounds == null) {
			return;
		}
		for (int i = 0; i < docs.length; i++) {
			docs[i].mergeDocument(null);
		}
		// invalidate paragraph start info
		parbounds = null;
	}

	/**
	 * @param index
	 * @return
	 */
	public Document getDoc(int index) {
		return docs[index];
	}

	/**
	 * Get the documents as vectors of bag of words, i.e., per document, a
	 * scrambled array of term indices is generated.
	 * 
	 * @param rand
	 *            random number generator or null to use standard generator
	 * @return
	 */
	public int[][] getDocWords(Random rand) {
		// words in documents
		int[][] documents = new int[getNumDocs()][];
		for (int i = 0; i < getNumDocs(); i++) {
			documents[i] = getDocWords(i, rand);
		}
		return documents;
	}

	public int getNumWords() {
		return numWords;
	}

	/**
	 * Get the words of document doc as a scrambled varseq. For paragraph-based
	 * documents, scrambles the paragraphs separately, preserving their
	 * boundaries.
	 * 
	 * @param m
	 * @param rand
	 *            random number generator or null to omit shuffling
	 * @return
	 */
	public int[] getDocWords(int m, Random rand) {
		if (parbounds == null || parbounds[m] == null
				|| parbounds[m].length == 1) {
			ArrayList<Integer> document = new ArrayList<Integer>();
			for (int i = 0; i < docs[m].getTerms().length; i++) {
				int term = docs[m].getTerms()[i];
				for (int j = 0; j < docs[m].getCount(i); j++) {
					document.add(term);
				}
			}
			// permute words so duplicates aren't juxtaposed
			if (rand != null) {
				Collections.shuffle(document, rand);
			} else {
				// no shuffling
				// Collections.shuffle(document);
			}
			int[] a = (int[]) ArrayUtils.asPrimitiveArray(document);
			return a;
		} else {
			if (wordparbounds == null) {
				wordparbounds = new int[parbounds.length][];
			}
			wordparbounds[m] = new int[parbounds[m].length];
			int nw = 0;
			int tstart = 0;
			int[] words = new int[Vectors.sum(docs[m].counts)];
			for (int s = 0; s < parbounds[m].length; s++) {
				int tend = parbounds[m][s];
				ArrayList<Integer> par = new ArrayList<Integer>();
				for (int i = tstart; i < tend; i++) {
					int term = docs[m].getTerms()[i];
					for (int j = 0; j < docs[m].getCount(i); j++) {
						par.add(term);
					}
				}
				// permute words so duplicates aren't juxtaposed
				if (rand != null) {
					Collections.shuffle(par, rand);
				} else {
					// no shuffling
					// Collections.shuffle(document);
				}
				for (int i = 0; i < par.size(); i++) {
					words[nw + i] = par.get(i);
				}
				tstart = tend;
				nw += par.size();
			}
			return words;
		}
	}

	/**
	 * @param index
	 * @param doc
	 */
	public void setDoc(int index, Document doc) {
		docs[index] = doc;
	}

	/**
	 * @return
	 */
	public int getNumDocs() {
		return numDocs;
	}

	/**
	 * @return
	 */
	public int getNumTerms() {
		return numTerms;
	}

	public int getNumTerms(int doc) {
		return docs[doc].getNumTerms();
	}

	public int getNumWords(int doc) {
		return docs[doc].getNumWords();
	}

	/**
	 * @param documents
	 */
	public void setDocs(Document[] documents) {
		docs = documents;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("Corpus {numDocs=" + numDocs + " numTerms=" + numTerms + "}");
		return b.toString();
	}

	/**
	 * reduce the size of the corpus to ndocs maximum. This should be called
	 * directly after loading as it only reduces the documents and count
	 * 
	 * @param ndocs
	 * @param rand
	 */
	public void reduce(int ndocs, Random rand) {
		System.out.println(numDocs + ".");
		if (numDocs > ndocs) {
			Document[] docsnew = new Document[ndocs];
			for (int i = 0; i < ndocs; i++) {
				docsnew[i] = docs[i];
			}
			docs = docsnew;
			numDocs = ndocs;
		}
	}

	/**
	 * splits two child corpora of size 1/nsplit off the original corpus, which
	 * itself is left unchanged (except storing the splits). The corpora can be
	 * retrieved using getTrainCorpus and getTestCorpus after using this
	 * function.
	 * 
	 * @param order
	 *            number of partitions
	 * @param split
	 *            0-based split of corpus returned
	 * @param rand
	 *            random source (null for reusing existing splits)
	 */
	// @Override
	public void split(int order, int split, Random rand) {

		int Mtest;
		int mstart;
		int numTestWords;
		if (rand != null) {
			RandomSamplers rs = new RandomSamplers(rand);
			splitperm = rs.randPerm(numDocs);
			splitstarts = new int[order + 1];
		}
		for (int p = 0; p <= order; p++) {
			splitstarts[p] = Math.round(numDocs * (p / (float) order));
		}
		Mtest = splitstarts[split + 1] - splitstarts[split];
		mstart = splitstarts[split];
		origDocIds = new int[][] { new int[numDocs - Mtest], new int[Mtest] };
		Document[] trainDocs = new Document[numDocs - Mtest];
		Document[] testDocs = new Document[Mtest];

		int mtrain = 0;
		// before test split
		for (int m = 0; m < mstart; m++) {
			trainDocs[mtrain] = docs[splitperm[m]];
			origDocIds[0][mtrain] = splitperm[m];
			mtrain++;
		}
		// after test split
		for (int m = splitstarts[split + 1]; m < numDocs; m++) {
			trainDocs[mtrain] = docs[splitperm[m]];
			origDocIds[0][mtrain] = splitperm[m];
			mtrain++;
		}
		// test split
		numTestWords = 0;
		for (int m = 0; m < Mtest; m++) {
			testDocs[m] = docs[splitperm[m + mstart]];
			origDocIds[1][m] = splitperm[m + mstart];
			numTestWords += testDocs[m].getNumWords();
		}

		trainCorpus = new NumCorpus(trainDocs, numTerms, numWords
				- numTestWords);
		testCorpus = new NumCorpus(testDocs, numTerms, numTestWords);
	}

	/**
	 * return the training corpus split
	 */
	public ICorpus getTrainCorpus() {
		return trainCorpus;
	}

	/**
	 * return the test corpus split
	 */
	public ICorpus getTestCorpus() {
		return testCorpus;
	}

	/**
	 * get the original ids of documents according to the corpus file read in.
	 * If never split, null.
	 * 
	 * @return [training documents, test documents]
	 */
	public int[][] getOrigDocIds() {
		return origDocIds;
	}

	/**
	 * write the corpus to to a file. TODO: write also document titles and
	 * labels (in subclass)
	 * 
	 * @param pathbase
	 * @throws IOException
	 */
	public void write(String pathbase) throws IOException {
		BufferedWriter bwcorp = new BufferedWriter(new FileWriter(pathbase
				+ ".corpus"));
		for (int m = 0; m < docs.length; m++) {
			if (m % 100 == 0) {
				System.out.println(m);
			}
			Document doc = docs[m];
			if (doc.getParBounds() == null) {
				bwcorp.append(Integer.toString(doc.numTerms));
				for (int n = 0; n < doc.numTerms; n++) {
					bwcorp.append(" " + doc.terms[n] + ":" + doc.counts[n]);
				}
				bwcorp.append('\n');
			} else {
				// paragraph mode
				int prevbound = 0;
				for (int s = 0; s < doc.parBounds.length; s++) {
					if (s > 0) {
						bwcorp.append(" ");
					}
					bwcorp.append(Integer.toString(doc.numTerms));
					for (int n = prevbound; n < doc.parBounds[s]; n++) {
						bwcorp.append(" " + doc.terms[n] + ":" + doc.counts[n]);
					}
					prevbound = doc.parBounds[s];
				}
				bwcorp.append('\n');
			}
		}
		bwcorp.close();
	}

	/**
	 * test corpus reading and splitting
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		NumCorpus nc = new NumCorpus("berry95/berry95.corpus");
		nc.split(10, 0, new Random());
		System.out.println("train");
		ICorpus ncc = nc.getTrainCorpus();
		System.out.println(ncc);
		int[][] x = ncc.getDocWords(new Random());
		System.out.println(Vectors.print(x));
		System.out.println("test");
		ncc = nc.getTestCorpus();
		System.out.println(ncc);
		x = ncc.getDocWords(new Random());
		System.out.println(Vectors.print(x));
		System.out.println("document mapping");
		System.out.println(Vectors.print(nc.getOrigDocIds()));
	}
}
