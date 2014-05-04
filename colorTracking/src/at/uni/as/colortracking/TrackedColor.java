package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;

public class TrackedColor {
	private double threshold = -1.0;
	private List<Double> dist = new ArrayList<Double>();
	private Mat probMap = null;
	
	public TrackedColor(Mat probMap) {
		this.probMap = probMap;
	}
	
	public double getThreshold() {
		return threshold;
	}
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	
	public List<Double> getDist() {
		return dist;
	}
	
	public void addDist(Double dist) {
		this.dist.add(dist);
	}
	
	public Mat getProbMap() {
		return probMap;
	}
	
	public void setProbMap(Mat probMap) {
		this.probMap = probMap;
	}
	
	public void release() {
		if(probMap != null)
			probMap.release();
	}
}
