package org.gesis.promoss.tools.text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

public class HMDP_Corpus extends Corpus {

	public String groupfile;

	
	public int F = 0; //Number of features
	public int[] Cf; //Number of clusters for each feature
	public int[] Cfd; //Number of documents for each feature
	public int[][] Cfc; //Number of documents for each cluster (FxCf)
	public int[][] Cfg; //Number of documents for each group (FxCg)

	public int[][][] A; //Groups and their clusters

	public ArrayList<ArrayList<Set<Integer>>> affected_groups; //Which clusters are affected by changes in group g; F x G x ...

	//document groups (M+empty_documents.size() x F)
	//we first have the groups of the non-empty documents
	//followed by the groups of the empty documents
	public int[][] groups;

	
	public void readCorpusSize() {
		// Read dictionary from file
		// The file contains words, one in each row

		int line_number=0;
		Text dictText = new Text();

		String line;
		while((line = dictText.readLine(documentfile)) != null) {
			line_number++;

			String[] lineSplit = line.split(" ");
			boolean empty = true;
			for (int i=1;i<lineSplit.length;i++) {
				if (dict.contains(lineSplit[i])) {
					empty = false;
					break;
				}
			}

			if (empty) {
				empty_documents.add(line_number);
			}
			else {
				M++;
			}
		}

		dictText = new Text();

		int doc_number=0;
		line_number = 0;
		while((line = dictText.readLine(documentfile)) != null && doc_number < Double.valueOf(M)*TRAINING_SHARE) {
			line_number++;
			if (!empty_documents.contains(line_number)) {
				doc_number++;
				String[] lineSplit = line.split(" ");

				for (int i=1;i<lineSplit.length;i++) {
					if (dict.contains(lineSplit[i])) {
						C++;
					}
				}


				String groupString = lineSplit[0];
				String[] groupSplit = groupString.split(",");
				for (int f=0;f<F;f++) {
					int g = Integer.valueOf(groupSplit[f]);
					Cfg[f][g]++;
					for (int c=0;c<A[f][g].length;c++) {
						int a = A[f][g][c];

						Cfc[f][a]++;
						Cfd[f]++;
					}
				}


			}
		}
		
		N = new int[M];
		
	}

	public void readGroups() {
		//initialise if not yet done
		if (A==null) {
			A = new int[F][][];
			for (int f = 0; f < F; f++) {
				A[f]=new int[0][];
			}
		}

		//initialise variable which stores the number of clusters for each feature
		Cf = new int[F];

		//get text of the groupfile
		Text grouptext = new Text();
		String line;
		if (new File(groupfile).exists()) {
			while ((line = grouptext.readLine(groupfile))!= null) {

				String[] lineSplit = line.split(" ");

				int f = Integer.valueOf(lineSplit[0]);
				int groupID = Integer.valueOf(lineSplit[1]);

				int[] cluster = new int[lineSplit.length - 2];
				for (int i=2;i<lineSplit.length;i++) {
					cluster[i-2] = Integer.valueOf(lineSplit[i]);

					//Find out about the maximum cluster ID. The number of clusters is this ID +1
					Cf[f] = 
							Math.max(Cf[f],cluster[i-2] + 1);
				}

				if(A[f].length - 1 < groupID) {
					int[][] Afold = A[f];
					A[f] = new int[groupID+1][];
					System.arraycopy(Afold, 0, A[f], 0, Afold.length);
				}
				A[f][groupID] = cluster;			
			}

		}
		else {
			//read info about groups from text document


			while ((line = grouptext.readLine(documentfile))!= null) {
				String[] groupSplit = line.split(" ")[0].split(",");
				for (int f=0;f<F;f++) {
					int g = Integer.valueOf(groupSplit[f]);
					Cf[f] = Math.max(Cf[f], g+1);
				}
			}
			for (int f=0;f<F;f++) {
				A[f]=new int[Cf[f]][1];
				for (int g=0;g<Cf[f];g++) {
					A[f][g][0]=g;
				}
			}
		}

		grouptext.close();
		
		//fill undefined groups with null
		//TODO: we have to add a mechanism for adding new groups...
		for (int f=0;f<F;f++) {
			for (int g=0;g<A[f].length;g++) {
				if (A[f][g] == null) {
					A[f][g] = new int[0];
				}
			}
		}

		affected_groups = new ArrayList<ArrayList<Set<Integer>>>();
		for (int f=0;f<F;f++) {
			affected_groups.add(f, new ArrayList<Set<Integer>>());
			for (int g=0;g<A[f].length;g++) {
				affected_groups.get(f).add(g, new HashSet<Integer>());
				for (int i = 0;i<A[f][g].length;i++) {
					for (int g2=0;g2<A[f].length;g2++) {
						for (int i2 = 0;i2<A[f][g].length;i2++) {
							if (i2==i) {
								affected_groups.get(f).get(g).add(g2);
							}
						}
					}
				}
			}


		}


	}

	/**
	 * Reads the number of features F from by counting the
	 * number of groups in the first line of the textfile
	 */
	public void readFfromTextfile() {
		String firstLine = Text.readLineStatic(documentfile);
		//File e.g. looks like groupID1,groupID2,groupID3,groupID4 word1 word2 word3
		F = firstLine.split(" ")[0].split(",").length;
	}

	@SuppressWarnings("unchecked")
	public void readDocs() {

		//Try to read parsed documents
		Load load = new Load();
		wordsets = load.readVarSet(directory+"wordsets");
		groups = load.readFileInt2(directory+"groups");
		if (wordsets!=null && groups != null) {

			for (int m=0;m<M;m++) {
				Set<Entry<Integer, Integer>> wordset = wordsets[m];
				for (Entry<Integer,Integer> e : wordset) {
					N[m]+=e.getValue();
				}		
			}	

			return ;
		}

		wordsets = new Set[M];
		groups = new int[M+empty_documents.size()][F];
		//Counter for the index of the groups of empty documents
		//They are added after the group information of the regular documents
		int empty_counter = 0;

		if (documentText == null) {
			documentText = new Text();
			documentText.setLang(language);
			documentText.setStopwords(stopwords);
			documentText.setStem(stemming);
		}

		String line = ""; 
		int m=0;
		int line_number = 0;
		while ((line = documentText.readLine(documentfile))!=null) {
			line_number++;
			HashMap<Integer,Integer> distinctWords = new HashMap<Integer, Integer>();

			String[] docSplit = line.split(" ",2);
			String[] groupString = docSplit[0].split(",");

			int[] group = new int[F];
			for (int f=0; f<F; f++) {
				group[f] = Integer.valueOf(groupString[f]);
			}

			if (!empty_documents.contains(line_number)) {

				if (docSplit.length>1) {
					if (processed) {
						String[] lineSplit2 = docSplit[1].split(" ");
						for(int i = 0; i < lineSplit2.length; i++) {
							String word = lineSplit2[i];
							if (dict.contains(word)) {
								int wordID = dict.getID(word);
								if (distinctWords.containsKey(wordID)) {
									int count = distinctWords.get(wordID);
									distinctWords.put(wordID, count+1);
								}
								else {
									distinctWords.put(wordID, 1);
								}
							}

						}
					}
					else {
						documentText.setText(docSplit[1]);
						Iterator<String> words = documentText.getTerms();

						while(words.hasNext()) {
							String word = words.next();
							if (dict.contains(word)) {
								int wordID = dict.getID(word);
								if (distinctWords.containsKey(wordID)) {
									int count = distinctWords.get(wordID);
									distinctWords.put(wordID, count+1);
								}
								else {
									distinctWords.put(wordID, 1);
								}
							}
						}
					}
					Set<Entry<Integer, Integer>> wordset = distinctWords.entrySet();

					if (m % Math.round(M/50) == 0)
						System.out.print(".");

					wordsets[m]=wordset;
					groups[m]=group;
					m++;
				}
				else {
					groups[M+empty_counter]=group;
					empty_counter++;
				}

			}


		}

		System.out.println("");

		for (m=0;m<M;m++) {
			Set<Entry<Integer, Integer>> wordset = wordsets[m];
			for (Entry<Integer,Integer> e : wordset) {
				N[m]+=e.getValue();
			}
		}

		Save save = new Save();
		save.saveVar(wordsets, directory+"wordsets");
		save.saveVar(groups, directory+"groups");

		return;

	}


}
