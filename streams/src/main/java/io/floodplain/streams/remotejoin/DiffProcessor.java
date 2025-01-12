/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.floodplain.streams.remotejoin;

import io.floodplain.immutable.api.ImmutableMessage.ValueType;
import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.replication.api.ReplicationMessage.Operation;
import io.floodplain.replication.factory.ReplicationFactory;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class DiffProcessor extends AbstractProcessor<String, ReplicationMessage> {

    private final String lookupStoreName;
    private KeyValueStore<String, ReplicationMessage> lookupStore;

    private final static Logger logger = LoggerFactory.getLogger(DiffProcessor.class);


    public DiffProcessor(String lookupStoreName) {
        this.lookupStoreName = lookupStoreName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(ProcessorContext context) {
        this.lookupStore = (KeyValueStore<String, ReplicationMessage>) context.getStateStore(lookupStoreName);
        super.init(context);
    }

    @Override
    public void close() {

    }

    private ReplicationMessage createMessage(String key) {
        Map<String, Object> value = new HashMap<>();
        value.put("key", key);
        Map<String, ValueType> types = new HashMap<>();
        types.put("key", ValueType.STRING);
        return ReplicationFactory.fromMap(key, value, types).withPrimaryKeys(Collections.singletonList("key"));
    }

    @Override
    public void process(String key, ReplicationMessage incoming) {
        if (incoming == null || incoming.operation() == Operation.DELETE) {
            logger.debug("Delete detected in store: {} with key: {}", lookupStoreName, key);
            ReplicationMessage previous = lookupStore.get(key);
            if (previous != null) {
                lookupStore.delete(key);
                context().forward(key, createMessage(key)
                        .withSubMessage("old", previous.message())
                        .withOperation(Operation.DELETE));
            }
        } else {
            ReplicationMessage previous = lookupStore.get(key);
            if (previous != null) {
                boolean isDifferent = diff(previous, incoming);
                if (isDifferent) {
                    lookupStore.put(key, incoming);
                    context().forward(key, createMessage(key)
                            .withSubMessage("old", previous.message())
                            .withSubMessage("new", incoming.message())
                            .withOperation(Operation.UPDATE));
                } else {
                    logger.debug("Ignoring identical message for key: {} for store: {}", key, lookupStoreName);
                }
            } else {
                // 'new message'
                lookupStore.put(key, incoming);
                context().forward(key, createMessage(key)
                        .withSubMessage("new", incoming.message())
                        .withOperation(Operation.UPDATE));
            }

            lookupStore.put(key, incoming);
        }
    }

    private boolean diff(ReplicationMessage previous, ReplicationMessage incoming) {
        return !previous.equalsToMessage(incoming);
    }

}
