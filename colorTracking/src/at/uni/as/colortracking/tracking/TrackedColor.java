package at.uni.as.colortracking.tracking;

import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import at.uni.as.colortracking.robot.Robot;

public class TrackedColor {
	private Rect borders;
	private Point bottom;
	private Color color;
	private double distance = -1;
	
	public TrackedColor(Color color, Rect borders) {
		this.color = color;
		this.borders = borders;
		
		if(borders != null)
			bottom = new Point(borders.x + borders.width / 2, borders.y + borders.height);
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
	
	public double getAngle(Point ref) {
		return (ref.x - bottom.x) * Robot.FIELD_OF_VIEW / (ref.x * 2.0); 
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
		distance = Math.sqrt(Math.pow(dest.x, 2) + Math.pow(dest.y, 2)) / 10.0;

		src.release();
		dst.release();
	}
	
	public static TrackedColor getBiggest(List<TrackedColor> colors) {
		TrackedColor max_c = null;
		double max_s = -1;
		
		for(TrackedColor c : colors) {
			if(c.getBorders().area() > max_s) {
				max_c = c;
				max_s = c.getBorders().area();
			}
		}
		
		return max_c;
	}
}
