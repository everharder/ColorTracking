package at.uni.as.colortracking.tracking;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

public class TrackedColor {
	private Rect borders;
	private Point bottom;
	private Color color;
	private double distance = -1;
	
	public TrackedColor(Color color, Rect borders) {
		this.color = color;
		this.borders = borders;
		
		if(borders != null)
			bottom = new Point(borders.x + borders.width, borders.y + borders.height / 2);
	}
	
	public Color getColor() {
		return color;
	}
	
	public Rect getBorders() {
		return borders;
	}
	
	public Point getBottom() {
		return bottom;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public void calcDistance(Mat homography) {
		Mat src = new Mat(1, 1, CvType.CV_32FC2);
		Mat dst = new Mat(1, 1, CvType.CV_32FC2);

		src.put(0, 0, new double[] { getBottom().x, getBottom().y });

		// Multiply homography matrix with bottom point.
		Core.perspectiveTransform(src, dst, homography);
		// Real world point.
		Point dest = new Point(dst.get(0, 0)[1], dst.get(0, 0)[0]);
		// Calc distance with scalar product.
		distance = Math.sqrt(Math.pow(dest.x, 2) + Math.pow(dest.y, 2));

		src.release();
		dst.release();
	}
}
