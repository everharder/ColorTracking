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
	public static final double ANGLE_TOLERANCE = 10.0;
	public static final double FIELD_OF_VIEW = 70.0;
	
	public static int MOVE_DIST =   	5; //cm
	public static long MOVE_TIME =    5000; //ms
	public static int MOVE_ANGL =   	 5; //TODO: measure estimated val.
	public static int VELOCITY_MIN =	15; //cm per second
	
	public static Point home = new Point(10.0, 10.0);
	
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

	public void move( int s ) {
		if(angle != null)
			updateRobotPosition(s, angle);
		
		int d = (int) (s * Command.MOVE.getCal());
		Pair<Integer, Long> moveParams = calcVelocityMoveTime(d);
		setVelocity( (byte) ((int)moveParams.first), (byte) ((int)moveParams.first));

		try {
			Thread.sleep(moveParams.second);
		} catch ( InterruptedException e ) {
		}
		stop();
	}

	// turn left
	public void turn( int angle ) {
		angle = optimizeTurnAngle(angle);
		updateRobotAngle(angle);
		
		int a = (int) (angle * Command.TURN.getCal() * 0.5);
		Pair<Integer, Long> moveParams = calcVelocityMoveTime(a);
		setVelocity( (byte) ((int)-moveParams.first), (byte) ((int)moveParams.first) );

		try {
			Thread.sleep( moveParams.second );
		} catch ( InterruptedException e ) {
		}
		stop();
	}

	private int optimizeTurnAngle(int angle) {
		while(angle > 180) 
			angle -= 360;
		while(angle < -180)
			angle += 360;
		return angle;
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
	

	private void updateRobotPosition(int dist, double angle) {
		if(this.position == null)
			return;
	
		double dX = Math.cos(Math.toRadians(angle)) * dist;
		double dY = Math.sin(Math.toRadians(angle)) * dist;
		
		this.position.x += dX;
		this.position.y += dY;
	}
	
	private void updateRobotAngle(double angle) {
		if(this.angle == null)
			return;
		
		this.angle += angle;
		if(this.angle > 360)
			this.angle -= 360;
		if(this.angle < 0)
			this.angle += 360;
	}
	
	private Pair<Integer, Long> calcVelocityMoveTime(int s) {
		long t = MOVE_TIME;
		int v = (int) (s / (t / 1000));
		
		if(v < VELOCITY_MIN) {
			v = VELOCITY_MIN;
			t = Math.abs((s * 1000) / v);
		}
		
		v = (s < 0) ? -v : v;
		return new Pair<Integer, Long>(v, t);
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
			case MOVE:
				move( s );
				break;
			case TURN:
				turn( s );
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
		MOVE	(1.0), 
		TURN	(1.0);
		
		private double calibrationFactor;
		
		Command(double calibrationFactor) {
			this.calibrationFactor = calibrationFactor;
		}
		
		public double getCal() {
			return calibrationFactor;
		}
		
		public void setCal(double calibrationFactor) {
			this.calibrationFactor = calibrationFactor;
		}
	}
	
	public boolean isAtHome() {
		if(position == null)
			return false;
		return Math.abs(position.x - home.x) < COORDS_TOLERANCE && Math.abs(position.y - home.y) < COORDS_TOLERANCE;  
	}
}