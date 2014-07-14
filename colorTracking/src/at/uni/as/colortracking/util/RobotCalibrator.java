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
	private static final int ITERATIONS = 5;
	
	private Point screenRef;
	
	private Robot robot = null;
	private Mat homography = null;
	
	private int errorCount = 0;
	private int iteraCount = 0;
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
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.MOVE,  Robot.MOVE_DIST));
			calibrationStack.add(new Pair<Robot.Command, Integer>(Command.TURN,  Robot.MOVE_ANGL));
			
			Robot.Command.MOVE.setCal(1.0);
			Robot.Command.TURN.setCal(1.0);
			
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
			
			if(errorCount > MAX_ERR) {
				calibrationRunning = false;
				calibrationFailed = true;
			}
			return;
		}
		
		TrackedColor referenceObject = ColorTrackingUtil.getBiggestContour(trackedColors.get(COLOR_REF));
		referenceObject.calcDistance(homography);
		
		if(!commandDone) {
			previousValue = getReferenceValue(calibrationStack.peek().first, referenceObject);
			robot.doCommand(calibrationStack.peek().first, calibrationStack.peek().second);
			commandDone = true;
		} else if(previousValue != null) {
			Double cal = null;
			if(isCalDist(calibrationStack.peek().first)) 	
				cal = ((double)calibrationStack.peek().second) / Math.abs((double) (previousValue - referenceObject.getDistance()));
			if(isCalAngle(calibrationStack.peek().first)) 
				cal = ((double)calibrationStack.peek().second) / Math.abs((double) (previousValue - referenceObject.getAngle(screenRef)));
				
			robot.doCommand(calibrationStack.peek().first, -calibrationStack.peek().second);
			
			commandDone = false;
			previousValue = null;
			
			if(iteraCount == 0)
				calibrationStack.peek().first.setCal(cal);
			else
				calibrationStack.peek().first.setCal((calibrationStack.peek().first.getCal() + cal) / 2.0);
			
			if(++iteraCount >= ITERATIONS) {
				calibrationStack.pop();
				iteraCount = 0;
			}		
		} else {
			calibrationFailed = true;
			calibrationRunning = false;
		}
	}
	
	private Double getReferenceValue(Command c, TrackedColor referenceObject) {
		if(isCalDist(c))
			return referenceObject.getDistance();
		if(isCalAngle(c))
			return referenceObject.getAngle(screenRef);
		return null;
	}

	private boolean isCalDist(Command c) {
		return c == Command.MOVE;
	}
	
	private boolean isCalAngle(Command c) {
		return c == Command.TURN;
	}
	
	public boolean isCalibrationRunning() {
		return calibrationRunning;
	}
	
	public boolean isCalibrationFailed() {
		return calibrationFailed;
	}
}
