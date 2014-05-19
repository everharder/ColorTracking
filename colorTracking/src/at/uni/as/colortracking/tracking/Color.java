package at.uni.as.colortracking.tracking;

import org.opencv.core.Scalar;

public enum Color {
	GREEN   (new Scalar(0, 120, 70), new Scalar(10, 60, 60)),

	private Scalar hsv;
	private Scalar rgb;
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
	
	public void setRGB(Scalar rgb) {
		this.rgb = rgb;
		this.hsv = ColorTrackingUtil.convertRGB2HSV(rgb);
	}
}
