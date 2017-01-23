package org.gesis.promoss.tools.text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;


public class DCTM_Corpus extends Corpus {

	public String metafile;

	public int G = 0; //Number of Groups
	public int[] Gd; //Number of documents per group
	public int[] Gc; //Number of comments per group

	
	public int[][] meta; //metadata of documents: Group, Document and Comment ID

	private HashMap<Integer,Integer> hm = new HashMap<Integer,Integer>();
	private HashMap<Integer,Integer>[] postMap;

	
	public int[][] Cd; //Number of comments per document d of group g
	public int D; //Number of commented documents
	

	
	public void readCorpusSize() {
		
		Load load = new Load();

		//Try to read parsed documents
		//TODO: add empty documents to svmlight!
		//if (load.readSVMlight(directory+"wordsets", this)) {			
		//	return;
		//}
		
		// Else read dictionary from file
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
		while((line = dictText.readLine(documentfile)) != null) {
			line_number++;
			if (!empty_documents.contains(line_number)) {
				doc_number++;
				String[] lineSplit = line.split(" ");

				for (int i=1;i<lineSplit.length;i++) {
					if (dict.contains(lineSplit[i])) {
						C++;
					}
				}

			}
		}	
		
		N = new int[M];
		}

	@SuppressWarnings("unchecked")
	public void readDocs() {
		
		

		//Try to read parsed documents
		Load load = new Load();

		meta = load.readFileInt2(directory+"meta");
		//Try to read parsed documents
		if (load.readSVMlight(directory+"wordsets", this) && meta != null) {	
			
			G=0;
			//System.out.println(meta.length + " " + M);
			
			for (int i=0;i<M;i++) {
				
				//map g to indices
				if (!hm.containsKey(meta[i][0])) {
					hm.put(meta[i][0],G);
					G++;
				}
				meta[i][0] = hm.get(meta[i][0]);

				//Save mapping
				Save save = new Save();
				save.saveVar(hm, directory+"groupmap");
				
				 postMap = new HashMap[G];
				 for (int g=0;g<G;g++) {
					 postMap[g]=new HashMap<Integer,Integer>();
							 }
				
			if (meta[i][2] == 0) {
				//we count the number of commmented documents
				//as their commentID is 0, we do it like this
				D++;
			}
			}
			Cd = new int[G][];
			Gd = new int[G]; 
			Gc = new int[G];
			

			
			for (int i=0;i<M;i++) {
				int g = meta[i][0];
				int docID = meta[i][1];
				int cID = meta[i][2];

				//map d to indices (per group g)
				if (!postMap[g].containsKey(docID)) {
					//new post!
					postMap[g].put(docID, Gd[g]);
					Gd[g]++;				
				}
				
				if (cID>0) {
					Gc[g]++;
				}
				meta[i][1] = postMap[g].get(docID);
				
			}
					
			for (int g=0;g<G;g++) {
				Gd[g] = postMap[g].size();

				Cd[g]=new int[Gd[g]];
			}

			for (int i=0;i<M;i++) {
				int g = meta[i][0];
				int d = meta[i][1];
				int cID = meta[i][2];
				
				//System.out.println(g + " " + d+ " " + G + " " + Gd[g] + " " + Cd[g][d]);

				if (cID > 0) {
				
					meta[i][2]=Cd[g][d]+1;
					Cd[g][d]++;

				}

			}
			
				
			
			return;
		}
		
		termIDs = new int[M][];
		termFreqs = new short[M][];
		
		Save saveSVMlight = new Save();

		meta = new int[M+empty_documents.size()][3];
		//Counter for the index of the groups of empty documents
		//They are added after the group information of the regular documents
		int empty_counter = 0;

		if (documentText == null) {
			documentText = new Text();
			documentText.setLang(language);
			documentText.setStopwords(stopwords);
			documentText.setStem(stemming);
		}
		Text metaText = new Text();
		

		String line = ""; 
		String metaline = "";
		int m=0;
		int line_number = 0;
		System.out.println(metafile);
		System.out.println(documentfile);

		while ((line = documentText.readLine(documentfile))!=null && (metaline = metaText.readLine(metafile))!=null) {
			line_number++;
			HashMap<Integer,Short> distinctWords = new HashMap<Integer, Short>();

			String[] metaString = metaline.split(",");

			int[] meta_value = new int[3];
			for (int f=0; f<3; f++) {
				meta_value[f] = Integer.valueOf(metaString[f]);
			}
			if (!hm.containsKey(meta_value[0])) {
				hm.put(meta_value[0],G);
				G++;
			}
			//use G as index
			meta_value[0]=hm.get(meta_value[0]);



			if (!empty_documents.contains(line_number)) {
					if (processed) {
						String[] docSplit = line.split(" ");

						for(int i = 0; i < docSplit.length; i++) {
							String word = docSplit[i];
							if (dict.contains(word)) {
								int wordID = dict.getID(word);
								if (distinctWords.containsKey(wordID)) {
									int count = distinctWords.get(wordID);
									distinctWords.put(wordID, (short) (count+1));
								}
								else {
									distinctWords.put(wordID, (short) 1);
								}
							}

						}
					}
					else {
						documentText.setText(line);
						Iterator<String> words = documentText.getTerms();

						while(words.hasNext()) {
							String word = words.next();
							//System.out.println("m " + m + " "+ word);
							if (dict.contains(word)) {
								int wordID = dict.getID(word);
								if (distinctWords.containsKey(wordID)) {
									int count = distinctWords.get(wordID);
									distinctWords.put(wordID, (short) (count+1));
								}
								else {
									distinctWords.put(wordID, (short) 1);
								}
							}
						}
					}
					Set<Entry<Integer, Short>> wordset = distinctWords.entrySet();

					if (m % Math.round(M/50) == 0) 
						System.out.print(".");

					saveSVMlight.saveVar(wordset, directory+"wordsets");
					
					//System.out.println(M+ " " + m + " " + empty_counter);
					
					meta[m]=meta_value;
					m++;
				}
				else {
					meta[M+empty_counter]=meta_value;
					empty_counter++;
				}
			
		}
		
		
		

		System.out.println("");

		documentText.close();

		Save save = new Save();
		save.saveVar(meta, directory+"meta");
		save.close();
		
		//now read the saved files
		readDocs();

		return;

	}


}
