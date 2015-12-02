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
package com.hortonworks.iotas.layout.runtime.normalization;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.normalization.FieldValueGenerator;
import com.hortonworks.iotas.layout.design.normalization.Transformer;
import com.hortonworks.iotas.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * It represents runtime abstraction of NormalizationProcessor.
 * It involves running list of {@link TransformerRuntime}, filters {@link NormalizationProcessor#fieldsToBeFiltered}
 * and list of {@link FieldValueGeneratorRuntime}.
 * <p>
 * todo:
 * we may still have uber script which can generate output fields for a given map of input fields.
 */
public class NormalizationProcessorRuntime {

    private static Logger LOG = LoggerFactory.getLogger(NormalizationProcessorRuntime.class);

    private final List<TransformerRuntime> transformerRuntimes;
    private final List<FieldValueGeneratorRuntime> fieldValueGeneratorRuntimes;
    private final NormalizationProcessor normalizationProcessor;
    private final Set<Schema.Field> declaredOutputFields;

    private NormalizationProcessorRuntime(Builder builder) {
        this.transformerRuntimes = builder.transformerRuntimes;
        this.fieldValueGeneratorRuntimes = builder.fieldValueGeneratorRuntimes;
        this.normalizationProcessor = builder.normalizationProcessor;
        this.declaredOutputFields = builder.declaredOutputFields;
    }

    public Map<String, Object> execute(IotasEvent iotasEvent) throws NormalizationException {
        Map<String, Object> outputFieldNameValuePairs = new HashMap<String, Object>(iotasEvent.getFieldsAndValues());

        LOG.debug("Received iotas event {}", iotasEvent);

        executeTransformers(iotasEvent, outputFieldNameValuePairs);

        executeFilters(outputFieldNameValuePairs);

        executeOutputFieldValueGenerators(iotasEvent, outputFieldNameValuePairs);

        // todo this should go to a common schema/parser layer
        validate(outputFieldNameValuePairs);

        return outputFieldNameValuePairs;
    }

    /**
     * Executes output FieldValueGenerators which add new output fields generated from given fieldValueGenerators
     */
    private void executeOutputFieldValueGenerators(IotasEvent iotasEvent, Map<String, Object> outputFieldNameValuePairs)
            throws NormalizationException {

        for (FieldValueGeneratorRuntime fieldValueGeneratorRuntime : fieldValueGeneratorRuntimes) {
            String name = fieldValueGeneratorRuntime.getField().getName();
            if (!outputFieldNameValuePairs.containsKey(name)) {
                Object value = fieldValueGeneratorRuntime.generateValue(iotasEvent);
                outputFieldNameValuePairs.putIfAbsent(name, value);
            } else {
                LOG.debug("Default value for field [{}] is not generated as it exists in the received event [{}]", name, iotasEvent);
            }
        }
    }

    /**
     * Executes filters which filters/removes the given fields from output field name/value pairs
     */
    private void executeFilters(Map<String, Object> outputFieldNameValuePairs) {
        List<String> fieldsToBeFiltered = normalizationProcessor.getFieldsToBeFiltered();

        if (fieldsToBeFiltered == null) {
            return;
        }

        for (String filterField : fieldsToBeFiltered) {
            outputFieldNameValuePairs.remove(filterField);
            LOG.debug("Removed filter field [{}] in [{}]", filterField, normalizationProcessor);
        }
    }

    /**
     * Executes transformerRuntimes which transform an input schema field to an output schema field.
     */
    private void executeTransformers(IotasEvent iotasEvent, Map<String, Object> outputFieldNameValuePairs)
            throws NormalizationException {

        for (TransformerRuntime transformerRuntime : transformerRuntimes) {
            Object result = transformerRuntime.execute(iotasEvent);
            outputFieldNameValuePairs.remove(transformerRuntime.getTransformer().getInputField().getName());
            outputFieldNameValuePairs.put(transformerRuntime.getTransformer().getOutputField().getName(), result);
        }
    }

    private void validate(Map<String, Object> outputFieldNameValuePairs) throws NormalizationException {
        LOG.debug("Validating generated output field values: [{}] with [{}]", outputFieldNameValuePairs, declaredOutputFields);

        for (Map.Entry<String, Object> entry : outputFieldNameValuePairs.entrySet()) {
            try {
                Object value = entry.getValue();
                if (value != null && !declaredOutputFields.contains(new Schema.Field(entry.getKey(), Schema.fromJavaType(value)))) {
                    throw new NormalizationException("Normalized payload does not conform to declared output schema.");
                }
            } catch (ParseException e) {
                throw new NormalizationException("Error occurred while validating normalized payload.", e);
            }
        }
    }

    public static class Builder {

        private final NormalizationProcessor normalizationProcessor;
        private List<TransformerRuntime> transformerRuntimes;
        private List<FieldValueGeneratorRuntime> fieldValueGeneratorRuntimes;
        private Set<Schema.Field> declaredOutputFields;

        public Builder(NormalizationProcessor normalizationProcessor) {
            this.normalizationProcessor = normalizationProcessor;
        }

        private void buildTransformerRuntimes() throws NormalizationException {
            if (normalizationProcessor.getTransformers() == null || normalizationProcessor.getTransformers().isEmpty()) {
                Collections.emptyList();
            }

            List<TransformerRuntime> transformerRuntimes = new ArrayList<>();
            for (Transformer transformer : normalizationProcessor.getTransformers()) {
                transformerRuntimes.add(new TransformerRuntime.Builder(transformer).build());
            }
            this.transformerRuntimes = transformerRuntimes;
        }

        private void buildValueGeneratorRuntimes() throws NormalizationException {
            if (normalizationProcessor.getNewFieldValueGenerators() == null || normalizationProcessor.getNewFieldValueGenerators().isEmpty()) {
                Collections.emptyList();
            }

            List<FieldValueGeneratorRuntime> fieldValueGeneratorRuntimes = new ArrayList<>();
            for (FieldValueGenerator fieldValueGenerator : normalizationProcessor.getNewFieldValueGenerators()) {
                fieldValueGeneratorRuntimes.add(new FieldValueGeneratorRuntime.Builder(fieldValueGenerator).build());
            }
            this.fieldValueGeneratorRuntimes = fieldValueGeneratorRuntimes;
        }

        private void buildDeclaredOutput() {
            this.declaredOutputFields = new HashSet<>(normalizationProcessor.getDeclaredOutput());
        }

        public NormalizationProcessorRuntime build() throws NormalizationException {
            buildTransformerRuntimes();
            buildValueGeneratorRuntimes();
            buildDeclaredOutput();

            return new NormalizationProcessorRuntime(this);
        }

    }

    @Override
    public String toString() {
        return "NormalizationProcessorRuntime{" +
                "transformerRuntimes=" + transformerRuntimes +
                ", valueGeneratorRuntimes=" + fieldValueGeneratorRuntimes +
                ", normalizationProcessor=" + normalizationProcessor +
                ", declaredOutputFields=" + declaredOutputFields +
                '}';
    }
}
