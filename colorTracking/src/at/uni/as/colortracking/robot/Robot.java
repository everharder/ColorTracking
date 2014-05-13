package at.uni.as.colortracking.robot;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import jp.ksksue.driver.serial.FTDriver;

import org.opencv.core.Point;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;

@SuppressLint("UseValueOf")
public class Robot{
	@SuppressWarnings("unused")
	private String TAG = "iRobot";
	
	@SuppressWarnings("unused")
	private static final double CATCH_DIST = 25.0;
	private static final double COORDS_TOLERANCE = 5.0;
	private static final double DEGREE_TOLERANCE = 5.0;
	
	public static final int DEFAULT_VELOCITY = 15;
	public static final int DEFAULT_MOVE_TIME = 250; //ms
	public static final int BEACONNOTFOUND_DELAY = 1000; //ms
	
	private FTDriver com;
	
	private Point position = null;
	private Double degree = null;
	private Queue<Point> targetCoords = new LinkedList<Point>();
	
	private boolean catchObjectFlag = false;
	private boolean moveToCoordFlag = false;

	public Robot() {
	}
	
	public Robot(FTDriver com) {
		this();
		this.com = com;
		connect();
	}
	
	public Robot(FTDriver com, Point position) {
		this(com);
		this.position = position;
	}

	public void connect() {
		if( com.begin( FTDriver.BAUD9600 ) )
			Log.d( "connect", "connected" );
		else
			Log.d( "connect", "not connected" );

	}

	public void disconnect() {
		if(com == null || !isConnected())
			return;
		
		com.end();
	}
	
	public boolean isConnected(){
		return com != null && com.isConnected();
	}

	/**
	 * transfers given bytes via the serial connection.
	 * 
	 * @param data
	 */
	public void comWrite(byte[] data) {
		if (isConnected()) {
			com.write(data);
		}
	}

	/**
	 * reads from the serial buffer. due to buffering, the read command is
	 * issued 3 times at minimum and continuously as long as there are bytes to
	 * read from the buffer. Note that this function does not block, it might
	 * return an empty string if no bytes have been read at all.
	 * 
	 * @return buffer content as string
	 */
	public String comRead() {
		String s = "";
		int i = 0;
		int n = 0;
		while (i < 3 || n > 0) {
			byte[] buffer = new byte[256];
			n = com.read(buffer);
			s += new String(buffer, 0, n);
			i++;
		}
		return s;
	}

	/**
	 * write data to serial interface, wait 100 ms and read answer.
	 * 
	 * @param data
	 *            to write
	 * @return answer from serial interface
	 */
	public String comReadWrite(byte[] data) {
		if( com != null )
			Log.d( "comNull", "com is not null" );
		com.write(data);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// ignore
		}
		return comRead();
	}

	private void setLed(byte red, byte blue) {
		comReadWrite(new byte[] { 'u', red, blue, '\r', '\n' });
	}

	private void setVelocity(byte left, byte right) {
		comReadWrite(new byte[] { 'i', left, right, '\r', '\n' });
	}

	private void setBar(byte value) {
		comReadWrite(new byte[] { 'o', value, '\r', '\n' });
	}

	// move forward
	public void moveForward(){
		comReadWrite(new byte[] { 'w', '\r', '\n' });
	}
	
	// move forward
	public void moveForward(int v, int t){
		setVelocity((byte)v, (byte)v);
		
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
		}
		
		stop();
	}
	
	// move backward
	public void moveBackward(int v, int t){
		setVelocity((byte)-v, (byte)-v);
		
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
		}
	}
		
	// turn left
	public void turnLeft(int v, int t){
		setVelocity((byte)0, (byte)v);
			
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
		}
	}
		
		// turn right
	public void turnRight(int v, int t){
		setVelocity((byte)v, (byte)0);
			
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
		}
	}

	// turn left
	public void turnLeft() {
		comReadWrite(new byte[] { 'a', '\r', '\n' });
	}

	// stop
	public void stop() {
		comReadWrite(new byte[] { 's', '\r', '\n' });
	}

	// turn right
	public void turnRight() {
		comReadWrite(new byte[] { 'd', '\r', '\n' });
	}

	// move backward
	public void moveBackward() {
		// logText(comReadWrite(new byte[] { 'x', '\r', '\n' }));
		setVelocity((byte) -30, (byte) -30);
	}

	// lower bar a few degrees
	public void barLower() {
		comReadWrite(new byte[] { '-', '\r', '\n' });
	}

	// rise bar a few degrees
	public void barRise() {
		comReadWrite(new byte[] { '+', '\r', '\n' });
	}

	// fixed position for bar (low)
	public void barDown() {
		setBar((byte) 0);
	}

	// fixed position for bar (high)
	public void barUp() {
		setBar((byte) 255);
	}

	public void ledOn() {
		// logText(comReadWrite(new byte[] { 'r', '\r', '\n' }));
		setLed((byte) 255, (byte) 128);
	}

	public void ledOff() {
		// logText(comReadWrite(new byte[] { 'e', '\r', '\n' }));
		setLed((byte) 0, (byte) 0);
	}

	public void sensor(View v) {
		comReadWrite(new byte[] { 'q', '\r', '\n' });
	}
	
	public Point getPosition() {
		return position;
	}

	public void setPosition(Point position) {
		this.position = position;
	}
	
	public void move(){
		if(moveToCoordFlag)
			moveToCoords();
		else if(catchObjectFlag)
			catchObject();
	}
	
	private void catchObject(){
		//TODO: implement
	}
	
	private void moveToCoords(){
		if(targetCoords == null || targetCoords.isEmpty()) {
			moveToCoordFlag = false;
			return;
		}
		
		Point target = targetCoords.peek();
		
		if(position == null) {
			turnLeft(Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME);
			
			try {
				Thread.sleep(BEACONNOTFOUND_DELAY);
			} catch (InterruptedException e) {
			}
			
		} else if(Math.abs(position.x - target.x) < COORDS_TOLERANCE && Math.abs(position.y - target.y) < COORDS_TOLERANCE) {
			//robot is at target coords
			
			//remove target coords from queue
			targetCoords.poll();
			success();
		} else {
			if(degree != null) {
				double targetDegree = getTargetDegree(position, target);
				
				if(Math.abs(degree - targetDegree) < DEGREE_TOLERANCE)
					moveForward(DEFAULT_VELOCITY, DEFAULT_MOVE_TIME);
				else if(degree < targetDegree) 
					turnLeft(DEFAULT_VELOCITY, DEFAULT_MOVE_TIME);
				else
					turnRight(DEFAULT_VELOCITY, DEFAULT_MOVE_TIME);
			} else {
				Log.d("ROBOT", "no degree data for movement");
			}
		}
	}
	
	private double getTargetDegree(Point robot, Point target) {
		double deltaX = Math.abs(robot.x - target.x);
		double deltaY = Math.abs(robot.x - target.x);
		
		//prevent division by zero
		if(deltaX == 0.0)
			deltaX = 1.0;
		
		double degree = Math.atan(deltaY / deltaX);
		if(robot.x <  target.x && robot.y <  target.y)
			return degree;
		if(robot.x >= target.x && robot.y <  target.y)
			return 180.0 - degree;
		if(robot.x >= target.x && robot.y >= target.y)
			return 270.0 - degree;
		if(robot.x <  target.x && robot.y >= target.y)
			return 360.0 - degree;
		
		return 0.0;
	}

	public static Command getRandomCommand() {
		return getRandomCommand(Arrays.asList(Command.values()));
	}
	
	public static Command getRandomCommand(List<Command> commands) {
		Random r = new Random();
		int randomNumber = r.nextInt(commands.size());
		
		return commands.get(randomNumber);
	}

	public void doCommand(Command c) {
		switch(c) {
			case FORWARD: moveForward();break;
			case BACKWARD: moveBackward();break;
			case LEFT: turnLeft();break;
			case RIGHT: turnRight();break;
		}
	}
	
	public void doCommand(Command c, int v, int t) {
		switch(c) {
			case FORWARD: moveForward(v,t);break;
			case BACKWARD: moveBackward(v,t);break;
			case LEFT: turnLeft(v,t);break;
			case RIGHT: turnRight(v,t);break;
		}
	}

	public void undoCommand(Command c) {
		switch(c) {
			case FORWARD: moveBackward();break;
			case BACKWARD: moveForward();break;
			case LEFT: turnRight();break;
			case RIGHT: turnLeft();break;
		}
	}
	
	public void undoCommand(Command c, int v, int t) {
		switch(c) {
			case FORWARD: moveBackward(v,t);break;
			case BACKWARD: moveForward(v,t);break;
			case LEFT: turnRight(v,t);break;
			case RIGHT: turnLeft(v,t);break;
		}
	}
	
	public boolean isCatchObjectEnabled() {
		return catchObjectFlag;
	}

	public void setCatchObjectEnabled(boolean enabled) {
		this.catchObjectFlag = enabled;
	}
	
	public boolean isMoveToCoordsEnabled() {
		return moveToCoordFlag;
	}
	
	public void setMoveToCoordsEnabled(boolean enabled) {
		this.moveToCoordFlag = enabled;
	}
	
	public void setTargetCoords(List<Point> coords) {
		if(coords == null || coords.size() == 0)
			return;
		
		targetCoords.clear();
		targetCoords.addAll(coords);
		moveToCoordFlag = true;
		catchObjectFlag = false;	
	}

	private void success() {
		barDown();
		ledOn();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		
		barUp();
		ledOff();
	}
	
 	public enum Command {
		FORWARD,
		BACKWARD,
		LEFT,
		RIGHT
	}
}