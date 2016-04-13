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
package com.hortonworks.iotas.layout.runtime.splitjoin;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link Joiner}
 *
 */
public class DefaultJoiner implements Joiner {

    private final String outputStream;

    public DefaultJoiner(String outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public IotasEvent join(EventGroup eventGroup) {
        Map<String, Object> fieldValues = new HashMap<>();
        Map<String, Object> auxiliaryFieldValues = new HashMap<>();
        for (IotasEvent subEvent : eventGroup.getSplitEvents()) {
            if(subEvent.getAuxiliaryFieldsAndValues() != null) {
                auxiliaryFieldValues.putAll(subEvent.getAuxiliaryFieldsAndValues());
            }
            if(subEvent.getFieldsAndValues() != null) {
                fieldValues.putAll(subEvent.getFieldsAndValues());
            }
        }

        return new IotasEventImpl(fieldValues, eventGroup.getDataSourceId(),
                UUID.randomUUID().toString(), Collections.<String, Object>emptyMap(), outputStream, auxiliaryFieldValues);
    }
}
