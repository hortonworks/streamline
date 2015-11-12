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
package com.hortonworks.iotas.layout.runtime.n11n;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.n11n.Transformer;
import com.hortonworks.iotas.layout.design.n11n.ValueGenerator;
import com.hortonworks.iotas.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class NormalizationProcessorRuntime {

    private static Logger LOG = LoggerFactory.getLogger(NormalizationProcessorRuntime.class);

    private final List<TransformerRuntime> transformerRuntimes;
    private final List<ValueGeneratorRuntime> valueGeneratorRuntimes;
    private final NormalizationProcessor normalizationProcessor;
    private final Set<Schema.Field> declaredOutputSet;

    private NormalizationProcessorRuntime(List<TransformerRuntime> transformerRuntimes,
                                          List<ValueGeneratorRuntime> valueGeneratorRuntimes, NormalizationProcessor normalizationProcessor) {
        this.transformerRuntimes = transformerRuntimes;
        this.valueGeneratorRuntimes = valueGeneratorRuntimes;
        this.normalizationProcessor = normalizationProcessor;
        declaredOutputSet = new HashSet<>(normalizationProcessor.getDeclaredOutput());
    }

    public Map<String, Object> execute(IotasEvent iotasEvent) throws NormalizationException {
        Map<String, Object> outputFieldValuesMap = new HashMap<String, Object>(iotasEvent.getFieldsAndValues());

        LOG.debug("Received iotas event {}", iotasEvent);

        // run transformers
        for (TransformerRuntime transformerRuntime : transformerRuntimes) {
            Object result = transformerRuntime.execute(iotasEvent);
            outputFieldValuesMap.remove(transformerRuntime.getTransformer().getInputField().getName());
            outputFieldValuesMap.put(transformerRuntime.getTransformer().getOutputField().getName(), result);
        }

        // run filters
        for (String filterField : normalizationProcessor.getNormalizer().getFieldsToBeFiltered()) {
            outputFieldValuesMap.remove(filterField);
            LOG.debug("Removed filter field [{}] in [{}]", filterField, normalizationProcessor);
        }

        // run value generators, fields to be added, may need to run script to compute the default value
        for (ValueGeneratorRuntime valueGeneratorRuntime : valueGeneratorRuntimes) {
            String name = valueGeneratorRuntime.getField().getName();
            if(!outputFieldValuesMap.containsKey(name)) {
                Object value = valueGeneratorRuntime.generateValue(iotasEvent);
                outputFieldValuesMap.putIfAbsent(name, value);
            } else {
                LOG.debug("Default value for field [{}] is not generated as it exists in the received event [{}]", name, iotasEvent);
            }
        }

        // todo this should go to a common schema/parser layer
        validate(outputFieldValuesMap);

        return outputFieldValuesMap;
    }

    private void validate(Map<String, Object> outputFieldValuesMap) throws NormalizationException {
        LOG.debug("Validating generated output field values: [{}] with [{}]", outputFieldValuesMap, declaredOutputSet);

        for (Map.Entry<String,Object> entry : outputFieldValuesMap.entrySet()) {
            try {
                Object value = entry.getValue();
                if(value != null && !declaredOutputSet.contains(new Schema.Field(entry.getKey(), Schema.fromJavaType(value)))) {
                    throw new NormalizationException("Normalized payload does not conform to declared output schema.");
                }
            } catch (ParseException e) {
                throw new NormalizationException("Error occurred while validating normalized payload.", e);
            }
        }
    }

    public static class Builder {

        private final NormalizationProcessor normalizationProcessor;

        public Builder(NormalizationProcessor normalizationProcessor) {
            this.normalizationProcessor = normalizationProcessor;
        }

        private List<TransformerRuntime> buildTransformerRuntimes() throws NormalizationException {
            List<TransformerRuntime> transformers = new ArrayList<>();
            for (Transformer transformer : normalizationProcessor.getNormalizer().getTransformers()) {
                transformers.add(new TransformerRuntime.Builder(transformer).build());
            }
            return transformers;
        }

        private List<ValueGeneratorRuntime> buildValueGeneratorRuntimes() throws NormalizationException {
            List<ValueGeneratorRuntime> valueGeneratorRuntimes = new ArrayList<>();
            for (ValueGenerator valueGenerator : normalizationProcessor.getNormalizer().getNewValueGenerators()) {
                valueGeneratorRuntimes.add(new ValueGeneratorRuntime.Builder().withValueGenerator(valueGenerator).build());
            }
            return valueGeneratorRuntimes;
        }

        public NormalizationProcessorRuntime build() throws NormalizationException {
            List<TransformerRuntime> transformerRuntimes = buildTransformerRuntimes();
            List<ValueGeneratorRuntime> valueGeneratorRuntimes = buildValueGeneratorRuntimes();
            return new NormalizationProcessorRuntime(transformerRuntimes, valueGeneratorRuntimes, normalizationProcessor);
        }

    }

    @Override
    public String toString() {
        return "NormalizationProcessorRuntime{" +
                "transformerRuntimes=" + transformerRuntimes +
                ", valueGeneratorRuntimes=" + valueGeneratorRuntimes +
                ", normalizationProcessor=" + normalizationProcessor +
                ", declaredOutputSet=" + declaredOutputSet +
                '}';
    }
}
