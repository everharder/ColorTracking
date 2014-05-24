package at.uni.as.colortracking.robot;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jp.ksksue.driver.serial.FTDriver;

import org.opencv.core.Point;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;

@SuppressLint("UseValueOf")
public class Robot {
	@SuppressWarnings("unused")
	private String TAG = "iRobot";

	public static final double CATCH_DIST = 25.0;
	public static final double COORDS_TOLERANCE = 10.0;
	public static final double FIELD_OF_VIEW = 110.0;
	
	
	public static int MOVE_DIST =   20; //cm
	public static int MOVE_TIME = 1000; //ms
	public static int MOVE_ANGL =    5; //TODO: measure estimated val.
	public static final int BEACONNOTFOUND_DELAY = 500; //ms
	
	private FTDriver com;
	private Point position = null;

	public Robot() {
	}

	public Robot(FTDriver com) {
		this();
		this.com = com;
		connect();
	}

	public Robot(FTDriver com, Point position) {
		this( com );
		this.position = position;
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

	/**
	 * transfers given bytes via the serial connection.
	 * 
	 * @param data
	 */
	public void comWrite( byte[] data ) {
		if ( isConnected() ) {
			com.write( data );
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

	/**
	 * write data to serial interface, wait 100 ms and read answer.
	 * 
	 * @param data
	 *            to write
	 * @return answer from serial interface
	 */
	public String comReadWrite( byte[] data ) {
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

	// move forward
	public void moveForward( int s, int t ) {
		s *= Command.FORWARD.cal();
		setVelocity( (byte) s, (byte) s );

		try {
			Thread.sleep( t );
		} catch ( InterruptedException e ) {
		}

		stop();
	}

	// move backward
	public void moveBackward( int s, int t ) {
		s *= Command.BACKWARD.cal();
		setVelocity( (byte) -s, (byte) -s );

		try {
			Thread.sleep( t );
		} catch ( InterruptedException e ) {
		}

		stop();
	}

	// turn left
	public void turnLeft( double angle ) {
		angle *= Command.LEFT.cal();
		setVelocity( (byte) 0, (byte) angle );

		try {
			Thread.sleep( MOVE_TIME );
		} catch ( InterruptedException e ) {
		}
		stop();
	}

	// turn right
	public void turnRight( double angle ) {
		angle *= Command.RIGHT.cal();
		setVelocity( (byte) angle, (byte) 0 );

		try {
			Thread.sleep( MOVE_TIME );
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

	public void sensor( View v ) {
		comReadWrite( new byte[] { 'q', '\r', '\n' } );
	}

	public Point getPosition() {
		return position;
	}

	public void setPosition( Point position ) {
		this.position = position;
	}

	public static Command getRandomCommand() {
		return getRandomCommand( Arrays.asList( Command.values() ) );
	}

	public static Command getRandomCommand( List<Command> commands ) {
		Random r = new Random();
		int randomNumber = r.nextInt( commands.size() );

		return commands.get( randomNumber );
	}

	public void doCommand( Command c ) {
		doCommand(c, MOVE_DIST);
	}

	public void doCommand( Command c, int s) {
		doCommand(c, s, MOVE_TIME);
	}
	
	public void doCommand( Command c, int s, int t ) {
		switch ( c ) {
			case FORWARD:
				moveForward( s, t );
				break;
			case BACKWARD:
				moveBackward( s, t );
				break;
			case LEFT:
				turnLeft( s * (t / 1000));
				break;
			case RIGHT:
				turnRight( s * (t / 1000));
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