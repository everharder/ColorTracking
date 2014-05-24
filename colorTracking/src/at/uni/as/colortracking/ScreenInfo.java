package at.uni.as.colortracking;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class ScreenInfo {
	private static ScreenInfo instance;

	public static final int POS_TOP_LEFT = 0;
	public static final int POS_TOP_RIGHT = 1;
	public static final int POS_BOTTOM_RIGHT = 2;
	public static final int POS_BOTTOM_LEFT = 3;
	public static final int DEFAULT_SIZE = 1;

	public static final Scalar COLOR_RED = new Scalar( 255.0, 0.0, 0.0 );
	public static final Scalar COLOR_GREEN = new Scalar( 0.0, 255.0, 0.0 );
	public static final Scalar COLOR_BLUE = new Scalar( 0.0, 0.0, 255.0 );
	public static final Scalar COLOR_WHITE = new Scalar( 255.0, 255.0, 255.0 );

	private ArrayList<ScreenInfoObject> messages = new ArrayList<ScreenInfoObject>();

	private ScreenInfo() {
	}

	public static ScreenInfo getInstance() {
		if ( instance == null ) instance = new ScreenInfo();

		return instance;
	}

	public void add( String message, int position, int size, Scalar color ) {
		messages.add( new ScreenInfoObject( message, position, size, color ) );
	}
	
	public void add( String message, int position, Scalar color ) {
		add(message, position, DEFAULT_SIZE, color);
	}

	public void print( Mat image ) {
		int screenOffset = 100;
		int charHeight = 30;
		int length = messages.size();
		ScreenInfoObject obj;

		for ( int i = length - 1; i >= 0; i-- ) {
			obj = messages.remove( i );
			Point pos;
			switch ( obj.getPosition() ) {
				case POS_TOP_LEFT:
					pos = new Point( screenOffset, screenOffset + ((i+1) * charHeight * obj.getSize()) );
					break;
				case POS_TOP_RIGHT:
					pos = new Point( image.width(), screenOffset + ((i+1) * charHeight * obj.getSize()) );
					break;
				case POS_BOTTOM_RIGHT:
					pos = new Point( screenOffset + image.width() / 2, image.height() - screenOffset - ((i+1) * charHeight * obj.getSize()) );
					break;
				case POS_BOTTOM_LEFT:
					pos = new Point( screenOffset, image.height() - screenOffset - ((i+1) * charHeight * obj.getSize()) );
					break;
				default:
					pos = new Point( screenOffset + image.width() / 2, image.height() / 2 - screenOffset - ((i+1) * charHeight * obj.getSize()) );
					break; // default center
			}
			Core.putText( image, obj.getMessage(), pos, Core.FONT_HERSHEY_SIMPLEX, obj.getSize(), obj.getColor() );
			screenOffset = 0;
		}
	}

	private class ScreenInfoObject {
		String message;
		int position;
		int size;
		Scalar color;

		public ScreenInfoObject(String message, int position, int size, Scalar color) {
			this.message = message;
			this.position = position;
			this.size = size;
			this.color = color;
		}

		public String getMessage() {
			return message;
		}

		public int getSize() {
			return size;
		}

		public Scalar getColor() {
			return color;
		}

		public int getPosition() {
			return position;
		}
	}
}
