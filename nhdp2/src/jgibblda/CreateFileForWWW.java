package jgibblda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import ckling.text.Text;

public class CreateFileForWWW {

	public static void main (String[] args) throws IOException {


		BufferedReader reader = new BufferedReader (new FileReader("/home/c/work/jgibblda/models/test/test.txt"));		
		HashMap<String, Integer> terms = new HashMap<String, Integer>(10000000);


		File file = new File("/home/c/work/test_image_tags.txt");
		FileWriter writer = new FileWriter(file ,true);
		File fileLoc = new File("/home/c/work/test_image_locations.txt");
		FileWriter writerLoc = new FileWriter(fileLoc ,true);
		File fileTag = new File("/home/c/work/test_tag_freq.txt");
		FileWriter writerTag = new FileWriter(fileTag ,true);

		String line="";
		String[] lineSplit;
		int count = 0;
		int countTerms =0;
		//read first line and ignore it
		reader.readLine();
		while ((line = reader.readLine()) != null) {
			System.out.println(count++);

			lineSplit = line.split(" ");
			writerLoc.write(lineSplit[0]+ " " + lineSplit[1]+"\n");
			writerLoc.flush();

			String termString = "";

			for (int i=2;i<lineSplit.length;i++) {		
				String word = lineSplit[i];
				int id = -1;
				if (terms.containsKey(word))
					id = terms.get(word);
				else {
					terms.put(word, ++countTerms);
					writerTag.write(word+"\n");
					writerTag.flush();
					id = countTerms;
				}
				if (termString.isEmpty()) {
					termString = String.valueOf(id);
				}
				else {
					termString += " " + id;
				}
			}
			
			writer.write(termString+"\n");
			writer.flush();
			
		}


		writer.write("\n");
		writer.flush();

		writer.close();
		writerLoc.close();
		writerTag.close();

	}

}
