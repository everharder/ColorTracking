package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ColorTrackingUtil {
	public static int CONTOUR_SIZE_MIN = 400;
	
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
				new Point(	0.0f, 309.0f), new Point(  0.0f, 321.0f), 
				new Point(  0.0f, 333.0f), new Point(  0.0f, 345.0f), 
				new Point(  0.0f, 357.0f), new Point(  0.0f, 369.0f), 
				new Point( 12.0f, 309.0f), new Point( 12.0f, 321.0f), 
				new Point( 12.0f, 333.0f), new Point( 12.0f, 345.0f), 
				new Point( 12.0f, 357.0f), new Point( 12.0f, 369.0f), 
				new Point( 24.0f, 309.0f), new Point( 24.0f, 321.0f), 
				new Point( 24.0f, 333.0f), new Point( 24.0f, 345.0f), 
				new Point( 24.0f, 357.0f), new Point( 24.0f, 369.0f), 
				new Point( 36.0f, 309.0f), new Point( 36.0f, 321.0f), 
				new Point( 36.0f, 333.0f), new Point( 36.0f, 345.0f), 
				new Point( 36.0f, 357.0f), new Point( 36.0f, 369.0f), 
				new Point( 48.0f, 309.0f), new Point( 48.0f, 321.0f), 
				new Point( 48.0f, 333.0f), new Point( 48.0f, 345.0f), 
				new Point( 48.0f, 357.0f), new Point( 48.0f, 369.0f));

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
	 * Converts an image from RGB format to RG format with separated channels.
	 * 
	 * @param image
	 *            Image in RGB format.
	 * @return Image with separated channels R and G.
	 */
	public static ArrayList<Mat> convertRGB2RG(Mat image) {
		ArrayList<Mat> splitImages = new ArrayList<Mat>();

		Core.split(image, splitImages);// splitting image into 3 channels
		Mat chR = new MatOfFloat();
		Mat chG = new MatOfFloat();
		Mat chB = new MatOfFloat();

		// channels of picture
		chR = (Mat) splitImages.get(0);
		chG = (Mat) splitImages.get(1);
		chB = (Mat) splitImages.get(2);

		Mat r = new MatOfFloat();
		Mat g = new MatOfFloat();
		Mat b = new MatOfFloat();

		Core.divide(chR, new Scalar(255.0), r, CvType.CV_64F);
		Core.divide(chG, new Scalar(255.0), g, CvType.CV_64F);
		Core.divide(chB, new Scalar(255.0), b, CvType.CV_64F);

		Mat mSum = new MatOfFloat();
		Core.add(r, g, mSum);
		Core.add(mSum, b, mSum);

		Core.divide(chR, mSum, chR, CvType.CV_64F);
		Core.divide(chG, mSum, chG, CvType.CV_64F);
		Core.divide(chB, mSum, chB, CvType.CV_64F);

		ArrayList<Mat> mv = new ArrayList<Mat>();
		mv.add(0, (Mat) chR);
		mv.add(1, (Mat) chG);

		return mv;
	}

	/**
	 * Searches for the biggest contour(s) in the image.
	 * 
	 * @param mImg
	 *            Image.
	 * @return List of biggest contour(s).
	 */
	public static MatOfPoint getBiggestContour(Mat img) {
		List<MatOfPoint> contours = getContours(img);

		if (contours.size() == 0)
			return null;

		MatOfPoint biggest = getBiggestContour(contours, CONTOUR_SIZE_MIN);

		// release memory
		for (MatOfPoint c : contours)
			if (c != biggest)
				c.release();

		return biggest;
	}

	public static MatOfPoint getBiggestContour(List<MatOfPoint> contours,
			double minSize) {
		double var = 0.5;
		double maxArea = 0;

		MatOfPoint biggest = null;
		for (MatOfPoint c : contours) {
			double area = Imgproc.contourArea(c);

			if (area < minSize)
				continue;

			if (area > maxArea * (1 + var)) {
				biggest = c;
				maxArea = area;
			}
		}

		return biggest;
	}

	public static List<MatOfPoint> getContours(Mat img) {
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat clone = img.clone();

		Imgproc.findContours(clone, contours, new Mat(), Imgproc.RETR_EXTERNAL,
				Imgproc.CHAIN_APPROX_SIMPLE);

		clone.release();

		return contours;
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
		Rect touchedRect = new Rect();
		
		touchedRect.x = (x > 4) ? x - 4 : 0;
		touchedRect.y = (y > 4) ? y - 4 : 0;
		touchedRect.width = (x + 4 < cols) 	? x + 4 - touchedRect.x 
											: cols - touchedRect.x;
		touchedRect.height = (y + 4 < rows)	? y + 4 - touchedRect.y  
											: rows - touchedRect.y;
		
		Mat touchedRegionRgba = image.submat(touchedRect);
		Mat touchedRegionHsv = new Mat();
		Mat mEmpty = new Mat();
		
		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv,
				Imgproc.COLOR_RGB2HSV_FULL);
		
		// Calculate average color of touched region
		Scalar hsvColor;
		hsvColor = Core.sumElems(touchedRegionHsv);
		int pointCount = touchedRect.width * touchedRect.height;
		for (int i = 0; i < hsvColor.val.length; i++)
			hsvColor.val[i] /= pointCount;
		Scalar mColorRadius = new Scalar(25, 50, 50, 0);
		Scalar mLowerBound = new Scalar(0);
		Scalar mUpperBound = new Scalar(0);
		
		// spectrum
		double minH = (hsvColor.val[0] - mColorRadius.val[0] > 0) 	? hsvColor.val[0] - mColorRadius.val[0]
																	: 0;
		double maxH = (hsvColor.val[0] + mColorRadius.val[0] < 255) ? hsvColor.val[0] + mColorRadius.val[0]
																	: 255;
		
		mLowerBound.val[0] = minH;
		mUpperBound.val[0] = maxH;
		mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
		mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];
		mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
		mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];
		mLowerBound.val[3] = 0;
		mUpperBound.val[3] = 255;
		
		Mat spectrumHsv = new Mat(1, (int) (maxH - minH), CvType.CV_8UC3);
		for (int j = 0; j < maxH - minH; j++) {
			byte[] tmp = { (byte) (minH + j), (byte) 255, (byte) 255 };
			spectrumHsv.put(0, j, tmp);
		}
		Mat mSpectrum = new Mat();
		//Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
		//Imgproc.resize(mSpectrum, mSpectrum, new Size(200, 64));

		Mat mPyrDownMat = new Mat();
		Mat mHsvMat = new Mat();
		Mat mMask = new Mat();
		Mat mDilatedMask = new Mat();
		Mat mHierarchy = new Mat();
		MatOfPoint wrapper = new MatOfPoint();
		MatOfPoint contour = new MatOfPoint();

		// image processing
		Imgproc.pyrDown(image, mPyrDownMat);
		Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
		Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
		Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
		Imgproc.dilate(mMask, mDilatedMask, mEmpty);

		// finding contours in image
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(mDilatedMask, contours, mHierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		// Find max contour area
		double maxArea = 0;
		for(MatOfPoint c : contours){
			double area = Imgproc.contourArea(c);
			if (area > maxArea)
				maxArea = area;
		}
		
		// Filter contours by area and resize to fit the original image size
		List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
		double mMinContourArea = 0.1;
		for(MatOfPoint c : contours) {
			if (Imgproc.contourArea(c) > mMinContourArea * maxArea) {
				Core.multiply(c, new Scalar(4, 4), c);
				mContours.add(c);
			}
		}
		Mat cli = new Mat();
		cli = image.submat(Imgproc.boundingRect(mContours.get(0)));
		
		// releasing matrix
		touchedRegionRgba.release();
		touchedRegionHsv.release();
		spectrumHsv.release();
		mPyrDownMat.release();
		mHsvMat.release();
		mMask.release();
		mSpectrum.release();
		mDilatedMask.release();
		mHierarchy.release();
		contours.clear();
		mContours.clear();
		wrapper.release();
		contour.release();
		mEmpty.release();

		return cli;

	}
}
