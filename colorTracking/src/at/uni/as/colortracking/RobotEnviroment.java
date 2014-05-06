package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

import android.util.Pair;

public class RobotEnviroment {
	private List<Beacon> beacons = new ArrayList<Beacon>();

	public RobotEnviroment() {
	}

	public void addBeacon(Beacon beacon) {
		beacons.add(beacon);
	}

	public List<Beacon> getBeacons() {
		return this.beacons;
	}

	public Point locate(List<TrackedObject> tracks) {
		Point robot = null;	
		List<TrackedObject> beacons = new ArrayList<TrackedObject>();

		// Get all beacons (objects with 2 tracked colors).
		for (TrackedObject t : tracks)
			if (t.getTrackCount() == 2)
				beacons.add(t);

		if (beacons.size() > 1) {
			List<Pair<Point, Double>> u = beacons.get(0).getCoherentDistances();
			List<Pair<Point, Double>> v = beacons.get(1).getCoherentDistances();
			
			if(u == null || u.size() == 0 || u.get(0) == null || u.get(0).second == null || v == null || v.size() == 0 || v.get(0) == null || v.get(0).second == null)
				return null;
			
			// Determine intersection of two circles.
			List<Point> intersects = CircleCut.circleIntersect(u.get(0).second, v.get(0).second, beacons.get(0).getCoords(),
					beacons.get(1).getCoords());

			System.out.println("Point 0: " + intersects.get(0) + "\t Point 1: "
					+ intersects.get(1));
		}
		
		return robot;
	}
}
