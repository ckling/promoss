package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import delaunay_triangulation.Triangle_dt;

import realtime.DatStreamer;
import realtime.FrameHistorian;
import realtime.RealtimeTriangulatorThread;
import realtime.TerrainStreamer;

public class RealtimeFrame extends JFrame implements ActionListener, Runnable, ItemListener {
	/**
	 * Defines how many milliseconds to sleep between each repaint()
	 * in the drawer thread's run() method.
	 */
	private final int NUMBER_OF_MILLISECONDS_TO_SLEEP = 200;
	
	/**
	 * An enum which represents the current frame state.
	 */
	private enum FrameState {
	    PAUSED, STARTED, GO_TO_FRAME, AFTER_GO_TO_FRAME
	}
	
	/**
	 * The checkbox that toggles on and off the simplification process.
	 */
	private JCheckBox m_btnSimplify = null;
	
	/**
	 * The checkbox that toggles on and off the triangulation process.
	 */
	private JCheckBox m_btnTriangulate = null;
	
	/**
	 * Defines if this is the first time the frame is run.
	 */
	private boolean m_isFirstTimeRun = true;
	
	/**
	 * The panel in which the original frame will be presented.
	 */
	private JPanel m_pnlOriginalFrame = null;

	/**
	 * The panel in which the simplified frame will be presented.
	 */
	private JPanel m_pnlsimplifiedFrame = null;
	
	/**
	 * The panel in which the triangulated frame will be presented.
	 */
	private JPanel m_pnlTriangulatedFrame = null;
	
	/**
	 * The streamer which will stream frames to the
	 * RealtimeTriangulatorThread.
	 */
	private TerrainStreamer m_streamer = null;
	
	/**
	 * The RealtimeTriangulatorThread object.
	 */
	private RealtimeTriangulatorThread m_rtt = null;
	
	/**
	 * The index of the next frame to process.
	 */
	private Integer m_nextFrame = 0;
	
	/**
	 * The saved frame index in case of "Go to Frame".
	 */
	private Integer m_savedFrame = 0;
	
	/**
	 * The thread that draws the frames.
	 */
	private Thread m_drawerThread = null;
	
	/**
	 * Represents the current state of the frame.
	 */
	private FrameState m_currentState = FrameState.PAUSED;
	
	/**
	 * Defines if the RealtimeTriangulatorThread should simplify the frames.
	 */
	private boolean m_shouldSimplify = true;
	
	/**
	 * Defines if the RealtimeTriangulatorThread should triangulate the frames.
	 */
	private boolean m_shouldTriangulate = true;
	
	/**
	 * The path in which the .pgm and .smf files will be created.
	 */
	private String m_temporaryFolderPath = null;
	
	/**
	 * The path to the Terra executable.
	 */
	String m_terraPath = null;
	
	/**
	 * Main function.
	 * @param args
	 */
	public static void main(String[] args) {
		// Create the frame
		new RealtimeFrame();
	}
	
	/**
	 * Constructor.
	 */
	public RealtimeFrame() {
		addPanelsAndButtons();
		addMenus();
		maximizeFrame();
		
		this.setTitle("Realtime Triangulation GUI");
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	public void paint(Graphics g) {
		ArrayList<Integer> nextOriginalFrame = null;
		ArrayList<Integer> nextSimplifiedFrame = null;
		Vector<Triangle_dt> nextTriangulatedFrame = null;
		
		try {
			// Check if anything should be drawn on the screen
			if (m_currentState == FrameState.STARTED || m_currentState == FrameState.GO_TO_FRAME) {
				if (m_currentState == FrameState.STARTED) { 
					nextOriginalFrame = m_rtt.getOriginalFrame(m_nextFrame);
				} else {
					// Take the frame from the FrameHistorian instead
					// of m_rtt, as it the .pgm is needed to be read again,
					// because m_rtt deletes the frame history
					nextOriginalFrame = FrameHistorian.getOriginalFrame(
							m_temporaryFolderPath,
							m_nextFrame);
				}
				
				if (nextOriginalFrame == null) {
					return;
				}
				
				// Check if a simplified frame should be presented
				if (m_shouldSimplify == true) {
					if (m_currentState == FrameState.STARTED) { 
						nextSimplifiedFrame = m_rtt.getSimplifiedFrame(m_nextFrame);
					} else {
						// Take the frame from the FrameHistorian instead
						// of m_rtt, as it the .pgm is needed to be read again,
						// because m_rtt deletes the frame history
						nextSimplifiedFrame = FrameHistorian.getSimplifiedFrame(
								m_temporaryFolderPath,
								m_nextFrame);
					}
					
					if (nextSimplifiedFrame == null) {
						return;
					}
				}
				
				// Check if a triangulated frame should be presented
				if (m_shouldTriangulate == true) {
					if (m_currentState == FrameState.STARTED) { 
						nextTriangulatedFrame = m_rtt.getTriangulatedFrame(m_nextFrame);
					} else {
						// Take the frame from the FrameHistorian instead
						// of m_rtt, as it the .pgm is needed to be read again,
						// because m_rtt deletes the frame history
						nextTriangulatedFrame = FrameHistorian.getTriangulatedFrame(
								m_temporaryFolderPath,
								m_nextFrame);
					}
					
					if (nextTriangulatedFrame == null) {
						return;
					}
				}
				
				if (nextOriginalFrame != null) {
					// Draw the original frame.
					BufferedImage original = PgmToImage.getImage(nextOriginalFrame);
					m_pnlOriginalFrame.getGraphics().drawImage(original, 65, 35, null);
					m_rtt.deleteOriginalFrame(m_nextFrame);
				}
				
				if (nextSimplifiedFrame != null) {
					/// Draw the simplified frame.
					BufferedImage simplified = PgmToImage.getImage(nextSimplifiedFrame);
					m_pnlsimplifiedFrame.getGraphics().drawImage(simplified, 65, 35, null);
					m_rtt.deleteSimplifiedFrame(m_nextFrame);
				}
				
				if (nextTriangulatedFrame != null) {
					clearJPanel(m_pnlTriangulatedFrame);
					/// Draw the triangulated frame.
					TriangulationDrawer td = new TriangulationDrawer(m_pnlTriangulatedFrame.getWidth(),
							m_pnlTriangulatedFrame.getHeight());
					td.drawTriangulation(m_pnlTriangulatedFrame.getGraphics(),
							nextTriangulatedFrame);
					m_rtt.deleteTriangulatedFrame(m_nextFrame);
				}
				
				// Only update the next frame if the RealtimeTriangulatorThread
				// and the drawing thread are running
				if (m_currentState == FrameState.STARTED) {
					m_nextFrame++;
				} else {
					// The "Go to Frame" frame was presented to the user,
					// update the current state
					m_currentState = FrameState.AFTER_GO_TO_FRAME;
				}
			}
		} catch (Exception e) {
			System.out.println("An error has occurred while updating screen: "
					+ e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Adds the panels and buttons to the frame.
	 */
	private void addPanelsAndButtons() {
		JPanel pnlOriginalAndSimplifiedFrames = new JPanel(new GridLayout(1, 2));
		JPanel pnlFrames = new JPanel();
		JPanel pnlButtons = new JPanel();
		
		m_pnlOriginalFrame = new JPanel();
		m_pnlOriginalFrame.add(new JLabel("Original Frame"));
		m_pnlOriginalFrame.setBorder(BorderFactory.createEtchedBorder());
		
		m_pnlsimplifiedFrame = new JPanel();
		m_pnlsimplifiedFrame.add(new JLabel("Simplified Frame"));
		m_pnlsimplifiedFrame.setBorder(BorderFactory.createEtchedBorder());
		
		m_pnlTriangulatedFrame = new JPanel();
		m_pnlTriangulatedFrame.add(new JLabel("Triangulated Frame"));
		m_pnlTriangulatedFrame.setBorder(BorderFactory.createEtchedBorder());
		
		m_btnSimplify = new JCheckBox("Simplify", true);
		m_btnSimplify.setActionCommand("Simplify");
		m_btnSimplify.addItemListener(this);
		m_btnTriangulate = new JCheckBox("Triangulate", true);
		m_btnTriangulate.setActionCommand("Triangulate");
		m_btnTriangulate.addItemListener(this);
		
		Container c = this.getContentPane();
		c.setLayout(new BorderLayout());
		
		pnlOriginalAndSimplifiedFrames.add(m_pnlOriginalFrame);
		pnlOriginalAndSimplifiedFrames.add(m_pnlsimplifiedFrame);
		
		pnlFrames.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.ipady = 370; 
		gbc.weightx = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		pnlFrames.add(pnlOriginalAndSimplifiedFrames, gbc);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.LAST_LINE_START;
		gbc.gridx = 0;
		gbc.gridy = 1;
		pnlFrames.add(m_pnlTriangulatedFrame, gbc);
		
		pnlButtons.add(m_btnSimplify);
		pnlButtons.add(m_btnTriangulate);
		
		c.add(pnlFrames, BorderLayout.CENTER);
		c.add(pnlButtons, BorderLayout.SOUTH);
		
	}
	
	/**
	 * Menu and menu buttons creation.
	 */
	private void addMenus() {
		MenuBar mbar = new MenuBar();

		Menu m = new Menu("Operations");
		MenuItem m1;
		m1 = new MenuItem("Start");
		m1.addActionListener(this);
		m.add(m1);
		m1 = new MenuItem("Pause");
		m1.addActionListener(this);
		m.add(m1);
		m1 = new MenuItem("Exit");
		m1.addActionListener(this);
		m.add(m1);
		mbar.add(m);

		m = new Menu("Functions");
		MenuItem m3 = new MenuItem("Go to Frame");
		m3.addActionListener(this);
		m.add(m3);
		mbar.add(m);
		
		setMenuBar(mbar);
	}
	
	/**
	 * Maximizes the frame.
	 */
	private void maximizeFrame() {
		GraphicsEnvironment env =
		     GraphicsEnvironment.getLocalGraphicsEnvironment();
		   this.setBounds(env.getMaximumWindowBounds());
	}
	
	private void startRealtimeTriangulation() {
		if (m_isFirstTimeRun == true) {
			FileDialog dialog = new FileDialog(this, "Open Terra executable file",
					FileDialog.LOAD);
			dialog.show();
			String fileName = dialog.getFile();
			String directoryName = dialog.getDirectory();
			
			if (fileName != null && directoryName != null) {
				m_terraPath = directoryName + fileName;
			}
			
			dialog = new FileDialog(this, "Choose directory to save temporary .pgm and .smf files",
					FileDialog.LOAD);
			dialog.show();
			
			directoryName = dialog.getDirectory();
			
			if (directoryName != null) {
				m_temporaryFolderPath = directoryName;
			}
			
			m_isFirstTimeRun = false;
		}
		
		resume();
	}

	@Override
	public void run() {
		try {
			// Run until paused
			while (m_currentState == FrameState.STARTED) {
				System.out.println(m_nextFrame);
				repaint();
				
				// Repaint every NUMBER_OF_MILLISECONDS_TO_SLEEP
				Thread.sleep(NUMBER_OF_MILLISECONDS_TO_SLEEP);
			}
		} catch (Exception e) {
			System.out.println("An error has occurred: " + e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Starts the drawer thread and the RealtimeTriangulatorThread.
	 */
	public void resume() {
		try {
			if (m_currentState == FrameState.AFTER_GO_TO_FRAME) {
				m_nextFrame = m_savedFrame;
				m_currentState = FrameState.PAUSED;
			}
			
			if (m_currentState == FrameState.PAUSED) {
				if (m_streamer == null) {
					m_streamer = new DatStreamer(m_temporaryFolderPath);
				}
				
				if (m_rtt == null) {
						m_rtt = new RealtimeTriangulatorThread(m_streamer,
								m_terraPath,
								m_temporaryFolderPath,
								m_shouldSimplify,
								m_shouldTriangulate);
					
					
					m_rtt.start();
				} else {
					m_rtt.setShouldSimplify(m_shouldSimplify);
					m_rtt.setShouldTriangulate(m_shouldTriangulate);
					m_rtt.setResumed();
				}
				
				m_currentState = FrameState.STARTED;
		
				m_drawerThread = new Thread(this);
				m_drawerThread.start();
				
				m_btnSimplify.setVisible(false);
				m_btnTriangulate.setVisible(false);
			}
		} catch (Exception e) {
			System.out.println("An error has occurred while resuming: " + e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Pauses the running of the drawer thread and the
	 * RealtimeTriangulatorThread.
	 */
	public void pause() {
		try {
			if (m_currentState == FrameState.STARTED) {
				m_currentState = FrameState.PAUSED;
				
				if (m_rtt != null) {
					m_rtt.setPaused();
				}	
				
				m_drawerThread = null;
			}
		} catch (Exception e) {
			System.out.println("An error has occurred while pausing: " + e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Clears the panel.
	 */
	private void clearJPanel(JPanel pnl) {
		Graphics g = pnl.getGraphics();
		Dimension size = pnl.getSize();
	
		Color c = pnl.getBackground();
		g.setColor(c);
		g.fillRect(0, 30, size.width, size.height);
	
		pnl.repaint();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			String command = e.getActionCommand();
			
			if (command.compareTo("Start") == 0) {
				startRealtimeTriangulation();
			} else if (command.compareTo("Pause") == 0) {
				pause();
			} else if (command.compareTo("Exit") == 0) {
				System.exit(0);
			} else if (command.compareTo("Go to Frame") == 0) {
				
				if (m_isFirstTimeRun == true) {
					 JOptionPane.showMessageDialog(null, "You first need to start" +
					 		" the simplification/triangulation process");
				} else {
					String frameNumber = JOptionPane.showInputDialog(null,
							"Current frame number: " + m_nextFrame.toString() + 
							"\nEnter the frame number: ", 
							null,
							1);
					
					// Only update the saved frame if it the first time
					// pressing "Go to Frame".
					// NOTICE: pressing "Go to Frame" after pausing
					// or starting will update the saved frame.
					if (m_currentState != FrameState.AFTER_GO_TO_FRAME) {
						m_savedFrame = m_nextFrame;
					}
					
					m_nextFrame = Integer.valueOf(frameNumber);
					m_currentState = FrameState.GO_TO_FRAME;
					
					repaint();
				}
				
				
			} else {
				System.out.println("Unknown command");
			}
		} catch (Exception ex) {
			System.out.println("An exception has occurred preforming action: " + 
					ex.getMessage());
		}
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();

		// Toggle simplifying on/off
		if (source == m_btnSimplify) {
			if (m_shouldSimplify == true) {
				m_shouldSimplify = false;
			} else {
				m_shouldSimplify = true;
			}
		}  else if (source == m_btnTriangulate) {
			// Toggle triangulation on/off
			if (m_shouldTriangulate == true) {
				m_shouldTriangulate = false;
			} else {
				m_shouldTriangulate = true;
			}
		} else {
			System.out.println("Unknown checkbox clicked");
		}
		
	}	

}
