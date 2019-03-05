package io.antmedia.webrtctest;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.EncodedImage;

public class WebRTCPlayer extends StreamManager implements IPacketListener{

	protected boolean running;
	ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
	private Logger logger = LoggerFactory.getLogger(WebRTCPlayer.class);

	
	OpusPlayer aPlayer;
	H264Player vPlayer;
	private Object audioCapturerFuture;
	int count = 0;
	int totalDt = 0;
	long last = System.currentTimeMillis();

	@Override
	public void start() {
		manager.getDecoder().subscribe(this);
		running = true;
		if(Settings.instance.useUI) {
			aPlayer = new OpusPlayer();
			aPlayer.init();
			audioCapturerFuture = scheduledExecutorService.scheduleAtFixedRate(new AudioCapturer(), 1000, 10, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public void onEncodedImage(EncodedImage frame) {
		long now = System.currentTimeMillis();
		totalDt += (now - last);
		last = now;
		
		if((count++)%Settings.instance.frameLogPeriod == 0) {
			logger.info("received fps total time/count:{}/{}={}",totalDt, count, (totalDt/count));
		}
		
		if(Settings.instance.useUI) {
			vPlayer.play(frame);
		}
	}

	class AudioCapturer implements Runnable{

		@Override
		public void run() {
			while(running) {
				ByteBuffer playoutData = manager.getAudioTrack().getPlayoutData();
				int readSizeInBytes = manager.getAudioTrack().getReadSizeInBytes();
				byte[] audioData = new byte[readSizeInBytes];

				playoutData.get(audioData, 0, audioData.length);

				aPlayer.play(audioData);
			}

		}

	}


	@Override
	public void onDecoderSettings(int width, int height) {
		if(Settings.instance.useUI) {
			vPlayer = new H264Player();
			vPlayer.init(width, height);
		}
		
	}

}
