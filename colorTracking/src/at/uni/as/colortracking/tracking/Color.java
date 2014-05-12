package at.uni.as.colortracking.tracking;

import org.opencv.core.Scalar;

public enum Color {
	RED		(new Scalar(9, 255, 176)),
	BLUE	(new Scalar(48, 210, 56)),
	GREEN	(new Scalar(113, 148, 32)),
	YELLOW	(new Scalar(33, 188, 210)),
	WHITE	(new Scalar(210, 210, 210));
	
	private final Scalar color;
	
	Color(Scalar color) {
		this.color = color;
	}
	
	public Scalar color() {
		return color;
	}
}
