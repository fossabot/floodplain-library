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
package io.floodplain.kotlindsl

import io.floodplain.reactive.source.topology.SinkTransformer
import io.floodplain.streams.api.ProcessorName
import io.floodplain.streams.api.Topic
import io.floodplain.streams.api.TopologyContext
import java.util.Optional

fun PartialStream.googleSheetsSink(config: GoogleSheetConfiguration) {
    val configMap: Map<String, String> = mapOf(Pair("connector.class", "io.floodplain.sink.SheetSinkConnector"))
    val sink = SinkTransformer(Optional.of(ProcessorName.from(config.name)), Topic.from(config.topic), false, Optional.empty(), true)
    addTransformer(Transformer(sink))
}

fun Stream.googleSheetConfig(topic: String, name: String, spreadsheetId: String, columns: List<String>): GoogleSheetConfiguration {
    return GoogleSheetConfiguration(topic, name, spreadsheetId, columns)
}

class GoogleSheetConfiguration(val name: String, val topic: String, val spreadsheetId: String, val columns: List<String>) : Config {
    override fun materializeConnectorConfig(topologyContext: TopologyContext): Pair<String, Map<String, String>> {
        TODO("Not yet implemented")
    }

    override fun sourceElements(): List<SourceTopic> {
        return emptyList<SourceTopic>()
    }

    override suspend fun connectSource(inputReceiver: InputReceiver) {
    }

    override fun sinkElements(): Map<Topic, FloodplainSink> {
        TODO("Not yet implemented")
    }
}
