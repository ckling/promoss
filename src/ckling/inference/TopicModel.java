package ckling.inference;

public class TopicModel {

	public static void main (String[] args) {
		
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

			System.out.println("Run " + i + " (alpha_0 "+pi.alpha_0+" alpha_1 "+ pi.alpha_1+ " beta_0 " + pi.beta_0 + " gamma "+pi.gamma + " delta " + pi.delta+ " epsilon " + pi.epsilon[0]);

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
