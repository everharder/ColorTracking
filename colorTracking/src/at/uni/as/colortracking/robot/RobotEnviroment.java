package at.uni.as.colortracking.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import at.uni.as.colortracking.tracking.Beacon;
import at.uni.as.colortracking.tracking.Color;
import at.uni.as.colortracking.tracking.TrackedBeacon;
import at.uni.as.colortracking.tracking.TrackedColor;

public class RobotEnviroment {
	public static final double MIN_X = 0;
	public static final double MAX_X = 100;
	public static final double MIN_Y = 0;
	public static final double MAX_Y = 100;
	public static final double HALFWAY_Y = 50;
	public static final double HALFWAY_X = 50;
	private static final double MAX_BEACON_STRIP_DIST_PXL = 50;
	
	private Mat homography = null;

	public RobotEnviroment() {
	}

	public static List<TrackedBeacon> extractBeacons(Map<Color, List<TrackedColor>> detectedObjects) {
		List<TrackedBeacon> beacons = new ArrayList<TrackedBeacon>();
		
		boolean beaconDetected = false;
		for(Beacon b : Beacon.values()) {
			List<TrackedColor> upperColor = detectedObjects.get(b.upperColor());
			List<TrackedColor> lowerColor = detectedObjects.get(b.lowerColor());
			
			if(	upperColor == null || upperColor.size() == 0 || 
				lowerColor == null || lowerColor.size() == 0)
				continue;
			
			for(TrackedColor u : upperColor) {
				for(TrackedColor l : lowerColor) {
					if(Math.abs(l.getBorders().x - u.getBorders().x) < MAX_BEACON_STRIP_DIST_PXL && (l.getBorders().y - (u.getBorders().y + u.getBorders().height)) < MAX_BEACON_STRIP_DIST_PXL) {
						beacons.add(new TrackedBeacon(b, u, l));
						beaconDetected = true;
					}
				}
				
				if(beaconDetected)
					break;
			}
			
			beaconDetected = false;
		}
		
		return beacons;
	}

	public static Point calcPosition(List<TrackedBeacon> beacons, Mat homography) {
		Point robot = null;

		// Get all beacons (objects with 2 tracked colors).
		if(beacons == null || beacons.size() < 2 || homography == null)
			return null;
		
		// Get distances to beacons.
		beacons.get(0).calcDistance(homography);
		beacons.get(1).calcDistance(homography);
		
		double d0 = beacons.get(0).getDistance();
		double d1 = beacons.get(1).getDistance();
		
		// Get coordinates of beacons.
		Point p0 = beacons.get(0).getBeacon().coords();
		Point p1 = beacons.get(1).getBeacon().coords();

		if (d0 < 0 || d1 < 0 || p0 == null || p1 == null)
			return null;
		
		// check if beacons are in correct order for calculation
		if( p0.y == MIN_Y && p1.y == MIN_Y ) {
			if( ( p1.x > p0.x ) ) {
				double temp_d = d0;
				d0 = d1;
				d1 = temp_d;
				
				Point temp_p = p0;
				p0 = p1;
				p1 = temp_p;
			}
		} else if( p0.x == MIN_X && p1.x == MIN_X ) {
			if( p0.y > p1.y ){
				double temp_d = d0;
				d0 = d1;
				d1 = temp_d;
				
				Point temp_p = p0;
				p0 = p1;
				p1 = temp_p;
			}
		} else if( p0.y == MAX_Y && p1.y == MAX_Y ) {
			if( p0.x > p1.x ){
				double temp_d = d0;
				d0 = d1;
				d1 = temp_d;
				
				Point temp_p = p0;
				p0 = p1;
				p1 = temp_p;
			}
		} else if( p0.x == MAX_X && p1.x == MAX_X ) {
			if( p1.y > p0.y ){
				double temp_d = d0;
				d0 = d1;
				d1 = temp_d;
				
				Point temp_p = p0;
				p0 = p1;
				p1 = temp_p;
			}
		}
		
		double beta = Math.acos( -(Math.pow(d1, 2) - Math.pow(d0, 2) - Math.pow(HALFWAY_X,2)  )/(2 * d0 * HALFWAY_X) );
		double lc = Math.sin( beta ) * d0;
		double la = Math.cos( beta ) * d0;

		// Different Cases
		if( p0.x == MIN_X && p1.x == MIN_X ){
			if( ( p0.y == MIN_Y && p1.y == HALFWAY_Y ) || ( p0.y == MIN_Y && p1.y == MAX_Y ) ) // beacons( 0:0,0:75 ) or beacons( 0:0, 0:150 )
				robot = new Point( lc, la );
			else if( p0.y == HALFWAY_Y && p1.y == MAX_Y ) // left beacon is 0:75, right one is 0:150
				robot = new Point( lc, HALFWAY_Y + la );
			
		} else if( p0.x == MAX_X && p1.x == MAX_X ){
			if( p0.y == HALFWAY_Y && p1.y == MIN_Y ) // left beacon is 150:75, right one is 150:0
				robot = new Point( MAX_X - lc, HALFWAY_Y - la );   
			else if( ( p0.y == MAX_Y && p1.y == HALFWAY_Y ) || ( p0.y == MAX_Y && p1.y == MIN_Y ) ) // beacons( 150:150,150:75 ) or beacons( 150:150, 150:0 )
				robot = new Point( MAX_X - lc, MAX_Y - la );
			
		} else if( p0.y == MIN_Y && p1.y == MIN_Y ){
			if( ( p0.x == MAX_X && p1.x == HALFWAY_X ) || ( p0.x == MAX_X && p1.x == MIN_X ) ) // beacons( 150:0,75:0 ) or beacons( 150:0, 0:0 )
				robot = new Point( MAX_X - la, lc ); 
			else if( p0.x == HALFWAY_X && p1.x == MIN_X ) // left beacon is 75:0, right one is 0:0
				robot = new Point( HALFWAY_X - la, lc ); 
			
		} else if( p0.y == MAX_Y && p1.y == MAX_Y ) { 
			if( ( p0.x == MIN_X && p1.x == HALFWAY_X ) || ( p0.x == MIN_X && p1.x == MAX_X ) ) // beacons( 0:150,75:150 ) or beacons( 0:150, 150:150 )
				robot = new Point( la, MAX_Y - lc ); 
			else if( p0.x == HALFWAY_X && p1.x == MAX_X )
				robot = new Point( HALFWAY_X + la, MAX_Y - lc ); // left beacon is 75:150, right one is 150:150
		}

		return robot;
	}
	
	public Mat getHomography() {
		return homography;
	}
}
