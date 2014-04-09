package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ColorTracking {
	private static final int BLUR_FACTOR = 41; //needs to be odd
	private static final float BACKPROJ_THRESH_MAX = 80;
	private static final float BACKPROJ_THRESH_MIN =  0;
	private static final float BACKPROJ_THRESH_STP =  1;
	private static final float BACKPROJ_SCALE = 40; 
	
	private Mat homography = null;
	private List<TrackedColor> trackedColors = null;
	private TrackedColor newTracking = null;
	private boolean trackingActive = false;
	private boolean calcProbMap = false;
	private boolean calcHomography = false;
	
	
	public ColorTracking() {
		this.trackedColors = new ArrayList<TrackedColor>();
	}
	
	public void trackColor(String color) {
		calcProbMap = false;
		newTracking = new TrackedColor(color);
	}
	
	public Mat processImage(Mat img, float x, float y) {
		if(newTracking != null && calcProbMap) {
			newTracking.setProbMap(calcProbability(img, x, y));
			calcProbMap = false;
			trackedColors.add(newTracking);
			newTracking = null;
		}
		
		if(calcHomography) {
			homography = ColorTrackingUtil.getHomographyMatrix(img);
			calcHomography = false;
		}
		
		if(trackingActive) {
			Mat trckImg = null;
			Point bottom = null;
			
			for(TrackedColor track : trackedColors) {
				trckImg = backprojection(img, track);
				if(trckImg == null)
					continue;
				
				//trckImg = ColorTrackingUtil.getBiggestContour(segment(trckImg));
				//if(trckImg == null)
					//continue;
				
				bottom = getBottom(segment(trckImg));
				Core.circle(img, bottom, 5, new Scalar(100.0));
				
				if(homography != null)
					Core.putText(img, track.getColor() + ": " + getDistance(bottom, homography), bottom, Core.FONT_HERSHEY_COMPLEX, 1.0, new Scalar(100.0));
				else
					Core.putText(img, track.getColor(), bottom, Core.FONT_HERSHEY_COMPLEX, 1.0, new Scalar(100.0));
				
				trckImg.release();
			}
		}
		
		return img;
	}
	
	/**
	 * Calculates the probability matrix of an image.
	 * 
	 * @param mImg
	 *            Image.
	 * @param mProbability
	 *            Probability matrix.
	 */
	private Mat calcProbability(Mat img, float x, float y) {
		Mat imgRGB = new Mat();
		Mat histImg = new Mat();
		Mat histFGImg = new MatOfFloat();
		Mat mProbability = new Mat();

		// Convert captured image from RGBA format to RG.
		Imgproc.cvtColor(img, imgRGB, Imgproc.COLOR_RGBA2RGB);

		// Calc histogram of captured image.
		Imgproc.calcHist(ColorTrackingUtil.convertRGB2RG(imgRGB), new MatOfInt(0, 1), new Mat(), histImg,
				new MatOfInt(100, 100), new MatOfFloat(0.0f, 255.0f, 0.0f, 255.0f));
		
		// Calc histogram of foreground image.
		Imgproc.calcHist(ColorTrackingUtil.convertRGB2RG(ColorTrackingUtil.getForegroundImage((int)x,(int)y, imgRGB)), new MatOfInt(0, 1), new Mat(), histFGImg,
				new MatOfInt(100, 100), new MatOfFloat(0.0f, 255.0f, 0.0f, 255.0f));

		// Calc probability := histFG/histI
		Core.divide(histFGImg, histImg, mProbability);

		imgRGB.release();
		histImg.release();
		histFGImg.release();

		return mProbability;
	}
	
	/**
	 * Calculates back projection of an image.
	 * 
	 * @param mImg
	 *            Image.
	 * @param mProbability
	 *            Probability matrix.
	 * @return Back projection.
	 */
	private Mat backprojection(Mat img, TrackedColor track) {
		Mat backproj = new Mat();
		Mat imgRGB = new Mat();

		// Convert from RGBA format to RG.
		Imgproc.cvtColor(img, imgRGB, Imgproc.COLOR_RGBA2RGB);
		Imgproc.calcBackProject(ColorTrackingUtil.convertRGB2RG(imgRGB), new MatOfInt(0, 1), track.getProbMap(),
				backproj, new MatOfFloat(0.0f, 255.0f, 0f, 255.0f), BACKPROJ_SCALE);

		if(track.getThreshold() < 0) {
			Mat backprojClone = null;
			Mat res = null;
			double currThreshold = ColorTracking.BACKPROJ_THRESH_MIN;
			
			do {
				if(res != null)
					res.release();
				res = new Mat();
				backprojClone = backproj.clone();
				
				currThreshold += ColorTracking.BACKPROJ_THRESH_STP;
				Imgproc.threshold(backprojClone, res, currThreshold, 255.0f, Imgproc.THRESH_BINARY);
				
				backprojClone.release();
			} while(ColorTrackingUtil.getContours(res).size() == 0 && currThreshold < ColorTracking.BACKPROJ_THRESH_MAX);
			
			if(currThreshold < ColorTracking.BACKPROJ_THRESH_MAX)
				track.setThreshold(currThreshold);
			else 
				return null;
		} 
		
		Imgproc.threshold(backproj, backproj, track.getThreshold(), 255.0f, Imgproc.THRESH_BINARY);
		

		imgRGB.release();
		return backproj;
	}

	/**
	 * Calculates all possible back projections of the image in order RGBYOW if
	 * all probability matrices are calibrated.
	 * 
	 * @param mImg
	 *            Image.
	 * @return List of back projections.
	 */
	public List<Mat> backprojection(Mat img, List<TrackedColor> trackedColors) {
		List<Mat> backprojection = null;

		if(trackedColors.size() == 0)
			return null;
		
		backprojection = new ArrayList<Mat>();
		for(TrackedColor m : trackedColors) {
			backprojection.add(backprojection(img, m));
		}

		return backprojection;
	}
	
	/**
	 * Segments the image with eroding, dilating and smoothing.
	 * 
	 * @param mImg
	 *            Image.
	 * @return Segmented image.
	 */
	private Mat segment(Mat img) {
		Mat copy = img.clone();

		Imgproc.erode(copy, copy, new Mat());
		Imgproc.dilate(copy, copy, new Mat());
		Imgproc.medianBlur(copy, copy, BLUR_FACTOR);

		return copy;
	}

	/**
	 * Segments all images in the list with eroding, dilating and smoothing.
	 * 
	 * @param imgs
	 *            List of images.
	 * @return List of segmented images.
	 */
	public List<Mat> segment(List<Mat> imgs) {
		List<Mat> segments = new ArrayList<Mat>();

		for (Mat m : imgs)
			segments.add(segment(m));

		return segments;
	}
	
	/**
	 * Searches for all bottom points of each image in the list.
	 * 
	 * @param imgs
	 *            List of image objects.
	 * @return List of bottom points.
	 */
	public Point getBottom(Mat img) {
		Point bottom = null;
		
		MatOfPoint contour = ColorTrackingUtil.getBiggestContour(img);

		if(contour == null)
			return null;

		Rect rec = Imgproc.boundingRect(contour);
		bottom = new Point(rec.x + rec.width, rec.y + rec.height / 2);

		return bottom;
	}
	
	/**
	 * Calculates the distances from the bottom points of the camera view to the
	 * real world bottom points.
	 * 
	 * @param bottoms
	 *            List of bottom points.
	 * @return List of distances.
	 */
	public Double getDistance(Point p, Mat homography) {
		Double dist = -1.0;
		Mat src = new Mat(1, 1, CvType.CV_32FC2);
		Mat dst = new Mat(1, 1, CvType.CV_32FC2);
		
		src.put(0, 0, new double[] { p.x, p.y });

		// Multiply homography matrix with bottom point.
		Core.perspectiveTransform(src, dst, homography);
		// Real world point.
		Point dest = new Point(dst.get(0, 0)[0], dst.get(0, 0)[1]);
		// Calc distance with scalar product.
		dist = Math.sqrt(Math.pow(dest.x, 2) + Math.pow(dest.y, 2));	

		src.release();
		dst.release();

		return dist;
	}
	
	public void resetTrackedObjects() {
		trackedColors.clear();
		newTracking = null;
	}
	
	public boolean getTrackingActive(){
		return trackingActive;
	}
	
	public void setTrackingActive(boolean trackingActive) {
		this.trackingActive = trackingActive;
	}
	
	public boolean getCalcProbMap() {
		return calcProbMap;
	}
	
	public void setCalcProbMap(boolean calcProbMap) {
		this.calcProbMap = calcProbMap;
	}
}
