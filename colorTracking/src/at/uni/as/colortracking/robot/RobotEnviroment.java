package at.uni.as.colortracking.robot;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

import android.util.Log;
import android.util.Pair;
import at.uni.as.colortracking.tracking.TrackedObject;

public class RobotEnviroment {
	public static final double MIN_X = 0;
	public static final double MAX_X = 100;
	public static final double MIN_Y = 0;
	public static final double MAX_Y = 100;
	public static final double HALFWAY_Y = 50;
	public static final double HALFWAY_X = 50;

	public RobotEnviroment() {
	}

	public Point locate(List<TrackedObject> tracks) {
		Point robot = null;
		List<TrackedObject> beacons = new ArrayList<TrackedObject>();

		// Get all beacons (objects with 2 tracked colors).
		if(tracks == null)
			return null;
		
		for (TrackedObject t : tracks)
			if (t.getTrackCount() == 2)
				beacons.add(t);

		if (beacons.size() > 1) {
			// Get distances to beacons.
			Pair<Point, Double> d0 = beacons.get(0)
					.getCoherentDistanceNearest();
			Pair<Point, Double> d1 = beacons.get(1)
					.getCoherentDistanceNearest();
			// Get coordinates of beacons.
			Point p0 = beacons.get(0).getCoords();
			Point p1 = beacons.get(1).getCoords();

			if (d0 == null || d0.second == null || d1 == null || d1.second == null || p0 == null || p1 == null)
				return null;
			
			// check if beacons are in correct order for calculation
			// TODO: clean up this code, by putting it into a separate function and create a wrapper class to be 
			// 		 able to change values as it looks horrible
			//
			if( p0.y == MIN_Y && p1.y == MIN_Y )
				if( ( p1.x > p0.x ) )
				{
					Pair<Point, Double> temp_d = d0;
					d0 = d1;
					d1 = temp_d;
					
					Point temp_p = p0;
					p0 = p1;
					p1 = temp_p;
				}
			else if( p0.x == MIN_X && p1.x == MIN_X )
				if( p0.y > p1.y )
					{
					Pair<Point, Double> temp_d = d0;
					d0 = d1;
					d1 = temp_d;
					
					Point temp_p = p0;
					p0 = p1;
					p1 = temp_p;
					}
			else if( p0.y == MAX_Y && p1.y == MAX_Y )
				if( p0.x > p1.x )
					{
					Pair<Point, Double> temp_d = d0;
					d0 = d1;
					d1 = temp_d;
					
					Point temp_p = p0;
					p0 = p1;
					p1 = temp_p;
					}
			else if( p0.x == MAX_X && p1.x == MAX_X )
				if( p1.y > p0.y )
					{
					Pair<Point, Double> temp_d = d0;
					d0 = d1;
					d1 = temp_d;
					
					Point temp_p = p0;
					p0 = p1;
					p1 = temp_p;
					}
			
			Log.d("blub", "reacgeh calcuation");
			
			double beta = Math.acos( -(Math.pow(d1.second, 2) - Math.pow(d0.second, 2) - Math.pow(HALFWAY_X,2)  )/(2*d0.second*HALFWAY_X) );
			double lc = Math.sin( beta ) * d0.second;
			double la = Math.cos( beta ) * d0.second;

			// Different Cases
			if( p0.x == MIN_X && p1.x == MIN_X )
			{
				if( ( p0.y == MIN_Y && p1.y == HALFWAY_Y ) || ( p0.y == MIN_Y && p1.y == MAX_Y ) ) // beacons( 0:0,0:75 ) or beacons( 0:0, 0:150 )
					robot = new Point( lc, la );
				else if( p0.y == HALFWAY_Y && p1.y == MAX_Y ) // left beacon is 0:75, right one is 0:150
					robot = new Point( lc, HALFWAY_Y + la );
			}
			else if( p0.x == MAX_X && p1.x == MAX_X )
			{
				if( p0.y == HALFWAY_Y && p1.y == MIN_Y ) // left beacon is 150:75, right one is 150:0
					robot = new Point( MAX_X - lc, HALFWAY_Y - la );   
				else if( ( p0.y == MAX_Y && p1.y == HALFWAY_Y ) || ( p0.y == MAX_Y && p1.y == MIN_Y ) ) // beacons( 150:150,150:75 ) or beacons( 150:150, 150:0 )
					robot = new Point( MAX_X - lc, MAX_Y - la );
			}
			else if( p0.y == MIN_Y && p1.y == MIN_Y )
			{
				if( ( p0.x == MAX_X && p1.x == HALFWAY_X ) || ( p0.x == MAX_X && p1.x == MIN_X ) ) // beacons( 150:0,75:0 ) or beacons( 150:0, 0:0 )
					robot = new Point( MAX_X - la, lc ); 
				else if( p0.x == HALFWAY_X && p1.x == MIN_X ) // left beacon is 75:0, right one is 0:0
					robot = new Point( HALFWAY_X - la, lc ); 
			}
			else if( p0.y == MAX_Y && p1.y == MAX_Y )
			{ 
				if( ( p0.x == MIN_X && p1.x == HALFWAY_X ) || ( p0.x == MIN_X && p1.x == MAX_X ) ) // beacons( 0:150,75:150 ) or beacons( 0:150, 150:150 )
					robot = new Point( la, MAX_Y - lc ); 
				else if( p0.x == HALFWAY_X && p1.x == MAX_X )
					robot = new Point( HALFWAY_X + la, MAX_Y - lc ); // left beacon is 75:150, right one is 150:150
			}
		}

		return robot;
	}
}
