package io.antmedia.webrtctest;

public abstract class StreamManager {
	WebRTCManager manager;
	long count = 0;
	long totalDt = 0;
	long last;
	boolean firstFrame = true;

	public void start() {}
	public void stop() {}

	public void setManager(WebRTCManager manager) {
		this.manager = manager;
	}
	public int getFramePeriod() {
		if(count != 0) {
			return (int) (totalDt/count);
		} 
		else {
			return 40;
		}
	}

	protected void update() {
		long now = System.currentTimeMillis();
		if(firstFrame) {
			firstFrame = false;
			count = 0;
		}
		else {
			totalDt += (now - last);
			count++;
		}
		last = now;
	}
}
