package io.antmedia.webrtctest;

import java.util.HashMap;

import org.webrtc.H264Utils;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoCodecType;
import org.webrtc.VideoDecoder;
import org.webrtc.VideoDecoderFactory;

public class VirtualVideoDecoderFactory implements VideoDecoderFactory{

	private VirtualH264Decoder decoder = new VirtualH264Decoder();

	@Override
	public VideoDecoder createDecoder(VideoCodecInfo info) {
		return decoder;
	}

	@Override
	public VideoCodecInfo[] getSupportedCodecs() {
		VideoCodecInfo[] codecInfo = new VideoCodecInfo[2];
		codecInfo[0] = new VideoCodecInfo(VideoCodecType.H264.name(), H264Utils.getDefaultH264Params(false));
		codecInfo[1] = new VideoCodecInfo(VideoCodecType.H265.name(), new HashMap<>());
        return codecInfo;
	}

	public VirtualH264Decoder getDecoder() {
		return decoder;
	}

}
