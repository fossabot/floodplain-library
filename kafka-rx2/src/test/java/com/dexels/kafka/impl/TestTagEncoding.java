package com.dexels.kafka.impl;

import com.dexels.kafka.factory.KafkaClientFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TestTagEncoding {
    private KafkaTopicSubscriber kts;

    @Before
    public void setup() {
        Map<String, String> config = new HashMap<>();
        config.put("wait", "5000");
        config.put("max", "50");
        kts = (KafkaTopicSubscriber) KafkaClientFactory.createSubscriber(System.getenv("KAFKA_DEVELOP"), config);
    }

    @Test
    @Ignore
    public void testTopicEncode() {
        final String initialTag = "0:1,1:1";
        Function<Integer, Long> offset = kts.decodeTopicTag(initialTag);
        String finalTag = kts.encodeTopicTag(offset, Arrays.asList(new Integer[]{0, 1}));
        Assert.assertEquals(initialTag, finalTag);

    }
}
