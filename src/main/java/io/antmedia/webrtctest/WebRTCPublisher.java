package io.antmedia.webrtctest;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;

public class WebRTCPublisher extends StreamManager{
	protected int fps = 24;
	private Logger logger = LoggerFactory.getLogger(WebRTCPublisher.class);


	private boolean started = false;
	private JavaI420Buffer i420Buffer;
	private MP4Reader reader;
	ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
	private ScheduledFuture<?> videoSenderFuture;
	private ScheduledFuture<?> audioSenderFuture;

	public WebRTCPublisher(MP4Reader reader) 
	{
		this.reader = reader; 
	}

	@Override
	public void start() {
		if(started) {
			return;
		}
		i420Buffer = JavaI420Buffer.allocate(reader.width, reader.height);

		videoSenderFuture = scheduledExecutorService.scheduleAtFixedRate(new VideoSender(), 200, 1000/fps, TimeUnit.MILLISECONDS);
		audioSenderFuture = scheduledExecutorService.scheduleAtFixedRate(new AudioSender(), 200, 10, TimeUnit.MILLISECONDS);

		started  = true;
	}

	void close()
	{
		scheduledExecutorService.shutdown();
	}

	@Override
	public void stop() {
		stopVideo();
		stopAudio();
	}

	class VideoSender implements Runnable {
		int lastSentPacket = 0;

		@Override
		public void run() {
			update();
			
			if(lastSentPacket < reader.videoFrames.size()) {
				MP4Reader.Frame frame = reader.videoFrames.get(lastSentPacket++);
				frame.data.rewind();
				manager.getEncoder().setEncodedFrameBuffer(frame.data, frame.isKeyFrame, 0);
				VideoFrame fakeFrame = new VideoFrame(i420Buffer, 0, frame.timeStamp*1000);
				manager.getVideoObserver().onFrameCaptured(fakeFrame);
			}
			else {
				WebRTCPublisher.this.stopVideo();
			}
		}
	}

	class AudioSender implements Runnable {

		int lastSentPacket = 0;
		@Override
		public void run() {
			if(lastSentPacket < reader.audioFrames.size()) {
				MP4Reader.Frame frame = reader.audioFrames.get(lastSentPacket++);
				frame.data.rewind();

				manager.getAudioRecord().notifyEncodedData(frame.data); //20ms of audio encoded data
				manager.getAudioRecord().notifyDataWithEmptyBuffer(); //10ms of raw audio data
				manager.getAudioRecord().notifyDataWithEmptyBuffer(); //10ms of raw audio data
			}
			else {
				WebRTCPublisher.this.stopAudio();
			}
		}
	}

	public void stopAudio() {
		audioSenderFuture.cancel(true);
	}

	public void stopVideo() {
		videoSenderFuture.cancel(true);
	}
}
