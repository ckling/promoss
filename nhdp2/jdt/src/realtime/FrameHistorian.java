package realtime;

import java.util.ArrayList;
import java.util.Vector;

import delaunay_triangulation.Triangle_dt;

/**
 * This class is responsible for retrieving already existing frames.
 * 
 * @author NS
 *
 */
public class FrameHistorian {

	/**
	 * Constructor.
	 */
	public FrameHistorian() {
		// Empty constructor.
	}
	
	/**
	 * Returns the values of an original frame.
	 * 
	 * @param temporaryFilesFolder The folder in which the temporary files reside.
	 * @param frameIndex The index of the frame to retrieve.
	 * @return The values of an original frame.
	 * @throws Exception
	 */
	public static ArrayList<Integer> getOriginalFrame(String temporaryFilesFolder,
			Integer frameIndex) throws Exception {
		String pgmFileName = temporaryFilesFolder + 
			Consts.PGM_FILE_NAME + frameIndex.toString() +
			Consts.PGM_FILE_EXT;

		return PgmReader.getValues(pgmFileName);
	}
	
	/**
	 * Returns the values of an simplified frame.
	 * 
	 * @param temporaryFilesFolder The folder in which the temporary files reside.
	 * @param frameIndex The index of the frame to retrieve.
	 * @return The values of an simplified frame.
	 * @throws Exception
	 */
	public static ArrayList<Integer> getSimplifiedFrame(String temporaryFilesFolder,
			Integer frameIndex) throws Exception {
		String pgmFileName = temporaryFilesFolder +
		Consts.TERRA_OUTPUT_FILE_NAME + frameIndex.toString() +
			Consts.TERRA_SIMPLIFICATION_OUTPUT_FILE_EXT;
		
		return PgmReader.getValues(pgmFileName);
	}
	
	/**
	 * Returns the values of an triangulated frame.
	 * 
	 * @param temporaryFilesFolder The folder in which the temporary files reside.
	 * @param frameIndex The index of the frame to retrieve.
	 * @return The values of an triangulated frame.
	 * @throws Exception
	 */
	public static Vector<Triangle_dt> getTriangulatedFrame(String temporaryFilesFolder,
			Integer frameIndex) throws Exception {
		String smfFileName = temporaryFilesFolder +
		Consts.TERRA_OUTPUT_FILE_NAME + frameIndex.toString() +
			Consts.TERRA_TRIANGULATION_OUTPUT_FILE_EXT;
		
		return SmfToTriangles.getTriangles(smfFileName);
	}
}
