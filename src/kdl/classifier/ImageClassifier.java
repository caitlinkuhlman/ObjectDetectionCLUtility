package kdl.classifier;
/*This class and all classes in the package kdl.classifier were written by Joseph Paul Cohen*/


import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AdaBoostM1;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import boofcv.gui.image.ShowImages;

public class ImageClassifier implements Serializable{

	static int numThreads = 1;
	static int numAttributes = 4240;
	Classifier classifier = null;
	Instances instances = null;
	static int NORMAL_WIDTH = 200;
	static int NORMAL_HEIGHT = 200;
	
	static BufferedImage scaleImage(BufferedImage image){

		Image temp = image.getScaledInstance(NORMAL_WIDTH, NORMAL_HEIGHT, BufferedImage.SCALE_AREA_AVERAGING);
		BufferedImage workingim = new BufferedImage(NORMAL_WIDTH, NORMAL_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
		workingim.createGraphics().drawImage(temp, 0, 0, null);
		
		return workingim;
	}
	
	public static BufferedImage copyImage(BufferedImage image){

		//Image temp = image.getScaledInstance(NORMAL_WIDTH, NORMAL_HEIGHT, BufferedImage.SCALE_AREA_AVERAGING);
		BufferedImage workingim = new BufferedImage(image.getWidth(), image.getHeight(),image.getType());
		workingim.createGraphics().drawImage(image, 0, 0, null);
		
		return workingim;
	}
	
	
	public void trainingImagesBalance(BufferedImage[] positive, BufferedImage[] negative) throws Exception{
		
		System.out.println("Before Balance: p/n Examples: "  +positive.length + "/" + negative.length);
		
		if (positive.length > negative.length){
			
			ArrayList<BufferedImage> newnegative = new ArrayList<BufferedImage>();
			
			newnegative.addAll(Arrays.asList(negative));
			
			while (newnegative.size() < positive.length){
				
				BufferedImage tocopy = negative[(int) (Math.random()*negative.length)];
				
				BufferedImage copy = deepCopy(tocopy);
				
				newnegative.add(copy);
			}
		
			if (newnegative.size() == positive.length){
			
				trainingImages(positive, newnegative.toArray(new BufferedImage[0]));
			}else{
				
				throw new Exception("Something went wrong");
			}

			
			
			
			
		}else{
		
			ArrayList<BufferedImage> newpositive = new ArrayList<BufferedImage>();
			
			newpositive.addAll(Arrays.asList(positive));
			
			while (newpositive.size() < negative.length){
				
				BufferedImage tocopy = positive[(int) (Math.random()*positive.length)];
				
				BufferedImage copy = deepCopy(tocopy);
				
				newpositive.add(copy);
			}
		
			if (newpositive.size() == negative.length){
			
				trainingImages(newpositive.toArray(new BufferedImage[0]),negative);
			}else{
				
				throw new Exception("Something went wrong");
			}
		}
		
		
	}
	
	static BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}
	
	
	public void trainingImages(BufferedImage[] positive, BufferedImage[] negative) throws Exception{
		
		System.out.println("p/n Examples: "  +positive.length + "/" + negative.length);

		FastVector fvClass = new FastVector(3);
		fvClass.addElement("0"); // unknown class
		fvClass.addElement("1"); // true
		fvClass.addElement("2"); // false
		
		
		FastVector fvWekaAttributes = new FastVector(101);
		for(int i = 0; i < numAttributes; i++){
			
			fvWekaAttributes.addElement(new Attribute("attr" + i));
		}
		fvWekaAttributes.addElement(new Attribute("class",fvClass));
		
		
		instances = new Instances("ImageTraining", fvWekaAttributes, positive.length + negative.length); 
		instances.setClassIndex(numAttributes);
		
		
		final ThreadPoolExecutor es = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
		
		int i = 10;
		
			// extract haar features to create instance objects
			for (final BufferedImage img : positive){
				//System.out.println(img);
				
				//print positive images to file
				try {
				    File outputfile = new File("data/output/positive_examples/pos" + i + ".png");
				    i++;
				    System.out.println("writing positive image " + i);
				    ImageIO.write(img, "png", outputfile);
				} catch (IOException e) {
				    System.out.println(e.getMessage());
				}
				
				es.execute(new Runnable() {
					
					@Override
					public void run() {
						Instance instance = extractFeatures(img);
						instance.setValue(numAttributes, 1);
						instances.add(instance);
						System.out.println("+=+=Threads Left= " + es.getQueue().size());
					}
				});

			}

			for (final BufferedImage img : negative){
				
				//print negative images to file
				try {
				    File outputfile = new File("data/output/negative_examples/neg" + i + ".png");
				    i++;
				    System.out.println("writingnegative image " + i);
				    ImageIO.write(img, "png", outputfile);
				} catch (IOException e) {
				    System.out.println(e.getMessage());
				}
				
				
				es.execute(new Runnable() {
//					
					@Override
					public void run() {
						Instance instance = extractFeatures(img);
						instance.setValue(numAttributes, 2);
						instances.add(instance);
						System.out.println("+=+=Threads Left= " + es.getQueue().size());
					}
				});
			}		
		
		
		es.shutdown();
		
		//wait forever
		es.awaitTermination(9999, TimeUnit.DAYS);
			
			
		//instances.
		// use instance objects to training classifier
		
		classifier = new AdaBoostM1();
		classifier.buildClassifier(instances);
		
		FileWriter f = new FileWriter("lastrun.arff");
		f.write(instances.toString());
		f.flush();
		f.close();
		
		//System.out.println(instances);
		//System.out.println(classifier);
		
	}
	
	
	public void trainingImages(Instances data) throws Exception {
		
		instances = data;
		classifier = new AdaBoostM1();
		instances.setClassIndex(numAttributes);
		classifier.buildClassifier(instances);
		
		
	}
	
	
	
	public double[] classifyImage(BufferedImage test) throws Exception{
		
		if (classifier == null) throw new Exception("Train me");
		
		// extract haar features
		
		Instance instance = extractFeatures(test);
		
		// classify using classifier
		instance.setDataset(instances);
		return classifier.distributionForInstance(instance);
	}
	
	public static boolean show = false;
	
	public static Instance extractFeatures(BufferedImage img){
		
		Instance instance = new Instance(numAttributes+1);
		
		if (show)
			ShowImages.showWindow(img,"");
		
		img  = scaleImage(img);
		
		//print negative images to file
		try {
		    File outputfile = new File("data/output/pad_dyn_sc" + Math.random()+ ".png");
		    ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
		    System.out.println(e.getMessage());
		}
		
		if (show)
			ShowImages.showWindow(img,"");
		
		BufferedImageIntegralImage bii = new BufferedImageIntegralImage(img);	
		

		int count = 0;
		int step  = 10;
		int mask_width = 20;
		for(int y = 0 ; y + mask_width < NORMAL_HEIGHT; y += step){
			for(int x = 0 ; x + mask_width < NORMAL_WIDTH; x += step){
				
				int fea = extractVerticalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
				
				fea = extractHorizontalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
			}
		}
		

		step  = 5;
		mask_width = 20;
		for(int y = 0 ; y + mask_width < NORMAL_HEIGHT; y += step){
			for(int x = 0 ; x + mask_width < NORMAL_WIDTH; x += step){
				
				int fea = extractVerticalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
				
				fea = extractHorizontalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
			}
		}
		
		
		
		step  = 10;
		mask_width = 40;
		for(int y = 0 ; y + mask_width < NORMAL_HEIGHT; y += step){
			for(int x = 0 ; x + mask_width < NORMAL_WIDTH; x += step){
				
				int fea = extractVerticalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
				
				fea = extractHorizontalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
			}
		}
		
		
		step  = 10;
		mask_width = 80;
		for(int y = 0 ; y + mask_width < NORMAL_HEIGHT; y += step){
			for(int x = 0 ; x + mask_width < NORMAL_WIDTH; x += step){
				
				int fea = extractVerticalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
				
				fea = extractHorizontalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
			}
		}
		
		
		
		
		step  = 10;
		mask_width = 100;
		for(int y = 0 ; y + mask_width < NORMAL_HEIGHT; y += step){
			for(int x = 0 ; x + mask_width < NORMAL_WIDTH; x += step){
				
				int fea = extractVerticalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
				
				fea = extractHorizontalHaarFeature(bii,x,y,mask_width,mask_width);
				instance.setValue(count, fea);
				count++;
			}
		}
		
		
		
		//System.out.println("numFeatures: " + count);
		//System.exit(1);
		
		/*
		for (int i = 0; i < numAttributes/2; i++)
			for (int j = 0; j < numAttributes/2; j++)
			
			extractHaarFeature(bii)
			bii.valueAt(0, 0);
			//TODO
			instance.setValue(i, 10);
		}
		*/
		
		
		return instance;
	}
	
	
	
	public static int extractVerticalHaarFeature(BufferedImageIntegralImage bii,int x, int y, int w, int h){
	
		
		/*
		 *	1     2
		 *
		 *  3     4
		 * 
		 *  sum in 1234 = 4 + 1 - (2 + 3)
		 */
		
		int b_one = bii.valueAt(x, y);
		int b_two = bii.valueAt(x+(w/2), y);
		int b_three = bii.valueAt(x, y+h);
		int b_four = bii.valueAt(x+(w/2), y+h);
		
		int b_featureValue = b_four + b_one - (b_two + b_three);
		
		
		int x_1 = x + w/2;  // offset to calculate the white part of the feature
		int w_one = bii.valueAt(x_1, y);
		int w_two = bii.valueAt(x_1+(w/2), y);
		int w_three = bii.valueAt(x_1, y+h);
		int w_four = bii.valueAt(x_1+(w/2), y+h);
		
		int w_featureValue = w_four + w_one - (w_two + w_three);
		
		//System.out.println(b_featureValue + "-" + w_featureValue);
		
		return b_featureValue - w_featureValue;		
	}


	public static int extractHorizontalHaarFeature(BufferedImageIntegralImage bii,int x, int y, int w, int h){
	
		
		/*
		 *	1     2
		 *
		 *  3     4
		 * 
		 *  sum in 1234 = 4 + 1 - (2 + 3)
		 */
		
		int b_one = bii.valueAt(x, y);
		int b_two = bii.valueAt(x+w, y);
		int b_three = bii.valueAt(x, y+h/2);
		int b_four = bii.valueAt(x+w, y+h/2);
		
		int b_featureValue = b_four + b_one - (b_two + b_three);
		
		
		y = y + h/2;  // offset to calculate the white part of the feature
		int w_one = bii.valueAt(x, y);
		int w_two = bii.valueAt(x+w, y);
		int w_three = bii.valueAt(x, y+h/2);
		int w_four = bii.valueAt(x+w, y+h/2);
		
		int w_featureValue = w_four + w_one - (w_two + w_three);
		
		//System.out.println(b_featureValue + "-" + w_featureValue);
		
		return b_featureValue - w_featureValue;		
	}
	
public double[] performCrossValidation() throws Exception{
		
		Evaluation eval = new Evaluation(instances);
		eval.crossValidateModel(classifier, instances, 10, new Random());
		
		double[] scores = {eval.weightedPrecision(), eval.weightedRecall(), eval.weightedFMeasure()};
//		double[] scores = {eval.precision(1), eval.recall(1), eval.fMeasure(1)};
		
		return scores;
		
		
		
	}
	
	
	
}
