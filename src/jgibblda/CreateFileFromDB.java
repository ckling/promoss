package jgibblda;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ckling.db.Database;

public class CreateFileFromDB {

	public static void main (String[] args) throws SQLException, IOException {

		Database db = new Database();
		Database db2 = new Database();

		int count = 0;
		String last = null;
		File file = new File("/media/500/flickrPhoto3.txt");
		FileWriter writer = new FileWriter(file ,true);

		//db.executeQuery("SELECT GROUP_CONCAT(FLICKR_ID SEPARATOR '\",\"'), LATITUDE, LONGITUDE FROM flickrPhoto WHERE LATITUDE BETWEEN 51.261915 AND 51.665741 AND LONGITUDE BETWEEN -0.570374 AND  0.308533 GROUP BY USER_ID,DATE(DATE_TAKEN) ORDER BY USER_ID");
		//db.executeQuery("SELECT GROUP_CONCAT(FLICKR_ID SEPARATOR '\",\"'), LATITUDE, LONGITUDE FROM flickrPhoto GROUP BY USER_ID,DATE(DATE_TAKEN)");
		//db.executeQuery("SELECT FLICKR_ID, LATITUDE, LONGITUDE FROM flickrPhoto WHERE LATITUDE BETWEEN 51.261915 AND 51.665741 AND LONGITUDE BETWEEN -0.570374 AND  0.308533 GROUP BY USER_ID,DATE(DATE_TAKEN) ORDER BY USER_ID");
		//db.executeQuery("SELECT FLICKR_ID, LATITUDE, LONGITUDE FROM flickrPhoto GROUP BY USER_ID,DATE(DATE_TAKEN) ORDER BY USER_ID");
		db.executeQuery("SELECT FLICKR_ID, LATITUDE, LONGITUDE FROM flickrPhoto JOIN flickrTagAs ON(PHOTO = FLICKR_ID) WHERE TAG='geotagged' ORDER BY USER_ID, DATE_TAKEN");
		//db.executeQuery("SELECT FLICKR_ID, LATITUDE, LONGITUDE FROM flickrPhoto WHERE LATITUDE BETWEEN 51.261915 AND 51.665741 AND LONGITUDE BETWEEN -0.570374 AND  0.308533 ORDER BY USER_ID,DATE_TAKEN");
		
		
		List<String> lines = new LinkedList<String>();
		
		while(db.rs.next()) {

			if (true) {
				
				db2.executeQuery("SELECT GROUP_CONCAT(DISTINCT a.TAG SEPARATOR ' ') FROM flickrTagAs a JOIN flickrTags USING(TAG) WHERE PHOTO = "+db.rs.getString(1)+" GROUP BY PHOTO HAVING COUNT(DISTINCT TAG) > 0");

				if (db2.rs.next()) {
					
					String tags = db2.rs.getString(1);

					if (! tags.equals(last)) {
						if (++count % 100 == 0)
							System.out.println(count);
						//System.out.println(tags + "\n" + last + " \n ");
						lines.add(db.rs.getString(2) + " " + db.rs.getString(3) + " " + tags);
					
					last = tags;
					}
					
				}
				
			}
			else {
			
			if (!db.rs.getString(1).endsWith("\"") && !db.rs.getString(1).endsWith(",")) {

				db2.executeQuery("SELECT GROUP_CONCAT(DISTINCT a.TAG SEPARATOR ' ') FROM flickrTagAs a JOIN flickrTags USING(TAG) WHERE PHOTO IN (\""+db.rs.getString(1)+"\") GROUP BY PHOTO HAVING COUNT(DISTINCT TAG) > 2");

				if (db2.rs.next()) {
					
					String tags = db2.rs.getString(1);

					if (! tags.equals(last)) {
						System.out.println(++count);
						//System.out.println(tags + "\n" + last + " \n ");
						lines.add(db.rs.getString(2) + " " + db.rs.getString(3) + " " + tags);
					
					last = tags;
					}
					
				}

			}
			
			}

		}
		
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
