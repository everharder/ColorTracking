package at.uni.as.colortracking.robot;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

import android.util.Pair;
import at.uni.as.colortracking.tracking.TrackedObject;

public class RobotEnviroment {
	private static final double MIN_X = 0;
	private static final double MAX_X = 150;

	public RobotEnviroment() {
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
			Point m = new Point(
					(intersects.get(0).x + intersects.get(1).x) / 2,
					(intersects.get(0).y + intersects.get(1).y) / 2);
			// Determine distance between beacon and Point m.
			double lb = Math.sqrt(Math.pow(Math.abs(m.x - p0.x), 2)
					+ Math.pow(Math.abs(m.y - p0.y), 2));
			// Determine distance between robot and Point m.
			double lr = Math.sqrt(Math.pow(d0.second, 2) - Math.pow(lb, 2));

			// Case LEFT or RIGHT
			if ((p0.x == MIN_X && p1.x == MIN_X)
					|| (p0.x == MAX_X && p1.x == MAX_X))
				robot = new Point(Math.abs(m.x - lr), m.y);
			// Case UP or DOWN
			else
				robot = new Point(m.x, Math.abs(m.y - lr));
		}

		return robot;
	}
}
