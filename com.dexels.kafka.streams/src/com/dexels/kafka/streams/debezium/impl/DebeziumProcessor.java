package com.dexels.kafka.streams.debezium.impl;

import static com.dexels.kafka.streams.api.CoreOperators.topicName;

import java.util.Map;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.Topology;
import org.osgi.service.component.annotations.Component;

import com.dexels.kafka.streams.api.TopologyContext;
import com.dexels.kafka.streams.base.StreamConfiguration;
import com.dexels.kafka.streams.processor.generic.GenericProcessorBuilder;
import com.dexels.pubsub.rx2.api.PubSubMessage;

@Component(name="dexels.debezium.processor",property={"name=dexels.debezium.processor"})
public class DebeziumProcessor implements GenericProcessorBuilder {

	
//	String source = element.getStringAttribute("source");
//	String group = element.getStringAttribute("group");
//	String destination = element.getStringAttribute("destination");

	private static String processorName(String sourceTopicName) {
        return sourceTopicName.replace(':',  '_').replace('@', '.');
    }

	
	@Override
	public void build(Topology topology, Map<String,String> config, TopologyContext context, StreamConfiguration streamConfig) {
		String source = config.get("source");
		String group = config.get("group");
		String destination = config.get("destination");
		boolean appendTenant = false;
		boolean appendSchema = false;
		
        String sourceTopic = topicName(source, context);
        final String sourceProcessorName = processorName(sourceTopic+"_debezium_conversion_source");
        final String convertProcessorName = processorName(sourceTopic+"_debezium_conversion");
        final String sinkProcessorName = processorName(sourceTopic+"_debezium_conversion_sink");
        Serializer<PubSubMessage> ser = new PubSubSerializer();
        
//		Serd
		topology.addSource(sourceProcessorName,Serdes.String().deserializer(),Serdes.ByteArray().deserializer(), sourceTopic);
		topology.addProcessor(convertProcessorName, ()->new DebeziumConversionProcessor(sourceTopic,context, appendTenant, appendSchema), sourceProcessorName);
		topology.addSink(sinkProcessorName, new PubSubTopicNameExtractor(),Serdes.String().serializer(), ser, convertProcessorName);
//		DebeziumComponent component = new DebeziumComponent(group,source,destination,streamConfig,context,appendTenant,appendSchema);
//		return ()->component.disposable().dispose();
	}

}
