package detection;


import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kdl.classifier.DynamicTimeWarping;
import kdl.classifier.ImageClassifier;

import org.apache.commons.math3.analysis.function.Gaussian;

import boofcv.gui.image.ShowImages;
import boofcv.struct.PointIndex_I32;



public class Candidate {
	
	static boolean debug;
	static boolean show;
	
	private BufferedImage baseImage;
	private BufferedImage subImage;
	
	private List<PointIndex_I32> contour;
	private List<PointIndex_I32> boundingBox;
	
	//set once Candidate is rotated
	private int theta;
	private int centerX;
	private int centerY;
	private boolean rotated;
	
	
	public Candidate(List<PointIndex_I32> contour, BufferedImage baseImage) throws Exception{
		
		try{
			this.boundingBox = createRect(contour, true);
			this.baseImage = baseImage;
			this.subImage = getImageRect(boundingBox);
			
		}
		catch(Exception e){
			throw e;
		}
		this.contour = contour;
		this.rotated = false;
	}
	
	
	public BufferedImage getImage(){
		return subImage;
	}
	
	
	public List<PointIndex_I32> getBoundingBox(){
		return boundingBox;
	}
	
	
	public boolean isRotated(){
		return rotated;
	}
	
	public List<PointIndex_I32> getNoPadBox(){
		return createRect(this.contour, false);
	}
	
	//Rotates candidate if not already. Replaces boundingBox and subImage, sets values for theta and center 
	//Returns true if rotation performed.
	public boolean rotate(){
		
		if(!rotated){
			
			//find center
			centerX = boundingBox.get(2).x - Math.abs(boundingBox.get(0).x - boundingBox.get(2).x)/2;
			centerY = boundingBox.get(2).y - Math.abs(boundingBox.get(0).y - boundingBox.get(2).y)/2;
			
			//find angle of rotation
			theta = 90 - detectSlope(contour);
		
			//rotate contour
			List<PointIndex_I32> rotContour = rotateContours(contour, theta, centerX, centerY);
			
			//draw box around rotated contours
			List<PointIndex_I32> box = createRect(rotContour, true);
		
			//rotate box back
			List<PointIndex_I32> rotBox = rotateContours(box, -theta, centerX, centerY);
			boundingBox = rotBox;
		
			//draw new box
			List<PointIndex_I32> cropBox = createRect(rotBox, false);
		
			//crop image
			BufferedImage cropped = getImageRect(cropBox);
		
			//rotate image
			subImage = rotateImage(cropped, box, Math.toRadians(theta));
			
			rotated = true;
			
			return true;
		}
		else
			return false;
	}
	
	
	//returns true if candidate bounding box is at least minsize
	public boolean size(int minsize){
		
		if((boundingBox.get(2).x - boundingBox.get(0).x) > minsize && (boundingBox.get(2).y - boundingBox.get(0).y) > minsize){
			return true;
		}
		else 
			return false;
	}
	
	
	public boolean sameBoundingBox(Candidate that){
		
		if(this.boundingBox.size() != that.boundingBox.size()){
			return false;
		}
		else{
			for(PointIndex_I32 p : this.boundingBox){
				if(p.x != that.boundingBox.get(p.index).x || p.y != that.boundingBox.get(p.index).y)
					return false;
			}
			return true;
		}		
	}
	
	

	
	private List<PointIndex_I32> createRect(List<PointIndex_I32> vertices, boolean pad){

		int l = Integer.MAX_VALUE;
		int r = 0;
		int t = Integer.MAX_VALUE;
		int b = 0;
		
		for (PointIndex_I32 p : vertices){
			
			l = Math.min(p.x,l);
			r = Math.max(p.x,r);
			t = Math.min(p.y,t);
			b = Math.max(p.y,b);
		}
		
		if(pad){
			
			//padding based on size of candidate
			int tol = (Math.abs(l - r) + Math.abs(t - b))/20;
			
//			int tol = 30;
			
			int padding = tol;
			
			l = l-padding;
			r = r+padding;
			t = t-padding;
			b = b+padding;
		}
		
		PointIndex_I32 ul = new PointIndex_I32(l, t, 1);
		PointIndex_I32 ur = new PointIndex_I32(r, t, 2);
		PointIndex_I32 lr = new PointIndex_I32(r, b, 3);
		PointIndex_I32 ll = new PointIndex_I32(l, b, 4);
		
		PointIndex_I32[] ret = {ul,ur,lr,ll};
		
		return Arrays.asList(ret);	
	}
	
	
	private BufferedImage getImageRect(List<PointIndex_I32> vertices){
		
		int l = vertices.get(0).x;
		int r = vertices.get(2).x;
		int t = vertices.get(0).y;
		int b = vertices.get(2).y;
		
		BufferedImage dest = baseImage.getSubimage(l,t,(r-l),(b-t));
		
		return dest;
	}
	
	
	private int detectSlope(List<PointIndex_I32> bldg){
		
		if(debug){
			System.out.println(bldg);
		}
		
		double maxdist = 0;
		for (int i = 0; i < bldg.size(); i++){
			PointIndex_I32 p1 = bldg.get(i);
			PointIndex_I32 p2 = bldg.get((i+1) %bldg.size());
			double dist = DynamicTimeWarping.distance(p1, p2);
			maxdist = Math.max(dist, maxdist);
		}
		
		List<Gaussian> ga = new ArrayList<Gaussian>();
		
		for (int i = 0; i < bldg.size(); i++){
			
			PointIndex_I32 p1 = bldg.get(i);
			PointIndex_I32 p2 = bldg.get((i+1) %bldg.size());
			
			double xdist = (p1.x-p2.x);
			double ydist = (p1.y-p2.y);
			
			if (ydist < 0){
				
				xdist = -xdist;
				ydist = -ydist;
			}
			
			double dist = DynamicTimeWarping.distance(p1, p2);
			
			double radians = Math.atan2(ydist,xdist);
			
			double angle = radians * 360 / (2*Math.PI);
			
			double mean = angle;
			double sigma = 1;
			double norm = dist/maxdist;
			
			ga.add(new Gaussian(norm, mean, sigma));
			
			if (debug){
				System.out.println(p1.x + " " +  p1.y + " " + dist);
				System.out.println("~" + angle + "degrees");
			}
		}
		
		int maxdegree = 0;
		double maxval = 0;
		for (int i = 0;i < 180;i++){
			double val = 0;
			for(Gaussian g : ga){
				val += g.value(i);
			}
			if (maxval < val){
				maxval = val;
				maxdegree = i;
			}
			if(debug){
				System.out.println((int)(val*1000000));
			}
		}
		if(debug){
			System.out.println("Maxdegree = " + (int) maxdegree);
		}
		
		return maxdegree;
	}
	
	
	private List<PointIndex_I32> rotateContours(List<PointIndex_I32> vertices, int slope, int centerX, int centerY){
		
		AffineTransform tx = AffineTransform.getRotateInstance(Math.toRadians(slope), centerX, centerY);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		
		List<Point2D> newVertices = new ArrayList<Point2D>();
		List<PointIndex_I32> rotVertices = new ArrayList<PointIndex_I32>();
		for(PointIndex_I32 p : vertices){
			Point p2 = new Point(p.x, p.y);
			newVertices.add(op.getPoint2D(p2, null));
		}
		int i = 0;
		for(Point2D p : newVertices){
			rotVertices.add(new PointIndex_I32((int) p.getX(), (int) p.getY(), i));
			i++;
		}
		return rotVertices;
	}
	
	
	private BufferedImage rotateImage(BufferedImage image, List<PointIndex_I32> box, double theta){

		int w = Math.abs(box.get(2).x - box.get(0).x);
		int h = Math.abs(box.get(2).y - box.get(0).y);
		
		BufferedImage output = new BufferedImage(w, h, image.getType());
		Graphics2D g2 = output.createGraphics();
	    
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.translate((w - image.getWidth())/2, (h - image.getHeight())/2);
	    g2.rotate(theta, image.getWidth()/2, image.getHeight()/2);
	    g2.drawRenderedImage(image, null);
	    g2.dispose();
	    return output;
	}
	
	
	private double hsearch(double[] h){
		
		//harmonic mean
		double result = 2.0*(h[1] * (1.0-h[2]))/(h[1] + (1.0-h[2]));
		
		// just h+
		//double result = h[1];
		
		return result;
	}
	
	public Candidate findBestCandidate(double[] probs, ImageClassifier classifier, BufferedImage bi, double diffsize) throws Exception{
		
		if (show) Draw.drawPolygonVary(boundingBox, bi, (int) (255 * hsearch(probs)));
//		ShowImages.showDialog(subImage);
		
		if(debug){
			System.out.println("\n\nBest Candidate called");
		}
		double oHeuristic = hsearch(probs);
		double bestHeuristic = oHeuristic;
		double[] bestClassification = null;
		BufferedImage bestImage = null;
		
		if(!rotated){
			rotate();
		}
		
		List<PointIndex_I32> box = rotateContours(boundingBox, theta, centerX, centerY);
		List<PointIndex_I32> bestBox = box;
		List<List<PointIndex_I32>> testBoxes = new ArrayList<List<PointIndex_I32>>();
		try{
			Frame frame = new Frame(box);
			
			if (show){
				
				Draw.drawPolygonWhite(boundingBox, baseImage);
				Draw.drawPolygonBlack(box, baseImage);
				ShowImages.showDialog(baseImage);
			}
			
			int width = Math.abs(box.get(2).x - box.get(0).x);
			int height = Math.abs(box.get(2).y - box.get(0).y);
			
			//vary step size 0.01, 0.02 .. 0.4
			int diff = (int) (diffsize * ((width + height) /2));
			
			if (diff == 0){
				throw new Exception("diff = 0");
			}
			
			frame.setDifference(diff);
			
			testBoxes = frame.permute();
			
			if (show){
				for(List<PointIndex_I32> l : testBoxes){
					
					Draw.drawPolygon(l, baseImage);
					ShowImages.showDialog(baseImage);
				}
				
			}
		}
		catch(Exception e){
			e.printStackTrace(System.out);
		}
	
		/*
		//expand corners
		testBoxes.add(permuteBoxes(box, diffx, diffy, 0, diffy, 0, 0, diffx, 0));
		testBoxes.add(permuteBoxes(box, 0, diffy, -diffx, diffy, -diffx, 0, 0, 0));
		testBoxes.add(permuteBoxes(box, 0, 0, -diffx, 0, -diffx, -diffy, 0, -diffy));
		testBoxes.add(permuteBoxes(box, diffx, 0, 0, 0, 0, -diffy, diffx, -diffy));
		
		//shrink corners
		testBoxes.add(permuteBoxes(box, -diffx, -diffy, 0, -diffy, 0, 0, -diffx, 0));
		testBoxes.add(permuteBoxes(box, 0, -diffy, diffx, -diffy, diffx, 0, 0, 0));
		testBoxes.add(permuteBoxes(box, 0, 0, diffx, 0, diffx, diffy, 0, diffy));
		testBoxes.add(permuteBoxes(box, -diffx, 0, 0, 0, 0, diffy, -diffx, diffy));
		*/

		int count = 0;
		for (List<PointIndex_I32> r : testBoxes){
			
			
			if(debug) System.out.println(count++ + "/" + testBoxes.size());
			
			System.gc();
//			System.out.println("Inside permute About to rotate");
//			System.in.read();
			
			try{
				//rotate box back
				List<PointIndex_I32> rotBox = rotateContours(r, -theta, centerX, centerY);
		
				//draw new box
				List<PointIndex_I32> cropBox = createRect(rotBox, false);
		
				//crop image
				BufferedImage cropped = getImageRect(cropBox);
		
				//rotate image
				BufferedImage rotImage = rotateImage(cropped, r, Math.toRadians(theta));
				
				
				double[] classification = classifier.classifyImage(rotImage);
				
				
				double result = hsearch(classification);

				
//				System.out.println("Prob = " + result);				
				if(result > bestHeuristic){
					bestBox = rotBox;
					bestHeuristic = result;
					bestImage = rotImage;
					bestClassification = classification;
				}
			}
			catch(Exception e){
				System.out.println(e.getMessage());
			}
		}
		if (show){
			Draw.drawPolygonGreen(bestBox, baseImage);
			ShowImages.showDialog(baseImage);
		}
		
//		System.out.println("Best prob " + bestProb);
//		System.out.println("## " + Thread.currentThread().getStackTrace().length);
		System.out.println("Best prob = " + bestHeuristic + " ? prob = " + oHeuristic);
		if(bestHeuristic > oHeuristic){
			System.out.println("Recurse");
			Candidate bestCan = new Candidate(contour, baseImage);
//			bestCan.boundingBox = rotateContours(bestBox, -theta, centerX, centerY);
			bestCan.boundingBox = bestBox;
			bestCan.subImage = bestImage;
			bestCan.theta = theta;
			Draw.drawPolygonBlack(bestBox, bi);
			
//			Draw.drawPolygons(boundingBox, baseImage);

//			BufferedImage image2 = UtilImageIO.loadImage(file.getAbsolutePath());n
//			System.out.println("best Candidate Image");
			
			//find center
			bestCan.centerX = boundingBox.get(2).x - Math.abs(boundingBox.get(0).x - boundingBox.get(2).x)/2;
			bestCan.centerY = boundingBox.get(2).y - Math.abs(boundingBox.get(0).y - boundingBox.get(2).y)/2;
			
			bestCan.rotated = true;
			
			return  bestCan.findBestCandidate(bestClassification, classifier, bi, diffsize);
		}
//		ShowImages.showDialog(bi);
		return this;
	
	}
	
	private List<PointIndex_I32> permuteBoxes(List<PointIndex_I32> can, int diff1, int diff2, int diff3, int diff4, int diff5, int diff6, int diff7, int diff8) throws Exception {
		
		if(can.size() != 4){
			throw new Exception();
		}
		
		PointIndex_I32 ul = new PointIndex_I32(can.get(0).x - diff1, can.get(0).y - diff2,  1);
		PointIndex_I32 ur = new PointIndex_I32(can.get(1).x - diff3, can.get(1).y - diff4, 2);
		PointIndex_I32 lr = new PointIndex_I32(can.get(2).x - diff5, can.get(2).y - diff6, 3);
		PointIndex_I32 ll = new PointIndex_I32(can.get(3).x - diff7, can.get(3).y - diff8, 4);
		
		PointIndex_I32[] ulExp = {ul,ur,lr,ll};
		
		List<PointIndex_I32> box =  Arrays.asList(ulExp);
		
		return box;
				
	}
	
	//8 possible symmetries of a square:
	//4 rotations
	//4 reflections
	public List<BufferedImage> getRotatedImages(){
		List<BufferedImage> images = new ArrayList<BufferedImage>();
		//4 rotations
		for(int i = 0 ; i<= 270 ; i += 90){
			BufferedImage newB = rotateSquare(subImage, i);
			images.add(newB);
		}
		//2 mirror images
		BufferedImage bi1 = verticalflip(subImage);
		images.add(bi1);
		BufferedImage bi2 = horizontalflip(subImage);
		images.add(bi2);
		
		//2 diagonal flips
		BufferedImage bi3 = verticalflip(subImage);
		bi3 = rotateSquare(bi3,270);
		images.add(bi3);
		BufferedImage bi4 = horizontalflip(subImage);
		bi4 = rotateSquare(bi4,270);
		images.add(bi4);
		return images;
	}
	
	static BufferedImage rotateSquare(BufferedImage src, int degrees) {
	    double radians = Math.toRadians(degrees);
	 
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
	
	static BufferedImage verticalflip(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = new BufferedImage(w, h, img.getColorModel()
                .getTransparency());
        Graphics2D g = dimg.createGraphics();
        g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);
        g.dispose();
        return dimg;
    }
 

   static BufferedImage horizontalflip(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = new BufferedImage(w, h, img.getType());
        Graphics2D g = dimg.createGraphics();
        g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
        g.dispose();
        return dimg;
    }

}
