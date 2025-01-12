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

import io.floodplain.kotlindsl.join
import io.floodplain.kotlindsl.message.empty
import io.floodplain.kotlindsl.postgresSource
import io.floodplain.kotlindsl.postgresSourceConfig
import io.floodplain.kotlindsl.scan
import io.floodplain.kotlindsl.set
import io.floodplain.kotlindsl.stream
import io.floodplain.mongodb.remoteMongoConfig
import io.floodplain.mongodb.toMongo
import kotlinx.coroutines.delay
import java.math.BigDecimal

fun main() {
    stream {
        val postgresConfig = postgresSourceConfig("mypostgres", "postgres", 5432, "postgres", "mysecretpassword", "dvdrental", "public")
        val mongoConfig = remoteMongoConfig("mongosink", "mongodb://mongo", "$generation-mongodump")
        postgresSource("customer", postgresConfig) {
            join {
                postgresSource("payment", postgresConfig) {
                    scan(
                        { msg -> msg["customer_id"].toString() },
                        { empty().set("total", BigDecimal(0)) },
                        {
                            set { _, msg, state ->
                                state["total"] = (state["total"] as BigDecimal).add(msg["amount"] as BigDecimal)
                                state
                            }
                        },
                        {
                            set { _, msg, state ->
                                state["total"] = (state["total"] as BigDecimal).add(msg["amount"] as BigDecimal)
                                state
                            }
                        }
                    )
                }
            }
            set { _, customer, paymenttotal ->
                customer["payments"] = paymenttotal["total"]; customer
            }
            toMongo("justtotal", "myfinaltopic", mongoConfig)
        }
    }.renderAndExecute {
        delay(1000000)
    }
}
