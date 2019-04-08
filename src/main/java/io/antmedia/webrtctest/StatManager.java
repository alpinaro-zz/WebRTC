package io.antmedia.webrtctest;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatManager {

	private ArrayList<StreamManager> streamManagers = new ArrayList<>();
	ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
	private Logger logger = LoggerFactory.getLogger(StatManager.class);

	private boolean streamsRunning = true;
	
	public void start() {

		new Thread() {
			@Override
			public void run() {
				while (true) {
					
					try {
						sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					logStats();
					if (!streamsRunning) {
						logger.info("Seems all streams are stopped and breaking the stats loop");
						break;
					}
				}
			}
		}.start();
	}


	private void logStats() {
		long total = 0; 
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int mean = 0;
		
		boolean streamsRunningLocal = false;
		for (StreamManager streamManager : streamManagers) 
		{
			
			if (!streamManager.isStarted() ||   //if stream is not started, assume that it is running
					(streamManager.isRunning() && !streamsRunningLocal)) {
				//if one stream is running, no need to enter again
				streamsRunningLocal = true;
			}
			int fp = streamManager.getFramePeriod();
			min = min < fp ? min : fp;
			max = max < fp ? fp : max;
			total += fp;
		}
		//if all stream is finished, change the global flag 
		if (!streamsRunningLocal) {
			streamsRunning = false;
		}
		if (!streamManagers.isEmpty()) {
			mean = (int) (total/streamManagers.size());
		}
		
		logger.info("stats:\t{}\t{}\t{}\t{}", streamManagers.size(), min, max, mean);
	}


	public void addStreamManager(StreamManager streamManager) {
		streamManagers.add(streamManager);
	}

}
