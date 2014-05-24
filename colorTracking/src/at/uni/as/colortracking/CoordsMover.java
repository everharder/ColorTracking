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
			robot.turnLeft( Robot.MOVE_ANGL );

		} else if ( Math.abs( robot.getPosition().x - target.x ) < Robot.COORDS_TOLERANCE && Math.abs( robot.getPosition().y - target.y ) < Robot.COORDS_TOLERANCE ) {
			// robot is at target coords
			targetCoords.poll();
			robot.success();
		} else {
			//calulate angle and dist to target
			double deltaX = target.x - robot.getPosition().x;
			double deltaY = target.y - robot.getPosition().y;
			double angle = (Math.atan2(deltaY, deltaX) + Math.PI) / (2 * Math.PI) * 360.0;
			double disto = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

			if(robot.getAngle() - Robot.ANGLE_TOLERANCE > angle)
				robot.turnRight(angle);
			else if(robot.getAngle() + Robot.ANGLE_TOLERANCE < angle)
				robot.turnLeft(angle);
			else
				robot.moveForward((int) disto);
		}
	}
	
	public void setTargetCoords( List<Point> coords ) {
		if ( coords == null || coords.size() == 0 ) return;

		targetCoords.clear();
		targetCoords.addAll(coords);
	}
	
	public void setMoveToCoordsEnabled(boolean enabled) {
		this.moveToCoordsEnabled = enabled;
	}
	
	public boolean isMoveToCoordsEnabled() {
		return moveToCoordsEnabled && !targetCoords.isEmpty();
	}
}
