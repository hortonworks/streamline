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
package com.hortonworks.iotas.layout.runtime.normalization;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.normalization.BulkNormalizationConfig;
import com.hortonworks.iotas.layout.design.normalization.FieldBasedNormalizationConfig;
import com.hortonworks.iotas.layout.design.normalization.NormalizationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class for runtime execution of normalization.
 */
public abstract class NormalizationRuntime {
    private static Logger LOG = LoggerFactory.getLogger(NormalizationRuntime.class);
    protected final NormalizationConfig normalizationConfig;

    protected NormalizationRuntime(NormalizationConfig normalizationConfig) {
        this.normalizationConfig = normalizationConfig;
    }

    public final IotasEvent execute(IotasEvent iotasEvent) throws NormalizationException {
        Map<String, Object> result = normalize(iotasEvent);
        return new IotasEventImpl(result, iotasEvent.getDataSourceId(), iotasEvent.getId(), iotasEvent.getHeader());
    }

    protected abstract Map<String, Object> normalize(IotasEvent iotasEvent) throws NormalizationException;

    public static class Factory {
        public NormalizationRuntime create(NormalizationConfig normalizationConfig, Schema declaredOutputSchema, NormalizationProcessor.Type type) {
            NormalizationRuntime normalizationProcessorRuntime = null;
            switch(type) {
                case fineGrained:
                    normalizationProcessorRuntime = new FieldBasedNormalizationRuntime.Builder((FieldBasedNormalizationConfig) normalizationConfig).build();
                    break;
                case bulk:
                    normalizationProcessorRuntime = new BulkNormalizationRuntime((BulkNormalizationConfig) normalizationConfig, declaredOutputSchema);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown normalization config type: "+type);
            }

            return normalizationProcessorRuntime;
        }

    }

}
