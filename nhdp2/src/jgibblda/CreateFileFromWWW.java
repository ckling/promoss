package jgibblda;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import ckling.text.Text;

public class CreateFileFromWWW {

	public static void main (String[] args) throws IOException {


		int count = 0;

		File file = new File("/home/c/work/manhattan.txt");
		FileWriter writer = new FileWriter(file ,true);

		ArrayList<String> lines = new ArrayList<String>();

		String basedir = "/home/c/ownCloud/papers/spatial/code/www2011/data/manhattan/manhattan";

		Text text = new Text();

		System.out.println("reading locations...");
		text.loadFile(basedir + "_image_location.txt");
		String[] latlon = text.text.split("\n");

		System.out.println("reading tags...");
		text.loadFile(basedir + "_tag_freq.txt");
		String[] tags = text.text.split("\n");
		for (int i = 0; i < tags.length;i++) {
			tags[i] = tags[i].split(" ")[0];
		}
		
		System.out.println("reading docs...");
		text.loadFile(basedir + "_image_tags.txt");
		String[] imageTags = text.text.split("\n");

		for (int i=0; i < imageTags.length; i++) {
					
			String[] idTagFreq= imageTags[i].split(" ");

			int docId = Integer.valueOf(idTagFreq[0]);
			int tagId = Integer.valueOf(idTagFreq[1]);
			int tagCount = Integer.valueOf(idTagFreq[2]);
			String tag = tags[tagId-1];

			String tagString;
			//lines and docid start with 1
			if (lines.size() < docId) {
				tagString = latlon[docId-1];
			}
			else {
				tagString = lines.get(docId-1);
			}

			for (int j = 0; j < tagCount; j++) {
				tagString += " " + tag;
			}

			//lines and docid start with 1
			if (lines.size() < docId) {
				lines.add(tagString);
				System.out.println(count++);
			}
			else {
				lines.remove(docId-1);
				lines.add(tagString);
			}
			
		}


		Collections.shuffle(lines); 

		writer.write(String.valueOf(lines.size()));
		writer.write("\n");
		writer.flush();

		for (Iterator<String> line = lines.iterator(); line.hasNext(); ) {

			writer.write(line.next());
			//no break after last line
			if (line.hasNext()) {
				writer.write("\n");
			}
			writer.flush();

		}

		writer.close();


	}

}
