package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;

public class TrackedColor {
	private String color = null;
	private Mat probMap = null;
	private double threshold = -1;
	private List<Double> dist = new ArrayList<Double>();

	public List<Double> getDist() {
		return dist;
	}

	public void addDist(double dist) {
		this.dist.add(dist);
	}

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
