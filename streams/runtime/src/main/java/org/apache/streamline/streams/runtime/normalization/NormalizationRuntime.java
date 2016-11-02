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
package org.apache.streamline.streams.runtime.normalization;

import org.apache.registries.common.Schema;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.layout.component.impl.normalization.BulkNormalizationConfig;
import org.apache.streamline.streams.layout.component.impl.normalization.FieldBasedNormalizationConfig;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationConfig;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Abstract class for runtime execution of normalization.
 */
public abstract class NormalizationRuntime {
    private static Logger LOG = LoggerFactory.getLogger(NormalizationRuntime.class);
    protected final NormalizationConfig normalizationConfig;

    protected NormalizationRuntime(NormalizationConfig normalizationConfig) {
        this.normalizationConfig = normalizationConfig;
    }

    public final StreamlineEvent execute(StreamlineEvent event) throws NormalizationException {
        Map<String, Object> result = normalize(event);
        return new StreamlineEventImpl(result, event.getDataSourceId(), event.getId(), event.getHeader());
    }

    protected abstract Map<String, Object> normalize(StreamlineEvent event) throws NormalizationException;

    public static class Factory {
        public NormalizationRuntime create(NormalizationConfig normalizationConfig, Schema declaredOutputSchema, NormalizationProcessor.Type type) {
            NormalizationRuntime normalizationProcessorRuntime = null;
            switch(type) {
                case fineGrained:
                    normalizationProcessorRuntime = new FieldBasedNormalizationRuntime.Builder((FieldBasedNormalizationConfig) normalizationConfig, declaredOutputSchema).build();
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
