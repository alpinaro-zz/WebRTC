package io.antmedia.webrtctest.filewriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.webrtc.EncodedImage;

public class H264Writer implements IFileWriter{
	
	private BufferedOutputStream videoFile;

	public H264Writer(String filename) {
		try {
			File file = new File(filename);
			videoFile = new BufferedOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onFrame(EncodedImage frame) {
		byte[] data = new byte[frame.buffer.capacity()];
		frame.buffer.get(data);
		try {
			videoFile.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			videoFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
