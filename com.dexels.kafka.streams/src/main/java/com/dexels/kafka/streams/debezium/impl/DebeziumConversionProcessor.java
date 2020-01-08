package com.dexels.kafka.streams.debezium.impl;

import java.util.Optional;

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

import com.dexels.kafka.streams.api.TopologyContext;
import com.dexels.kafka.streams.debezium.JSONToReplicationMessage;
import com.dexels.pubsub.rx2.api.PubSubMessage;
import com.dexels.pubsub.rx2.factory.PubSubTools;

public class DebeziumConversionProcessor implements Processor<String, byte[]> {

	private ProcessorContext processorContext;
	private final String topic;
	private final boolean appendTenant;
	private final boolean appendSchema;
	private final TopologyContext context;

	public DebeziumConversionProcessor(String topic, TopologyContext context,boolean appendTenant, boolean appendSchema) {
		this.topic = topic;
		this.context = context;
		this.appendTenant = appendTenant;
		this.appendSchema = appendSchema;
	}
	
	@Override
	public void close() {
		
	}

	@Override
	public void init(ProcessorContext processorContext) {
		this.processorContext = processorContext;
		
	}

	@Override
	public void process(String key, byte[] value) {

		PubSubMessage psm = PubSubTools.create(key, value, this.processorContext.timestamp(), Optional.of(topic),Optional.of(this.processorContext.partition()),Optional.of(this.processorContext.offset()));
		final PubSubMessage parse = JSONToReplicationMessage.parse(this.context,psm,appendTenant,appendSchema);
//		System.err.println("Parsed key: "+parse.key());
		processorContext.forward(parse.key(), parse);
//		processorContext.commit();
				
	}

}