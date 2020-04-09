package io.floodplain.kotlindsl.example

import io.floodplain.kotlindsl.*
import io.floodplain.kotlindsl.message.*
import java.util.*
import com.dexels.kafka.streams.api.TopologyContext
import com.dexels.kafka.streams.remotejoin.TopologyConstructor
import java.net.URL

private val logger = mu.KotlinLogging.logger {}


fun main() {
    val tenant = "mytenant"
    val deployment = "mydeployment"
    val instance = "myinstance"
    val generation = "mygeneration"
    var topologyContext = TopologyContext(Optional.ofNullable(tenant), deployment, instance, generation)
    var topologyConstructor = TopologyConstructor()

    pipe(topologyContext, topologyConstructor) {

        val postgresConfig = postgresSourceConfig("mypostgres", "postgres", 5432, "postgres", "mysecretpassword", "dvdrental")
        val mongoConfig = mongoConfig("mongosink","mongodb://mongo", "mongodump")
        debeziumSource("public", "customer", postgresConfig) {
            joinWith {
                debeziumSource("public", "payment",postgresConfig) {

                    scan({ msg -> msg["customer_id"].toString() }, { empty().set("total", 0.0).set("customer_id",0) },
                            {
                                each{
                                    msg,state->
                                    logger.info { "inmsg: ${msg.toString()}" }
                                    logger.info { "instate: ${state.toString()}" }
                                }
                                set { msg, state -> state["total"] = state["total"] as Double + msg["amount"] as Double; state["customer_id"]=msg["customer_id"]!!; state } },
                            { set { msg, state -> state["total"] = state["total"] as Double - msg["amount"] as Double; state } }
                    )
                    set { _, state ->
                        val result = empty()
                        result["total"] ?: state["total"]
                        result["customer_id"]=state["customer_id"] as Int
                        result
                    }
                    each{
                        msg,state->
                        logger.info { "msg: ${msg.toString()}" }
                    }
                    set { msg, state ->
                        msg.set("total", state.get("total")!!)
                    }
                }
            }
            set {
                msg,state->msg["payments"] = state; state
            }

            mongoSink(topologyContext,"customerwithtotal", "myfinaltopic", mongoConfig)
        }
    }.renderAndStart(URL( "http://localhost:8083/connectors"),"kafka:9092",UUID.randomUUID().toString())
}