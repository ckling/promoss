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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class LDADataset {
	//---------------------------------------------------------------
	// Instance Variables
	//---------------------------------------------------------------

	public Dictionary dict;			// local dictionary	
	public int M; 			 		// number of documents
	public int V;			 		// number of words

	// map from local coordinates (id) to global ones 
	// null if the global dictionary is not set
	public Map<Integer, Integer> lid2gid; 

	//link to a global dictionary (optional), null for train data, not null for test data 		

	//--------------------------------------------------------------
	// Constructor
	//--------------------------------------------------------------
	public LDADataset(){
		dict = new Dictionary();
		M = 0;
		V = 0;
		docs = null;

		globalDict = null;
		lid2gid = null;
	}


	//-------------------------------------------------------------
	//Public Instance Methods
	//-------------------------------------------------------------
	/**
	 * set the document at the index idx if idx is greater than 0 and less than M
	 * @param str string contains doc
	 * @param idx index in the document array
	 */
	public void setDoc(String str, int idx){

		if (0 <= idx && idx < M){
			String [] words = str.split("[ \\t\\n]");

			Vector<Integer> ids = new Vector<Integer>();

			int position = 0;

			Integer group = null;
			for (String word : words){

				position++;
				if (position == 1) {
					group = Integer.valueOf(word);
				}
				else {
					
					//no coordinate

					int _id = dict.word2id.size();

					if (dict.contains(word))		
						_id = dict.getID(word);

					if (globalDict != null){
						//get the global id					
						Integer id = globalDict.getID(word);
						//System.out.println(id);

						if (id != null){
							dict.addWord(word);

							lid2gid.put(_id, id);
							ids.add(_id);
						}
						else { //not in global dictionary
							//do nothing currently
						}
					}
					else {
						dict.addWord(word);
						ids.add(_id);
					}


				}

			}

			Document doc = new Document(ids, str,group);
			docs[idx] = doc;
			V = dict.word2id.size();			

		}
	}
	//---------------------------------------------------------------
	// I/O methods
	//---------------------------------------------------------------

	/**
	 *  read a dataset from a stream, create new dictionary
	 *  @return dataset if success and null otherwise
	 */
	public static LDADataset readDataSet(String filename){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));

			LDADataset data = readDataSet(reader);

			reader.close();
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}


	/**
	 *  read a dataset from a stream, create new dictionary
	 *  @return dataset if success and null otherwise
	 */
	public static LDADataset readDataSet(BufferedReader reader){
		try {
			//read number of document
			String line;
			line = reader.readLine();
			int M = Integer.parseInt(line);

			LDADataset data = new LDADataset();
			while ((line = reader.readLine()) != null){
				
				data.setDoc(line);
				
			}

			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}


}
