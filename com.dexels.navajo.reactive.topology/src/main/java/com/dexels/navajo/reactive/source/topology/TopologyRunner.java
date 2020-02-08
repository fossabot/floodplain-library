package com.dexels.navajo.reactive.source.topology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.stream.FileImageInputStream;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.kafka.converter.ReplicationMessageConverter;
import com.dexels.kafka.streams.api.CoreOperators;
import com.dexels.kafka.streams.api.StreamConfiguration;
import com.dexels.kafka.streams.api.TopologyContext;
import com.dexels.kafka.streams.api.sink.ConnectConfiguration;
import com.dexels.kafka.streams.api.sink.ConnectType;
import com.dexels.kafka.streams.base.StreamInstance;
import com.dexels.kafka.streams.remotejoin.ReplicationTopologyParser;
import com.dexels.kafka.streams.remotejoin.TopologyConstructor;
import com.dexels.kafka.streams.remotejoin.TopologyConstructor.ConnectorTopicTuple;
import com.dexels.kafka.streams.remotejoin.TopologyDefinitionException;
import com.dexels.kafka.streams.tools.KafkaUtils;
import com.dexels.navajo.parser.compiled.ParseException;
import com.dexels.navajo.reactive.ReactiveStandalone;
import com.dexels.navajo.reactive.api.CompiledReactiveScript;
import com.dexels.navajo.reactive.topology.ReactivePipeParser;
import com.dexels.navajo.repository.api.RepositoryInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TopologyRunner {
	
	
	private final static Logger logger = LoggerFactory.getLogger(TopologyRunner.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final TopologyContext topologyContext;
	private final TopologyConstructor topologyConstructor;

	
	private final Map<String,String> baseSettings;
	private final Properties props;

	public TopologyRunner(TopologyContext topologyContext, String brokers, String storagePath,String applicationId) {
		Map<String,String> settings = new HashMap<>();			
		settings.put("key.converter", ReplicationMessageConverter.class.getName());
		settings.put("key.converter.schemas.enable", "false");
		settings.put("value.converter", ReplicationMessageConverter.class.getName());
		settings.put("value.converter.schemas.enable", "false");
		baseSettings = Collections.unmodifiableMap(settings);
		this.topologyContext = topologyContext;
		props = StreamInstance.createProperties(applicationId, brokers, storagePath);
		this.topologyConstructor =  new TopologyConstructor(Optional.empty() , Optional.of(AdminClient.create(props)));
	}
	
	public KafkaStreams runTopology(Topology topology, Optional<StreamConfiguration> streamConfiguration) throws InterruptedException, IOException {
		if(streamConfiguration.isPresent()) {
			materializeConnectors(streamConfiguration.get(),true);
		}
		KafkaStreams stream = new KafkaStreams(topology, props);
		stream.setUncaughtExceptionHandler((thread,exception)->{
			logger.error("Error in streams: ",exception);
		});
		stream.setStateListener((oldState,newState)->{
			logger.info("State moving from {} to {}",oldState,newState);
		});
		stream.start();
		return stream;
//		for (int i = 0; i < 50; i++) {
//			boolean isRunning = stream.state().isRunning();
//	        String stateName = stream.state().name();
//	        System.err.println("State: "+stateName+" - "+isRunning);
//	        final Collection<StreamsMetadata> allMetadata = stream.allMetadata();
//	        System.err.println("meta: "+allMetadata);
//			Thread.sleep(10000);
//		}
//
//		stream.close();
//		Thread.sleep(5000);
	}

	public TopologyConstructor topologyConstructor() {
		return topologyConstructor;
	}
	
//	private Topology parseReactivePipeTopology(String repositoryPath) throws ParseException, IOException {
////		FileRepositoryInstanceImpl
//	}
//	
	public Topology parseReactivePipeTopology(File repoPath) throws ParseException, IOException {
		Topology topology = new Topology();
		File streams = new File(repoPath,"streams");
		parseReactivePipeFolder(topology,streams);
		return topology;
	}
	
	public KafkaStreams runPipeFolder(File repoPath) throws ParseException, IOException, InterruptedException {
		Topology topology = parseReactivePipeTopology(repoPath);
		System.err.println("Combined topology:\n"+topology.describe());
		File resources = new File(repoPath,"config/resources.xml");
		StreamConfiguration streamConfiguration;
		try(InputStream is = new FileInputStream(resources)) {
			streamConfiguration = StreamConfiguration.parseConfig("test", is);
			return runTopology(topology, Optional.of(streamConfiguration));
		}

	}
	
//	private void runTopology(Topology topology, Optional<StreamConfiguration> streamConfiguration) throws InterruptedException, IOException {
//		KafkaStreams stream = runner.runTopology(topology, streamConfiguration);
//		for (int i = 0; i < 50; i++) {
//			boolean isRunning = stream.state().isRunning();
//	        String stateName = stream.state().name();
//	        System.err.println("State: "+stateName+" - "+isRunning);
//	        final Collection<StreamsMetadata> allMetadata = stream.allMetadata();
//	        System.err.println("meta: "+allMetadata);
//			Thread.sleep(100);
//		}
//		stream.close();
//		Thread.sleep(1000);
//	}
	
	private Topology parseReactivePipeFolder(Topology topology, File folder) throws ParseException, IOException {
		File[] files = folder.listFiles(e->e.getName().endsWith(".rr"));
		for (File file : files) {
			try(InputStream is = new FileInputStream(file)) {
				parseSinglePipeDefinition(topology,is,file.getName().split("\\.")[0]);
			}
		}
		ReplicationTopologyParser.materializeStateStores(topologyConstructor, topology);
		return topology;
	}
	public Topology parseSinglePipeDefinition(Topology topology, InputStream input, String namespace) throws ParseException, IOException {
		CompiledReactiveScript crs = ReactiveStandalone.compileReactiveScript(input);
		ReactivePipeParser.parseReactiveStreamDefinition(topology, crs, topologyContext, topologyConstructor(),namespace);
		logger.info("Topology before materialize: {}", topology.describe());
		return topology;
	}
	
	public void startConnector(URL connectURL, String connectorName, ConnectType type, boolean force, Map<String,String> parameters) throws IOException {
		String generatedName = CoreOperators.topicName(connectorName, topologyContext);

		List<String> current = existingConnectors(connectURL);
		if(current.contains(generatedName)) {
			if(force) {
				logger.warn("Force enabled, deleting old");
				deleteConnector(generatedName,connectURL);
			} else {
				logger.warn("Connector: {} already present, ignoring",generatedName);
				return;
			}
		}
		String connector = parameters.get("connector.class");
		if(connector==null) {
			throw new TopologyDefinitionException("Error creating connector message for connector: "+connectorName+" it has no connector.class setting");
		}
		ObjectNode node = objectMapper.createObjectNode();
		node.put("name", generatedName);
		ObjectNode configNode = objectMapper.createObjectNode();
		node.set("config", configNode);
		parameters.forEach((k,v)->{
			configNode.put(k, v);
		});
		// override name to match general name
		configNode.put("name", generatedName);
		configNode.put("database.server.name", generatedName);
		String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
		logger.info(">> {}", jsonString);
		postToHttp(connectURL, jsonString);
	}
	
	private List<String> existingConnectors(URL url) throws IOException {
		ArrayNode an = (ArrayNode) objectMapper.readTree(url.openStream());
		List<String> result = new ArrayList<>();
		an.forEach(j->result.add(j.asText()));
		return Collections.unmodifiableList(result);
	}
	
	
	private void deleteConnector(String name,URL connectURL) throws IOException {
		URL url = new URL(connectURL+"/"+name);
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("DELETE");
		int code = con.getResponseCode();
		logger.info("Delete result: {}",code);
}
	
	// TODO replace with Java 11 client when we can go to graal 19.3
	private void postToHttp(URL url, String jsonString) throws ProtocolException, IOException {
//		URL url = new URL(this.connectURL);
		logger.info("Posting to: {}",url);
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		
//		-H "Accept:application/json" -H "Content-Type:application/json"
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);
		try(OutputStream os = con.getOutputStream()) {
		    byte[] input = jsonString.getBytes("utf-8");
		    os.write(input, 0, input.length);           
		}
		logger.info("Result code: {} and message: {}",con.getResponseCode(),con.getResponseMessage());
		
		try(BufferedReader br = new BufferedReader(
				  new InputStreamReader(con.getInputStream(), "utf-8"))) {
				    StringBuilder response = new StringBuilder();
				    String responseLine = null;
				    while ((responseLine = br.readLine()) != null) {
				        response.append(responseLine.trim());
				    }
				    System.out.println(response.toString());
				}
	}
	
    
    public void materializeConnectors(StreamConfiguration sc, boolean force) throws IOException {
    	if(!sc.connectURL().isPresent()) {
    		logger.warn("No connectURL present, so not materializing anything");
    		return;
    	}
    	Set<String> topics = new HashSet<>();
		for (Entry<String,List<ConnectorTopicTuple>> e : topologyConstructor.connectorAssociations.entrySet()) {
			for (ConnectorTopicTuple tuple : e.getValue()) {
				topics.add(tuple.topicName);
			}
		}
    	topologyConstructor.adminClient.ifPresent(admin->{
    		KafkaUtils.ensureExistSync(topologyConstructor.adminClient, topics,CoreOperators.topicPartitionCount(),CoreOperators.topicReplicationCount());
    	});
		
		for (Entry<String,List<ConnectorTopicTuple>> e : topologyConstructor.connectorAssociations.entrySet()) {
			List<ConnectorTopicTuple> list = e.getValue();
			
//			logger.info("CTT: resource {} topic: {} parameters: {}",e.getKey(), e.getValue().topicName, ctt.sinkParameters);
			Optional<ConnectConfiguration> cc = sc.connector(e.getKey());
			if(!cc.isPresent()) {
				throw new TopologyDefinitionException("Missing sink resource named: "+e.getKey());
			}
			List<Map<String,String>> parsed = parseConnector(list, cc.get());
			int connectorCount = 0;
			for (Map<String, String> element : parsed) {
				Map<String,String> processed = element.entrySet().stream().collect(Collectors.toMap(key->key.getKey(),v->CoreOperators.resolveGenerations(v.getValue(), topologyContext)) );
				assembleConnector(cc.get(),processed,sc.connectURL().get(),force,connectorCount);
				connectorCount++;
			}
		}

    }
    
    public List<Map<String,String>> parseConnector(List<ConnectorTopicTuple> tuples, ConnectConfiguration connectorConfig) {
    	String clazz = connectorConfig.settings().get("connector.class");
    	switch (clazz) {
		case "io.debezium.connector.postgresql.PostgresConnector":
			String whitelist = tuples.stream().map(e->e.sinkParameters.get("schema")+"."+e.sinkParameters.get("table")).collect(Collectors.joining(","));
			Map<String,String> settings = new HashMap<>(connectorConfig.settings());
//			settings.putAll(this.baseSettings);
			// TODO if 'resource' is still in the map, I should remove it, right?
			settings.put("table.whitelist", whitelist);
			return Arrays.asList(settings);
		case "com.mongodb.kafka.connect.MongoSinkConnector":
			List<Map<String,String>> result = new ArrayList<>();
			for (ConnectorTopicTuple connectorTopicTuple : tuples) {
				Map<String,String> cSettings = new HashMap<>(connectorConfig.settings());
				cSettings.putAll(connectorTopicTuple.sinkParameters);
				cSettings.putAll(this.baseSettings);
				cSettings.put("topics",connectorTopicTuple.topicName);
				cSettings.put("tasks.max","1");
				cSettings.put("document.id.strategy","com.mongodb.kafka.connect.sink.processor.id.strategy.FullKeyStrategy");
				logger.info("Settings: {}", cSettings);
				result.add(cSettings);
			}
			return result;
		default:
			throw new UnsupportedOperationException("Unknown connector class: "+clazz);
		}
    }
    
	public void assembleConnector(ConnectConfiguration cc,Map<String,String> parameters, URL connectURL, boolean force, int connectorCount) throws IOException {
		
		Map<String,String> result = new HashMap<>();
//		result.putAll(baseSettings);
//		result.putAll(cc.settings());
		result.putAll(parameters);
		startConnector(connectURL, cc.name()+"_"+connectorCount,cc.type, force, result);
		
//		public void startConnector(TopologyContext context, StreamConfiguration streamConfig, ConnectConfiguration config, boolean force) throws IOException {

	}

}