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
	
	public Point locate(Beacon b1, Beacon b2){
		Point robot = new Point();
		
		return robot;
	}
	
	public Point locate(List<TrackedColor> tracks){
		Point robot = null;
		
		return robot;
	}
}
