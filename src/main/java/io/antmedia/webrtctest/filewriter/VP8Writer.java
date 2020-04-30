package io.antmedia.webrtctest.filewriter;

import java.io.IOException;

import org.webrtc.EncodedImage;

public class VP8Writer extends IvfWriter implements IFileWriter{
	
	public VP8Writer(String filename, int width, int height, int scale, int rate) throws IOException {
		super(filename, width, height, scale, rate);
	}

	@Override
	public void onFrame(EncodedImage frame) {
		byte[] data = new byte[frame.buffer.capacity()];
		frame.buffer.get(data);
		try {
			writeFrame(data, frame.captureTimeMs);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}


}
