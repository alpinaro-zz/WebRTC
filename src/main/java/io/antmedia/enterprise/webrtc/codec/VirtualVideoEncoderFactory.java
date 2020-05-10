package io.antmedia.enterprise.webrtc.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.H264Utils;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoCodecType;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

import io.antmedia.webrtctest.Mode;
import io.antmedia.webrtctest.Settings;

public class VirtualVideoEncoderFactory implements VideoEncoderFactory {

	
	protected static Logger logger = LoggerFactory.getLogger(VirtualVideoEncoderFactory.class);
	
	private VirtualVideoEncoder virtualH264Encoder = new VirtualVideoEncoder();
	List<VideoCodecInfo> supportedCodecInfos = new ArrayList<>();

	public VirtualVideoEncoderFactory(boolean h264, boolean vp8, boolean h265) {
		// Generate a list of supported codecs in order of preference:
		// we support h264 baseline for encoding because it is the one that is supported by all browsers

		// Available encoders can be VP8, VP9, H264 (high profile), and H264 (baseline profile).

		if(h264) {
			supportedCodecInfos.add(new VideoCodecInfo(VideoCodecType.H264.name(), H264Utils.getDefaultH264Params(false)));
		}
		if(vp8) {
			supportedCodecInfos.add(new VideoCodecInfo(VideoCodecType.VP8.name(), new HashMap<>()));
		}
		if (h265) {
			supportedCodecInfos.add(new VideoCodecInfo(VideoCodecType.H265.name(), new HashMap<>()));
		}
	}

	public VirtualVideoEncoder getEncoder() {
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
		
		// Generate a list of supported codecs in order of preference:
		// we support h264 baseline for encoding because it is the one that is supported by all browsers

		// Available encoders can be VP8, VP9, H264 (high profile), and H264 (baseline profile).

		return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
	}

}
