package realtime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import delaunay_triangulation.*;

/**
 * This class is responsible for simplifying a terrain received as input.
 * The simplification process in done using Terra. 
 * The simplified terrain can than be used to build a triangulation
 * and be presented graphically.
 * 
 * @author NS
 *
 */
public class TerrainSimplifier {
	/**
	 * The output file format argument of Terra. This is actually a .smf format.
	 */
	private final String TRIANGULATION_OUTPUT_FORMAT = "obj";
	
	/**
	 * The output file format argument of Terra. This is actually a .pgm format.
	 */
	private final String SIMPLIFICATION_OUTPUT_FORMAT = "dem";
	
	/**
	 * The percentage of number of points that the simplified terrain
	 * 	   should contain.
	 */
	private final int PERCENTAGE_OF_POINTS_IN_THE_SIMPLIFIED_TERRAIN = 15;

	/**
	 * The file name of the input terrain file (.pgm file).
	 */
	private String m_terrainFilename = null;
	
	/**
	 * The path to the Terra executable.
	 */
	private String m_terraExePath = null;
	
	/**
	 * The path of the output file of Terra.
	 */
	private String m_outputFile = null;
	
	/**
	 * The number of points in the input .pgm file.
	 */
	private Integer m_numberOfPoints = 0;
	
	/**
	 * Constructor.
	 * 
	 * @param terrainFilename The file name of the input terrain 
	 * 		  file (.pgm file).
	 * @param terraExePath The path to the Terra executable.
	 * @param outputFile The name of the output file of Terra.
	 * @param outputFormat The format of the output file of Terra.
	 * @throws IOException 
	 * 
	 */
	public TerrainSimplifier(String terrainFilename,
							 String terraExePath,
							 String outputFile) throws IOException {
		m_terrainFilename = terrainFilename;
		m_terraExePath = terraExePath;
		m_outputFile = outputFile;
		
		// Get the number of points in the .pgm file and decide how many points
		// should Terra use in the triangulation.
		m_numberOfPoints = getNumberOfPointsInTheTerrain() *
			PERCENTAGE_OF_POINTS_IN_THE_SIMPLIFIED_TERRAIN / 100;
	}
	
	/**
	 * Simplifies the terrain and returns an ArrayList of Triangle_dt  
	 * objects that represents the triangulation.
	 * 
	 * @return An ArrayList of Triangle_dt objects that represents 
	 * the triangulation.
	 * @throws Exception 
	 */
	public Vector<Triangle_dt> triangulate() throws Exception {
		
		String whatToExecute = "-p " + 
			m_numberOfPoints.toString() + " -o " + m_outputFile + 
			Consts.TERRA_TRIANGULATION_OUTPUT_FILE_EXT + 
			" " + TRIANGULATION_OUTPUT_FORMAT + " " + m_terrainFilename;
		System.out.println("Executing: " + m_terraExePath + " " + whatToExecute);
		
		// Run Terra and wait for it to finish running.
		ProcessRunner.runProcess(m_terraExePath, whatToExecute);
		
		// Return the array of triangles.
		return SmfToTriangles.getTriangles(m_outputFile + 
				Consts.TERRA_TRIANGULATION_OUTPUT_FILE_EXT);
	}
	
	/**
	 * Simplifies the terrain - creates a simplified .pgm file using Terra.
	 * 
	 * @throws Exception 
	 */
	public void simplify() throws Exception {
		String whatToExecute = "-p " +
			m_numberOfPoints.toString() + " -o " + m_outputFile + 
			Consts.TERRA_SIMPLIFICATION_OUTPUT_FILE_EXT +
			" " + SIMPLIFICATION_OUTPUT_FORMAT + " " + m_terrainFilename;
		System.out.println("Executing: " + m_terraExePath + " " +whatToExecute);
		
		// Run Terra and wait for it to finish running.
		ProcessRunner.runProcess(m_terraExePath, whatToExecute);
	}
	
	/**
	 * Returns the number of points in the .pgm file.
	 * @return The number of points in the .pgm file.
	 * @throws IOException
	 */
	private Integer getNumberOfPointsInTheTerrain() throws IOException {
		// Create the stream used to read the input file
		BufferedReader br = new BufferedReader(new FileReader(m_terrainFilename));
		
		String firstLine = br.readLine();
		br.close();
		
		String[] splittedLine = firstLine.split(" ");
		return Integer.valueOf(splittedLine[1]) * Integer.valueOf(splittedLine[2]);
	}
}
