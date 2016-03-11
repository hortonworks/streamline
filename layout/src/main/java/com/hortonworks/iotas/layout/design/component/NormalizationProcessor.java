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

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.normalization.NormalizationConfig;

import java.util.HashSet;
import java.util.List;
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

    /**
     * {@link NormalizationConfig} for each inbound stream for this component.
     */
    public Map<String, NormalizationConfig> inputStreamsWithConfig;

    public NormalizationProcessor(Map<String, NormalizationConfig> inputStreamsWithConfig) {
        super(new HashSet<Stream>());
        this.inputStreamsWithConfig = inputStreamsWithConfig;
    }

    public void setDeclaredOutput(List<Schema.Field> declaredOutput) {
        addOutputStream(new Stream(declaredOutput));
        Schema outputSchema = Schema.of(declaredOutput);
        for (NormalizationConfig normalizationConfig : inputStreamsWithConfig.values()) {
            normalizationConfig.setOutputSchema(outputSchema);
        }
    }

    @Override
    public String toString() {
        return "NormalizationProcessor{" +
                "inputStreamsWithConfig=" + inputStreamsWithConfig +
                '}' + super.toString();
    }
}
