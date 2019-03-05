package io.antmedia.webrtctest;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.javacpp.avcodec.av_init_packet;
import static org.bytedeco.javacpp.avcodec.av_packet_rescale_ts;
import static org.bytedeco.javacpp.avcodec.avcodec_alloc_context3;
import static org.bytedeco.javacpp.avcodec.avcodec_find_decoder;
import static org.bytedeco.javacpp.avcodec.avcodec_open2;
import static org.bytedeco.javacpp.avcodec.avcodec_receive_frame;
import static org.bytedeco.javacpp.avcodec.avcodec_register_all;
import static org.bytedeco.javacpp.avcodec.avcodec_send_packet;
import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avutil.AVERROR_EOF;
import static org.bytedeco.javacpp.avutil.AV_LOG_VERBOSE;
import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.javacpp.avutil.av_frame_alloc;
import static org.bytedeco.javacpp.avutil.av_log_set_level;
import static org.bytedeco.javacpp.presets.avutil.AVERROR_EAGAIN;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.EncodedImage;

public class H264Player {

	private AVCodecContext codecContext;
	private AVFrame decodedFrame;
	private JPanel panel;
	private byte[] dataY;
	private byte[] dataU;
	private byte[] dataV;
	private int width = 640;
	private int height = 360;
	private BufferedImage image;
	private JFrame jFrame;
	private Logger logger = LoggerFactory.getLogger(H264Player.class);


	public H264Player() {
		
		jFrame = new JFrame();
		panel = new JPanel() {
			protected void paintComponent(java.awt.Graphics gr) {
				super.paintComponent(gr);
				gr.drawImage(image, 0, 0, null);
			}
		};
		jFrame.add(panel);
	}



	protected void draw() {
		if(dataY != null) {
			for(int y=0; y<height;y++) {
				for(int x=0; x<width;x++) {

					int Y = (dataY[y*width+x] & 0xFF) - 16;
					int U = (dataU[y/2*width/2+x/2] & 0xFF) - 128;
					int V = (dataV[y/2*width/2+x/2] & 0xFF) - 128;

					int r, g, b;


					r = (int) (Y + 1.370705 * V);
					g = (int) (Y - 0.698001 * U - 0.337633*V);
					b = (int) (Y + 1.732446 * U);

					// Clamp RGB values to [0,255]
					r = ( r > 255 ) ? 255 : ( r < 0 ) ? 0 : r;
					g = ( g > 255 ) ? 255 : ( g < 0 ) ? 0 : g;
					b = ( b > 255 ) ? 255 : ( b < 0 ) ? 0 : b;

					image.setRGB(x, y, new Color(r,g,b).getRGB());
				}
			}
		}

		panel.updateUI();

	}



	public void init(int width, int height) {
		this.width = width;
		this.height = height;
		av_log_set_level(AV_LOG_VERBOSE);

		jFrame.setVisible(true);
		jFrame.setSize(width, height);
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		int ret;
		AVDictionary dict = null;
		AVCodec codec;
		codec = avcodec_find_decoder(AV_CODEC_ID_H264);
		codecContext = avcodec_alloc_context3(codec);
		codecContext.width(width);
		codecContext.height(height);
		codecContext.pix_fmt(AV_PIX_FMT_YUV420P);
		ret = avcodec_open2(codecContext, codec, dict);
		if(ret < 0) {
			logger.error("cannot open decoder");
		}
		decodedFrame = new AVFrame();

		decodedFrame = av_frame_alloc();
	}

	public void play(EncodedImage frame) {
		AVPacket avpkt = new AVPacket();
		av_init_packet(avpkt);

		ByteBuffer bb = frame.buffer;
		byte[] data = new byte[bb.capacity()];
		bb.get(data);
		avpkt.data(new BytePointer(data));
		avpkt.size(data.length);

		codecContext.reordered_opaque(frame.captureTimeMs*1000);

		av_packet_rescale_ts(avpkt,
				codecContext.time_base(),
				codecContext.time_base()
				);

		int ret = avcodec_send_packet(codecContext, avpkt);
		if (ret < 0) {
			logger.error("Encoding error send");
		}

		ret = avcodec_receive_frame(codecContext, decodedFrame);

		if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF()) {
			//nothing it is normal for P,B frames 
		}
		else if (ret < 0) {
			logger.error("Encoding error receive: {}",ret);
		}
		else {
			dataY = new byte[decodedFrame.buf(0).size()];
			decodedFrame.buf(0).data().get(dataY);

			dataU = new byte[decodedFrame.buf(1).size()];
			decodedFrame.buf(1).data().get(dataU);

			dataV = new byte[decodedFrame.buf(2).size()];
			decodedFrame.buf(2).data().get(dataV);

			//TODO change here
			new  Thread() {
				public void run() {
					draw();
				}
			}.start();
		}
	}


}
