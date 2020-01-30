package com.dexels.navajo.reactive.source.topology;

import static com.dexels.kafka.streams.api.CoreOperators.topicName;

import java.util.List;
import java.util.Optional;
import java.util.Stack;

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
//		String filterName = createName(transformerNames.size(), pipeId);
		StreamScriptContext context =new StreamScriptContext(topologyContext.tenant.orElse(TopologyContext.DEFAULT_TENANT), topologyContext.instance, topologyContext.deployment);
		ReactiveResolvedParameters resolved = parameters.resolve(context, Optional.empty(), ImmutableFactory.empty(), metadata);
//		resolved.unnamedParameters()
//			.stream()
//			.map(e->(String)(e.value))
//			.forEach(sinkName->{
//				System.err.println("Stack top for transformer: "+transformerNames.peek());
//		        String sinkTopic = topicName(sinkName, topologyContext);
//				topology.addSink(sinkTopic, sinkTopic, transformerNames.peek());
//				System.err.println("Sink source >>> "+sinkTopic+" >>> name: "+sinkName);
//			});
		
		boolean create = resolved.optionalBoolean("create").orElse(false);
		Optional<String> logName = resolved.optionalString("logName");
		if(logName.isPresent()) {
			logger.info("Stack top for transformer: "+transformerNames.peek());
			topology.addProcessor(SINKLOG_PREFIX+"sinkTopic", ()->new LogProcessor(logName.get()), transformerNames.peek());
			transformerNames.push(SINKLOG_PREFIX+"sinkTopic");
		}
		Optional<Integer> partitions = resolved.optionalInteger("partitions");
		List<Operand> operands = resolved.unnamedParameters();
		for (Operand operand : operands) {
	        String sinkTopic = topicName( operand.stringValue(), topologyContext);
	        // TODO shouldn't we use the createName?
	        String sinkName = sinkTopic;
			if(create) {
				topologyConstructor.ensureTopicExists(sinkTopic,partitions);
			}
			logger.info("Stack top for transformer: "+transformerNames.peek());
			topology.addSink(SINK_PREFIX+sinkTopic, sinkTopic, transformerNames.peek());
//			transformerNames.push(SINK_PREFIX+sinkTopic);
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