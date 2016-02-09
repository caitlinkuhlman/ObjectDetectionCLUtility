package detection;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

import boofcv.gui.feature.VisualizeShapes;
import boofcv.struct.PointIndex_I32;

/**
 * Utility class for drawing candidates.
 * @author caitlinkuhlman
 *
 */
public class Draw {

	static Random rand = new Random(234);

	/**
	 * Draw multiple candidates onto image each with different color.
	 * 
	 * @param shapes
	 *            List of candidates to draw.
	 * @param im
	 */
	static void drawPolygons(List<List<PointIndex_I32>> shapes, BufferedImage im) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));

		for (List<PointIndex_I32> vertices : shapes) {

			g2.setColor(new Color(rand.nextInt()));
			VisualizeShapes.drawPolygon(vertices, true, g2);
		}
	}

	/**
	 * Draw multiple candidates onto image with black outlines.
	 * 
	 * @param shapes
	 *            List of candidates to draw.
	 * @param im
	 */
	public static void drawPolygonsBlack(List<List<PointIndex_I32>> shapes,
			BufferedImage im) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));

		for (List<PointIndex_I32> vertices : shapes) {

			g2.setColor(new Color(0, 0, 0));
			VisualizeShapes.drawPolygon(vertices, true, g2);
		}
	}

	/**
	 * Draw multiple candidates onto image with white outlines.
	 * 
	 * @param shapes
	 *            List of candidates to draw.
	 * @param im
	 */
	public static void drawPolygonsWhite(List<List<PointIndex_I32>> shapes,
			BufferedImage im) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));

		for (List<PointIndex_I32> vertices : shapes) {

			System.out.println(vertices.toString());
			g2.setColor(new Color(255, 255, 255));

			VisualizeShapes.drawPolygon(vertices, true, g2);
		}
	}

	/**
	 * Draw multiple candidates onto image with green outlines.
	 * 
	 * @param shapes
	 *            List of candidates to draw.
	 * @param im
	 */
	static void drawPolygonsGreen(List<List<PointIndex_I32>> shapes,
			BufferedImage im) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));

		for (List<PointIndex_I32> vertices : shapes) {

			g2.setColor(new Color(124, 252, 0));
			VisualizeShapes.drawPolygon(vertices, true, g2);
		}
	}

	/**
	 * Draw one candidate onto image with random color.
	 * 
	 * @param shapes
	 *            candidates to draw.
	 * @param im
	 */
	static void drawPolygon(List<PointIndex_I32> shape, BufferedImage im) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));
		g2.setColor(new Color(rand.nextInt()));
		VisualizeShapes.drawPolygon(shape, true, g2);
	}

	/**
	 * Draw one candidate onto image, set color of outline.
	 * 
	 * @param shape
	 * @param im
	 * @param color
	 */
	static void drawPolygonVary(List<PointIndex_I32> shape, BufferedImage im,
			int color) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));
		g2.setColor(new Color(color, color, color));
		VisualizeShapes.drawPolygon(shape, true, g2);

	}

	/**
	 * Draw one candidate onto image with white outline.
	 * 
	 * @param shape
	 *            candidates to draw.
	 * @param im
	 */
	public static void drawPolygonWhite(List<PointIndex_I32> shape,
			BufferedImage im) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));
		g2.setColor(new Color(255, 255, 255));
		VisualizeShapes.drawPolygon(shape, true, g2);
	}

	/**
	 * Draw one candidate onto image with black outline.
	 * 
	 * @param shape
	 *            candidates to draw.
	 * @param im
	 */
	public static void drawPolygonBlack(List<PointIndex_I32> shape,
			BufferedImage im) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));
		g2.setColor(new Color(0, 0, 0));
		VisualizeShapes.drawPolygon(shape, true, g2);
	}

	/**
	 * Draw one candidate onto image with green outline.
	 * 
	 * @param shape
	 *            candidates to draw.
	 * @param im
	 */
	public static void drawPolygonGreen(List<PointIndex_I32> shape,
			BufferedImage im) {

		Graphics2D g2 = im.createGraphics();
		g2.setStroke(new BasicStroke(2));
		g2.setColor(new Color(124, 252, 0));
		VisualizeShapes.drawPolygon(shape, true, g2);
	}

	/**
	 * Rotate image by specified degree.
	 * @param src
	 * @param degree
	 * @return new image.
	 */
	public static BufferedImage rotateSquare(BufferedImage src, int degree) {
		double radians = Math.toRadians(degree);

		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();

		double sin = Math.abs(Math.sin(radians));
		double cos = Math.abs(Math.cos(radians));
		int newWidth = (int) Math.floor(srcWidth * cos + srcHeight * sin);
		int newHeight = (int) Math.floor(srcHeight * cos + srcWidth * sin);

		BufferedImage result = new BufferedImage(newWidth, newHeight,
				src.getType());
		Graphics2D g = result.createGraphics();
		g.translate((newWidth - srcWidth) / 2, (newHeight - srcHeight) / 2);
		g.rotate(radians, srcWidth / 2, srcHeight / 2);
		g.drawRenderedImage(src, null);

		return result;
	}

	/**
	 * Flip image vertically.
	 * @param img
	 * @return new image.
	 */
	public static BufferedImage verticalflip(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(w, h, img.getColorModel()
				.getTransparency());
		Graphics2D g = dimg.createGraphics();
		g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);
		g.dispose();
		return dimg;
	}

	/**
	 * Flip image horizontally.
	 * @param img
	 * @return new image.
	 */
	public static BufferedImage horizontalflip(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(w, h, img.getType());
		Graphics2D g = dimg.createGraphics();
		g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
		g.dispose();
		return dimg;
	}

}
