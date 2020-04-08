package com.dexels.kafka.streams.base;

import com.dexels.kafka.streams.serializer.ReplicationMessageListSerde;
import com.dexels.kafka.streams.serializer.ReplicationMessageSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamOperators {

    public static final int DEFAULT_MAX_LIST_SIZE = 500;
    public static final ReplicationMessageSerde replicationSerde = new ReplicationMessageSerde();
    public static final ReplicationMessageListSerde replicationListSerde = new ReplicationMessageListSerde();


    private static final Logger logger = LoggerFactory.getLogger(StreamOperators.class);

    private StreamOperators() {
        // -- no instances
    }

}
