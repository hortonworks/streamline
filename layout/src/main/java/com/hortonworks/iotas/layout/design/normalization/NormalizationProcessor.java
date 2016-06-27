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
package com.hortonworks.iotas.layout.design.normalization;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.topology.component.IotasProcessor;
import com.hortonworks.iotas.topology.component.Stream;

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
public class NormalizationProcessor extends IotasProcessor {
    public static final String DEFAULT_STREAM_ID = "default";
    public static final String CONFIG_KEY_TYPE = "type";
    public static final String CONFIG_KEY_NORMALIZATION = "normalization-config";

    /**
     * {@link NormalizationConfig} for each inbound stream for this component.
     */
    private Map<String, NormalizationConfig> inputStreamsWithNormalizationConfig;
    private Type type;

    private NormalizationProcessor() {
        inputStreamsWithNormalizationConfig = new HashMap<>();
    }

    /**
     *
     * @param inputStreamsWithNormalizationConfig normalization configuration for each input stream
     * @param declaredOutputStream output stream of this component
     * @param type type of normalization which can be {@code Type.bulk} or {@code Type.fineGrained}
     */
    public NormalizationProcessor(Map<String, NormalizationConfig> inputStreamsWithNormalizationConfig, Stream declaredOutputStream, Type type) {
        this.inputStreamsWithNormalizationConfig = inputStreamsWithNormalizationConfig;
        this.type = type;
        addOutputStream(declaredOutputStream);
    }

    public Type getType() {
        return type;
    }

    public Map<String, NormalizationConfig> getInputStreamsWithNormalizationConfig() {
        return inputStreamsWithNormalizationConfig;
    }

    @Override
    public String toString() {
        return "NormalizationProcessor{" +
                "inputStreamsWithNormalizationConfig=" + inputStreamsWithNormalizationConfig +
                ", type=" + type +
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
