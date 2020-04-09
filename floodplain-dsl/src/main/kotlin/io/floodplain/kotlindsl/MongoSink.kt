package io.floodplain.kotlindsl

import com.dexels.kafka.streams.api.CoreOperators
import com.dexels.kafka.streams.api.TopologyContext
import com.dexels.navajo.reactive.source.topology.SinkTransformer
import java.util.*

class MongoConfig(val name: String, val uri: String, val database: String): Config() {

    val sinkInstancePair: MutableList<Pair<String,String>> = mutableListOf()
    override fun materializeConnectorConfig(topologyContext: TopologyContext): Pair<String,Map<String, String>> {
        println("Pairs: $sinkInstancePair")
        val collections: String = sinkInstancePair.map { e->e.first }.joinToString ("," )
        val topics: String = sinkInstancePair.map {r->CoreOperators.topicName(r.second,topologyContext)}.joinToString ( "," )
        val generationalDatabase = CoreOperators.generationalGroup(database,topologyContext)
        return name to mapOf("connector.class" to "com.mongodb.kafka.connect.MongoSinkConnector",
                "value.converter.schemas.enable" to "false",
                "key.converter.schemas.enable" to "false",
                "value.converter" to "com.dexels.kafka.converter.ReplicationMessageConverter",
               "key.converter" to "com.dexels.kafka.converter.ReplicationMessageConverter",
                "document.id.strategy" to "com.mongodb.kafka.connect.sink.processor.id.strategy.FullKeyStrategy",
                "connection.uri" to uri,
                "database" to generationalDatabase,
                "collections" to collections,
                "topics" to topics)


    }
}

fun Pipe.mongoConfig(name: String, uri: String, database: String): MongoConfig {
    val c = MongoConfig(name, uri, database)
    this.addSinkConfiguration(c)
    return c
}


fun PartialPipe.mongoSink(topologyContext: TopologyContext, collection: String, topic: String, config: MongoConfig) {
    config.sinkInstancePair.add(collection to topic)
    val sink = SinkTransformer(topic, Optional.empty())
    addTransformer(Transformer(sink))
}