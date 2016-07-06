/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.streams.layout.component.impl.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

/**
 * Builder for creating {@link NormalizationProcessor} instance from a given JSON string.
 *
 */
public class NormalizationProcessorJsonBuilder implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(NormalizationProcessorJsonBuilder.class);

    private final String json;

    public NormalizationProcessorJsonBuilder(String json) {
        this.json = json;
    }

    public NormalizationProcessor build() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, NormalizationProcessor.class);
        } catch (IOException e) {
            log.error("Encountered error in deserialization of json [{}] to [{}] ", json, NormalizationProcessor.class, e);
            throw new RuntimeException(e);
        }
    }
}
