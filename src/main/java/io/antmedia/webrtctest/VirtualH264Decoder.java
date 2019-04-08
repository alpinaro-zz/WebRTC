package io.antmedia.webrtctest;


import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.EncodedImage;
import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoDecoder;

import io.antmedia.enterprise.webrtc.codec.VirtualH264Encoder;

public class VirtualH264Decoder implements VideoDecoder {

		protected Logger logger = LoggerFactory.getLogger(VirtualH264Decoder.class);
		private Callback decodeCallback;
		private JavaI420Buffer i420Buffer;
		private long lastKeyFrameRequestTimeMs = 0;
		private long firstFrameTimeMs = 0;
		ArrayList<IPacketListener> listeners = new ArrayList<>();
		
		/**
		 * Timestamp of the last frame that is written to mock encoder
		 * It should be less than 0, because it's compared to 0 at first
		 */
		private long lastFrameTimeMs = -1;
		private Object receivedAudioFrameCount;
		private static final int VIDEO_STREAM_INDEX = 1;
		
		@Override
		public VideoCodecStatus initDecode(Settings settings, Callback decodeCallback) {
			logger.info("initDecode width:{} height:{} ", settings.width, settings.height);
			this.decodeCallback = decodeCallback;
			if (i420Buffer != null) {
				i420Buffer.release();
				i420Buffer = null;
			}
	 		i420Buffer = JavaI420Buffer.allocate(settings.width, settings.height);
	 		
	 		for (IPacketListener listener : listeners) {
				listener.onDecoderSettings(settings.width, settings.height);
			}
			return VideoCodecStatus.OK;
		}

		@Override
		public VideoCodecStatus release() {
			logger.info("0 release decode {}", this);
			if (i420Buffer != null) {
				i420Buffer.release();
				i420Buffer = null;
			}
			logger.info("1 release decode {}", this);
			return VideoCodecStatus.OK;
		}

		@Override
		public VideoCodecStatus decode(EncodedImage frame, DecodeInfo info) 
		{
			for (IPacketListener listener : listeners) {
				listener.onEncodedImage(frame);
			}
			return VideoCodecStatus.OK;
		}

		@Override
		public boolean getPrefersLateDecoding() {
			return false;
		}

		@Override
		public String getImplementationName() {
			return this.getClass().getSimpleName();
		}

		public void subscribe(IPacketListener listener) {
			listeners.add(listener);
		}

	}