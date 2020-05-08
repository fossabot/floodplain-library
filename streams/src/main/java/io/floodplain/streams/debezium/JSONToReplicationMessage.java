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
package io.floodplain.streams.debezium;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.floodplain.immutable.api.ImmutableMessage;
import io.floodplain.immutable.api.ImmutableMessage.ValueType;
import io.floodplain.immutable.factory.ImmutableFactory;
import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.replication.api.ReplicationMessage.Operation;
import io.floodplain.replication.factory.ReplicationFactory;
import io.floodplain.streams.api.TopologyContext;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class JSONToReplicationMessage {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    private final static Logger logger = LoggerFactory.getLogger(JSONToReplicationMessage.class);


    //TODO Beware of threading issues
    private final static DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");

    public static KeyValue parse(String keyInput, byte[] data, boolean appendTenant, boolean appendSchema, boolean appendTable) {
        try {
            ObjectNode keynode = (ObjectNode) objectMapper.readTree(keyInput);
            TableIdentifier key = processDebeziumKey(keynode, appendTenant, appendSchema);

            ObjectNode valuenode = (ObjectNode) objectMapper.readTree(data);
            if (!valuenode.has("payload") || valuenode.get("payload").isNull()) {
                ReplicationMessage replMsg = ReplicationFactory.empty().withOperation(Operation.DELETE);
                final ReplicationMessage converted = appendTenant ? replMsg.with("_tenant", key.tenant, ValueType.STRING) : replMsg;
                final ReplicationMessage convertedWTable = appendTable ? converted.with("_tenant", key.tenant, ValueType.STRING) : converted;
                return new KeyValue(key.combinedKey, ReplicationFactory.getInstance().serialize(convertedWTable));
            }
            final ReplicationMessage convOptional = convertToReplication(false, valuenode, Optional.ofNullable(key.table));
            ReplicationMessage conv = convOptional.withPrimaryKeys(key.fields);
            if (appendTable) {
                conv = conv.with("_table", key.table, ImmutableMessage.ValueType.STRING);

            }
            final ReplicationMessage converted = appendTenant ? conv.with("_tenant", key.tenant, ImmutableMessage.ValueType.STRING) : conv;

            byte[] serialized = ReplicationFactory.getInstance().serialize(converted);
            return new KeyValue(key.combinedKey,serialized);
        } catch (IOException e) {
            logger.error("Error: ", e);
        }
        return null;
    }

    public static Deserializer<ReplicationMessage> replicationFromConnect() {
        return (topic, data) -> parseConnectMessage(data);
    }

    public static ReplicationMessage parseConnectMessage(byte[] data) {
        ObjectNode valuenode;
        try {
            valuenode = (ObjectNode) objectMapper.readTree(data);
            if (!valuenode.has("payload") || valuenode.get("payload").isNull()) {
                return null;
            }
            return convertToReplication(false, valuenode, Optional.empty());
        } catch (IOException e) {
            throw new RuntimeException("JSON parse issue while parsing expected json to replication message: "+new String(data),e);
        }

    }

    public static ImmutableMessage convert(ObjectNode node, Consumer<String> callbackFieldList, boolean isKey, Optional<Operation> o, Optional<String> table) {
        if (!isKey && o.isPresent() && o.get().equals(Operation.DELETE) && table.isPresent()) {
            return ImmutableFactory.empty().with("table", table.get(), ImmutableMessage.ValueType.STRING);
        }
        try {
            JsonNode payLoad = node.get("payload");
            final Optional<ObjectNode> payload = payLoad.isNull() ? Optional.empty() : Optional.of((ObjectNode) payLoad);
            final JsonNode schema = node.get("schema");
            if (schema.isNull()) {
                logger.info("WRITING FAILED: {}", objectMapper.writeValueAsString(node));
            }
            ArrayNode fields = (ArrayNode) schema.get("fields");
            if (!isKey) {
                Optional<JsonNode> firstFields = findFirstChild(fields, e -> true);
                fields = firstFields.isPresent() ? (ArrayNode) firstFields.get().get("fields") : fields;
            }

            Map<String, ValueType> types = new HashMap<>();
            Map<String, Object> jsonValues = new HashMap<>();

            fields.forEach(e -> {
                String field = e.get("field").asText();
                callbackFieldList.accept(field);
                JsonNode name = e.get("name");
                Optional<String> typeName = name == null ? Optional.empty() : Optional.of(name.asText());
                Optional<JsonNode> typeParameters = Optional.ofNullable(e.get("parameters"));
                String rawType = e.get("type").asText();
                ValueType type = resolveType(rawType, typeName, typeParameters);
                types.put(field, type);
                boolean hasAfter = payload.get().has("after");
                boolean reallyHasAfter = hasAfter && !payload.get().get("after").isNull();
                final Optional<ObjectNode> after = reallyHasAfter ? Optional.ofNullable((ObjectNode) payload.get().get("after")) : Optional.empty();
                final Object resolvedValue = reallyHasAfter ? resolveValue(after, field, rawType, typeName, e) : resolveValue(payload, field, rawType, typeName, e);
                jsonValues.put(field, resolvedValue);
            });
            return ImmutableFactory.create(jsonValues, types);

        } catch (JsonProcessingException e1) {
            logger.error("Error: ", e1);
        }
        return ImmutableFactory.empty();
    }

    public static ReplicationMessage convertToReplication(boolean isKey, ObjectNode node, Optional<String> table) {
        ObjectNode payload = (ObjectNode) node.get("payload");
        long millis = payload.get("ts_ms").asLong();
        Operation o = resolveOperation(payload, payload.get("op").asText());
        ImmutableMessage core = convert(node, s->{}, isKey, Optional.of(o), table);
        return ReplicationFactory.standardMessage(core).withOperation(o).atTime(millis);
    }


    private static Optional<JsonNode> findFirstChild(ArrayNode node, Predicate<JsonNode> pred) {
        return StreamSupport.stream(node.spliterator(), false).filter(pred).findFirst();
    }

    private static Object resolveValue(Optional<ObjectNode> fields, String field, String type, Optional<String> typeName, JsonNode currentType) {
        try {
            JsonNode node = fields.get().get(field);
            if (node == null) {
                throw new NullPointerException("Missing node for field: " + field + " type: " + type + " typeName: " + typeName);
            }
            return resolveValue(type, typeName, node, currentType);
        } catch (Throwable e) {
            throw new RuntimeException("Error resolving value: " + field + " with type: " + type + " named type: " + typeName, e);
        }
    }

    public static ValueType resolveType(String type, Optional<String> namedType, Optional<JsonNode> parameters) {
        if (namedType.isEmpty()) {
            return resolveSimpleType(type);
        }
        switch (namedType.get()) {
            case "io.debezium.time.Date":
                return ValueType.DATE;
            case "io.debezium.time.ZonedTimestamp":
            case "io.debezium.time.NanoTimestamp":
            case "io.debezium.time.MicroTimestamp":
            case "io.debezium.data.VariableScaleDecimal":
                return ValueType.LONG;
            case "org.apache.kafka.connect.data.Decimal":
                if (parameters.isPresent()) {
                    JsonNode scaleNode = parameters.get().get("scale");
                    if (scaleNode != null) {
                        return Integer.parseInt(scaleNode.asText()) > 0 ? ValueType.DOUBLE : ValueType.LONG;
                    }
                }
                return ValueType.LONG;
            case "io.debezium.data.Enum":
                return ValueType.ENUM;
            default:
                logger.warn("Unknown type with name, this will probably fail: {}", namedType.get());
                return resolveSimpleType(type);
        }

    }

    public static Object resolveValue(String type, Optional<String> namedType, JsonNode value, JsonNode typeParameters) {

        if (value.isNull()) {
            return null;
        }
        if (namedType.isEmpty()) {
            return resolveSimple(type, value);
        }
        switch (namedType.get()) {
            case "io.debezium.time.Date":
                int valueInt = value.asInt();
                // I have no clue...
                long timemillis = 24 * 60 * 60 * 1000 * (long) valueInt;
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, valueInt);
                Date d = new Date(timemillis);
                return format.format(d);
            case "io.debezium.time.ZonedTimestamp":
                Instant instant = Instant.parse(value.asText());
                return instant.toEpochMilli();
            case "io.debezium.time.NanoTimestamp":
                long l2 = Long.parseLong(value.asText());
                Instant instant2 = Instant.ofEpochMilli(l2);
                return instant2.toEpochMilli();
            case "io.debezium.data.VariableScaleDecimal":
                ObjectNode node = (ObjectNode) value;
                int scale = node.get("scale").asInt();
                String val = node.get("value").asText();
                byte[] binary = Base64.getDecoder().decode(val);
                final BigDecimal decoded = new BigDecimal(new BigInteger(binary), scale);
                logger.info("VariableScale: {} -> decoded length: {}", decoded, binary.length);
                return decoded.longValue();
            case "org.apache.kafka.connect.data.Decimal":
                String decval = value.asText();
                Optional<ObjectNode> typeParams = Optional.ofNullable((ObjectNode) typeParameters.get("parameters"));
                byte[] da = Base64.getDecoder().decode(decval);

                return parseDecimal(da, typeParams);
            case "io.debezium.data.Enum":
                return value.asText();
            default:
                return resolveSimple(type, value);
        }
    }

//	private BigDecimal parseDecimal(byte[] data, int scale, )

    private static Object parseDecimal(byte[] bytes, Optional<ObjectNode> typeParams) {
//		logger.info("long byte size: {}",bytes.length);
        Optional<JsonNode> scaleNode = typeParams.map(e -> e.get("scale"));
        Optional<Integer> scale = scaleNode.filter(e -> !e.isNull()).map(e -> Integer.parseInt(e.asText()));
//		Optional<Integer> scale = typeParams.map(t->t.get("scale").asText()).map(Integer::parseInt);

        final BigDecimal decoded = scale.map(integer -> new BigDecimal(new BigInteger(bytes), integer)).orElseGet(() -> new BigDecimal(new BigInteger(bytes)));
        if (decoded.scale() > 0) {
            return decoded.doubleValue();
        } else {
            return decoded.longValue();

        }

    }

    private static Object resolveSimple(String type, JsonNode value) {
        switch (type) {
            case "int16":
            case "integer":
            case "int32":
                return value.asInt();
            case "int64":
            case "long":
                return value.asLong();
            case "string":
                return value.asText();
            case "boolean":
                return value.asBoolean();
            case "double":
                return value.asDouble();
            case "binary":
            case "bytes":
                return Base64.getDecoder().decode(value.asText());
            case "list":
            case "array":
                List<String> ar = new ArrayList<>();
                value.forEach(e -> ar.add(e.asText()));
                return Collections.unmodifiableList(ar);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    private static ValueType resolveSimpleType(String type) {
        switch (type) {
            case "int16":
            case "int32":
                return ValueType.INTEGER;
            case "int64":
                return ValueType.LONG;
            case "string":
                return ValueType.STRING;
            case "double":
                return ValueType.DOUBLE;
            case "bytes":
                return ValueType.BINARY;
            case "array":
                return ValueType.STRINGLIST;
            case "boolean":
                return ValueType.BOOLEAN;
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    // TODO Complete when I have more examples;
    private static Operation resolveOperation(ObjectNode payloadNode, String opName) {
        boolean hasBefore = payloadNode.has("before");
        boolean hasAfter = payloadNode.has("after");
        if (hasBefore && !hasAfter) {
            if (!"d".equals(opName)) {
                logger.warn("Unexpected operation: " + opName);
            }
            return Operation.DELETE;
        }
        switch (opName) {
            case "u":
            case "r":
            case "c":
                return Operation.UPDATE;
            case "d":
                return Operation.DELETE;
            default:
                return Operation.NONE;
        }
    }

    public static TableIdentifier processDebeziumKey(ObjectNode on, boolean appendTenant, boolean appendSchema) {
        List<String> fields = new ArrayList<>();
        ImmutableMessage converted = convert(on, fields::add, true, Optional.empty(), Optional.empty());
        Optional<Object> tableId = converted.value("__dbz__physicalTableIdentifier");
        fields.remove("__dbz__physicalTableIdentifier");
        // for demo, shouldn't do any harm
        return new TableIdentifier(tableId.map(e->(String)e).orElse(on.get("schema").get("name").asText()), converted, fields, appendTenant, appendSchema);
    }
}
