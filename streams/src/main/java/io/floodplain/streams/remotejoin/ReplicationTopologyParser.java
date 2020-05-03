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

import io.floodplain.immutable.api.ImmutableMessage;
import io.floodplain.reactive.source.topology.api.TopologyPipeComponent;
import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.replication.api.ReplicationMessage.Operation;
import io.floodplain.streams.api.CoreOperators;
import io.floodplain.streams.api.TopologyContext;
import io.floodplain.streams.remotejoin.ranged.GroupedUpdateProcessor;
import io.floodplain.streams.remotejoin.ranged.ManyToManyGroupedProcessor;
import io.floodplain.streams.remotejoin.ranged.ManyToOneGroupedProcessor;
import io.floodplain.streams.remotejoin.ranged.OneToManyGroupedProcessor;
import io.floodplain.streams.serializer.ImmutableMessageSerde;
import io.floodplain.streams.serializer.ReplicationMessageSerde;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ReplicationTopologyParser {
    public static final String STORE_PREFIX = "STORE_";

    private static final Serde<ReplicationMessage> messageSerde = new ReplicationMessageSerde();
    private static final Serde<ImmutableMessage> immutableMessageSerde = new ImmutableMessageSerde();

    public enum Flatten {FIRST, LAST, NONE};

    private static final Logger logger = LoggerFactory.getLogger(ReplicationTopologyParser.class);

    private ReplicationTopologyParser() {
        // - no instances
    }


    public static final void addStateStoreMapping(Map<String, List<String>> processorStateStoreMapper, String processor, String stateStore) {
        logger.info("Adding processor: {} with statestore: {}", processor, stateStore);
        List<String> parts = processorStateStoreMapper.get(stateStore);
        if (parts == null) {
            parts = new ArrayList<>();
            processorStateStoreMapper.put(stateStore, parts);
        }
        parts.add(processor);
    }

    public static void materializeStateStores(TopologyConstructor topologyConstructor, Topology current) {
        for (Entry<String, List<String>> element : topologyConstructor.processorStateStoreMapper.entrySet()) {
            final String key = element.getKey();
            final StoreBuilder<KeyValueStore<String, ReplicationMessage>> supplier = topologyConstructor.stateStoreSupplier.get(key);
            if (supplier == null) {
                final StoreBuilder<KeyValueStore<String, ImmutableMessage>> immutableSupplier = topologyConstructor.immutableStoreSupplier.get(key);
//				supplier = topologyConstructor.immutableStoreSupplier.get(key);
                if (immutableSupplier != null) {
                    current = current.addStateStore(immutableSupplier, element.getValue().toArray(new String[]{}));
                    logger.info("Added processor: {} with sttstatestores: {} mappings: {}", element.getKey(), element.getValue(), topologyConstructor.processorStateStoreMapper.get(element.getKey()));
                } else {
                    logger.error("Missing supplier for: {}\nStore mappings: {} available suppliers: {}", element.getKey(), topologyConstructor.processorStateStoreMapper, topologyConstructor.immutableStoreSupplier);
                    logger.error("Available state stores: {}\nimm: {}", topologyConstructor.stateStoreSupplier.keySet(), topologyConstructor.immutableStoreSupplier.keySet());
                    throw new RuntimeException("Missing supplier for: " + element.getKey());
                }

            } else {
                current = current.addStateStore(supplier, element.getValue().toArray(new String[]{}));
                logger.info("Added processor: {} with sttstatestores: {} mappings: {}", element.getKey(), element.getValue(), topologyConstructor.processorStateStoreMapper.get(element.getKey()));
            }
        }
    }


    public static void addDiffProcessor(Topology current, TopologyContext context,
                                         TopologyConstructor topologyConstructor, String fromProcessor,
                                         String diffProcessorNamePrefix) {
        current = current.addProcessor(diffProcessorNamePrefix, () -> new DiffProcessor(diffProcessorNamePrefix), fromProcessor);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, diffProcessorNamePrefix, diffProcessorNamePrefix);
        logger.info("Granting access for processor: {} to store: {}", diffProcessorNamePrefix, diffProcessorNamePrefix);
        topologyConstructor.stateStoreSupplier.put(diffProcessorNamePrefix, createMessageStoreSupplier(diffProcessorNamePrefix, true));
    }

    public static String addLazySourceStore(final Topology currentBuilder, TopologyContext context,
                                            TopologyConstructor topologyConstructor, String topicName, Deserializer<?> keyDeserializer, Deserializer<?> valueDeserializer) {
        topologyConstructor.addDesiredTopic(topicName, Optional.empty());
        if (!topologyConstructor.sources.containsKey(topicName)) {
            currentBuilder.addSource(topicName, keyDeserializer, valueDeserializer, topicName);
            topologyConstructor.sources.put(topicName, topicName);
            // TODO Optimize. The topology should be valid without adding identityprocessors
        }
        return topicName;
    }

    //    ss
    public static String addMaterializeStore(final Topology currentBuilder, TopologyContext context,
                                             TopologyConstructor topologyConstructor, String name, String parentProcessor) {
        final String sourceProcessorName = name;
        currentBuilder.addProcessor(name, () -> new StoreProcessor(STORE_PREFIX + sourceProcessorName), parentProcessor);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, sourceProcessorName, STORE_PREFIX + sourceProcessorName);
        topologyConstructor.stores.add(STORE_PREFIX + sourceProcessorName);
        topologyConstructor.stateStoreSupplier.put(STORE_PREFIX + sourceProcessorName, createMessageStoreSupplier(STORE_PREFIX + sourceProcessorName, true));
        return name;
    }

    public static String addSourceStore(final Topology currentBuilder, TopologyContext context, TopologyConstructor topologyConstructor, Optional<ProcessorSupplier<String, ReplicationMessage>> processorFromChildren,
                                        String sourceTopicName,
                                        boolean materializeStore) {
        String storeTopic = CoreOperators.topicName(sourceTopicName, context);
        // TODO It might be better to fail if the topic does not exist? -> Well depends, if it is external yes, but if it is created by the same instance, then no.
        final String sourceProcessorName = storeTopic;
        if (storeTopic != null) {
            String sourceName;
            if (!topologyConstructor.sources.containsKey(storeTopic)) {
                sourceName = sourceProcessorName + "_src";
                currentBuilder.addSource(sourceName, storeTopic);
                topologyConstructor.sources.put(storeTopic, sourceName);
                if (processorFromChildren.isPresent()) {
                    if (materializeStore) {
                        currentBuilder.addProcessor(sourceProcessorName + "_transform", processorFromChildren.get(), sourceName);
                        currentBuilder.addProcessor(sourceProcessorName, () -> new StoreProcessor(STORE_PREFIX + sourceProcessorName), sourceProcessorName + "_transform");
                    } else {
                        currentBuilder.addProcessor(sourceProcessorName, processorFromChildren.get(), sourceName);

                    }
                } else {
                    if (materializeStore) {
                        currentBuilder.addProcessor(sourceProcessorName, () -> new StoreProcessor(STORE_PREFIX + sourceProcessorName), sourceName);
                    } else {
                        currentBuilder.addProcessor(sourceProcessorName, () -> new IdentityProcessor(), sourceName);

                    }
                }

            } else {
                sourceName = topologyConstructor.sources.get(storeTopic);
            }

        }
        if (materializeStore) {
            addStateStoreMapping(topologyConstructor.processorStateStoreMapper, sourceProcessorName, STORE_PREFIX + sourceProcessorName);
            topologyConstructor.stores.add(STORE_PREFIX + sourceProcessorName);
            topologyConstructor.stateStoreSupplier.put(STORE_PREFIX + sourceProcessorName, createMessageStoreSupplier(STORE_PREFIX + sourceProcessorName, true));
        }

        logger.info("Granting access for processor: {} to store: {}", sourceProcessorName, STORE_PREFIX + storeTopic);

        return sourceProcessorName;
    }

    private static Flatten parseFlatten(String flatten) {
        if (flatten == null) {
            return Flatten.NONE;
        }
        if ("true".equals(flatten) || "first".equals(flatten)) {
            return Flatten.FIRST;
        }
        if ("last".equals(flatten)) {
            return Flatten.LAST;
        }
        return Flatten.NONE;
    }

    public static String addSingleJoinGrouped(final Topology current, TopologyContext topologyContext,
                                              TopologyConstructor topologyConstructor, String fromProcessor, String name,
                                              Optional<Predicate<String, ReplicationMessage>> associationBypass,
                                              boolean isList, String withProcessor, boolean optional) {

        String firstNamePre = name + "-forwardpre";
        String secondNamePre = name + "-reversepre";
        String finalJoin = name + "-joined";

        //Preprocessor - add info whether the resulting message is a reverse-join or not
        current.addProcessor(
                firstNamePre
                , () -> new PreJoinProcessor(false)
                , fromProcessor
        ).addProcessor(
                secondNamePre
                , () -> new PreJoinProcessor(true)
                , withProcessor
        ).addProcessor(
                finalJoin
                , () -> (!isList) ?
                        new ManyToOneGroupedProcessor(
                                fromProcessor,
                                withProcessor,
                                associationBypass,
                                optional
                        )
                        :
                        new ManyToManyGroupedProcessor(
                                fromProcessor,
                                withProcessor,
                                associationBypass,
                                optional
                        )
                , firstNamePre, secondNamePre
        );
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, finalJoin, STORE_PREFIX + withProcessor);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, finalJoin, STORE_PREFIX + fromProcessor);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, name, STORE_PREFIX + name);
        topologyConstructor.stores.add(STORE_PREFIX + withProcessor);
        topologyConstructor.stores.add(STORE_PREFIX + fromProcessor);
        topologyConstructor.stores.add(STORE_PREFIX + name);

        topologyConstructor.stateStoreSupplier.put(STORE_PREFIX + name, createMessageStoreSupplier(STORE_PREFIX + name, true));
        current.addProcessor(name, () -> new StoreProcessor(STORE_PREFIX + name), finalJoin);
        return finalJoin;
    }


    public static String addGroupedProcessor(final Topology current, TopologyContext topologyContext, TopologyConstructor topologyConstructor, String name, String from, boolean ignoreOriginalKey,
                                             Function<ReplicationMessage, String> keyExtractor, Optional<ProcessorSupplier<String, ReplicationMessage>> transformerSupplier) {

        String mappingStoreName;
        if (!topologyConstructor.stores.contains(STORE_PREFIX + from)) {
            logger.error("Adding grouped with from, no source processor present for: " + from + " created: " + topologyConstructor.stateStoreSupplier.keySet() + " and from: " + from);
        }
        mappingStoreName = from + "_mapping";

        String transformProcessor = name + "_transform";
        current.addProcessor(transformProcessor, transformerSupplier.orElse(() -> new IdentityProcessor()), from);
        // allow override to avoid clashes
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, name, STORE_PREFIX + name);
        topologyConstructor.stores.add(STORE_PREFIX + name);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, name, STORE_PREFIX + mappingStoreName);
        topologyConstructor.stores.add(STORE_PREFIX + mappingStoreName);
        topologyConstructor.stateStoreSupplier.put(STORE_PREFIX + name, createMessageStoreSupplier(STORE_PREFIX + name, true));
        topologyConstructor.stateStoreSupplier.put(STORE_PREFIX + mappingStoreName, createMessageStoreSupplier(STORE_PREFIX + mappingStoreName, true));
        current.addProcessor(name, () -> new GroupedUpdateProcessor(STORE_PREFIX + name, keyExtractor, STORE_PREFIX + mappingStoreName, ignoreOriginalKey), transformProcessor);
        return name;
    }


    public static void addPersistentCache(Topology current, TopologyContext topologyContext,
                                          TopologyConstructor topologyConstructor, String name, String fromProcessorName, Duration cacheTime,
                                          int maxSize, boolean inMemory) {
        current.addProcessor(
                name
                , () -> new CacheProcessor(name, cacheTime, maxSize, inMemory)
                , fromProcessorName
        );
        logger.info("Buffer using statestore: {}",STORE_PREFIX+name);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, name, STORE_PREFIX+name);
        topologyConstructor.stateStoreSupplier.put(STORE_PREFIX+name, createMessageStoreSupplier(STORE_PREFIX+name, true));
    }

    public static String addReducer(final Topology topology, TopologyContext topologyContext, TopologyConstructor topologyConstructor,
                                    String namespace, Stack<String> transformerNames, int currentPipeId, List<TopologyPipeComponent> onAdd, List<TopologyPipeComponent> onRemove,
                                    Function<ImmutableMessage,ImmutableMessage> initialMessage, boolean materialize, Optional<BiFunction<ImmutableMessage, ImmutableMessage, String>> keyExtractor) {

        String parentName = transformerNames.peek();
        String reduceReader = topologyContext.qualifiedName("reduce", transformerNames.size(), currentPipeId);
        transformerNames.push(reduceReader);
        String ifElseName = topologyContext.qualifiedName("ifelse", transformerNames.size(), currentPipeId);
        transformerNames.push(ifElseName);
        int trueBranchPipeId = topologyConstructor.generateNewPipeId();
        int falseBranchPipeId = topologyConstructor.generateNewPipeId();

        String trueBranchName = topologyContext.qualifiedName("addbranch", transformerNames.size(), currentPipeId);
        String falseBranchName = topologyContext.qualifiedName("removeBranch", transformerNames.size(), currentPipeId);

        String reduceName = topologyContext.qualifiedName("reduce", transformerNames.size(), currentPipeId);

        String reduceStoreName = STORE_PREFIX +"accumulator_"+ reduceName;
        String inputStoreName = STORE_PREFIX + parentName + "_reduce_inputstore";

        topology.addProcessor(reduceReader, () -> new ReduceReadProcessor(inputStoreName, reduceStoreName, initialMessage, keyExtractor), parentName);
        topology.addProcessor(ifElseName, () -> new IfElseProcessor(msg -> msg.operation() != Operation.DELETE, trueBranchName, Optional.of(falseBranchName)), reduceReader);

        Stack<String> addProcessorStack = new Stack<>();
        addProcessorStack.addAll(transformerNames);

        topology.addProcessor(trueBranchName, () -> new IdentityProcessor(), addProcessorStack.peek());
        addProcessorStack.push(trueBranchName);


        Stack<String> removeProcessorStack = new Stack<>();
        removeProcessorStack.addAll(transformerNames);
        topology.addProcessor(falseBranchName, () -> new IdentityProcessor(), removeProcessorStack.peek());
        removeProcessorStack.push(falseBranchName);

        for (TopologyPipeComponent addBranchComponents : onAdd) {
            addBranchComponents.addToTopology(addProcessorStack, trueBranchPipeId, topology, topologyContext, topologyConstructor);
        }
        for (TopologyPipeComponent removePipeComponents : onRemove) {
            removePipeComponents.addToTopology(removeProcessorStack, falseBranchPipeId, topology, topologyContext, topologyConstructor);
        }
//		topologyConstructor
        topology.addProcessor(materialize ? "_proc" + reduceName : reduceName, () -> new StoreStateProcessor(reduceName, reduceStoreName, initialMessage, keyExtractor), addProcessorStack.peek(), removeProcessorStack.peek());
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, materialize ? "_proc" + reduceName : reduceName, reduceStoreName);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, reduceReader, reduceStoreName);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, reduceReader, inputStoreName);

        if (!topologyConstructor.immutableStoreSupplier.containsKey(reduceStoreName)) {
            topologyConstructor.immutableStoreSupplier.put(reduceStoreName, createImmutableMessageSupplier(reduceStoreName, false));
        }
        if (!topologyConstructor.stateStoreSupplier.containsKey(inputStoreName)) {
            topologyConstructor.stateStoreSupplier.put(inputStoreName, createMessageStoreSupplier(inputStoreName, false));
        }
        if (materialize) {
            addMaterializeStore(topology, topologyContext, topologyConstructor, reduceName, "_proc" + reduceName);
        }
        return reduceName;
    }

    public static Topology addJoin(final Topology current, TopologyContext topologyContext,
                                   TopologyConstructor topologyConstructor, String fromProcessorName, String withProcessorName, String name,
                                   boolean optional,
                                   boolean multiple,
                                   boolean materialize,
                                   boolean debug) {
        String firstNamePre = name + "-forwardpre";
        String secondNamePre = name + "-reversepre";

        //Preprocessor - add info whether the resulting message is a reverse-join or not
        current.addProcessor(
                firstNamePre
                , () -> new PreJoinProcessor(false)
                , fromProcessorName
        ).addProcessor(
                secondNamePre
                , () -> new PreJoinProcessor(true)
                , withProcessorName
        );

        @SuppressWarnings("rawtypes") final Processor proc;
        if (multiple) {
            proc = new OneToManyGroupedProcessor(
                    STORE_PREFIX + fromProcessorName,
                    STORE_PREFIX + withProcessorName,
                    optional,
                    CoreOperators.getListJoinFunctionToParam(false),
                    debug
            );
        } else {
            proc = new OneToOneProcessor(
                    STORE_PREFIX + fromProcessorName,
                    STORE_PREFIX + withProcessorName,
                    optional,
                    (msg, comsg) -> msg.withParamMessage(comsg.message()),debug);
        }
        String procName = materialize ? "proc_" + name : name;
        current.addProcessor(
                procName
                , () -> proc
                , firstNamePre, secondNamePre
        );
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, procName, STORE_PREFIX + withProcessorName);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, procName, STORE_PREFIX + fromProcessorName);
        if (materialize) {
            topologyConstructor.stores.add(STORE_PREFIX + name);
            topologyConstructor.stateStoreSupplier.put(STORE_PREFIX + name, createMessageStoreSupplier(STORE_PREFIX + name, true));
            addStateStoreMapping(topologyConstructor.processorStateStoreMapper, name, STORE_PREFIX + name);
            current.addProcessor(name, () -> new StoreProcessor(STORE_PREFIX + name), procName);

        }
        return current;
    }

    public static StoreBuilder<KeyValueStore<String, ReplicationMessage>> createMessageStoreSupplier(String name, boolean persistent) {
        logger.info("Creating messagestore supplier: {}", name);
        KeyValueBytesStoreSupplier storeSupplier = persistent ? Stores.persistentKeyValueStore(name) : Stores.inMemoryKeyValueStore(name);
        return Stores.keyValueStoreBuilder(storeSupplier, Serdes.String(), messageSerde);
    }

    public static StoreBuilder<KeyValueStore<String, ImmutableMessage>> createImmutableMessageSupplier(String name, boolean persistent) {
        logger.info("Creating messagestore supplier: {}", name);
        KeyValueBytesStoreSupplier storeSupplier = persistent ? Stores.persistentKeyValueStore(name) : Stores.inMemoryKeyValueStore(name);
        return Stores.keyValueStoreBuilder(storeSupplier, Serdes.String(), immutableMessageSerde);
    }

    public static StoreBuilder<KeyValueStore<String, Long>> createKeyRowStoreSupplier(String name) {
        logger.info("Creating key/long supplier: {}", name);
        KeyValueBytesStoreSupplier storeSupplier = Stores.inMemoryKeyValueStore(name);
        return Stores.keyValueStoreBuilder(storeSupplier, Serdes.String(), Serdes.Long());
    }

    public static String addKeyRowProcessor(Topology current, TopologyContext context,
                                            TopologyConstructor topologyConstructor, String fromProcessor,
                                            String name, boolean materialize) {
        current = current.addProcessor(name, () -> new RowNumberProcessor(STORE_PREFIX + name), fromProcessor);
        addStateStoreMapping(topologyConstructor.processorStateStoreMapper, name, STORE_PREFIX + name);
        logger.info("Granting access for processor: {} to store: {}", name, STORE_PREFIX + name);
        topologyConstructor.stateStoreSupplier.put(STORE_PREFIX + name, createMessageStoreSupplier(STORE_PREFIX + name, false));
        if (materialize) {
            throw new UnsupportedOperationException("Sorry, didn't implement materialization yet");
        }
        return name;
    }

}