package realtime;

import java.io.IOException;
import java.util.ArrayList;

/**
 * An abstract class that represents a terrain streamer. This classes'
 * sub-classes are being used by the RealtimeTriangulatorThread.
 * 
 * @author NS
 *
 */
public abstract class TerrainStreamer {
	/**
	 * The index of the next input frame.
	 */
	protected int m_nextFrame = 0;
	
	/**
	 * An abstract methods that can be used to retrieve the next set of values.
	 * The list of integers returned represents the next frame to be presented.
	 * 
	 * @return An ArrayList of Integer objects that represents the next frame
	 * to be presented.
	 * @throws IOException
	 */
	public abstract ArrayList<Integer> getNextListOfValues() throws IOException;
}
