package org.gesis.promoss.tools.text;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;


public class Save {	

	private static Text text;
	//here we store the name of the currently open file
	//we have to open a new text object when we see
	//a new filename.
	private static String currentFile;

	public static void main (String[] args) {
		Save save = new Save();
		double[][] test = {{1.0,2.0},{3.0},{4,5}};
		save.saveVar(test, "/home/c/test");
	}

	/**
	 * Save documents in SVMlight format
	 * @param var
	 * @param filename
	 */
	public void saveVar (Set<Entry<Integer, Short>>[] var, String filename) {

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);

		}

	}
	
	
	public void saveVar (Set<Entry<Integer, Short>> var, String filename) {

		checkFile(filename);

		if (var==null || var.isEmpty()) return;
		
		int sum = 0;
		for (Entry<Integer,Short> e : var) {	
			sum += e.getValue();
		}
		//first sign is the sum over the words of the document
		text.write(filename, sum+" ", true);

		
		int i=0;
		for (Entry<Integer,Short> e : var) {	

			i++;
			String key_val = e.getKey() + ":" + e.getValue();
			saveVar(key_val,filename);
			if (i < var.size()) {
				text.write(filename, " ", true);
			}

		}
		text.write(filename, "\n", true);


	}
	
	public void saveVarSet (Set<Entry<Integer, Integer>> var, String filename) {

		checkFile(filename);

		if (var==null || var.isEmpty()) return;
		
		int sum = 0;
		for (Entry<Integer,Integer> e : var) {	
			sum += e.getValue();
		}
		//first sign is the sum over the words of the document
		text.write(filename, sum+" ", true);

		
		int i=0;
		for (Entry<Integer,Integer> e : var) {	

			i++;
			String key_val = e.getKey() + ":" + e.getValue();
			saveVar(key_val,filename);
			if (i < var.size()) {
				text.write(filename, " ", true);
			}

		}
		text.write(filename, "\n", true);


	}
	
	public void saveVarSetString (Set<Entry<String, Integer>> var, String filename) {

		checkFile(filename);

		if (var==null || var.isEmpty()) return;
		
		int sum = 0;
		for (Entry<String,Integer> e : var) {	
			sum += e.getValue();
		}
		//first sign is the sum over the words of the document
		text.write(filename, sum+" ", true);

		
		int i=0;
		for (Entry<String,Integer> e : var) {	

			i++;
			String key_val = e.getKey() + ":" + e.getValue();
			saveVar(key_val,filename);
			if (i < var.size()) {
				text.write(filename, " ", true);
			}

		}
		text.write(filename, "\n", true);


	}




		
	public void saveVar (int[][] var, String filename) {

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);
			text.write(filename, "\n", true);

		}

	}

	public void saveVar (int[] var, String filename) {

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);
			if (i<var.length-1) {
				text.write(filename, ",", true);
			}

		}

	}
	
	
	public void saveVar (float[][] var, String filename) {

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);
			text.write(filename, "\n", true);

		}

	}

	public void saveVar (float[] var, String filename) {

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);
			if (i<var.length-1) {
				text.write(filename, ",", true);
			}

		}

	}
	
	
	public void saveVar (int var, String filename) {

		checkFile(filename);
		text.write(filename, String.valueOf(var), true);

	}
	
	public void saveVar (float var, String filename) {

		checkFile(filename);

		text.write(filename, String.valueOf(var), true);


	}
	
	public void saveVar (double[][] var, String filename) {

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);
			text.write(filename, "\n", true);

		}

	}

	public void saveVar (double[] var, String filename) {	

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);
			if (i<var.length-1) {
			text.write(filename, ",", true);
			}

		}

	}

	public void saveVar (double var, String filename) {

		checkFile(filename);
		text.write(filename, ""+var, true);


	}

	public void saveVar (String[][] var, String filename) {

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);
			text.write(filename, "\n", true);

		}

	}

	public void saveVar (String[] var, String filename) {	

		checkFile(filename);

		for (int i=0;i<var.length;i++)  {

			saveVar(var[i],filename);
			text.write(filename, ",", true);

		}

	}	

	public void saveVar (String var, String filename) {

		checkFile(filename);
		text.write(filename, ""+var, true);


	}

	public void close () {
		text.close();
	}

	private void checkFile(String filename) {
		if (text == null || !filename.equals(currentFile)) {
			text = new Text();
			text.write(filename, "", false);
			currentFile = filename;
		}
	}



	public void saveVar(HashMap<Integer, Integer> hm, String filename) {
		Set<Entry<Integer, Integer>> set = hm.entrySet();
		this.saveVarSet(set,filename);
		
	}

	public void saveVarString(HashMap<String, Integer> hm2, String filename) {
		Set<Entry<String, Integer>> set = hm2.entrySet();
		this.saveVarSetString(set,filename);
				
	}

}
