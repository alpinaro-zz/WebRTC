package io.antmedia.webrtctest;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.bytedeco.javacpp.avcodec.AVCodecContext;

public class OpusPlayer {
	AVCodecContext codecContext;
	private SourceDataLine line;

	void init() {
		// select audio format parameters
		AudioFormat af = new AudioFormat(16000, 16, 1, true, false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			// prepare audio output
			line.open(af, 4096);
			line.start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	public void play(byte[] audioData) {

		line.write(audioData, 0, audioData.length);
	}
}
