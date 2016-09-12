package org.gesis.promoss.inference;

import java.io.File;

import org.gesis.promoss.metadata.ClusterMetadata;

public class Experiments {
	public static void main(String[] args) {

		
		DMR_CSVB dmr = new DMR_CSVB();
		
		dmr.c.directory = "/home/c/work/topicmodels/ml6/";
		
		dmr.c.MIN_DICT_WORDS = 1000;
		
		dmr.BATCHSIZE = 512;
		
		dmr.T = 10;
		
		dmr.initialise();
		dmr.run();
		
		
		
	}
	
}
