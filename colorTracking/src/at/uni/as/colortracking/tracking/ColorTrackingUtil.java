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
	private static final int BLUR_FACTOR = 7; // needs to be odd
	public static int FOREGROUND_TOLERANCE_H = 25;
	public static int FOREGROUND_TOLERANCE_S = 50;
	public static int FOREGROUND_TOLERANCE_V = 50;

	public static final Scalar DEFAULT_TOL_HSV = new Scalar(10, 120, 120);
	private static final double DETECTION_AREA_MIN = 1000;
	private static final int TRACKED_RECT_THICKNESS = 3;

	/**
	 * Returns the homography matrix of the image.
	 * 
	 * @param mRgba
	 *            Image.
	 * @return Homography matrix.
	 */
	public static Mat calcHomographyMatrix(Mat mRgba) {
		Mat gray = new Mat();
		final Size mPatternSize = new Size(6, 9);
		MatOfPoint2f mCorners, RealWorldC;
		mCorners = new MatOfPoint2f();
		Mat homography = null;
		boolean mPatternWasFound = false;

		// coordinates for phone in horizontal positioning
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

		// No clue if this works to transform the Mat from horizontal to
		// landscape
		// Core.transpose(RealWorldC, RealWorldC);

		Imgproc.cvtColor(mRgba, gray, Imgproc.COLOR_RGBA2GRAY);
		// getting inner corners of chessboard
		List<Mat> mCornersBuffer = new ArrayList<Mat>();
		mPatternWasFound = Calib3d.findChessboardCorners(gray, mPatternSize,
				mCorners);

		if (mPatternWasFound) {
			homography = new Mat();
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

	public static List<TrackedColor> detectColor(Mat img, Color color,
			Scalar tolerance) {

		if (img == null || tolerance == null)
			return null;

		Mat imgHsv = img.clone();

		Scalar lowerBound = new Scalar(color.hsv().val[0] - tolerance.val[0],
				color.hsv().val[1] - tolerance.val[1], color.hsv().val[2]
						- tolerance.val[2]);
		Scalar upperBound = new Scalar(color.hsv().val[0] + tolerance.val[0],
				color.hsv().val[1] + tolerance.val[1], color.hsv().val[2]
						+ tolerance.val[2]);

		Mat imgBinary = new Mat();
		Core.inRange(imgHsv, lowerBound, upperBound, imgBinary);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.dilate(imgBinary, imgBinary, new Mat());
		Imgproc.findContours(imgBinary, contours, new Mat(),
				Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		// fill blobs
		List<TrackedColor> detected = new ArrayList<TrackedColor>();
		for (MatOfPoint m : contours) {
			Core.multiply(m, new Scalar(4, 4), m);
			TrackedColor c = new TrackedColor(color, Imgproc.boundingRect(m));
			if (c.getBorders().area() >= DETECTION_AREA_MIN)
				detected.add(c);
		}

		imgHsv.release();
		imgBinary.release();

		return detected;
	}

	public static Map<Color, List<TrackedColor>> detectColors(Mat rgba) {
		if (rgba == null)
			return null;

		Mat rgb = new Mat();
		Mat hsv = new Mat();

		Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);
		Imgproc.pyrDown(rgb, rgb);
		Imgproc.pyrDown(rgb, rgb);
		Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV);

		Map<Color, List<TrackedColor>> detectedObjects = new HashMap<Color, List<TrackedColor>>();
		for (Color c : Color.values()) {
			List<TrackedColor> l = ColorTrackingUtil.detectColor(hsv, c, c.tol());

			if (l != null && l.size() > 0)
				detectedObjects.put(c, l);
		}

		rgb.release();
		hsv.release();

		return detectedObjects;
	}

	public static Mat drawTrackedColors(Mat image,
			Map<Color, List<TrackedColor>> trackedColors) {
		if (image == null || trackedColors == null)
			return null;

		for (Color c : trackedColors.keySet()) {
			for (TrackedColor t : trackedColors.get(c)) {
				Rect rect = t.getBorders();
				Core.rectangle(image, new Point(rect.x, rect.y), new Point(
						rect.x + rect.width, rect.y + rect.height), c.rgb(),
						TRACKED_RECT_THICKNESS);
			}
		}

		return image;
	}

	// Not HSV full!!
	public static Scalar convertRGB2HSV(Scalar rgb) {
		double r, g, b, m, n, h0, h, s, v;

		r = rgb.val[0] / 255.0;
		g = rgb.val[1] / 255.0;
		b = rgb.val[2] / 255.0;

		m = Math.max(r, Math.max(g, b));
		n = Math.min(r, Math.min(g, b));

		if (m == r)
			h0 = 30 * (g - b) / (m - n);
		else if (m == g)
			h0 = 30 * (2 + (b - r) / (m - n));
		else if (m == b)
			h0 = 30 * (4 + (r - g) / (m - n));
		else
			// m == n
			h0 = 0;

		if (h0 < 0)
			h = h0 + 180;
		else
			h = h0;

		if (m == 0)
			s = 0;
		else
			s = 255 * (m - n) / m;

		v = 255 * m;
		
		return new Scalar(h, s, v);
	}
}
