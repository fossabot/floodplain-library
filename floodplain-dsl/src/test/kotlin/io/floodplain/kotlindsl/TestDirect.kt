package io.floodplain.kotlindsl

import io.floodplain.kotlindsl.message.fromImmutable
import io.floodplain.postgresDataSource
import io.floodplain.replication.api.ReplicationMessageParser
import io.floodplain.replication.factory.ReplicationFactory
import io.floodplain.replication.impl.json.JSONReplicationMessageParserImpl
import io.floodplain.streams.debezium.JSONToReplicationMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Optional
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals

private val logger = mu.KotlinLogging.logger {}

private val parser: ReplicationMessageParser = JSONReplicationMessageParserImpl()

class TestDirect {
    /**
     * Test the simplest imaginable pipe: One source and one sink.
     */
    @Test
    fun testPostgresSource() {
        stream {
            val pgConfig = postgresSourceConfig("mypostgres", "localhost", 5432, "postgres", "mysecretpassword", "dvdrental", "public")
            pgConfig.source("city") {
                sink("topic")
            }
        }.renderAndTest {
            val ctx = this
            var count: Int = 0
            logger.info("Sources found: ${sources().keys}")

            ctx.sources().forEach { (k, v) ->
                println("soure: $k")
                v.collect { record ->
                    println(">>> ${record.topic} ${record.key} ${record.value}"); count++
                }
            }
            delay(38000)
            assertEquals(2, outputSize("topic"))
            logger.info("TopologySources: ${topologyConstructor().desiredTopicNames()}")
        }.start()
        runBlocking {
            println("job started")
            delay(12000)
        }
        // TODO Continue
    }


    @Test(expected = CancellationException::class)
    fun testPostgresSourceSimple() {
        ReplicationFactory.setInstance(parser);
        val tempPath = Paths.get("path_${UUID.randomUUID()}")
        runBlocking {
            launch {
                postgresDataSource( "mypostgres", "localhost", 5432, "dvdrental", "postgres", "mysecretpassword", tempPath)
                     .map {
                            record ->
                        val kv = JSONToReplicationMessage.parse(record.key, record.value.toByteArray())
                        println("<<<<|>>>> ${kv.key}")

                        val msg = parser.parseBytes(Optional.empty<String>(), kv.value)
                        kv.key to msg
                    }
                    .collect {
                        println(it.first)
                    }
            }
            delay(10000)
            cancel("done")
        }
        Files.deleteIfExists(tempPath)
    }


    @Test
    fun testPostgresSourceBroadcast() {
        // CoRoutine
        val path = Paths.get("_someoffsetpath"+ UUID.randomUUID())

        val scope = CoroutineScope(EmptyCoroutineContext)
        val broadcastFlow = postgresDataSource("mypostgres", "localhost", 5432, "dvdrental", "postgres", "mysecretpassword", path)
            .broadcastIn(scope)
            .asFlow()
            // .onEach {
            //     println("topic::: ${it.topic}")
            // }

        val count = AtomicLong()
        runBlocking {
            val broadcastJob = launch {
                broadcastFlow
                    .broadcastIn(GlobalScope)
                    .asFlow()
            }
            val connector = launch {
                val job1 = GlobalScope.launch {
                    broadcastFlow
                        .filter { "mypostgres.public.city" == it.topic }
                        .collect {
                            println("Flow 1 ${it.topic} : ${it.key}")
                        }
                }
                println("Flow 1 kicked")
                val job2 = GlobalScope.launch {
                    broadcastFlow
                        .filter { "mypostgres.public.country" == it.topic }
                        .onEach {
                            val ll = count.incrementAndGet()
                            // logger.info { "Broadcast count: $ll" }
                        }
                        .collect {
                            println("Flow 2 ${it.topic} : ${it.key}")
                        }
                }

                delay(15000)
                broadcastJob.cancel("done")
            }
            connector.join()
            logger.info("done!!!")
            Files.deleteIfExists(path)
        }
    }


}