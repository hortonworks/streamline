/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/

package com.hortonworks.streamline.streams.runtime.normalization;

import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.layout.component.impl.normalization.FieldBasedNormalizationConfig;
import com.hortonworks.streamline.streams.layout.component.impl.normalization.FieldValueGenerator;
import com.hortonworks.streamline.streams.layout.component.impl.normalization.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * It represents runtime abstraction of NormalizationProcessor.
 * It involves running list of {@link TransformerRuntime}, filters {@link FieldBasedNormalizationConfig#fieldsToBeFiltered}
 * and list of {@link FieldValueGeneratorRuntime}.
 *
 */
public class FieldBasedNormalizationRuntime extends NormalizationRuntime {

    private static final Logger LOG = LoggerFactory.getLogger(FieldBasedNormalizationRuntime.class);

    private final List<TransformerRuntime> transformerRuntimes;
    private final List<FieldValueGeneratorRuntime> fieldValueGeneratorRuntimes;
    private final FieldBasedNormalizationConfig normalizationConfig;
    private final List<String> fieldsToBeFiltered;

    private FieldBasedNormalizationRuntime(Builder builder) {
        super(builder.normalizationConfig);
        this.transformerRuntimes = builder.transformerRuntimes;
        this.fieldValueGeneratorRuntimes = builder.fieldValueGeneratorRuntimes;
        this.normalizationConfig = builder.normalizationConfig;
        final List<String> fieldsToBeFiltered = normalizationConfig.getFieldsToBeFiltered();
        this.fieldsToBeFiltered = fieldsToBeFiltered == null || fieldsToBeFiltered.isEmpty() ? builder.fieldsTobeFiltered : fieldsToBeFiltered;
    }

    public Map<String, Object> normalize(StreamlineEvent event) throws NormalizationException {
        Map<String, Object> outputFieldNameValuePairs = new HashMap<>(event);

        LOG.debug("Received streamline event {}", event);

        executeTransformers(event, outputFieldNameValuePairs);

        executeOutputFieldValueGenerators(event, outputFieldNameValuePairs);

        // filtered fields should not exist in the output event.
        executeFilters(outputFieldNameValuePairs);

        return outputFieldNameValuePairs;
    }

    /**
     * Executes output FieldValueGenerators which add new output fields generated from given fieldValueGenerators
     */
    private void executeOutputFieldValueGenerators(StreamlineEvent event, Map<String, Object> outputFieldNameValuePairs)
            throws NormalizationException {

        for (FieldValueGeneratorRuntime fieldValueGeneratorRuntime : fieldValueGeneratorRuntimes) {
            String name = fieldValueGeneratorRuntime.getField().getName();
            if (!outputFieldNameValuePairs.containsKey(name)) {
                Object value = fieldValueGeneratorRuntime.generateValue(event);
                outputFieldNameValuePairs.put(name, value);
            } else {
                LOG.debug("Default value for field [{}] is not generated as it exists in the received event [{}]", name, event);
            }
        }
    }

    /**
     * Executes filters which filters/removes the given fields from output field name/value pairs
     */
    private void executeFilters(Map<String, Object> outputFieldNameValuePairs) {
        LOG.debug("Fields to be filtered: [{}] ", fieldsToBeFiltered);
        for (String filterField : fieldsToBeFiltered) {
            outputFieldNameValuePairs.remove(filterField);
            LOG.debug("Removed filter field [{}] in [{}]", filterField, normalizationConfig);
        }
    }

    /**
     * Executes transformerRuntimes which transform an input schema field to an output schema field.
     */
    private void executeTransformers(StreamlineEvent event, Map<String, Object> outputFieldNameValuePairs)
            throws NormalizationException {

        for (TransformerRuntime transformerRuntime : transformerRuntimes) {
            Object result = transformerRuntime.execute(event);
            outputFieldNameValuePairs.remove(transformerRuntime.getTransformer().getInputField().getName());
            outputFieldNameValuePairs.put(transformerRuntime.getTransformer().getOutputField().getName(), result);
        }
    }

    public static class Builder {
        private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

        private final FieldBasedNormalizationConfig normalizationConfig;
        private final Schema declaredOutputSchema;
        private List<TransformerRuntime> transformerRuntimes;
        private List<FieldValueGeneratorRuntime> fieldValueGeneratorRuntimes;
        private final List<String> fieldsTobeFiltered = new ArrayList<>();

        public Builder(FieldBasedNormalizationConfig normalizationConfig, Schema declaredOutputSchema) {
            this.normalizationConfig = normalizationConfig;
            this.declaredOutputSchema = declaredOutputSchema;
        }

        private void buildTransformerRuntimes() {
            if (normalizationConfig.getTransformers() == null || normalizationConfig.getTransformers().isEmpty()) {
                transformerRuntimes = Collections.emptyList();
            } else {
                List<TransformerRuntime> transformerRuntimes = new ArrayList<>();
                for (Transformer transformer : normalizationConfig.getTransformers()) {
                    transformerRuntimes.add(new TransformerRuntime.Builder(transformer).build());
                }
                this.transformerRuntimes = transformerRuntimes;
            }
        }

        private void buildValueGeneratorRuntimes() {
            if (normalizationConfig.getNewFieldValueGenerators() == null || normalizationConfig.getNewFieldValueGenerators().isEmpty()) {
                fieldValueGeneratorRuntimes = Collections.emptyList();
            } else {
                List<FieldValueGeneratorRuntime> fieldValueGeneratorRuntimes = new ArrayList<>();
                for (FieldValueGenerator fieldValueGenerator : normalizationConfig.getNewFieldValueGenerators()) {
                    fieldValueGeneratorRuntimes.add(new FieldValueGeneratorRuntime.Builder(fieldValueGenerator).build());
                }
                this.fieldValueGeneratorRuntimes = fieldValueGeneratorRuntimes;
            }
        }

        private void buildFilters() {
            for (Schema.Field inputField : normalizationConfig.getInputSchema().getFields()) {
                if(!declaredOutputSchema.getFields().contains(inputField)) {
                    fieldsTobeFiltered.add(inputField.getName());
                }
            }
            LOG.debug("Creating fields to be filtered: [{}]", fieldsTobeFiltered);
        }

        public FieldBasedNormalizationRuntime build() {
            buildTransformerRuntimes();
            buildValueGeneratorRuntimes();
            buildFilters();

            return new FieldBasedNormalizationRuntime(this);
        }

    }

    @Override
    public String toString() {
        return "FieldBasedNormalizationRuntime{" +
                "transformerRuntimes=" + transformerRuntimes +
                ", fieldValueGeneratorRuntimes=" + fieldValueGeneratorRuntimes +
                ", normalizationConfig=" + normalizationConfig +
                ", fieldsToBeFiltered=" + fieldsToBeFiltered +
                '}';
    }
}
