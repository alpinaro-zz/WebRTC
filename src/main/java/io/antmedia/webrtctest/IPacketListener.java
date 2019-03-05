package io.antmedia.webrtctest;

import org.webrtc.EncodedImage;

public interface IPacketListener {
	public void onEncodedImage(EncodedImage frame);
	public void onDecoderSettings(int width, int height);
}
