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


java -Xmx4000M -jar hmdp.jar -directory "/home/c/work/topicmodels/ml/" -T 100 -TRAINING_SHARE 1.0 -BATCHSIZE 128 -BATCHSIZE_GROUPS 128 -RUNS 100 -BURNIN 0 


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
