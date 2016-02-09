package kdl.classifier;
/*This class and all classes in the package kdl.classifier were written by Joseph Paul Cohen*/


public interface IntegralImage {

	/**
	 * The integral image at location 
	 * x, y contains the sum of the pixels 
	 * above and to the left of x, y, 
	 * inclusive
	 * 
	 * @param x
	 * @param y
	 * @return value at x y
	 */
	public int valueAt(int x, int y);
	
	/**
	 * 
	 * @return Width of IntegralImage
	 */
	public int getWidth();
	
	/**
	 * 
	 * @return Height of IntegralImage
	 */
	public int getHeight();
	
}
