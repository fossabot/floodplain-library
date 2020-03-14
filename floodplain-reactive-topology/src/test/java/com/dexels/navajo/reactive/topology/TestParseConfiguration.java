package com.dexels.navajo.reactive.topology;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.dexels.immutable.factory.ImmutableFactory;
import com.dexels.kafka.streams.api.StreamConfiguration;
import com.dexels.kafka.streams.api.TopologyContext;
import com.dexels.kafka.streams.api.sink.ConnectType;
import com.dexels.navajo.reactive.CoreReactiveFinder;
import com.dexels.navajo.reactive.api.Reactive;
import com.dexels.navajo.reactive.source.topology.TopologyReactiveFinder;
import com.dexels.navajo.reactive.source.topology.TopologyRunner;


public class TestParseConfiguration {
	
	
	private TopologyContext topologyContext;

	private String brokers = "kafka:9092";
	private String storagePath = "mystorage";

	private TopologyRunner runner;
	@Before
	public void setup() {
		String applicationId = UUID.randomUUID().toString();
		ImmutableFactory.setInstance(ImmutableFactory.createParser());
		topologyContext = new TopologyContext(Optional.of("Generic"), "test", "someinstance", "5");
		CoreReactiveFinder finder = new TopologyReactiveFinder();
		Reactive.setFinderInstance(finder);
		StreamConfiguration sc = StreamConfiguration.parseConfig("test", getClass().getClassLoader().getResourceAsStream("resources.xml"));

		runner = new TopologyRunner(storagePath,applicationId,sc,false);

		// TODO fill in props
//		Reactive.finderInstance().addReactiveSourceFactory(new MongoReactiveSourceFactory(), "topic");

	}

	// Ignoring for now, we need a working kafka cluster for it
//		java.lang.RuntimeException: Error listing topics
//			at com.dexels.kafka.streams.remotejoin.TopologyConstructor.<init>(TopologyConstructor.java:51)
//			at com.dexels.navajo.reactive.source.topology.TopologyRunner.<init>(TopologyRunner.java:91)
//			at com.dexels.navajo.reactive.topology.TestParseConfiguration.setup(TestParseConfiguration.java:43)

	@Test @Ignore
	public void testParse() throws IOException {
		try(InputStream r = getClass().getClassLoader().getResourceAsStream("resources.xml")) {
			StreamConfiguration sc = StreamConfiguration.parseConfig("test", r);
			Map<String,String> params = new HashMap<>();
			params.put("schema", "public");
			params.put("table", "address");
//			params.put("topic", "blabla");
			String topic = "blabla";
			runner.topologyConstructor().addConnectSink("dvd",topic, params);
			Map<String,String> mongoParams = new HashMap<>();
			mongoParams.put("collection", "somecollection");
//			mongoParams.put("topics", "blabla");
//			runner.topologyConstructor().addConnectSink("@replication", topic, mongoParams);
			runner.materializeConnectors(topologyContext, true);
			int connectors = sc.connectors().size();
			Assert.assertEquals(4, connectors);
		}
	}

//	public void startConnector(TopologyContext context, URL connectURL, String connectorName, ConnectType type, boolean force, Map<String,String> parameters) throws IOException {
	
	
}