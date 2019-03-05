package io.antmedia.webrtctest;

public abstract class StreamManager {
	WebRTCManager manager;

	public void start() {}
	public void stop() {}
	
	public void setManager(WebRTCManager manager) {
		this.manager = manager;
	}
}
