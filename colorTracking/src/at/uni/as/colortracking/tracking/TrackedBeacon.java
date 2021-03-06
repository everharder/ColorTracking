package at.uni.as.colortracking.tracking;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class TrackedBeacon {
	private TrackedColor upperColor;
	private TrackedColor lowerColor;
	private Beacon beacon;
	
	public TrackedBeacon(Beacon beacon, TrackedColor upperColor, TrackedColor lowerColor) {
		this.beacon = beacon;
		this.upperColor = upperColor;
		this.lowerColor = lowerColor;
	}
	
	public Beacon getBeacon() {
		return beacon;
	}
	
	public TrackedColor getUpperColor() {
		return upperColor;
	}
	public TrackedColor getLowerColor() {
		return lowerColor;
	}
	
	public Point getBottom() {
		return lowerColor.getBottom();
	}
	
	/**
	 * @param screenCenter
	 * @return Angle between screenCenter and tracked beacon in degrees.
	 */
	public Double getAngle(Point screenCenter) {
		return lowerColor.getAngle(screenCenter);
	}
	
	public double getDistance() {
		return lowerColor.getDistance();
	}

	public void calcDistance(Mat homography) {
		lowerColor.calcDistance(homography);
	}
}
