package realtime;

import java.io.IOException;
import java.util.Vector;

import delaunay_triangulation.Triangle_dt;

/**
 * This class is responsible to run a TerrianSimplifier triangulate() method
 * in its own thread - only on a single frame.
 * 
 * @author NS
 *
 */
public class TerrainTriangulatorThread extends Thread {
	/**
	 * The RealtimeTriangulatorThread objected that contains
	 * the list of triangulated frames. This list should be updated by this thread.
	 */
	RealtimeTriangulatorThread m_rtt = null;
	/**
	 * The terrain simplifier object
	 */
	TerrainSimplifier m_ts = null;
	
	/**
	 * The path to the Terra output file name.
	 */
	String m_outputFileName = null;
	
	/**
	 * The index of the frame this thread is handling.
	 */
	int m_frameIndex;
	
	/**
	 * Constructor.
	 * 
	 * @param rtt The RealtimeTriangulatorThread objected that contains
	 * the list of triangulated frames. This list should be updated by this thread.
	 * @param pgmFileName The path to the input .pgm file.
	 * @param terraExePath The path to the Terra exe file.
	 * @param outputFileName The path to the Terra output file name.
	 * @throws IOException 
	 */
	public TerrainTriangulatorThread(RealtimeTriangulatorThread rtt,
			String pgmFileName,
			String terraExePath,
			String outputFileName,
			int frameIndex) throws IOException {
		m_outputFileName = outputFileName;
		m_ts = new TerrainSimplifier(pgmFileName, 
				terraExePath, 
				m_outputFileName);
		m_rtt = rtt;
		m_frameIndex = frameIndex;
	}

	@Override
	public void run() {
		try {
			Vector<Triangle_dt> values = m_ts.triangulate();
			m_rtt.addTriangulatedFrame(values, m_frameIndex);
		} catch (Exception ex) {
			System.out.println(this.getClass().getSimpleName() + "-> " +
					"an error has occurred: " + ex.getMessage());
			ex.printStackTrace();
		}
		
	}
	
	
}