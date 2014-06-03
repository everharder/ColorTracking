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
	private TrackedBall ball;
	private Robot robot;
	
	private ArrayList<Scalar> statusColor = new ArrayList<Scalar>();
	
	private boolean centered = false;
	private boolean initalBallFound = false;
	private boolean ballInView = false;
	private boolean readyToCatch = false;
	private boolean done = false;
	
	private float imageHeight;
	private RobotEnviroment environment;
	
	private boolean ballCatchingEnabled;
	
	private static final int CAMERA_X_OFFSET = -5; // cam is on the left side
	private static final double MIN_DISTANCE = 15; // cm
	private static final int MIDDLE_TOLERANCE = 50;

	public BallCatcher(Robot robot, RobotEnviroment environment, float h) {
		this.robot = robot;
		this.imageHeight = h;
		this.environment = environment;
	}

	public void catchBall( Map<Color, List<TrackedColor>> trackedColors ) {
		if ( environment == null || environment.getHomography() == null)
			return;
		
		ball = RobotEnviroment.findBall( trackedColors );

		if ( ball != null ) {
			ball.calcDistance( environment.getHomography() );
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
			robot.turn( Robot.MOVE_ANGL ); // turn left until we see a ball
		}
	}

	private void alignToBall() {
		ScreenInfo.getInstance().add( "CENTERING...", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );
		
		float imgWidthMin = (imageHeight / 2) - MIDDLE_TOLERANCE + CAMERA_X_OFFSET;
		float imgWidthMax = (imageHeight / 2) + MIDDLE_TOLERANCE + CAMERA_X_OFFSET;
		
		if( ballInView) {
			if(ball.getBallColor().getBottom().x < imgWidthMin ) {
				robot.turn( Robot.MOVE_ANGL );
			} else if (ball.getBallColor().getBottom().x > imgWidthMax ) {
				robot.turn( -Robot.MOVE_ANGL );
			} else {
				centered = true;
				ScreenInfo.getInstance().add( "CENTERED!", ScreenInfo.POS_BOTTOM_RIGHT, 4, ScreenInfo.COLOR_BLUE );
			}
		} else {
			robot.turn( Robot.MOVE_ANGL ); // turn left until we see a ball again
		}
		
	}

	private void moveToBall() {
		if ( ballInView && ball != null) {
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
				robot.move( (int) (ball.getDistance() - MIN_DISTANCE) );
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
	
	public boolean isBallCatchingEnabled() {
		return !done && ballCatchingEnabled;
	}
	
	public void setBallCatchingEnabled(boolean enabled) {
		ballCatchingEnabled = enabled;
	}
}
