package at.uni.as.colortracking;

import org.opencv.core.Point;

public class Beacon {
	private TrackedColor top;
	private TrackedColor bot;
	private Point coords;
	
	public Beacon(TrackedColor top, TrackedColor bot, Point coords) {
		this.top = top;
		this.bot = bot;
		this.coords = coords;
	}
	
	public TrackedColor getTop() {
		return top;
	}

	public void setTop(TrackedColor top) {
		this.top = top;
	}

	public TrackedColor getBot() {
		return bot;
	}

	public void setBot(TrackedColor bot) {
		this.bot = bot;
	}
	
	public Point getCoords() {
		return coords;
	}

	public void setCoords(Point coords) {
		this.coords = coords;
	}
}
