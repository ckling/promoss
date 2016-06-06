package ckling.inference;

import java.lang.management.ManagementFactory;

import ckling.text.Text;

public class Start {


	public static void main (String[] args)  {

			
		
		food8();

		//food25();

		//food8ppx();

		//porn25();
		
		//multi();
		
		//lkml50();
		//ppxporn3();
		//topicporn();
		//kappaporn();
		//burninporn();
		//test5();
		//burninfood();
		//topicfood();
		
		//pornT(Integer.valueOf(args[0]));
		//pornT(Integer.valueOf(50));
		
		
		//ppxpornNolink(args[0],Integer.valueOf(args[1]),Integer.valueOf(args[2]));
		
		//ppxpornNolink("p1",100,8);
		//polseb();
	
	}
	
	
	
	public static void startHMDP(String[] args) {
		HMDP model = new HMDP();
		
		for (int i=0; i < args.length; i++) {
			
			
			if (args[i].equals("-dir")) 
				model.basedirectory = args[++i];
			
			if (args[i].equals("-K")) 
				model.K = Integer.valueOf(args[++i]);
						
			if (args[i].equals("-twords")) 
				model.topk = Integer.valueOf(args[++i]);
					
			if (args[i].equals("-savestep")) 
				savestep = Integer.valueOf(args[++i]);
			
			if (args[i].equals("-delta")) 
				delta = Double.valueOf(args[++i]);
			
			if (args[i].equals("-niters")) 
				niters = Integer.valueOf(args[++i]);
			
			if (args[i].equals("-alpha")) 
				alpha = Double.valueOf(args[++i]);
			
			if (args[i].equals("-beta")) 
				beta = Double.valueOf(args[++i]);
									
			if (args[i].equals("-zeta")) 
				zeta = Double.valueOf(args[++i]);
						
			if (args[i].equals("-gammaa")) 
				gammaa = Double.valueOf(args[++i]);
			
			if (args[i].equals("-gammab")) 
				gammab = Double.valueOf(args[++i]);
			
			if (args[i].equals("-alpha0a")) 
				alpha0a = Double.valueOf(args[++i]);

			if (args[i].equals("-alpha0b")) 
				alpha0b = Double.valueOf(args[++i]);

			if (args[i].equals("-Alphaa")) 
				Alphaa = Double.valueOf(args[++i]);
			
			if (args[i].equals("-Alphab")) 
				Alphab = Double.valueOf(args[++i]);
			
			if (args[i].equals("-betaa")) 
				betaa = Double.valueOf(args[++i]);
			
			if (args[i].equals("-betab")) 
				betab = Double.valueOf(args[++i]);
			
			if (args[i].equals("-sampleHyper")) 
				sampleHyper = Boolean.valueOf(args[++i]);
			
			if (args[i].equals("-runs")) 
				runs = Integer.valueOf(args[++i]);
			
			if (args[i].equals("-alpha0")) 
				alpha0 = Double.valueOf(args[++i]);
			
			if (args[i].equals("-Alpha")) 
				Alpha = Double.valueOf(args[++i]);
			
			if (args[i].equals("-gamma")) 
				gamma = Double.valueOf(args[++i]);
			
		}
	}
	
	

	public static void polseb() {

		HMDP pi = new HMDP();

		pi.dataset = "polseb";
		pi.save_prefix = "100";
		pi.K=100;
		pi.TRAINING_SHARE = 1.0;
		pi.delta_fix = 10;
		pi.BATCHSIZE = 128;
		pi.BATCHSIZE_GROUPS = 128;
		pi.RUNS = 100;
		pi.BURNIN = 0;
		pi.alpha_1 = 0.1;
		pi.MIN_DICT_WORDS = 100;
		pi.INIT_RAND = 1;
		pi.BURNIN_DOCUMENTS=1;
		pi.SAMPLE_ALPHA=1;
		pi.rhokappa=pi.rhokappa_document = 0.5;


		pi.initialise();
		pi.run();

}
	
	public static void food8() {

			HMDP pi = new HMDP();

			pi.dataset = "food3";
			pi.save_prefix = "topic8b";
			pi.K=8;
			pi.TRAINING_SHARE = 1.0;
			pi.delta_fix = 10;
			pi.BATCHSIZE = 64;
			pi.BATCHSIZE_GROUPS = 64;
			pi.RUNS = 100;
			pi.BURNIN = 10;
			pi.alpha_1 = 0.1;
			pi.MIN_DICT_WORDS = 1;
			pi.BURNIN_DOCUMENTS=10;
			pi.SAMPLE_ALPHA=1;
			pi.rhokappa=pi.rhokappa_document = 0.5;


			pi.initialise();
			pi.run();

	}
	public static void food25() {


			HMDP pi = new HMDP();

			pi.dataset = "food3";
			pi.save_prefix = "topic25b";
			pi.K=25;
			pi.TRAINING_SHARE = 1.0;
			pi.delta_fix = 10;
			pi.BATCHSIZE = 128;
			pi.BATCHSIZE_GROUPS = 128;
			pi.RUNS = 200;
			pi.BURNIN = pi.BURNIN_DOCUMENTS=0;
			pi.SAMPLE_ALPHA=1;
			pi.rhokappa=pi.rhokappa_document = 0.5;
			
			//new parameters:
			pi.dataset = "food3";
			pi.save_prefix = "topic25c";
			pi.K=15;
			pi.TRAINING_SHARE = 1.0;
			pi.delta_fix = 10;
			pi.BATCHSIZE = 8;
			pi.BATCHSIZE_GROUPS = 8;
			pi.RUNS = 200;
			pi.INIT_RAND=0;
			pi.BURNIN = pi.BURNIN_DOCUMENTS=0;
			pi.SAMPLE_ALPHA=1;
			pi.rhokappa=pi.rhokappa_document = 0.5;


			pi.initialise();
			pi.run();

	}
		
	public static void pornT(int T) {
		

			HMDP pi = new HMDP();

			pi.dataset = "porn_full3";
			pi.K=T;
			pi.save_prefix = "faster_"+pi.K+"_";
			pi.MIN_DICT_WORDS = 100;
			pi.TRAINING_SHARE = 1;
			pi.delta_fix = 10;
			pi.BATCHSIZE = 69;
			pi.BATCHSIZE_GROUPS = 69;
			//pi.gamma = 10;
			pi.alpha_0 = 0.1;
			pi.beta_0 = 0.01;
			pi.RUNS = 200;
			pi.SAMPLE_ALPHA=1;
			pi.INIT_RAND=1;
			pi.BURNIN = pi.BURNIN_DOCUMENTS = 10;
			pi.rhokappa=pi.rhokappa_document=pi.rhokappa_group=0.5;
			//pi.rhos=10;
			//pi.rhos_document=1;
			//pi.rhos_group = 10;
			//pi.rhotau = 1000;
			//pi.rhotau_document = 10;
			//pi.rhotau_group = 1000;

			
			pi.initialise();
			pi.run();

	}


	
	public static void lkml50() {
		Thread thread = new Thread() {	    public void run() {

			HMDP pi = new HMDP();

			pi.dataset = "test5";
			pi.save_prefix = "lkml50c";
			pi.K=50;
			pi.TRAINING_SHARE = 1;
			pi.delta_fix = 10;
			pi.BATCHSIZE = 4096;
			pi.BATCHSIZE_GROUPS = 4096;
			pi.RUNS = 200;
			pi.INIT_RAND=1;
			pi.BURNIN = pi.BURNIN_DOCUMENTS=10;
			pi.SAMPLE_ALPHA = 1000;
			pi.rhokappa=pi.rhokappa_document = 0.5;


			pi.initialise();
			pi.run();
		}};

		thread.start();
	}
	
	public static void food8ppx() {
		Thread thread = new Thread() {	    public void run() {
			
			
			//int[] batchsizes = {4096,2048,1024,512,256,128,64,32,16,8,4,2,1};
			int[] batchsizes = {8};
			Text ppx_file = new Text();

			
			//ppx_file.write(ppxfilename , "", false);
			
			for (int bs : batchsizes) {
				
				HMDP pi = new HMDP();
				
				pi.dataset = "food3";
				pi.save_prefix = "";
				pi.K=8;
				pi.TRAINING_SHARE = 0.8;
				pi.delta_fix = 10;
				pi.BATCHSIZE = 4096;
				pi.BATCHSIZE_GROUPS = 16;
				pi.RUNS = 200;
				pi.BURNIN = 0;
				pi.alpha_1 = 0.1;
				pi.MIN_DICT_WORDS = 1;
				pi.BURNIN_DOCUMENTS=0;
				pi.SAMPLE_ALPHA=1;
				pi.rhokappa=pi.rhokappa_document = 0.5;
			pi.BATCHSIZE = bs;
			pi.BATCHSIZE_GROUPS = bs;
			String ppxfilename = "/home/c/ppx_test2_"+pi.dataset+"_"+pi.K;

			
			pi.readSettings();

			System.out.println("Reading dictionary...");
			pi.readDict();		

			System.out.println("Initialising parameters...");
			pi.getParameters();

			System.out.println("Processing documents...");

			pi.readDocs();

			System.out.println("Estimating topics...");

			for (int i=0;i<pi.RUNS;i++) {

				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);

				ppx_file.writeLine(ppxfilename, bs + " " + i + " " + pi.perplexity()+ " "+ ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime(), true);
				
				pi.rhot_step++;
				//get step size
				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

				int progress = pi.M / 50;
				if (progress==0) progress = 1;
				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
					if(m%progress == 0) {
						System.out.print(".");
					}

					pi.inferenceDoc(m);
				}
				System.out.println();

				pi.updateHyperParameters();


				if (pi.rhot_step%pi.SAVE_STEP==0) {
					//store inferred variables
					System.out.println("Storing variables...");
					pi.save();
				}

			}

			double perplexity = pi.perplexity();

			System.out.println("Perplexity: " + perplexity);

			}

		}};

		thread.start();
	}
	

	public static void food8learnppx() {
		Thread thread = new Thread() {	    public void run() {
			
			//wait till we have optimal batch size
			double[] learningrate = {0.9,0.5};
			//int[] batchsizes = {8};
			Text ppx_file = new Text();
			
			HMDP pi = new HMDP();
			
			pi.dataset = "food3";
			pi.save_prefix = "food8learn";
			pi.K=8;
			pi.TRAINING_SHARE = 0.8;
			pi.delta_fix = 10;
			pi.BATCHSIZE = 25;
			pi.BATCHSIZE_GROUPS = 8;
			pi.RUNS = 200;
			pi.BURNIN = pi.BURNIN_DOCUMENTS=0;
			pi.rhokappa=pi.rhokappa_document = 0.5;
			
			String ppxfilename = "/home/c/ppx_lr_"+pi.dataset+"_"+pi.K;
			ppx_file.write(ppxfilename , "", false);
			
			for (double lr : learningrate) {
				pi.rhokappa=pi.rhokappa_document = lr;
			
			
			pi.readSettings();

			System.out.println("Reading dictionary...");
			pi.readDict();		

			System.out.println("Initialising parameters...");
			pi.getParameters();

			System.out.println("Processing documents...");

			pi.readDocs();

			System.out.println("Estimating topics...");

			for (int i=0;i<pi.RUNS;i++) {

				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);

				ppx_file.writeLine(ppxfilename, lr + " " + i + " " + pi.perplexity(), true);
				
				pi.rhot_step++;
				//get step size
				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

				int progress = pi.M / 50;
				if (progress==0) progress = 1;
				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
					if(m%progress == 0) {
						System.out.print(".");
					}

					pi.inferenceDoc(m);
				}
				System.out.println();

				pi.updateHyperParameters();


				if (pi.rhot_step%pi.SAVE_STEP==0) {
					//store inferred variables
					System.out.println("Storing variables...");
					pi.save();
				}

			}

			double perplexity = pi.perplexity();

			System.out.println("Perplexity: " + perplexity);

			}

		}};

		thread.start();
	}

	public static void topicfood() {

		HMDP pi = new HMDP();

		pi.dataset = "food2";
		pi.K=25;
		pi.TRAINING_SHARE = 1.0;
		pi.delta_fix = 10;
		pi.BATCHSIZE = 8;
		pi.BATCHSIZE_GROUPS = 8;
		pi.RUNS = 1000;
		pi.BURNIN = pi.BURNIN_DOCUMENTS=0;
		pi.rhokappa=pi.rhokappa_document = 0.5;


		pi.initialise();
		pi.run();
	}

	public static void topicporn() {
		Thread thread = new Thread() {	    public void run() {

		HMDP pi = new HMDP();

		pi.dataset = "porn_full3";
		pi.save_prefix = "fas";
		pi.K=25;
		pi.MIN_DICT_WORDS = 100;
		pi.TRAINING_SHARE = 0.8;
		pi.delta_fix = 0;
		pi.BATCHSIZE = 4;
		pi.BATCHSIZE_GROUPS = 4;
		//pi.gamma = 10;
		pi.beta_0 = 0.1;
		pi.RUNS = 200;
		pi.SAMPLE_ALPHA=1000;
		pi.BURNIN = 10;
		pi.BURNIN_DOCUMENTS=10;
		pi.rhokappa=pi.rhokappa_document = 0.5;
		
		Text ppx_file = new Text();
		String ppxfilename = "/home/c/ppx_lr_"+pi.dataset+"_"+pi.K;
		ppx_file.write(ppxfilename , "", false);

		pi.initialise();
		

		for (int i=0;i<pi.RUNS;i++) {

			System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);

			ppx_file.writeLine(ppxfilename, pi.BATCHSIZE + " " + i + " " + pi.perplexity(), true);
			
			pi.rhot_step++;
			//get step size
			pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
			pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

			int progress = pi.M / 50;
			if (progress==0) progress = 1;
			for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
				if(m%progress == 0) {
					System.out.print(".");
				}

				pi.inferenceDoc(m);
			}
			System.out.println();

			pi.updateHyperParameters();


			if (pi.rhot_step%pi.SAVE_STEP==0) {
				//store inferred variables
				System.out.println("Storing variables...");
				pi.save();
			}

		}

		double perplexity = pi.perplexity();

		System.out.println("Perplexity: " + perplexity);

		
		}};

		thread.start();
	
	}


	public static void test5() {

		HMDP pi = new HMDP();

		pi.dataset = "test5";
		pi.K=50;
		pi.TRAINING_SHARE = 1.0;
		pi.delta_fix = 10;
		pi.BATCHSIZE = 4096;
		pi.BURNIN = 0;
		pi.BURNIN_DOCUMENTS = 0;
		pi.rhokappa=pi.rhokappa_document = 0.5;
		pi.RUNS = 200;

		pi.initialise();
		pi.run();
	}



	public static void kappaporn() {

		double[] kappas = {1.0/16,1.0/8,1.0/4,1.0/2};
		//int[] batchsizes = {8};
		Text ppx_file = new Text();

		for (double kappa : kappas) {

			HMDP pi = new HMDP();

			pi.BATCHSIZE=4096;
			pi.dataset = "porn_full3";
			pi.K=25;
			pi.TRAINING_SHARE = 0.8;
			pi.delta_fix = 0;
			pi.RUNS = 200;



			ppx_file.write("/home/c/kappa_test_"+pi.dataset+"_"+pi.BATCHSIZE+"_"+pi.K , "", false);

			pi.rhokappa=pi.rhokappa_document=kappa;


			pi.readSettings();

			System.out.println("Reading dictionary...");
			pi.readDict();		

			System.out.println("Initialising parameters...");
			pi.getParameters();

			System.out.println("Processing documents...");

			pi.readDocs();

			System.out.println("Estimating topics...");

			for (int i=0;i<pi.RUNS;i++) {

				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);

				if (i > pi.BURNIN)
					ppx_file.writeLine("/home/c/ppx_test", kappa + " " + i + " " + pi.perplexity(), true);

				pi.rhot_step++;
				//get step size
				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

				int progress = pi.M / 50;
				if (progress==0) progress = 1;
				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
					if(m%progress == 0) {
						System.out.print(".");
					}

					pi.inferenceDoc(m);
				}
				System.out.println();

				pi.updateHyperParameters();


				if (pi.rhot_step%pi.SAVE_STEP==0) {
					//store inferred variables
					System.out.println("Storing variables...");
					pi.save();
				}

			}

			double perplexity = pi.perplexity();

			System.out.println("Perplexity: " + perplexity);

		}
	}

	public static void burninfood() {

		//int[] batchsizes = {8};
		Text ppx_file = new Text();

		int[] burnins = {64,32,16,8};
		for (int burnin : burnins) {

			HMDP pi = new HMDP();

			pi.BURNIN=0;
			pi.BURNIN_DOCUMENTS=0;
			pi.BATCHSIZE=burnin;
			pi.dataset = "food2";
			pi.K=25;
			pi.beta_0=0.1;
			//pi.alpha_1=1;
			//pi.alpha_0=1;
			//pi.gamma=10;
			pi.MIN_DICT_WORDS=1;
			pi.TRAINING_SHARE = 0.8;
			pi.delta_fix = 10;
			pi.RUNS = 200;
			pi.rhokappa=pi.rhokappa_document=0.5;
			
			pi.rhokappa_group = 0.5;

			String dest = "/home/c/batch_test_"+pi.dataset+"_"+pi.BATCHSIZE+"_"+pi.K;

			ppx_file.write(dest , "", false);



			pi.readSettings();

			System.out.println("Reading dictionary...");
			pi.readDict();		

			System.out.println("Initialising parameters...");
			pi.getParameters();

			System.out.println("Processing documents...");

			pi.readDocs();

			System.out.println("Estimating topics...");

			for (int i=0;i<pi.RUNS;i++) {

				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);

				double ppx=pi.perplexity();
				ppx_file.writeLine(dest, burnin + " " + i + " " + ppx, true);

				System.out.println(ppx);

				pi.rhot_step++;
				//get step size
				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

				int progress = pi.M / 50;
				if (progress==0) progress = 1;
				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
					if(m%progress == 0) {
						System.out.print(".");
					}

					pi.inferenceDoc(m);
				}
				System.out.println();

				pi.updateHyperParameters();

				if (pi.rhot_step%pi.SAVE_STEP==0) {
					//store inferred variables
					System.out.println("Storing variables...");
					pi.save();
				}

			}

			double perplexity = pi.perplexity();

			System.out.println("Perplexity: " + perplexity);

		}
	}

	public static void burninporn() {

		//int[] batchsizes = {8};
		Text ppx_file = new Text();

		int[] burnins = {4,8,16,32,64};
		for (int burnin : burnins) {

			HMDP pi = new HMDP();

			pi.BURNIN=10;
			pi.BURNIN_DOCUMENTS=1;
			pi.BATCHSIZE=burnin;
			pi.dataset = "porn_full3";
			pi.K=25;
			pi.TRAINING_SHARE = 0.8;
			pi.delta_fix = 10;
			pi.RUNS = 200;
			pi.rhokappa=pi.rhokappa_document=0.5;


			String dest = "/home/c/burnin_test_"+pi.dataset+"_"+pi.BATCHSIZE+"_"+pi.K;

			ppx_file.write(dest , "", false);



			pi.readSettings();

			System.out.println("Reading dictionary...");
			pi.readDict();		

			System.out.println("Initialising parameters...");
			pi.getParameters();

			System.out.println("Processing documents...");

			pi.readDocs();

			System.out.println("Estimating topics...");

			for (int i=0;i<pi.RUNS;i++) {

				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);

				ppx_file.writeLine(dest, burnin + " " + i + " " + pi.perplexity(), true);

				pi.rhot_step++;
				//get step size
				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

				int progress = pi.M / 50;
				if (progress==0) progress = 1;
				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
					if(m%progress == 0) {
						System.out.print(".");
					}

					pi.inferenceDoc(m);
				}
				System.out.println();

				pi.updateHyperParameters();

				if (pi.rhot_step%pi.SAVE_STEP==0) {
					//store inferred variables
					System.out.println("Storing variables...");
					pi.save();
				}

			}

			double perplexity = pi.perplexity();

			System.out.println("Perplexity: " + perplexity);

		}
	}

	

	public static void ppxpornNolink(String ds, int K, int bs) {

		
		Text ppx_file = new Text();


		
		
			HMDP pi = new HMDP();

			pi.dataset = ds;
			pi.save_prefix = "nl";
			pi.K=K;
			pi.MIN_DICT_WORDS = 100;
			pi.TRAINING_SHARE = 0.8;
			pi.delta_fix = 0;
			pi.BATCHSIZE = bs;
			pi.BATCHSIZE_GROUPS = bs;
			//pi.gamma = 10;
			pi.beta_0 = 0.1;
			pi.RUNS = 200;
			pi.SAMPLE_ALPHA=1000;
			pi.BURNIN = 0;
			pi.BURNIN_DOCUMENTS=0;
			pi.rhokappa=pi.rhokappa_document = 0.5;



			//ppx_file.write("/home/c/ppx_test_"+pi.dataset+"_"+pi.T , "", false);


			pi.readSettings();

			System.out.println("Reading dictionary...");
			pi.readDict();		

			System.out.println("Initialising parameters...");
			pi.getParameters();

			System.out.println("Processing documents...");

			pi.readDocs();

			System.out.println("Estimating topics...");

			for (int i=0;i<pi.RUNS;i++) {

				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);

				if (i > pi.BURNIN)
					ppx_file.writeLine("/home/c/ppx_testnl", pi.dataset + " " + pi.K + " " + i + " " + pi.perplexity(), true);

				pi.rhot_step++;
				//get step size
				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

				int progress = pi.M / 50;
				if (progress==0) progress = 1;
				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
					if(m%progress == 0) {
						System.out.print(".");
					}

					pi.inferenceDoc(m);
				}
				System.out.println();

				pi.updateHyperParameters();


				if (pi.rhot_step%pi.SAVE_STEP==0) {
					//store inferred variables
					System.out.println("Storing variables...");
					pi.save();
				}

			}

			double perplexity = pi.perplexity();

			System.out.println("Perplexity: " + perplexity);

		
	}


	public static void ppxporn3() {

		int[] batchsizes = {4096,2048,1024,512,256,128,64,32,16,8,4,2,1};
		//int[] batchsizes = {8};
		Text ppx_file = new Text();

		for (int bs : batchsizes) {

			HMDP pi = new HMDP();

			pi.dataset = "porn_full3";
			pi.K=25;
			pi.TRAINING_SHARE = 0.8;
			pi.delta_fix = 0;



			ppx_file.write("/home/c/ppx_test_"+pi.dataset+"_"+pi.K , "", false);

			pi.BATCHSIZE = bs;
			pi.BATCHSIZE_GROUPS = bs;


			pi.readSettings();

			System.out.println("Reading dictionary...");
			pi.readDict();		

			System.out.println("Initialising parameters...");
			pi.getParameters();

			System.out.println("Processing documents...");

			pi.readDocs();

			System.out.println("Estimating topics...");

			for (int i=0;i<pi.RUNS;i++) {

				System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta[0]+ " epsilon " + pi.epsilon[0]);

				if (i > pi.BURNIN)
					ppx_file.writeLine("/home/c/ppx_test", bs + " " + i + " " + pi.perplexity(), true);

				pi.rhot_step++;
				//get step size
				pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
				pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

				int progress = pi.M / 50;
				if (progress==0) progress = 1;
				for (int m=0;m<Double.valueOf(pi.M)*pi.TRAINING_SHARE;m++) {
					if(m%progress == 0) {
						System.out.print(".");
					}

					pi.inferenceDoc(m);
				}
				System.out.println();

				pi.updateHyperParameters();


				if (pi.rhot_step%pi.SAVE_STEP==0) {
					//store inferred variables
					System.out.println("Storing variables...");
					pi.save();
				}

			}

			double perplexity = pi.perplexity();

			System.out.println("Perplexity: " + perplexity);

		}
	}





}
