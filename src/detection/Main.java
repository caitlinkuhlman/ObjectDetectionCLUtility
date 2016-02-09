package detection;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.shapes.ShapeFittingOps;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

/*This class has three methods for detecting buildings in images. 
 * 
 * testContoursOnly returns all candidates, above a threshold size, produced using contour generation.
 * 
 * testML uses a classifier to identify buildings from the set of candidates produced by contour generation.
 * 
 * testPHM uses a classifier, and a permutation search to attempt to recover candidates missed by contour generation.
 * 
 * 
 * 
 * 
 * 
 * The main method of this class expects a filename of a png image, and the name of a method  - ML, PHM, or Contours
 * as a command line arguments.
 * 
 * It displays the image with the buildings identified.   */

public class Main {

	static float threshLow = 0.2f;
	static float threshHigh = 0.4f;

	static int blurRadius = 1;
	static double toleranceDist = 2;
	static double toleranceAngle = Math.PI / 10;
	static boolean dynamicThreshold = true;
	// type of equalize
	static boolean local = false;
	// type of sharpen
	static int sharp = 4;
	static boolean eq = false;
	static boolean sh = true;

	// degree of freedom for contour - 4 or 8
	static int rule = 8;

	// proximity threshold for candidates
	static int minsize;

	static String filename;
	static Random rand = new Random(234);
	static String dir = "";

	public static void main(String[] args) throws IOException {

		if (args.length == 0) {
			System.out
					.println("usage: crop-objects [OPTION]... FILE [DIR]\n"
							+ "crops detected objects from image FILE and writes their subimages to files. \n"
							+ "Can specify the DIR in which to create the files, otherwise subimage files are "
							+ "created in the same directory as FILE is in by default. \n"
							+ "-t [n] [m] 		set the high and low threshold values. n and m are values between 0 and 1.");
			System.exit(1);
		}
		// interpret options and read arguments
		if (args[0].equals("-t")) {

			threshLow = Float.parseFloat(args[1]);
			threshHigh = Float.parseFloat(args[2]);

			filename = args[3];
			if (args.length == 5) {
				dir = args[4];
			}
		} else {

			filename = args[0];
			if (args.length == 2) {
				dir = args[1];
			}
		}
		// get path to directory file is in
		String name = FilenameUtils.removeExtension(filename);

		// get image
		BufferedImage image = UtilImageIO.loadImage(new File(filename)
				.getAbsolutePath());
		if(image == null) {
			System.out
					.println("usage: crop-objects [OPTION]... FILE [DIR]\n"
							+ "crops detected objects from image FILE and writes their subimages to files. \n"
							+ "Can specify the DIR in which to create the files, otherwise subimage files are "
							+ "created in the same directory as FILE is in by default. \n"
							+ "-t [n] [m] 		set the high and low threshold values. n and m are values between 0 and 1.");
			System.exit(1);
		}

		minsize = (int) (0.1 * Math.min(image.getHeight(), image.getWidth()));

		// find objects in image
		// generate candidate contours

		ArrayList<List<PointIndex_I32>> objects = new ArrayList<List<PointIndex_I32>>();
		List<BufferedImage> results = new ArrayList<BufferedImage>();

		List<List<PointIndex_I32>> candidates = new ArrayList<List<PointIndex_I32>>();

		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image,
				null, ImageFloat32.class);

		BufferedImage bw = ConvertBufferedImage.convertTo(
				input,
				new BufferedImage(image.getWidth(), image.getHeight(), image
						.getType()));
		File binaryfile = new File(name + "_" + "binary.png");
		ImageIO.write(bw, "png", binaryfile);

		ImageUInt8 binary = new ImageUInt8(input.width, input.height);

		// Finds edges inside the image
		CannyEdge<ImageFloat32, ImageFloat32> canny = FactoryEdgeDetectors
				.canny(blurRadius, true, dynamicThreshold, ImageFloat32.class,
						ImageFloat32.class);

		canny.process(input, threshLow, threshHigh, binary);

		List<Contour> contours = BinaryImageOps.contour(binary, rule, null);

		BufferedImage visualBinary = VisualizeBinaryData.renderBinary(binary,
				null);
		File cannyfile = new File(name + "_" + "canny.png");
		ImageIO.write(visualBinary, "png", cannyfile);

		BufferedImage cannyContour = VisualizeBinaryData.renderExternal(
				contours, null, binary.width, binary.height, null);
		File cannyContourfile = new File(name + "_" + "contour.png");
		ImageIO.write(cannyContour, "png", cannyContourfile);

		for (Contour c : contours) {
			// Only the external contours are relevant.
			List<PointIndex_I32> vertices = ShapeFittingOps.fitPolygon(
					c.external, true, toleranceDist, toleranceAngle, 100);
			candidates.add(vertices);
		}

		for (List<PointIndex_I32> vertices : candidates) {
			try {
				Candidate c = new Candidate(vertices, image);
				if (c.size(minsize)) {
					c.rotate();
					results.add(c.getImage());
					objects.add(vertices);
				}
			} catch (Exception e) {
				System.out.println("Error creating candidate from contour "
						+ e.getMessage());
			}
		}

		// write subimages of objects to files
		int i = 0;
		for (BufferedImage obj : results) {
			// print images to file
			try {
				File outputfile = new File(name + "_" + i + ".png");
				i++;
				ImageIO.write(obj, "png", outputfile);
			} catch (IOException e) {
				System.out.println("Error writing subimages" + e.getMessage());
			}
		}

		// draw objects onto original image and save
		Draw.drawPolygons(objects, image);
		File outputfile = new File(name + "_" + "annotated.png");
		ImageIO.write(image, "png", outputfile);

	}

	/*******************************************************************************************************************************************/

	public static List<BufferedImage> findObjects(BufferedImage image)
			throws IOException {

		List<BufferedImage> positive = new ArrayList<BufferedImage>();

		// generate candidate contours
		Collection<List<PointIndex_I32>> candidates = getCandidates(image,
				blurRadius, threshLow, threshHigh, toleranceDist,
				toleranceAngle, dynamicThreshold);

		for (List<PointIndex_I32> vertices : candidates) {
			try {
				Candidate c = new Candidate(vertices, image);
				if (c.size(minsize)) {
					c.rotate();
					positive.add(c.getImage());
				}
			} catch (Exception e) {
				System.out.println("Error creating candidate from contour "
						+ e.getMessage());
			}
		}

		return positive;

	}

	/*******************************************************************************************************************************************/

	private static List<Candidate> evalCandidates(File file, BufferedImage image) {

		List<Candidate> candidatesToClassify = new ArrayList<Candidate>();

		// get candidate contours
		Collection<List<PointIndex_I32>> candidates = getCandidates(image,
				blurRadius, threshLow, threshHigh, toleranceDist,
				toleranceAngle, dynamicThreshold);

		for (List<PointIndex_I32> vertices : candidates) {
			try {
				Candidate c = new Candidate(vertices, image);
				if (c.size(minsize)) {
					c.rotate();
					candidatesToClassify.add(c);
				}
			} catch (Exception e) {
				// System.out.println("Error creating candidate from contour " +
				// e.getMessage());
			}
		}
		return candidatesToClassify;
	}

	/*******************************************************************************************************************************************/

	private static List<List<PointIndex_I32>> getCandidates(
			BufferedImage image, int blurRadius, float threshLow,
			float threshHigh, double toleranceDist, double toleranceAngle,
			boolean dynamicThreshold) {

		List<List<PointIndex_I32>> candidates = new ArrayList<List<PointIndex_I32>>();

		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image,
				null, ImageFloat32.class);

		ImageUInt8 binary = new ImageUInt8(input.width, input.height);

		// Finds edges inside the image
		CannyEdge<ImageFloat32, ImageFloat32> canny = FactoryEdgeDetectors
				.canny(blurRadius, false, dynamicThreshold, ImageFloat32.class,
						ImageFloat32.class);

		canny.process(input, threshLow, threshHigh, binary);

		List<Contour> contours = BinaryImageOps.contour(binary, rule, null);

		for (Contour c : contours) {
			// Only the external contours are relevant.
			List<PointIndex_I32> vertices = ShapeFittingOps.fitPolygon(
					c.external, true, toleranceDist, toleranceAngle, 100);
			candidates.add(vertices);
		}
		return candidates;

	}
}
