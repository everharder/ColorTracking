package at.uni.as.colortracking;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.opencv.core.Point;

import at.uni.as.colortracking.robot.Robot;

public class CoordsMover {
	private Robot robot = null;
	private Queue<Point> targetCoords = new LinkedList<Point>();
	private boolean moveToCoordsEnabled = false;
	
	public CoordsMover(Robot robot) {
		this.robot = robot;
	}

	public void moveToCoords() {
		if ( targetCoords == null || targetCoords.isEmpty() ) {
			return;
		}

		Point target = targetCoords.peek();

		if ( robot.getPosition() == null || robot.getAngle() == null) {
			robot.turn( Robot.MOVE_ANGL );

		} else if ( Math.abs( robot.getPosition().x - target.x ) < Robot.COORDS_TOLERANCE && Math.abs( robot.getPosition().y - target.y ) < Robot.COORDS_TOLERANCE ) {
			// robot is at target coords
			targetCoords.poll();
			robot.success();
		} else if(robot.isAtHome()) {
			//calulate angle and dist to target
			double deltaX = Math.abs(robot.getPosition().x - target.x);
			double deltaY = Math.abs(robot.getPosition().y - target.y);
			double angle = ((Math.atan(deltaY / deltaX) + (Math.PI / 2))) / Math.PI * 360.0;
			double disto = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

			if(Math.abs(angle - robot.getAngle()) < Robot.ANGLE_TOLERANCE) {
				robot.move((int) disto);
			} else {
				robot.turn((int) -robot.getAngle());
				robot.turn((int) angle);
			}
		} else {
			robot.turn((int) -robot.getAngle());
			robot.move((int) -(robot.getPosition().x - Robot.home.x));
			robot.turn((int) 90.0);
			robot.move((int) -(robot.getPosition().y - Robot.home.y));
		}
	}
	
	public void setTargetCoords( List<Point> coords ) {
		if ( coords == null || coords.size() == 0 ) 
			return;

		targetCoords.clear();
		targetCoords.addAll(coords);
		moveToCoordsEnabled = true;
	}
	
	public void setMoveToCoordsEnabled(boolean enabled) {
		this.moveToCoordsEnabled = enabled;
	}
	
	public boolean isMoveToCoordsEnabled() {
		return moveToCoordsEnabled && !targetCoords.isEmpty();
	}
}
