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
package io.floodplain.integration

import com.mongodb.client.MongoClients
import io.floodplain.kotlindsl.each
import io.floodplain.kotlindsl.postgresSource
import io.floodplain.kotlindsl.postgresSourceConfig
import io.floodplain.kotlindsl.stream
import io.floodplain.mongodb.mongoConfig
import io.floodplain.mongodb.toMongo
import io.floodplain.test.InstantiatedContainer
import io.floodplain.test.InstantiatedKafkaContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.testcontainers.containers.Network
import java.net.URL
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals

private val logger = mu.KotlinLogging.logger {}

class FilmToMongoIntegratedSink {

    private val containerNetwork = Network.newNetwork()
    private val kafkaContainer = InstantiatedKafkaContainer { it.withNetwork(containerNetwork).withNetworkAliases("kafka") } // KafkaContainer("5.5.3").withEmbeddedZookeeper().withExposedPorts(9092)
    private val postgresContainer = InstantiatedContainer("floodplain/floodplain-postgres-demo:1.0.0", 5432, mapOf()) { it.withNetwork(containerNetwork).withNetworkAliases("postgres") }
    private val mongoContainer = InstantiatedContainer("mongo:latest", 27017, mapOf()) { it.withNetwork(containerNetwork).withNetworkAliases("mongo") }
    private var debeziumContainer: InstantiatedContainer? = null

    @Before
    fun setup() {
        val bootstrap = "${kafkaContainer.host}:${kafkaContainer.exposedPort}"
        logger.info("kafka.getBootstrapServers(): ${kafkaContainer.container.bootstrapServers} bootstrap: $bootstrap")

        debeziumContainer = InstantiatedContainer(
            "debezium/connect:1.5",
            8083,
            mapOf(
                "BOOTSTRAP_SERVERS" to "kafka:9092",
                "CONFIG_STORAGE_TOPIC" to "CONNECTOR_STORAGE",
                "OFFSET_STORAGE_TOPIC" to "OFFSET_STORAGE"
            )
        ) {
            it.withNetwork(containerNetwork)
                .withNetworkAliases("debezium")
        }
        debeziumContainer?.container?.start()
        logger.info("Setup done")
        Thread.sleep(20000)
    }

    @After
    fun shutdown() {
        postgresContainer.close()
        mongoContainer.close()
        kafkaContainer.close()
        debeziumContainer?.close()
    }

    /**
     * Test the simplest imaginable pipe: One source and one sink.
     */
    @Test
    fun testPostgresRunLocal() {
        stream {
            val postgresConfig = postgresSourceConfig(
                "mypostgres",
                "postgres",
                5432,
                "postgres",
                "mysecretpassword",
                "dvdrental",
                "public"
            )
            val mongoConfig = mongoConfig(
                "mongosink",
                "mongodb://localhost:${mongoContainer.exposedPort}",
                "mongodump"
            )
            postgresSource("film", postgresConfig) {
                each { _,m,_ ->
                    logger.info("Film: $m")
                }
                toMongo("filmwithactors", "somtopic", mongoConfig)
            }
        }.renderAndSchedule(URL("http://${debeziumContainer?.host}:${debeziumContainer?.exposedPort}/connectors"), "${kafkaContainer.host}:${kafkaContainer.exposedPort}", true, mapOf()) { kafkaStreams ->
            val database = "mongodump"  //topologyContext.topicName("@mongodump")
            var hits = 0L
            val start = System.currentTimeMillis()
            withTimeout(200000) {
                MongoClients.create("mongodb://${mongoContainer.host}:${mongoContainer.exposedPort}")
                    .use { client ->
                        repeat(1000) {
                            val collection = client.getDatabase(database).getCollection("filmwithactors")
                            hits = collection.countDocuments()
                            logger.info("Count of Documents: $hits in database: $database")
                            if (hits == 1000L) {
                                return@withTimeout
                            }
                            delay(1000)
                        }
                    }
                throw TimeoutException("Test timed out")
            }

            val diff = System.currentTimeMillis() - start
            logger.info("Elapsed: $diff millis")
            assertEquals(1000L, hits)
            kafkaStreams.close()
        }
    }
}
