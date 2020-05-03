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
package io.floodplain.streams.base;

import io.floodplain.streams.serializer.ReplicationMessageListSerde;
import io.floodplain.streams.serializer.ReplicationMessageSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamOperators {

    public static final int DEFAULT_MAX_LIST_SIZE = 500;
    public static final ReplicationMessageSerde replicationSerde = new ReplicationMessageSerde();
    public static final ReplicationMessageListSerde replicationListSerde = new ReplicationMessageListSerde();


    private static final Logger logger = LoggerFactory.getLogger(StreamOperators.class);

    private StreamOperators() {
        // -- no instances
    }

}