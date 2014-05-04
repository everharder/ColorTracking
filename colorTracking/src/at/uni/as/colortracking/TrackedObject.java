package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class TrackedObject {
	private static final double DEFAULT_TOLERANCE = 15.0;
	
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
	
	public List<Double> getCoherentDistances() {
		if(colors.size() == 0)
			return null;
		else if(colors.size() == 1)
			return colors.get(0).getDist();
		else {
			List<Double> dists = new ArrayList<Double>(colors.get(0).getDist());
			Map<Double, Integer> coherentDistances = new HashMap<Double, Integer>();
			
			for(Double u : dists) {
				for(int i = 1; i < colors.size(); i++) {
					for(Double v : colors.get(i).getDist()) {
						if(Math.abs(u - v) < coherenceTolerance) {
							if(coherentDistances.containsKey(u)) {
								coherentDistances.put(u, coherentDistances.get(u) + 1);
							} else {
								coherentDistances.put(u, 2);
							}
						}
					}
				}
			}
			
			for(Double d : new ArrayList<Double>(coherentDistances.keySet())) {
				if(coherentDistances.get(d) != colors.size())
					coherentDistances.remove(d);
			}
			
			return new ArrayList<Double>(coherentDistances.keySet());
		}
	}
	
	public Double getCoherentDistanceNearest() {
		List<Double> dists = getCoherentDistances();
		
		if(dists == null || dists.size() == 0)
			return null;
		
		Double min = dists.get(0);
		for(int i = 1; i < dists.size(); i++) {
			if(dists.get(i) < min)
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
