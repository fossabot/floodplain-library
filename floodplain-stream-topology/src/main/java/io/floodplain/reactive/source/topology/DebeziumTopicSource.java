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
import io.floodplain.streams.api.CoreOperators;
import io.floodplain.streams.api.TopologyContext;
import io.floodplain.streams.debezium.impl.DebeziumConversionProcessor;
import io.floodplain.streams.remotejoin.IdentityProcessor;
import io.floodplain.streams.remotejoin.ReplicationTopologyParser;
import io.floodplain.streams.remotejoin.StoreProcessor;
import io.floodplain.streams.remotejoin.TopologyConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;

import java.util.Stack;

public class DebeziumTopicSource implements TopologyPipeComponent {

    private final String table;
    private final String schema;
    private final String resource;
    private final boolean appendTenant;
    private final boolean appendSchema;
    private boolean materialize;

    public DebeziumTopicSource(String resource, String table, String schema, boolean appendTenant, boolean appendSchema) {
        this.resource = resource;
        this.table = table;
        this.schema = schema;
        this.appendTenant = appendTenant;
        this.appendSchema = appendSchema;
    }


    public String topicName(TopologyContext topologyContext) {
        return CoreOperators.topicName(resource + "." + schema + "." + table, topologyContext);

    }

    @Override
    public void addToTopology(Stack<String> transformerNames, int pipeId, Topology topology, TopologyContext topologyContext, TopologyConstructor topologyConstructor) {
        boolean appendTable = false;
        final String metadataName = "debezium";

        String topicName = topicName(topologyContext);


//        topologyConstructor.addConnectSink(resource, topicName, associatedSettings);

//        final String sourceProcessorName = topologyContext.qualifiedName(metadataName + "_debsrc", transformerNames.size(), pipeId);
        final String convertProcessorName = topologyContext.qualifiedName(metadataName + "_debconv", transformerNames.size(), pipeId);
        final String finalProcessorName = topologyContext.qualifiedName(metadataName + "_deb", transformerNames.size(), pipeId);
        ReplicationTopologyParser.addLazySourceStore(topology, topologyContext, topologyConstructor, topicName, Serdes.String().deserializer(), Serdes.ByteArray().deserializer());
        topology.addProcessor(convertProcessorName, () -> new DebeziumConversionProcessor(topicName, topologyContext, appendTenant, appendSchema, appendTable), topicName);

        if (materialize) {
            topology.addProcessor(finalProcessorName, () -> new StoreProcessor(ReplicationTopologyParser.STORE_PREFIX + finalProcessorName), convertProcessorName);
        } else {
            topology.addProcessor(finalProcessorName, () -> new IdentityProcessor(), convertProcessorName);

        }

        if (materialize) {
            ReplicationTopologyParser.addStateStoreMapping(topologyConstructor.processorStateStoreMapper, finalProcessorName, ReplicationTopologyParser.STORE_PREFIX + finalProcessorName);
            topologyConstructor.stores.add(ReplicationTopologyParser.STORE_PREFIX + finalProcessorName);
            topologyConstructor.stateStoreSupplier.put(ReplicationTopologyParser.STORE_PREFIX + finalProcessorName, ReplicationTopologyParser.createMessageStoreSupplier(ReplicationTopologyParser.STORE_PREFIX + finalProcessorName, true));
        }
        transformerNames.push(finalProcessorName);
    }
//
//    private static String processorName(String sourceTopicName) {
//        return sourceTopicName.replace(':', '_').replace('@', '.');
//    }

    @Override
    public boolean materializeParent() {
        return false;
    }

    @Override
    public void setMaterialize() {
        this.materialize = true;
    }

    @Override
    public boolean materialize() {
        return this.materialize;
    }

}