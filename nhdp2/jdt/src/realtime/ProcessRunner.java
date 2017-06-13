package realtime;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This class is responsible for running a process 
 * in a safe way, including waiting for it to finish
 * and closing its input/output/error streams.
 * 
 * @author NS
 *
 */
public class ProcessRunner {
	/**
	 * Constructor.
	 */
	public ProcessRunner() {
		// Empty constructor.
	}
	
	/**
	 * Runs a process and waits for it to finish.
	 * 
	 * @param processName The name of the process to run.
	 * @param args The arguments to the process.
	 * @throws Exception 
	 */
	public static void runProcess(String processName, String args) throws Exception {
		Process p = null;
		
		try {			
			p = Runtime.getRuntime().exec(processName + " " + args);
				
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while (br.readLine() != null) {
			}
		} catch(Exception ex) {
			System.out.println("An error has occurred while running the process: " +
					processName + ": " + ex.getMessage());
		} finally {
			if (p != null) {
			    p.waitFor();
			    
			    // Close input stream
			    try {
			    	p.getInputStream().close();
			    } catch (Exception ex) {
			    	System.out.println("An error has occurred while running the process: " +
							processName + ": " + ex.getMessage());
			    }
			    
			    // Close output stream
			    try {
			    	p.getOutputStream().close();
			    } catch (Exception ex) {
			    	System.out.println("An error has occurred while running the process: " +
							processName + ": " + ex.getMessage());
			    }
			    
			    // Close error stream
			    try {
			    	p.getErrorStream().close(); 
			    } catch (Exception ex) {
			    	System.out.println("An error has occurred while running the process: " +
							processName + ": " + ex.getMessage());
			    }
			}
		}
	}
}
