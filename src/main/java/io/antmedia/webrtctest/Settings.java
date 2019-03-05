package io.antmedia.webrtctest;

public class Settings {

	String webSockAdr = "localhost";
	String label = "nolabel";
	String streamId = "myStream";
	String streamSource = "camera";
	Mode mode = Mode.PLAYER;
	boolean useUI = true;
	int period = 0;
	boolean verbose = false;
	boolean isSequre = false;
	int load = 1;
	public int frameLogPeriod = 200; //every 200 frames
	
	public static Settings instance = new Settings();
	
	void printUsage() {
	    System.out.println("WebRTC Test Tool for Ant Media Server v0.2\n");
	    System.out.println("Flag \t Name      \t Default   \t Description                 ");
	    System.out.println("---- \t ----      \t -------   \t -----------                 ");
	    System.out.println("s    \t Server Ip \t localhost \t server ip                   ");
	    System.out.println("q    \t Sequrity  \t false     \t true(wss) or false(ws)      ");
	    System.out.println("l    \t Label     \t nolabel   \t window lable                ");
	    System.out.println("i    \t Stream Id \t myStream  \t id for stream               ");
	    System.out.println("f    \t File Name \t camera    \t media file in same directory");
	    System.out.println("m    \t Mode      \t player    \t publisher or player         ");
	    System.out.println("u    \t Show GUI  \t true      \t true or false               ");
	    System.out.println("p    \t Period    \t 0         \t frame period to save as png ");
	    System.out.println("v    \t Verbose   \t false     \t true or false               ");
	    System.out.println("n    \t Load Size \t 1         \t number of load              ");
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
	        label = value;
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
	        else {
	            System.out.println("undefined mode:"+strMode);
	            return false;
	        }
	    }
	    else if(flag.charAt(1) == 'u') {
	        String strUI = value;
	        useUI = Boolean.parseBoolean(strUI);
	    }
	    else if(flag.charAt(1) == 'p') {
	        String strPeriod = value;
	        period = Integer.parseInt(strPeriod);
	    }
	    else if(flag.charAt(1) == 'v') {
	        String verb = value;
	        verbose = Boolean.parseBoolean(verb);
	    }
	    else if(flag.charAt(1) == 'n') {
	        String strLoad = value;
	        load  = Integer.parseInt(strLoad);
	    }
	    else {
	        return false;
	    }
	    return true;
	}

	boolean parse(String [] args) {
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
	    System.out.println("- label:" + label);
	    System.out.println("- stream id:"+ streamId);
	    System.out.println("- stream source:" + streamSource);
	    System.out.println("- mode:" + mode);
	    System.out.println("- use ui:" + useUI);
	    System.out.println("- period:" + period);
	    System.out.println("- verbose:" + verbose);
	    System.out.println("- load:" + load);
	}

}


