package kdl.classifier;

/*This class and all classes in the package kdl.classifier were written by Joseph Paul Cohen*/


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Implementation of an Integral Image as 
 * discussed by Viola et al. in 
 * "Robust Real-Time Face Detection" 2003
 * 
 * @author Joseph Paul Cohen
 * @author 2010
 */

public class BufferedImageIntegralImage implements IntegralImage{

	private int[][] imgMatrix = null;
	
	/**
	 * O(n) where n = number of pixels
	 * @param img
	 */
	public BufferedImageIntegralImage(BufferedImage img) {
		
		int width = img.getWidth();
		int height = img.getHeight();
		
		imgMatrix = new int[height][width];
		
		// we do the first element
		imgMatrix[0][0] = getGreyValue(img, 0, 0);
		
		// we do the first row
		for (int x = 1; x < height; x++)
			imgMatrix[0][x] = imgMatrix[0][x-1] + getGreyValue(img, x, 0);
		
		// we get the first column
		for (int y = 1; y < width; y++)
			imgMatrix[y][0] = imgMatrix[y-1][0] + getGreyValue(img, 0, y);
		
		// scan the rest of the elements
		// add two rectangles and then remove the overlap
		for (int y = 1; y < width; y++)
			for (int x = 1; x < height; x++)
				imgMatrix[y][x] = imgMatrix[y][x-1] + imgMatrix[y-1][x] - imgMatrix[y-1][x-1] + getGreyValue(img, x, y);
	}
	
	
	@Override
	public int valueAt(int x, int y) {

		return imgMatrix[x][y];
	}

	@Override
	public int getWidth() {

		return imgMatrix.length;
	}

	@Override
	public int getHeight() {

		return imgMatrix[0].length;
	}

	public static int getGreyValue(BufferedImage img, int x, int y){
		double greyValue = 0;
				
		int rgb = img.getRGB(x, y);
		
		int red = img.getColorModel().getRed(rgb);
		int green = img.getColorModel().getGreen(rgb);
		int blue =  img.getColorModel().getBlue(rgb);
		
		greyValue = (.299 * red + .587 * green + .114 * blue + .5);
		
		return (int) greyValue;
	}
	
	public static void main(String[] args) throws IOException{
		
		IntegralImage ii = new BufferedImageIntegralImage(ImageIO.read(new File("imgs/craters/1179-1442.png")));
		
		System.out.println("Height:" + ii.getHeight());
		System.out.println("Width:" + ii.getWidth());
		
		System.out.println(ii.valueAt(0, 0));
		System.out.println(ii.valueAt(2, 2));
		System.out.println(ii.valueAt(125, 125));
		System.out.println(ii.valueAt(38, 38));
	}
	
}
