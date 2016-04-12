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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Design time Normalization processor configuration for each field in schema. It contains {@link Transformer}s, {@link FieldValueGenerator}s and
 * {@link com.hortonworks.iotas.common.Schema.Field}s to be filtered/removed before they are emitted to the next component in topology.
 *
 */
public class FieldBasedNormalizationConfig extends NormalizationConfig {
    private List<Transformer> transformers;
    // List of input fields filtered or removed. These will not be passed to output fields.
    private List<String> fieldsToBeFiltered;
    private List<FieldValueGenerator> newFieldValueGenerators;

    private FieldBasedNormalizationConfig() {
        this(null, null, null, null);
    }

    public FieldBasedNormalizationConfig(Schema inputSchema, List<Transformer> transformers, List<String> fieldsToBeFiltered, List<FieldValueGenerator> newFieldValueGenerators) {
        super(inputSchema);
        this.transformers = transformers;
        this.fieldsToBeFiltered = fieldsToBeFiltered;
        this.newFieldValueGenerators = newFieldValueGenerators;
    }

    public List<Transformer> getTransformers() {
        return transformers;
    }

    public void setTransformers(List<Transformer> transformers) {
        this.transformers = transformers;
    }

    public List<String> getFieldsToBeFiltered() {
        return fieldsToBeFiltered;
    }

    public void setFieldsToBeFiltered(List<String> fieldsToBeFiltered) {
        this.fieldsToBeFiltered = fieldsToBeFiltered;
    }

    public List<FieldValueGenerator> getNewFieldValueGenerators() {
        return newFieldValueGenerators;
    }

    public void setNewFieldValueGenerators(List<FieldValueGenerator> newFieldValueGenerators) {
        this.newFieldValueGenerators = newFieldValueGenerators;
    }

    //todo this should be called when a topology/component is saved.
    public void validate() {
        Set<String> fields = new HashSet<>();
        for (Schema.Field field : getInputSchema().getFields()) {
            fields.add(field.getName());
        }

        for (String field : fieldsToBeFiltered) {
            if (!fields.contains(field)) {
                throw new RuntimeException("field [" + field + "] does not exist in input schema");
            }
        }
    }

    @Override
    public String toString() {
        return "NormalizationProcessor{" +
                "transformers=" + transformers +
                ", fieldsToBeFiltered=" + fieldsToBeFiltered +
                ", newValueGenerators=" + newFieldValueGenerators +
                '}';
    }
}
