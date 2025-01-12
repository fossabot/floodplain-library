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
import io.floodplain.kotlindsl.postgresSource
import io.floodplain.kotlindsl.postgresSourceConfig
import io.floodplain.kotlindsl.stream
import io.floodplain.mongodb.remoteMongoConfig
import io.floodplain.mongodb.toMongo
import io.floodplain.test.InstantiatedContainer
import io.floodplain.test.InstantiatedKafkaContainer
import io.floodplain.test.useIntegraton
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.testcontainers.containers.Network
import java.net.URL
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals

private val logger = mu.KotlinLogging.logger {}

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FilmToMongoIntegration {

    private val containerNetwork = Network.newNetwork()
    private val kafkaContainer = InstantiatedKafkaContainer { it.withNetwork(containerNetwork).withNetworkAliases("kafka") } // KafkaContainer("5.5.3").withEmbeddedZookeeper().withExposedPorts(9092)
    private val postgresContainer = InstantiatedContainer("floodplain/floodplain-postgres-demo:1.0.0", 5432, mapOf()) { it.withNetwork(containerNetwork).withNetworkAliases("postgres") }
    private val mongoContainer = InstantiatedContainer("mongo:latest", 27017, mapOf()) { it.withNetwork(containerNetwork).withNetworkAliases("mongo") }
    private var debeziumContainer: InstantiatedContainer? = null

    private fun createTopics(server: String, vararg topics: String) {
        val adminClient = AdminClient.create(mapOf(BOOTSTRAP_SERVERS_CONFIG to server))
        logger.info("Creating topics on server: $server")
        val newTopics = topics.map {
            val newTopic = NewTopic(it, 1, 1.toShort())
            newTopic.configs(mapOf("cleanup.policy" to "compact"))
            newTopic
        }.toList()
        val res = adminClient.createTopics(newTopics)
        logger.info("Create topics: ${topics.asList()} scheduled to $server")
        res.all().get()
        logger.info("Created topics: ${topics.asList()} done")
        adminClient.close()
    }

    @Before
    fun setup() {
        if (!useIntegraton) {
            logger.info("Not performing integration tests, doesn't seem to work in circleci")
            return
        }
        val bootstrap = "${kafkaContainer.host}:${kafkaContainer.exposedPort}"
        logger.info("kafka.getBootstrapServers(): ${kafkaContainer.container.bootstrapServers} bootstrap: $bootstrap")
        createTopics(bootstrap, "CONNECTOR_STORAGE")
        // "debezium/connect:1.4"

        debeziumContainer = InstantiatedContainer(
            "floodplain/debezium-with-mongodb:1.0",
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
        if (!useIntegraton) {
            logger.info("Not performing integration tests, doesn't seem to work in circleci")
            return
        }
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
        if (!useIntegraton) {
            logger.info("Not performing integration tests, doesn't seem to work in circleci")
            return
        }
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
            val mongoConfig = remoteMongoConfig(
                "mongosink",
                "mongodb://mongo:27017",
                "@mongodump"
            )
            postgresSource("film", postgresConfig) {
                toMongo("filmwithactors", "somtopic", mongoConfig)
            }
        }.renderAndSchedule(URL("http://${debeziumContainer?.host}:${debeziumContainer?.exposedPort}/connectors"), "${kafkaContainer.host}:${kafkaContainer.exposedPort}", true, null) { kafkaStreams ->
            val database = topologyContext.topicName("@mongodump")
            var hits = 0L
            val start = System.currentTimeMillis()
            withTimeout(200000) {
                MongoClients.create("mongodb://${mongoContainer.host}:${mongoContainer.exposedPort}")
                    .use { client ->
                        repeat(1000) {
                            val collection = client.getDatabase(database).getCollection("filmwithactors")
                            collection.find().first()?.let {
                                logger.info("Example doc: $it")
                            }
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
