package at.uni.as.colortracking.tracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.util.Pair;

public class TrackedColor {
	private double threshold = -1.0;
	private List<Pair<Point, Double>> dist = new ArrayList<Pair<Point,Double>>();
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
	
	public List<Pair<Point, Double>> getDist() {
		return dist;
	}
	
	public void addDist(Point bottom, Double dist) {
		this.dist.add(new Pair<Point,Double>(bottom, dist));
	}
	
	public void addBottom(Point bottom) {
		this.dist.add(new Pair<Point,Double>(bottom, null));
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

	public void addBottoms(List<Point> bottom) {
		for(Point p : bottom) {
			dist.add(new Pair<Point, Double>(p, null));
		}
	}
}
