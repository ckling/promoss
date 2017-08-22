
# Promoss Topic Modelling Toolbox
(C) Copyright 2016, Christoph Carl Kling

Promoss makes use of multiple free software packages -- thanks to the authors of:

Knoceans by Gregor Heinrich Gregor Heinrich (gregor :: arbylon : net) published under GNU GPL.

Tartarus Snowball stemmer by Martin Porter and Richard Boulton published under BSD License (see http://www.opensource.org/licenses/bsd-license.html ), with Copyright (c) 2001, Dr Martin Porter, and (for the Java developments) Copyright (c) 2002, Richard Boulton. 

Quickhull3D Copyright by John E. Lloyd, 2004. 

Apache Xerces Java and NekoHTML are released under Apache License 2.0.

McCallum, Andrew Kachites.  "MALLET: A Machine Learning for Language Toolkit." http://mallet.cs.umass.edu. 2002, released under the Common Public License. http://www.opensource.org/licenses/cpl1.0.php

Promoss is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.

Promoss is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

## Support
Please contact me if you need help running the code: promoss (ät) c-kling.de

---

## First steps

### Building the jar file
You can build the promoss.jar using Ant. Go to the directory of the extracted promoss.tar.gz file (in which the build.xml is located) and enter the command:
```
ant; ant build-jar
```
(The ant build might yield errors for classes under development which can be ignored.)

### Demo files
If you would like to have demo files to play around with, just write a mail to promoss@c-kling.de

---

## Latent Dirichlet Allocation (LDA)
Collapsed stochastic variational inference for LDA with an asymmetric document-topic prior.

### Example command line usage
```
java -Xmx11000M -jar promoss.jar -directory demo/ml_demo/ -method "LDA" -MIN_DICT_WORDS 0 -T 5
```

### Input files
The most simple way to feed your documents into the topic model is via the corpus.txt file, which can include raw documents (each line corresponds to a document). From this corpus.txt, a wordsets file with the processed documents in SVMlight format is created, called wordsets. You can also directly give the wordsets file and a words.txt dictionary, where the line number (starting with 0) corresponds to the word ID in the SVMlight file.

#### corpus.txt
Each line corresponds to a document. Words of documents are separated by spaces. (However, one can also input raw text and set the -processed parameter to false in order to use a library-specific code for splitting words.)

Example corpus.txt:
```
exist distribut origin softwar distributor agre gpl
gpl establish term distribut origin softwar even goe unmodifi word distribut gpl softwar one agre 
dynam link constitut make deriv work allow dynam link long rule follow code make deriv work rule
gpl also deal deriv work link creat deriv work gpl affect gpl defin scope copyright law gpl section 
```

#### words.txt 
This optional file gives the vocabulary, one word per row. The line numbers correspond to the later indices in the topic-word matrix.

### Output files
Cluster descriptions (e.g. means of the geographical clusters, bins of timestamps etc.) are saved in the cluster_desc/ folder.
After each 10 runs, important parameters are stored in the output_Promoss/ subfolder, with the number of runs as folder name. The clusters_X file contains the topic loadings of each cluster of the Xth metadata. The topktopics file contains the top words of each topic (the number of returned top words can be set via the -topk parameter).

### Mandatory parameter
* directory 		String. Gives the directory of the texts.txt file.


### Optional parameters:
* T			Integer. Number of topics. Default: 100
* RUNS			Integer. Number of iterations the sampler will run. Default: 200
* SAVE_STEP		Integer. Number of iterations after which the learned paramters are saved. Default: 10
* TRAINING_SHARE		Double. Gives the share of documents which are used for training (0 to 1). Default: 1
* BATCHSIZE		Integer. Batch size for topic estimation. Default: 128
* BURNIN			Integer. Number of iterations till the topics are updated. Default: 0
* INIT_RAND		Double. Topic-word counts are initiatlised as INIT_RAND * RANDOM(). Default: 0
* MIN_DICT_WORDS		Integer. If the words.txt file is missing, words.txt is created by using words which occur at least MIN_DICT_WORDS times in the corpus. Default: 100
* save_prefix		String. If given, this String is appended to all output files.
* alpha			Double. Initial value of alpha_0. Default: 1
* rhokappa		Double. Initial value of kappa, a parameter for the learning rate of topics. Default: 0.5
* rhotau			Integer. Initial value of tau, a parameter for the learning rate of topics. Default: 64
* rhos			Integer. Initial value of s, a parameter for the learning rate of topics. Default: 1
* rhokappa_document	Double. Initial value of kappa, a parameter for the learning rate of the document-topic distribution. Default: kappa
* rhotau_document	Integer. Initial value of tau, a parameter for the learning rate of the document-topic distribution. Default: tau
* rhos_document		Integer. Initial value of tau, a parameter for the learning rate of the document-topic distribution. Default: rhos
* processed		Boolean. Tells if the text is already processed, or if words should be split with complex regular expressions. Otherwise split by spaces. Default: true.
* stemming		Boolean. Activates word stemming in case no words.txt/wordsets file is given. Default: false
* stopwords		Boolean. Activates stopword removal in case no words.txt/wordsets file is given. Default: false
* language		String. Currently "en" and "de" are available languages for stemming. Default: "en"
* store_empty		Boolean. Determines if empty documents should be omitted in the final document-topic matrix or if the topic distribution should be predicted using the context. Default: True
* topk			Integer. Set the number of top words returned in the topktopics file of the output. Default: 100

---

## Hierarchical Multi-Dirichlet Process Topic Model (Promoss)
An efficient topic model which uses arbitrary document metadata!

For a description of the model, I refer to Chapter 4 of my dissertation: 

[Christoph Carl Kling. Probabilistic Models for Context in Social Media - Novel Approaches and Inference Schemes. 2016](https://kola.opus.hbz-nrw.de/frontdoor/deliver/index/docId/1397/file/DissertationChristophKling.pdf)

### Example command line usage
```
java -Xmx11000M -jar promoss.jar -directory demo/ml_demo/ -meta_params "T(L1000,W1000,D10,Y100,M20);N" -MIN_DICT_WORDS 1000
```

This will sample topics from a demo dataset of 1000 messages of the linux kernel mailing list. Messages are already stemmed and stopwords were removed. There are 1000 clusters for the first four contexts (which are the timeline and the yearly, weekly and daily cycle). Many clusters are empty, because the original dataset contained >3m documents. This is just for testing if the algorithm runs, a demo dataset  with nicer results is in preparation.

### Input file format
There is two standard input formats for the data.
The first is based on raw, unclustered metadata stored in meta.txt and the corpus, stored in corpus.txt
The second is based on already clustered data (texts.txt) with given document groups defined in groups.txt (groups of documents share the same parent clusters in the Promoss).
When running the code, the script first looks for a texts.txt and a groups.txt. If any of those documents is missing, the script looks for the corpus.txt and meta.txt, from which it generates the texts.txt and corpus.txt.
Finally, a groups file with the groups of the documents and a wordsets file with the processed documents in SVMlight format are created.

#### Variant 1
##### corpus.txt 
Each line corresponds to a document. Words of documents are separated by spaces. (However, one can also input raw text and set the -processed parameter to false in order to use a library-specific code for splitting words.)

Example corpus.txt:
```
  exist distribut origin softwar distributor agre gpl
  gpl establish term distribut origin softwar even goe unmodifi word distribut gpl softwar one agre 
  dynam link constitut make deriv work allow dynam link long rule follow code make deriv work rule
  gpl also deal deriv work link creat deriv work gpl affect gpl defin scope copyright law gpl section 
```


##### meta.txt 
Here we give the metadata values separated by semicolons. Possible metadata are geographical coordinates (latitude and longitude separated by comma), UNIX timestamps (in seconds), nominal values (e.g. category names, numbers) or oordinal variables (stored numbers which correspond to the ordering). The metadata types have to be specified via the -meta_params parameter (see below for a description).

Example meta.txt:
```
33.150051,-114.365448;1139316299;1
34.150051,-118.365448;1139316058;2
43.59772,-116.235705;1139261931;3
14.559243,120.982732;1139256458;2
```

#### Variant 2
##### texts.txt 
Each line corresponds to a document. First, the context group IDs (for each context one) are given, separated by commas. The context group in context 0 is given first, then the context group in context 1 and so on. Then follows a space and the words of the documents separated by spaces. 

Example texts.txt:
```
254,531,790,157,0  exist distribut origin softwar distributor agre gpl
254,528,789,157,0  gpl establish term distribut origin softwar even goe unmodifi word distribut gpl softwar one agre 
254,901,700,157,0  dynam link constitut make deriv work allow dynam link long rule follow code make deriv work rule
254,838,691,157,0  gpl also deal deriv work link creat deriv work gpl affect gpl defin scope copyright law gpl section 
```

##### groups.txt 
Each line gives the parent context clusters of a context group. Data are separated by spaces. The first column gives the context id, the second column gives the group ID of the context group, and then the IDs of the context clusters from which the documents of that context group draw their topics are given.

Example groups.txt
```
0 0 0 1
0 1 0 1 2
0 2 1 2 3
0 3 2 3 4
0 4 3 4 5
0 5 4 5 6
0 6 5 6 7
0 7 6 7 8
0 8 7 8 9
0 9 8 9 10
0 10 9 10 11
```

The first line reads: For context 0, documents which are assigned to context group 0 draw their topics from context cluster 0 and context cluster 1.
If no groups.txt is given, all context groups will be linked to a context cluster with the same ID, which means that all context clusters are independent.

#### words.txt 
This optional file gives the vocabulary, one word per row. The line numbers correspond to the later indices in the topic-word matrix.

### Output files
Cluster descriptions (e.g. means of the geographical clusters, bins of timestamps etc.) are saved in the cluster_desc/ folder.
After each 10 runs, important parameters are stored in the output_Promoss/ subfolder, with the number of runs as folder name. The clusters_X file contains the topic loadings of each cluster of the Xth metadata. The topktopics file contains the top words of each topic (the number of returned top words can be set via the -topk parameter).

### Mandatory parameter
* directory 		String. Gives the directory of the texts.txt and groups.txt file.

### Mandatory Parameters when Using corpus.txt and meta.txt (Input Variant 1)
* meta_params		String. Specifies the metadata types and gives the desired clustering. Types of metadata are given separated by semicolons (and correspond to the number of different metadata in the meta.txt file. Possible datatypes are:
 * G	Geographical coordinates. The number of desired clusters is specified in brackets, i.e. G(1000) will cluster the documents into 1000 clusters based on the geographical coordinates. (Technical detail: we use EM to fit a mixture of fisher distributions.)
 * T	UNIX timestamps (in seconds). The number of clusters (based on binning) is given in brackets, and there can be multiple clusterings based on a binning on the timeline or temporal cycles. This is indicated by a letter followed by the number of desired clusters:
 * L	Binning based on the timeline. Example: L1000 gives 1000 bins.
 * Y	Binning based on the yearly cycle. Example: L1000 gives 1000 bins.
 * M	Binning based on the monthly cycle. Example: L1000 gives 1000 bins.
 * W	Binning based on the weekly cycle. Example: L1000 gives 1000 bins.
 * D	Binning based on the daily  cycle. Example: L1000 gives 1000 bins.
 * O	Ordinal values (numbers)
 * N	Nominal values (text strings)
			

Example usage in the -meta_params parameter: 
```
-meta_params "G(1000);T(L1000,Y100,M10,W20,D10);O"
```

This command can be used for the meta.txt given above. It would create 1000 geographical clusters based on the latitude and longitude. Then it would parse each UNIX timestamp to create 1000 clusters on the timeline, 100 clusters on the yearly, 10 clusters on the monthly, 20 clusters on the weekly and 10 clusters on the daily cycle (based on simple binning). Then the third metadata variable would be interpreted as an ordinal variable, meaning that each different value is an own cluster which is smoothed with the previous and next cluster (if existent).

#### Rule of thumb for clustering
Clusters should not be too small, because the observed documents in a cluster should be sufficient to learn a cluster-specific topic prior.
On the other hand, too few clusters prevent the model from capturing differences in topic frequencies in the context space.
One rule of thumb for the number of clusters C in a corpus with M documents and (an expected number of) T topics is: C = M/T. I.e. if we have 1.000.000 documents and expect about 100 topics, it is reasonable to pick 10.000 clusters. This approximation is very simplistic, I recommend to use e.g. Dirichlet process-based methods such as infinite Gaussian mixture models for cluster detection before running the model. 

### Optional parameters
The parameters are sorted, most common parameters are on top:
* T			Integer. Number of truncated topics. Default: 100
* RUNS			Integer. Number of iterations the sampler will run. Default: 200
* processed		Boolean. Tells if the text is already processed, or if words should be split with complex regular expressions. Otherwise split by spaces. Default: true.
* stemming		Boolean. Activates word stemming in case no words.txt/wordsets file is given. Default: false
* stopwords		Boolean. Activates stopword removal in case no words.txt/wordsets file is given. Default: false
* language		String. Currently "en" and "de" are available languages for stemming. Default: "en"
* store_empty		Boolean. Determines if empty documents should be omitted in the final document-topic matrix or if the topic distribution should be predicted using the context. Default: True
* TRAINING_SHARE		Double. Gives the share of documents which are used for training (0 to 1). Default: 1
* topk			Integer. Set the number of top words returned in the topktopics file of the output. Default: 100
* gamma			Double. Initial scaling parameter of the top-level Dirichlet process. Default: 1
* learn_gamma		Boolean. Should gamma be learned during inference? Default: True
* SAVE_STEP		Integer. Number of iterations after which the learned paramters are saved. Default: 10
* BATCHSIZE		Integer. Batch size for topic estimation. Default: 128
* BATCHSIZE_GROUPS	Integer. Batch size for group-specific parameter estimation. Default: BATCHSIZE
* BURNIN			Integer. Number of iterations till the topics are updated. Default: 0
* BURNIN_DOCUMENTS	Integer. Gives the number of sampling iterations where the group-specific parameters are not updated yet. Default: 0
* INIT_RAND		Double. Topic-word counts are initiatlised as INIT_RAND * RANDOM(). Default: 0
* SAMPLE_ALPHA		Integer. Every SAMPLE_ALPHAth document is used to estimate alpha_1. Default: 1
* BATCHSIZE_ALPHA	Integer. How many observations do we take before updating alpha_1. Default: 1000
* MIN_DICT_WORDS		Integer. If the words.txt file is missing, words.txt is created by using words which occur at least MIN_DICT_WORDS times in the corpus. Default: 100
* save_prefix		String. If given, this String is appended to all output files.
* alpha_0		Double. Initial value of alpha_0. Default: 1
* alpha_1		Double. Initial value of alpha_1. Default: 1
* epsilon		Comma-separated double. Dirichlet prior over the weights of contexts. Comma-separated double values, with dimensionality equal to the number of contexts.
* delta_fix 		If set, delta is fixed and set to this value. Otherwise delta is learned during inference.
* rhokappa		Double. Initial value of kappa, a parameter for the learning rate of topics. Default: 0.5
* rhotau			Integer. Initial value of tau, a parameter for the learning rate of topics. Default: 64
* rhos			Integer. Initial value of s, a parameter for the learning rate of topics. Default: 1
* rhokappa_document	Double. Initial value of kappa, a parameter for the learning rate of the document-topic distribution. Default: kappa
* rhotau_document	Integer. Initial value of tau, a parameter for the learning rate of the document-topic distribution. Default: tau
* rhos_document		Integer. Initial value of tau, a parameter for the learning rate of the document-topic distribution. Default: rhos
* rhokappa_group		Double. Initial value of kappa, a parameter for the learning rate of the group-topic distribution. Default: kappa
* rhotau_group		Integer. Initial value of tau, a parameter for the learning rate of the group-topic distribution. Default: tau
* rhos_group		Integer. Initial value of tau, a parameter for the learning rate of the group-topic distribution. Default: rhos




