package at.uni.as.colortracking.tracking;

import org.opencv.core.Mat;

public class TrackedBall {
	private TrackedColor ballColor;
	
	public TrackedColor getBallColor() {
		return ballColor;
	}

	public void setBallColor( TrackedColor ballColor ) {
		this.ballColor = ballColor;
	}
	
	public double getDistance() {
		if(ballColor == null)
			return -1;
		return ballColor.getDistance();
	}

	public void calcDistance(Mat homography) {
		ballColor.calcDistance(homography);
	}
}
