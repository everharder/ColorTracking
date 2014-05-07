package at.uni.as.colortracking.tracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.util.Pair;

public class TrackedObject {
	private static final double DEFAULT_TOLERANCE = 25.0;
	
	private String label = null;
	private int trackCount = 0;
	private List<TrackedColor> colors = new ArrayList<TrackedColor>();
	private Point coords = null;
	private double coherenceTolerance = DEFAULT_TOLERANCE;  
	
	
	public TrackedObject(String label, int trackCount) {
		this.label = label;
		this.trackCount = trackCount;
	}
	
	public void addColorTrack(Mat probMap) {
		if(colors.size() < trackCount)
			colors.add(new TrackedColor(probMap));
	}
	
	public List<Pair<Point,Double>> getCoherentDistances() {
		if(colors.size() == 0)
			return null;
		else if(colors.size() == 1)
			return colors.get(0).getDist();
		else if(colors.size() == 2){
			List<Pair<Point,Double>> dists = new ArrayList<Pair<Point,Double>>();
			
			for(Pair<Point, Double> u : colors.get(0).getDist()) {
				for(Pair<Point, Double> v : colors.get(1).getDist()) {
					if(v.first.x > u.first.x) {
						if(u.second != null && v.second != null) {	
							if(u.second - v.second < coherenceTolerance) {
								dists.add(u);
							}
						} else {
							dists.add(u);
						}
					}
				}
			}
			
			return dists;
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	public Pair<Point, Double> getCoherentDistanceNearest() {
		List<Pair<Point, Double>> dists = getCoherentDistances();
		
		if(dists == null || dists.size() == 0)
			return null;
		
		Pair<Point, Double> min = dists.get(0);
		for(int i = 1; i < dists.size(); i++) {
			if(dists.get(i).second < min.second)
				min = dists.get(i);
		}
		
		return min;
	}
	
	public Point getCoords() {
		if(coords != null)
			return coords;
		
		coords = ColorTrackingUtil.parseCoords(label);
		return coords;
	}
	
	public void setCoords(Point coords) {
		this.coords = coords;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public int getTrackCount() {
		return trackCount;
	}

	public void setTrackCount(int trackCount) {
		this.trackCount = trackCount;
	}
	
	public TrackedColor getTrack(int i) {
		return colors.get(i);
	}
	
	public void addTrack(TrackedColor track) {
		colors.add(track);
	}
	
	public List<TrackedColor> getTracks() {
		return colors;
	}

	public void resetDists() {
		for(TrackedColor t : colors) 
			t.getDist().clear();
	}

	public void release() {
		for(TrackedColor t : colors)
			t.release();
	}
}
