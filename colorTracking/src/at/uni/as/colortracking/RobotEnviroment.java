package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

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
			// Determine intersection of two circles.
			List<Point> intersects = CircleCut.circleIntersect(beacons.get(0)
					.getCoherentDistanceNearest().second, beacons.get(1)
					.getCoherentDistanceNearest().second, beacons.get(0).getCoords(),
					beacons.get(1).getCoords());

			System.out.println("Point 0: " + intersects.get(0) + "\t Point 1: "
					+ intersects.get(1));
		}
		
		return robot;
	}
}
