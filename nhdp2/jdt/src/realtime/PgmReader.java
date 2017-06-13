package realtime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Reads a .pgm file and parses the values contained in it.
 * 
 * @author NS
 *
 */
public class PgmReader {	
	/**
	 * Constructor.
	 */
	public PgmReader() {
		// Empty constructor.
	}
	
	/**
	 * Parses the values of the .pgm file and returns an array containing them.
	 *
	 * @param fileName The path to the input .pgm file.
	 * 
	 * @return An ArrayList containing the values extracted from the .pgm file.
	 * @throws IOException 
	 */
	public static ArrayList<Integer> getValues(String fileName) throws IOException {
		// Create the stream used to read the input file
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		
		ArrayList<Integer> values = new ArrayList<Integer>();
		
		ArrayList<String> inputLines = readLines(br);
		br.close();
		
		for (int i = 1; i < inputLines.size(); i++) {
			String currentLine = inputLines.get(i);
			
			// Check if the last line is not empty
			if (currentLine.isEmpty() == false) {
				// Check if it is the last line
				if (i == (inputLines.size() -1)) {
					// Remove \n at the end of the line
					currentLine = currentLine.substring(0, 
							currentLine.length() - 1);
				}
				
				if (currentLine.charAt(0) == ' ') {
					// Remove whitespace at the start of the line
					currentLine = currentLine.substring(1);
				}
				
				String[] splittedLine = currentLine.split(" ");
				
				// Get the values
				for (int j = 0; j < splittedLine.length; j++) {
					values.add(Integer.valueOf(splittedLine[j]));
				}
			}
		}
		
		return values;
	}
	
	/**
	 * Reads a whole file, line by line and returns an array of lines.
	 * 
	 * @param br The BufferedReader used to read the lines from the file.
	 * @return An ArrayList which holds the lines of the file.
	 * @throws IOException
	 */
	private static ArrayList<String> readLines(BufferedReader br) throws IOException {
		ArrayList<String> lines = new ArrayList<String>();
		String currentLine;
		
		// Read the file, line by line
		currentLine = br.readLine();
		while (currentLine != null) {
			lines.add(currentLine);
			currentLine = br.readLine();
		}
		
		return lines;
	}
}
