package io.antmedia.webrtctest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.client.ClientManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;

import io.antmedia.websocket.WebSocketConstants;


@ClientEndpoint
public class WebsocketClientEndpoint {

	private static Logger logger = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
	private JSONParser jsonParser = new JSONParser();
	WebRTCManager webrtcManager;
	private Session session;
	private URI uri;
	private ClientManager websocketClient;
	private Settings settings;

	public WebsocketClientEndpoint(Settings settings) {
		String unsecure = "ws://"+settings.webSockAdr+":"+settings.port+"/WebRTCAppEE/websocket";
		String secure = "wss://"+settings.webSockAdr+":"+settings.port+"/WebRTCAppEE/websocket";
		try {
			uri = new URI(settings.isSequre ? secure : unsecure);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		this.settings = settings;
	}

	public void connect() {
		try {
			websocketClient = (ClientManager) ContainerProvider.getWebSocketContainer();
			websocketClient.asyncConnectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		websocketClient.shutdown();
	}
	
	
	@OnOpen
	public void onOpen(Session session, EndpointConfig config)
	{
		logger.info("websocket opened {}", this.hashCode());
		this.session = session;
		
		if (webrtcManager != null) {
			webrtcManager.webSocketOpened();
		}
	}

	@OnClose
	public void onClose(Session session) {
		logger.info("websocket closed {}", this.hashCode());
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		logger.info("websocket onError {}", this.hashCode());
	}

	@OnMessage
	public void onMessage(Session session, String message) {
		try {

			if (message == null) {
				logger.error("Received message null for session id: {}" , session.getId());
				return;
			}

			JSONObject jsonObject = (JSONObject) jsonParser.parse(message);

			String cmd = (String) jsonObject.get(WebSocketConstants.COMMAND);
			if (cmd == null) {
				logger.error("Received message does not contain any command for session id: {}" , session.getId());
				return;
			}

			final String streamId = (String) jsonObject.get(WebSocketConstants.STREAM_ID);
			
			if ((streamId == null || streamId.isEmpty()) &&
					!cmd.equals(WebSocketConstants.PONG_COMMAND)) 
			{
				logger.error("Incoming message:{}" , message);
				//sendNoStreamIdSpecifiedError();
				return;
			}
			
			if (cmd.equals(WebSocketConstants.PONG_COMMAND)) {
				webrtcManager.pongMessageReceived();
			}
			
			if (cmd.equals(WebSocketConstants.START_COMMAND))  
			{
				webrtcManager.createOffer();
			}
			else if (cmd.equals(WebSocketConstants.TAKE_CONFIGURATION_COMMAND))  
			{
				processTakeConfigurationCommand(jsonObject, session.getId(), streamId);
			}
			else if (cmd.equals(WebSocketConstants.TAKE_CANDIDATE_COMMAND)) 
			{
				processTakeCandidateCommand(jsonObject, session.getId(), streamId);
			}
			else if (cmd.equals(WebSocketConstants.STOP_COMMAND)) {
				
			}
			else if (cmd.equals(WebSocketConstants.PLAY_FINISHED)) {
				logger.info("play finished received from websocket {}", this.hashCode());
				webrtcManager.stop();
			}
			else if (cmd.equals(WebSocketConstants.ERROR_COMMAND)) {
				logger.error("Incoming message:{}" , message);
			}
			else if (cmd.equals(WebSocketConstants.NOTIFICATION_COMMAND)) {
				
				String definition = (String) jsonObject.get(WebSocketConstants.DEFINITION);
				if (definition.equals(WebSocketConstants.JOINED_THE_ROOM)) {
					webrtcManager.joinedTheRoom();
				}
			}
			else if (cmd.equals(WebSocketConstants.STREAM_INFORMATION_NOTIFICATION)) {
			}
			else if (cmd.equals(WebSocketConstants.PUBLISH_STARTED)) {
			}
			else if (cmd.equals(WebSocketConstants.PONG_COMMAND)) {
			}
			else {
				logger.info("Undefined incoming message:{} ", message);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	@SuppressWarnings("unchecked")
	public  void sendSDPConfiguration(String description, String type, String streamId) {

		sendMessage(getSDPConfigurationJSON (description, type,  streamId).toJSONString());
	}

	@SuppressWarnings("unchecked")
	public  void sendPublishStartedMessage(String streamId) {

		JSONObject jsonObj = new JSONObject();
		jsonObj.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObj.put(WebSocketConstants.DEFINITION, WebSocketConstants.PUBLISH_STARTED);
		jsonObj.put(WebSocketConstants.STREAM_ID, streamId);

		sendMessage(jsonObj.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public  void sendPublishFinishedMessage(String streamId) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObject.put(WebSocketConstants.DEFINITION,  WebSocketConstants.PUBLISH_FINISHED);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);

		sendMessage(jsonObject.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public  void sendStartMessage(String streamId) 
	{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.START_COMMAND);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);

		sendMessage(jsonObject.toJSONString());
	}



	@SuppressWarnings("unchecked")
	protected  final  void sendNoStreamIdSpecifiedError()  {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_ID_SPECIFIED);
		sendMessage(jsonResponse.toJSONString());	
	}

	@SuppressWarnings("unchecked")
	protected  final  void sendPlay(String streamId)  {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.PLAY_COMMAND);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
		sendMessage(jsonResponse.toJSONString());	
	}

	@SuppressWarnings("unchecked")
	protected  final  void sendPublish(String streamId)  {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
		jsonResponse.put(WebSocketConstants.VIDEO, !settings.audioOnly);
		jsonResponse.put(WebSocketConstants.AUDIO, true);
		if(settings.mainTrack != null) {
			jsonResponse.put(WebSocketConstants.MAIN_TRACK, settings.mainTrack);
		}
		sendMessage(jsonResponse.toJSONString());	
	}

	@SuppressWarnings("unchecked")
	public void sendTakeCandidateMessage(long sdpMLineIndex, String sdpMid, String sdp, String streamId)
	{
		sendMessage(getTakeCandidateJSON(sdpMLineIndex, sdpMid, sdp, streamId).toJSONString());
	}


	@SuppressWarnings("unchecked")
	public void sendMessage(String message) {
		synchronized (this) {
			if (session.isOpen()) {
				try {
					session.getBasicRemote().sendText(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}


	public static JSONObject getTakeCandidateJSON(long sdpMLineIndex, String sdpMid, String sdp, String streamId) {

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND,  WebSocketConstants.TAKE_CANDIDATE_COMMAND);
		jsonObject.put(WebSocketConstants.CANDIDATE_LABEL, sdpMLineIndex);
		jsonObject.put(WebSocketConstants.CANDIDATE_ID, sdpMid);
		jsonObject.put(WebSocketConstants.CANDIDATE_SDP, sdp);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);

		return jsonObject;
	}

	public static JSONObject getSDPConfigurationJSON(String description, String type, String streamId) {

		JSONObject jsonResponseObject = new JSONObject();
		jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
		jsonResponseObject.put(WebSocketConstants.SDP, description);
		jsonResponseObject.put(WebSocketConstants.TYPE, type);
		jsonResponseObject.put(WebSocketConstants.STREAM_ID, streamId);

		return jsonResponseObject;
	}

	private void processTakeConfigurationCommand(JSONObject jsonObject, String sessionId, String streamId) {
		String typeString = (String)jsonObject.get(WebSocketConstants.TYPE);
		String sdpDescription = (String)jsonObject.get(WebSocketConstants.SDP);

		SessionDescription.Type type;
		if (typeString.equals("offer")) {
			type = Type.OFFER;
		}
		else {
			type = Type.ANSWER;
		}


		SessionDescription sdp = new SessionDescription(type, sdpDescription);
		webrtcManager.setRemoteDescription(sdp);
	}

	private void processTakeCandidateCommand(JSONObject jsonObject, String sessionId, String streamId) {
		String sdpMid = (String) jsonObject.get(WebSocketConstants.CANDIDATE_ID);
		String sdp = (String) jsonObject.get(WebSocketConstants.CANDIDATE_SDP);
		long sdpMLineIndex = (long)jsonObject.get(WebSocketConstants.CANDIDATE_LABEL);

		IceCandidate iceCandidate = new IceCandidate(sdpMid, (int)sdpMLineIndex, sdp);
		webrtcManager.addIceCandidate(iceCandidate);

	}

	public void setManager(WebRTCManager manager) {
		this.webrtcManager = manager;
	}

	public void sendPingMessage() {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.PING_COMMAND);
		sendMessage(jsonResponse.toJSONString());	
	}

	public void sendJoinTheRoom(String streamId, String roomId, String multiTrack) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.JOIN_ROOM_COMMAND);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
		jsonResponse.put(WebSocketConstants.ROOM, roomId);
		jsonResponse.put(WebSocketConstants.MODE, multiTrack);
		sendMessage(jsonResponse.toJSONString());	
	}

}