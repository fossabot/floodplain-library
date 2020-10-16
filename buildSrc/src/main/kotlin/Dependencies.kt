package io.floodplain.build

object Versions {
    const val gradle = "3.5.0"
    const val kotlin = "1.4.0"

    /* test */
    const val junit = "4.12"
}

object Libs {
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
}

fun isReleaseVersion(): Boolean {
    return !FloodplainDeps.floodplain_version.endsWith("SNAPSHOT")
}

object FloodplainDeps {
    const val floodplain_version = "0.10.1-SNAPSHOT"
    const val jackson_version = "2.11.0"
    const val kafka_version = "2.5.0"
    const val slf4j_version = "1.7.30"
    const val mysql_version = "8.0.20"
    const val mongodb_version = "3.12.5"
    const val debezium_version = "1.2.1.Final"
    const val testContainer_version = "1.15.0-rc2"
    const val kotlin_coroutines_version = "1.4.0-M1"
    const val debeziumCore = "io.debezium:debezium-core:$debezium_version"
    const val debeziumPostgres  = "io.debezium:debezium-connector-postgres:$debezium_version"
    const val debeziumMySQL     = "io.debezium:debezium-connector-mysql:$debezium_version"
    const val debeziumEmbedded  = "io.debezium:debezium-embedded:$debezium_version"
    const val debeziumServerCore= "io.debezium:debezium-server-core:$debezium_version"
    const val cdiApi            = "jakarta.enterprise:jakarta.enterprise.cdi-api:2.0.2"
    const val microProfileConfig= "org.eclipse.microprofile.config:microprofile-config-api:1.3"
    const val jUnit             = "junit:junit:4.12"
    const val mongoClientSync   = "org.mongodb:mongodb-driver-sync:$mongodb_version"
    const val slf4j             = "org.slf4j:slf4j-api:$slf4j_version"
    const val slf4jLog4j        = "org.slf4j:slf4j-log4j12:$slf4j_version"
    const val protobuf          = "com.google.protobuf:protobuf-java:3.11.4"
    const val kotlinLogging     = "io.github.microutils:kotlin-logging:1.8.3"
    const val jacksonCore       = "com.fasterxml.jackson.core:jackson-core:$jackson_version"
    const val jacksonDatabind   = "com.fasterxml.jackson.core:jackson-databind:$jackson_version"
    const val kafkaClient       = "org.apache.kafka:kafka-clients:$kafka_version"
    const val kafkaConnectApi   = "org.apache.kafka:connect-api:$kafka_version"
    const val kafkaConnectRuntime = "org.apache.kafka:connect-runtime:$kafka_version"
    const val kafkaStreams      = "org.apache.kafka:kafka-streams:$kafka_version"
    const val testContainer     = "org.testcontainers:testcontainers:$testContainer_version"
    const val kotlinCoroutines  = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutines_version"
    const val argParser         = "com.xenomachina:kotlin-argparser:2.0.7"
}