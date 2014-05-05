package at.uni.as.colortracking;

import java.text.DecimalFormat;
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

import android.content.res.Resources.NotFoundException;

public class ColorTracking {

	private static final float BACKPROJ_THRESH_MAX = 30;
	private static final float BACKPROJ_THRESH_MIN = 1;
	private static final float BACKPROJ_THRESH_STP = 1;
	private static final float BACKPROJ_SCALE = 15;
	private static final float SEGMENT_AREA_MIN = 200;

	private Mat homography = null;
	private List<TrackedObject> trackedObjects = null;
	private TrackedObject newTracking = null;
	private boolean trackingActive = false;
	private boolean calcProbMap = false;
	private boolean calcHomography = false;

	public ColorTracking() {
		this.trackedObjects = new ArrayList<TrackedObject>();
	}

	public void trackColor(String label, int trackCount) {
		calcProbMap = false;
		newTracking = new TrackedObject(label, trackCount);
	}

	public Mat processImage(Mat img, int x, int y) throws NotFoundException {
		if (newTracking != null
				&& newTracking.getTracks().size() < newTracking.getTrackCount()
				&& calcProbMap) {
			Mat probMap = calcProbability(img, x, y);
			if (probMap == null)
				throw new NotFoundException();
			else
				newTracking.addColorTrack(probMap);

			calcProbMap = false;

			if (newTracking.getTracks().size() == newTracking.getTrackCount()) {
				trackedObjects.add(newTracking);
				newTracking = null;
			}
		}

		if (calcHomography) {
			homography = ColorTrackingUtil.getHomographyMatrix(img);
			calcHomography = false;
		}

		if (trackingActive) {
			Mat trckImg = null;
			List<Point> bottom = null;

			for (TrackedObject trackedObject : trackedObjects) {

				trackedObject.resetDists();
				for (int i = 0; i < trackedObject.getTracks().size(); i++) {
					trckImg = backprojection(img, trackedObject.getTrack(i));

					if (trckImg == null)
						continue;

					bottom = getBottom(ColorTrackingUtil.segment(trckImg));
					if (bottom == null || bottom.size() == 0)
						continue;

					for (Point p : bottom) {
						Core.circle(img, p, 5, new Scalar(0.0));

						if (homography != null) {
							trackedObject.getTrack(i).addDist(
									getDistance(p, homography) / 10.0);
							Core.putText(
									img,
									trackedObject.getLabel()
											+ ": "
											+ DecimalFormat
													.getIntegerInstance()
													.format(trackedObject
															.getTrack(i)
															.getDist()
															.get(trackedObject
																	.getTrack(i)
																	.getDist()
																	.size() - 1)),
									new Point(p.x + 4, p.y),
									Core.FONT_HERSHEY_SIMPLEX, 0.75,
									new Scalar(50.0));
						} else {
							Core.putText(img, trackedObject.getLabel(),
									new Point(p.x + 4, p.y),
									Core.FONT_HERSHEY_SIMPLEX, 0.75,
									new Scalar(50.0));
						}
					}

					trckImg.release();
				}
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
	private Mat calcProbability(Mat img, int x, int y) {
		// Convert captured image from RGBA format to RG
		Mat imgRGB = new Mat();
		Imgproc.cvtColor(img, imgRGB, Imgproc.COLOR_RGBA2RGB);

		// Calc histogram of foreground image.
		Mat fg = ColorTrackingUtil.getForegroundImage(x, y, imgRGB);
		if (fg == null)
			return null;

		Mat histFGImg = new MatOfFloat();
		Imgproc.calcHist(ColorTrackingUtil.convertRGB2RG(fg),
				new MatOfInt(0, 1), new Mat(), histFGImg,
				new MatOfInt(100, 100), new MatOfFloat(0.0f, 255.0f, 0.0f,
						255.0f));

		// Calc histogram of captured image.
		Mat histImg = new Mat();
		Imgproc.calcHist(ColorTrackingUtil.convertRGB2RG(imgRGB), new MatOfInt(
				0, 1), new Mat(), histImg, new MatOfInt(100, 100),
				new MatOfFloat(0.0f, 255.0f, 0.0f, 255.0f));

		// Calc probability := histFG/histI
		Mat mProbability = new Mat();
		Core.divide(histFGImg, histImg, mProbability);

		fg.release();
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

		// Convert from RGBA fproormat to RG.
		Imgproc.cvtColor(img, imgRGB, Imgproc.COLOR_RGBA2RGB);
		Imgproc.calcBackProject(ColorTrackingUtil.convertRGB2RG(imgRGB),
				new MatOfInt(0, 1), track.getProbMap(), backproj,
				new MatOfFloat(0.0f, 255.0f, 0f, 255.0f), BACKPROJ_SCALE);

		if (track.getThreshold() < 0) {
			Mat backprojClone = null;
			Mat res = null;
			MatOfPoint contour = null;
			double currThreshold = ColorTracking.BACKPROJ_THRESH_MIN;

			do {
				if (res != null)
					res.release();
				res = new Mat();
				backprojClone = backproj.clone();

				currThreshold += ColorTracking.BACKPROJ_THRESH_STP;
				Imgproc.threshold(backprojClone, res, currThreshold, 255.0f,
						Imgproc.THRESH_BINARY);
				contour = ColorTrackingUtil.getBiggestContour(res);

				backprojClone.release();
			} while (contour == null
					&& currThreshold < ColorTracking.BACKPROJ_THRESH_MAX);

			if (currThreshold < ColorTracking.BACKPROJ_THRESH_MAX)
				track.setThreshold(currThreshold);
			else
				return null;
		}

		Imgproc.threshold(backproj, backproj, track.getThreshold(), 255.0f,
				Imgproc.THRESH_BINARY);

		imgRGB.release();
		return backproj;
	}

	/**
	 * Searches for all bottom points of each image in the list.
	 * 
	 * @param imgs
	 *            List of image objects.
	 * @return List of bottom points.
	 */
	public List<Point> getBottom(Mat img) {
		List<Point> bottom = new ArrayList<Point>();

		List<MatOfPoint> contour = ColorTrackingUtil.getContours(img);

		if (contour == null)
			return null;

		for (MatOfPoint p : contour) {
			Rect rec = Imgproc.boundingRect(p);
			if (rec.area() > SEGMENT_AREA_MIN)
				bottom.add(new Point(rec.x + rec.width, rec.y + rec.height / 2));
		}

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
		for (TrackedObject t : trackedObjects)
			t.release();

		trackedObjects.clear();
		newTracking = null;
	}

	public boolean getTrackingActive() {
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

	public boolean isCalcHomography() {
		return calcHomography;
	}

	public void setCalcHomography(boolean calcHomography) {
		this.calcHomography = calcHomography;
	}

	public int getTrackedColorCount() {
		return trackedObjects.size();
	}

	public boolean isWaitingForProbMap() {
		return newTracking != null;
	}

	public List<TrackedObject> getTrackedObjects() {
		return trackedObjects;
	}

	public TrackedObject getNewTracking() {
		return newTracking;
	}

	public boolean isHomographyEnabled() {
		return homography != null;
	}
}
