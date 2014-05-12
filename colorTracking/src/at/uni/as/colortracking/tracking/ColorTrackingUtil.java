package at.uni.as.colortracking.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ColorTrackingUtil {
	private static final int BLUR_FACTOR = 11; // needs to be odd
	public static int CONTOUR_SIZE_MIN = 25;
	public static int FOREGROUND_TOLERANCE_H = 25;
	public static int FOREGROUND_TOLERANCE_S = 50;
	public static int FOREGROUND_TOLERANCE_V = 50;
	
    public static final Scalar DEFAULT_TOL_HSV = new Scalar(5, 40, 40);
	private static final double DETECTION_AREA_MIN = 25;
	private static final int TRACKED_RECT_THICKNESS = 3;

	/**
	 * Returns the homography matrix of the image.
	 * 
	 * @param mRgba
	 *            Image.
	 * @return Homography matrix.
	 */
	public static Mat getHomographyMatrix(Mat mRgba) {
		Mat gray = new Mat();
		final Size mPatternSize = new Size(6, 9);
		MatOfPoint2f mCorners, RealWorldC;
		mCorners = new MatOfPoint2f();
		Mat homography = new Mat();
		boolean mPatternWasFound = false;

		// second version of points, in real world coordinates X=width, Y=height
		RealWorldC = new MatOfPoint2f(new Point(-48.0f, 309.0f), new Point(
				-48.0f, 321.0f), new Point(-48.0f, 333.0f), new Point(-48.0f,
				345.0f), new Point(-48.0f, 357.0f), new Point(-48.0f, 369.0f),
				new Point(-36.0f, 309.0f), new Point(-36.0f, 321.0f),
				new Point(-36.0f, 333.0f), new Point(-36.0f, 345.0f),
				new Point(-36.0f, 357.0f), new Point(-36.0f, 369.0f),
				new Point(-24.0f, 309.0f), new Point(-24.0f, 321.0f),
				new Point(-24.0f, 333.0f), new Point(-24.0f, 345.0f),
				new Point(-24.0f, 357.0f), new Point(-24.0f, 369.0f),
				new Point(-12.0f, 309.0f), new Point(-12.0f, 321.0f),
				new Point(-12.0f, 333.0f), new Point(-12.0f, 345.0f),
				new Point(-12.0f, 357.0f), new Point(-12.0f, 369.0f),
				new Point(0.0f, 309.0f), new Point(0.0f, 321.0f), new Point(
						0.0f, 333.0f), new Point(0.0f, 345.0f), new Point(0.0f,
						357.0f), new Point(0.0f, 369.0f), new Point(12.0f,
						309.0f), new Point(12.0f, 321.0f), new Point(12.0f,
						333.0f), new Point(12.0f, 345.0f), new Point(12.0f,
						357.0f), new Point(12.0f, 369.0f), new Point(24.0f,
						309.0f), new Point(24.0f, 321.0f), new Point(24.0f,
						333.0f), new Point(24.0f, 345.0f), new Point(24.0f,
						357.0f), new Point(24.0f, 369.0f), new Point(36.0f,
						309.0f), new Point(36.0f, 321.0f), new Point(36.0f,
						333.0f), new Point(36.0f, 345.0f), new Point(36.0f,
						357.0f), new Point(36.0f, 369.0f), new Point(48.0f,
						309.0f), new Point(48.0f, 321.0f), new Point(48.0f,
						333.0f), new Point(48.0f, 345.0f), new Point(48.0f,
						357.0f), new Point(48.0f, 369.0f));

		Imgproc.cvtColor(mRgba, gray, Imgproc.COLOR_RGBA2GRAY);
		// getting inner corners of chessboard
		List<Mat> mCornersBuffer = new ArrayList<Mat>();
		mPatternWasFound = Calib3d.findChessboardCorners(gray, mPatternSize,
				mCorners);

		if (mPatternWasFound) {
			Mat cornersClone = mCorners.clone();
			mCornersBuffer.add(cornersClone);
			homography = Calib3d.findHomography(mCorners, RealWorldC);
			cornersClone.release();
		}

		gray.release();
		mCorners.release();
		RealWorldC.release();
		mCornersBuffer.clear();

		return homography;
	}

	/**
	 * Extracts the foreground picture.
	 * 
	 * @param x
	 *            X-Coordinate of touched point.
	 * @param y
	 *            Y-Coordinate of touched point.
	 * @param image
	 *            Matrix, which contains image with RGBA format.
	 * @return
	 */
	public static Mat getForegroundImage(int x, int y, Mat image) {
		int cols = image.cols();
		int rows = image.rows();
		
		// sample rectangle
		Rect touchedRect = new Rect();
		touchedRect.x = (x > 4) ? x - 4 : 0;
		touchedRect.y = (y > 4) ? y - 4 : 0;
		touchedRect.width  = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
		touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;
		Mat touchedRegionRgba = image.submat(touchedRect);
		
		Mat touchedRegionHsv = new Mat();
		Mat mEmpty = new Mat();
		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
		
		// Calculate average color of touched region
		Scalar hsvColor = Core.sumElems(touchedRegionHsv);;
		int pointCount = touchedRect.width * touchedRect.height;
		for (int i = 0; i < hsvColor.val.length; i++)
			hsvColor.val[i] /= pointCount;
		
		//tolerances
		Scalar mColorRadius = new Scalar(FOREGROUND_TOLERANCE_H, FOREGROUND_TOLERANCE_S, FOREGROUND_TOLERANCE_V, 0);
		
		double minH = (hsvColor.val[0] - mColorRadius.val[0] > 0) 	? hsvColor.val[0] - mColorRadius.val[0]
																	: 255 - hsvColor.val[0] - mColorRadius.val[0];
		double maxH = (hsvColor.val[0] + mColorRadius.val[0] < 255) ? hsvColor.val[0] + mColorRadius.val[0]
																	: hsvColor.val[0] + mColorRadius.val[0] - 255;

		Mat mPyrDownMat = new Mat();
		Mat mHsvMat = new Mat();
		Mat mMask = new Mat();
		Mat mDilatedMask = new Mat();
		Mat mHierarchy = new Mat();

		// image processing
		Imgproc.pyrDown(image, mPyrDownMat);
		Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
		Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
		
		// get binary image from ranges
		Scalar mLowerBound = new Scalar(0);
		Scalar mUpperBound = new Scalar(0);
		mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
		mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];
		mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
		mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];
		mLowerBound.val[3] = 0;
		mUpperBound.val[3] = 255;
		
		if(minH < maxH) {
			mLowerBound.val[0] = minH;
			mUpperBound.val[0] = maxH;
			Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
		} else {
			Mat min = new Mat();
			Mat max = new Mat();

			mLowerBound.val[0] = minH;
			mUpperBound.val[0] = 255;
			Core.inRange(mHsvMat, mLowerBound, mUpperBound, min);
			
			mLowerBound.val[0] = 0;
			mUpperBound.val[0] = maxH;
			Core.inRange(mHsvMat, mLowerBound, mUpperBound, max);
			
			Core.bitwise_or(min, max, mMask);
		}
		mDilatedMask = ColorTrackingUtil.segment(mMask);

		// finding contours in image
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(mDilatedMask, contours, mHierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);	

		// Find max contour area
		double maxArea = 0;
		MatOfPoint biggest = null;
		for(MatOfPoint c : contours) {
			double area = Imgproc.contourArea(c);
			if (area > maxArea) {
				maxArea = area;
				Core.multiply(c, new Scalar(4, 4), c);
				biggest = c;
			}
		}

		Mat cli = null;	
		if(biggest != null) 
			cli = image.submat(Imgproc.boundingRect(biggest));
		
		// releasing memory
		for(MatOfPoint c : contours)
			c.release();
		touchedRegionRgba.release();
		touchedRegionHsv.release();
		mPyrDownMat.release();
		mHsvMat.release();
		mMask.release();
		mDilatedMask.release();
		mHierarchy.release();
		mEmpty.release();

		return cli;
	}
	
	/**
	 * Segments the image with eroding, dilating and smoothing.
	 * 
	 * @param mImg
	 *            Image.
	 * @return Segmented image.
	 */
	public static Mat segment(Mat img) {
		Imgproc.erode(img, img, new Mat());
		Imgproc.dilate(img, img, new Mat());
		Imgproc.medianBlur(img, img, BLUR_FACTOR);

		return img;
	}

	/**
	 * Segments all images in the list with eroding, dilating and smoothing.
	 * 
	 * @param imgs
	 *            List of images.
	 * @return List of segmented images.
	 */
	public static List<Mat> segment(List<Mat> imgs) {
		List<Mat> segments = new ArrayList<Mat>();

		for (Mat m : imgs)
			segments.add(segment(m));

		return segments;
	}

	public static boolean hasCoordFormat(String value) {
		try {
			String[] vals = value.split(":");
			if (vals.length != 2)
				return false;
			
			Double.parseDouble(vals[0]);
			Double.parseDouble(vals[1]);
		} catch(Exception e) {
			return false;
		}
		
		return true;
	}
	
	public static Point parseCoords(String s) {
		if(!hasCoordFormat(s))
			return null;
		
		Point p = null;
		try {
			String[] vals = s.split(":");
			if (vals.length != 2)
				return null;
			
			p = new Point(Double.parseDouble(vals[0]), Double.parseDouble(vals[1]));
		} catch(Exception e) {
			return null;
		}
		
		return p;
	}
	
	public static List<Point> parseCoordsList(String s) {
		if(s == null)
			return null;
		
		String[] coordPairs = s.split("\n");
		List<Point> coordList = new ArrayList<Point>();
		for(int i=0; i < coordPairs.length; i++) {
			try {
				String[] coords = coordPairs[i].split(":");
				double x = Double.valueOf(coords[0]);
				double y = Double.valueOf(coords[1]);
				
				coordList.add(new Point(x, y));
			} catch(Exception e) {
				return null;
			}
		}
		
		return coordList;
	}
	
	public static List<TrackedColor> detectColor(Mat img, Color color, Scalar tolerance) {
		Mat imgRgb = null;
		
		if(img == null || tolerance == null)
			return null;
		
		imgRgb = img.clone();
		
		
		Mat imgHsv = new Mat();
		Imgproc.cvtColor(imgRgb, imgHsv, Imgproc.COLOR_RGB2HSV_FULL);
		
		Scalar lowerBound = new Scalar(color.color().val[0] - tolerance.val[0], color.color().val[1] - tolerance.val[1], color.color().val[2] - tolerance.val[2]);
		Scalar upperBound = new Scalar(color.color().val[0] + tolerance.val[0], color.color().val[1] + tolerance.val[1], color.color().val[2] + tolerance.val[2]);
		
		Mat imgBinary = new Mat();
		Core.inRange(imgHsv, lowerBound, upperBound, imgBinary);
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.dilate(imgBinary, imgBinary, new Mat());
		Imgproc.findContours(imgBinary, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// fill blobs
        List<TrackedColor> detected = new ArrayList<TrackedColor>();
        for (MatOfPoint m : contours) {
            Core.multiply(m, new Scalar(4, 4), m);
            TrackedColor c = new TrackedColor(color, Imgproc.boundingRect(m));
            if (c.getBorders().area() >= DETECTION_AREA_MIN)
                detected.add(c);
        }
		
		imgRgb.release();
		imgHsv.release();
		imgBinary.release();
		
		return detected;
	}
	
	public static Map<Color, List<TrackedColor>> detectColors(Mat rgba) {
		if(rgba == null)
			return null;
		
		Mat rgb = new Mat();
		Mat hsv = new Mat();
		
		Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);
		Imgproc.pyrDown(rgb, rgb);
		Imgproc.pyrDown(rgb, rgb);
		Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV_FULL);
		
		Map<Color, List<TrackedColor>> detectedObjects = new HashMap<Color, List<TrackedColor>>();
		for(Color c : Color.values()) {
			List<TrackedColor> l = ColorTrackingUtil.detectColor(hsv, c, ColorTrackingUtil.DEFAULT_TOL_HSV);
			
			if(l != null && l.size() > 0)
				detectedObjects.put(c, l);
		}
		
		rgb.release();
		hsv.release();
		
		return detectedObjects;
	}

	public static Mat drawTrackedColors(Mat image, Map<Color, List<TrackedColor>> trackedColors) {
		for(Color c : trackedColors.keySet()) {
			for(TrackedColor t : trackedColors.get(c)) {
				Rect rect = t.getBorders();
				Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), c.color(), TRACKED_RECT_THICKNESS);
			}
		}
		
		return image;
	}
}
