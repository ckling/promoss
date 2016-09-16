package org.gesis.promoss.inference;


public class Experiments {
	public static void main(String[] args) {

			
		DMR_CSVB dmr = new DMR_CSVB();
		
		dmr.c.directory = "/home/c/work/topicmodels/ml8/";
		
		dmr.c.MIN_DICT_WORDS = 1000;
		
		dmr.BATCHSIZE = 512;
		
		dmr.T = 100;
		
		dmr.TRAINING_SHARE = 0.9;
		
		dmr.initialise();
		dmr.run();
		
		
		
	}
	
}
