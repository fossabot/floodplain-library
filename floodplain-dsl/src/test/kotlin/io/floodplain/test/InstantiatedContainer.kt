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
package io.floodplain.test

import org.testcontainers.containers.GenericContainer

val useIntegraton: Boolean by lazy {
    System.getenv("NO_INTEGRATION") == null
}

/**
 * Kotlin wrapper, to make testcontainers easier to use
 */
class InstantiatedContainer(image: String, port: Int, env: Map<String, String> = emptyMap()) {

    class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
    var container: KGenericContainer?
    var host: String
    var exposedPort: Int = -1
    init {
        container = KGenericContainer(image)
            .apply { withExposedPorts(port) }
            .apply { withEnv(env) }
        container?.start()
        host = container?.host ?: "localhost"
        exposedPort = container?.firstMappedPort ?: -1
    }
    fun close() {
        container?.close()
    }
}