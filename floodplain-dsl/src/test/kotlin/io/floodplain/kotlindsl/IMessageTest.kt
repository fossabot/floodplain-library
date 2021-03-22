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
/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package io.floodplain.kotlindsl

import io.floodplain.immutable.api.ImmutableMessage
import io.floodplain.immutable.factory.ImmutableFactory
import io.floodplain.kotlindsl.message.IMessage
import io.floodplain.kotlindsl.message.empty
import io.floodplain.kotlindsl.message.fromImmutable
import io.floodplain.replication.api.ReplicationMessageParser
import io.floodplain.replication.factory.ReplicationFactory
import io.floodplain.replication.impl.json.JSONReplicationMessageParserImpl
import io.floodplain.replication.impl.protobuf.impl.ProtobufReplicationMessageParser
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val logger = mu.KotlinLogging.logger {}

@Suppress("UNCHECKED_CAST")
class IMessageTest {
    private val parser: ReplicationMessageParser = JSONReplicationMessageParserImpl()
    private val protoParser: ReplicationMessageParser = ProtobufReplicationMessageParser()

    val exampleMessage = empty()
        .set("adouble", 1.1)
        .set("anint", -3)
        .set("along", 15L)
        .set("adecimal", BigDecimal.valueOf(99))
        .set("astring", "somestring")
        .set("aboolean", false)
        .set("alist", listOf("foo", "bar"))
        .set("adate", LocalDate.of(2013, 3, 3))

    private fun createComplexMessage(): IMessage {
        val list = List(2) { exampleMessage.copy() }
        return exampleMessage
            .set("sublist", list)
            .set("sub", exampleMessage.copy())
    }

    // Utility function to check if a message remains unchanged after serialization and deserialization for all parsers
    private fun convertThereAndBack(input: IMessage): IMessage {
        val afterJSON = convertThereAndBack(input, parser)
        println(afterJSON)
        return convertThereAndBack(afterJSON, protoParser)
    }

    // Utility function to check if a message remains unchanged after serialization and deserialization for a specific parser
    private fun convertThereAndBack(input: IMessage, currentParser: ReplicationMessageParser): IMessage {
        val immutable = input.toImmutable()
        val repl = ReplicationFactory.standardMessage(immutable)
        val serialized = currentParser.serialize(repl)
        val deserialized = currentParser.parseBytes(Optional.empty(), serialized)
        println("S: ${String(serialized)}")
        return fromImmutable(deserialized.message())
    }

    @Test
    fun testImmutableConversion() {
        val m = ImmutableFactory.empty().with("aap", "vla", ImmutableMessage.ValueType.STRING)
        val msg: IMessage = fromImmutable(m)
        val m2 = msg.toImmutable()
        assertNotNull(m2.value("aap"), "app conversion has failed")
        assertEquals("vla", m2.value("aap").get() as String)
    }

    @Test
    fun testIMessageToData() {
        val msg = empty().set("key", "stringvalue")
        val data = msg.data()
        assertEquals(1, data.size)
        assertEquals("stringvalue", data["key"])
        assertEquals(msg, convertThereAndBack(msg))
    }

    @Test
    fun testNumerics() {
        val original = empty().set("adouble", 1.1)
            .set("anint", -3)
            .set("along", 15L)
            .set("adecimal", BigDecimal.valueOf(99))

        val msg = convertThereAndBack(original)
        assertEquals(original, msg)

        assertEquals(1.1, msg.double("adouble"))
        assertEquals(1.1, msg.optionalDouble("adouble"))
        assertNull(msg.optionalDouble("doesntexist"))

        assertEquals(-3, msg.integer("anint"))
        assertEquals(-3, msg.optionalInteger("anint"))
        assertNull(msg.optionalInteger("doesntexist"))

        assertEquals(15L, msg.long("along"))
        assertEquals(15L, msg.optionalLong("along"))
        assertNull(msg.optionalLong("doesntexist"))

        assertEquals(BigDecimal.valueOf(99), msg.decimal("adecimal"))
        assertEquals(BigDecimal.valueOf(99), msg.optionalDecimal("adecimal"))
        assertNull(msg.optionalLong("doesntexist"))
    }

    @Test
    fun testStringList() {
        val original = empty().set("alist", listOf("foo", "bar", "baz"))
        val msg = convertThereAndBack(original)
        assertEquals(listOf("foo", "bar", "baz"), msg.list("alist"))
        assertEquals(listOf("foo", "bar", "baz"), msg.optionalList("alist"))
        assertNull(msg.optionalList("doesntexist"))
    }

    @Test
    fun testList() {
        val initialList = listOf("alpha", "bravo", "charlie")
        val vv = empty().set("somelist", initialList)
        val data = vv.data()
        assertEquals(1, data.size)
        val list = data["somelist"] as List<String>
        assertEquals(initialList, list)
    }

    @Test
    fun testSimpleSubList() {
        val subList = listOf(empty().set("mystring", "value1"), empty().set("mystring", "value2"))
        val msg = empty().set("someother", "string").set("sublist", subList)
        val msg2 = convertThereAndBack(msg)
        assertEquals(msg, msg2)
    }

    @Test
    fun testEqualityIssue() {
        val msg1 = empty().set("field1", "value1").set("field2", "value2")
        val msg1b = convertThereAndBack(msg1)
        val msg2 = empty().set("field2", "value2").set("field1", "value1")
        val msg2b = convertThereAndBack(msg2)
        assertEquals(msg1, msg2)
        assertEquals(msg1, msg2b)
        assertEquals(msg1b, msg2)
    }

    @Test
    fun testCopy() {
        val submessage = empty().set("submessage", "value")
        val baseMessage = empty().set("foo", "bar").set("subm", submessage)
        val copy = baseMessage.copy()
        assertEquals(baseMessage, copy)
    }

    @Test
    fun testSomewhatComplexMessage() {
        val baseMessage = empty().set("foo", "bar")
        val nested = baseMessage.set("subm", listOf(baseMessage.copy(), baseMessage.copy()))
            .set("othersub", baseMessage.copy())
        val msg = convertThereAndBack(nested.copy())
        assertEquals(nested, msg)
    }
    @Test
    fun testComplexMessage() {
        val original = createComplexMessage()
        val msg = convertThereAndBack(original)
        assertEquals(original, msg)
    }

    @Test
    fun testMessageWithDate() {
        val localDate = LocalDate.of(2013, 3, 3)
        val exampleMessage = empty()
            .set("adate", localDate)
        logger.info("A date: $localDate")
        val msg = convertThereAndBack(exampleMessage)
        val actualDate = msg["adate"]
        assertEquals(LocalDate.of(2013, 3, 3), actualDate)
        assertEquals(exampleMessage, msg)
    }

    @Test
    fun testMessageWithTime() {
        val localTime = LocalTime.of(23, 30, 3)
        val exampleMessage = empty()
            .set("atime", localTime)
        logger.info("A timw: $localTime")
        val msg = convertThereAndBack(exampleMessage, parser)
        val actualDate = msg.time("atime")
        assertEquals(localTime.toSecondOfDay(), actualDate.toSecondOfDay())
        assertEquals(exampleMessage, msg)
    }

    @Test
    fun testMessageWithDateTime() {
        val localTime = LocalDateTime.now()
        val exampleMessage = empty()
            .set("atime", localTime)
        logger.info("A datetime: $localTime")
        val msg = convertThereAndBack(exampleMessage)
        val actualDate = msg.dateTime("atime")
        assertEquals(localTime.toInstant(ZoneOffset.UTC), actualDate.toInstant(ZoneOffset.UTC))
        assertEquals(exampleMessage, msg)
    }
    @Test
    fun testDate() {
        val original = exampleMessage.copy()
        val msg = convertThereAndBack(original)
        assertNotNull(msg.optionalDate("adate"))
        val dt = msg.date("adate")
        logger.info("Date recovered: $dt")
    }

    @Test
    fun testQueryMessageListElement() {
        val msg = createComplexMessage()
        val id = msg.messageList("sublist")?.get(0)?.string("astring")
        val id2 = msg.messageElement("sublist",0)?.string("astring")
        val nonExistingIndex = msg.messageElement("sublist",15)?.string("astring")
        val nonExistingList = msg.messageElement("sublist2",0)?.string("astring")
        assertEquals(id,id2)
        assertNull(nonExistingIndex)
        assertNull(nonExistingList)
    }
}
