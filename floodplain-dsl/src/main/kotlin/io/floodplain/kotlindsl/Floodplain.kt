@file:Suppress("ImplicitThis")

package io.floodplain.kotlindsl

import com.dexels.immutable.api.ImmutableMessage
import com.dexels.kafka.streams.api.StreamConfiguration
import com.dexels.kafka.streams.api.TopologyContext
import com.dexels.kafka.streams.base.StreamInstance
import com.dexels.kafka.streams.remotejoin.TopologyConstructor
import com.dexels.navajo.reactive.source.topology.*
import com.dexels.navajo.reactive.source.topology.api.TopologyPipeComponent
import com.dexels.navajo.reactive.topology.ReactivePipe
import io.floodplain.kotlindsl.message.IMessage
import io.floodplain.kotlindsl.message.fromImmutable
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import java.io.IOException
import java.io.InputStream
import java.util.*

private val logger = mu.KotlinLogging.logger {}

open class Transformer(val component: TopologyPipeComponent) : PartialPipe() {
}

abstract class Config() {

    abstract fun materializeConnectorConfig(topologyContext: TopologyContext): Pair<String,Map<String,String>>

}
abstract class SinkConfiguration(val name: String) {
}


fun PartialPipe.filter(flt: (IMessage, IMessage) -> Boolean) {
    val transformerFilter: (ImmutableMessage, ImmutableMessage) -> Boolean = { msg: ImmutableMessage, param: ImmutableMessage -> flt.invoke(fromImmutable(msg), fromImmutable(param)) }
    val transformer = FilterTransformer(transformerFilter)
    addTransformer(Transformer(transformer))
}

fun PartialPipe.set(transform: (IMessage, IMessage) -> IMessage): Transformer {
    val transformer: (ImmutableMessage, ImmutableMessage) -> ImmutableMessage = { msg: ImmutableMessage, param: ImmutableMessage -> transform.invoke(fromImmutable(msg), fromImmutable(param)).toImmutable() }
    val set = SetTransformer(transformer)
    return addTransformer(Transformer(set))
}

//fun PartialPipe.copy()
fun PartialPipe.joinRemote(key: (IMessage) -> String, source: () -> Source) {
    val keyExtractor: (ImmutableMessage, ImmutableMessage) -> String = { msg, _ -> key.invoke(fromImmutable(msg)) }
    val jrt = JoinRemoteTransformer(source.invoke().toReactivePipe(), keyExtractor)
    addTransformer(Transformer(jrt))
}

fun PartialPipe.joinWith(source: () -> Source) {
    val jrt = JoinWithTransformer(source.invoke().toReactivePipe(), Optional.empty())
    addTransformer(Transformer(jrt))
}

fun PartialPipe.scan(key: (IMessage) -> String, initial: () -> IMessage, onAdd: Block.() -> Transformer, onRemove: Block.() -> Transformer) {
    val keyExtractor: (ImmutableMessage, ImmutableMessage) -> String = { msg, _ -> key.invoke(fromImmutable(msg)) }
    val onAddBlock = Block()
    onAdd.invoke(onAddBlock)
    val onRemoveBlock = Block()
    onRemove.invoke(onRemoveBlock)
    addTransformer(Transformer(ScanTransformer(keyExtractor, initial.invoke().toImmutable(), onAddBlock.transformers.map { e -> e.component }, onRemoveBlock.transformers.map { e -> e.component })))
}

fun pipe(topologyContext: TopologyContext, topologyConstructor: TopologyConstructor, init: Pipe.() -> Unit): Pipe {
    val pipe = Pipe(topologyContext,topologyConstructor)
    pipe.init()
    return pipe
}

fun pipe(init: Pipe.() -> Unit): Pipe {
    val pipe = Pipe(defaultTopologyContext(), TopologyConstructor(Optional.empty()))
    pipe.init()
    return pipe
}

fun defaultTopologyContext(): TopologyContext {
    return TopologyContext(Optional.of("defaultTenant"),"defaultDeployment","defaultInstance","defaultGeneration")
}
open class Source(val component: TopologyPipeComponent) : PartialPipe() {
    fun toReactivePipe(): ReactivePipe {
        return ReactivePipe(component, transformers.map { e -> e.component })
    }
}


abstract class PartialPipe {
    val transformers: MutableList<Transformer> = mutableListOf()

    fun addTransformer(transformer: Transformer): Transformer {
        transformers.add(transformer)
        return transformer
    }
}

class Block() : PartialPipe() {
}

fun run(pipe: Pipe, tenant: String?, deployment: String, instance: String, generation: String) {
    var topologyContext = TopologyContext(Optional.ofNullable(tenant), deployment, instance, generation)
    var topologyConstructor = TopologyConstructor(Optional.empty())
    val (topology,configs) = pipe.render()

    println("sources: ${topology.describe()}")
}

@Throws(InterruptedException::class, IOException::class)
fun runTopology(topology: Topology, applicationId: String, kafkaHosts: String, storagePath: String): KafkaStreams? {
    val props = StreamInstance.createProperties(applicationId, kafkaHosts, storagePath)
    val stream = KafkaStreams(topology, props)
    println("CurrentTopology:\n ${topology.describe()}")
    stream.setUncaughtExceptionHandler { thread: Thread, exception: Throwable? ->
        logger.error("Error in streams. thread: ${thread.name} exception: ", exception)
        stream.close()
    }
    stream.setStateListener { oldState: KafkaStreams.State?, newState: KafkaStreams.State? -> logger.info("State moving from {} to {}", oldState, newState) }
    stream.start()
    return stream
}
