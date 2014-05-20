package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Scalar;

import at.uni.as.colortracking.robot.Robot;
import at.uni.as.colortracking.robot.RobotEnviroment;
import at.uni.as.colortracking.tracking.Color;
import at.uni.as.colortracking.tracking.TrackedBall;
import at.uni.as.colortracking.tracking.TrackedColor;

public class BallCatcher {
	TrackedBall ball;
	Robot robot;
	ArrayList<String> statusMessage = new ArrayList<String>();
	ArrayList<Scalar> statusColor = new ArrayList<Scalar>();
	boolean centered = false;
	boolean initalBallFound = false;
	boolean ballInView = false;
	boolean readyToCatch = false;
	boolean done = false;
	int left;
	int right;
	float imageWidth;
	float imageHeight;
	
	private static final int MOVE_TIME = 400;
	private static final int MOVE_STEP_SIZE = 15;
	private static final int MOVE_TIME_CENTERING = 100;
	private static final int CAMERA_X_OFFSET = -5; // cam is on the left side
	private static final double MIN_DISTANCE = 15; // cm
	private static final int MIDDLE_TOLERANCE = 50;

	public BallCatcher(Robot robot, float cAMERA_W, float cAMERA_H) {
		this.robot = robot;
		this.imageHeight = cAMERA_H;
		this.imageWidth = cAMERA_W;
	}

	public void catchBall( RobotEnviroment environment, Map<Color, List<TrackedColor>> trackedColors ) {
		ball = RobotEnviroment.findBall( trackedColors );

		if ( ball != null ) {
			if ( environment.getHomography() != null ) ball.calcDistance( environment.getHomography() );
			ScreenInfo.getInstance().add( "BALL IN VIEW", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_GREEN );
			initalBallFound = true;
			ballInView = true;
		} else {
			statusColor.add( new Scalar( 255.0, 0.0, 0.0 ) );
			ScreenInfo.getInstance().add( "NO BALL", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_RED );
			ballInView = false;
		}

		if ( initalBallFound ) {
			if ( !centered && !done ) {
				// if the ball was found, first move left, until ball is out of view, then move right until ball is out of view, half way back and the robot looks directly at the ball
				alignToBall();
			} else if ( !readyToCatch && !done ) {
				moveToBall();
			} else if ( readyToCatch && !done ) {
				grabBall();
			}
		} else {
			robot.turnLeft( MOVE_STEP_SIZE, MOVE_TIME ); // turn left until we see a ball
		}
	}

	private void alignToBall() {
		ScreenInfo.getInstance().add( "CENTERING...", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );
		
		
		float imgWidthMin = (imageHeight / 2) - MIDDLE_TOLERANCE + CAMERA_X_OFFSET;
		float imgWidthMax = (imageHeight / 2) + MIDDLE_TOLERANCE + CAMERA_X_OFFSET;
		
		if( ballInView) {
			if(ball.getBallColor().getBottom().x < imgWidthMin ) {
				robot.turnLeft( MOVE_STEP_SIZE, MOVE_TIME_CENTERING );
			} else if (ball.getBallColor().getBottom().x > imgWidthMax ) {
				robot.turnRight( MOVE_STEP_SIZE, MOVE_TIME_CENTERING );
			} else {
				centered = true;
				ScreenInfo.getInstance().add( "CENTERED!", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );
			}
		} else {
			robot.turnLeft( MOVE_STEP_SIZE, MOVE_TIME ); // turn left until we see a ball again
		}
		
	}

	private void moveToBall() {
		if ( ballInView ) {
			double centerOffset = ((imageHeight / 2) + CAMERA_X_OFFSET) - ball.getBallColor().getBottom().x;
			if(Math.abs(centerOffset) > MIDDLE_TOLERANCE) {
				centered = false;
				readyToCatch = false;
				return;
			}
			
			ScreenInfo.getInstance().add( "MOVING...", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );

			if ( ball.getDistance() < MIN_DISTANCE ) {
				// TODO: check sensors
				ScreenInfo.getInstance().add( "READY TO CATCH...", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );
				readyToCatch = true;
			} else {
				robot.moveForward( MOVE_STEP_SIZE, MOVE_TIME );
			}
		} else {
			ScreenInfo.getInstance().add( "BALL LOST...", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_RED );
			centered = false;
			readyToCatch = false;
		}
	}

	private void grabBall() {
		done = true;
		robot.barDown();
		robot.ledOn();
		try {
			Thread.sleep( 1000 );
		} catch ( InterruptedException e ) {
		}
		robot.ledOff();
	}

	public boolean isDone() {
		return done;
	}
}
