package gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import realtime.Consts;


/**
 * This class is responsible for translating a .pgm file to an Image.
 * 
 * @author NS
 *
 */
public class PgmToImage {
	/**
	 * Defines by how much should the image's size be resized.
	 */
	private static final double RESIZE_MULTIPLIER = 2.5;
		
	/**
	 * Constructor.
	 */
	public PgmToImage() {
		// Empty constructor.
	}
	
	/**
	 * Returns a resized PGM image that can be presented.
	 * 
	 * @param pgmPath The path to the .pgm file.
	 * 
	 * @return A resized BufferedImage that contains a pgm image.
	 * @throws IOException 
	 */
	public static BufferedImage getImage(ArrayList<Integer> values) throws IOException {
		int width = Consts.PGM_FILE_WIDTH;
		int height = Consts.PGM_FILE_HEIGHT;
		
		// Create the Image instance
		BufferedImage bi = new BufferedImage(width, height, 
				BufferedImage.TYPE_USHORT_GRAY);
		WritableRaster raster = bi.getRaster();
		
		// Set the image's grayscale values
		for (int i = 0; i < width; i++) {
		    for (int j = 0; j < height; j++) {
		        raster.setSample(i ,j, 0, values.get(j * width + i) * 10); 
		    }
		}

		// Return a resized image
		return resize(bi, (int)(width * RESIZE_MULTIPLIER),(int)(height * RESIZE_MULTIPLIER));
	}
	
	/**
	 * Resizes an image and returns the resized one.
	 * 
	 * @param image The image to resize.
	 * @param width The new width.
	 * @param height The new height.
	 * @return A BufferedImage that contains the resized image.
	 */
	private static BufferedImage resize(BufferedImage image, int width, int height) {
		// Create the resized image instance
		BufferedImage resizedImage = new BufferedImage(width, height,
				BufferedImage.TYPE_USHORT_GRAY);
		
		// Resize the image
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(image, 0, 0, width, height, null);

		return resizedImage;
		} 
}
