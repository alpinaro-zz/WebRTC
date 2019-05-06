package io.antmedia.webrtctest;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatManager {

	private static final String VIDEO_FRAME_PERIOD = "video_frame_period";
	private static final String CLIENT_TYPE = "client_type";
	private static final String TOTAL_VIDEO_FRAME_COUNT = "total_video_frame_count";
	private static final String TOPIC_NAME = "kafka-webrtc-tester-stats";
	private static final String CLIENT_ID = "client_id";
	
	private ArrayList<StreamManager> streamManagers = new ArrayList<>();
	ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
	private Logger logger = LoggerFactory.getLogger(StatManager.class);

	private boolean streamsRunning = true;
	private Producer<Long, String> producer;
	
	
	public static Producer<Long, String> createProducer(String kafkaBrokers, String clientId) {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		return new KafkaProducer<>(props);
	}
	
	public void start() {
		
		if (Settings.instance.kafkaBrokers != null) {
			producer = createProducer(Settings.instance.kafkaBrokers, String.valueOf(this.hashCode()));
		}

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
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;
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
			jsonObject = new JSONObject();
			jsonObject.put(VIDEO_FRAME_PERIOD, fp);
			jsonObject.put(CLIENT_TYPE, streamManager instanceof WebRTCPublisher ? "publisher" : "player");
			jsonObject.put(TOTAL_VIDEO_FRAME_COUNT, streamManager.getCount());
			jsonObject.put(CLIENT_ID, streamManager);
	
			jsonArray.add(jsonObject);
		}
		
		if (producer != null) {
			ProducerRecord<Long, String> record = new ProducerRecord<>(TOPIC_NAME,
					jsonArray.toJSONString());
			try {
				producer.send(record).get();
			} 
			catch (ExecutionException e) {
				logger.error("Error in sending record {}", e.getStackTrace());
			} 
			catch (InterruptedException e) {
				logger.error("Error in sending record {}", e.getStackTrace());
				Thread.currentThread().interrupt();
			}
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
