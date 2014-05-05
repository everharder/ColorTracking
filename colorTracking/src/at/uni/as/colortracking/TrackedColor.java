package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class TrackedColor {
	private double threshold = -1.0;
	private List<Double> dist = new ArrayList<Double>();
	private List<Point> bottoms = new ArrayList<Point>();
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

	public List<Point> getBottoms() {
		return bottoms;
	}
	
	public void addBottom(Point bottom) {
		this.bottoms.add(bottom);
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
