package io.antmedia.webrtctest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CapturerObserver;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.Observer;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.WebRtcAudioRecord;
import org.webrtc.audio.WebRtcAudioTrack;

import io.antmedia.enterprise.webrtc.codec.VirtualH264Encoder;
import io.antmedia.enterprise.webrtc.codec.VirtualVideoEncoderFactory;
import io.antmedia.webrtc.api.IAudioRecordListener;
import io.antmedia.webrtc.api.IAudioTrackListener;


public class WebRTCManager implements Observer, SdpObserver {
	private Logger logger = LoggerFactory.getLogger(WebRTCManager.class);
	String stunServerUri = "stun:stun.l.google.com:19302";
	private PeerConnectionFactory peerConnectionFactory;
	private PeerConnection peerConnection;

	private String streamId;
	private WebRtcAudioTrack webRtcAudioTrack;
	private WebsocketClientEndpoint websocket;
	private MediaConstraints audioConstraints;
	private MediaConstraints sdpMediaConstraints;
	private StreamManager streamManager;

	private VirtualVideoEncoderFactory encoderFactory;
	private CapturerObserver capturerObserver;
	private WebRtcAudioRecord audioRecord;
	private VirtualVideoDecoderFactory decoderFactory;
	private IWebRTCEventListerner listener;

	private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
	private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
	private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
	private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
	private static final String FALSE = "false";
	public static final String VIDEO_TRACK_ID = "ARDAMSv0";

	public static final String AUDIO_TRACK_ID = "ARDAMSa0";


	public WebRTCManager(String streamId) 
	{
		this.setStreamId(streamId);
		String unsecure = "ws://"+Settings.instance.webSockAdr+":"+Settings.instance.port+"/WebRTCAppEE/websocket";
		String secure = "wss://"+Settings.instance.webSockAdr+":"+Settings.instance.port+"/WebRTCAppEE/websocket";
		

		URI uri = null;

		try {
			uri = new URI(Settings.instance.isSequre ? secure : unsecure);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		System.out.println(uri);

		
		websocket = new WebsocketClientEndpoint(uri);
		websocket.setManager(this);
	}

	private void initPeerConnection() {


		createMediaConstraintsInternal();
		peerConnectionFactory = createPeerConnectionFactory();

		List<IceServer> iceServers = new ArrayList<>();
		iceServers.add(IceServer.builder(stunServerUri).createIceServer());
		PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);

		rtcConfig.enableDtlsSrtp = true;

		//rtcConfig.minPort = 5000;
		//rtcConfig.maxPort = 65000;
		//rtcConfig.tcpCandidatePolicy = TcpCandidatePolicy.ENABLED; 

		peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, WebRTCManager.this);


		//why ARDAMS is used
		List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");

		//instantiate video source
		VideoSource videoSource = peerConnectionFactory.createVideoSource(false);
		capturerObserver = videoSource.getCapturerObserver();

		//by normal app, video capturer started immediately after creating creating peer connection


		/**
		 *  It seems that we do not need custom video capturer technically
		 *  because video capturer call capturerObserver's below functions.
		 *  We can call below functions in WebRTCClient. 
		 *  
		 *  capturerObserver.onCapturerStarted(success)
		 *  capturerObserver.onCapturerStopped();
		 *  capturerObserver.onFrameCaptured(frame);
		 */

		VideoTrack videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);

		RtpSender videoTrackSender = peerConnection.addTrack(videoTrack, mediaStreamLabels);

		createMediaConstraintsInternal();

		AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
		AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

		RtpSender audioTrackSender = peerConnection.addTrack(localAudioTrack, mediaStreamLabels);

		capturerObserver = videoSource.getCapturerObserver();

	}

	public void setRemoteDescription(SessionDescription sdp) {
		System.out.println("WebRTCManager.setRemoteDescription()");
		if (peerConnection != null) {
			peerConnection.setRemoteDescription(WebRTCManager.this, sdp);

			if(sdp.type == Type.OFFER) {
				peerConnection.createAnswer(this, sdpMediaConstraints);
			}
		}
		else {
			logger.warn("Peer connection is null. It cannot add ice candidate for stream Id {}", getStreamId());
		}

	}


	public PeerConnectionFactory createPeerConnectionFactory(){
		PeerConnectionFactory.initialize(
				PeerConnectionFactory.InitializationOptions.builder(null)
				.createInitializationOptions());

		encoderFactory = new VirtualVideoEncoderFactory();

		decoderFactory = new VirtualVideoDecoderFactory(); 

		PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
		options.disableNetworkMonitor = true;


		JavaAudioDeviceModule adm;

		if(Settings.instance.mode == Mode.PUBLISHER) {
			adm = (JavaAudioDeviceModule)JavaAudioDeviceModule.builder(null)
					.setUseHardwareAcousticEchoCanceler(false)
					.setUseHardwareNoiseSuppressor(false)
					.setAudioRecordErrorCallback(null)
					.setAudioTrackErrorCallback(null)
					.setAudioRecordListener(new IAudioRecordListener() {

						@Override
						public void audioRecordStoppped() {
						}

						@Override
						public void audioRecordStarted() {
						}
					})
					.createAudioDeviceModule();

			audioRecord = adm.getAudioRecord();
		}
		else {
			// in receiving stream only Audio Track should be enabled
			// in sending stream only AudioRecord should be enabled 
			adm = (JavaAudioDeviceModule)
					JavaAudioDeviceModule.builder(null)
					.setUseHardwareAcousticEchoCanceler(false)
					.setUseHardwareNoiseSuppressor(false)
					.setAudioRecordErrorCallback(null)
					.setAudioTrackErrorCallback(null)
					.setAudioTrackListener(new IAudioTrackListener() {
						public void playoutStarted() {
							logger.info("starting playout for stream {}", streamId);
						}

						public void playoutStopped() {
							logger.info("stopping playout for stream {}", streamId);

						}
					})
					.createAudioDeviceModule();

			webRtcAudioTrack = adm.getAudioTrack();
		}

		return  PeerConnectionFactory.builder()
				.setOptions(options)
				.setAudioDeviceModule(adm)
				.setVideoEncoderFactory(encoderFactory)
				.setVideoDecoderFactory(decoderFactory)
				.createPeerConnectionFactory();
	}

	public void createOffer() {
		peerConnection.createOffer(WebRTCManager.this, sdpMediaConstraints);
	}

	private void createMediaConstraintsInternal() {

		audioConstraints = new MediaConstraints();
		audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, FALSE));
		audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, FALSE));
		audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, FALSE));
		audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, FALSE));


		// Create SDP constraints.
		sdpMediaConstraints = new MediaConstraints();
		sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")); 
		sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")); 
	}

	private void initAudioTrackExecutor() {

		/*
		audioEncoderExecutorFuture = audioEncoderExecutor.scheduleWithFixedDelay(this::encodeAudioRunner, 5, 10, TimeUnit.MILLISECONDS);
		audioTrackFuture = signallingExecutor.scheduleAtFixedRate(new Runnable() {

			private int audioFrameLogCounter = 0;
			// number of frames * number of bytes * number of channels * 2(20ms data)
			private long lasttAudioTimeStamp = 0;
			private long totalAudioFrameInterval = 0;

			public void run() {
				if (isStopped.get()) {
					logger.info("renderFrame returning because encoder adaptor is stopped for {}", streamId);
					return;
				}

				audioFrameLogCounter++;
				if (!ready.get()) {
					if (audioFrameLogCounter % 50 == 0) {
						//each frame is 10 ms and prints log every 500ms
						logger.info("Waiting for encoder to be ready for {}", streamId);
					}
					return;
				}

				if (firstPacketReceivedTime == 0) {
					firstPacketReceivedTime = System.currentTimeMillis();
				}

				audioFramePending.incrementAndGet();

				if (audioFrameLogCounter % 300 == 0) {
					logger.info("Pending audio frames: {}, audio thread entrance interval: {}ms, total received audio frames: {}"
							,audioFramePending, (float)totalAudioFrameInterval/receivedAudioFrameCount, receivedAudioFrameCount);
					audioFrameLogCounter=0;

				}

				long now = System.currentTimeMillis();
				long timestampMS = (now - firstPacketReceivedTime);

				totalAudioFrameInterval += lasttAudioTimeStamp != 0 ? now - lasttAudioTimeStamp : 0;

				receivedAudioFrameCount++;

				lasttAudioTimeStamp = now;

				ByteBuffer playoutData = webRtcAudioTrack.getPlayoutData();
				int readSizeInBytes = webRtcAudioTrack.getReadSizeInBytes();
				byte[] audioData = new byte[readSizeInBytes];

				int numberOfFrames = readSizeInBytes/webRtcAudioTrack.getBytesPerSample();

				playoutData.get(audioData, 0, audioData.length);

				audioFrameQueue.offer(new AudioFrameContext(audioData, timestampMS, numberOfFrames, webRtcAudioTrack.getChannels(), webRtcAudioTrack.getSampleRate()));

			}

		}, 5, 10, TimeUnit.MILLISECONDS);


		 */
	}

	public void addIceCandidate(IceCandidate iceCandidate) {
		if (!peerConnection.addIceCandidate(iceCandidate)) 
		{
			logger.error("Cannot add ice candidate({}) for stream {}", iceCandidate, getStreamId());
		}	
	}

	public CapturerObserver getVideoObserver() {
		return capturerObserver;
	}

	public void stop() {
		peerConnection.close();

	}

	public VirtualH264Encoder getEncoder() {
		return encoderFactory.getEncoder();
	}

	@Override
	public void onCreateSuccess(SessionDescription sdp) {
		logger.info("onCreateSuccess for {}", getStreamId());
		logger.info(sdp.description);

		SdpObserver dummy = new SdpObserver() {
			@Override
			public void onSetSuccess() {
				logger.info("local SDP is set");
			}
			@Override
			public void onSetFailure(String error) {
				logger.error("Cannot set local description for {}", getStreamId());
			}
			@Override
			public void onCreateSuccess(SessionDescription sdp) {}
			@Override
			public void onCreateFailure(String error) {}
		};

		if (sdp.type == Type.ANSWER) {
			websocket.sendSDPConfiguration(sdp.description, "answer", getStreamId());
			peerConnection.setLocalDescription(dummy, sdp);
		}
		else  {
			websocket.sendSDPConfiguration(sdp.description, "offer", getStreamId());
			peerConnection.setLocalDescription(dummy, sdp);
		}
	}

	@Override
	public void onSetSuccess() {
		logger.info("onSetSuccess for {}", getStreamId());
	}

	@Override
	public void onCreateFailure(String error) {
		logger.info("onCreateFailure: {} " , error);		
	}

	@Override
	public void onSetFailure(String error) {
		logger.info("onSetFailure: {} " , error);		
	}

	@Override
	public void onSignalingChange(SignalingState newState) {
		logger.info("onSignalingChange new state: {}" , newState);		
	}

	@Override
	public void onIceConnectionChange(IceConnectionState newState) {
		logger.info("onIceConnectionChange {}" , newState);
		if (newState == IceConnectionState.CONNECTED) {
			if(Settings.instance.mode == Mode.PLAYER) {
				streamManager.start();
				listener.onCompleted();
			}
		}
		else if (newState == IceConnectionState.COMPLETED) {
			if(Settings.instance.mode == Mode.PUBLISHER) {
				streamManager.start();
				listener.onCompleted();
			}
		}
		else if (newState == IceConnectionState.DISCONNECTED || newState == IceConnectionState.FAILED
				|| newState == IceConnectionState.CLOSED) 
		{
			streamManager.stop();
			stop();
		}

	}

	@Override
	public void onIceConnectionReceivingChange(boolean receiving) {
		logger.info("onIceConnectionReceivingChange new state: {}" , receiving);
	}

	@Override
	public void onIceGatheringChange(IceGatheringState newState) {
		logger.info("onIceGatheringChange new state: {}" , newState);
	}

	@Override
	public void onIceCandidate(IceCandidate candidate) {
		logger.info("onIceCandidate candidate: {}" , candidate);

		websocket.sendTakeCandidateMessage(candidate.sdpMLineIndex,	candidate.sdpMid, candidate.sdp, getStreamId());
	}

	@Override
	public void onIceCandidatesRemoved(IceCandidate[] candidates) {
		logger.info("onIceCandidatesRemoved: {}" , candidates);
	}

	@Override
	public void onAddStream(MediaStream stream) {
		logger.info("onAddStream for streamId {}", getStreamId());

		if (!stream.videoTracks.isEmpty()) {
			VideoTrack videoTrack = stream.videoTracks.get(0);
			if (videoTrack != null) {
				/*
				 * no need to video sink because we get data with decoder
				videoTrack.addSink(new VideoSink() {

					@Override
					public void onFrame(VideoFrame frame) {
						System.out.println("WebRTCManager.onAddStream(...).new VideoSink() {...}.onFrame()");
					}
				});
				 */
			}
		}
	}

	@Override
	public void onRemoveStream(MediaStream stream) {
		logger.info("onRemoveStream for stream Id {}", getStreamId());		
	}

	@Override
	public void onDataChannel(DataChannel dataChannel) {
		logger.info("onDataChannel for stream Id {}", getStreamId());
	}

	@Override
	public void onRenegotiationNeeded() {
		logger.info("onRenegotiationNeeded for stream Id {}", getStreamId());
	}

	@Override
	public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
		logger.info("onAddTrack for streamId {}", getStreamId());
	}

	public void setStreamManager(StreamManager streamManager) {
		this.streamManager = streamManager;		
	}

	public WebRtcAudioRecord getAudioRecord() {
		return audioRecord;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public void start() {
		initPeerConnection();

		websocket.connect();
	}

	public VirtualH264Decoder getDecoder() {
		return decoderFactory.getDecoder();
	}

	public WebRtcAudioTrack getAudioTrack() {
		return webRtcAudioTrack;
	}

	public void setListener(IWebRTCEventListerner listener) {
		this.listener = listener;

	}
	
	public StreamManager getStreamManager() {
		return streamManager;
	}

}
