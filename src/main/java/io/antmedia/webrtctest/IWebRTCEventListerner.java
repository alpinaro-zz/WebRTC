package io.antmedia.webrtctest;

public interface IWebRTCEventListerner {
	public void onCompleted();
	public void onDataChannelMessage(String string);
}
