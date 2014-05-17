package at.uni.as.colortracking.tracking;

import org.opencv.core.Point;
import at.uni.as.colortracking.robot.RobotEnviroment;

public enum Beacon {
	BLUE_YELLOW	(Color.BLUE, 	Color.YELLOW, 	new Point(RobotEnviroment.MIN_X, RobotEnviroment.MAX_Y)),
	BLUE_RED	(Color.BLUE, 	Color.RED, 		new Point(RobotEnviroment.HALFWAY_X, RobotEnviroment.MAX_Y)),
	RED_YELLOW	(Color.RED, 	Color.YELLOW, 	new Point(RobotEnviroment.MAX_X, RobotEnviroment.MAX_Y)),
	YELLOW_BLUE	(Color.YELLOW, 	Color.BLUE, 	new Point(RobotEnviroment.MIN_X, RobotEnviroment.MIN_Y)),
	RED_BLUE	(Color.RED, 	Color.BLUE, 	new Point(RobotEnviroment.HALFWAY_X, RobotEnviroment.MIN_Y)),
	YELLOW_RED	(Color.YELLOW, 	Color.RED, 		new Point(RobotEnviroment.MAX_X, RobotEnviroment.MIN_Y)),
	WHITE_BLUE	(Color.WHITE, 	Color.BLUE, 	new Point(RobotEnviroment.MIN_X, RobotEnviroment.HALFWAY_Y)),
	WHITE_RED	(Color.WHITE, 	Color.RED, 		new Point(RobotEnviroment.MAX_X, RobotEnviroment.HALFWAY_Y));
	
	private final Color upperColor;
	private final Color lowerColor;
	private final Point coords;
	
	Beacon(Color upperColor, Color lowerColor, Point coords) {
		this.upperColor = upperColor;
		this.lowerColor = lowerColor;
		this.coords = coords;
	}
	
	public Color upperColor() {
		return upperColor;
	}
	
	public Color lowerColor() {
		return lowerColor;
	}
	
	public Point coords() {
		return coords;
	}
}
