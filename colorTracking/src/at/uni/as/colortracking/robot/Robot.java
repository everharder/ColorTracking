package at.uni.as.colortracking.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import jp.ksksue.driver.serial.FTDriver;

import org.opencv.core.Point;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import at.uni.as.colortracking.tracking.TrackedObject;

@SuppressLint("UseValueOf")
public class Robot{
	@SuppressWarnings("unused")
	private String TAG = "iRobot";
	
	private static Double CATCH_DIST = 25.0;
	public static int DEFAULT_VELOCITY = 15;
	public static int DEFAULT_MOVE_TIME = 250; //ms
	
	private FTDriver com;
	private Point position = null;
	private Point lastPos = null;
	private Stack<Command> history = new Stack<Robot.Command>();
	
	private TrackedObject catchObject = null;
	private List<Point> moveToCoords = new ArrayList<Point>();
	
	private Double targetDistCur = null;
	private Double targetDistOld = null;
	
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
	
	@SuppressLint("UseValueOf")
	public void setCatchObject(TrackedObject obj) {
		if(obj == null || obj.getTrackCount() > 1)
			return;
		if(obj.getTrack(0).getDist() == null || obj.getTrack(0).getDist().size() == 0 || obj.getTrack(0).getDist().get(0).second == null)
			return;
		
		this.targetDistCur = obj.getTrack(0).getDist().get(0).second;
		this.targetDistOld = new Double(obj.getTrack(0).getDist().get(0).second);
		this.catchObject = obj;
	}
	
	public TrackedObject getCatchObject() {
		return catchObject;
	}
	
	public boolean isCatchObjectSet() {
		return targetDistOld != null && targetDistCur != null && catchObject != null;
	}
	
	public void catchObject() {
		if(!isCatchObjectEnabled() || !isConnected())
			return;
		
		Pair<Point, Double> dist = catchObject.getCoherentDistanceNearest();
		
		if(dist == null || dist.second == null || dist.second < 0) {
			if(history.isEmpty()) {
				Command c = getRandomCommand();
				doCommand(c, Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME);
				history.push(c);
				
				try {
					Thread.sleep( 1000 );
				} catch ( InterruptedException e ) {
					//ignore
				}
				
				targetDistCur = null;
				targetDistOld = null;
			} else {
				undoCommand(history.pop(), Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME);
				try {
					Thread.sleep( 1000 );
				} catch ( InterruptedException e ) {
					//ignore
				}
			}
		} else {
			targetDistOld = targetDistCur;	
			targetDistCur = dist.second;

			if(targetDistCur < CATCH_DIST) {
				barDown();
				catchObjectFlag = false;
			} else if(targetDistCur <= targetDistOld) {
				Command c = null;
				
				if(!history.isEmpty())
					c = history.peek();
				else 
					c = getRandomCommand();
				
				doCommand(c, Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME);
				history.push(c);
			} else {
				Command c = null;
				do {
					c = getRandomCommand();
				} while(!history.isEmpty() && c == history.peek());
			
				if(c != null) {
					doCommand(c);
					history.push(c);

					try {
						Thread.sleep( 1000 );
					} catch ( InterruptedException e ) {
						//ignore
					}
				} else {
					targetDistCur = null;
					targetDistOld = null;
					catchObject = null;
				}
			}
		}
	}
	
	public void moveToCoords(){
		if(!moveToCoordFlag) 
			return;
		if(moveToCoords == null || moveToCoords.size() == 0) {
			moveToCoordFlag = false;
			return;
		}
		
		Point target = moveToCoords.get(moveToCoords.size() - 1);
		
		if(position == null) {
			turnLeft(Robot.DEFAULT_VELOCITY, Robot.DEFAULT_MOVE_TIME);
		} else if(Math.abs(position.x - target.x) < 5 && Math.abs(position.y - target.y) < 5) {
			moveToCoords.remove(target);
			
			barDown();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			barUp();
		} else {
			Command c = getRandomCommand();
				
			if(Math.abs(position.x - target.x) <= Math.abs(lastPos.x - target.x) && Math.abs(position.y - target.y) <= Math.abs(lastPos.y - target.y) && !history.isEmpty()) {
					c = history.peek();
			} 
				
			doCommand(c, DEFAULT_VELOCITY, DEFAULT_MOVE_TIME);
			history.push(c);
		}
	}
	
	private Command getRandomCommand() {
		Random r = new Random();
		int randomNumber = r.nextInt(Command.values().length - 1);
		
		return Command.values()[randomNumber];
	}

	public void doCommand(Command c) {
		switch(c) {
			case FORWARD: moveForward();break;
			case BACKWARD: moveBackward();break;
			case LEFT: turnLeft();break;
			case RIGHT: turnRight();break;
			default: moveForward();
		}
	}
	
	public void doCommand(Command c, int v, int t) {
		switch(c) {
			case FORWARD: moveForward(v,t);break;
			case BACKWARD: moveBackward(v,t);break;
			case LEFT: turnLeft(v,t);break;
			case RIGHT: turnRight(v,t);break;
			default: moveForward(v,t);
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

	public void setCatchObjectEnabled(boolean catchObjectFlag) {
		this.catchObjectFlag = catchObjectFlag;
	}
	
	public boolean isMoveToCoordsEnabled() {
		return moveToCoordFlag;
	}
	
	public void setMoveToCoordsEnabled(boolean enabled) {
		this.moveToCoordFlag = enabled;
	}
	
	public void setMoveToCoords(List<Point> coords) {
		if(coords == null || coords.size() == 0)
			return;
		
		this.moveToCoords = coords;
		moveToCoordFlag = true;
		targetDistCur = null;
		targetDistOld = null;
		
		if(position != null)
			lastPos = position.clone();
	}


	public enum Command {
		FORWARD,
		BACKWARD,
		LEFT,
		RIGHT
	}
}