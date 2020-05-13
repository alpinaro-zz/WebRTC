package io.antmedia.webrtctest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.EncodedImage;

import io.antmedia.webrtc.VideoCodec;
import io.antmedia.webrtctest.filewriter.H264Writer;
import io.antmedia.webrtctest.filewriter.IFileWriter;
import io.antmedia.webrtctest.filewriter.VP8Writer;

public class WebRTCPlayer extends StreamManager implements IPacketListener{

	protected boolean running;
	ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
	private Logger logger = LoggerFactory.getLogger(WebRTCPlayer.class);

	
	OpusPlayer aPlayer;
	PlayerUI vPlayer;
	
	private Settings settings;
	private ScheduledFuture<?> audioCapturerFuture;
	private IFileWriter fileWriter;
	
	public WebRTCPlayer(Settings settings) {
		this.settings = settings;
	}
	
	@Override
	public void start() {
		super.start();
		manager.getDecoder().subscribe(this);
		running = true;
		if(settings.useUI) {
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
		
		if(fileWriter != null) {
			fileWriter.stop();
		}
	}

	@Override
	public void onEncodedImage(EncodedImage frame) {
		update();
		
		if(settings.useUI) {
			vPlayer.play(frame);
		}
		
		if(fileWriter != null) {
			fileWriter.onFrame(frame);
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
		if(settings.useUI) {
			vPlayer = new PlayerUI();
			vPlayer.init(width, height, settings.codec);
		}
		
		if(!settings.streamSource.isEmpty()) {
			String extension = settings.streamSource.contains(".") ?
					settings.streamSource.split("\\.")[1] : 
						"na";
					
			if (settings.codec == VideoCodec.H264 && extension.equals("h264")) {
				fileWriter= new H264Writer(settings.streamSource);
			}
			else if (settings.codec == VideoCodec.VP8 && extension.equals("ivf")) {
				try {
					fileWriter= new VP8Writer(settings.streamSource, width, height, 1, 1000000);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (settings.codec == VideoCodec.H265 && extension.equalsIgnoreCase("h265")) {
				fileWriter = new H264Writer(settings.streamSource);
			}
		} 
	}

}
