package io.antmedia.enterprise.webrtc.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.H264Utils;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoCodecType;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

public class VirtualVideoEncoderFactory implements VideoEncoderFactory {

	
	protected static Logger logger = LoggerFactory.getLogger(VirtualVideoEncoderFactory.class);
	
	private VirtualH264Encoder virtualH264Encoder = new VirtualH264Encoder();

	public VirtualH264Encoder getEncoder() {
		return this.virtualH264Encoder;
	}
	
	@Override
	public VideoEncoder createEncoder(VideoCodecInfo input) {
		logger.debug("createEncoder: {} for factory: {}" , input.name, this.hashCode());
		for (String key : input.params.keySet()) {
			logger.debug("encoder input key: {}  value: {} ", key, input.params.get(key));
		} 
		//VideoCodecType type = VideoCodecType.valueOf(input.name)
		/*
		 * Codec settings for H264 
		 * Key frame interval is 20 
		 * Forced key frame interval is 0
		 * 
		 */
 		return virtualH264Encoder;
	}

	@Override
	public VideoCodecInfo[] getSupportedCodecs() {
		
		List<VideoCodecInfo> supportedCodecInfos = new ArrayList<>();
		// Generate a list of supported codecs in order of preference:
		// we support h264 baseline for encoding because it is the one that is supported by all browsers

		// Available encoders can be VP8, VP9, H264 (high profile), and H264 (baseline profile).

		VideoCodecType h264 = VideoCodecType.H264;
		if (logger.isInfoEnabled()) {
			logger.info("getSupportedCodecs: {} encoder factory: {}" , h264.name(), this);
		}
		supportedCodecInfos.add(new VideoCodecInfo(h264.name(), H264Utils.getDefaultH264Params(false)));
		return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
	}

}
