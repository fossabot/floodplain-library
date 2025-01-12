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
package io.floodplain.kotlindsl.example

import io.floodplain.kotlindsl.each
import io.floodplain.kotlindsl.mysqlSource
import io.floodplain.kotlindsl.mysqlSourceConfig
import io.floodplain.kotlindsl.stream
import io.floodplain.mongodb.mongoConfig
import io.floodplain.mongodb.toMongo
import java.net.URL

private val logger = mu.KotlinLogging.logger {}
fun main() {
    stream {
        val mysqlConfig = mysqlSourceConfig("mysqlsource", "mysql", 3306, "root", "mysecretpassword", "wpdb")
        val mongoConfig = mongoConfig("mongosink", "mongodb://mongo", "$generation-mongodumpalt")
        mysqlSource("wpdb.wp_posts", mysqlConfig) {
            each { key, msg, _ ->
                logger.info("Detected key: $key and message: $msg")
            }
            toMongo("posts", "$tenant-$deployment-$generation-topicdef", mongoConfig)
        }
    }.renderAndSchedule(URL("http://localhost:8083/connectors"), "localhost:9092", true)
}
