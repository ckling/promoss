package jgibblda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import ckling.text.Text;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ckling.db.Database;

/*
 * 
 *
CREATE TABLE twitterS (
location VARCHAR(30),
latitude DECIMAL(5,3) NOT NULL,
longitude DECIMAL(6,3) NOT NULL,
id BIGINT UNSIGNED NOT NULL,
from_user VARCHAR(15),
from_user_id BIGINT UNSIGNED,
to_user_id BIGINT UNSIGNED,
created_at TIMESTAMP NOT NULL,
iso_language_code VARCHAR(2),
text VARCHAR(140),
type TINYINT DEFAULT 0,
PRIMARY KEY(id)
)
ENGINE = MYISAM
;

SELECT latitude, longitude, text FROM twitterS WHERE 
text like "% esse%" OR
text like "% trinke%" OR
text like "%döner%" OR
text like "%spaghetti%" OR
text like "%nudeln%" OR
text like "%schnitzel%" OR
text like "%wurst%" OR
text like "%fleisch%" OR
text like "%brötchen%" OR
text like "%pizza%" OR
text like "%kartoffel%" OR
text like "% reis %" OR
text like "%fisch%" OR
text like "%steak%" OR
text like "%brot%" OR
text like "%schok%" OR
text like "%tee%" OR
text like "%kaffee%" OR
text like "%cola%" OR
text like "%bier%" OR
text like "%weisswein%" OR
text like "%weißwein%" OR
text like "%rotwein%" OR
text like "% wein %" OR
text like "% lecker%" OR
text like "%schnaps%" OR
text like "%wodka%" OR
text like "%espresso%" OR
text like "%cappuchino%" OR
text like "%fanta%" OR
text like "%orange%" OR
text like "%banane%" OR
text like "%apfel%" OR
text like "%birne%" OR 
text like "%milch%" OR 
text like "%wasser%" OR
text like "%saft%" OR
text like "%mehl%" OR 
text like "%zucker%" OR 
text like "%macchiato%" OR 
text like "%kakao%" OR 
text like "%salz%" OR 
text like "%brezel%" OR 
text like "%croissant%" OR 
text like "%marmelade%" OR 
text like "%honig%"


GROUP BY from_user_id;
 * 
 */

// SELECT text FROM twitterS WHERE text like "%%" GROUP BY from_user_id;

public class CreateDBFromJSON {

	private static GregorianCalendar calendar = new GregorianCalendar();
	private static TimeZone utc = TimeZone.getTimeZone("UTC");
	private static SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
	private static SimpleDateFormat mysqlFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public static void main (String[] args) throws IOException {

		Database db = new Database();

		int count = 0;
		int count2 = 0;

		FileReader reader = new FileReader("/media/500/work/twitter/Twitter20022012.txt");
		BufferedReader bReader = new BufferedReader(reader);
		
		String line;
		while((line = bReader.readLine()) != null) {
			
			if (line.startsWith("{")) {
			
			try {
				
							
				JSONObject jso = new JSONObject(line);
				String[] latlon = jso.getString("refresh_url").split("&")[2].substring(8).split("%2C");
				String latQuery = latlon[0];
				String lonQuery = latlon[1];
				
				JSONArray jsa = jso.getJSONArray("results");
				
				int length = jsa.length();
				count +=length;
				if (count2++%100==0)
					System.out.println(count);
				
				if (count > 27069000) {
				for (int i=0;i<length;i++) {
					
					boolean write = true;
					
					
					String latPost = latQuery;
					String lonPost = lonQuery;
									
					JSONObject post = jsa.getJSONObject(i);
					
					if (post.getString("iso_language_code").equals("de")) {
					
					String id = post.getString("id");
					String from_user_id = post.getString("from_user_id");
					String text = post.getString("text");
					text = text.trim();
					if (text.startsWith("@")) {
						String[] textSplit = text.split(" ",2);
						if (textSplit.length > 1) {
							text = textSplit[1];
						}
						else {
							write = false;
						}
					}

					text = StringEscapeUtils.unescapeHtml(text);
					text = text.replace("\\", "");
					text = text.replace("'", "");
					text = text.substring(0,Math.min(140, text.length()));
					
//					//transfer date from RFC 2822 to a mySQL format
//					calendar.setTime(format.parse(post.getString("created_at")));
//
//					//add summer time
//					calendar.add(GregorianCalendar.MILLISECOND, calendar.get(GregorianCalendar.DST_OFFSET));
//					String created_at = mysqlFormat.format(calendar.getTime());
					
					if (!post.isNull("geo")) {

						JSONObject geo =post.getJSONObject("geo");
						if (geo.getString("type").equals("Point")) {

							JSONArray coordinates = geo.getJSONArray("coordinates");
							double latPost2 = coordinates.getDouble(0);
							double lonPost2 = coordinates.getDouble(1);
							if (latPost2 < 46.8 || latPost2 > 55 || lonPost2 < 5.9 || lonPost2 > 14.96) {
								write = false;
							}
							else {
								latPost = String.valueOf(latPost2);
								lonPost = String.valueOf(lonPost2);
							}

						}
					}
					
					//searchParameters.setBBox("5.9","46.8","14.96","55");
					if (write && !text.equals(""))
					
					db.addValue("id",id);
					db.addValue("from_user_id",from_user_id);
					db.addValue("longitude",lonPost);
					db.addValue("latitude",latPost);
					db.addValue("text",text);
//					db.addValue("created_at",created_at);
					db.updateInto("insert delay","twitterS");
					
//					System.out.println(id + " " + text);
					
					}
				}
			
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			}
			
			
			
		}
		



	}

}
