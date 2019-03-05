package io.antmedia.enterprise.webrtc.codec;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.BitrateAdjuster;
import org.webrtc.DynamicBitrateAdjuster;
import org.webrtc.EncodedImage;
import org.webrtc.FramerateBitrateAdjuster;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoFrame;

public class VirtualH264Encoder implements VideoEncoder {

	protected static Logger logger = LoggerFactory.getLogger(VirtualH264Encoder.class);

	private static final int MAX_ENCODER_Q_SIZE = 2;
	private static final int MAX_VIDEO_FRAMERATE = 30;

	private BitrateAdjuster bitrateAdjuster;
	private Callback callback;
	private boolean automaticResizeOn;
	private int adjustedBitrate;

	@Nullable private ByteBuffer encodedFrameBuffer = null;

	private boolean isKeyFrame;

	private int frameRotation;

	private int time2Log;

	VirtualH264Encoder() {
		bitrateAdjuster = new FramerateBitrateAdjuster();
	}


	@Override
	public VideoCodecStatus initEncode(Settings settings, Callback encodeCallback) {
		this.callback = encodeCallback;
		automaticResizeOn = settings.automaticResizeOn;

		if (settings.startBitrate != 0 && settings.maxFramerate != 0) {
			bitrateAdjuster.setTargets(settings.startBitrate * 1000, settings.maxFramerate);
		}
		adjustedBitrate = bitrateAdjuster.getAdjustedBitrateBps();

		logger.info("initEncode: {} x {}. @ {}kbps"
						+ " {}fps automaticResizeOn: {} adjusted bitrate: {} for client:", settings.width , settings.height, 
						settings.startBitrate, settings.maxFramerate,  automaticResizeOn, adjustedBitrate, this.hashCode());
		return VideoCodecStatus.OK;
	}

	@Override
	public VideoCodecStatus release() {
		return VideoCodecStatus.OK;
	}

	@Override
	public VideoCodecStatus encode(VideoFrame videoFrame, EncodeInfo info) {

		synchronized (this) {
			if (info != null) {
				logger.debug("encode video frame type: {}", info.frameTypes[0]);
			}
			EncodedImage.Builder builder = EncodedImage.builder()
					.setCaptureTimeNs(videoFrame.getTimestampNs())
					.setCompleteFrame(false)
					.setEncodedWidth(videoFrame.getBuffer().getWidth())
					.setEncodedHeight(videoFrame.getBuffer().getHeight())
					.setRotation(videoFrame.getRotation());

			int encodedFrameSize = encodedFrameBuffer.limit();
			bitrateAdjuster.reportEncodedFrame(encodedFrameSize);
			if (adjustedBitrate != bitrateAdjuster.getAdjustedBitrateBps()) {
				updateBitrate();
			}	

			logger.trace("encode video frame timestamp: {} encodedFrameBuffer: {}  ",
					videoFrame.getTimestampNs(), encodedFrameBuffer);

			final EncodedImage.FrameType frameType = isKeyFrame
					? EncodedImage.FrameType.VideoFrameKey
							: EncodedImage.FrameType.VideoFrameDelta;

			encodedFrameBuffer.rewind();
			builder.setBuffer(encodedFrameBuffer).setFrameType(frameType).setRotation(frameRotation);
			callback.onEncodedFrame(builder.createEncodedImage(), new CodecSpecificInfo());
			return VideoCodecStatus.OK;

		}
	}

	private void updateBitrate() {
		adjustedBitrate = bitrateAdjuster.getAdjustedBitrateBps();
	}

	public int getAdjustedBitrate() {
		return adjustedBitrate;
	}


	@Override
	public VideoCodecStatus setChannelParameters(short packetLoss, long roundTripTimeMs) {
		return VideoCodecStatus.OK; // No op.
	}

	@Override
	public VideoCodecStatus setRateAllocation(BitrateAllocation bitrateAllocation, int framerate) {
		if (framerate > MAX_VIDEO_FRAMERATE) {
			framerate = MAX_VIDEO_FRAMERATE;
		}
		if (time2Log > 50) {
			logger.info("setRateAllocation bitrate: {} framerate: {} adjusted bitrate:{} for client: {}" , 
							bitrateAllocation.getSum() , framerate, adjustedBitrate, this.hashCode());
			time2Log = 0;
		}
		time2Log++;
		bitrateAdjuster.setTargets(bitrateAllocation.getSum(), framerate);
		return VideoCodecStatus.OK;
	}

	@Override
	public ScalingSettings getScalingSettings() 
	{
		logger.debug("getScalingSettings for {}", this);
		return ScalingSettings.OFF;
	}

	@Override
	public String getImplementationName() {
		return this.getClass().getSimpleName();
	}

	public void setEncodedFrameBuffer(ByteBuffer encodedFrameBuffer, boolean isKeyFrame, int frameRotation) {
		synchronized (this) {
			logger.trace("setEncodedFrameBuffer: {} iskeyFrame:{}, rotation:{}", encodedFrameBuffer, isKeyFrame, frameRotation);
			this.encodedFrameBuffer = encodedFrameBuffer;
			this.isKeyFrame = isKeyFrame;
			this.frameRotation = frameRotation;
		}
	}


}
