package io.antmedia.webrtctest;

import static org.bytedeco.javacpp.avcodec.av_bsf_alloc;
import static org.bytedeco.javacpp.avcodec.av_bsf_get_by_name;
import static org.bytedeco.javacpp.avcodec.av_bsf_init;
import static org.bytedeco.javacpp.avcodec.av_bsf_receive_packet;
import static org.bytedeco.javacpp.avcodec.av_bsf_send_packet;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_copy;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_OPUS;
import static org.bytedeco.javacpp.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.av_dump_format;
import static org.bytedeco.javacpp.avformat.av_find_input_format;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;

import static org.bytedeco.javacpp.avutil.av_rescale_q;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.bytedeco.javacpp.avcodec.AVBSFContext;
import org.bytedeco.javacpp.avcodec.AVBitStreamFilter;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVRational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.webrtc.VideoCodec;

public class FileReader{
	AVFormatContext inputContext = new AVFormatContext(null);
	private int videoIndex = -1;
	private int audioIndex = -1;	
	private String fileName;
	ArrayList<Frame> videoFrames = new ArrayList();
	ArrayList<Frame> audioFrames = new ArrayList();
	protected int fps = 24;
	WebRTCManager manager;
	private Logger logger = LoggerFactory.getLogger(FileReader.class);


	byte[] extradata;
	private boolean started = false;
	private AVBSFContext bsfContext;
	private AVRational videoTimebase;
	private AVRational timeBaseForMS;
	private AVRational audioTimebase;
	public int width;
	public int height;
	
	private Settings settings;
		
	public FileReader(Settings settings) 
	{
		this.fileName = settings.streamSource; 
		this.settings = settings;
	}

	public boolean init()
	{
		av_register_all();
		int ret;
		String fileExtension = fileName.split("\\.")[1];
		
		if ((ret = avformat_open_input(inputContext, fileName, av_find_input_format(fileExtension), (AVDictionary) null)) < 0) {
			System.err.println("Could not open input file.");
			close();
			return false;
		}

		if ((ret = avformat_find_stream_info(inputContext, (AVDictionary)null)) < 0) {
			System.err.println("Failed to retrieve input stream information");
			close();
			return false;
		}

		for(int i=0; i<inputContext.nb_streams(); i++) 
			if(inputContext.streams(i).codec().codec_type() == AVMEDIA_TYPE_VIDEO){
				videoIndex = i;

				int extradatasize = inputContext.streams(i).codec().extradata_size(); 

				if(extradatasize > 0) {
					extradata = new byte[extradatasize];
					inputContext.streams(i).codec().extradata().get(extradata);
				}
				
				this.width = inputContext.streams(i).codec().width();
				this.height = inputContext.streams(i).codec().height();
				
				videoTimebase = inputContext.streams(videoIndex).time_base();
				break;
			}

		for(int i=0; i<inputContext.nb_streams(); i++) 
			if(inputContext.streams(i).codec().codec_type() == AVMEDIA_TYPE_AUDIO){
				audioIndex = i;
				break;
			}
		av_dump_format(inputContext, 0, fileName, 0);

		if(videoIndex == -1) {
			logger.error("Media not contains video!");
			settings.audioOnly  = true;
		}

		timeBaseForMS = new AVRational();
		timeBaseForMS.num(1);
		timeBaseForMS.den(1000);

		if(!settings.audioOnly && settings.codec == VideoCodec.H264) {
			initVideoBSF();
		}

		if(audioIndex != -1) {
			audioTimebase = inputContext.streams(audioIndex).time_base();

			if(inputContext.streams(audioIndex).codec().codec_id() != AV_CODEC_ID_OPUS) {
				logger.warn("\nAudio codec is not opus which is required for this tool. You can convert by:\n"
						+ "\t ffmpeg -i input.mp4 -vcodec copy -acodec libopus output.mp4");
			}
		}
		
		return true;
	}

	private void initVideoBSF() {
		AVBitStreamFilter h264bsfc = av_bsf_get_by_name("h264_mp4toannexb");
		bsfContext = new AVBSFContext(null);

		av_bsf_alloc(h264bsfc, bsfContext);
		AVCodecParameters codecpar = inputContext.streams(videoIndex).codecpar();
		avcodec_parameters_copy(bsfContext.par_in(), codecpar );
		videoTimebase = inputContext.streams(videoIndex).time_base();
		
		bsfContext.time_base_in(videoTimebase);
		av_bsf_init(bsfContext);
		videoTimebase = bsfContext.time_base_out();
	}

	public void start() {
		if(started) {
			return;
		}
		started  = true;
		new Thread() {
			@Override
			public void run() {
				startReaderThread();
				close();
				super.run();
			}
		}.start();
	}

	private void startReaderThread() {
		boolean read = true;
		while (read) {
			AVPacket pkt = new AVPacket();
			int ret = av_read_frame(inputContext, pkt);
			read = (ret >= 0);

			if (pkt.stream_index() == videoIndex) {
				if(settings.codec == VideoCodec.H264) {
					av_bsf_send_packet(bsfContext, pkt);

					while (av_bsf_receive_packet(bsfContext, pkt) == 0) 
					{
						ByteBuffer data = ByteBuffer.allocateDirect(pkt.size());
						data.put(pkt.data().position(0).limit(pkt.size()).asByteBuffer());
						long timeStamp = av_rescale_q(pkt.pts(), videoTimebase, timeBaseForMS);
						videoFrames.add(new Frame(data, timeStamp, (pkt.flags() & AV_PKT_FLAG_KEY)==1));
					}
				}
				else {
					ByteBuffer data = ByteBuffer.allocateDirect(pkt.size());
					if(pkt.size() > 0) {
						data.put(pkt.data().position(0).limit(pkt.size()).asByteBuffer());
						long timeStamp = av_rescale_q(pkt.pts(), videoTimebase, timeBaseForMS);
						videoFrames.add(new Frame(data, timeStamp, (pkt.flags() & AV_PKT_FLAG_KEY)==1));
					}
				}
			}
			else if (pkt.stream_index() == audioIndex) {
				ByteBuffer data = ByteBuffer.allocateDirect(pkt.size());
				if(pkt.size() > 0) {
					data.put(pkt.data().position(0).limit(pkt.size()).asByteBuffer());
					long timeStamp = av_rescale_q(pkt.pts(), audioTimebase, timeBaseForMS);
					audioFrames.add(new Frame(data, timeStamp, false));
				}
			}
		}
	}

	void close()
	{
		/* close input */
		if (inputContext != null && ((inputContext.flags() & AVFMT_NOFILE) == 0))
		{
			avformat_close_input(inputContext);
			avformat_free_context(inputContext);
		}
	}

	public void setManager(WebRTCManager manager) {
		this.manager = manager;
	}

	class Frame {
		ByteBuffer data;
		long timeStamp;
		boolean isKeyFrame;

		public Frame(ByteBuffer data, long timeStamp, boolean isKeyFrame) {
			this.data = data;
			this.timeStamp = timeStamp;
			this.isKeyFrame = isKeyFrame;
		}
	}
}
