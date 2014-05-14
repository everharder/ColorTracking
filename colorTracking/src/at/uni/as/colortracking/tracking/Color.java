package at.uni.as.colortracking.tracking;

import org.opencv.core.Scalar;

public enum Color {
	RED		(new Scalar(255,   0,   0)),
	BLUE	(new Scalar(  0,   0, 255)),
	GREEN	(new Scalar(  0, 255,   0)),
	YELLOW	(new Scalar(255, 255,   0)),
	WHITE	(new Scalar(255, 255, 255));
	
	private final Scalar hsv;
	private final Scalar rgb;
	
	Color(Scalar hsv, Scalar rgb) {
		this.hsv = hsv;
		this.rgb = rgb;
	}
	
	Color(Scalar rgb){
		this.rgb = rgb;
		this.hsv = ColorTrackingUtil.convertRGB2HSV(rgb);
	}
	
	public Scalar hsv() {
		return hsv;
	}
	
	public Scalar rgb() {
		return rgb;
	}
}
