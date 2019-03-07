package io.antmedia.webrtctest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Hello world!
 *
 */
public class Starter implements IWebRTCEventListerner
{

	ArrayList<WebRTCManager> managers = new ArrayList<>();
	private MP4Reader reader;
	
	StatManager statManager = new StatManager();

	
	int startingIndex = 0;

	public Starter(String[] args) {
		Settings.instance.parse(args);
		
		

		if(Settings.instance.mode == Mode.PUBLISHER) {
			reader = new MP4Reader(Settings.instance.streamSource);
			if(reader.init()) {
				reader.start();
			}
			else {
				System.exit(1);
			}
		}

		for (int i = 0; i < Settings.instance.load; i++) {
			String suffix = Settings.instance.mode == Mode.PUBLISHER && Settings.instance.load > 1 ? "-"+i : ""; 
			WebRTCManager webRTCManager = new WebRTCManager(Settings.instance.streamId+suffix);

			StreamManager streamManager = null;
			if(Settings.instance.mode == Mode.PUBLISHER) {
				streamManager = new WebRTCPublisher(reader);
			}
			else if(Settings.instance.mode == Mode.PLAYER){
				streamManager = new WebRTCPlayer();
			}

			streamManager.setManager(webRTCManager);
			webRTCManager.setStreamManager(streamManager);
			

			webRTCManager.setListener(this);

			managers.add(webRTCManager);
		}
		
		statManager.start();

	}

	private void start() {
		System.out.println("start");
		managers.get(startingIndex).start();
		startingIndex++;		
	}


	public static void main( String[] args )
	{
		//String test = "-m publisher -f /home/burak/test/Test.mp4 -i deneme";
		//String test = "-n 60";
		//args = test.split(" ");
		System.out.println(Arrays.toString(args));
		Starter starter = new Starter(args);
		starter.start();
	}

	@Override
	public void onCompleted() {
		
		System.out.println("on completed");
		
		statManager.addStreamManager(managers.get(startingIndex-1).getStreamManager());

		
		if(startingIndex < Settings.instance.load) {
			start();
		}
		
	}


}
