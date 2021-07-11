package io.antmedia;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import io.antmedia.webrtctest.IWebRTCEventListerner;
import io.antmedia.webrtctest.FileReader;
import io.antmedia.webrtctest.Mode;
import io.antmedia.webrtctest.Settings;
import io.antmedia.webrtctest.StatManager;
import io.antmedia.webrtctest.StreamManager;
import io.antmedia.webrtctest.WebRTCManager;
import io.antmedia.webrtctest.WebRTCPlayer;
import io.antmedia.webrtctest.WebRTCPublisher;

public class Starter implements IWebRTCEventListerner
{

	private ArrayList<WebRTCManager> managers = new ArrayList<>();
	private FileReader reader;
	
	StatManager statManager;
    Settings settings = new Settings();
    
    private IWebRTCEventListerner listener;
	
	protected int startingIndex = 0;

	public Starter(String[] args) {
		System.out.println("~~~~~~~~ Start ("+hashCode()+") ~~~~~~~~");
		settings.parse(args);
		statManager = new StatManager(settings.kafkaBrokers);
		if(settings.mode == Mode.PUBLISHER || settings.mode == Mode.PARTICIPANT) {
			reader = new FileReader(settings);
			if(reader.init()) {
				reader.start();
			}
			else {
				System.exit(1);
			}
		}

		for (int i = 0; i < settings.load; i++) {
			String suffix = settings.mode == Mode.PUBLISHER && settings.load > 1 ? "-"+i : ""; 
			WebRTCManager webRTCManager = new WebRTCManager(settings.streamId+suffix, settings);

			StreamManager streamManager = null;
			if(settings.mode == Mode.PUBLISHER || settings.mode == Mode.PARTICIPANT) {
				streamManager = new WebRTCPublisher(reader, settings.loop);
			}
			else if(settings.mode == Mode.PLAYER){
				streamManager = new WebRTCPlayer(settings);
			}

			if (streamManager == null) {
				throw new IllegalArgumentException("Illegal mode not publisher or player");
			}
			
			streamManager.setManager(webRTCManager);
			webRTCManager.setStreamManager(streamManager);
			

			webRTCManager.setListener(this);

			getManagers().add(webRTCManager);
		}
		
		
		statManager.start();

	}

	public void start() {
		getManagers().get(startingIndex).start();
		startingIndex++;		
	}


	public static void main( String[] args )
	{
		System.out.println(Arrays.toString(args));
		Starter starter = new Starter(args);
		starter.start();
		System.out.println("Leaving main method");
	}

	public void stop() {
		statManager.stop();
		for (WebRTCManager webRTCManager : getManagers()) {
			webRTCManager.stop();
		}
		getManagers().clear();
		System.out.println("~~~~~~~~ Stop ("+hashCode()+")~~~~~~~~");
	}
	
	public boolean isStopped() {
		for (WebRTCManager webRTCManager : getManagers()) {
			if (webRTCManager.isStopped()) {
				return true;
			}
		}
		return false;
	}
	
	public IWebRTCEventListerner getListener() {
		return listener;
	}

	public void setListener(IWebRTCEventListerner listener) {
		this.listener = listener;
	}
	
	public void sendDataChannelMessage(String message) {
		getManagers().get(0).sendDataChannelMessage(message);
	}

	@Override
	public void onCompleted() {
		System.out.println("on completed");
		
		statManager.addStreamManager(getManagers().get(startingIndex-1).getStreamManager());

		if(startingIndex < settings.load) {
			start();
		}
	}
	
	@Override
	public void onDataChannelMessage(String string) {
	}

	public ArrayList<WebRTCManager> getManagers() {
		return managers;
	}

	public void setManagers(ArrayList<WebRTCManager> managers) {
		this.managers = managers;
	}
	

}
