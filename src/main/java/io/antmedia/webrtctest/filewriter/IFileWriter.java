package io.antmedia.webrtctest.filewriter;

import org.webrtc.EncodedImage;

public interface IFileWriter {
	public void onFrame(EncodedImage frame);
	public void stop();
}
