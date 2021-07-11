package io.antmedia.webrtctest;

import org.webrtc.Logging.Severity;

import io.antmedia.webrtc.VideoCodec;

public class Settings {

	String webSockAdr = "localhost";
	public String streamId = "myStream";
	public String streamSource = "test.mp4";
	public Mode mode = Mode.PLAYER;
	public Severity logLevel = Severity.LS_ERROR;
	
	public boolean useUI = true;
	public int port = 5080;
	boolean verbose = false;
	boolean isSequre = false;
	public int load = 1;
	public int frameLogPeriod = 200; //every 200 frames
	public boolean audioOnly = false;
	public boolean loop;

	
	public String kafkaBrokers = null;
	public VideoCodec codec = VideoCodec.H264;
	public boolean dataChannel;
	public String roomId = "";
	public String roomMode = "legacy";

	
	void printUsage() {
	    System.out.println("WebRTC Test Tool for Ant Media Server v0.2\n");
	    System.out.println("Flag \t Name         \t Default   \t Description                 ");
	    System.out.println("---- \t ----         \t -------   \t -----------                 ");
	    System.out.println("s    \t Server Ip    \t localhost \t server ip                   ");
	    System.out.println("q    \t Sequrity     \t false     \t true(wss) or false(ws)      ");
	    System.out.println("l    \t Log Level    \t 3         \t 0:VERBOSE,1:INFO,2:WARNING,3:ERROR,4:NONE");
	    System.out.println("i    \t Stream Id    \t myStream  \t id for stream               ");
	    System.out.println("f    \t File Name    \t test.mp4  \t Source file* for publisher output file for player");
	    System.out.println("m    \t Mode         \t player    \t publisher | player | participant");
	    System.out.println("u    \t Show GUI     \t true      \t true or false               ");
	    System.out.println("p    \t Port         \t 5080      \t websocket port number       ");
	    System.out.println("v    \t Verbose      \t false     \t true or false               ");
	    System.out.println("n    \t Load Size    \t 1         \t number of load              ");
	    System.out.println("k    \t Kafka Broker \t null      \t Kafra broker address withp port");
	    System.out.println("r    \t Loop Publish \t false     \t true or false");
	    System.out.println("c    \t Codec        \t h264      \t h264 or VP8");
	    System.out.println("d    \t DataChannel  \t false     \t true or false");
	    System.out.println("o    \t RoomId       \t room1     \t id for room                 ");
	    System.out.println("e    \t RoomMode     \t legacy    \t legacy | mcu | multitrack   ");

	}

	boolean parseLocal(String flag, String value) {
	    if(flag.charAt(0) != '-' && flag.length()<2) {
	        return false;
	    }
	    if(flag.charAt(1) == 's') {
	        webSockAdr = value;
	    }
	    else if(flag.charAt(1) == 'q') {
	        String seq = value;
	        isSequre = Boolean.parseBoolean(seq);
	    }
	    else if(flag.charAt(1) == 'l') {
	        logLevel = Severity.values()[Integer.parseInt(value)];
	    }
	    else if(flag.charAt(1) == 'i') {
	        streamId = value;
	    }
	    else if(flag.charAt(1) == 'f') {
	        streamSource = value;
	    }
	    else if(flag.charAt(1) == 'm') {
	        String strMode = value;
	        if(strMode.contentEquals("publisher")){
	            mode = Mode.PUBLISHER;
	        }
	        else if(strMode.contentEquals("player")){
	            mode = Mode.PLAYER;
	        }
	        else if(strMode.contentEquals("participant")){
	            mode = Mode.PARTICIPANT;
	        }
	        else {
	            System.out.println("undefined mode:"+strMode);
	            return false;
	        }
	    }
	    else if(flag.charAt(1) == 'u') {
	        String strUI = value;
	        useUI = Boolean.parseBoolean(strUI);
	    }
	    else if(flag.charAt(1) == 'r') {
	        String strLoop = value;
	        loop = Boolean.parseBoolean(strLoop);
	    }
	    else if(flag.charAt(1) == 'p') {
	        String strPort = value;
	        port = Integer.parseInt(strPort);
	    }
	    else if(flag.charAt(1) == 'v') {
	        String verb = value;
	        verbose = Boolean.parseBoolean(verb);
	    }
	    else if(flag.charAt(1) == 'n') {
	        String strLoad = value;
	        load  = Integer.parseInt(strLoad);
	    }
	    else if (flag.charAt(1) == 'k') {
	    		kafkaBrokers = value;
	    }
	    else if(flag.charAt(1) == 'c') {
	        String strCodec = value;
	        if(strCodec.equalsIgnoreCase("h264")){
	            codec = VideoCodec.H264;
	        }
	        else if(strCodec.equalsIgnoreCase("VP8")){
	        		codec = VideoCodec.VP8;
	        }
	        else if (strCodec.equalsIgnoreCase("h265")) {
	        		codec = VideoCodec.H265;
	        }
	        else {
	            System.err.println("undefined codec:"+strCodec);
	            return false;
	        }
	    }
	    else if(flag.charAt(1) == 'd') {
	        String strDC = value;
	        dataChannel = Boolean.parseBoolean(strDC);
	    }
	    else if(flag.charAt(1) == 'o') {
	        roomId = value;
	    }
	    else if(flag.charAt(1) == 'e') {
	        roomMode = value;
	    }
	    else {
	        return false;
	    }
	    return true;
	}

	public boolean parse(String [] args) {
	    for(int i = 0; i < args.length; i+=2) {
	        if(!parseLocal(args[i], args[i+1]))
	        {
	            return false;
	        }
	    }

	    print();
	    return true;
	}

	void print() {
	    System.out.println("Settings:\n");
	    System.out.println("- webSockAdr:" + webSockAdr);
	    System.out.println("- is sequre:" + isSequre);
	    System.out.println("- logLevel:" + logLevel);
	    System.out.println("- stream id:"+ streamId);
	    System.out.println("- stream source:" + streamSource);
	    System.out.println("- mode:" + mode);
	    System.out.println("- use ui:" + useUI);
	    System.out.println("- port:" + port);
	    System.out.println("- verbose:" + verbose);
	    System.out.println("- load:" + load);
	    System.out.println("- loop:" + loop);
	    System.out.println("- codec:" + codec);
	    System.out.println("- dataChannel:" + dataChannel);
	    if(mode.equals(Mode.PARTICIPANT)) {
	    	System.out.println("- room:" + roomId);
	    	System.out.println("- room mode:" + roomMode);
	    }
	}

}


