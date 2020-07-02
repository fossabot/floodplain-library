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
package io.floodplain.mongodb

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.kafka.connect.MongoSinkConnector
import com.mongodb.kafka.connect.sink.MongoSinkTask
import io.floodplain.kotlindsl.Config
import io.floodplain.kotlindsl.FloodplainSink
import io.floodplain.kotlindsl.InputReceiver
import io.floodplain.kotlindsl.MaterializedSink
import io.floodplain.kotlindsl.PartialStream
import io.floodplain.kotlindsl.SourceTopic
import io.floodplain.kotlindsl.Stream
import io.floodplain.kotlindsl.Transformer
import io.floodplain.reactive.source.topology.SinkTransformer
import io.floodplain.streams.api.ProcessorName
import io.floodplain.streams.api.Topic
import io.floodplain.streams.api.TopologyContext
import java.util.Optional
import java.util.concurrent.atomic.AtomicLong
import org.apache.kafka.connect.json.JsonDeserializer
import org.apache.kafka.connect.sink.SinkRecord
import org.apache.kafka.connect.sink.SinkTask

private val logger = mu.KotlinLogging.logger {}

class MongoConfig(val name: String, val uri: String, val database: String, private val topologyContext: TopologyContext) : Config {

    var sinkTask: MongoSinkTask? = null
    val sinkInstancePair: MutableList<Pair<String, Topic>> = mutableListOf()
    override fun materializeConnectorConfig(topologyContext: TopologyContext): List<MaterializedSink> {
        val additional = mutableMapOf<String, String>()
        sinkInstancePair.forEach { (key, value) -> additional.put("topic.override.${value.qualifiedString(topologyContext)}.collection", key) }
        logger.debug("Pairs: $sinkInstancePair")
        val collections: String = sinkInstancePair.joinToString(",") { e -> e.first }
        logger.debug("Collections: $collections")
        val topics: String =
            sinkInstancePair.joinToString(",") { (_, topic) -> topic.qualifiedString(topologyContext) }
        logger.debug("Topics: $topics")

        val generationalDatabase = topologyContext.topicName(database)
        val settings = mutableMapOf("connector.class" to "com.mongodb.kafka.connect.MongoSinkConnector",
                "value.converter.schemas.enable" to "false",
                "key.converter.schemas.enable" to "false",
                "value.converter" to "org.apache.kafka.connect.json.JsonConverter",
                "key.converter" to "org.apache.kafka.connect.json.JsonConverter",
                "document.id.strategy" to "com.mongodb.kafka.connect.sink.processor.id.strategy.FullKeyStrategy",
                "delete.on.null.values" to "true",
                "debug" to "true",
                "connection.uri" to uri,
                "database" to generationalDatabase,
                "collection" to collections,
                "topics" to topics)
        settings.putAll(additional)
        settings.forEach { (key, value) ->
            logger.info { "Setting: $key value: $value" }
        }
        return listOf(MaterializedSink(name, sinkInstancePair.map { (_, topic) -> topic }.toList(), settings))
    }

    override fun sourceElements(): List<SourceTopic> {
        return emptyList()
    }

    override suspend fun connectSource(inputReceiver: InputReceiver) {
        throw UnsupportedOperationException("MongoSink can not be used as a source")
    }

    override fun sinkElements(topologyContext: TopologyContext): Map<Topic, List<FloodplainSink>> {
        return materializeConnectorConfig(topologyContext)
            .map {

                val connector = MongoSinkConnector()
                connector.start(it.settings)
                val task = connector.taskClass().getDeclaredConstructor().newInstance() as MongoSinkTask
                task.start(it.settings)
                sinkTask = task

                // sinkInstancePair
                //     .map { (name,topic)-> topic to listOf(MongoFloodplainSink(task, this))}
                //     .toMap()
                val localSink = MongoFloodplainSink(task, this)
                it.topics
                    .map { topic -> topic to listOf(localSink) }
                    .toMap()

                // name to MongoFloodplainSink(task, this)
            }.first() // I know there is only one
    }

    override fun sinkTask(): Any? {
        TODO("Not yet implemented")
    }
}

private class MongoFloodplainSink(private val task: SinkTask, private val config: Config) : FloodplainSink {
    val deserializer = JsonDeserializer()
    var mapper: ObjectMapper = ObjectMapper()

    private val offsetCounter = AtomicLong(System.currentTimeMillis())
    // override fun send(topic: Topic, elements: List<Pair<String, ByteArray?>>, topologyContext: TopologyContext) {
    //     logger.info("Inserting # of documents ${elements.size} for topic: $topic")
    //     val list = elements.map { (key, value) ->
    //         val parsed = deserializer.deserialize(topic.qualifiedString(topologyContext),value) as ObjectNode
    //         var result = mapper.convertValue(parsed, object : TypeReference<Map<String, Any>>() {})
    //         SinkRecord(topic.qualifiedString(topologyContext), 0, null, mapOf(Pair("key", key)), null, result, offsetCounter.incrementAndGet())
    //     }.toList()
    //     task.put(list)
    // }

    override fun send(topic: Topic, elements: List<Pair<String, Map<String, Any>?>>, topologyContext: TopologyContext) {
        // override fun send(docs: List<Triple<Topic, String, IMessage?>>) {
        val list = elements.map { (key, value) ->
            // logger.info("Sending document to elastic. Topic: $topic Key: $key message: $result")
            SinkRecord(topic.qualifiedString(topologyContext), 0, null, key, null, value, offsetCounter.incrementAndGet())
        }.toList()
        task.put(list)
    }

    override fun config(): Config {
        return config
    }

    override fun flush() {
        task.flush(emptyMap())
    }

    override fun close() {
        task.close(emptyList())
    }

    override fun taskObject(): Any? {
        TODO("Not yet implemented")
    }
}
/**
 * Creates a config for this specific connector type, add the required params as needed. This config object will be passed
 * to all sink objects
 */
fun Stream.mongoConfig(name: String, uri: String, database: String): MongoConfig {
    val c = MongoConfig(name, uri, database, this.context)
    this.addSinkConfiguration(c)
    return c
}

fun PartialStream.mongoSink(collection: String, topicDefinition: String, config: MongoConfig) {
    val topic = Topic.from(topicDefinition)
    config.sinkInstancePair.add(collection to topic)
    val sinkName = ProcessorName.from(config.name)
    val sink = SinkTransformer(Optional.of(sinkName), topic, false, Optional.empty(), false, true)
    val transform = Transformer(sink)
    addTransformer(transform)
}
