package io.antmedia.webrtctest;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
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
	private static final String CONNECTED = "connected";
	private static final String SYSTEM_CPU_LOAD = "system_cpu_load";
	private static final String INSTANCE_ID = "instance_id";
	
	private ArrayList<StreamManager> streamManagers = new ArrayList<>();
	ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
	private Logger logger = LoggerFactory.getLogger(StatManager.class);

	private boolean streamsRunning = true;
	private Producer<Long, String> producer;
	private String instanceId;
	
	private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private String kafkaBrokers;
	
	public StatManager(String kafkaBrokers) {
		this.kafkaBrokers = kafkaBrokers;
	}
	
	public static Producer<Long, String> createProducer(String kafkaBrokers, String clientId) {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		return new KafkaProducer<>(props);
	}
	
	public void start() {
		
		if (kafkaBrokers != null) {
			producer = createProducer(kafkaBrokers, String.valueOf(this.hashCode()));
			instanceId = UUID.randomUUID().toString();
		}

		executorService.scheduleWithFixedDelay(() -> {
			logStats();
			if (!streamsRunning) {
				logger.info("Seems all streams are stopped and breaking the stats loop hash: {}", hashCode());
				
			}
		}, 10, 10, TimeUnit.SECONDS);
	}


	private void logStats() {
		//logger.info("<- Logging stats ->");
		long total = 0; 
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int mean = 0;
		
		boolean streamsRunningLocal = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;
		int droppedConnections = 0;
		int activeConnections = 0;
		Integer systemCpuLoad = getSystemCpuLoad();
		int numberOfClientsForFPSCalculation = 0;
		for (StreamManager streamManager : streamManagers) 
		{
			if (!streamManager.isStarted() ||   //if stream is not started, assume that it is running
					(streamManager.isRunning() && !streamsRunningLocal)) {
				//if one stream is running, no need to enter again
				streamsRunningLocal = true;
			}
			int fp = streamManager.getFramePeriod();
			if (fp != -1) {
				min = min < fp ? min : fp;
				max = max < fp ? fp : max;
				total += fp;
				numberOfClientsForFPSCalculation++;
			}
			
			if (!streamManager.isRunning()) {
				droppedConnections++;
			}
			else {
				activeConnections++;
			}
			
			if (producer != null) {
				jsonObject = new JSONObject();
				jsonObject.put(VIDEO_FRAME_PERIOD, fp);
				jsonObject.put(CLIENT_TYPE, streamManager instanceof WebRTCPublisher ? "publisher" : "player");
				jsonObject.put(TOTAL_VIDEO_FRAME_COUNT, streamManager.getCount());
				jsonObject.put(CLIENT_ID, streamManager.hashCode());
				jsonObject.put(CONNECTED, streamManager.isRunning());
				jsonObject.put(SYSTEM_CPU_LOAD, systemCpuLoad);
				jsonObject.put(INSTANCE_ID, instanceId);

				ProducerRecord<Long, String> record = new ProducerRecord<>(TOPIC_NAME,
						jsonObject.toJSONString());
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
		}
		
		//if all stream is finished, change the global flag 
		if (!streamsRunningLocal) {
			streamsRunning = false;
		}
		if (!streamManagers.isEmpty()) {
			mean = (int) (total/numberOfClientsForFPSCalculation);
		}
		
		logger.info("stats :\tNumber of Clients:{} Active Connections:{} Dropped Connections:{} Received Min frame period:{}ms, Max frame period: {}ms, Mean frame period:{}ms, cpu load: %{} time: {} hash: {}", streamManagers.size(), activeConnections, droppedConnections, min, max, mean, systemCpuLoad, System.currentTimeMillis()/1000, hashCode());
	}


	public void addStreamManager(StreamManager streamManager) {
		if (!streamManagers.contains(streamManager)) {
			streamManagers.add(streamManager);
		}
	}
	
	public static Integer getSystemCpuLoad() {

		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getSystemCpuLoad");
			m.setAccessible(true);
			return (int)(((Double)m.invoke(osBean))*100);
		} catch (Exception e) {
			return -1;
		}
	}

}
