package realtime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Reads a .dat file and parses the values contained in it.
 * 
 * @author NS
 *
 */
public class DatReader {
	/**
	 * Used for multiplying the .dat file's distance values in order for them
	 * to be integers (and not doubles).
	 */
	private static final int MULTIPLIER = 10000;
	
	/**
	 * Constructor.
	 */
	public DatReader() {
		// Empty constructor.
	}
	
	/**
	 * Parses the values of the .dat file and returns an array containing them.
	 *
	 * @param fileName The path to the input .dat file.
	 * 
	 * @return An ArrayList containing the values extracted from the .dat file.
	 * @throws IOException 
	 */
	public static ArrayList<Integer> getValues(String fileName) throws IOException {
		// Create the stream used to read the input file
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		
		ArrayList<Integer> values = new ArrayList<Integer>();
		
		ArrayList<String> inputLines = readLines(br);
		br.close();
		
		for (int i = 1; i < inputLines.size(); i++) {
			// Check if we looped over the whole distances values
			if (inputLines.get(i).indexOf('%') != -1) {
				break;
			} else {
				String currentLine = inputLines.get(i);
				
				// Remove \n at the end of the line
				currentLine = currentLine.substring(0, 
						currentLine.length() - 1);
				String[] splittedLine = currentLine.split("\t");
				
				// Get the values
				for (int j = 0; j < splittedLine.length; j++) {
					Integer currentValue = (int)(Double.valueOf(splittedLine[j])
							* MULTIPLIER);
					
					values.add(currentValue);
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
