package org.gesis.promoss.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

import org.gesis.promoss.tools.geo.MF_Delaunay;
import org.gesis.promoss.tools.text.Dictionary;
import org.gesis.promoss.tools.text.Text;


public class ClusterMetadata {


	public static void transformData(String params,String dir,String meta_file_name, String corpus_file_name, String cluster_folder)  {

		//parameters separated by semicolons
		//options in brackets, comma separated
		//params = "G(1000);T(L1000,Y100,M10,W10,D10);N";

		//dir = "/home/c/work/topicmodels/easy/";

		//raw_file_name = "meta.txt";
		//corpus_file_name = "corpus.txt";

		String rawLocation = dir+meta_file_name;

		cluster_folder = "cluster_desc/";

		File file = new File(dir+cluster_folder);
		if (!file.exists()) file.mkdir();

		ArrayList<ArrayList<Integer>> groupmembers = new ArrayList<ArrayList<Integer>>();
		String groupclusters = "";

		Text raw = new Text();
		int n = 0;
		String line;

		while((line = raw.readLine(rawLocation))!=null) {
			n++;
			groupmembers.add(new ArrayList<Integer>());
		}


		String[] param_split = params.split(";");

		//the category index can be larger than the current position
		//because e.g. time can generate multiple context variables
		int currentCatIdx = 0;

		for (int i=0; i< param_split.length; i++) {

			String[] p_split = param_split[i].split("\\(");
			String[] p_args = null;
			if (p_split.length > 1) {
				//remove last sign which should be a bracket
				p_split[1]=p_split[1].substring(0,p_split[1].length() - 1);
				p_args = p_split[1].split(",");

			}

			String p = p_split[0];


			//Geographical context variable: latitude, longitude
			if(p.startsWith("G")) {

				if (p_args == null) {
					p_args = new String[1];
					p_args[0] = String.valueOf(Math.max(Math.round(n/100),1));
				}

				ArrayList<Double> latlist = new ArrayList<Double>();
				ArrayList<Double> lonlist = new ArrayList<Double>();

				raw = new Text();

				while((line = raw.readLine(rawLocation))!=null) {

					String[] lineSplit = line.split(";");

					String[] latlon = lineSplit[i].split(",");

					latlist.add(Double.valueOf(latlon[0]));
					lonlist.add(Double.valueOf(latlon[1]));


				}

				Double[] lats = latlist.toArray(new Double[latlist.size()]);
				Double[] lons = latlist.toArray(new Double[latlist.size()]);

				MF_Delaunay mfd = new MF_Delaunay(lats,lons,Integer.valueOf(p_args[0]),dir + cluster_folder,i);				

				ArrayList<CopyOnWriteArraySet<Integer>> qqN = mfd.getqqN();
				int[] q = mfd.getq();
				//assign cluster-memberships to documents
				for (int j=0;j<n;j++) {
					groupmembers.get(j).add(q[j]);					
				}
				//assign groups to context clusters
				for (int j=0;j<qqN.size();j++) {
					//every group has the cluster of the same ID plus the neighbours as parents!
					groupclusters+=currentCatIdx + " " +j + " " + j;
					CopyOnWriteArraySet<Integer> neighbours = qqN.get(j);
					Iterator<Integer> it = neighbours.iterator();
					while(it.hasNext()) {
						int neighbour = it.next();
						groupclusters+=" "+neighbour;
					}
					groupclusters+="\n";
				}		

				//write file with geographical cluster description
				double[][] coords = mfd.getqm();
				double[] ks = mfd.getqk();
				String cluster_file_path = dir + cluster_folder + "cluster"+ i;
				Text cluster_file = new Text();
				cluster_file.write(cluster_file_path, "", false);
				for (int l=0;l<ks.length;l++) {
					cluster_file.writeLine(cluster_file_path, coords[l][0]+" "+coords[l][1]+ " " + ks[l], true);
				}


			}

			//Temporal context variable: Unixtime.
			if(p.startsWith("T")) {

				//T(L1000,Y100,M10,W10,D10)

				for (int j = 0; j<p_args.length;j++) {

					//Linear time 
					//Here we bin the documents based on their timestamps.
					//The number of bins is the value in p_args[j]
					if (p_args[j].startsWith("L")) {

						int bins = Integer.valueOf(p_args[j].substring(1));

						int[] timestamps = new int[n];

						int k = 0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestamp = Integer.valueOf(lineSplit[i].trim().replace("\"",""));
							timestamps[k] = timestamp;
							k++;						
						}
						Arrays.sort(timestamps);

						//how many document are in a bin?
						int clustersize = (int) Math.floor(n / Double.valueOf(bins));

						//document index
						int m=0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestamp = Integer.valueOf(lineSplit[i]);
							//here we find the bin to which the timestamp of a document belongs to
							int l;
							for (l=0;l<bins;l++) {
								int offset = Math.min((l+1)*clustersize,timestamps.length-1);
								if (timestamp <= timestamps[offset]) break;
							}
							if (l>=bins) l=bins-1;
							groupmembers.get(m).add(l);					
							m++;
						}

						for (int c=0;c<bins;c++) {
							//for every category, there is a group which is linked to the cluster of the same ID
							groupclusters += currentCatIdx+ " " + c + " " + c;

							if (c>0) {
								groupclusters += " " + (c-1);
							}
							if (c<bins-1) {
								groupclusters += " " + (c+1);
							}

							groupclusters += "\n";
						}

						//write median (!) of the timestamps in the bins
						String cluster_file_path = dir+ cluster_folder + "cluster"+ i+"_"+p_args[j].substring(0,1);
						Text cluster_desc = new Text();
						cluster_desc.write(cluster_file_path, "", false);
						for (int c=0;c<bins;c++) {
							int offset = (int) Math.round((c+0.5)*clustersize);
							cluster_desc.write(cluster_file_path, c + " " + timestamps[offset]+"\n", false);
						}

					}

					//Yearly cycle 
					//Here we bin the documents based on their timestamps.
					//The number of bins is the value in p_args[j]
					if (p_args[j].startsWith("Y")) {

						//year = 365.25 days, day = 24*60*60 s

						int bins = Integer.valueOf(p_args[j].substring(1));

						double[] timestamps = new double[n];

						int k = 0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestamp = Integer.valueOf(lineSplit[i]);
							timestamps[k] = timestamp % (365.25 * 86400);
							k++;						
						}
						Arrays.sort(timestamps);

						//how many document are in a bin?
						int clustersize = (int) Math.floor(n / Double.valueOf(bins));

						//document index
						int m=0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestampInt = Integer.valueOf(lineSplit[i]);
							double timestamp = timestampInt % (365.25 * 86400);
							//here we find the bin to which the timestamp of a document belongs to
							int l;
							for (l=0;l<bins;l++) {
								//Beginning and end of bin
								int end = Math.min((l+1)*clustersize - 1,timestamps.length-1);
								int start = Math.min(0, l*clustersize);
								if (timestamps[start] <= timestamp && timestamp <= timestamps[end]) {
									break;
								}
							}
							if (l>=bins) l=bins-1;
							groupmembers.get(m).add(l);					
							m++;
						}

						for (int c=0;c<bins;c++) {
							//for every category, there is a group which is linked to the cluster of the same ID
							groupclusters += currentCatIdx+ " " + c + " " + c;

							if (c>0) {
								groupclusters += " " + (c-1);
							}
							if (c<bins-1) {
								groupclusters += " " + (c+1);
							}

							groupclusters += "\n";
						}

						//write median (!) of the timestamps in the bins
						String cluster_file_path = dir + cluster_folder + "cluster"+ i+"_"+p_args[j].substring(0,1);
						Text cluster_desc = new Text();
						cluster_desc.write(cluster_file_path, "", false);
						for (int c=0;c<bins;c++) {
							int offset = (int) Math.round((c+0.5)*clustersize);
							cluster_desc.write(cluster_file_path, c + " " + timestamps[offset]+"\n", false);
						}


					}

					//Monthly cycle 
					//Here we bin the documents based on their timestamps.
					//The number of bins is the value in p_args[j]
					if (p_args[j].startsWith("M")) {

						//year = 365.25 days -> Month 30,4375 days, day = 24*60*60 s

						int bins = Integer.valueOf(p_args[j].substring(1));

						double[] timestamps = new double[n];

						int k = 0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestamp = Integer.valueOf(lineSplit[i]);
							timestamps[k] = timestamp % (30.4375 * 86400);
							k++;						
						}
						Arrays.sort(timestamps);

						//how many document are in a bin?
						int clustersize = (int) Math.floor(n / Double.valueOf(bins));

						//document index
						int m=0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestampInt = Integer.valueOf(lineSplit[i]);
							double timestamp = timestampInt % (30.4375 * 86400);
							//here we find the bin to which the timestamp of a document belongs to
							int l;
							for (l=0;l<bins;l++) {
								//Beginning and end of bin
								int end = Math.min((l+1)*clustersize - 1,timestamps.length-1);
								int start = Math.min(0, l*clustersize);
								if (timestamps[start] <= timestamp && timestamp <= timestamps[end]) {
									break;
								}
							}
							if (l>=bins) l=bins-1;
							groupmembers.get(m).add(l);					
							m++;
						}

						for (int c=0;c<bins;c++) {
							//for every category, there is a group which is linked to the cluster of the same ID
							groupclusters += currentCatIdx+ " " + c + " " + c;

							if (c>0) {
								groupclusters += " " + (c-1);
							}
							if (c<bins-1) {
								groupclusters += " " + (c+1);
							}

							groupclusters += "\n";
						}

						//write median (!) of the timestamps in the bins
						String cluster_file_path = dir + cluster_folder + "cluster"+ i+"_"+p_args[j].substring(0,1);
						Text cluster_desc = new Text();
						cluster_desc.write(cluster_file_path, "", false);
						for (int c=0;c<bins;c++) {
							int offset = (int) Math.round((c+0.5)*clustersize);
							cluster_desc.write(cluster_file_path, c + " " + timestamps[offset]+"\n", false);
						}

					}


					//Weekly cycle 
					//Here we bin the documents based on their timestamps.
					//The number of bins is the value in p_args[j]
					if (p_args[j].startsWith("W")) {

						//Week = 7*24*60*60 s

						int bins = Integer.valueOf(p_args[j].substring(1));

						double[] timestamps = new double[n];

						int k = 0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestamp = Integer.valueOf(lineSplit[i]);
							//1.1.1970 is a Thursday, so we shift 4*24*60*60 = 345600s
							timestamps[k] = (timestamp + 345600) % 604800;
							k++;						
						}
						Arrays.sort(timestamps);

						//how many document are in a bin?
						int clustersize = (int) Math.floor(n / Double.valueOf(bins));

						//document index
						int m=0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestamp = Integer.valueOf(lineSplit[i]);
							timestamp = (timestamp + 345600) % 604800;
							//here we find the bin to which the timestamp of a document belongs to
							int l;
							for (l=0;l<bins;l++) {
								//Beginning and end of bin
								int end = Math.min((l+1)*clustersize - 1,timestamps.length-1);
								int start = Math.min(0, l*clustersize);
								if (timestamps[start] <= timestamp && timestamp <= timestamps[end]) {
									break;
								}
							}
							if (l>=bins) l=bins-1;
							groupmembers.get(m).add(l);					
							m++;
						}

						for (int c=0;c<bins;c++) {
							//for every category, there is a group which is linked to the cluster of the same ID
							groupclusters += currentCatIdx+ " " + c + " " + c;

							if (c>0) {
								groupclusters += " " + (c-1);
							}
							if (c<bins-1) {
								groupclusters += " " + (c+1);
							}

							groupclusters += "\n";
						}

						//write median (!) of the timestamps in the bins
						String cluster_file_path = dir + cluster_folder + "cluster"+ i+"_"+p_args[j].substring(0,1);
						Text cluster_desc = new Text();
						cluster_desc.write(cluster_file_path, "", false);
						for (int c=0;c<bins;c++) {
							int offset = (int) Math.round((c+0.5)*clustersize);
							cluster_desc.write(cluster_file_path, c + " " + timestamps[offset]+"\n", false);
						}

					}

					//Weekly cycle 
					//Here we bin the documents based on their timestamps.
					//The number of bins is the value in p_args[j]
					if (p_args[j].startsWith("D")) {

						//Day = 24*60*60 = 86400 s

						int bins = Integer.valueOf(p_args[j].substring(1));

						double[] timestamps = new double[n];

						int k = 0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestamp = Integer.valueOf(lineSplit[i]);
							timestamps[k] = timestamp % 86400;
							k++;						
						}
						Arrays.sort(timestamps);

						//how many document are in a bin?
						int clustersize = (int) Math.floor(n / Double.valueOf(bins));

						//document index
						int m=0;
						raw = new Text();
						while((line = raw.readLine(rawLocation))!=null) {
							String[] lineSplit = line.split(";");
							int timestamp = Integer.valueOf(lineSplit[i]);
							timestamp = timestamp % 86400;
							//here we find the bin to which the timestamp of a document belongs to
							int l;
							for (l=0;l<bins;l++) {
								int end = Math.min((l+1)*clustersize - 1,timestamps.length-1);
								int start = Math.min(0, l*clustersize);
								if (timestamps[start] <= timestamp && timestamp <= timestamps[end]) {
									break;
								}
							}
							if (l>=bins) l=bins-1;
							groupmembers.get(m).add(l);					
							m++;
						}

						for (int c=0;c<bins;c++) {
							//for every category, there is a group which is linked to the cluster of the same ID
							groupclusters += currentCatIdx+ " " + c + " " + c;

							if (c>0) {
								groupclusters += " " + (c-1);
							}
							if (c<bins-1) {
								groupclusters += " " + (c+1);
							}

							groupclusters += "\n";
						}

						//write median (!) of the timestamps in the bins
						String cluster_file_path = dir + cluster_folder + "cluster"+ i+"_"+p_args[j].substring(0,1);
						Text cluster_desc = new Text();
						cluster_desc.write(cluster_file_path, "", false);
						for (int c=0;c<bins;c++) {
							int offset = (int) Math.round((c+0.5)*clustersize);
							cluster_desc.write(cluster_file_path, c + " " + timestamps[offset]+"\n", false);
						}
					}


					currentCatIdx++;

				}


			}

			//Nominal context variable
			if(p.startsWith("N")) {
				
				Dictionary dict = new Dictionary();

				raw = new Text();
				int j = 0;
				while((line = raw.readLine(rawLocation))!=null) {
					String[] lineSplit = line.split(";");
					String category = lineSplit[i];
					dict.addWord(category);	
					int categoryID = dict.getID(category);
					groupmembers.get(j).add(categoryID);
					j++;
				}

				String cluster_file_path = dir + cluster_folder + "cluster"+ i;

				dict.writeWordMap(cluster_file_path);

				for (int k=0;k<dict.length();k++) {
					//for every category, there is a group which is linked to the cluster of the same ID
					groupclusters += currentCatIdx+ " " + k + " " + k+"\n";
				}

				currentCatIdx++;

			}

			//Ordinal context variables
			if(p.equals("O")) {

				Dictionary dict = new Dictionary();

				raw = new Text();
				while((line = raw.readLine(rawLocation))!=null) {
					String[] lineSplit = line.split(";");
					String category = lineSplit[i];
					dict.addWord(category);	
				}
				dict.sortDictionary();

				int j = 0;
				while((line = raw.readLine(rawLocation))!=null) {
					String[] lineSplit = line.split(";");
					String category = lineSplit[i];
					int categoryID = dict.getID(category);
					groupmembers.get(j).add(categoryID);
					j++;
				}				

				String cluster_file_path = dir + cluster_folder + "cluster"+ i;

				dict.writeWordMap(cluster_file_path);

				for (int k=0;k<dict.length();k++) {
					//for every category, there is a group which is linked to the cluster of the same ID
					groupclusters += currentCatIdx+ " " + k + " " + k;

					if (k>0) {
						groupclusters += " " + (k-1);
					}
					if (k<dict.length()-1) {
						groupclusters += " " + k+1;
					}

					groupclusters += "\n";
				}

				currentCatIdx++;

			}
			//Cyclic context variable
			if(p.equals("C")) {

				Dictionary dict = new Dictionary();

				raw = new Text();
				while((line = raw.readLine(rawLocation))!=null) {
					String[] lineSplit = line.split(";");
					String category = lineSplit[i];
					dict.addWord(category);	
				}
				dict.sortDictionary();

				int j = 0;
				while((line = raw.readLine(rawLocation))!=null) {
					String[] lineSplit = line.split(";");
					String category = lineSplit[i];
					int categoryID = dict.getID(category);
					groupmembers.get(j).add(categoryID);
					j++;
				}				

				String cluster_file_path = dir + cluster_folder + "cluster"+ i;

				dict.writeWordMap(cluster_file_path);

				for (int k=0;k<dict.length();k++) {
					//for every category, there is a group which is linked to the cluster of the same ID
					groupclusters += currentCatIdx+ " " + k + " " + k;

					if (k>0) {
						groupclusters += " " + (k-1);
					}
					else {
						//connect circle
						groupclusters += " " + (dict.length()-1);
					}
					if (k<dict.length()-1) {
						groupclusters += " " + k+1;
					}
					else {
						//connect circle
						groupclusters += " " + 0;
					}

					groupclusters += "\n";
				}

				currentCatIdx++;

			}


			Text groups_file = new Text();
			groups_file.write( dir+"groups.txt",groupclusters,false);

			Text member_file = new Text();
			member_file.write( dir+"texts.txt","",false);

			Text corpus_file = new Text();

			for(ArrayList<Integer> doc : groupmembers) {
				String corpus_line = corpus_file.readLine(dir+corpus_file_name);
				boolean first = true;
				for(Integer group : doc) {
					if (!first) {
						member_file.write( dir+"member",",",true);
					}
					first = false;
					member_file.write( dir+"member",String.valueOf(group),true);				
				}
				member_file.write( dir+"member"," " + corpus_line + "\n",true);
			}
		}



	}

}
