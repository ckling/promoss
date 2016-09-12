package org.gesis.promoss.tools.text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;


public class DMR_Corpus extends Corpus {

	public String metafile;

	
	public int F = 0; //Number of features
	public double[][] meta; //metadata of documents (F)

	

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
		meta = load.readFileDouble2(directory+"meta");
		if (wordsets!=null && meta != null) {

			for (int m=0;m<M;m++) {
				Set<Entry<Integer, Integer>> wordset = wordsets[m];
				for (Entry<Integer,Integer> e : wordset) {
					N[m]+=e.getValue();
				}		
			}	

			return ;
		}

		wordsets = new Set[M];
		meta = new double[M+empty_documents.size()][F];
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
		while ((line = documentText.readLine(documentfile))!=null && (metaline = metaText.readLine(metafile))!=null) {
			line_number++;
			HashMap<Integer,Integer> distinctWords = new HashMap<Integer, Integer>();

			String[] docSplit = line.split(" ");
			String[] metaString = metaline.split(",");

			double[] meta_value = new double[F];
			for (int f=0; f<F; f++) {
				meta_value[f] = Double.valueOf(metaString[f]);
			}

			if (!empty_documents.contains(line_number)) {

				if (docSplit.length>=1) {
					if (processed) {
						for(int i = 0; i < docSplit.length; i++) {
							String word = docSplit[i];
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
					meta[m]=meta_value;
					m++;
				}
				else {
					meta[M+empty_counter]=meta_value;
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
		save.saveVar(meta, directory+"meta");

		return;

	}


}
