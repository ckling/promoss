/*
 * Copyright (C) 2007 by
 * 
 * 	Xuan-Hieu Phan
 *	hieuxuan@ecei.tohoku.ac.jp or pxhieu@gmail.com
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 * 
 *  Cam-Tu Nguyen
 *  ncamtu@gmail.com
 *  College of Technology
 *  Vietnam National University, Hanoi
 *
 * JGibbsLDA is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JGibbsLDA is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGibbsLDA; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package jgibblda;


import java.io.File;
import java.lang.management.ManagementFactory;

import ckling.text.Text;

import ckling.math.BasicMath;



public class LDA2 {

	
	
	public static void main(String args[]){
		
    	String name = "food";
		int runs = 12;
		double[] ppx = new double[runs];
		double[] ppxLoc = new double[runs];
		int[] K=new int[runs];

		String dir ="/export/ckling/jgibblda/models/"+name+"/";

		File dirFile = new File(dir);
		if (!dirFile.exists()) {
			dir ="/home/c/work/jgibblda/models/"+name+"/";
			System.out.println(dir);
		}

		//args = "-dir /home/c/work/jgibblda/models/LSD2/ -dfile flickrPhoto.txt -est -ntopics 200 -alpha 0.25 -beta 0.1 -savestep 100 -twords 20 -niters 2000".split(" ");
		String arg = "-dir "+dir+" -dfile "+name+".txt " +
				"-est " +
				"-J 2000 " +
				"-R 1 " +
				"-beta 0.5 " +
				"-savestep 5 " +
				" -twords 20 " +
				"-niters 200 " 				;
		args = arg.split(" ");

		//args = "-dir /home/c/work/jgibblda/LSD4 -dfile flickrPhoto.txt -est -ntopics 50 -alpha 1.0 -beta 0.1 -savestep 100 -twords 20 -niters 2000".split(" ");
		//args = "-dir /export/ckling/LSD3 -dfile flickrPhoto.txt -est -ntopics 50 -alpha 1.0 -beta 0.1 -savestep 100 -twords 20 -niters 2000".split(" ");


		Option option = new Option(args);

		int[] regions = {250,750};
		double[] timeSpent = new double[runs];
		for (int i=0;i<runs;i++) {
			option.J = regions[i%regions.length];

			//time in nanoseconds
			long t = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			Estimator estimator = new Estimator();
			estimator.init(option);
			estimator.estimate();
			long tspent = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - t;
			System.out.println(tspent);
			timeSpent[i]=tspent;

			estimator.getPerplexity();
			estimator.getPerplexityPlaceKnown();
			if (estimator.perplexityLoc!=0.0) {
				K[i]=estimator.K;
				ppx[i]=estimator.perplexity;
				ppxLoc[i]=estimator.perplexityLoc;
			}
			
			if (estimator.K==6) break;
			
		}

		int rand = (int) Math.floor(Math.random() * 1000);
		if (K[0]!=0) {
			String content = "";
			for (int i=0;i<runs;i++) {
				content += regions[i%regions.length]+","+K[i]+","+ppxLoc[i]+"\n";
				//content += K[i]+","+ppx[i]+","+ppxLoc[i]+"\n";
			}
			System.out.println(content);
			Text text = new Text();
			text.setText(content);
			String fileName = dir + "ppx2_"+rand;
			text.write(fileName);
		}

		System.out.println("time:");
		String timeText ="";
		for (int i=0;i<timeSpent.length;i++) {
			timeText += regions[i%regions.length] + " "+ timeSpent[i] + "\n";
			System.out.println(regions[i%regions.length] + " "+ timeSpent[i] + "\n");
		}
		Text text = new Text(timeText);
		String fileName = option.dir + "time2_"+rand;
		text.write(fileName);
		

	}
}
