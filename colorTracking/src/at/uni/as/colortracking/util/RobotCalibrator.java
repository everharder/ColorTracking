package at.uni.as.colortracking.util;

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
	
	public RobotCalibrator(Robot robot, RobotEnviroment enviroment) {
			this.robot = robot;
			this.homography = enviroment.getHomography();
			
			errorCount = 0;
			
			//init calibrationstack: command - expected change
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.FORWARD,  Robot.MOVE_DIST));
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.BACKWARD,-Robot.MOVE_DIST));
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.LEFT,     Robot.MOVE_ANGL));
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.RIGHT,   -Robot.MOVE_ANGL));
			
			calibrationRunning = true;
	}

	public void calibrate(Mat image) {
		if(calibrationStack.isEmpty()) {
			calibrationRunning = false;
			return;
		} 
		
		TrackedColor referenceObject = TrackedColor.getBiggest(ColorTrackingUtil.detectColor(image, COLOR_REF, COLOR_REF.tol()));
		if(referenceObject == null) {
			errorCount++;
		}
		
		if(errorCount > MAX_ERR || homography == null) {
			calibrationRunning = false;
			calibrationFailed = true;
			
			return;
		}
		
		if(isCalDist(calibrationStack.peek().first)) {	
			referenceObject.calcDistance(homography);
			
			if(commandDone && previousValue != null && referenceObject.getDistance() > 0) {
				calibrationStack.pop().first.setCal(((double)calibrationStack.peek().second) / Math.abs((double) (previousValue - referenceObject.getDistance())));
				
				commandDone = false;
				previousValue = null;
			} else {
				previousValue = referenceObject.getDistance();
				robot.doCommand(calibrationStack.peek().first, calibrationStack.peek().second);
				commandDone = true;
			}
		} else if(isCalAngle(calibrationStack.peek().first)) {
			if(commandDone && previousValue != null) {
				calibrationStack.pop().first.setCal(((double)calibrationStack.peek().second) / Math.abs((double) (previousValue - referenceObject.getAngle(screenRef))));
				
				commandDone = false;
				previousValue = null;
			} else {
				previousValue = referenceObject.getAngle(screenRef);
				robot.doCommand(calibrationStack.peek().first, calibrationStack.peek().second);
				commandDone = true;
			}
		}
	}
	
	private boolean isCalDist(Command c) {
		return c == Command.BACKWARD || c == Command.BACKWARD;
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
