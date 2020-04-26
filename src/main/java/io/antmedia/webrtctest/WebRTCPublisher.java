package io.antmedia.webrtctest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.JavaI420Buffer;
import org.webrtc.NaluIndex;
import org.webrtc.VideoFrame;

public class WebRTCPublisher extends StreamManager{
	protected int fps = 24;
	private Logger logger = LoggerFactory.getLogger(WebRTCPublisher.class);


	private boolean started = false;
	private JavaI420Buffer i420Buffer;
	private FileReader reader;
	ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
	private ScheduledFuture<?> videoSenderFuture;
	private ScheduledFuture<?> audioSenderFuture;
	private boolean loop;

	public WebRTCPublisher(FileReader reader, boolean loop) 
	{
		this.reader = reader; 
		this.loop = loop;
	}

	@Override
	public void start() {
		super.start();
		if(started) {
			return;
		}
		i420Buffer = JavaI420Buffer.allocate(reader.width, reader.height);

		videoSenderFuture = scheduledExecutorService.scheduleAtFixedRate(new VideoSender(), 200, 1000/fps, TimeUnit.MILLISECONDS);
		audioSenderFuture = scheduledExecutorService.scheduleAtFixedRate(new AudioSender(), 200, 20, TimeUnit.MILLISECONDS);

		started  = true;
	}

	void close()
	{
		scheduledExecutorService.shutdown();
	}

	@Override
	public void stop() {
		super.stop();
		stopVideo();
		stopAudio();
	}

	class VideoSender implements Runnable {
		int lastSentPacket = 0;

		@Override
		public void run() {
			update();
			
			if(lastSentPacket < reader.videoFrames.size()) {
				FileReader.Frame frame = reader.videoFrames.get(lastSentPacket++);
				frame.data.rewind();
				List<NaluIndex> naluIndices = findNaluIndices(frame.data);
				manager.getEncoder().setEncodedFrameBuffer(frame.data, frame.isKeyFrame, frame.timeStamp*1000*1000, 0, naluIndices, "0");
				//VideoFrame fakeFrame = new VideoFrame(i420Buffer, 0, frame.timeStamp*1000*1000);
				//manager.getVideoObserver().onFrameCaptured(fakeFrame);
			}
			else if(loop) {
					lastSentPacket = 0;
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
				FileReader.Frame frame = reader.audioFrames.get(lastSentPacket++);
				frame.data.rewind();
				manager.getAudioRecord().notifyEncodedData(frame.data); //20ms of audio encoded data
			}
			else if(loop) {
				lastSentPacket = 0;
			}
			else {
				WebRTCPublisher.this.stopAudio();
			}
		}
	}

	public void stopAudio() {
		logger.info("Stopping audio streaming");
		audioSenderFuture.cancel(true);
		if (videoSenderFuture.isCancelled()) {
			manager.stop();
		}
	}

	public void stopVideo() {
		logger.info("Stopping video streaming");
		videoSenderFuture.cancel(true);
		if (audioSenderFuture.isCancelled()) {
			manager.stop();
		}
	}
	
private List<NaluIndex> findNaluIndices(ByteBuffer buffer) {
		
		int kNaluShortStartSequenceSize = 3;
		List<NaluIndex> naluSequence = new ArrayList<>();
		
		int size = 0;
		if (buffer.limit() >= kNaluShortStartSequenceSize) {
			int end = buffer.limit() - kNaluShortStartSequenceSize;
			
			for (int i = 0; i < end;) {
				if (buffer.get(i+2) > 1) {
					i += 3;
				}
				else if (buffer.get(i+2) == 1 && buffer.get(i+1) == 0 && buffer.get(i) == 0) {
					// We found a start sequence, now check if it was a 3 of 4 byte one.
					NaluIndex index = new NaluIndex(i,i+3, 0);
					if (index.startOffset > 0 && buffer.get(index.startOffset - 1) == 0) {
						index.startOffset--;
					}
					size = naluSequence.size();
					if (size >= 1) {
						naluSequence.get(size-1).payloadSize = index.startOffset - naluSequence.get(size-1).payloadStartOffset;
					}
					
					naluSequence.add(index);
					i += 3;
				}
				else {
					++i;
				}
			}
		}
		
		size = naluSequence.size();
		if (size >= 1) {
			naluSequence.get(size-1).payloadSize = buffer.limit() - naluSequence.get(size-1).payloadStartOffset;
		}
		
		
		return naluSequence;
	}
	
	
}
