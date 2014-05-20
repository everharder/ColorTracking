package at.uni.as.colortracking;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.opencv.core.Point;

import at.uni.as.colortracking.robot.Robot;

public class CoordsMover {
	private Robot robot = null;
	
	private Point positionOld = null;
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

		if ( robot.getPosition() == null ) {
			robot.turnLeft( Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME );

			try {
				Thread.sleep( Robot.BEACONNOTFOUND_DELAY );
			} catch ( InterruptedException e ) {
			}

		} else if ( Math.abs( robot.getPosition().x - target.x ) < Robot.COORDS_TOLERANCE && Math.abs( robot.getPosition().y - target.y ) < Robot.COORDS_TOLERANCE ) {
			// robot is at target coords

			// remove target coords from queue
			targetCoords.poll();
			robot.success();
		} else {
			if ( positionOld == null ) {
				positionOld = robot.getPosition().clone();
			}

			double deltaX = Math.abs( robot.getPosition().x - target.x );
			double deltaY = Math.abs( robot.getPosition().y - target.y );
			double deltaXOld = Math.abs( positionOld.x - target.x );
			double deltaYOld = Math.abs( positionOld.y - target.y );

			if ( deltaX < deltaXOld && deltaY < deltaYOld ) {
				robot.moveForward( (int) (Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2)) * 0.9), 1000 );
			} else {
				robot.moveBackward( Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME );
				robot.turnLeft( Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME );
				robot.moveForward( Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME );
			}

			positionOld = robot.getPosition().clone();
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
