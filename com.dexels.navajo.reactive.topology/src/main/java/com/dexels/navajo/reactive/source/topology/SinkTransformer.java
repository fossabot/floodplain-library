package com.dexels.navajo.reactive.source.topology;

import static com.dexels.kafka.streams.api.CoreOperators.topicName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.immutable.factory.ImmutableFactory;
import com.dexels.kafka.streams.api.TopologyContext;
import com.dexels.kafka.streams.remotejoin.TopologyConstructor;
import com.dexels.navajo.document.Operand;
import com.dexels.navajo.document.stream.DataItem;
import com.dexels.navajo.document.stream.api.StreamScriptContext;
import com.dexels.navajo.reactive.api.ReactiveParameters;
import com.dexels.navajo.reactive.api.ReactiveParseException;
import com.dexels.navajo.reactive.api.ReactiveResolvedParameters;
import com.dexels.navajo.reactive.api.ReactiveTransformer;
import com.dexels.navajo.reactive.api.TransformerMetadata;
import com.dexels.navajo.reactive.source.topology.api.TopologyPipeComponent;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;

public class SinkTransformer implements ReactiveTransformer, TopologyPipeComponent {

	private TransformerMetadata metadata;
	private ReactiveParameters parameters;
	private boolean materialize = false;

	
	private final static Logger logger = LoggerFactory.getLogger(SinkTransformer.class);

	public static final String SINK_PREFIX = "SINK_";
	public static final String SINKLOG_PREFIX = "SINK_LOG_";
	public SinkTransformer(TransformerMetadata metadata, ReactiveParameters params) {
		this.metadata = metadata;
		this.parameters = params;
	}
	@Override
	public FlowableTransformer<DataItem, DataItem> execute(StreamScriptContext context,
			Optional<ImmutableMessage> current, ImmutableMessage param) {
		return item->Flowable.error(()->new ReactiveParseException("Sink transformer shouldn't be executed"));
	}

	@Override
	public int addToTopology(Stack<String> transformerNames, int pipeId,  Topology topology, TopologyContext topologyContext,TopologyConstructor topologyConstructor) {
		StreamScriptContext context =new StreamScriptContext(topologyContext.tenant.orElse(TopologyContext.DEFAULT_TENANT), topologyContext.instance, topologyContext.deployment);
		ReactiveResolvedParameters resolved = parameters.resolve(context, Optional.empty(), ImmutableFactory.empty(), metadata);
	
		boolean create = resolved.optionalBoolean("create").orElse(false);
		Optional<Integer> partitions = resolved.optionalInteger("partitions");
		List<Operand> operands = resolved.unnamedParameters();
		Optional<String> sinkName = resolved.optionalString("connector");
		for (Operand operand : operands) {
	        String sinkTopic = topicName( operand.stringValue(), topologyContext);
	        // TODO shouldn't we use the createName?
	        // TODO still weird if we use multiple
			if(create) {
				topologyConstructor.ensureTopicExists(sinkTopic,partitions);
			}
			logger.info("Stack top for transformer: "+transformerNames.peek());
			Map<String,String> values = resolved.namedParameters().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e->(String)e.getValue().value));
			Map<String,String> withTopic = new HashMap<>(values);
			withTopic.put("topic", sinkTopic);
			sinkName.ifPresent(sink->topologyConstructor.addConnectSink(sink,sinkTopic, values));
			topology.addSink(SINK_PREFIX+sinkTopic, sinkTopic, transformerNames.peek());
		}
		return pipeId;
	}
	
	private  String createName(int transformerNumber, int pipeId) {
		return pipeId+"_"+metadata.name()+"_"+transformerNumber;
	}
	
	@Override
	public TransformerMetadata metadata() {
		return metadata;
	}
	
	@Override
	public ReactiveParameters parameters() {
		return parameters;
	}
	@Override
	public boolean materializeParent() {
		return false;
	}
	@Override
	public void setMaterialize() {
		this.materialize  = true;
	}

	@Override
	public boolean materialize() {
		return this.materialize;
	}

}
