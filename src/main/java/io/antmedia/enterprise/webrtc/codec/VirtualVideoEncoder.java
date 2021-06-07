package io.antmedia.enterprise.webrtc.codec;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.BitrateAdjuster;
import org.webrtc.EncodedImage;
import org.webrtc.FramerateBitrateAdjuster;
import org.webrtc.NaluIndex;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoFrame;


public class VirtualVideoEncoder implements VideoEncoder {
	
	public interface IVideoPacketSender {
		public void sendVideo(ByteBuffer encodedFrameBuffer, boolean isKeyFrame, long timestampNs, int frameRotation, List<NaluIndex> naluIndices, String trackId);
	}

	protected static Logger logger = LoggerFactory.getLogger(VirtualVideoEncoder.class);

	private static final int MAX_VIDEO_FRAMERATE = 60;

	private BitrateAdjuster bitrateAdjuster;
	private int adjustedBitrate;

	/**
	 * The method to send video. 
	 * This interface let us not to have callback
	 */
	private IVideoPacketSender sendVideo;
	private int time2Log;

	private Queue<Short> packetLossMeasurements = new LinkedList<>();

	private Queue<Long> rttMeasurements = new LinkedList<>();
	
	private CodecSpecificInfo codecInfo = new CodecSpecificInfo();

	private int windowSize = 5;

	private int packetLossAverage;

	private int rttMeasurementsAverage;

	private int time2LogChannel = 0;

	VirtualVideoEncoder() {
		bitrateAdjuster = new FramerateBitrateAdjuster();
		setSendVideo((encodedFrameBuffer, isKeyFrame, timestampNs, frameRotation, naluIndices, trackIndex) -> {
			logger.info("Callback is not set in encoder for {}", this.hashCode());
		});
		
	}

	public VideoCodecStatus initEncode(Settings settings, Callback encodeCallback) {

		if (settings.startBitrate != 0 && settings.maxFramerate != 0) {
			bitrateAdjuster.setTargets(settings.startBitrate * 1000, settings.maxFramerate);
		}
		adjustedBitrate = bitrateAdjuster.getAdjustedBitrateBps();

		logger.info("initEncode: {} x {}. @ {}kbps"
						+ " {}fps automaticResizeOn: {} adjusted bitrate: {} for client:", settings.width , settings.height, 
						settings.startBitrate, settings.maxFramerate,  settings.automaticResizeOn, adjustedBitrate, this.hashCode());
		
		setSendVideo((encodedFrameBuffer, isKeyFrame, timestampNs, frameRotation, naluIndices, trackIndex) -> {
			
			//this is the part where video is being sent
			encodedFrameBuffer.rewind();
			
			EncodedImage encodedImage = EncodedImage.builder()
					.setCompleteFrame(true)
					.setFrameType(isKeyFrame ? EncodedImage.FrameType.VideoFrameKey
							: EncodedImage.FrameType.VideoFrameDelta)
					.setRotation(frameRotation)
					.setBuffer(encodedFrameBuffer)
					.setNaluIndices(naluIndices)
					.setEncodedHeight(1)
					.setEncodedWidth(1)
					.createEncodedImage();
						
			int encodedFrameSize = encodedImage.buffer.limit();
			bitrateAdjuster.reportEncodedFrame(encodedFrameSize);
			if (adjustedBitrate != bitrateAdjuster.getAdjustedBitrateBps()) {
				updateBitrate();
			}	
			encodedImage.setCaptureTimeNs(timestampNs);
			encodeCallback.onEncodedFrame(encodedImage, codecInfo, naluIndices);
		});
		return VideoCodecStatus.OK;
	}

	@Override
	public VideoCodecStatus release() {
		return VideoCodecStatus.OK;
	}

	@Override
	public VideoCodecStatus encode(VideoFrame videoFrame, EncodeInfo info) {
		if (info != null) {
			logger.debug("encode video frame type: {}", info.frameTypes[0]);
		}

		return VideoCodecStatus.OK;
	}

	private void updateBitrate() {
		adjustedBitrate = bitrateAdjuster.getAdjustedBitrateBps();
	}

	public int getAdjustedBitrate() {
		return adjustedBitrate;
	}


	//TODO: This method is removed in m79. Which method is available for this?
	//@Override
	public VideoCodecStatus setChannelParameters(short packetLoss, long roundTripTimeMs) {

		packetLossMeasurements.add(packetLoss);
		if(packetLossMeasurements.size() > windowSize) {
			packetLossMeasurements.poll();
		}

		int total = 0;
		for (int msrmnt : packetLossMeasurements) {
			total += msrmnt;
		}		
		packetLossAverage = total/packetLossMeasurements.size();


		rttMeasurements.add(roundTripTimeMs);
		if(rttMeasurements.size() > windowSize) {
			rttMeasurements.poll();
		}

		total = 0;
		for (long msrmnt : rttMeasurements) {
			total += msrmnt;
		}
		rttMeasurementsAverage = total/rttMeasurements.size();

		if (time2LogChannel > 50 ) {
			logger.info("Channel parameters. packetloss:{}  roundTripTimeMs:{}", packetLoss, roundTripTimeMs);
			time2LogChannel = 0;
		}
		time2LogChannel++;
		return VideoCodecStatus.OK; // No op.
	}

	@Override
	public VideoCodecStatus setRateAllocation(BitrateAllocation bitrateAllocation, int framerate) {
		if (framerate > MAX_VIDEO_FRAMERATE) {
			framerate = MAX_VIDEO_FRAMERATE;
		}
		if (time2Log > 50) 
		{
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

	public void setEncodedFrameBuffer(ByteBuffer encodedFrameBuffer, boolean isKeyFrame, long timestampNs, int frameRotation, List<NaluIndex> naluIndices, String trackId ) {
		sendVideo.sendVideo(encodedFrameBuffer, isKeyFrame, timestampNs, frameRotation, naluIndices, trackId);
	}

	public int getPacketLossAverage() {
		return packetLossAverage;
	}

	public int getRttMeasurementsAverage() {
		return rttMeasurementsAverage;
	}

	public IVideoPacketSender getSendVideo() {
		return sendVideo;
	}

	public void setSendVideo(IVideoPacketSender sendVideo) {
		this.sendVideo = sendVideo;
	}
	
	@Override
	public void notifyFrameId(int frameid, long captureTimeNs) {
		
	}

}
