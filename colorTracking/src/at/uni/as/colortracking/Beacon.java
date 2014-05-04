package at.uni.as.colortracking;

import org.opencv.core.Point;

public class Beacon {
	private TrackedObject top;
	private TrackedObject bot;
	private Point coords;
	
	public Beacon(TrackedObject top, TrackedObject bot, Point coords) {
		this.top = top;
		this.bot = bot;
		this.coords = coords;
	}
	
	public TrackedObject getTop() {
		return top;
	}

	public void setTop(TrackedObject top) {
		this.top = top;
	}

	public TrackedObject getBot() {
		return bot;
	}

	public void setBot(TrackedObject bot) {
		this.bot = bot;
	}
	
	public Point getCoords() {
		return coords;
	}

	public void setCoords(Point coords) {
		this.coords = coords;
	}
}
