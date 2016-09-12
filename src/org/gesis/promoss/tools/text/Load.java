package org.gesis.promoss.tools.text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

public class Load {
	
	private static Text text;
	//here we store the name of the currently open file
	//we have to open a new text object when we see
	//a new filename.
	private static String currentFile;
	
	

	/**
	 * Read document from SVMlight format
	 * @param filename
	 * @return
	 */
	public Set<Entry<Integer, Integer>>[] readVarSet (String filename) {
		if (! new File(filename).exists()) return null;

		checkFile(filename);
		String line;

		int N=0;
		while((line = text.readLine(filename))!=null) {
			N++;
		}
		@SuppressWarnings("unchecked")
		Set<Entry<Integer, Integer>>[] var = new Set[N];
		checkFile(filename);
		int n=0;
		while((line = text.readLine(filename))!=null) {
			HashMap<Integer,Integer> distinctWords = new HashMap<Integer, Integer>();
			if(!line.equals("")) {
				String[] lineSplit = line.split(" ");
				//first we have the total number of words, which we ignoe
				for (int i=1;i<lineSplit.length;i++) {
					String word = lineSplit[i];
					String[] key_val = word.split(":");
					String key = key_val[0];
					String val = key_val[1];
					distinctWords.put(Integer.valueOf(key),Integer.valueOf(val));
				}
			}
			var[n]=distinctWords.entrySet();
			n++;
		}
		return var;

	}


	public int[][] readVarInt (String filename) {
		if (! new File(filename).exists()) return null;

		checkFile(filename);
		String line;

		int N=0;
		while((line = text.readLine(filename))!=null) {
			N++;
		}
		int[][] var = new int[N][];
		checkFile(filename);
		int n=0;
		while((line = text.readLine(filename))!=null) {
			String[] lineSplit = line.split(",");
			var[n]=new int[lineSplit.length];
			for (int i=0;i<lineSplit.length;i++) {
				var[n][i]=Integer.valueOf(lineSplit[i]);
			}
			n++;
		}
		return var;

	}
	
	public String[] readFileString1(String filename) {
		
		if (checkFile(filename)) {
		
		String line;
		ArrayList<String> al = new ArrayList<String>();
		
		while((line = text.readLine(filename))!=null) {
			al.add(line);
		}

		String[] result = new String[al.size()];
		for (int i=0;i<al.size();i++) {
			result[i]=al.get(i);
		}
		
		return result;
		}
		else {
			return null;
		}
	}
	
	public String[][] readFileString2(String filename,String separator) {
		
		if (checkFile(filename)) {
		
		String line;
		ArrayList<String[]> al = new ArrayList<String[]>();
		
		while((line = text.readLine(filename))!=null) {
			String[] lineSplit = line.split(separator);
			al.add(lineSplit);
		}

		String[][] result = new String[al.size()][];
		for (int i=0;i<al.size();i++) {
			result[i]=al.get(i);
		}
		
		return result;
		}
		else {
			return null;
		}
	}
	
	public double[][] readFileDouble2(String filename) {
		
		if (checkFile(filename)) {
		
		String line;
		ArrayList<double[]> al = new ArrayList<double[]>();
		
		while((line = text.readLine(filename))!=null) {
			String[] lineSplit = line.split(",");
			double[] numbers = new double[lineSplit.length];
			for (int i=0;i<lineSplit.length;i++) {
				numbers[i]=Double.valueOf(lineSplit[i]);
			}
			al.add(numbers);

			 
		}

		double[][] result = new double[al.size()][];
		for (int i=0;i<al.size();i++) {
			result[i]=al.get(i);
		}
		
		return result;
		}
		else {
			return null;
		}
	}
	
	public double[] readFileDouble1(String filename) {
		
		if(checkFile(filename)) {
		
		String line;
		line = text.readLine(filename);
		if (line == null) return null;
		
		String[] lineSplit = line.split(",");
		double[] numbers = new double[lineSplit.length];
		for (int i=0;i<lineSplit.length;i++) {
			numbers[i]=Double.valueOf(lineSplit[i]);
		}
				
		return numbers;
		}
		else {
			return null;
		}
	}
	
	public int[][] readFileInt2(String filename) {
		
		if (checkFile(filename)) {
		
		String line;
		ArrayList<int[]> al = new ArrayList<int[]>();
		
		while((line = text.readLine(filename))!=null) {
			if (line.equals("")) {
				int[] numbers = new int[0];
				al.add(numbers);
			}
			else {
			String[] lineSplit = line.split(",");
			int[] numbers = new int[lineSplit.length];
			for (int i=0;i<lineSplit.length;i++) {
				numbers[i]=Integer.valueOf(lineSplit[i]);
			}
			al.add(numbers);
			}
			 
		}

		int[][] result = new int[al.size()][];
		for (int i=0;i<al.size();i++) {
			result[i]=al.get(i);
		}
		
		return result;
		}
		else {
			return null;
		}
	}
	
	public int[] readFileInt1(String filename) {
		
		if (checkFile(filename)) {
		
		String line;
		line = text.readLine(filename);
		if (line == null) return null;
		
		String[] lineSplit = line.split(",");
		int[] numbers = new int[lineSplit.length];
		for (int i=0;i<lineSplit.length;i++) {
			numbers[i]=Integer.valueOf(lineSplit[i]);
		}
				
		return numbers;
		}
		else {
			return null;
		}
	}
	

	private boolean checkFile(String filename) {
		if (!new File(filename).exists()) {
			return false;
		}
		if (text == null || !filename.equals(currentFile)) {
			text = new Text();
			currentFile = filename;
		}
		return true;
	}
		
	public void close () {
		text.close();
	}
}
