package at.uni.as.colortracking;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.opencv.core.Point;

import at.uni.as.colortracking.robot.Robot;

public class CoordsMover {
	private Robot robot = null;
	private Queue<Point> targets = new LinkedList<Point>();
	
	public CoordsMover(Robot robot) {
		this.robot = robot;
	}
	
	public void moveTo(Point target) {
		if ( robot.getPosition() == null || robot.getAngle() == null) 
			return;
		
		//turn to 0°
		robot.turn((int) -robot.getAngle());
		//move to x
		robot.move((int) (target.x - robot.getPosition().x), true);
		//turn to 90°
		robot.turn((int) 90.0);
		//move to y
		robot.move((int) (target.y - robot.getPosition().y), true);
	}
	
	public void moveTo() {
		moveTo(targets.poll());
	}
	
	public void setTarget(List<Point> targets) {
		this.targets.addAll(targets);
	}
	
	public boolean isEmpty() {
		return targets.isEmpty();
	}
}
