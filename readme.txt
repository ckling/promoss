***************************
Hierarchical Multi-Dirichlet Process Topic Model (HMDP topic model)
***************************

(C) Copyright 2016, Christoph Carl Kling

Knoceans by Gregor Heinrich Gregor Heinrich (gregor :: arbylon : net)
published under GNU GPL.

Tartarus Snowball stemmer by Martin Porter and Richard Boulton published under 
BSD License (see http://www.opensource.org/licenses/bsd-license.html ), with Copyright 
(c) 2001, Dr Martin Porter, and (for the Java developments) Copyright (c) 2002, 
Richard Boulton. 

Java Delaunay Triangulation (JDT) by boaz88 :: gmail : com published under Apache License 2.0 
(http://www.apache.org/licenses/LICENSE-2.0)

PCFSTM is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 3 of the License, or (at your option) 
any later version.

PCFSTM is distributed in the hope that it will be useful, but WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program; if not, write to the Free Software Foundation, Inc., 59 Temple
Place, Suite 330, Boston, MA 02111-1307 USA

***************************
Notes
***************************

This is the practical collapsed stochastic variational Bayesian inference (PCSVB) for the HMDP.

A detailed tutorial and documentation of the HMDP is in preparation.

In future versions, there will be:
- support for typical metadata, i.e. temporal and geographical context
- a distributed version of PCSVB of HMDP based on Hadoop 

The goal is to create an easy-to-use, scalable topic model for arbitrary context.

***************************
Example command line usage
***************************
java -Xmx4000M -jar hmdp.jar -directory "/home/c/work/topicmodels/ml/" -T 100 -TRAINING_SHARE 1.0 -BATCHSIZE 128 -BATCHSIZE_GROUPS 128 -RUNS 100 -BURNIN 0 


***************************
Input file format
***************************
--------------------------- 
texts.txt
--------------------------- 
Each line corresponds to a document. First, the context group IDs (for each context one) are given, separated by commas. The context group in context 0 is given first, then the context group in context 1 and so on. Then follows a space and the words of the documents separated by spaces. 
Example file:
254,531,790,157,0  claus exist distribut origin softwar distributor agre gpl
254,528,789,157,0  gpl establish term distribut origin softwar even goe unmodifi word distribut gpl softwar one agre 
254,901,700,157,0  dynam link constitut make deriv work allow dynam link long rule follow code make deriv work rule
254,838,691,157,0  gpl also deal deriv work link creat deriv work gpl affect gpl defin scope copyright law gpl section 

--------------------------- 
groups.txt
--------------------------- 
Each line gives the parent context clusters of a context group. Data are separated by spaces. The first column gives the context id, the second column gives the group ID of the context group, and then the IDs of the context clusters from which the documents of that context group draw their topics are given.
Example file:
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

The first line reads: For context 0, documents which are assigned to context group 0 draw their topics from context cluster 0 and context cluster 1.


***************************
Mandatory parameter
***************************
-directory 		String. Gives the directory of the texts.txt and groups.txt file.

***************************
Optional parameters:
***************************
-T			Integer. Number of truncated topics
-RUNS			Integer. Number of iterations the sampler will run. Default: 200
-SAVE_STEP		Integer. Number of iterations after which the learned paramters are saved. Default: 10
-TRAINING_SHARE		Double. Gives the share of documents which are used for training (0 to 1). Default: 1
-BATCHSIZE		Integer. Batch size for topic estimation. Default: 128
-BATCHSIZE_GROUPS	Integer. Batch size for group-specific parameter estimation. Default: BATCHSIZE
-BURNIN			Integer. Number of iterations till the topics are updated. Default: 200
-BURNIN_DOCUMENTS	Integer. Gives the number of sampling iterations where the group-specific parameters are not updated yet. Default: 0
-INIT_RAND		Double. Topic-word counts are initiatlised as INIT_RAND * RANDOM(). Default: 0
-SAMPLE_ALPHA		Integer. Every SAMPLE_ALPHAth document is used to estimate alpha_1. Default: 1
-BATCHSIZE_ALPHA	Integer. How many observations do we take before updating alpha_1. Default: 1000
-MIN_DICT_WORDS		Integer. If the words.txt file is missing, words.txt is created by using words which occur at least MIN_DICT_WORDS times in the corpus. Default: 100
-save_prefix		String. If given, this String is appended to all output files.
-delta_fix 		If set, delta is fixed and set to this value.
-alpha_0		Double. Initial value of alpha_0. Default: 1
-alpha_1		Double. Initial value of alpha_1. Default: 1
-rhokappa		Double. Initial value of kappa, a parameter for the learning rate of topics. Default: 0.5
-rhotau			Integer. Initial value of tau, a parameter for the learning rate of topics. Default: 64
-rhos			Integer. Initial value of s, a parameter for the learning rate of topics. Default: 1
-rhokappa_document	Double. Initial value of kappa, a parameter for the learning rate of the document-topic distribution. Default: kappa
-rhotau_document	Integer. Initial value of tau, a parameter for the learning rate of the document-topic distribution. Default: tau
-rhos_document		Integer. Initial value of tau, a parameter for the learning rate of the document-topic distribution. Default: rhos
-rhokappa_group		Double. Initial value of kappa, a parameter for the learning rate of the group-topic distribution. Default: kappa
-rhotau_group		Integer. Initial value of tau, a parameter for the learning rate of the group-topic distribution. Default: tau
-rhos_group		Integer. Initial value of tau, a parameter for the learning rate of the group-topic distribution. Default: rhos
-processed		Boolean. Tells if the text is already processed, or if words should be split with complex regular expressions. Otherwise split by spaces.
-stemming		Boolean. Activates word stemming in case no words.txt/wordsets file is given.
-stopwords		Boolean. Activates stopword removal in case no words.txt/wordsets file is given.
-language		String. Currently "en" and "de" are available languages for stemming.
