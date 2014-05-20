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
	boolean movedLeft = false;
	boolean movedRight = false;
	boolean centered = false;
	boolean initalBallFound = false;
	boolean ballInView = false;
	boolean readyToCatch = false;
	boolean done = false;
	int left;
	int right;

	private static final int MOVE_TIME = 1;
	private static final int MOVE_STEP_SIZE = 1;
	private static final double MIN_DISTANCE = 200; // mm

	public BallCatcher(Robot robot) {
		this.robot = robot;
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

		if ( !movedLeft && ballInView && !centered ) { // move left as long as we see the ball
			robot.turnLeft( MOVE_STEP_SIZE, MOVE_TIME );
		} else if ( !movedLeft && !ballInView && !centered ) { // moved to far, so stop
			movedLeft = true;
		} else if ( !movedRight && ballInView && !centered ) { // move right as long as we see the ball
			robot.turnRight( MOVE_STEP_SIZE, MOVE_TIME );
			right += MOVE_STEP_SIZE;
		} else if ( !movedRight && !ballInView && !centered ) { // moved to far, so stop
			movedRight = true;
		} else if ( movedRight && movedLeft && !centered ) { // moveleft again, bu only half way
			if ( left < right / 2 ) {
				robot.turnLeft( MOVE_STEP_SIZE, MOVE_TIME );
				left += MOVE_STEP_SIZE;
			} else {
				centered = true;
				ScreenInfo.getInstance().add( "CENTERED", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );
			}
		}
	}

	private void moveToBall() {
		if ( ballInView ) {
			ScreenInfo.getInstance().add( "MOVING...", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );

			if ( ball.getDistance() < MIN_DISTANCE ) {
				ScreenInfo.getInstance().add( "READY TO CATCH...", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );
				readyToCatch = true;
			} else {
				robot.moveForward( MOVE_STEP_SIZE, MOVE_TIME );
			}
		} else {
			ScreenInfo.getInstance().add( "BALL LOST...", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_RED );
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
