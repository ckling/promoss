package org.gesis.promoss.inference;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.gesis.promoss.metadata.ClusterMetadata;
import org.gesis.promoss.tools.math.BasicMath;
import org.gesis.promoss.tools.text.Text;
import org.gesis.promoss.inference.DMR_CSVB;


public class Experiments {
	


	private static int RUNS = 50;
	private static int MIN_DICT_WORDS = 2;
	private static int BATCHSIZE = 64;
	private static int T = 50;

	private static String directory = "/home/c/work/topicmodels/porn_hmd/";

	
	public static void main(String[] args) {

		String corpusname = "corpus.txt";
		String metaname = "meta.txt";
		

		String params ="";
		//String params ="T(L1000)";

		//String params ="G(100)";
		//String params ="T(L1000)";

		//String params ="N";

		//File corpusfile = new File(hmdp.c.directory + corpusname);
		//File metafile = new File(hmdp.c.directory + metaname);

		File textsFile = new File(directory + "texts.txt");
		File groupClusterFile = new File(directory + "groups.txt");

		if (1==0 || !textsFile.exists() || !groupClusterFile.exists()) {
			
			System.out.println("Clustering metadata...");
			ClusterMetadata.transformData(params, directory, metaname, corpusname, "cluster/");

			File wordsetfile = new File(directory + "wordsets");
			if (wordsetfile.exists())
				wordsetfile.delete();
			File groupfile = new File(directory + "groups");
			if (groupfile.exists())
				groupfile.delete();
		}
		
		//delall();

		//lda();
		//delall();
		//dmr();
		//delall();
		//hmd();
		//delall();
		//dmr2();
		//delall();
		//hmd2();
		//delall();
		//hmdp();

		mvhmdp2();

		if (1==1)return;
		
		directory = "/home/c/work/topicmodels/porn_hmd/";
		MIN_DICT_WORDS = 100;
		BATCHSIZE = 64;
		T=100;
		delall();
		hmd_p();
		//delall();
		//lda();

		directory = "/home/c/work/topicmodels/porn_dmr/";
		//delall();

		//dmr2();
		
	}
	
	public static void delall() {
		String [] filenames = {"meta","groups","wordsets"};
		
		for (int i=0;i<filenames.length;i++) {
		if (new File(directory+filenames[i]).exists()) {
			new File(directory+filenames[i]).delete();
		}
		}
	}
	
	public static void dmr() {
		
			
		DMR_CSVB dmr = new DMR_CSVB();
		
		dmr.c.directory = directory;
		
		dmr.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		dmr.BATCHSIZE = BATCHSIZE;
		
		dmr.T = T;
		
		dmr.TRAINING_SHARE = 0.9;
		
		dmr.BURNIN_DOCUMENTS =1;	
				
		dmr.initialise();
		
		
		Text text = new Text();
		text.write(directory+"dmrperplexity","",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			dmr.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			

			text.writeLine(directory+"dmrperplexity",dmr.perplexity()+" " + timeSpent,true);
		}
		text.close();

		dmr.save();
		dmr = null;
		
		
	}
	public static void dmr2() {

			
		DMR_CSVB dmr = new DMR_CSVB();
		
		dmr.c.directory = directory;
		
		dmr.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		dmr.BATCHSIZE = BATCHSIZE;
		
		dmr.T = T;
		
		dmr.TRAINING_SHARE = 0.9;
		
		dmr.BURNIN_DOCUMENTS = 1;
		dmr.OPTIMIZE_INTERVAL = 1;
				
		dmr.initialise();
		String ppxFileName = directory+"dmr2perplexity"+(System.currentTimeMillis()/1000);


		Text text = new Text();
		text.write(ppxFileName,"",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			dmr.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			

			text.writeLine(ppxFileName,dmr.perplexity()+" " + timeSpent,true);
		}
		text.close();

		dmr.save();
				
		dmr = null;
		
	}
	
	public static void hmd () {

		HMD_PCSVB hmd = new HMD_PCSVB();
		
		hmd.c.directory = directory;
		
		hmd.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		hmd.BATCHSIZE = BATCHSIZE;
		
		hmd.T = T;
		
		hmd.BURNIN_DOCUMENTS = 20;
		
		hmd.DELTA_CYCLE = 20;
		
		hmd.TRAINING_SHARE = 0.9;
		
		hmd.delta_fix = 10;
				
		hmd.initialise();
		
		String ppxFileName = directory+"hmdperplexity"+(System.currentTimeMillis()/1000);

		Text text = new Text();
		text.write(ppxFileName,"",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			hmd.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			
			text.writeLine(ppxFileName,hmd.perplexity()+" " + timeSpent,true);
			
			System.out.println(hmd.c.directory + " run " + i + " (alpha_0 "+BasicMath.sum(hmd.alpha_0)+" alpha_1 "+ hmd.alpha_1+ " beta_0 " + hmd.beta_0  + " delta " + hmd.delta[0]+ " epsilon " + hmd.epsilon[0]);
		}
		text.close();

		hmd = null;
		
	}
	
	public static void hmdp () {

		HMDP_PCSVB hmd = new HMDP_PCSVB();
		
		hmd.c.directory = directory;
		
		hmd.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		hmd.BATCHSIZE = BATCHSIZE;
		
		hmd.T = T;
		
		hmd.BURNIN_DOCUMENTS = 10;
		
		
		hmd.TRAINING_SHARE = 0.9;
		
		hmd.delta_fix = 10;
				
		hmd.initialise();
		
		String ppxFileName = directory+"hmdperplexity"+(System.currentTimeMillis()/1000);


		Text text = new Text();
		text.write(ppxFileName,"",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			hmd.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			
			text.writeLine(ppxFileName,hmd.perplexity()+" " + timeSpent,true);
			
			System.out.println(hmd.c.directory + " run " + i + " (alpha_0 "+hmd.alpha_0+" alpha_1 "+ hmd.alpha_1+ " beta_0 " + hmd.beta_0  + " delta " + hmd.delta[0]+ " epsilon " + hmd.epsilon[0] + " gamma "+hmd.gamma);
		}
		text.close();
		hmd.save();

		hmd = null;
		
	}
	

	
	public static void mvhmdp () {

		MVHMDP_PCSVB hmd = new MVHMDP_PCSVB();
		
		hmd.c.directory = directory;
		
		hmd.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		hmd.BATCHSIZE = BATCHSIZE;
		
		hmd.T = T;
		
		hmd.BURNIN_DOCUMENTS = 10;
		
		
		hmd.TRAINING_SHARE = 0.9;
		
		hmd.delta_fix = 10;
				
		hmd.initialise();
		
		String ppxFileName = directory+"hmdperplexity"+(System.currentTimeMillis()/1000);

		Text text = new Text();
		text.write(ppxFileName,"",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			hmd.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			
			text.writeLine(ppxFileName,hmd.perplexity()+" " + timeSpent,true);
			
			System.out.println(hmd.c.directory + " run " + i + " (alpha_0 "+hmd.alpha_0+" alpha_1 "+ hmd.alpha_1+ " beta_0 " + hmd.beta_0  + " delta " + hmd.delta[0]+ " epsilon " + hmd.epsilon[0] + " gamma "+hmd.gamma);
		}
		text.close();

		hmd.save();
		
		hmd = null;
		
	}
	
	public static void mvhmdp2 () {

		MVHMDP_PCSVB hmd = new MVHMDP_PCSVB();
		
		hmd.c.directory = directory;
		
		hmd.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		hmd.BATCHSIZE = BATCHSIZE;
		
		hmd.T = T;
		
		hmd.c.processed=false;
		hmd.c.stemming=true;
		hmd.c.stopwords=true;
		hmd.c.language="de";
		

		hmd.BURNIN_DOCUMENTS = 0;

		
		
		hmd.TRAINING_SHARE = 0.9;
		
		hmd.delta_fix = 10;
				
		hmd.initialise();
		
		String ppxFileName = directory+"hmdperplexity"+(System.currentTimeMillis()/1000);

		Text text = new Text();
		text.write(ppxFileName,"",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			hmd.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			
			text.writeLine(ppxFileName,hmd.perplexity()+" " + timeSpent,true);
			
			System.out.println(hmd.c.directory + " run " + i + " (alpha_0 "+hmd.alpha_0+" alpha_1 "+ hmd.alpha_1+ " beta_0 " + hmd.beta_0  + " delta " + hmd.delta[0]+ " epsilon " + hmd.epsilon[0] + " gamma "+hmd.gamma);
		}
		text.close();

		hmd.save();
		
		hmd = null;
		
	}
	
	public static void hmd2 () {

		HMD_PCSVB hmd = new HMD_PCSVB();
		
		hmd.c.directory = directory;
		
		hmd.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		hmd.BATCHSIZE = BATCHSIZE;
		
		hmd.T = T;
		
		//hmd.alpha_1_fix = 0;
		
		hmd.BURNIN_DOCUMENTS = 1;
		
		//hmd.DELTA_CYCLE = 20;
		
		hmd.TRAINING_SHARE = 0.9;
		
		//hmd.delta_fix = 10;
		
		hmd.initialise();
		String ppxFileName =directory+"hmdperplexity"+(System.currentTimeMillis()/1000);


		Text text = new Text();
		text.write(ppxFileName,"",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			hmd.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			System.out.println(hmd.c.directory + " run " + i + " (alpha_0 "+BasicMath.sum(hmd.alpha_0)+" alpha_1 "+ hmd.alpha_1+ " beta_0 " + hmd.beta_0  + " delta " + hmd.delta[0]+ " epsilon " + hmd.epsilon[0]);


			text.writeLine(ppxFileName,hmd.perplexity()+" " + timeSpent,true);
		}
		text.close();

		hmd = null;
	}
	
	public static void hmd_p () {

		HMD_PCSVB hmd = new HMD_PCSVB();
		
		hmd.c.directory = directory;
		
		hmd.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		hmd.BATCHSIZE = BATCHSIZE;
		
		hmd.BATCHSIZE_GROUPS = BATCHSIZE;
		
		hmd.T = T;
		
		hmd.alpha_1_fix = 0;
		
		hmd.BURNIN_DOCUMENTS = 1;
		
		hmd.DELTA_CYCLE = 20;
		
		hmd.TRAINING_SHARE = 0.9;
		
		hmd.delta_fix = 10;
		
		hmd.initialise();
		
		for (int k=0;k<hmd.T;k++)  {
			hmd.burninPrior[k] = 1;
		}
		
		String ppxFileName = directory+"hmdperplexity"+(System.currentTimeMillis()/1000);


		Text text = new Text();
		text.write(ppxFileName,"",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			hmd.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			System.out.println(hmd.c.directory + " run " + i + " (alpha_0 "+BasicMath.sum(hmd.alpha_0)+" alpha_1 "+ hmd.alpha_1+ " beta_0 " + hmd.beta_0  + " delta " + hmd.delta[0]+ " epsilon " + hmd.epsilon[0]);


			text.writeLine(ppxFileName,hmd.perplexity()+" " + timeSpent,true);
		}
		text.close();

		hmd = null;

	}
	


	public static void lda() {

		LDA_CSVB lda = new LDA_CSVB();
		
		lda.c.directory = directory;
		
		lda.c.MIN_DICT_WORDS = MIN_DICT_WORDS;
		
		lda.BATCHSIZE = BATCHSIZE;
		
		lda.T = T;
		
		lda.BURNIN_DOCUMENTS = 1;
		
		lda.TRAINING_SHARE = 0.9;
			
		lda.initialise();
		
		for (int k=0;k<lda.T;k++)  {
			lda.alpha[k] = 1;
		}
		lda.BURNIN_DOCUMENTS = 1000;

		
		Text text = new Text();
		text.write(directory+"ldaperplexity"+(System.currentTimeMillis()/1000),"",false);
		
		long timeSpent = 0;
		for (int i=0;i<RUNS;i++) {
			long timeStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			lda.onePass();		
			long timeNow = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			timeSpent +=  timeNow - timeStart;
			

			text.writeLine(directory+"ldaperplexity"+(System.currentTimeMillis()/1000),lda.perplexity()+" " + timeSpent,true);
		}
		text.close();

		lda = null;
		
	}
	
	
	
}
