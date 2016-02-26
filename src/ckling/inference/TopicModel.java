package ckling.inference;

import ckling.text.Text;

public class TopicModel {


	public static void main (String[] args) {
		//ppxporn3();
		//topicporn();
		//kappaporn();
		burninporn();
	}
	
	
	public static void topicporn() {

		//int[] batchsizes = {8};
		Text ppx_file = new Text();
		
		
		PracticalInference pi = new PracticalInference();
		
		pi.dataset = "porn_full3";
		pi.T=25;
		pi.TRAINING_SHARE = 1.0;
		pi.delta_fix = 0;
		pi.BATCHSIZE = 64;
		
		
		
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


	}
	



	public static void kappaporn() {

		double[] kappas = {1.0/16,1.0/8,1.0/4,1.0/2};
		//int[] batchsizes = {8};
		Text ppx_file = new Text();
		
		for (double kappa : kappas) {
		
		PracticalInference pi = new PracticalInference();
		
		pi.BATCHSIZE=4096;
		pi.dataset = "porn_full3";
		pi.T=25;
		pi.TRAINING_SHARE = 0.8;
		pi.delta_fix = 0;
		pi.RUNS = 200;
		

		
		ppx_file.write("/home/c/kappa_test_"+pi.dataset+"_"+pi.BATCHSIZE+"_"+pi.T , "", false);

		pi.rhokappa=pi.rhokappa_document=pi.rhokappa_hyper=kappa;
		
		
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
	
	public static void burninporn() {

		//int[] batchsizes = {8};
		Text ppx_file = new Text();
		
		int[] burnins = {0,16,32,64,128};
		for (int burnin : burnins) {
		
		PracticalInference pi = new PracticalInference();
		
		pi.BURNIN=0;
		pi.BURNIN_DOCUMENTS=burnin;
		pi.BATCHSIZE=16;
		pi.dataset = "porn_full3";
		pi.T=25;
		pi.TRAINING_SHARE = 0.8;
		pi.delta_fix = 10;
		pi.RUNS = 200;
		pi.rhokappa=pi.rhokappa_document=pi.rhokappa_hyper=0.5;

		
		String dest = "/home/c/burnin_test_"+pi.dataset+"_"+pi.BATCHSIZE+"_"+pi.T;
		
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
	
	
	public static void ppxporn3() {

		int[] batchsizes = {4096,2048,1024,512,256,128,64,32,16,8,4,2,1};
		//int[] batchsizes = {8};
		Text ppx_file = new Text();
		
		for (int bs : batchsizes) {
		
		PracticalInference pi = new PracticalInference();
		
		pi.dataset = "porn_full3";
		pi.T=25;
		pi.TRAINING_SHARE = 0.8;
		pi.delta_fix = 0;
		

		
		ppx_file.write("/home/c/ppx_test_"+pi.dataset+"_"+pi.T , "", false);

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
	
	public static void main2 (String[] args) {
		
		//Threads == Batchsize
		int THREADS = 8;
		int THREADSIZE = 1;
		
		PracticalInference pi = new PracticalInference();

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

			pi.rhot_step++;
			//get step size
			pi.rhostkt_document = pi.rho(pi.rhos_document,pi.rhotau_document,pi.rhokappa_document,pi.rhot_step);
			pi.oneminusrhostkt_document = (1.0 - pi.rhostkt_document);

			int m=0;
			int t = 0;
			InferenceThread[] threads = new InferenceThread[THREADS];
			while (m<pi.M) {
				
				//if(t == 0) {
				//	System.out.print(".");
				//}
						
				int end;
				if (m+THREADSIZE-1<pi.M) {
					end = m+THREADSIZE-1;
				}
				else {
					end = pi.M-1;
				}
					
				//If thread already started, wait for it to finish
				if (threads[t]!=null) {
					try {
						threads[t].join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				//Then start a new thread
				threads[t] = new InferenceThread(pi,m,end,t);
				
				m+=THREADSIZE;
				t=(t+1)%THREADS;
				
				//we have to wait after processing the batch to update the parameters
				if ( (m%pi.BATCHSIZE) == 0) {
					for (int t2=0;t2<THREADS;t2++) {
						try {
							threads[t2].join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					t=0;
				}
				
			}
				
				
			
			System.out.println();

			pi.updateHyperParameters();


			if (pi.rhot_step%pi.SAVE_STEP==0) {
				//store inferred variables
				System.out.println("Storing variables...");
				pi.save();
			}

		}



		//inferenceDoc(null, null); //do update parameters using the new document

	}
	
}
