package realtime;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is responsible for translating a .dat frame to a .pgm file.
 * The .dat file is the output of some specific cameras that the realtime
 * triangulation is run on. The .pgm file is the input format to the Terra
 * program.
 * 
 * @author NS
 *
 */
public class DatToPgmTranslator {
	/**
	 * The identifier used by Terra to identify .pgm files. 
	 */
	final String PGM_IDENTIFIER = "P2";
	
	/**
	 * The number of values in each line of the .pgm file.
	 */
	final int NUMBER_OF_VALUES_IN_LINE = 8;
	
	/**
	 * Constructor.
	 * Performs all the translation logic.
	 * 
	 * @param inputFileName The .dat file name.
	 * @param outputFileName The .pgm file name.
	 * @throws IOException
	 */
	public DatToPgmTranslator(ArrayList<Integer> values, String outputFileName) 
	throws IOException {
		// Get the array of lines needed to be written to the .pgm file
		ArrayList<String> outputLines = getPgmValues(values);
		
		// Create the streams used to write to the output file
		FileWriter fw = new FileWriter(outputFileName);
		BufferedWriter bw = new BufferedWriter(fw);
		
		// Write the lines to the output file
		for (int i = 0; i < outputLines.size(); i++) {
			bw.write(outputLines.get(i));
		}
		bw.close();
		
		System.out.println("Created " + outputFileName);
	}
	
	/**
	 * Returns an array that holds the lines of the .pgm file
	 * @param values The height field values from the .dat file.
	 * @return An ArrayList of lines to be written to the .pgm file.
	 */
	private ArrayList<String> getPgmValues(ArrayList<Integer> values) {
		ArrayList<String> newValues = new ArrayList<String>();
		int counter = 0;
		Integer maxValue = 0;
		
		String newLine = "";
		for (int i = 0; i < values.size(); i++) {		
			// Create the new pgm line
			Integer currentValue = values.get(i);
			
			newLine += " " + currentValue.toString();
			
			counter++;
			
			// Update max value
			if (maxValue < currentValue) {
				maxValue = currentValue;
			}
			
			// If we reached the maximum number of values in a line,
			// then break the line
			if (counter == NUMBER_OF_VALUES_IN_LINE) {
				newValues.add(newLine + "\n");
				newLine = "";
				counter = 0;
			}
		}
		
		// Add the first line of the .pgm file
		String firstPgmLine = PGM_IDENTIFIER + " " + Consts.PGM_FILE_WIDTH +
			" " + Consts.PGM_FILE_HEIGHT + " " +
			maxValue.toString() + "\n";
		newValues.add(0, firstPgmLine);
		
		return newValues;
	}
}
