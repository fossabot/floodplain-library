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
package io.floodplain.replication.impl.protobuf.test;

import io.floodplain.immutable.api.ImmutableMessage;
import io.floodplain.immutable.api.ImmutableMessage.ValueType;
import io.floodplain.immutable.factory.ImmutableFactory;
import io.floodplain.protobuf.generated.Replication;
import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.replication.api.ReplicationMessageParser;
import io.floodplain.replication.factory.ReplicationFactory;
import io.floodplain.replication.impl.json.JSONReplicationMessageParserImpl;
import io.floodplain.replication.impl.protobuf.impl.ProtobufReplicationMessageParser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class ReplicationProtobufTest {

    private final ReplicationMessageParser protoBufParser = new ProtobufReplicationMessageParser();
    private final ReplicationMessageParser jsonParser = new JSONReplicationMessageParserImpl();
    private final static Logger logger = LoggerFactory.getLogger(ReplicationProtobufTest.class);

    @BeforeClass
    public static void setup() {
        System.setProperty(ReplicationMessage.PRETTY_JSON, "true");
    }

    @Test
    public void test() {
        Map<String, Object> values = new HashMap<>();
        Map<String, ValueType> types = new HashMap<>();
        values.put("astring", "blob");
        types.put("astring", ValueType.STRING);
        ReplicationMessage m = ReplicationFactory.fromMap("key", values, types);
        m = m.withSubMessage("subb", createSubMessage());
        byte[] bb = m.toBytes(protoBufParser);
        logger.info("Length: {}", bb.length);
        ReplicationMessage rm = protoBufParser.parseBytes(Optional.empty(), bb);
        logger.info("Astring: {}", rm.value("astring").get());
    }

    public ImmutableMessage createSubMessage() {
        Map<String, Object> values = new HashMap<>();
        Map<String, ValueType> types = new HashMap<>();
        values.put("bstring", "subblob");
        types.put("bstring", ValueType.STRING);
        return ImmutableFactory.create(values, types);
    }

    @Test
    public void testInteger() {
        Map<String, Object> values = new HashMap<>();
        Map<String, ValueType> types = new HashMap<>();
        values.put("anint", 3);
        types.put("anint", ValueType.INTEGER);
        ReplicationMessage m = ReplicationFactory.fromMap("key", values, types);
        Object value = m.value("anint").get();
        Assert.assertTrue(value instanceof Integer);
        byte[] bb = m.toBytes(protoBufParser);
        logger.info("Length: {}", bb.length);
        ReplicationMessage rm = protoBufParser.parseBytes(Optional.empty(), bb);
        Object value2 = rm.value("anint").get();
        Assert.assertTrue(value2 instanceof Integer);
    }

    @Test
    public void testDate() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("submessage.json")) {
            ReplicationMessage rm = jsonParser.parseStream(is);
            LocalDateTime dd = (LocalDateTime) rm.value("formdate").get();
            logger.info(">> {}", dd);
        }
    }

    @Test
    public void testConversion() {
        try (InputStream is = getClass().getResourceAsStream("submessage.json")) {
            ReplicationMessage rm = jsonParser.parseStream(is);
            Assert.assertEquals(26, rm.values().size());
            byte[] bb = protoBufParser.serialize(rm);
            logger.info("bytes: {}", bb.length);
            ReplicationMessage rm2 = protoBufParser.parseBytes(Optional.empty(), bb);
            Assert.assertEquals(26, rm2.values().size());
            byte[] bb2 = jsonParser.serialize(rm2);
            logger.info("JSON again: {}", bb2.length);
            logger.info(">>>>\n{}", new String(bb2, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testClocktime() {
        try (InputStream is = ReplicationProtobufTest.class.getResourceAsStream("calendarday.json")) {
            ReplicationMessage rm = jsonParser.parseStream(is);
            final LocalTime columnValue = (LocalTime) rm.value("starttime").get();
            Assert.assertEquals(columnValue.getMinute(),0 );
            Assert.assertEquals(columnValue.getHour(),17 );
            Assert.assertEquals(7, rm.values().size());
            byte[] bb = protoBufParser.serialize(rm);
            logger.info("bytes: " + bb.length);
            ReplicationMessage rm2 = protoBufParser.parseBytes(Optional.empty(), bb);
            Assert.assertEquals(7, rm2.values().size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMessageListParser() {
        ReplicationMessageParser jsonparser = new JSONReplicationMessageParserImpl();
        ReplicationMessageParser parser = new ProtobufReplicationMessageParser();
        try(InputStream stream = ReplicationProtobufTest.class.getResourceAsStream("addresslist.json")) {
            List<ReplicationMessage> list = jsonparser.parseMessageList(Optional.of("addresstopic"), stream);
            Assert.assertEquals(1, list.size());
            byte[] data = parser.serializeMessageList(list);
            logger.info("Datasize: " + data.length);
            Assert.assertEquals(68, data.length);
            logger.info("First: " + data[0]);
            logger.info("Secon: " + data[1]);
            List<ReplicationMessage> list2 = parser.parseMessageList(data);
            byte[] data2 = jsonParser.serializeMessageList(list2);
            logger.info("DATA: " + new String(data2, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("IO issue",e);
        }
    }

    @Test
    public void testMessageStreamListParser() {
        ReplicationMessageParser jsonparser = new JSONReplicationMessageParserImpl();
        ReplicationMessageParser parser = new ProtobufReplicationMessageParser();
        InputStream stream = getClass().getResourceAsStream("addresslist.json");
        List<ReplicationMessage> list = jsonparser.parseMessageList(Optional.of("addresstopic"), stream);
        Assert.assertEquals(1, list.size());
        byte[] data = parser.serializeMessageList(list);
        logger.info("Datasize: " + data.length);
        Assert.assertEquals(68, data.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        List<ReplicationMessage> list2 = parser.parseMessageList(Optional.of("addresstopic"), bais);
        for (ReplicationMessage replicationMessage : list2) {
            Assert.assertFalse(replicationMessage.isErrorMessage());
        }
        logger.info("LEN: " + list.size());
    }

    @Test
    public void testEmptyValue() {
        ReplicationMessageParser jsonparser = new JSONReplicationMessageParserImpl();
        ReplicationMessageParser parser = new ProtobufReplicationMessageParser();
        Map<String, Object> values = new HashMap<>();
        Map<String, ValueType> types = new HashMap<>();
        values.put("key", "bla");
        types.put("key", ValueType.STRING);
        values.put("empty", null);
        types.put("empty", ValueType.STRING);
        ReplicationMessage msg = ReplicationFactory.fromMap("key", values, types);
        Assert.assertTrue(msg.value("empty").isEmpty());
        byte[] protobytes = msg.toBytes(parser);
        ReplicationMessage reserialized = parser.parseBytes(Optional.empty(), protobytes);
        Assert.assertTrue(reserialized.value("empty").isEmpty());
        String json2 = new String(reserialized.toBytes(jsonparser), StandardCharsets.UTF_8);
        logger.info("json: " + json2);
    }

    @Test
    public void testParseProtobuf() {
        try (InputStream is = ReplicationProtobufTest.class.getResourceAsStream("organizationwithparam.json")) {
            ReplicationMessage rm = jsonParser.parseStream(is);
            logger.info("paramsize: " + rm.paramMessage().get().values().size());
            byte[] bb = protoBufParser.serialize(rm);
            logger.info("# of bytes: " + bb.length);
            Replication.ReplicationMessageProtobuf rmp = Replication.ReplicationMessageProtobuf.parseFrom(bb);
            logger.info("magic? " + rmp.getMagic());
            logger.info("rmp: " + rmp.getParamMessage().getValuesCount());
            ReplicationMessage repl = protoBufParser.parseBytes(Optional.empty(), bb);
            logger.info("paramsize: " + repl.paramMessage().get().values().size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testParamMessage() {
        try (InputStream is = ReplicationProtobufTest.class.getResourceAsStream("organizationwithparam.json")) {
            ReplicationMessage rm = jsonParser.parseStream(is);
            byte[] bb = protoBufParser.serialize(rm);
            ReplicationMessage repl = protoBufParser.parseBytes(Optional.empty(), bb);
            Assert.assertTrue(repl.paramMessage().isPresent());
            Assert.assertEquals(12, repl.paramMessage().get().value("col2").get());
            logger.info("Names: " + repl.queueKey() + " names: " + repl.columnNames() + "\n sub: " + repl.message().subMessageNames() + " subli: " + repl.subMessageListNames());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSubmessageMessage() {
        try (InputStream is = ReplicationProtobufTest.class.getResourceAsStream("submessage.json")) {
            ReplicationMessage rm = jsonParser.parseStream(is);
            byte[] bb = protoBufParser.serialize(rm);
            ReplicationMessage repl = protoBufParser.parseBytes(Optional.empty(), bb);
            logger.info("Names: " + repl.queueKey() + " names: " + repl.columnNames() + "\n sub: " + repl.message().subMessageNames() + " subli: " + repl.subMessageListNames());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
