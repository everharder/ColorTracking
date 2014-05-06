package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

import android.util.Pair;

public class RobotEnviroment {
	private static final double MIN_X = 0;
	private static final double MAX_X = 150;

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
			// Get distances to beacons.
			Pair<Point, Double> d0 = beacons.get(0)
					.getCoherentDistanceNearest();
			Pair<Point, Double> d1 = beacons.get(1)
					.getCoherentDistanceNearest();
			// Get coordinates of beacons.
			Point p0 = beacons.get(0).getCoords();
			Point p1 = beacons.get(1).getCoords();

			if (d0 == null || d1 == null || p0 == null || p1 == null)
				return null;

			// Determine intersection of two circles.
			List<Point> intersects = CircleCut.circleIntersect(d0.second,
					d1.second, p0, p1);
			// Determine middle point of intersection.
			Point m = new Point(Math.abs(intersects.get(0).x
					- intersects.get(1).x), Math.abs(intersects.get(0).y
					- intersects.get(1).y));

			// Case LEFT or RIGHT
			if ((p0.x == MIN_X && p1.x == MIN_X)
					|| (p0.x == MAX_X && p1.x == MAX_X)) {
				// Determine distance between beacon and Point m.
				double l = Math.sqrt(Math.pow(m.x, 2)
						+ Math.pow(Math.abs(m.y - p0.y), 2));
				// Determine distance between robot and Point m.
				double lr = Math.sqrt(Math.pow(d0.second, 2) - Math.pow(l, 2));

				robot = new Point(Math.abs(m.x - lr), m.y);
			}
			// Case UP or DOWN
			else {
				// Determine distance between beacon and Point m.
				double l = Math.sqrt(Math.pow(Math.abs(m.x - p0.x), 2)
						+ Math.pow(m.y, 2));
				// Determine distance between robot and Point m.
				double lr = Math.sqrt(Math.pow(d0.second, 2) - Math.pow(l, 2));

				robot = new Point(m.x, Math.abs(m.y - lr));
			}
		}

		return robot;
	}
}
