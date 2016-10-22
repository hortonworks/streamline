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
package org.apache.streamline.streams.layout.component.impl.normalization;

import org.apache.streamline.common.Schema;
import org.apache.streamline.streams.layout.component.StreamlineProcessor;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Design time component of Normalization processor containing configuration of normalization in {@link NormalizationConfig} for
 * each given input {@link Schema}.
 *
 * This processor can have multiple incoming streams from Parser bolts (for now) which will have their respective schemas and
 * it should normalize the payload from input schema to output schema.
 *
 */
public class NormalizationProcessor extends StreamlineProcessor {
    public static final String DEFAULT_STREAM_ID = "default";
    public static final String CONFIG_KEY_TYPE = "normalizationProcessorType";
    public static final String CONFIG_KEY_NORMALIZATION = "normalizationConfig";

    /**
     * {@link NormalizationConfig} for each inbound stream for this component.
     */
    private Map<String, NormalizationConfig> inputStreamsWithNormalizationConfig = new HashMap<>();
    private Type normalizationProcessorType;

    private NormalizationProcessor() {
    }

    /**
     *
     * @param inputStreamsWithNormalizationConfig normalization configuration for each input stream
     * @param declaredOutputStream output stream of this component
     * @param type type of normalization which can be {@code Type.bulk} or {@code Type.fineGrained}
     */
    public NormalizationProcessor(Map<String, NormalizationConfig> inputStreamsWithNormalizationConfig, Stream declaredOutputStream, Type type) {
        this.inputStreamsWithNormalizationConfig = inputStreamsWithNormalizationConfig;
        this.normalizationProcessorType = type;
        addOutputStream(declaredOutputStream);
    }

    public Type getNormalizationProcessorType() {
        return normalizationProcessorType;
    }

    public Map<String, NormalizationConfig> getInputStreamsWithNormalizationConfig() {
        return inputStreamsWithNormalizationConfig;
    }

    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "NormalizationProcessor{" +
                "inputStreamsWithNormalizationConfig=" + inputStreamsWithNormalizationConfig +
                ", normalizationProcessorType=" + normalizationProcessorType +
                '}'+super.toString();
    }

    public enum Type {
        /**
         * It represents a configuration of using a bulk script for normalizing input to output schema.
         */
        bulk,

        /**
         * It represents a configuration of using a script for each field for normalizing input to output schema.
         */
        fineGrained
    }

}
