/*
 * Created on Jul 23, 2009
 */
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

import java.util.Random;

/**
 * ISplitCorpus allows a corpus to resize and split a cross validation data set.
 * 
 * @author gregor
 */
public interface ISplitCorpus {

    /**
     * splits two child corpora of size 1/nsplit off the original corpus, which
     * itself is left unchanged (except storing the splits). The corpora can be
     * retrieved using getTrainCorpus and getTestCorpus after using this
     * function.
     * 
     * @param order number of partitions
     * @param split 0-based split of corpus returned
     * @param rand random source (null for reusing existing splits)
     */
    public void split(int order, int split, Random rand);

    /**
     * called after split()
     * 
     * @return the training corpus according to the last splitting operation
     */
    public ICorpus getTrainCorpus();

    /**
     * called after split()
     * 
     * @return the test corpus according to the last splitting operation
     */
    public ICorpus getTestCorpus();

    /**
     * get the original ids of documents
     * 
     * @return [training documents, test documents]
     */
    public int[][] getOrigDocIds();
}
