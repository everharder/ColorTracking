package at.uni.as.colortracking.tracking;

import org.opencv.core.Scalar;

public enum Color {
	RED		(new Scalar(255,   0,   0), new Scalar( 10, 100, 100)),
	BLUE	(new Scalar(  0,   0, 255), new Scalar( 10, 120, 120)),
	YELLOW	(new Scalar(255, 255,   0), new Scalar( 10,  50,  50)),
	WHITE	(new Scalar(255, 255, 255), new Scalar(  0,   0,   0));
	
	private final Scalar hsv;
	private final Scalar rgb;
	private final Scalar tol;
	
	Color(Scalar hsv, Scalar rgb, Scalar tol) {
		this.hsv = hsv;
		this.rgb = rgb;
		this.tol = tol;
	}
	
	Color(Scalar rgb, Scalar tol){
		this.rgb = rgb;
		this.hsv = ColorTrackingUtil.convertRGB2HSV(rgb);
		this.tol = tol;
	}
	
	public Scalar hsv() {
		return hsv;
	}
	
	public Scalar rgb() {
		return rgb;
	}
	
	public Scalar tol() {
		return tol;
	}
}
