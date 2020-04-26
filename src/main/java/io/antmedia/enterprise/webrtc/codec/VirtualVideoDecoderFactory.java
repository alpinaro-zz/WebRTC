package io.antmedia.enterprise.webrtc.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.webrtc.H264Utils;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoCodecType;
import org.webrtc.VideoDecoder;
import org.webrtc.VideoDecoderFactory;

public class VirtualVideoDecoderFactory implements VideoDecoderFactory{

	private VirtualVideoDecoder decoder = new VirtualVideoDecoder();

	List<VideoCodecInfo> supportedCodecInfos = new ArrayList<>();

	public VirtualVideoDecoderFactory(boolean h264, boolean vp8) {
		if(h264) {
			supportedCodecInfos.add(new VideoCodecInfo(VideoCodecType.H264.name(), H264Utils.getDefaultH264Params(false)));
		}
		if(vp8) {
			supportedCodecInfos.add(new VideoCodecInfo(VideoCodecType.VP8.name(), new HashMap<>()));
		}
	}
	
	@Override
	public VideoDecoder createDecoder(VideoCodecInfo info) {
		return decoder;
	}

	@Override
	public VideoCodecInfo[] getSupportedCodecs() {
		return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
	}

	public VirtualVideoDecoder getDecoder() {
		return decoder;
	}

}
