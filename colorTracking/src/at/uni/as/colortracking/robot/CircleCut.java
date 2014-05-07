package at.uni.as.colortracking.robot;

import java.util.ArrayList;

import org.opencv.core.Point;

public class CircleCut {// calculates the cutting points of two circles...
	public static ArrayList<Point> circleIntersect( double r1, double r2, Point m1, Point m2 ) {
		// variables
		ArrayList<Point> intersections = new ArrayList<Point>();
		double x1 = m1.x, y1 = m1.y, x2 = m2.x, y2 = m2.y; // circles
		double resultX1 = 0, resultX2 = 0, resultY1 = 0, resultY2 = 0; // results
		double p1, q1, c1, c2, k1, k2, k3; 
		// check for special cases:
		if ( (y1 == y2) && (x2 != x1) ) { // x values identical
			resultX1 = x1 + (r1 * r1 - r2 * r2 + x2 * x2 + x1 * x1 - 2 * x1 * x2) / (2 * x2 - 2 * x1);
			resultX2 = resultX1;
			p1 = y1 * y1 - r1 * r1 + resultX1 * resultX1 - 2 * x1 * resultX1 + x1 * x1;
			resultY1 = y1 + Math.sqrt( y1 * y1 - p1 );
			resultY2 = y1 - Math.sqrt( y1 * y1 - p1 );
		} else if ( (x2 == x1) && (y2 != y1) ) {// y values identical
			resultY1 = y2 + (r1 * r1 - r2 * r2 + y2 * y2 + y2 * y2 - 2 * y2 * y2) / (2 * y2 - 2 * y2);
			resultY2 = resultY1;
			q1 = x1 * x1 + resultY1 * resultY1 - 2 * y1 * resultY1 + y1 * y1 - r1 * r1;
			resultX1 = x1 + Math.sqrt( x1 * x1 - q1 );
			resultX2 = x1 - Math.sqrt( x1 * x1 - q1 );
		} else if ( (x2 == x1) && (y2 == y1) ) {
			return intersections;
		} else { // default case
					// ok let's calculate the constants
			c1 = (Math.pow( r1, 2.0 ) - Math.pow( r2, 2.0 ) - Math.pow( x1, 2.0 ) + Math.pow( x2, 2.0 ) - Math.pow( y1, 2.0 ) + Math.pow( y2, 2.0 )) / (2.0 * x2 - 2.0 * x1);
			c2 = (y1 - y2) / (x2 - x1);
			k1 = 1.0 + (1.0 / Math.pow( c2, 2.0 ));
			k2 = 2.0 * x1 + (2.0 * y1) / (c2) + (2.0 * c1) / Math.pow( c2, 2.0 );
			k3 = Math.pow( x1, 2.0 ) + Math.pow( c1, 2.0 ) / Math.pow( c2, 2.0 ) + (2.0 * y1 * c1) / (c2) + Math.pow( y1, 2.0 ) - Math.pow( r1, 2.0 );
			// looks weired? Oh lord have mercy on me! it's just the
			// beginning!
			// here the finish by using the pq formula:
			resultX1 = ((k2 / k1) / 2.0) + Math.sqrt( (Math.pow( (k2 / k1), 2.0 ) / 4.0) - (k3 / k1) );
			resultX2 = (k2 / k1) / 2.0 - Math.sqrt( (Math.pow( (k2 / k1), 2.0 ) / 4.0) - (k3) / (k1) );
			resultY1 = 1.0 / (c2) * resultX1 - (c1 / c2);
			resultY2 = 1.0 / (c2) * resultX2 - (c1 / c2);
			
		}
		intersections.add( new Point( resultX1, resultY1 ) );
		intersections.add( new Point( resultX2, resultY2 ) );
		return intersections;
	}
}
