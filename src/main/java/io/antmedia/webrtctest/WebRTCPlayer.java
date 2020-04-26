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
	PlayerUI vPlayer;
	private Object audioCapturerFuture;
	private boolean useUI;
	private VideoCodec videoCodec;
	
	public WebRTCPlayer(boolean useUI, VideoCodec videoCodec) {
		this.useUI = useUI;
		this.videoCodec = videoCodec;
	}
	
	@Override
	public void start() {
		super.start();
		manager.getDecoder().subscribe(this);
		running = true;
		if(useUI) {
			aPlayer = new OpusPlayer();
			aPlayer.init();
			audioCapturerFuture = scheduledExecutorService.scheduleAtFixedRate(new AudioCapturer(), 1000, 10, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void stop() {
		super.stop();
		logger.info("WebRTCPlayer is stopping");
		running = false;
	}

	@Override
	public void onEncodedImage(EncodedImage frame) {
		update();
		if(useUI) {
			vPlayer.play(frame);
		}
	}
	

	class AudioCapturer implements Runnable{

		@Override
		public void run() {
			while(running) 
			{	
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
		if(useUI) {
			vPlayer = new PlayerUI();
			vPlayer.init(width, height, videoCodec);
		}
	}

}
