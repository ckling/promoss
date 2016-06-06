/*
 * Created on 09.06.2007
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

/**
 * ILabelCorpus is a corpus with label information. It can be used to add an
 * additional observed node to a topic model.
 * 
 * @author gregor
 */
public interface ILabelCorpus extends ICorpus {

    public static final int LDOCS = -2;
    public static final int LTERMS = -1;
    // these are the rows of the data field in label corpus
    public static final int LAUTHORS = 0;
    public static final int LCATEGORIES = 1;
    public static final int LVOLS = 2;
    public static final int LREFERENCES = 3;
    public static final int LTAGS = 4;
    public static final int LYEARS = 5;

    /**
     * array with label ids for documents
     * 
     * @param kind of labels
     * @return
     */
    int[][] getDocLabels(int kind);

    /**
     * get the number of tokens in the label field
     * 
     * @param kind
     * @return
     */
    public int getLabelsW(int kind);

    /**
     * get the number of distinct labels in the label field
     * 
     * @param kind
     * @return
     */
    public int getLabelsV(int kind);

}
