package org.gesis.promoss.inference;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.gesis.promoss.metadata.ClusterMetadata;
import org.gesis.promoss.tools.text.Text;

public class Experiments {
	
	private static int RUNS = 200;
	private static String directory = "/home/c/work/topicmodels/ml9/";

	
	public static void main(String[] args) {

		String corpusname = "corpus.txt";
		String metaname = "meta.txt";
		
		String params ="T(L1000)";

		//File corpusfile = new File(hmdp.c.directory + corpusname);
		//File metafile = new File(hmdp.c.directory + metaname);

		File textsFile = new File(directory + "texts.txt");
		File groupClusterFile = new File(directory + "groups.txt");

		if (!textsFile.exists() || !groupClusterFile.exists()) {
			
			System.out.println("Clustering metadata...");
			ClusterMetadata.transformData(params, directory, metaname, corpusname, "cluster/");

			File wordsetfile = new File(directory + "wordsets");
			if (wordsetfile.exists())
				wordsetfile.delete();
			File groupfile = new File(directory + "groups");
			if (groupfile.exists())
				groupfile.delete();
		}
		
		//lda();
		//dmr();
		hmd();
		
	}
	public static void dmr() {
		
			
		DMR_CSVB dmr = new DMR_CSVB();
		
		dmr.c.directory = directory;
		
		dmr.c.MIN_DICT_WORDS = 1000;
		
		dmr.BATCHSIZE = 512;
		
		dmr.T = 100;
		
		dmr.TRAINING_SHARE = 0.9;
		
		dmr.initialise();
		

		Text text = new Text();
		text.write("/home/c/dmrperplexity","",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			dmr.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			

			text.writeLine("/home/c/dmrperplexity",dmr.perplexity()+" " + timeSpent,true);
			text.close();
		}
		
				
		
		
	}
	
	public static void hmd () {

		HMD_PCSVB hmd = new HMD_PCSVB();
		
		hmd.c.directory = directory;
		
		hmd.c.MIN_DICT_WORDS = 1000;
		
		hmd.BATCHSIZE = 512;
		
		hmd.T = 100;
		
		hmd.BURNIN_DOCUMENTS = 20;
		
		
		hmd.TRAINING_SHARE = 0.9;
		
		hmd.initialise();
		

		Text text = new Text();
		text.write("/home/c/hmdperplexity","",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			hmd.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			

			text.writeLine("/home/c/hmdperplexity",hmd.perplexity()+" " + timeSpent,true);
			text.close();
		}
		
	}
	

	public static void lda() {

		LDA_CSVB lda = new LDA_CSVB();
		
		lda.c.directory = directory;
		
		lda.c.MIN_DICT_WORDS = 1000;
		
		lda.BATCHSIZE = 512;
		
		lda.T = 100;
		
		
		lda.TRAINING_SHARE = 0.9;
		
		lda.initialise();
		

		Text text = new Text();
		text.write("/home/c/ldaperplexity","",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			lda.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			

			text.writeLine("/home/c/ldaperplexity",lda.perplexity()+" " + timeSpent,true);
			text.close();
		}
		
	}
	
}
