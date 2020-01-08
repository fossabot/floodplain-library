package com.dexels.kafka.streams.processor.programmatic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.kafka.streams.api.TopologyContext;
import com.dexels.kafka.streams.base.StreamInstance;
import com.dexels.kafka.streams.remotejoin.ReplicationTopologyParser;
import com.dexels.kafka.streams.remotejoin.TopologyConstructor;
import com.dexels.replication.api.ReplicationMessage;
import com.dexels.replication.transformer.api.MessageTransformer;

public class TestCreateTopology {

	private static final String BROKERS = "kafka:9092";

	
	private static final Logger logger = LoggerFactory.getLogger(TestCreateTopology.class);

	@Test @Ignore
	public void testTopology() {
		Map<String,Object> config = new HashMap<>();

		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,BROKERS);
		config.put(AdminClientConfig.CLIENT_ID_CONFIG ,UUID.randomUUID().toString());
		AdminClient adminClient = AdminClient.create(config);
		
		final String applicationId = "shazam-"+UUID.randomUUID().toString();
		Properties properties = StreamInstance.createProperties( applicationId,BROKERS, "tempdump");
		System.err.println("ApplicationId: "+applicationId);
		ProcessorSupplier<String, ReplicationMessage> supplier =()->new IdentityProcessor();
		Topology topology = new Topology();
		TopologyContext context = new TopologyContext(Optional.of("Generic"), "test", "my_instance", "20191214");
		Map<String,MessageTransformer> transformerRegistry = new HashMap<>();
		TopologyConstructor topologyConstructor = new TopologyConstructor(transformerRegistry, adminClient);
//		ReplicationTopologyParser.addGroupedProcessor(topology, context, topologyConstructor, name, from, ignoreOriginalKey, key, transformerSupplier);
		ReplicationTopologyParser.addSourceStore(topology, context, topologyConstructor, supplier, "PHOTO", Optional.empty());
		ReplicationTopologyParser.materializeStateStores(topologyConstructor, topology);
		System.err.println(topology.describe().toString());
//		KafkaStreams stream = new KafkaStreams(topology, properties);
//		stream.setUncaughtExceptionHandler((thread,exception)->logger.error("Uncaught exception from stream instance: ",exception));
//		stream.start();
//		Thread.sleep(300000);
		
	}
}