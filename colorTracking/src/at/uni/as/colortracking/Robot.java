package at.uni.as.colortracking;

import java.util.Random;
import java.util.Stack;

import jp.ksksue.driver.serial.FTDriver;

import org.opencv.core.Point;

import android.annotation.SuppressLint;
import android.view.View;

@SuppressLint("UseValueOf")
public class Robot{
	@SuppressWarnings("unused")
	private String TAG = "iRobot";
	private static Double CATCH_DIST = 25.0;
	private FTDriver com;
	private Point position = null;
	private Stack<Command> history = new Stack<Robot.Command>();
	
	private TrackedObject catchObject = null;
	private Double catchObjectDistCurrent = null;
	private Double catchObjectDistOld = null;
	
	private boolean catchObjectFlag = false;

	public Robot() {
	}
	
	public Robot(FTDriver com) {
		this();
		
		connect(com);
	}
	
	public Robot(FTDriver com, Point position) {
		this(com);
		this.position = position;
	}

	public boolean connect(FTDriver com) {
		if(com == null)
			return false;
		
		if (com.begin(9600)) {
			this.com = com;
			return true;
		}

		return false;
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
		if(obj.getTrack(0).getDist() == null || obj.getTrack(0).getDist().get(0) == null || obj.getTrack(0).getDist().get(0).second == null)
			return;
		
		this.catchObjectDistCurrent = obj.getTrack(0).getDist().get(0).second;
		this.catchObjectDistOld = new Double(obj.getTrack(0).getDist().get(0).second);
		this.catchObject = obj;
		
		//ledOn();
	}
	
	public TrackedObject getCatchObject() {
		return catchObject;
	}
	
	public boolean isInCatchMode() {
		return catchObjectDistOld != null && catchObjectDistCurrent != null && catchObject != null;
	}
	
	public void catchObject() {
		//if(!isInCatchMode() || !isConnected())
		if(!isInCatchMode())
			return;
		if(catchObject.getTrack(0).getDist() == null || catchObject.getTrack(0).getDist().get(0) == null || catchObject.getTrack(0).getDist().get(0).second == null)
			return;
		
		Double catchObjectDistCurrent = catchObject.getTrack(0).getDist().get(0).second;
		
		if(catchObjectDistCurrent == null)
			return; 
		
		if(catchObjectDistCurrent == -1) {
			if(history.isEmpty()) {
				catchObjectDistCurrent = null;
				catchObjectDistOld = null;
			} else {
				undoCommand(history.pop());
			}
		} else {
			if(catchObjectDistCurrent < CATCH_DIST) {
				barDown();
				catchObjectFlag = false;
			} else if(catchObjectDistCurrent <= catchObjectDistOld) {
				Command c = null;
				if(!history.isEmpty())
					c = history.peek();
				else 
					c = getRandomCommand();
				doCommand(c);
				history.push(c);
				
				catchObjectDistOld = new Double(catchObjectDistCurrent.doubleValue());
			} else {
				Command c = null;
				do {
					c = getRandomCommand();
				} while(!history.isEmpty() && c == history.peek());
				
				if(c != null) {
					doCommand(c);
					history.push(c);
				} else {
					catchObjectDistCurrent = null;
					catchObjectDistOld = null;
					catchObject = null;
				}
				
			}
		}
	}
	
	private Command getRandomCommand() {
		Random r = new Random();
		int randomNumber = r.nextInt(Command.values().length - 1);
		
		return Command.values()[randomNumber];
	}

	private void doCommand(Command c) {
		switch(c) {
			case FORWARD: moveForward();break;
			case BACKWARD: moveBackward();break;
			case LEFT: turnLeft();break;
			case RIGHT: turnRight();break;
			default: moveForward();
		}
	}

	private void undoCommand(Command c) {
		switch(c) {
			case FORWARD: moveBackward();break;
			case BACKWARD: moveForward();break;
			case LEFT: turnRight();break;
			case RIGHT: turnLeft();break;
		}
	}
	
	public boolean isCatchObjectEnabled() {
		return catchObjectFlag;
	}

	public void setCatchObjectEnabled(boolean catchObjectFlag) {
		this.catchObjectFlag = catchObjectFlag;
	}


	public enum Command {
		FORWARD,
		BACKWARD,
		LEFT,
		RIGHT
	}
}