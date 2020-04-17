package io.floodplain.streams.debezium.impl;

import io.floodplain.pubsub.rx2.api.PubSubMessage;
import io.floodplain.streams.debezium.JSONToReplicationMessage;
import io.floodplain.streams.remotejoin.TopologyConstructor;
import org.apache.kafka.streams.processor.RecordContext;
import org.apache.kafka.streams.processor.TopicNameExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class PubSubTopicNameExtractor implements TopicNameExtractor<String, PubSubMessage> {

    private final TopologyConstructor topologyConstructor;
    private final static Logger logger = LoggerFactory.getLogger(PubSubTopicNameExtractor.class);

    public PubSubTopicNameExtractor(TopologyConstructor topologyConstructor) {
        this.topologyConstructor = topologyConstructor;
    }

    @Override
    public String extract(String key, PubSubMessage msg, RecordContext context) {
        String result = msg.topic().orElse(context.topic());
        logger.info("TOPICNAME extracted: {}", result);

        topologyConstructor.addDesiredTopic(result, Optional.empty());
        return result;
    }

}
