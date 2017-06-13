package jgibblda;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ckling.text.Text;

import ckling.db.Database;

public class CreateTwitterFileFromDB {

	public static void main (String[] args) throws SQLException, IOException {

		//DELETE FROM twitterS WHERE latitude NOT BETWEEN 47.41005000 AND 55.01670000 OR longitude NOT BETWEEN 5.91667000 AND 14.98290000;
		//INSERT INTO test (SELECT distinct user_id FROM (SELECT user_id FROM twitterS GROUP BY user_id,text HAVING count(*) > 1)b );
		//delete from twitterS WHERE user_id IN (SELECT user_id FROM test);


		Database db = new Database();
		Database db2 = new Database();


		HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
		int idNr = 0;
		Text text = new Text();
		text.setStopwords(false);
		text.setStem(true);
		text.setLang("de");

		double chunkSize = 500;


		int count = 0;
		//File file = new File("/export/ckling/twitterTrink.txt");
		File file = new File("/home/c/twitterTrink.txt");
		FileWriter writer = new FileWriter(file ,true);

		List<String> lines = new LinkedList<String>();

			//GROUP_CONCAT(text  SEPARATOR '\" \"') as
			db.executeQuery("SELECT latitude,longitude, text FROM twitterTrink");


			while(db.rs.next()) {


				count++;
				if (count%1000 == 0) {
					System.out.println(count);
				}

				String message = db.rs.getString("text");
				message = message.trim();
				message = message.replace("\n", " ");
				message = message.replace("&lt;", "<");

				if (message != null) {

					text.setText(message);
					Iterator<String> terms = text.getTerms();
					while (terms.hasNext()) {
						String term = terms.next();
						if (!wordCount.containsKey(term)) {
							wordCount.put(term, 1);
							//System.out.println(term+ " " + idNr);
						}

						int counter = wordCount.get(term);
						wordCount.remove(term);
						wordCount.put(term, counter+1);

					}

				}

			}

		


		System.out.println(count);

		count = 0;

		lines = new LinkedList<String>();

		db.executeQuery("SELECT latitude,longitude, text FROM twitterTrink");
		
			while(db.rs.next()) {

				String message = db.rs.getString("text");
				message = message.trim();
				message = message.replace("\n", " ");
				message = message.replace("&lt;", "<");

				if (message != null) {

					String termString = "" ;

					text.setText(message);
					Iterator<String> terms = text.getTerms();
					while (terms.hasNext()) {
						String term = terms.next();

						if (term.length() > 1) {

							int counter = wordCount.get(term);

							if (counter > 100) {

									termString += " " + term;
							}

						}
					}


					count++;
					if (count%1000 == 0) {
						System.out.println(count);
					}
					if (!termString.equals("")){
						lines.add(db.rs.getString("latitude") + " " + db.rs.getString("longitude") + termString);
					}


				}

			}

		
		System.out.println(count);


		Collections.shuffle(lines); 

		writer.write(String.valueOf(count));
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
