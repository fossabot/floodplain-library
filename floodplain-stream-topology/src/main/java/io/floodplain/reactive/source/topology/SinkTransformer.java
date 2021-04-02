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

import io.floodplain.immutable.api.ImmutableMessage;
import io.floodplain.reactive.source.topology.api.TopologyPipeComponent;
import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.replication.factory.ReplicationFactory;
import io.floodplain.streams.api.ProcessorName;
import io.floodplain.streams.api.Topic;
import io.floodplain.streams.api.TopologyContext;
import io.floodplain.streams.remotejoin.ReplicationTopologyParser;
import io.floodplain.streams.remotejoin.TopologyConstructor;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Stack;

public class SinkTransformer implements TopologyPipeComponent {

    private final Optional<String> name;
    private final Optional<Integer> partitions;
    private final Topic.FloodplainKeyFormat keyFormat;
    private final Topic.FloodplainBodyFormat valueFormat;
    private final Topic topic;


    private final static Logger logger = LoggerFactory.getLogger(SinkTransformer.class);

    private boolean materializeParent = false;
    private boolean debug = false;
    public SinkTransformer(Optional<String> name, Topic topic, Optional<Integer> partitions, Topic.FloodplainKeyFormat keyFormat, Topic.FloodplainBodyFormat valueFormat) {
        this.name = name;
        this.topic = topic;
        this.partitions = partitions;
        this.keyFormat = keyFormat;
        this.valueFormat = valueFormat;
    }

    @Override
    public void addToTopology(Stack<String> transformerNames, int pipeId, Topology topology, TopologyContext topologyContext, TopologyConstructor topologyConstructor) {

        final String qualifiedSinkTopic = topic.qualifiedString();
        topologyConstructor.ensureTopicExists(topic, partitions);
        String qualifiedName;
        // TODO effective deconflicting but ugly
        //topologyContext.applicationId();
        qualifiedName = name.map(processorName -> processorName + "_" + topologyContext.topicName(processorName + "_" + topic.qualifiedString())).orElse(qualifiedSinkTopic);
        topologyConstructor.addSink(qualifiedName);
        logger.info("Stack top for transformer: " + transformerNames.peek());
        Serializer<String> keySerializer = ReplicationTopologyParser.keySerializer(this.keyFormat);
        Serializer<ReplicationMessage> valueSerializer = ReplicationTopologyParser.bodySerializer(this.valueFormat);
        if(debug) {
            debug("debug_"+qualifiedName, topology,keySerializer,valueSerializer,transformerNames);
        }
        topology.addSink(qualifiedName, qualifiedSinkTopic, keySerializer, valueSerializer, transformerNames.peek());

//
//        if(connectFormat) {
//            Serializer<String> connectKeySerde = stringKeys ? Serdes.String().serializer() : new ConnectKeySerde().serializer();
//            Serializer<ReplicationMessage> connectValueSerde = new ConnectReplicationMessageSerde().serializer();
//            topology.addSink(qualifiedName, qualifiedSinkTopic, connectKeySerde, connectValueSerde, transformerNames.peek());
//        } else {
//            topology.addSink(qualifiedName, qualifiedSinkTopic, transformerNames.peek());
//        }
    }

    private void debug(String name, Topology topology, Serializer<String> keySerializer, Serializer<ReplicationMessage> valueSerializer, Stack<String> transformerNames) {
        ImmutableMessage.TriConsumer cons = (key, message, secondary) -> {
            ReplicationMessage m = ReplicationFactory.standardMessage(message);
            byte[] data = valueSerializer.serialize("unknown",m);
            String str = new String(data);
            logger.info("DEBIG: "+str);
        };
        EachProcessor processor = new EachProcessor(cons);
        topology.addProcessor(name, ()->processor,transformerNames.peek());
    }


    @Override
    public boolean materializeParent() {
        return materializeParent;
    }

    @Override
    public void setMaterialize() {
        this.materializeParent = true;
    }


}
