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

import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.replication.api.ReplicationMessage.Operation;
import io.floodplain.replication.factory.ReplicationFactory;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class OneToOneProcessor extends AbstractProcessor<String, ReplicationMessage> {

    private final String forwardLookupStoreName;
    private final String reverseLookupStoreName;
    private KeyValueStore<String, ReplicationMessage> forwardLookupStore;
    private KeyValueStore<String, ReplicationMessage> reverseLookupStore;

    private final BiFunction<ReplicationMessage, ReplicationMessage, ReplicationMessage> joinFunction;
    private final boolean optional;
    private final boolean debug;

    private static final Logger logger = LoggerFactory.getLogger(OneToOneProcessor.class);

    public OneToOneProcessor(String forwardLookupStoreName, String reverseLookupStoreName, boolean optional,
                             BiFunction<ReplicationMessage, ReplicationMessage, ReplicationMessage> joinFunction, boolean debug) {
        this.forwardLookupStoreName = forwardLookupStoreName;
        this.reverseLookupStoreName = reverseLookupStoreName;
        this.optional = optional;
        this.joinFunction = joinFunction;
        this.debug = debug;
    }


    @SuppressWarnings("unchecked")
    @Override
    public void init(ProcessorContext context) {
        logger.info("inner lookup Looking up: " + forwardLookupStoreName);
        this.forwardLookupStore = (KeyValueStore<String, ReplicationMessage>) context.getStateStore(forwardLookupStoreName);
        logger.info("inner lookup Looking up: " + reverseLookupStoreName);
        this.reverseLookupStore = (KeyValueStore<String, ReplicationMessage>) context.getStateStore(reverseLookupStoreName);
        super.init(context);
        logger.info("One-to-one successfully started");
    }

    @Override
    public void process(String key, ReplicationMessage innerMessage) {
        boolean reverse = false;
        if (innerMessage == null) {
            context().forward(key, null);
            return;
        }
        KeyValueStore<String, ReplicationMessage> lookupStore = reverseLookupStore;
        if (key.endsWith(PreJoinProcessor.REVERSE_IDENTIFIER)) {
            reverse = true;
            key = key.substring(0, key.length() - PreJoinProcessor.REVERSE_IDENTIFIER.length());
            lookupStore = forwardLookupStore;
        }
        if (debug) {
            logger.info("Joining key: {} reverse: {}", key, reverse);
        }

        ReplicationMessage counterpart = lookupStore.get(key);
        if (debug) {
            if (counterpart == null) {
                logger.info("Null Join (reverse? {}) key: {} lookupsize: {}", reverse, key, lookupStore.approximateNumEntries());
                List<String> keys = new ArrayList<>();
                lookupStore.all().forEachRemaining(k -> keys.add(k.key));
                logger.info("Keys: {}", String.join(",", keys));
            } else {
                logger.info("Join Result: {} {}", key, counterpart.toFlatString(ReplicationFactory.getInstance()));
            }
        }

        if (counterpart == null) {
            if (reverse) {
                // We are doing a reverse join, but the original message isn't there.
                // Nothing to do for us here
            } else if (optional) {
                context().forward(key, innerMessage);
            }
            return;
        }
        ReplicationMessage msg;
        if (reverse) {
            if (innerMessage.operation() == Operation.DELETE && !optional) {
                // Reverse join  - the message we join with is deleted, and we are not optional
                // This means we should forward a delete too for the forward-join message
                context().forward(key, counterpart.withOperation(Operation.DELETE));
                context().forward(key, null);
            } else if (innerMessage.operation() == Operation.DELETE) {
                // The message we join with is gone, but we are optional. Forward the forward-join message as-is
                context().forward(key, counterpart);
            } else {
                // Regular reverse join
                msg = joinFunction.apply(counterpart, innerMessage);
                context().forward(key, msg);
            }
        } else {
            // Operation DELETE doesn't really matter for forward join - we can join as usual
            // The DELETE operation will be preserved and forwarded
            // TODO Shouldn't we delete from the store? I think now
            msg = joinFunction.apply(innerMessage, counterpart);
            context().forward(key, msg);
        }
    }

    @Override
    public void close() {
        super.close();
    }
}
