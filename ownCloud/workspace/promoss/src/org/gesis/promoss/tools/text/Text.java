package org.gesis.promoss.tools.text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.gesis.promoss.tools.db.Database;
import org.tartarus.snowball.SnowballStemmer;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.cyberneko.html.parsers.DOMFragmentParser;


//import ckling.db.Database;


public class Text {


	public String text;
	public boolean stopwords = true;
	public boolean stem = true;
	public String lang;
	private BufferedReader bufferedReader = null;
	private BufferedWriter bufferedWriter = null;

	private static Pattern[] p = new Pattern[2];

	private Pattern stopword = null;
	private SnowballStemmer stemmer = null;

	private static Database db = null;

	public Text () {
	}
	public Text(String input) {
		setText(input);
	}
	


	public String readLine(String fileLocation) {

		try {

			if (bufferedReader == null) {

				bufferedReader = new BufferedReader (new FileReader(fileLocation));

			}

			String line = "";
			if ((line = bufferedReader.readLine())!=null) {
				return line;
			}
			else {
				this.closeReader();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	public static String readLineStatic(String fileLocation) {

		try {

			
			BufferedReader bufferedReader = new BufferedReader (new FileReader(fileLocation));

			

			String line = "";
			if ((line = bufferedReader.readLine())!=null) {
				bufferedReader.close();
				return line;
			}
			else {
				bufferedReader.close();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	public static String readFileStatic(String fileLocation) {

		try {
			FileReader fr = new FileReader(fileLocation);
			BufferedReader bufferedReader = new BufferedReader (fr);
	        StringBuilder sb = new StringBuilder();
			String line = "";
			if ((line = bufferedReader.readLine())!=null) {
				sb.append(line);
				sb.append(System.lineSeparator());
			}
			else {
				bufferedReader.close();
			}
			
			return sb.toString();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}

	public void loadFile (String fileLocation) {

		try {

			String output = "";

			BufferedReader reader;
			reader = new BufferedReader (new FileReader(fileLocation));

			String line;
			boolean first = true;
			while ((line = reader.readLine()) != null) {

				if (first) first=false;
				else output+="\n";

				output += line;

			}

			this.setText(output);

			reader.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean loadUrl(String site, boolean save) throws Exception {
		String user_agent = "University of Koblenz-Landau Crawler";
		return loadUrl(site, save, user_agent);
	}

	public boolean loadUrl(String site, String user_agent) throws Exception {
		return loadUrl(site, false, user_agent);
	}

	public boolean loadUrl(String site, boolean save, String user_agent) throws Exception
	{

		if (user_agent.toLowerCase().equals("mozilla")) {
			user_agent="Mozilla/5.0 (Windows NT 5.1; rv:10.0.2) Gecko/20100101 Firefox/10.0.2";
		}
		
		String urlDB = site;
		if (site.length()>255) {
			urlDB = site.substring(0,255);
		}

		if (save) {
			
			if (db==null) db = new Database();
			db.executeQuery("SELECT * FROM site WHERE url = '" + urlDB +"'");	

			if (db.rs.next()) {
				text = db.rs.getString("html");
				return false;
			}
		}

		StringBuffer buf;

		URL url = new URL(site);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setAllowUserInteraction(false);
		conn.setDoOutput(true);
		conn.addRequestProperty("User-Agent",user_agent);
		conn.addRequestProperty("Accept-Language","en-us,en,de;q=0.5");
		conn.addRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Charset", "UTF-8");
		conn.addRequestProperty("Connection","keep-alive");
				
		conn.connect();
		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
		{
			System.out.println(conn.getResponseMessage());
			text = null;
			return true;
		}

		int responseCode = conn.getResponseCode();

		int retryCounter = 0;
		while (responseCode != 200) {

			if (responseCode == 420) {

				//get, how many seconds we should wait
				int wait;
				if (conn.getHeaderField("Retry-After") != null) { 
					wait = Integer.valueOf(conn.getHeaderField("Retry-After"));
					if (wait >= 60*60) wait = 2*60*60;
				}
				else {
					wait = 5*60;
				}
				//GregorianCalendar now=new GregorianCalendar();	
				//System.out.println("Limit: " + count + ", time: " + format.format(now.getTime()) + " retry after: " + wait);
				Thread.sleep(1000*wait + 1000);
				//no success due to the rate limit


			}

			else if (responseCode == 502 || responseCode == 503 || responseCode == 500) {


				//System.out.println("Fail at " + count + ", time: " + format.format(now.getTime()));
				retryCounter++;
				//retry 1 times
				if (retryCounter >= 1) text = null;

			}
			if (responseCode == 403) {

				System.out.println(conn.getResponseCode());

			}


			//if result not modified or some other errors
			else {
				text = null;
				return true;
			}


		}


		String contentType = conn.getContentType();
		String[] values = contentType.split(";"); // values.length should be 2
		String charset = "";

		for (String value : values) {
		    value = value.trim();

		    if (value.toLowerCase().startsWith("charset=")) {
		        charset = value.substring("charset=".length());
		    }
		}

		if (charset.equals("")) {
		    charset = "UTF-8"; //Assumption
		}
		
		System.out.println(charset);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(conn
				.getInputStream(),charset));
		buf = new StringBuffer();

		char[] c = new char[50000];
		int numChars = br.read(c);
		while (numChars > 0)
		{
			buf.append(c, 0, numChars);
			numChars = br.read(c);
		}
		br.close();
		br = null;

		
		
		conn.disconnect();
		conn = null;

		//TODO: REMOVE after crawling News!
		//String htmlstring = buf.toString().replace("\\","");
		
		if (save) {
			if (db==null) db = new Database();
			db.addValue("url", urlDB);
			db.addValue("html", buf.toString());
			db.updateInto("INSERT IGNORE", "site");
		}

		text = buf.toString();
		return true;
	}

	public boolean downloadFile(String address, String outfile) throws Exception
	{

		String user_agent="Mozilla/5.0 (Windows NT 5.1; rv:10.0.2) Gecko/20100101 Firefox/10.0.2";


		URL url = new URL(address);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setAllowUserInteraction(false);
		conn.setDoOutput(true);
		conn.addRequestProperty("User-Agent",user_agent);
		conn.addRequestProperty("Accept-Language","en-us,en;q=0.5");
		conn.addRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.addRequestProperty("Connection","keep-alive");

		conn.connect();
		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
		{
			System.out.println(conn.getResponseMessage());
			text = null;
			return true;
		}

		int responseCode = conn.getResponseCode();

		while (responseCode != 200) {

			if (responseCode == 420) {

				//get, how many seconds we should wait
				int wait;
				if (conn.getHeaderField("Retry-After") != null) { 
					wait = Integer.valueOf(conn.getHeaderField("Retry-After"));
					if (wait >= 60*60) wait = 2*60*60;
				}
				else {
					wait = 5*60;
				}
				//GregorianCalendar now=new GregorianCalendar();	
				//System.out.println("Limit: " + count + ", time: " + format.format(now.getTime()) + " retry after: " + wait);
				Thread.sleep(1000*wait + 1000);
				//no success due to the rate limit


			}

			else if (responseCode == 502 || responseCode == 503 || responseCode == 500) {


				//System.out.println("Fail at " + count + ", time: " + format.format(now.getTime()));
				//retry 1 times
				return false;

			}
			if (responseCode == 403) {

				System.out.println(conn.getResponseCode());

			}
		}

		InputStream in = conn.getInputStream();

		FileOutputStream fos = new java.io.FileOutputStream(outfile);
		int oneChar;
		while ((oneChar=in.read()) != -1)
		{
			fos.write(oneChar);
		}
		in.close();
		fos.close();


		conn.disconnect();
		conn = null;

		return true;


	}

	public String getText() {	
		return text;		
	}


	public void setText(String input) {
		this.text = input;
	}

	public void setStopwords(boolean input) {
		stopwords = input;		
	}
	public void setStem(boolean input) {
		stem = input;		
	}

	public void write (String dest, String content, Boolean append) {

		try {

			if (bufferedWriter == null) {
				OutputStreamWriter fw;
				fw = new OutputStreamWriter(new FileOutputStream(dest,append), "UTF-8");
				bufferedWriter = new BufferedWriter(fw); 
			}

			bufferedWriter.write(content);
			bufferedWriter.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public void write (String dest, String content) {

		try {

			if (bufferedWriter == null) {
				OutputStreamWriter fw;
				fw = new OutputStreamWriter(new FileOutputStream(dest,false), "UTF-8");
				bufferedWriter = new BufferedWriter(fw); 
			}

			bufferedWriter.write(content);

			this.closeWriter();

		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public void writeLine (String dest, String content, Boolean append) {
		content+="\n";
		this.write(dest,content,append);
	}

	public void write (String dest) {

		try {

			if (bufferedWriter == null) {
				File outputFile = new File(dest); ; 
				FileWriter fw;
				fw = new FileWriter(outputFile, false);
				bufferedWriter = new BufferedWriter(fw); 
			}

			bufferedWriter.write(text);

			this.closeWriter();

		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public void close () {
		try {
			if (bufferedReader!=null) {
				bufferedReader.close();
				bufferedReader = null;
			}
			if (bufferedWriter!=null) {
				bufferedWriter.close();
				bufferedWriter = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeWriter () {
		try {
			if (bufferedWriter!=null) {
				bufferedWriter.close();
				bufferedWriter = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeReader () {
		try {
			if (bufferedReader!=null) {
				bufferedReader.close();
				bufferedReader = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void write (String dest, Boolean append) {

		try {
			File outputFile = new File(dest); ; 
			FileWriter fw;
			fw = new FileWriter(outputFile, append);


			BufferedWriter bw = new BufferedWriter(fw); 

			bw.write(text);

			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public void setLang(String input) {

		//filter out these
		p[0] = Pattern.compile("^([ \\t\\n\\x0B\\f\\r\"]+|(http|https|ftp)\\://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(:[a-zA-Z0-9]*)?/?([a-zA-Z0-9\\-\\._\\?\\,\\'/\\\\\\+&amp;%\\$#\\=~])*|[?!\\.,]+)");
		//keep normal words, smilies, numbers
		p[1] = Pattern.compile("^((:D|:-D|:P|:-P|:O|:-O|B\\)|B-\\)|:S|:-S|:X|:-X|XD|xD|X-D|X-\\)|8\\)|8-\\)|X\\(|X-\\(|:d|:-d|n8|w8|n1)|[0-9]([\\.:]?[0-9]+)?|[#@]?[_a-zA-ZÀ-ÖØ-öø-ž0-9']+|[‘‚¨\\\\’ ‘•0-9~®‹›*—´«»`@ł€¶ŧ←↓→øþæſðđŋħł»«¢„“”µ°!\"§$%&/()=?'_:;>¹²³¼½¬{\\[\\]}–…·|<,.\\-#+'\\^]+)");

		//p[0] = Pattern.compile("^[ \\t\\n\\x0B\\f\\r]+");
		//p[1] = Pattern.compile("^(http|https|ftp)\\://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(:[a-zA-Z0-9]*)?/?([a-zA-Z0-9\\-\\._\\?\\,\\'/\\\\\\+&amp;%\\$#\\=~])*");
		//p[2] = Pattern.compile("^[?!.]+");
		//p[3] = Pattern.compile("^(:D|:-D|:P|:-P|:O|:-O|B\\)|B-\\)|:S|:-S|:X|:-X|XD|xD|X-D|X-\\)|8\\)|8-\\)|X\\(|X-\\(|:d|:-d|n8|w8|n1)");
		//p[4] = Pattern.compile("^[0-9]([\\.:]?[0-9]+)?");
		//p[5] = Pattern.compile("^[#@]?[_a-zA-ZÀ-ÖØ-öø-ž0-9']+");
		//p[6] = Pattern.compile("^[‘‚¨\\\\’ ‘•0-9~®‹›*—´«»`@ł€¶ŧ←↓→øþæſðđŋħł»«¢„“”µ°!\"§$%&/()=?'_:;>¹²³¼½¬{\\[\\]}–…·|<,.\\-#+'\\^]+");

		lang = input;		
		if (lang.equals("de")) {
			stopword = Pattern.compile("^(aber|alle|allem|allen|aller|alles|als|also|am|an|ander|andere|anderem|anderen|anderer|anderes|anderm|andern|anderr|anders|auch|auf|aus|bei|bin|bis|bist|da|damit|dann|der|den|des|dem|die|das|dass|daß|derselbe|derselben|denselben|desselben|demselben|dieselbe|dieselben|dasselbe|dazu|dein|deine|deinem|deinen|deiner|deines|denn|derer|dessen|dich|dir|du|dies|diese|diesem|diesen|dieser|dieses|doch|dort|durch|ein|eine|einem|einen|einer|eines|einig|einige|einigem|einigen|einiger|einiges|einmal|er|ihn|ihm|es|etwas|euer|eure|eurem|euren|eurer|eures|für|gegen|gewesen|hab|habe|haben|hat|hatte|hatten|hier|hin|hinter|ich|mich|mir|ihr|ihre|ihrem|ihren|ihrer|ihres|euch|im|in|indem|ins|ist|jede|jedem|jeden|jeder|jedes|jene|jenem|jenen|jener|jenes|jetzt|kann|kein|keine|keinem|keinen|keiner|keines|können|könnte|machen|man|manche|manchem|manchen|mancher|manches|mein|meine|meinem|meinen|meiner|meines|mit|muss|musste|nach|nicht|nichts|noch|nun|nur|ob|oder|ohne|sehr|sein|seine|seinem|seinen|seiner|seines|selbst|sich|sie|ihnen|sind|so|solche|solchem|solchen|solcher|solches|soll|sollte|sondern|sonst|über|um|und|uns|unse|unsem|unsen|unser|unses|unter|viel|vom|von|vor|während|war|waren|warst|was|weg|weil|weiter|welche|welchem|welchen|welcher|welches|wenn|werde|werden|wie|wieder|will|wir|wird|wirst|wo|wollen|wollte|würde|würden|zu|zum|zur|zwar|zwischen)$");
			stemmer = new org.tartarus.snowball.ext.germanStemmer();
		}
		else if (lang.equals("en")) {
			stopword = Pattern.compile("^(i|me|my|myself|we|us|our|ours|ourselves|you|your|yours|yourself|yourselves|he|him|his|himself|she|her|hers|herself|it|its|itself|they|them|their|theirs|themselves|what|which|who|whom|this|that|these|those|am|is|are|was|were|be|been|being|have|has|had|having|do|does|did|doing|would|could|should|ought|might|however|will|would|shall|should|can|could|may|might|must|ought|i'm|you're|he's|she's|it's|we're|they're|i've|you've|we've|they've|i'd|you'd|he'd|she'd|we'd|they'd|i'll|you'll|he'll|she'll|we'll|they'll|isn't|aren't|wasn't|weren't|hasn't|haven't|hadn't|doesn't|don't|didn't|won't|wouldn't|shan't|shouldn't|can't|cannot|couldn't|mustn't|let's|that's|who's|what's|here's|there's|when's|where's|why's|how's|daren't|needn't|oughtn't|mightn't|a|an|the|and|but|if|or|because|as|until|while|of|at|by|for|with|about|against|between|into|through|during|before|after|above|below|to|from|up|down|in|out|on|off|over|under|again|further|then|once|here|there|when|where|why|how|all|any|both|each|few|more|most|other|some|such|no|nor|not|only|own|same|so|than|too|very)$");
			stemmer = new org.tartarus.snowball.ext.englishStemmer();
		}
		else if (lang.equals("du")) {
			stopword = Pattern.compile("");
			stemmer = new org.tartarus.snowball.ext.dutchStemmer();
		}
		else {
			stopword = null;
			//english as standard
			stemmer = new org.tartarus.snowball.ext.englishStemmer();
		}
	}

	public Iterator<String> getTerms() {

		if (lang == null) setLang("en");

		ArrayList<String> words = new ArrayList<String>();

		/*
		if (terms.containsKey("test"))
			System.out.println(terms.get("test"));
		else {
			terms.put("test", 1);
		}

		if (terms.containsKey("test"))
			System.out.println(terms.get("test"));
		else {
			terms.put("test", 1);
		}

		if (1==1)	
		return;
		 */

		String content = text;
		String word = "";
		Matcher matcher;
		MatchResult matchResult = null;
		int i = 0;


		//System.out.println(db.rs.getString("content"));


		while (content != null && content.length() > 0) {

			for (i = 0; i < p.length;i++) {

				matcher = p[i].matcher(content);

				if (matcher.find()) {

					matchResult = matcher.toMatchResult();
					word = matchResult.group();
					content = content.substring(matchResult.end());
					word = word.toLowerCase();

					break;
				}

			}

			if (i < p.length) {

				if (i >= 1) {

					if (	! stopwords || 
							! stopword.matcher(word).matches()) {

						if (stem) {
							stemmer.setCurrent(word);
							stemmer.stem();
							word = stemmer.getCurrent();
						}

						if (word.length() > 32) { word = word.substring(0,32); }

						words.add(word);

					}
				}

			}
			else {
				content = content.substring(1);
			}

		}

		//System.out.println();

		Iterator<String> output = words.iterator();

		return output;
	}

	public DocumentFragment getXML () throws SAXException, IOException {

		DOMFragmentParser parser = new DOMFragmentParser();
		CoreDocumentImpl codeDoc = new CoreDocumentImpl();
		DocumentFragment doc = codeDoc.createDocumentFragment();

		InputSource source = new InputSource(new StringReader(text));
		parser.parse(source, doc);

		return doc;

	}

	public String stem (String word) {
		
		word = word.trim();
		
		if (	! stopwords || 
				! stopword.matcher(word).matches()) {

			if (stem) {
				stemmer.setCurrent(word);
				stemmer.stem();
				word = stemmer.getCurrent();
			}

			if (word.length() > 32) { word = word.substring(0,32); }

			return(word);

		}
		return null;
		
	}
	
	public ArrayList<Node> searchTag(String searchtag) throws SAXException, IOException {

		NodeList nl = this.getXML().getChildNodes();

		ArrayList<Node> al = new ArrayList<Node>();

		searchtag = searchtag.toLowerCase();

		for (int i=0; i<nl.getLength();i++) {
			Node item = nl.item(i);
			if(item.getNodeName().toLowerCase().equals(searchtag)) {
				al.add(item);
			}
			if (item.hasChildNodes()) {
				al.addAll(searchTag(item.getChildNodes(),searchtag));
			}
		}

		return al;

	}

	public ArrayList<Node> searchTag(NodeList nl, String searchtag) {

		ArrayList<Node> al = new ArrayList<Node>();

		searchtag = searchtag.toLowerCase();

		for (int i=0; i<nl.getLength();i++) {
			Node item = nl.item(i);
			if(item.getNodeName().toLowerCase().equals(searchtag)) {
				al.add(item);
			}
			if (item.hasChildNodes()) {
				al.addAll(searchTag(item.getChildNodes(),searchtag));
			}
		}

		return al;

	}

	public ArrayList<Node> searchTag(NodeList nl, String searchtag, String attributeName, String attributeValue) {

		ArrayList<Node> al = new ArrayList<Node>();

		searchtag = searchtag.toLowerCase();

		for (int i=0; i<nl.getLength();i++) {
			Node item = nl.item(i);

			if(item.getNodeName().toLowerCase().equals(searchtag)) {
				NamedNodeMap attributes = item.getAttributes();
				Node attribute = attributes.getNamedItem(attributeName);
				if (attribute != null && attribute.getTextContent().equals(attributeValue)) {
					al.add(item);
				}
			}
			if (item.hasChildNodes()) {
				al.addAll(searchTag(item.getChildNodes(),searchtag, attributeName, attributeValue));
			}
		}

		return al;

	}
	
	
	public ArrayList<Node> searchTagAttributeStart(NodeList nl, String searchtag, String attributeName, String attributeValue) {

		ArrayList<Node> al = new ArrayList<Node>();

		searchtag = searchtag.toLowerCase();

		for (int i=0; i<nl.getLength();i++) {
			Node item = nl.item(i);

			if(item.getNodeName().toLowerCase().equals(searchtag)) {
				NamedNodeMap attributes = item.getAttributes();
				Node attribute = attributes.getNamedItem(attributeName);
				if (attribute != null && attribute.getTextContent().startsWith(attributeValue)) {
					al.add(item);
				}
			}
			if (item.hasChildNodes()) {
				al.addAll(searchTagAttributeStart(item.getChildNodes(),searchtag, attributeName, attributeValue));
			}
		}

		return al;

	}



}
