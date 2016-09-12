package org.gesis.promoss.inference;

import java.io.File;

import org.gesis.promoss.metadata.ClusterMetadata;


public class Run {


	public static void main (String[] args)  {

		//if (new File("/home/c/").exists() && args == null) {
		//	args = "-directory /home/c/work/topicmodels/maryam7/ -method HMDP -INIT_RAND 1 -T 100 -meta_params G(400) -MIN_DICT_WORDS 1 -RUNS 100 -TRAINING_SHARE 0.8".split(" ");
		//}

		if (args == null || args.length == 0) {

			System.out.println("Please specify the model parameters. See readme.txt for details.");

			return;
		}

		String method = "HMDP";
		for (int i=0; i < args.length; i++) {

			if (args[i].equals("-method")) 
				method = args[++i];
		}

		if (method.equals("HMDP")) {

			//TODO: insert in importer
			String params ="G(1000)";

			HMDP_PCSVB hmdp = new HMDP_PCSVB();


			for (int i=0; i < args.length; i++) {

				if (args[i].equals("-directory")) 
					hmdp.c.directory = args[++i];

				else if (args[i].equals("-save_prefix")) 
					hmdp.save_prefix = args[++i];
				
				else if (args[i].equals("-topk")) 
					hmdp.topk = Integer.valueOf(args[++i]);

				else if (args[i].equals("-T")) 
					hmdp.T = Integer.valueOf(args[++i]);

				else if (args[i].equals("-TRAINING_SHARE")) 
					hmdp.TRAINING_SHARE = Double.valueOf(args[++i]);

				else if (args[i].equals("-delta_fix")) 
					hmdp.delta_fix = Double.valueOf(args[++i]);

				else if (args[i].equals("-BATCHSIZE")) 
					hmdp.BATCHSIZE = Integer.valueOf(args[++i]);

				else if (args[i].equals("-BATCHSIZE_GROUPS")) 
					hmdp.BATCHSIZE_GROUPS = Integer.valueOf(args[++i]);

				else if (args[i].equals("-RUNS")) 
					hmdp.RUNS = Integer.valueOf(args[++i]);

				else if (args[i].equals("-BURNIN")) 
					hmdp.BURNIN = Integer.valueOf(args[++i]);

				else if (args[i].equals("-alpha_1")) 
					hmdp.alpha_1 = Double.valueOf(args[++i]);

				else if (args[i].equals("-alpha_0")) 
					hmdp.alpha_0 = Double.valueOf(args[++i]);

				else if (args[i].equals("-MIN_DICT_WORDS")) 
					hmdp.c.MIN_DICT_WORDS = Integer.valueOf(args[++i]);

				else if (args[i].equals("-language")) 
					hmdp.c.language = args[++i];

				else if (args[i].equals("-stemming")) 
					hmdp.c.stemming = Boolean.valueOf(args[++i]);

				else if (args[i].equals("-stopwords")) 
					hmdp.c.stopwords = Boolean.valueOf(args[++i]);

				else if (args[i].equals("-INIT_RAND")) 
					hmdp.INIT_RAND = Double.valueOf(args[++i]);

				else if (args[i].equals("-INIT_RAND")) 
					hmdp.INIT_RAND = Double.valueOf(args[++i]);

				else if (args[i].equals("-BURNIN_DOCUMENTS")) 
					hmdp.BURNIN_DOCUMENTS = Integer.valueOf(args[++i]);

				else if (args[i].equals("-SAMPLE_ALPHA")) 
					hmdp.SAMPLE_ALPHA = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhokappa")) 
					hmdp.rhokappa = Double.valueOf(args[++i]);

				else if (args[i].equals("-rhos")) 
					hmdp.rhos = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhotau")) 
					hmdp.rhotau = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhokappa_document")) 
					hmdp.rhokappa_document = Double.valueOf(args[++i]);

				else if (args[i].equals("-rhos_document")) 
					hmdp.rhos_document = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhotau_document")) 
					hmdp.rhotau_document = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhokappa_group")) 
					hmdp.rhokappa_group = Double.valueOf(args[++i]);

				else if (args[i].equals("-rhos_group")) 
					hmdp.rhos_group = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhotau_group")) 
					hmdp.rhotau_group = Integer.valueOf(args[++i]);

				else if (args[i].equals("-BATCHSIZE_ALPHA")) 
					hmdp.BATCHSIZE_ALPHA = Integer.valueOf(args[++i]);

				else if (args[i].equals("-SAVE_STEP")) 
					hmdp.SAVE_STEP = Integer.valueOf(args[++i]);

				else if (args[i].equals("-processed")) 
					hmdp.c.processed = Boolean.valueOf(args[++i]);

				else if (args[i].equals("-epsilon")) {
					String[] argssplit = args[++i].split(",");
					double[] epsilon = new double[argssplit.length]; 
					for (int j=0;j<epsilon.length;j++) {
						epsilon[j] = Double.valueOf(argssplit[j]);
					}
					hmdp.epsilon = epsilon;
				}

				else if (args[i].equals("-store_empty")) 
					hmdp.store_empty = Boolean.valueOf(args[++i]);

				//G;T(L1000,Y100,M10,W10,D10);N
				else if (args[i].equals("-meta_params")) 
					params = args[++i];
				//Skip method parameter
				else if (args[i].equals("-method")) 
					i++;

				else {
					System.out.println("Can not parse "+args[i]);
					System.out.println("Please specify correct model parameters. See readme.txt for details.");

					return;
				}

			}

			String corpusname = "corpus.txt";
			String metaname = "meta.txt";

			//File corpusfile = new File(hmdp.c.directory + corpusname);
			//File metafile = new File(hmdp.c.directory + metaname);

			File textsFile = new File(hmdp.c.directory + "texts.txt");
			File groupClusterFile = new File(hmdp.c.directory + "groups.txt");

			if (!textsFile.exists() || !groupClusterFile.exists()) {
				
				System.out.println("Clustering metadata...");
				ClusterMetadata.transformData(params, hmdp.c.directory, metaname, corpusname, "cluster/");

				File wordsetfile = new File(hmdp.c.directory + "wordsets");
				if (wordsetfile.exists())
					wordsetfile.delete();
				File groupfile = new File(hmdp.c.directory + "groups");
				if (groupfile.exists())
					groupfile.delete();
			}

			hmdp.initialise();
			hmdp.run();

		}
		else if (method.equals("LDA")) {

			//TODO: insert in importer
			String params ="G(1000)";

			LDA_CSVB lda = new LDA_CSVB();


			for (int i=0; i < args.length; i++) {

				if (args[i].equals("-directory")) 
					lda.c.directory = args[++i];

				else if (args[i].equals("-save_prefix")) 
					lda.save_prefix = args[++i];

				else if (args[i].equals("-T")) 
					lda.T = Integer.valueOf(args[++i]);

				else if (args[i].equals("-TRAINING_SHARE")) 
					lda.TRAINING_SHARE = Double.valueOf(args[++i]);

				else if (args[i].equals("-BATCHSIZE")) 
					lda.BATCHSIZE = Integer.valueOf(args[++i]);

				else if (args[i].equals("-RUNS")) 
					lda.RUNS = Integer.valueOf(args[++i]);

				else if (args[i].equals("-BURNIN")) 
					lda.BURNIN = Integer.valueOf(args[++i]);

				else if (args[i].equals("-MIN_DICT_WORDS")) 
					lda.c.MIN_DICT_WORDS = Integer.valueOf(args[++i]);

				else if (args[i].equals("-language")) 
					lda.c.language = args[++i];

				else if (args[i].equals("-stemming")) 
					lda.c.stemming = Boolean.valueOf(args[++i]);

				else if (args[i].equals("-stopwords")) 
					lda.c.stopwords = Boolean.valueOf(args[++i]);

				else if (args[i].equals("-INIT_RAND")) 
					lda.INIT_RAND = Double.valueOf(args[++i]);

				else if (args[i].equals("-INIT_RAND")) 
					lda.INIT_RAND = Double.valueOf(args[++i]);

				else if (args[i].equals("-BURNIN_DOCUMENTS")) 
					lda.BURNIN_DOCUMENTS = Integer.valueOf(args[++i]);

				else if (args[i].equals("-SAMPLE_ALPHA")) 
					lda.SAMPLE_ALPHA = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhokappa")) 
					lda.rhokappa = Double.valueOf(args[++i]);

				else if (args[i].equals("-rhos")) 
					lda.rhos = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhotau")) 
					lda.rhotau = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhokappa_document")) 
					lda.rhokappa_document = Double.valueOf(args[++i]);

				else if (args[i].equals("-rhos_document")) 
					lda.rhos_document = Integer.valueOf(args[++i]);

				else if (args[i].equals("-rhotau_document")) 
					lda.rhotau_document = Integer.valueOf(args[++i]);

				else if (args[i].equals("-BATCHSIZE_ALPHA")) 
					lda.BATCHSIZE_ALPHA = Integer.valueOf(args[++i]);

				else if (args[i].equals("-SAVE_STEP")) 
					lda.SAVE_STEP = Integer.valueOf(args[++i]);

				else if (args[i].equals("-processed")) 
					lda.c.processed = Boolean.valueOf(args[++i]);

				else if (args[i].equals("-store_empty")) 
					lda.store_empty = Boolean.valueOf(args[++i]);

				//G;T(L1000,Y100,M10,W10,D10);N
				else if (args[i].equals("-meta_params")) 
					params = args[++i];
				//Skip method parameter
				else if (args[i].equals("-method")) 
					i++;

				else {
					System.out.println("Can not parse "+args[i]);
				}

			}

			//System.out.println("Clustering metadata...");
			//ClusterMetadata.transformData(params, lda.c.directory, "meta.txt", "corpus.txt", "cluster/");

			

			lda.initialise();
			lda.run();

		}
	}
}

































































/*
 * Here goes some old code which I just keep for reproducing experiments.
 * You can ignore it.
 */

//
//	public static void maryam() {
//
//		HMDP_PCSVB pi = new HMDP_PCSVB();
//
//		pi.c.directory = "/home/c/work/topicmodels/maryam7/";
//		pi.save_prefix = "100";
//		pi.T=100;
//		pi.TRAINING_SHARE = 1.0;
//		pi.delta_fix = 10;
//		pi.BATCHSIZE = 128;
//		pi.BATCHSIZE_GROUPS = 128;
//		pi.RUNS = 100;
//		pi.BURNIN = 0;
//		pi.alpha_1 = 0.1;
//		pi.c.MIN_DICT_WORDS = 100;
//		pi.INIT_RAND = 1;
//		pi.BURNIN_DOCUMENTS=1;
//		pi.SAMPLE_ALPHA=1;
//		pi.rhokappa=pi.rhokappa_document = 0.5;
//
//
//		pi.initialise();
//		pi.run();
//
//	}
//
//	public static void polseb() {
//
//		HMDP_PCSVB pi = new HMDP_PCSVB();
//
//		pi.directory = "/home/c/work/topicmodels/wiki/";
//		pi.save_prefix = "100";
//		pi.T=100;
//		pi.TRAINING_SHARE = 1.0;
//		pi.delta_fix = 10;
//		pi.BATCHSIZE = 128;
//		pi.BATCHSIZE_GROUPS = 128;
//		pi.RUNS = 100;
//		pi.BURNIN = 0;
//		pi.alpha_1 = 0.1;
//		pi.MIN_DICT_WORDS = 100;
//		pi.INIT_RAND = 1;
//		pi.BURNIN_DOCUMENTS=1;
//		pi.SAMPLE_ALPHA=1;
//		pi.rhokappa=pi.rhokappa_document = 0.5;
//
//
//		pi.initialise();
//		pi.run();
//
//	}
//
//	public static void food8() {
//
//		HMDP_PCSVB pi = new HMDP_PCSVB();
//
//		pi.directory = "food3";
//		pi.save_prefix = "topic8b";
//		pi.T=8;
//		pi.TRAINING_SHARE = 1.0;
//		pi.delta_fix = 10;
//		pi.BATCHSIZE = 64;
//		pi.BATCHSIZE_GROUPS = 64;
//		pi.RUNS = 100;
//		pi.BURNIN = 10;
//		pi.alpha_1 = 0.1;
//		pi.MIN_DICT_WORDS = 1;
//		pi.BURNIN_DOCUMENTS=10;
//		pi.SAMPLE_ALPHA=1;
//		pi.rhokappa=pi.rhokappa_document = 0.5;
//
//
//		pi.initialise();
//		pi.run();
//
//	}
//	public static void food25() {
//
//
//		HMDP_PCSVB pi = new HMDP_PCSVB();
//
//		pi.directory = "food3";
//		pi.save_prefix = "topic25b";
//		pi.T=25;
//		pi.TRAINING_SHARE = 1.0;
//		pi.delta_fix = 10;
//		pi.BATCHSIZE = 128;
//		pi.BATCHSIZE_GROUPS = 128;
//		pi.RUNS = 200;
//		pi.BURNIN = pi.BURNIN_DOCUMENTS=0;
//		pi.SAMPLE_ALPHA=1;
//		pi.rhokappa=pi.rhokappa_document = 0.5;
//
//		//new parameters:
//		pi.directory = "food3";
//		pi.save_prefix = "topic25c";
//		pi.T=15;
//		pi.TRAINING_SHARE = 1.0;
//		pi.delta_fix = 10;
//		pi.BATCHSIZE = 8;
//		pi.BATCHSIZE_GROUPS = 8;
//		pi.RUNS = 200;
//		pi.INIT_RAND=0;
//		pi.BURNIN = pi.BURNIN_DOCUMENTS=0;
//		pi.SAMPLE_ALPHA=1;
//		pi.rhokappa=pi.rhokappa_document = 0.5;
//
//
//		pi.initialise();
//		pi.run();
//
//	}
//
//	public static void pornT(int T) {
//
//
//		HMDP_PCSVB pi = new HMDP_PCSVB();
//
//		pi.directory = "porn_full3";
//		pi.T=T;
//		pi.save_prefix = "faster_"+pi.T+"_";
//		pi.MIN_DICT_WORDS = 100;
//		pi.TRAINING_SHARE = 1;
//		pi.delta_fix = 10;
//		pi.BATCHSIZE = 69;
//		pi.BATCHSIZE_GROUPS = 69;
//		//pi.gamma = 10;
//		pi.alpha_0 = 0.1;
//		pi.beta_0 = 0.01;
//		pi.RUNS = 200;
//		pi.SAMPLE_ALPHA=1;
//		pi.INIT_RAND=1;
//		pi.BURNIN = pi.BURNIN_DOCUMENTS = 10;
//		pi.rhokappa=pi.rhokappa_document=pi.rhokappa_group=0.5;
//		//pi.rhos=10;
//		//pi.rhos_document=1;
//		//pi.rhos_group = 10;
//		//pi.rhotau = 1000;
//		//pi.rhotau_document = 10;
//		//pi.rhotau_group = 1000;
//
//
//		pi.initialise();
//		pi.run();
//
//	}
//
//
//
//	public static void lkml50() {
//		Thread thread = new Thread() {	    public void run() {
//
//			HMDP_PCSVB pi = new HMDP_PCSVB();
//
//			pi.directory = "test5";
//			pi.save_prefix = "lkml50c";
//			pi.T=50;
//			pi.TRAINING_SHARE = 1;
//			pi.delta_fix = 10;
//			pi.BATCHSIZE = 4096;
//			pi.BATCHSIZE_GROUPS = 4096;
//			pi.RUNS = 200;
//			pi.INIT_RAND=1;
//			pi.BURNIN = pi.BURNIN_DOCUMENTS=10;
//			pi.SAMPLE_ALPHA = 1000;
//			pi.rhokappa=pi.rhokappa_document = 0.5;
//
//
//			pi.initialise();
//			pi.run();
//		}};
//
//		thread.start();
//	}
//
//	public static void food8ppx() {
//		Thread thread = new Thread() {	    public void run() {
//
//
//			//int[] batchsizes = {4096,2048,1024,512,256,128,64,32,16,8,4,2,1};
//			int[] batchsizes = {8};
//			Text ppx_file = new Text();
//
//
//			//ppx_file.write(ppxfilename , "", false);
//
//			for (int bs : batchsizes) {
//
//				HMDP_PCSVB pi = new HMDP_PCSVB();
//
//				pi.directory = "food3";
//				pi.save_prefix = "";
//				pi.T=8;
//				pi.TRAINING_SHARE = 0.8;
//				pi.delta_fix = 10;
//				pi.BATCHSIZE = 4096;
//				pi.BATCHSIZE_GROUPS = 16;
//				pi.RUNS = 200;
//				pi.BURNIN = 0;
//				pi.alpha_1 = 0.1;
//				pi.MIN_DICT_WORDS = 1;
//				pi.BURNIN_DOCUMENTS=0;
//				pi.SAMPLE_ALPHA=1;
//				pi.rhokappa=pi.rhokappa_document = 0.5;
//				pi.BATCHSIZE = bs;
//				pi.BATCHSIZE_GROUPS = bs;
//				String ppxfilename = "/home/c/ppx_test2_"+pi.directory+"_"+pi.T;
//
//
//				pi.checkParameters();
//
//				System.out.println("Reading dictionary...");
//				pi.readDict();		
//
//				System.out.println("Initialising parameters...");
//				pi.initParameters();
//
//				System.out.println("Processing documents...");
//
//				pi.readDocs();
//
//				System.out.println("Estimating topics...");
//
//				for (int i=0;i<pi.RUNS;i++) {
//
//					System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);
//
//					ppx_file.writeLine(ppxfilename, bs + " " + i + " " + pi.perplexity()+ " "+ ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime(), true);
//
//					pi.rhot_step++;
//					//get step size
//					pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
//					pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);
//
//					int progress = pi.M / 50;
//					if (progress==0) progress = 1;
//					for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
//						if(m%progress == 0) {
//							System.out.print(".");
//						}
//
//						pi.inferenceDoc(m);
//					}
//					System.out.println();
//
//					pi.updateHyperParameters();
//
//
//					if (pi.rhot_step%pi.SAVE_STEP==0) {
//						//store inferred variables
//						System.out.println("Storing variables...");
//						pi.save();
//					}
//
//				}
//
//				double perplexity = pi.perplexity();
//
//				System.out.println("Perplexity: " + perplexity);
//
//			}
//
//		}};
//
//		thread.start();
//	}
//
//
//	public static void food8learnppx() {
//		Thread thread = new Thread() {	    public void run() {
//
//			//wait till we have optimal batch size
//			double[] learningrate = {0.9,0.5};
//			//int[] batchsizes = {8};
//			Text ppx_file = new Text();
//
//			HMDP_PCSVB pi = new HMDP_PCSVB();
//
//			pi.directory = "food3";
//			pi.save_prefix = "food8learn";
//			pi.T=8;
//			pi.TRAINING_SHARE = 0.8;
//			pi.delta_fix = 10;
//			pi.BATCHSIZE = 25;
//			pi.BATCHSIZE_GROUPS = 8;
//			pi.RUNS = 200;
//			pi.BURNIN = pi.BURNIN_DOCUMENTS=0;
//			pi.rhokappa=pi.rhokappa_document = 0.5;
//
//			String ppxfilename = "/home/c/ppx_lr_"+pi.directory+"_"+pi.T;
//			ppx_file.write(ppxfilename , "", false);
//
//			for (double lr : learningrate) {
//				pi.rhokappa=pi.rhokappa_document = lr;
//
//
//				pi.checkParameters();
//
//				System.out.println("Reading dictionary...");
//				pi.readDict();		
//
//				System.out.println("Initialising parameters...");
//				pi.initParameters();
//
//				System.out.println("Processing documents...");
//
//				pi.readDocs();
//
//				System.out.println("Estimating topics...");
//
//				for (int i=0;i<pi.RUNS;i++) {
//
//					System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);
//
//					ppx_file.writeLine(ppxfilename, lr + " " + i + " " + pi.perplexity(), true);
//
//					pi.rhot_step++;
//					//get step size
//					pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
//					pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);
//
//					int progress = pi.M / 50;
//					if (progress==0) progress = 1;
//					for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
//						if(m%progress == 0) {
//							System.out.print(".");
//						}
//
//						pi.inferenceDoc(m);
//					}
//					System.out.println();
//
//					pi.updateHyperParameters();
//
//
//					if (pi.rhot_step%pi.SAVE_STEP==0) {
//						//store inferred variables
//						System.out.println("Storing variables...");
//						pi.save();
//					}
//
//				}
//
//				double perplexity = pi.perplexity();
//
//				System.out.println("Perplexity: " + perplexity);
//
//			}
//
//		}};
//
//		thread.start();
//	}
//
//	public static void topicfood() {
//
//		HMDP_PCSVB pi = new HMDP_PCSVB();
//
//		pi.directory = "food2";
//		pi.T=25;
//		pi.TRAINING_SHARE = 1.0;
//		pi.delta_fix = 10;
//		pi.BATCHSIZE = 8;
//		pi.BATCHSIZE_GROUPS = 8;
//		pi.RUNS = 1000;
//		pi.BURNIN = pi.BURNIN_DOCUMENTS=0;
//		pi.rhokappa=pi.rhokappa_document = 0.5;
//
//
//		pi.initialise();
//		pi.run();
//	}
//
//	public static void topicporn() {
//		Thread thread = new Thread() {	    public void run() {
//
//			HMDP_PCSVB pi = new HMDP_PCSVB();
//
//			pi.directory = "porn_full3";
//			pi.save_prefix = "fas";
//			pi.T=25;
//			pi.MIN_DICT_WORDS = 100;
//			pi.TRAINING_SHARE = 0.8;
//			pi.delta_fix = 0;
//			pi.BATCHSIZE = 4;
//			pi.BATCHSIZE_GROUPS = 4;
//			//pi.gamma = 10;
//			pi.beta_0 = 0.1;
//			pi.RUNS = 200;
//			pi.SAMPLE_ALPHA=1000;
//			pi.BURNIN = 10;
//			pi.BURNIN_DOCUMENTS=10;
//			pi.rhokappa=pi.rhokappa_document = 0.5;
//
//			Text ppx_file = new Text();
//			String ppxfilename = "/home/c/ppx_lr_"+pi.directory+"_"+pi.T;
//			ppx_file.write(ppxfilename , "", false);
//
//			pi.initialise();
//
//
//			for (int i=0;i<pi.RUNS;i++) {
//
//				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);
//
//				ppx_file.writeLine(ppxfilename, pi.BATCHSIZE + " " + i + " " + pi.perplexity(), true);
//
//				pi.rhot_step++;
//				//get step size
//				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
//				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);
//
//				int progress = pi.M / 50;
//				if (progress==0) progress = 1;
//				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
//					if(m%progress == 0) {
//						System.out.print(".");
//					}
//
//					pi.inferenceDoc(m);
//				}
//				System.out.println();
//
//				pi.updateHyperParameters();
//
//
//				if (pi.rhot_step%pi.SAVE_STEP==0) {
//					//store inferred variables
//					System.out.println("Storing variables...");
//					pi.save();
//				}
//
//			}
//
//			double perplexity = pi.perplexity();
//
//			System.out.println("Perplexity: " + perplexity);
//
//
//		}};
//
//		thread.start();
//
//	}
//
//
//	public static void test5() {
//
//		HMDP_PCSVB pi = new HMDP_PCSVB();
//
//		pi.directory = "test5";
//		pi.T=50;
//		pi.TRAINING_SHARE = 1.0;
//		pi.delta_fix = 10;
//		pi.BATCHSIZE = 4096;
//		pi.BURNIN = 0;
//		pi.BURNIN_DOCUMENTS = 0;
//		pi.rhokappa=pi.rhokappa_document = 0.5;
//		pi.RUNS = 200;
//
//		pi.initialise();
//		pi.run();
//	}
//
//
//
//	public static void kappaporn() {
//
//		double[] kappas = {1.0/16,1.0/8,1.0/4,1.0/2};
//		//int[] batchsizes = {8};
//		Text ppx_file = new Text();
//
//		for (double kappa : kappas) {
//
//			HMDP_PCSVB pi = new HMDP_PCSVB();
//
//			pi.BATCHSIZE=4096;
//			pi.directory = "porn_full3";
//			pi.T=25;
//			pi.TRAINING_SHARE = 0.8;
//			pi.delta_fix = 0;
//			pi.RUNS = 200;
//
//
//
//			ppx_file.write("/home/c/kappa_test_"+pi.directory+"_"+pi.BATCHSIZE+"_"+pi.T , "", false);
//
//			pi.rhokappa=pi.rhokappa_document=kappa;
//
//
//			pi.checkParameters();
//
//			System.out.println("Reading dictionary...");
//			pi.readDict();		
//
//			System.out.println("Initialising parameters...");
//			pi.initParameters();
//
//			System.out.println("Processing documents...");
//
//			pi.readDocs();
//
//			System.out.println("Estimating topics...");
//
//			for (int i=0;i<pi.RUNS;i++) {
//
//				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);
//
//				if (i > pi.BURNIN)
//					ppx_file.writeLine("/home/c/ppx_test", kappa + " " + i + " " + pi.perplexity(), true);
//
//				pi.rhot_step++;
//				//get step size
//				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
//				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);
//
//				int progress = pi.M / 50;
//				if (progress==0) progress = 1;
//				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
//					if(m%progress == 0) {
//						System.out.print(".");
//					}
//
//					pi.inferenceDoc(m);
//				}
//				System.out.println();
//
//				pi.updateHyperParameters();
//
//
//				if (pi.rhot_step%pi.SAVE_STEP==0) {
//					//store inferred variables
//					System.out.println("Storing variables...");
//					pi.save();
//				}
//
//			}
//
//			double perplexity = pi.perplexity();
//
//			System.out.println("Perplexity: " + perplexity);
//
//		}
//	}
//
//	public static void burninfood() {
//
//		//int[] batchsizes = {8};
//		Text ppx_file = new Text();
//
//		int[] burnins = {64,32,16,8};
//		for (int burnin : burnins) {
//
//			HMDP_PCSVB pi = new HMDP_PCSVB();
//
//			pi.BURNIN=0;
//			pi.BURNIN_DOCUMENTS=0;
//			pi.BATCHSIZE=burnin;
//			pi.directory = "food2";
//			pi.T=25;
//			pi.beta_0=0.1;
//			//pi.alpha_1=1;
//			//pi.alpha_0=1;
//			//pi.gamma=10;
//			pi.MIN_DICT_WORDS=1;
//			pi.TRAINING_SHARE = 0.8;
//			pi.delta_fix = 10;
//			pi.RUNS = 200;
//			pi.rhokappa=pi.rhokappa_document=0.5;
//
//			pi.rhokappa_group = 0.5;
//
//			String dest = "/home/c/batch_test_"+pi.directory+"_"+pi.BATCHSIZE+"_"+pi.T;
//
//			ppx_file.write(dest , "", false);
//
//
//
//			pi.checkParameters();
//
//			System.out.println("Reading dictionary...");
//			pi.readDict();		
//
//			System.out.println("Initialising parameters...");
//			pi.initParameters();
//
//			System.out.println("Processing documents...");
//
//			pi.readDocs();
//
//			System.out.println("Estimating topics...");
//
//			for (int i=0;i<pi.RUNS;i++) {
//
//				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);
//
//				double ppx=pi.perplexity();
//				ppx_file.writeLine(dest, burnin + " " + i + " " + ppx, true);
//
//				System.out.println(ppx);
//
//				pi.rhot_step++;
//				//get step size
//				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
//				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);
//
//				int progress = pi.M / 50;
//				if (progress==0) progress = 1;
//				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
//					if(m%progress == 0) {
//						System.out.print(".");
//					}
//
//					pi.inferenceDoc(m);
//				}
//				System.out.println();
//
//				pi.updateHyperParameters();
//
//				if (pi.rhot_step%pi.SAVE_STEP==0) {
//					//store inferred variables
//					System.out.println("Storing variables...");
//					pi.save();
//				}
//
//			}
//
//			double perplexity = pi.perplexity();
//
//			System.out.println("Perplexity: " + perplexity);
//
//		}
//	}
//
//	public static void burninporn() {
//
//		//int[] batchsizes = {8};
//		Text ppx_file = new Text();
//
//		int[] burnins = {4,8,16,32,64};
//		for (int burnin : burnins) {
//
//			HMDP_PCSVB pi = new HMDP_PCSVB();
//
//			pi.BURNIN=10;
//			pi.BURNIN_DOCUMENTS=1;
//			pi.BATCHSIZE=burnin;
//			pi.directory = "porn_full3";
//			pi.T=25;
//			pi.TRAINING_SHARE = 0.8;
//			pi.delta_fix = 10;
//			pi.RUNS = 200;
//			pi.rhokappa=pi.rhokappa_document=0.5;
//
//
//			String dest = "/home/c/burnin_test_"+pi.directory+"_"+pi.BATCHSIZE+"_"+pi.T;
//
//			ppx_file.write(dest , "", false);
//
//
//
//			pi.checkParameters();
//
//			System.out.println("Reading dictionary...");
//			pi.readDict();		
//
//			System.out.println("Initialising parameters...");
//			pi.initParameters();
//
//			System.out.println("Processing documents...");
//
//			pi.readDocs();
//
//			System.out.println("Estimating topics...");
//
//			for (int i=0;i<pi.RUNS;i++) {
//
//				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);
//
//				ppx_file.writeLine(dest, burnin + " " + i + " " + pi.perplexity(), true);
//
//				pi.rhot_step++;
//				//get step size
//				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
//				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);
//
//				int progress = pi.M / 50;
//				if (progress==0) progress = 1;
//				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
//					if(m%progress == 0) {
//						System.out.print(".");
//					}
//
//					pi.inferenceDoc(m);
//				}
//				System.out.println();
//
//				pi.updateHyperParameters();
//
//				if (pi.rhot_step%pi.SAVE_STEP==0) {
//					//store inferred variables
//					System.out.println("Storing variables...");
//					pi.save();
//				}
//
//			}
//
//			double perplexity = pi.perplexity();
//
//			System.out.println("Perplexity: " + perplexity);
//
//		}
//	}
//
//
//
//	public static void ppxpornNolink(String ds, int K, int bs) {
//
//
//		Text ppx_file = new Text();
//
//
//
//
//		HMDP_PCSVB pi = new HMDP_PCSVB();
//
//		pi.directory = ds;
//		pi.save_prefix = "nl";
//		pi.T=K;
//		pi.MIN_DICT_WORDS = 100;
//		pi.TRAINING_SHARE = 0.8;
//		pi.delta_fix = 0;
//		pi.BATCHSIZE = bs;
//		pi.BATCHSIZE_GROUPS = bs;
//		//pi.gamma = 10;
//		pi.beta_0 = 0.1;
//		pi.RUNS = 200;
//		pi.SAMPLE_ALPHA=1000;
//		pi.BURNIN = 0;
//		pi.BURNIN_DOCUMENTS=0;
//		pi.rhokappa=pi.rhokappa_document = 0.5;
//
//
//
//		//ppx_file.write("/home/c/ppx_test_"+pi.dataset+"_"+pi.T , "", false);
//
//
//		pi.checkParameters();
//
//		System.out.println("Reading dictionary...");
//		pi.readDict();		
//
//		System.out.println("Initialising parameters...");
//		pi.initParameters();
//
//		System.out.println("Processing documents...");
//
//		pi.readDocs();
//
//		System.out.println("Estimating topics...");
//
//		for (int i=0;i<pi.RUNS;i++) {
//
//			System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);
//
//			if (i > pi.BURNIN)
//				ppx_file.writeLine("/home/c/ppx_testnl", pi.directory + " " + pi.T + " " + i + " " + pi.perplexity(), true);
//
//			pi.rhot_step++;
//			//get step size
//			pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
//			pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);
//
//			int progress = pi.M / 50;
//			if (progress==0) progress = 1;
//			for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
//				if(m%progress == 0) {
//					System.out.print(".");
//				}
//
//				pi.inferenceDoc(m);
//			}
//			System.out.println();
//
//			pi.updateHyperParameters();
//
//
//			if (pi.rhot_step%pi.SAVE_STEP==0) {
//				//store inferred variables
//				System.out.println("Storing variables...");
//				pi.save();
//			}
//
//		}
//
//		double perplexity = pi.perplexity();
//
//		System.out.println("Perplexity: " + perplexity);
//
//
//	}
//
//
//	public static void ppxporn3() {
//
//		int[] batchsizes = {4096,2048,1024,512,256,128,64,32,16,8,4,2,1};
//		//int[] batchsizes = {8};
//		Text ppx_file = new Text();
//
//		for (int bs : batchsizes) {
//
//			HMDP_PCSVB pi = new HMDP_PCSVB();
//
//			pi.directory = "porn_full3";
//			pi.T=25;
//			pi.TRAINING_SHARE = 0.8;
//			pi.delta_fix = 0;
//
//
//
//			ppx_file.write("/home/c/ppx_test_"+pi.directory+"_"+pi.T , "", false);
//
//			pi.BATCHSIZE = bs;
//			pi.BATCHSIZE_GROUPS = bs;
//
//
//			pi.checkParameters();
//
//			System.out.println("Reading dictionary...");
//			pi.readDict();		
//
//			System.out.println("Initialising parameters...");
//			pi.initParameters();
//
//			System.out.println("Processing documents...");
//
//			pi.readDocs();
//
//			System.out.println("Estimating topics...");
//
//			for (int i=0;i<pi.RUNS;i++) {
//
//				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);
//
//				if (i > pi.BURNIN)
//					ppx_file.writeLine("/home/c/ppx_test", bs + " " + i + " " + pi.perplexity(), true);
//
//				pi.rhot_step++;
//				//get step size
//				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
//				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);
//
//				int progress = pi.M / 50;
//				if (progress==0) progress = 1;
//				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
//					if(m%progress == 0) {
//						System.out.print(".");
//					}
//
//					pi.inferenceDoc(m);
//				}
//				System.out.println();
//
//				pi.updateHyperParameters();
//
//
//				if (pi.rhot_step%pi.SAVE_STEP==0) {
//					//store inferred variables
//					System.out.println("Storing variables...");
//					pi.save();
//				}
//
//			}
//
//			double perplexity = pi.perplexity();
//
//			System.out.println("Perplexity: " + perplexity);
//
//		}
//	}





