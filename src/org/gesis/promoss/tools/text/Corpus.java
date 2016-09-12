package org.gesis.promoss.tools.text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.gesis.promoss.tools.math.BasicMath;

public class Corpus {

	public Document[] documents;
	public Dictionary dict;
	public Set<Integer> empty_documents = new HashSet<Integer>();
	
	public String dictfile;
	public String documentfile;

	protected Text documentText;

	//Directory of corpus files
	public String directory = "~/";

	public int MIN_DICT_WORDS = 0;

	//relative size of the training set
	public double TRAINING_SHARE = 1.0;
	
	//document-words (M x doclength)
	public Set<Entry<Integer, Integer>>[] wordsets;
		
	//In case that there is no numeric corpus file
	//What language shall we use for processing?
	public String language = "en";	
	//Should stopwords be removed? (Stopwords from Snowball stemmer)
	public Boolean stopwords = false;
	//Should words be stemmed? (using the Snowball stemmer)
	public Boolean stemming = false;
	//Should words be separated using regular expressions, or is the text processed already?
	public Boolean processed = true;
	//Store some zeros for empty documents in the doc_topic matrix?
	public Boolean store_empty = true;
	

	public int M = 0; //Number of Documents
	public int C = 0; //Number of words in the corpus
	public int V = 0; //Number of distinct words in the corpus, read from dictfile
	public int[] N; //Number of Words per document

	
	
	
	public void readCorpusSize() {
		
		//Try to read parsed documents
		Load load = new Load();
		wordsets = load.readVarSet(directory+"wordsets");
		
		if (wordsets!=null) {
			M = wordsets.length;
			N = new int[M];
			
			for (int m=0;m<M;m++) {
				Set<Entry<Integer, Integer>> wordset = wordsets[m];
				if (wordset.isEmpty()) {
					empty_documents.add(m);
				}
				for (Entry<Integer,Integer> e : wordset) {
					N[m]+=e.getValue();
				}		
				C += N[m];
			}	
			
			
			return;
		}
		
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

			}
		}
		
		N = new int[M];
		
	}

	public void readDict() {
		// Read dictionary from file
		// The file contains words, one in each row

		dict = new Dictionary();
		String line;
		if (!new File(dictfile).exists()) {



			if (!processed && documentText == null) {
				documentText = new Text();
				documentText.setLang(language);
				documentText.setStopwords(stopwords);
				documentText.setStem(stemming);

			}

			//create the dict from all the words in the document
			Text text = new Text();

			HashMap<String,Integer> hs = new HashMap<String,Integer>();

			while((line = text.readLine(documentfile))!=null){

				String[] lineSplit = line.split(" ");
				if (lineSplit.length >= 1) {

					if (processed) {
						for(int i = 0; i < lineSplit.length; i++) {
							String word = lineSplit[i];
							int freq = 1;
							if (hs.containsKey(word)) {
								freq += hs.get(word);
							}

							hs.put(word,freq);

						}
					}
					else {

						documentText.setText(line);

						Iterator<String> words = documentText.getTerms();

						while(words.hasNext()) {
							String word = words.next();
							int freq = 1;
							if (hs.containsKey(word)) {
								freq += hs.get(word);
							}

							hs.put(word,freq);

						}
					}

				}

			}


			text.write(dictfile, "", false);

			Set<Entry<String, Integer>> hses = hs.entrySet();
			Iterator<Entry<String, Integer>> hsit = hses.iterator();
			while(hsit.hasNext()) {
				Entry<String,Integer> e = hsit.next();
				if (e.getValue() >= MIN_DICT_WORDS && e.getKey().length() > 1) {
					text.writeLine(dictfile, e.getKey(), true);
				}
			}

		}
		Text dictText = new Text();

		while((line = dictText.readLine(dictfile)) != null) {

			dict.addWord(line);

		}

	}


	@SuppressWarnings("unchecked")
	public void readDocs() {
		
		if (wordsets!=null) {

			return ;
		}

		wordsets = new Set[M];
		
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

			String[] docSplit = line.split(" ");

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
						documentText.setText(line);
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
					
					m++;
				}
				else {
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

		return;

	}


}
