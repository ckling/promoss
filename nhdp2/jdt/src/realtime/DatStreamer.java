package realtime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is responsible for simulating real-time frames streaming
 * using .dat files.
 * 
 * @author NS
 *
 */
public class DatStreamer extends TerrainStreamer {
	/**
	 * The base name of the .dat frames. The index is then added to this name.
	 */
	final String DAT_FRAMES_NAME = "";
	
	/**
	 * The file type of .dat files.
	 */
	final String DAT_FRAMES_EXT = ".dat";
	
	
	/**
	 * The path to the input frames (directory path).
	 */
	private String m_inputFramesPath = null;
	
	/**
	 * Constructor.
	 * @param inputFrame The path to the input frames (directory path).
	 */
	public DatStreamer(String inputFramePath) {
		m_inputFramesPath = inputFramePath;
	}
	
	@Override
	public ArrayList<Integer> getNextListOfValues() throws IOException {
		return getNextFrame();
	}
	
	/**
	 * @return The values of the next frame.
	 * @throws IOException
	 */
	private ArrayList<Integer> getNextFrame() throws IOException {
		// Block until the next frame file exists.
		blockOnNextFrame();
		
		ArrayList<Integer> values = DatReader.getValues(getNextFrameName());
		
		updateNextFrame();
		return values;
	}
	
	/**
	 * Blocks until the file containing the next frame exists.
	 */
	private void blockOnNextFrame() {
		File f = new File(getNextFrameName());
		while (true) {
			if (f.exists()) {
				break;
			}
		}
		
	}
	
	/**
	 * @return The name of the next frame to process.
	 */
	private String getNextFrameName() {
		return m_inputFramesPath + DAT_FRAMES_NAME + 
			String.valueOf(m_nextFrame) + DAT_FRAMES_EXT;
	}
	
	/**
	 * Updates the name of the next frame to process.
	 */
	private void updateNextFrame() {
		m_nextFrame++;
	}
}