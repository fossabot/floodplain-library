package com.dexels.navajo.reactive.topology;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.immutable.factory.ImmutableFactory;
import com.dexels.kafka.streams.api.StreamConfiguration;
import com.dexels.kafka.streams.api.TopologyContext;
import com.dexels.kafka.streams.base.StreamInstance;
import com.dexels.kafka.streams.remotejoin.TopologyConstructor;
import com.dexels.navajo.parser.compiled.ParseException;
import com.dexels.navajo.reactive.CoreReactiveFinder;
import com.dexels.navajo.reactive.ReactiveStandalone;
import com.dexels.navajo.reactive.api.CompiledReactiveScript;
import com.dexels.navajo.reactive.api.Reactive;
import com.dexels.navajo.reactive.source.topology.TopologyReactiveFinder;
import com.dexels.replication.transformer.api.MessageTransformer;

public class TestBuildTopology {
	
	
	private final static Logger logger = LoggerFactory.getLogger(TestBuildTopology.class);

	AdminClient adminClient;
	private Map<String, MessageTransformer> transformerRegistry = Collections.emptyMap();
	private TopologyContext topologyContext;
	private Properties props;

	private String brokers = "kafka:9092";
	private String storagePath = "mystorage";

	private TopologyConstructor topologyConstructor;
	@Before
	public void setup() {
		ImmutableFactory.setInstance(ImmutableFactory.createParser());
		topologyContext = new TopologyContext(Optional.of("Generic"), "test", "someinstance", "5");
		props = StreamInstance.createProperties(UUID.randomUUID().toString(), brokers, storagePath);
		adminClient = AdminClient.create(props);
		topologyConstructor = new TopologyConstructor(transformerRegistry , Optional.of(adminClient));
		CoreReactiveFinder finder = new TopologyReactiveFinder();
		Reactive.setFinderInstance(finder);
		// TODO fill in props
//		Reactive.finderInstance().addReactiveSourceFactory(new MongoReactiveSourceFactory(), "topic");

	}
	
	private void runTopology(Topology topology) throws InterruptedException {
		KafkaStreams stream = new KafkaStreams(topology, props);
		stream.setUncaughtExceptionHandler((thread,exception)->{
			logger.error("Error in streams: ",exception);
		});
		stream.setStateListener((oldState,newState)->{
			logger.info("State moving from {} to {}",oldState,newState);
		});
		stream.start();
		for (int i = 0; i < 50; i++) {
			boolean isRunning = stream.state().isRunning();
	        String stateName = stream.state().name();
	        System.err.println("State: "+stateName+" - "+isRunning);
	        final Collection<StreamsMetadata> allMetadata = stream.allMetadata();
	        System.err.println("meta: "+allMetadata);
			Thread.sleep(1000);
		}

		stream.close();
		Thread.sleep(5000);
	}


	private Topology parseReactivePipeTopology(InputStream input) throws ParseException, IOException {
		CompiledReactiveScript crs = ReactiveStandalone.compileReactiveScript(input);
		Topology topology = ReactivePipeParser.parseReactiveStreamDefinition(crs, topologyContext, topologyConstructor);
		return topology;
	}
	
	@Test
	public void testSimpleTopic() throws ParseException, IOException {
		Topology topology = parseReactivePipeTopology(getClass().getClassLoader().getResourceAsStream("simpletopic.rr"));
		System.err.println("Topology: \n"+topology.describe());
	}

	@Test
	public void testDatabase() throws ParseException, IOException {
		Topology topology = parseReactivePipeTopology(getClass().getClassLoader().getResourceAsStream("database.rr"));
		System.err.println("Topology: \n"+topology.describe());
	}
	
	@Test
	public void testStorelessTopic() throws ParseException, IOException {
		Topology topology = parseReactivePipeTopology(getClass().getClassLoader().getResourceAsStream("simplewithoutstore.rr"));
		System.err.println("Topology: \n"+topology.describe());
	}
	
	@Test @Ignore
	public void testJoinTopic() throws ParseException, IOException, InterruptedException {
		Topology topology = parseReactivePipeTopology(getClass().getClassLoader().getResourceAsStream("jointopic.rr"));
		System.err.println("Topology: \n"+topology.describe());
		runTopology(topology);
	}

	@Test
	public void testConfigurationStreamInstance() throws ParseException, IOException, InterruptedException {
		StreamConfiguration sc = StreamConfiguration.parseConfig("test", getClass().getClassLoader().getResourceAsStream("resources.xml"));
		Topology topology = parseReactivePipeTopology(getClass().getClassLoader().getResourceAsStream("address.rr"));
		System.err.println("Topology: \n"+topology.describe());
	}

	@Test @Ignore
	public void testRemoteJoin() throws ParseException, IOException, InterruptedException {
		Topology topology = parseReactivePipeTopology(getClass().getClassLoader().getResourceAsStream("remotejoin.rr"));
		System.err.println("Topology: \n"+topology.describe());
		runTopology(topology);
	}

	@Test @Ignore
	public void testDebezium() throws ParseException, IOException, InterruptedException {
		Topology topology = parseReactivePipeTopology(getClass().getClassLoader().getResourceAsStream("sinkLog.rr"));
		System.err.println("Topology: \n"+topology.describe());
		runTopology(topology);
	}


	
	@Test @Ignore
	public void testAddressTopic() throws ParseException, IOException, InterruptedException {

		Topology topology = parseReactivePipeTopology(getClass().getClassLoader().getResourceAsStream("address.rr"));
		System.err.println("Topology: \n"+topology.describe());
		runTopology(topology);

	}

}