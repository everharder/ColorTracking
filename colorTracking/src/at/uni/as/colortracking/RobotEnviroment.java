package at.uni.as.colortracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

public class RobotEnviroment {
	private List<Beacon> beacons = new ArrayList<Beacon>();
	
	public RobotEnviroment() {
	}
	
	public void addBeacon(Beacon beacon){
		beacons.add(beacon);
	}
	
	public List<Beacon> getBeacons(){
		return this.beacons;
	}
	
	public Point locate(List<TrackedObject> tracks){
		Point robot = null;
		
		return robot;
	}
}
