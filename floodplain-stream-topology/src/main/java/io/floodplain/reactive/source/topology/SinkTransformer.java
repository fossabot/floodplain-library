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
package io.floodplain.reactive.source.topology;

import io.floodplain.reactive.source.topology.api.TopologyPipeComponent;
import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.streams.api.TopologyContext;
import io.floodplain.streams.remotejoin.TopologyConstructor;
import io.floodplain.streams.serializer.ConnectKeySerde;
import io.floodplain.streams.serializer.ConnectReplicationMessageSerde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Stack;

public class SinkTransformer implements TopologyPipeComponent {

    private final Optional<String> name;
    private final Optional<Integer> partitions;
    private final boolean materializeParent;
    private final boolean connectFormat;
    private final String topic;


    private final static Logger logger = LoggerFactory.getLogger(SinkTransformer.class);

    public SinkTransformer(Optional<String> name, String topic, boolean materializeParent, Optional<Integer> partitions, boolean connectFormat) {
        this.name = name;
        this.topic = topic;
        this.partitions = partitions;
        this.materializeParent = materializeParent;
        this.connectFormat = connectFormat;
    }

    @Override
    public void addToTopology(Stack<String> transformerNames, int pipeId, Topology topology, TopologyContext topologyContext, TopologyConstructor topologyConstructor) {

        String sinkTopic = topologyContext.topicName(topic);
        topologyConstructor.ensureTopicExists(sinkTopic, partitions);
        String qualifiedName;
        if(name.isPresent()) {
            // TODO effective deconflicting but ugly
            qualifiedName = topologyContext.topicName(name.get()+"_"+topic);
        } else {
            qualifiedName = sinkTopic; //topologyContext.applicationId();
        }

        logger.info("Stack top for transformer: " + transformerNames.peek());
        if(connectFormat) {
            Serializer<String> connectKeySerde = new ConnectKeySerde().serializer();
            Serializer<ReplicationMessage> connectValueSerde = new ConnectReplicationMessageSerde().serializer();
            topology.addSink(qualifiedName, sinkTopic, connectKeySerde, connectValueSerde, transformerNames.peek());
        } else {
            topology.addSink(qualifiedName, sinkTopic, transformerNames.peek());
        }
    }


    @Override
    public boolean materializeParent() {
        return materializeParent;
    }

    @Override
    public void setMaterialize() {
        throw new UnsupportedOperationException("Sinks should never be materialized");
    }


}
