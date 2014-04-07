package org.opencv.samples.tutorial1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

public class Tutorial1Activity extends Activity implements
		CvCameraViewListener2, OnTouchListener {
	private static final String TAG = "OCVSample::Activity";
	private static final float displayHeight = 1280;
	private static final float displayWidth = 720;
	private static final float touchX = displayHeight / 2;
	private static final float touchY = displayWidth / 2;

	private CameraBridgeViewBase mOpenCvCameraView;
	private boolean mIsJavaCamera = true;
	// Menu Items
	private MenuItem menuItemSwitchCamera = null;
	private MenuItem menuCaptureImage = null;
	private MenuItem menuCalibrateRed = null;
	private MenuItem menuCalibrateGreen = null;
	private MenuItem menuCalibrateBlue = null;
	private MenuItem menuCalibrateYellow = null;
	private MenuItem menuCalibrateOrange = null;
	private MenuItem menuCalibrateWhite = null;
	private MenuItem menuHomography = null;
	private MenuStatus menuState = MenuStatus.INIT;
	// Matrices
	private Mat mCapture = null;
	private Mat mProbabilityRed = null;
	private Mat mProbabilityGreen = null;
	private Mat mProbabilityBlue = null;
	private Mat mProbabilityYellow = null;
	private Mat mProbabilityOrange = null;
	private Mat mProbabilityWhite = null;
	private Mat mHomography = null;
	private boolean touchedObj = false;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public Tutorial1Activity() {
		Log.i(TAG, "Instantiated new " + this.getClass());

	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.tutorial1_surface_view);

		if (mIsJavaCamera)
			mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
		else
			mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);

		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnTouchListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		// this.menuItemSwitchCamera = menu.add("Toggle Native/Java camera");
		// this.mCaptureImage = menu.add("Capture image");
		this.menuCalibrateRed = menu.add("Calibrate RED");
		this.menuCalibrateGreen = menu.add("Calibrate GREEN");
		this.menuCalibrateBlue = menu.add("Calibrate BLUE");
		this.menuCalibrateYellow = menu.add("Calibrate YELLOW");
		this.menuCalibrateOrange = menu.add("Calibrate ORANGE");
		this.menuCalibrateWhite = menu.add("Calibrate WHITE");
		this.menuHomography = menu.add("Calibrate HOMOGRAPHY");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean saved = false;
		String toastMesage = new String();
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item == menuItemSwitchCamera) {
			mOpenCvCameraView.setVisibility(SurfaceView.GONE);
			mIsJavaCamera = !mIsJavaCamera;

			if (mIsJavaCamera) {
				mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
				toastMesage = "Java Camera";
			} else {
				mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
				toastMesage = "Native Camera";
			}

			mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
			mOpenCvCameraView.setCvCameraViewListener(this);
			mOpenCvCameraView.enableView();
			Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
			toast.show();
		}
		// Save captured image to file.
		else if (item == this.menuCaptureImage) {
			saved = saveToFile(this.mCapture, "capture");
			if (saved) {
				Toast toast = Toast.makeText(this, "Image saved",
						Toast.LENGTH_LONG);
				toast.show();
				saved = false;
			} else {
				Toast toast = Toast.makeText(this, "Could not save image",
						Toast.LENGTH_LONG);
				toast.show();
			}
		}
		// Calibrate probability matrix for red.
		else if (item == this.menuCalibrateRed) {
			this.menuState = MenuStatus.RED;
			Toast toast = Toast.makeText(this,
					"Touch to calibrate the RED probability matrix",
					Toast.LENGTH_LONG);
			toast.show();
		}
		// Calibrate probability matrix for green.
		else if (item == this.menuCalibrateGreen) {
			this.menuState = MenuStatus.GREEN;
			Toast toast = Toast.makeText(this,
					"Touch to calibrate the GREEN probability matrix",
					Toast.LENGTH_LONG);
			toast.show();
		}
		// Calibrate probability matrix for blue.
		else if (item == this.menuCalibrateBlue) {
			this.menuState = MenuStatus.BLUE;
			Toast toast = Toast.makeText(this,
					"Touch to calibrate the BLUE probability matrix",
					Toast.LENGTH_LONG);
			toast.show();
		}
		// Calibrate probability matrix for yellow.
		else if (item == this.menuCalibrateYellow) {
			this.menuState = MenuStatus.YELLOW;
			Toast toast = Toast.makeText(this,
					"Touch to calibrate the YELLOW probability matrix",
					Toast.LENGTH_LONG);
			toast.show();
		}
		// Calibrate probability matrix for orange.
		else if (item == this.menuCalibrateOrange) {
			this.menuState = MenuStatus.ORANGE;
			Toast toast = Toast.makeText(this,
					"Touch to calibrate the ORANGE probability matrix",
					Toast.LENGTH_LONG);
			toast.show();
		}
		// Calibrate probability matrix for white.
		else if (item == this.menuCalibrateWhite) {
			this.menuState = MenuStatus.WHITE;
			Toast toast = Toast.makeText(this,
					"Touch to calibrate the WHITE probability matrix",
					Toast.LENGTH_LONG);
			toast.show();
		}
		// Calibrate homography matrix.
		else if (item == this.menuHomography) {
			this.menuState = MenuStatus.HOMOGRAPHY;
			Toast toast = Toast.makeText(this,
					"Touch to calibrate the HOMOGRAPHY matrix",
					Toast.LENGTH_LONG);
			toast.show();
		}

		return true;
	}

	public void onCameraViewStarted(int width, int height) {
	}

	public void onCameraViewStopped() {
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		this.mCapture = inputFrame.rgba();
		// Make copy of captured image.
		Mat mCopy = this.mCapture.clone();

		// Draw cross for color calibration.
		if (isColor(this.menuState)) {
			Core.line(mCopy, new Point(0, displayWidth / 2), new Point(
					displayHeight, touchY), new Scalar(255, 255, 255));
			Core.line(mCopy, new Point(displayHeight / 2, 0), new Point(touchX,
					displayWidth), new Scalar(255, 255, 255));

			return mCopy;
		}

		if (this.touchedObj) {
			if (!isCalibrated()) {
				this.touchedObj = false;
				System.out.println("No probability matrix calibrated");

				return this.mCapture;
			}
			// Back Projection
			List<Mat> backProjects = calcBackProjects(mCopy);
			// Segmentation
			List<Mat> segments = doSegmentations(backProjects);
			// Find bottom points
			List<Point> bottoms = findBottomPoints(segments);

			if (bottoms == null) {
				this.touchedObj = false;
				System.out.println("No bottom point(s) found");

				return this.mCapture;
			}
			// Display bottom points.
			for (Point p : bottoms)
				Core.circle(this.mCapture, p, 10, new Scalar(255, 255, 255), 5);

			if (!isCalibrated(this.mHomography)) {
				this.touchedObj = false;
				System.out.println("No homography matrix calibrated");

				return this.mCapture;
			}

			// Calculate distance to each bottom point.
			List<Double> distances = getDistances(bottoms);
			// Display distances.
			Iterator<Point> iter = bottoms.iterator();
			for (Double d : distances) {
				Point p = iter.next();
				Core.putText(this.mCapture, String.valueOf(Math.round(d))
						+ "mm", new Point(p.x + 15, p.y + 10),
						Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 255, 255));
			}

			backProjects.clear();
			segments.clear();
			bottoms.clear();
			distances.clear();
		}

		mCopy.release();

		return this.mCapture;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_UP:
			if (this.menuState == MenuStatus.OBJECT)
				if (this.touchedObj)
					this.touchedObj = false;
				else
					this.touchedObj = true;
			else {
				calibrateMatrix(this.menuState);
				this.menuState = MenuStatus.OBJECT;
			}

			break;
		}

		return true;
	}

	/**
	 * Calibrates a matrix selected from the menu.
	 * 
	 * @param s
	 *            Menu item.
	 */
	private void calibrateMatrix(MenuStatus s) {
		switch (s.ordinal()) {
		// Calibration of probability matrices
		case 0:
			this.mProbabilityRed = calcProbability(this.mCapture);
			break;
		case 1:
			this.mProbabilityGreen = calcProbability(this.mCapture);
			break;
		case 2:
			this.mProbabilityBlue = calcProbability(this.mCapture);
			break;
		case 3:
			this.mProbabilityYellow = calcProbability(this.mCapture);
			break;
		case 4:
			this.mProbabilityOrange = calcProbability(this.mCapture);
			break;
		case 5:
			this.mProbabilityWhite = calcProbability(this.mCapture);
			break;
		// Calibration of homography matrix
		case 6:
			this.mHomography = getHomographyMatrix(this.mCapture);
		default:
			break;
		}
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
	Mat getForegroundImage(int x, int y, Mat image) {
		int cols = image.cols();
		int rows = image.rows();
		Rect touchedRect = new Rect();
		touchedRect.x = (x > 4) ? x - 4 : 0;
		touchedRect.y = (y > 4) ? y - 4 : 0;
		touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols
				- touchedRect.x;
		touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows
				- touchedRect.y;
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
		double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]
				- mColorRadius.val[0]
				: 0;
		double maxH = (hsvColor.val[0] + mColorRadius.val[0] <= 255) ? hsvColor.val[0]
				+ mColorRadius.val[0]
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
		Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
		Imgproc.resize(mSpectrum, mSpectrum, new Size(200, 64));

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
		Imgproc.findContours(mDilatedMask, contours, mHierarchy,
				Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		// Find max contour area
		double maxArea = 0;
		Iterator<MatOfPoint> each = contours.iterator();
		while (each.hasNext()) {
			wrapper = each.next();
			double area = Imgproc.contourArea(wrapper);
			if (area > maxArea)
				maxArea = area;
		}
		// Filter contours by area and resize to fit the original image size
		List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
		double mMinContourArea = 0.1;
		mContours.clear();
		each = contours.iterator();
		while (each.hasNext()) {
			contour = each.next();
			if (Imgproc.contourArea(contour) > mMinContourArea * maxArea) {
				Core.multiply(contour, new Scalar(4, 4), contour);
				mContours.add(contour);
			}
		}
		Mat cli = new Mat();
		each = contours.iterator();
		MatOfPoint contour1 = each.next();

		Rect box = null;
		box = Imgproc.boundingRect(contour1);

		// conversion from rgba to rgb
		Mat img = new Mat();
		Imgproc.cvtColor(image, img, Imgproc.COLOR_RGBA2RGB);

		cli = image.submat(box);
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
		img.release();
		mContours.clear();
		wrapper.release();
		contour.release();
		mEmpty.release();

		return cli;
	}

	/**
	 * Converts an image from RGB format to RG format with separated channels.
	 * 
	 * @param image
	 *            Image in RGB format.
	 * @return Image with separated channels R and G.
	 */
	public ArrayList<Mat> convertRGB2RG(Mat image) {
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

		Core.divide(chR, new Scalar(255.0), r, CvType.CV_64F);// dividing it at
																// first in
																// order not to
																// lose data
		Core.divide(chG, new Scalar(255.0), g, CvType.CV_64F);// and to have
																// scaled data
																// to values
																// 0-255
		Core.divide(chB, new Scalar(255.0), b, CvType.CV_64F);

		Mat mSum = new MatOfFloat();

		Core.add(r, g, mSum); // making sum
		Core.add(mSum, b, mSum);

		Core.divide(chR, mSum, chR, CvType.CV_64F);// getting RG chromacity
		Core.divide(chG, mSum, chG, CvType.CV_64F);
		Core.divide(chB, mSum, chB, CvType.CV_64F);

		ArrayList<Mat> mv = new ArrayList<Mat>();

		mv.add(0, (Mat) chR); // mv contains rg values of pictures
		mv.add(1, (Mat) chG);

		return mv;
	}

	/**
	 * Calculates the probability matrix of an image.
	 * 
	 * @param mImg
	 *            Image.
	 * @param mProbability
	 *            Probability matrix.
	 */
	private Mat calcProbability(Mat mImg) {
		Mat mRGImg = mImg.clone();
		Mat mHistImg = new Mat();
		Mat mHistFGImg = new MatOfFloat();
		Mat mProbability = new Mat();

		// Convert captured image from RGBA format to RG.
		Imgproc.cvtColor(mRGImg, mRGImg, Imgproc.COLOR_RGBA2RGB);
		List<Mat> RGImgs = convertRGB2RG(mRGImg);
		// Get foreground image from touched X and Y positions and convert
		// to RG format.
		List<Mat> fgRGImgs = convertRGB2RG(getForegroundImage(
				(int) (touchX / 1920 * displayHeight),
				(int) (touchY / 1080 * displayWidth), mImg));

		// Calc histogram of captured image.
		Imgproc.calcHist(RGImgs, new MatOfInt(0, 1), new Mat(), mHistImg,
				new MatOfInt(100, 100), new MatOfFloat(0.0f, 255.0f, 0.0f,
						255.0f));
		// Calc histogram of foreground image.
		Imgproc.calcHist(fgRGImgs, new MatOfInt(0, 1), new Mat(), mHistFGImg,
				new MatOfInt(100, 100), new MatOfFloat(0.0f, 255.0f, 0.0f,
						255.0f));

		// Calc probability := histFG/histI
		Core.divide(mHistFGImg, mHistImg, mProbability);

		mRGImg.release();
		mHistImg.release();
		mHistFGImg.release();
		RGImgs.clear();
		fgRGImgs.clear();

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
	private Mat calcBackProject(Mat mImg, Mat mProbability) {
		Mat mBack = new Mat();

		// Convert from RGBA format to RG.
		Imgproc.cvtColor(mImg, mImg, Imgproc.COLOR_RGBA2RGB);
		List<Mat> RGImgs = convertRGB2RG(mImg);

		Imgproc.calcBackProject(RGImgs, new MatOfInt(0, 1), mProbability,
				mBack, new MatOfFloat(0.0f, 255.0f, 0f, 255.0f), 25.0);
		// Set threshold
		Imgproc.threshold(mBack, mBack, 10, 255.0f, Imgproc.THRESH_BINARY);

		RGImgs.clear();

		return mBack;
	}

	/**
	 * Calculates all possible back projections of the image in order RGBYOW if
	 * all probability matrices are calibrated.
	 * 
	 * @param mImg
	 *            Image.
	 * @return List of back projections.
	 */
	public List<Mat> calcBackProjects(Mat mImg) {
		List<Mat> backProjects = new ArrayList<Mat>();

		if (isCalibrated(this.mProbabilityRed))
			backProjects.add(calcBackProject(mImg, this.mProbabilityRed));
		if (isCalibrated(this.mProbabilityGreen))
			backProjects.add(calcBackProject(mImg, this.mProbabilityGreen));
		if (isCalibrated(this.mProbabilityBlue))
			backProjects.add(calcBackProject(mImg, this.mProbabilityBlue));
		if (isCalibrated(this.mProbabilityYellow))
			backProjects.add(calcBackProject(mImg, this.mProbabilityYellow));
		if (isCalibrated(this.mProbabilityOrange))
			backProjects.add(calcBackProject(mImg, this.mProbabilityOrange));
		if (isCalibrated(this.mProbabilityWhite))
			backProjects.add(calcBackProject(mImg, this.mProbabilityWhite));

		return backProjects;
	}

	/**
	 * Segments the image with eroding, dilating and smoothing.
	 * 
	 * @param mImg
	 *            Image.
	 * @return Segmented image.
	 */
	private Mat doSegmentation(Mat mImg) {
		Mat mCopy = mImg.clone();

		Imgproc.erode(mCopy, mCopy, new Mat());
		Imgproc.dilate(mCopy, mCopy, new Mat());
		Imgproc.medianBlur(mCopy, mCopy, 41);

		return mCopy;
	}

	/**
	 * Segments all images in the list with eroding, dilating and smoothing.
	 * 
	 * @param imgs
	 *            List of images.
	 * @return List of segmented images.
	 */
	public List<Mat> doSegmentations(List<Mat> imgs) {
		List<Mat> segments = new ArrayList<Mat>();

		for (Mat m : imgs)
			segments.add(doSegmentation(m));

		return segments;
	}

	/**
	 * Searches for all bottom points of each image in the list.
	 * 
	 * @param imgs
	 *            List of image objects.
	 * @return List of bottom points.
	 */
	public List<Point> findBottomPoints(List<Mat> imgs) {
		List<Point> points = new ArrayList<Point>();

		for (Mat m : imgs) {
			List<MatOfPoint> contours = findBiggestContours(m);

			if (contours.size() > 0) {
				for (MatOfPoint c : contours) {
					Rect rec = Imgproc.boundingRect(c);
					points.add(new Point(rec.x + rec.width, rec.y + rec.height
							/ 2));
				}
			}

			contours.clear();
		}

		if (points.size() == 0)
			return null;

		return points;
	}

	/**
	 * Searches for the biggest contour(s) in the image.
	 * 
	 * @param mImg
	 *            Image.
	 * @return List of biggest contour(s).
	 */
	private List<MatOfPoint> findBiggestContours(Mat mImg) {
		double var = 0.5;
		double maxArea = 0;
		Mat mCopy = mImg.clone();
		// This list contains all contours in the processed image.
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		// This list where contains only biggest contour(s)
		List<MatOfPoint> contoursBig = new ArrayList<MatOfPoint>();

		Imgproc.findContours(mCopy, contours, new Mat(), Imgproc.RETR_EXTERNAL,
				Imgproc.CHAIN_APPROX_SIMPLE);

		for (MatOfPoint c : contours) {
			double area = Imgproc.contourArea(c);

			// Clear only contours which are 'var'-times greater than maxArea.
			if (area > maxArea * (1 + var)) {
				contoursBig.clear();
				maxArea = area;
			}
			// Add contours which are at least 'var'-times of maxArea.
			if (area >= maxArea * (1 - var))
				contoursBig.add(c);
		}

		contours.clear();
		mCopy.release();

		return contoursBig;
	}

	/**
	 * Returns the homography matrix of the image.
	 * 
	 * @param mRgba
	 *            Image.
	 * @return Homography matrix.
	 */
	private Mat getHomographyMatrix(Mat mRgba) {
		Mat gray = new Mat();
		final Size mPatternSize = new Size(6, 9);
		MatOfPoint2f mCorners, RealWorldC;
		mCorners = new MatOfPoint2f();
		Mat homography = new Mat();
		boolean mPatternWasFound = false;

		// defining real world coordinates
		// RealWorldC=new MatOfPoint2f(new Point(364.4f,-42.6f),new
		// Point(352.5f,-42.6f),new Point(340.6f,-42.6f),new
		// Point(328.7f,-42.6f),new Point(316.8f,-42.6f),new
		// Point(304.9f,-42.6f),new Point(364.4f,-30.7f),new
		// Point(352.5f,-30.7f),new Point(340.6f,-30.7f),new
		// Point(328.7f,-30.7f),new Point(316.8f,-30.7f),new
		// Point(304.9f,-30.7f),new Point(364.4f,-18.8f),new
		// Point(352.5f,-18.8f),new Point(340.6f,-18.8f),new
		// Point(328.7f,-18.8f),new Point(316.8f,-18.8f),new
		// Point(304.9f,-18.8f),new Point(364.4f,-6.9f),new
		// Point(352.5f,-6.9f),new Point(340.6f,-6.9f),new
		// Point(328.7f,-6.9f),new Point(316.8f,-6.9f),new
		// Point(304.9f,-6.9f),new Point(364.4f,5.0f),new Point(352.5f,5.0f),new
		// Point(340.6f,5.0f),new Point(328.7f,5.0f),new Point(316.8f,5.0f),new
		// Point(304.9f,5.0f),new Point(364.4f,16.9f), new
		// Point(352.5f,16.9f),new Point(340.6f,16.9f),new
		// Point(328.7f,16.9f),new Point(316.8f,16.9f),new
		// Point(304.9f,16.9f),new Point(364.4f,28.8f), new
		// Point(352.5f,28.8f),new Point(340.6f,28.8f),new
		// Point(328.7f,28.8f),new Point(316.8f,28.8f),new
		// Point(304.9f,28.8f),new Point(364.4f,40.7f), new
		// Point(352.5f,40.7f),new Point(340.6f,40.7f),new
		// Point(328.7f,40.7f),new Point(316.8f,40.7f),new
		// Point(304.9f,40.7f),new Point(364.4f,52.5f), new
		// Point(352.5f,52.5f),new Point(340.6f,52.5f),new
		// Point(328.7f,52.5f),new Point(316.8f,52.5f),new Point(304.9f,52.5f));

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
				new Point(-12.0, 309.0f), new Point(-12.0, 321.0f), new Point(
						-12.0, 333.0f), new Point(-12.0, 345.0f), new Point(
						-12.0, 357.0f), new Point(-12.0, 369.0f), new Point(
						0.0f, 309.0f), new Point(0.0f, 321.0f), new Point(0.0f,
						333.0f), new Point(0.0f, 345.0f), new Point(0.0f,
						357.0f), new Point(0.0f, 369.0f), new Point(12.0,
						309.0f), new Point(12.0, 321.0f), new Point(12.0,
						333.0f), new Point(12.0, 345.0f), new Point(12.0,
						357.0f), new Point(12.0, 369.0f), new Point(24.0f,
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
			// Calib3d.drawChessboardCorners(mRgba, mPatternSize, mCorners,
			// mPatternWasFound);//for testing
			mCornersBuffer.add(mCorners.clone());
			homography = Calib3d.findHomography(mCorners, RealWorldC);
		}

		gray.release();
		mCorners.release();
		RealWorldC.release();
		mCornersBuffer.clear();

		return homography;
	}

	/**
	 * Calculates the distances from the bottom points of the camera view to the
	 * real world bottom points.
	 * 
	 * @param bottoms
	 *            List of bottom points.
	 * @return List of distances.
	 */
	public List<Double> getDistances(List<Point> bottoms) {
		List<Double> distances = new ArrayList<Double>();
		Mat mSrc = null;
		Mat mDest = null;

		for (Point p : bottoms) {
			mSrc = new Mat(1, 1, CvType.CV_32FC2);
			mDest = new Mat(1, 1, CvType.CV_32FC2);

			mSrc.put(0, 0, new double[] { p.x, p.y });
			// mBottom = new Mat(new Size(1, 3), CvType.CV_32FC2);
			// mDistance = new Mat(new Size(1, 3), CvType.CV_32FC2);

			// Create 3x1 matrix out of bottom point.
			// mBottom.put(0, 0, p.x, 0);
			// mBottom.put(1, 0, p.y, 0);
			// mBottom.put(2, 0, 1, 0);

			// Multiply homography matrix with bottom point.
			Core.perspectiveTransform(mSrc, mDest, this.mHomography);
			// Real world point.
			Point dest = new Point(mDest.get(0, 0)[0], mDest.get(0, 0)[1]);
			// Calc distance with scalar product.
			double dist = Math.sqrt(Math.pow(dest.x, 2) + Math.pow(dest.y, 2));
			distances.add(dist);
		}

		mSrc.release();
		mDest.release();

		return distances;
	}

	/**
	 * Checks if color calibration is selected from the menu.
	 * 
	 * @param s
	 *            Menu item.
	 * @return True if color calibration is selected, false otherwise;
	 */
	private boolean isColor(MenuStatus s) {
		switch (s.ordinal()) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Checks if a specific matrix is calibrated.
	 * 
	 * @param m
	 *            Matrix.
	 * @return True if a specific matrix is calibrated, false otherwise.
	 */
	private boolean isCalibrated(Mat m) {
		return m != null ? true : false;
	}

	/**
	 * Checks if any probability matrix is calibrated.
	 * 
	 * @return True if any probability matrix is calibrated, false if no
	 *         probability matrix is calibrated.
	 */
	private boolean isCalibrated() {
		return isCalibrated(this.mProbabilityRed)
				|| isCalibrated(this.mProbabilityGreen)
				|| isCalibrated(this.mProbabilityBlue)
				|| isCalibrated(this.mProbabilityYellow)
				|| isCalibrated(this.mProbabilityOrange)
				|| isCalibrated(this.mProbabilityWhite);
	}

	/**
	 * Saves image to file.
	 * 
	 * @param img
	 *            Image.
	 * @param name
	 *            Name of file.
	 * @return True if image saved to file, otherwise false.
	 */
	private boolean saveToFile(Mat img, String name) {
		if (img == null)
			return false;

		Mat bgrImg = new Mat();
		String filename = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES).getAbsolutePath()
				+ "/" + name + ".jpg";

		// Convert RGB to BGR for jpg files
		Imgproc.cvtColor(img, bgrImg, Imgproc.COLOR_RGB2BGR);
		Highgui.imwrite(filename, bgrImg);

		bgrImg.release();

		return true;
	}

}
