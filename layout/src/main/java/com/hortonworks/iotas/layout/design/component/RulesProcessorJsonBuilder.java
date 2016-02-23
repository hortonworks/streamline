/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.layout.design.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RulesProcessorJsonBuilder implements ComponentBuilder<RulesProcessor> {
    private static final Logger LOG = LoggerFactory.getLogger(RulesProcessorJsonBuilder.class);
    private final String rulesProcessorJson;

    public RulesProcessorJsonBuilder (String rulesProcessorJson) {
        this.rulesProcessorJson = rulesProcessorJson;
    }

    @Override
    public RulesProcessor build() {
        ObjectMapper mapper = new ObjectMapper();
        RulesProcessor rulesProcessor = null;
        try {
            rulesProcessor = mapper.readValue(rulesProcessorJson, RulesProcessor.class);
        } catch (IOException e) {
            LOG.error("Error during deserialization of JSON string: {}", rulesProcessorJson, e);
        }
        return rulesProcessor;
    }
}
