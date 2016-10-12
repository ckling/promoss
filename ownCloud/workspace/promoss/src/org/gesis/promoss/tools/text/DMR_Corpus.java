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
		String firstLine = Text.readLineStatic(metafile);
		//File e.g. looks like groupID1,groupID2,groupID3,groupID4 word1 word2 word3
		F = firstLine.split(" ")[0].split(",").length;
	}
	
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

			}
		}
		
		N = new int[M];
		
	}

	@SuppressWarnings("unchecked")
	public void readDocs() {

		//Try to read parsed documents
		Load load = new Load();

		meta = load.readFileDouble2(directory+"meta");
		//Try to read parsed documents
		if (load.readSVMlight(directory+"wordsets", this) && meta != null) {			
			return;
		}
		
		termIDs = new int[M][];
		termFreqs = new short[M][];
		
		Save saveSVMlight = new Save();

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
			HashMap<Integer,Short> distinctWords = new HashMap<Integer, Short>();

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
									distinctWords.put(wordID, (short) (count+1));
								}
								else {
									distinctWords.put(wordID, (short) 1);
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

		documentText.close();

		Save save = new Save();
		save.saveVar(meta, directory+"meta");
		save.close();
		
		//now read the saved files
		readDocs();

		return;

	}


}
