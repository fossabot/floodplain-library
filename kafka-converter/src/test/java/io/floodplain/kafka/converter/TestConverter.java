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
package io.floodplain.kafka.converter;

import org.apache.kafka.connect.data.SchemaAndValue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class TestConverter {

    private final static Logger logger = LoggerFactory.getLogger(TestConverter.class);

    @Before
    public void setup() {
//		ReplicationFactory.setInstance(new FallbackReplicationMessageParser());

    }

    @Test
    public void testConverter() throws IOException {
        ReplicationMessageConverter converter = new ReplicationMessageConverter();
        converter.configure(Collections.emptyMap(), false);
        SchemaAndValue sav = converter.toConnectData("any", readStream(TestConverter.class.getClassLoader().getResourceAsStream("example.json")));
        logger.info("Result: {}",sav.value());
    }

    public byte[] readStream(InputStream is) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }
}
