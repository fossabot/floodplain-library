package io.floodplain.streams.serializer;

import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.replication.factory.ReplicationFactory;
import io.floodplain.replication.impl.protobuf.FallbackReplicationMessageParser;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReplicationMessageListSerde implements Serde<List<ReplicationMessage>> {

    private final FallbackReplicationMessageParser parser = new FallbackReplicationMessageParser();
    private static final Logger logger = LoggerFactory.getLogger(ReplicationMessageListSerde.class);

    public ReplicationMessageListSerde() {
        ReplicationFactory.setInstance(parser);
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        logger.info("Configuring: {}", configs);
    }

    @Override
    public Deserializer<List<ReplicationMessage>> deserializer() {
        return new Deserializer<List<ReplicationMessage>>() {

            @Override
            public void close() {
            }

            @Override
            public void configure(Map<String, ?> config, boolean isKey) {

            }

            @Override
            public List<ReplicationMessage> deserialize(String topic, byte[] data) {
                if (data == null) {
                    return Collections.emptyList();
                }
                return parser.parseMessageList(data);
            }
        };
    }

    @Override
    public Serializer<List<ReplicationMessage>> serializer() {
        return new Serializer<List<ReplicationMessage>>() {

            @Override
            public void close() {

            }

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {

            }

            @Override
            public byte[] serialize(String topic, List<ReplicationMessage> data) {

                return parser.serializeMessageList(data);


            }
        };
    }

}
