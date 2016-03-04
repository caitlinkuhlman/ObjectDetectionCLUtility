# ObjectDetectionCLUtility
This tool automatically identifies objects in images using Canny Edge Detection. Objects are rotated to best fit in a rectangular frame and cropped from the image. 

This was used as a preprocessing step for automatic annotaion of  buildings in satellite images in the work "Rapid Building Detection using Machine Learning"
by Joseph Paul Cohen, Wei Ding, Caitlin Kuhlman, Aijun Chen, and Liping Di in the journal of Applied Intelligence. 

The [BoofCV](https://github.com/lessthanoptimal/BoofCV) computer vision library is used for edge detection. 

This work is released under an Apache 2.0 license for both academic and commercial use.


## Usage

The tool reads in an image file and outputs a number of new images. Detected objects are cropped out and saved in their own image files. 4 additional copies of the image are produced with the following suffixes:

**_binary**: greyscale version of the image

**_canny**: result of the canny edge detection

**_contour**: canny edges rendered as 'outside contours' (outlines) of object

**_annotated**: original image with the outlines of the detected objects superimposed.

Hysteresis thresholding is used for edge detection. High and low threshold values can be specified as command line arguments. A large high threshold will filter out noise in some images, but miss detail in others. These parameters should be tuned per image. For example, higher values will miss the lighter colored objects in the provided shapes file.

The tool is designed to ignore detected noise, and will only crop out objects at least roughly 10% as large as the image.

usage: crop-objects [OPTION]... FILE [DIR]
crops detected objects from image FILE and writes their subimages to files. Can specify the DIR in which to create the files, 
otherwise subimage files are created in the directory that the FILE is in by default 

-t [n] [m] 		set the high and low threshold values. n and m are values between 0 and 1. default is 0.2 0.4
