package at.uni.as.colortracking;

import org.opencv.core.Mat;

public class TrackedColor {
	private String color = null;
	private Mat probMap = null;
	private double threshold = -1;

	public TrackedColor(String color) {
		this.color = color;
	}
	
	public TrackedColor(String color, Mat probMap) {
		this(color);
		this.probMap = probMap;
	}
	
	public TrackedColor(String color, Mat probMap, double threshold) {
		this(color, probMap);
		this.color = color;
	}
	
	public Mat getProbMap() {
		return probMap;
	}
	
	public void setProbMap(Mat probMap) {
		this.probMap = probMap;
	}
	
	public String getColor() {
		return color;
	}
	
	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
}
