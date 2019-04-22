package io.antmedia.monitor;


import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaWebRTCViewerProducer {

	public KafkaWebRTCViewerProducer() {

	}

	public interface IKafkaConstants {
		public static String KAFKA_BROKERS = "localhost:9092";
		public static Integer MESSAGE_COUNT=1000;
		public static String CLIENT_ID="client1";
		public static String TOPIC_NAME="demo";
		public static String OFFSET_RESET_LATEST="latest";
		public static String OFFSET_RESET_EARLIER="earliest";
		public static Integer MAX_POLL_RECORDS=1;
	}

	public static Producer<Long, String> createProducer() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, IKafkaConstants.KAFKA_BROKERS);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, IKafkaConstants.CLIENT_ID);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		//props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class.getName());
		return new KafkaProducer<>(props);
	}

	static void runProducer() {
		Producer<Long, String> producer = createProducer();
		for (int index = 0; index < IKafkaConstants.MESSAGE_COUNT; index++) {
			ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(IKafkaConstants.TOPIC_NAME,
					"This is record " + index);
			try {
				RecordMetadata metadata = producer.send(record).get();
				System.out.println("Record sent with key " + index + " to partition " + metadata.partition()
				+ " with offset " + metadata.offset());
			} 
			catch (ExecutionException e) {
				System.out.println("Error in sending record");
				System.out.println(e);
			} 
			catch (InterruptedException e) {
				System.out.println("Error in sending record");
				System.out.println(e);
			}
		}
	}
	
	static void createTopic() {
		
	}
	
	//@Test
	public void testProducer() {
		runProducer();
	}

}
