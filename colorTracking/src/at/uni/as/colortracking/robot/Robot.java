package at.uni.as.colortracking.robot;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jp.ksksue.driver.serial.FTDriver;

import org.opencv.core.Point;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Pair;

@SuppressLint("UseValueOf")
public class Robot {
	@SuppressWarnings("unused")
	private String TAG = "iRobot";

	public static final double CATCH_DIST = 25.0;
	public static final double COORDS_TOLERANCE = 10.0;
	public static final double ANGLE_TOLERANCE = 5.0;
	public static final double FIELD_OF_VIEW = 70.0;
	
	public static int MOVE_DIST =   	10; //cm
	public static long MOVE_TIME =  	  2000; //ms
	public static int MOVE_ANGL =   	 5; //TODO: measure estimated val.
	public static int VELOCITY_MIN =	20; //cm per second
	public static final int BEACONNOTFOUND_DELAY = 500; //ms
	
	private FTDriver com;
	private Point position = null;
	private Double angle = null; 

	public Robot(FTDriver com) {
		this.com = com;
		connect();
	}

	public void connect() {
		if ( com.begin( FTDriver.BAUD9600 ) )
			Log.d( "connect", "connected" );
		else
			Log.d( "connect", "not connected" );

	}

	public void disconnect() {
		if ( com == null || !isConnected() ) return;

		com.end();
	}

	public boolean isConnected() {
		return com != null && com.isConnected();
	}

	private String comRead() {
		if ( !isConnected() ) return "NOTCONNECTED";

		String s = "";
		int i = 0;
		int n = 0;
		while ( i < 3 || n > 0 ) {
			byte[] buffer = new byte[256];
			n = com.read( buffer );
			s += new String( buffer, 0, n );
			i++;
		}
		return s;
	}

	private String comReadWrite( byte[] data ) {
		if ( !isConnected() ) return "NOTCONNECTED";

		if ( com != null ) Log.d( "comNull", "com is not null" );
		com.write( data );
		try {
			Thread.sleep( 100 );
		} catch ( InterruptedException e ) {
			// ignore
		}
		return comRead();
	}

	private void setLed( byte red, byte blue ) {
		comReadWrite( new byte[] { 'u', red, blue, '\r', '\n' } );
	}

	private void setVelocity( byte left, byte right ) {
		comReadWrite( new byte[] { 'i', left, right, '\r', '\n' } );
	}

	private void setBar( byte value ) {
		comReadWrite( new byte[] { 'o', value, '\r', '\n' } );
	}

	public void moveForward( int s ) {
		if(angle != null)
			updatePosition(s, angle);
		
		s *= Command.FORWARD.cal();
		Pair<Integer, Long> moveParams = calcVelocityMoveTime(s);
		setVelocity( (byte) ((int)moveParams.first), (byte) ((int)moveParams.first));

		try {
			Thread.sleep(moveParams.second);
		} catch ( InterruptedException e ) {
		}
		stop();
	}
	
	// move backward
	public void moveBackward( int s ) {
		if(angle != null)
			updatePosition(s, angle);
		
		s *= Command.BACKWARD.cal();
		Pair<Integer, Long> moveParams = calcVelocityMoveTime(s);
		setVelocity( (byte) ((int) -moveParams.first), (byte) ((int) -moveParams.first));

		try {
			Thread.sleep(moveParams.second);
		} catch ( InterruptedException e ) {
		}
		stop();			
	}

	// turn left
	public void turnLeft( int angle ) {
		if(this.angle != null)
			this.angle += angle;
		
		angle *= Command.LEFT.cal();
		Pair<Integer, Long> moveParams = calcVelocityMoveTime((int) angle);
		setVelocity( (byte) 0, (byte) ((int)moveParams.first) );

		try {
			Thread.sleep( moveParams.second );
		} catch ( InterruptedException e ) {
		}
		stop();
	}

	// turn right
	public void turnRight( int angle ) {
		if(this.angle != null)
			this.angle -= angle;
		
		angle *= Command.RIGHT.cal();
		Pair<Integer, Long> moveParams = calcVelocityMoveTime((int) angle);
		setVelocity((byte) ((int)moveParams.first), (byte) 0);

		try {
			Thread.sleep( moveParams.second );
		} catch ( InterruptedException e ) {
		}
		stop();
	}

	// stop
	public void stop() {
		comReadWrite( new byte[] { 's', '\r', '\n' } );
	}

	// fixed position for bar (low)
	public void barDown() {
		setBar( (byte) 0 );
	}

	// fixed position for bar (high)
	public void barUp() {
		setBar( (byte) 255 );
	}

	public void ledOn() {
		// logText(comReadWrite(new byte[] { 'r', '\r', '\n' }));
		setLed( (byte) 255, (byte) 128 );
	}

	public void ledOff() {
		// logText(comReadWrite(new byte[] { 'e', '\r', '\n' }));
		setLed( (byte) 0, (byte) 0 );
	}

	public Point getPosition() {
		return position;
	}

	public void setPosition( Point position ) {
		this.position = position;
	}
	
	public Double getAngle() {
		return this.angle;
	}
	
	public void setAngle(Double angle) {
		this.angle = angle;
	}
	

	private void updatePosition(int s, double a) {
		a = a / 360.0 * 2 * Math.PI;
		double dX = Math.cos(a) * s;
		double dY = Math.sin(a) * s;
		
		this.position.x += dX;
		this.position.y += dY;
	}
	
	private Pair<Integer, Long> calcVelocityMoveTime(int s) {
		int v = (int) (s / (MOVE_TIME / 1000));
		
		if(v > VELOCITY_MIN) {
			if(s < 0)
				return new Pair<Integer, Long>(-v, MOVE_TIME);
			else
				return new Pair<Integer, Long>(v, MOVE_TIME);
		} else {
			long t = Math.abs((s * 1000) / VELOCITY_MIN);
			if(s < 0) 
				return new Pair<Integer, Long>(-VELOCITY_MIN, t);
			else
				return new Pair<Integer, Long>(VELOCITY_MIN, t);
		}
	}

	public static Command getRandomCommand() {
		return getRandomCommand( Arrays.asList( Command.values() ) );
	}

	public static Command getRandomCommand( List<Command> commands ) {
		Random r = new Random();
		int randomNumber = r.nextInt( commands.size() );

		return commands.get( randomNumber );
	}
	
	public void doCommand( Command c, int s) {
		switch ( c ) {
			case FORWARD:
				moveForward( s);
				break;
			case BACKWARD:
				moveBackward( s );
				break;
			case LEFT:
				turnLeft( s );
				break;
			case RIGHT:
				turnRight( s );
				break;
		}
	}

	public void success() {
		barDown();
		ledOn();
		try {
			Thread.sleep( 1000 );
		} catch ( InterruptedException e ) {
		}

		barUp();
		ledOff();
	}

	public enum Command {
		FORWARD	(1.0), 
		BACKWARD(1.0), 
		LEFT	(1.0), 
		RIGHT	(1.0);
		
		private double calibrationFactor;
		
		Command(double calibrationFactor) {
			this.calibrationFactor = calibrationFactor;
		}
		
		public double cal() {
			return calibrationFactor;
		}
		
		public void setCal(double calibrationFactor) {
			this.calibrationFactor = calibrationFactor;
		}
	}
}