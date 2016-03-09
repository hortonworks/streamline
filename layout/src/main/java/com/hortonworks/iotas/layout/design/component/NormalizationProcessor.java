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


import com.hortonworks.iotas.layout.design.normalization.FieldValueGenerator;
import com.hortonworks.iotas.layout.design.normalization.Transformer;

import java.util.List;

/**
 * Design time component of Normalization processor containing configuration of Transformers,
 * fields to be filtered/removed and new output field/value generators.
 *
 */
public class NormalizationProcessor extends Processor {
    private List<Transformer> transformers;
    // List of input fields filtered or removed. These will not be passed to output fields.
    private List<String> fieldsToBeFiltered;
    private List<FieldValueGenerator> newFieldValueGenerators;

    public NormalizationProcessor() {
    }

    public NormalizationProcessor(List<Transformer> transformers, List<String> fieldsToBeFiltered, List<FieldValueGenerator> newFieldValueGenerators) {
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

    @Override
    public String toString() {
        return "NormalizationProcessor{" +
                "transformers=" + transformers +
                ", fieldsToBeFiltered=" + fieldsToBeFiltered +
                ", newValueGenerators=" + newFieldValueGenerators +
                '}';
    }
}
