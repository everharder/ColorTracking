package at.uni.as.colortracking.util;

import java.util.Stack;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.util.Pair;
import at.uni.as.colortracking.robot.Robot;
import at.uni.as.colortracking.robot.Robot.Command;
import at.uni.as.colortracking.robot.RobotEnviroment;
import at.uni.as.colortracking.tracking.Color;
import at.uni.as.colortracking.tracking.ColorTrackingUtil;
import at.uni.as.colortracking.tracking.TrackedColor;

public class CalibrationHelper  implements CvCameraViewListener2{
	private static final Color COLOR_REF = Color.GREEN;
	private static final int MAX_ERR = 5;
	
	private Point screenRef;
	
	private Robot robot = null;
	private RobotEnviroment enviroment = null;
	private CameraBridgeViewBase camera = null;
	private CvCameraViewListener2 currentListener = null;
	
	private int errors = 0;
	private boolean commandDone = false;
	private boolean calibrated = false;
	
	private Double disto = null;
	private Double distoOld = null;
	private Double angle = null;
	private Double angleOld = null;

	
	private Stack<Pair<Robot.Command, Integer>> calibrationStack = new Stack<Pair<Command,Integer>>();
	
	public CalibrationHelper(Robot robot, RobotEnviroment enviroment, CameraBridgeViewBase camera, CvCameraViewListener2 currentListener) {
			this.robot = robot;
			this.enviroment = enviroment;
			this.camera = camera;
			this.currentListener = currentListener;
	}
	
	public void calibrate() {
		if(enviroment.getHomography() == null)
			return;
		
		errors = 0;
		
		calibrationStack.add(new Pair<Robot.Command, Integer>(Command.FORWARD,  Robot.MOVE_DIST));
		calibrationStack.add(new Pair<Robot.Command, Integer>(Command.BACKWARD,-Robot.MOVE_DIST));
		calibrationStack.add(new Pair<Robot.Command, Integer>(Command.LEFT,     Robot.MOVE_ANGL));
		calibrationStack.add(new Pair<Robot.Command, Integer>(Command.RIGHT,    Robot.MOVE_ANGL));
		
		camera.setCvCameraViewListener(this);
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		if(calibrationStack.isEmpty()) {
			calibrated = true;
			finish();
		} else if(isCalDist(calibrationStack.peek().first)) {
			
			TrackedColor color = TrackedColor.getBiggest(ColorTrackingUtil.detectColor(inputFrame.rgba(), COLOR_REF, COLOR_REF.tol()));
			if(color == null || color.getDistance() < 0) {
				errors++;
				if(errors > MAX_ERR)
					finish();
				return inputFrame.rgba();
			}
			
			color.calcDistance(enviroment.getHomography());
			
			if(commandDone && distoOld != null) {
				disto = color.getDistance();
				
				calibrationStack.peek().first.setCal(((double)calibrationStack.peek().second) / Math.abs((double) (distoOld - disto)));
				
				calibrationStack.pop();
				commandDone = false;
				disto = null;
				distoOld = null;
			} else {
				distoOld = color.getDistance();
				robot.doCommand(calibrationStack.peek().first, calibrationStack.peek().second);
				commandDone = true;
			}
		} else if(isCalAngle(calibrationStack.peek().first)) {
			TrackedColor color = TrackedColor.getBiggest(ColorTrackingUtil.detectColor(inputFrame.rgba(), COLOR_REF, COLOR_REF.tol()));
			
			if(color == null) {
				errors++;
				if(errors > MAX_ERR)
					finish();
				return inputFrame.rgba();
			}
			
			if(commandDone && angleOld != null) {
				angle = color.getAngle(screenRef);
				
				calibrationStack.peek().first.setCal(((double)calibrationStack.peek().second) / Math.abs((double) (angleOld - angle)));
				
				calibrationStack.pop();
				commandDone = false;
				angle = null;
				angleOld = null;
			} else {
				angleOld = color.getAngle(screenRef);
				robot.doCommand(calibrationStack.peek().first, calibrationStack.peek().second);
				commandDone = true;
			}
		}
		
		return inputFrame.rgba();
	}
	
	private boolean isCalDist(Command c) {
		return c == Command.BACKWARD || c == Command.BACKWARD;
	}
	
	private boolean isCalAngle(Command c) {
		return c == Command.LEFT || c == Command.RIGHT;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		screenRef = new Point(width, height);
	}

	@Override
	public void onCameraViewStopped() {
	}
	
	private void finish() {
		camera.setCvCameraViewListener(currentListener);
	}
	
	public boolean isCalibrationDone() {
		return calibrated;
	}
}
