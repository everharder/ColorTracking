package at.uni.as.colortracking.util;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.util.Pair;
import at.uni.as.colortracking.robot.Robot;
import at.uni.as.colortracking.robot.Robot.Command;
import at.uni.as.colortracking.robot.RobotEnviroment;
import at.uni.as.colortracking.tracking.Color;
import at.uni.as.colortracking.tracking.ColorTrackingUtil;
import at.uni.as.colortracking.tracking.TrackedColor;

public class RobotCalibrator{
	private static final Color COLOR_REF = Color.GREEN;
	private static final int MAX_ERR = 5;
	
	private Point screenRef;
	
	private Robot robot = null;
	private Mat homography = null;
	
	private int errorCount = 0;
	private boolean commandDone = false;
	private boolean calibrationRunning = false;
	private boolean calibrationFailed = false;
	
	private Double previousValue = null;

	
	private Stack<Pair<Robot.Command, Integer>> calibrationStack = new Stack<Pair<Command,Integer>>();
	
	public RobotCalibrator(Robot robot, RobotEnviroment enviroment, Point screenRef) {
			this.robot = robot;
			this.homography = enviroment.getHomography();
			
			errorCount = 0;
			
			//init calibrationstack: command - expected change
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.FORWARD,  Robot.MOVE_DIST));
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.BACKWARD, Robot.MOVE_DIST));
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.LEFT,     Robot.MOVE_ANGL));
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.RIGHT,    Robot.MOVE_ANGL));
			
			Robot.Command.BACKWARD.setCal(1.0);
			Robot.Command.FORWARD.setCal(1.0);
			Robot.Command.LEFT.setCal(1.0);
			Robot.Command.RIGHT.setCal(1.0);
			
			this.screenRef = screenRef;
			calibrationRunning = true;
	}

	public void calibrate(Mat image) {
		if(calibrationStack.isEmpty()) {
			calibrationRunning = false;
			return;
		} 
		
		Map<Color, List<TrackedColor>> trackedColors = ColorTrackingUtil.detectColors(image);		
		if(trackedColors.get(COLOR_REF) == null || trackedColors.get(COLOR_REF).size() == 0 || homography == null) {
			errorCount++;
			return;
		}
		TrackedColor referenceObject = ColorTrackingUtil.getBiggestContour(trackedColors.get(COLOR_REF));
		
		if(errorCount > MAX_ERR) {
			calibrationRunning = false;
			calibrationFailed = true;
			
			return;
		}
		
		if(isCalDist(calibrationStack.peek().first)) {	
			referenceObject.calcDistance(homography);
			
			if(commandDone && previousValue != null && referenceObject.getDistance() > 0) {
				double cal = ((double)calibrationStack.peek().second) / Math.abs((double) (previousValue - referenceObject.getDistance()));
				robot.doCommand(calibrationStack.peek().first, -calibrationStack.peek().second);
				
				commandDone = false;
				previousValue = null;
				calibrationStack.peek().first.setCal(cal);
				calibrationStack.pop();
			} else {
				previousValue = referenceObject.getDistance();
				robot.doCommand(calibrationStack.peek().first, calibrationStack.peek().second);
				commandDone = true;
			}
		} else if(isCalAngle(calibrationStack.peek().first)) {
			if(commandDone && previousValue != null) {
				double cal = ((double)calibrationStack.peek().second) / Math.abs((double) (previousValue - referenceObject.getAngle(screenRef)));
				robot.doCommand(calibrationStack.peek().first, -calibrationStack.peek().second);
				
				commandDone = false;
				previousValue = null;
				calibrationStack.peek().first.setCal(cal);
				calibrationStack.pop();
			} else {
				previousValue = referenceObject.getAngle(screenRef);
				robot.doCommand(calibrationStack.peek().first, calibrationStack.peek().second);
				commandDone = true;
			}
		}
	}
	
	private boolean isCalDist(Command c) {
		return c == Command.FORWARD || c == Command.BACKWARD;
	}
	
	private boolean isCalAngle(Command c) {
		return c == Command.LEFT || c == Command.RIGHT;
	}
	
	public boolean isCalibrationRunning() {
		return calibrationRunning;
	}
	
	public boolean isCalibrationFailed() {
		return calibrationFailed;
	}
}
