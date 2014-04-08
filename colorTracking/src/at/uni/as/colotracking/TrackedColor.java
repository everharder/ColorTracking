package at.uni.as.colotracking;

import org.opencv.core.Mat;

public class TrackedColor {
	private String color = null;
	private Mat probMap = null;
	
	public TrackedColor(String color) {
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
}
