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


	public void start() {
		//ScheduledFuture<?> logFuture = scheduledExecutorService.scheduleAtFixedRate(() -> logStats(), 2000, 2000, TimeUnit.MILLISECONDS);

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
					super.run();
				}
			}
		}.start();
	}


	private void logStats() {
		long total = 0; 
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int mean = 0;
		for (StreamManager streamManager : streamManagers) {
			int fp = streamManager.getFramePeriod();
			min = min < fp ? min : fp;
			max = max < fp ? fp : max;
			total += fp;
		}
		mean = (int) (total/streamManagers.size());

		logger.info("stats:\t{}\t{}\t{}\t{}", streamManagers.size(), min, max, mean);
	}


	public void addStreamManager(StreamManager streamManager) {
		streamManagers.add(streamManager);
	}

}
