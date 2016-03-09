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
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.common.errors.ProcessingException;
import com.hortonworks.iotas.exception.ParserException;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.layout.design.normalization.NormalizationConfig;
import com.hortonworks.iotas.processor.ProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class NormalizationProcessorRuntime implements ProcessorRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizationProcessorRuntime.class);

    private Map<String, NormalizationRuntime> schemasWithNormalizationRuntime;

    final NormalizationProcessor normalizationProcessor;
    private SchemaValidator schemaValidator;

    public NormalizationProcessorRuntime(NormalizationProcessor normalizationProcessor) {
        this.normalizationProcessor = normalizationProcessor;
    }

    /*
     * todo: It should receive input Stream also and generate output Stream along with IotasEvent. This support should
     * come from processor framework, will add later.
     */
    @Override
    public List<Result> process(IotasEvent iotasEvent) throws ProcessingException {
        String currentStreamId = iotasEvent.getSourceStream() != null ? iotasEvent.getSourceStream() : NormalizationProcessor.DEFAULT_STREAM_ID;
        NormalizationRuntime normalizationRuntime = schemasWithNormalizationRuntime.get(currentStreamId);
        LOG.debug("Normalization runtime for this stream [{}]", normalizationRuntime);

        IotasEvent outputEvent = iotasEvent;
        if (normalizationRuntime != null) {
            try {
                outputEvent =  normalizationRuntime.execute(iotasEvent);
                schemaValidator.validate(iotasEvent.getFieldsAndValues());
            } catch (NormalizationException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.debug("No normalization defined for stream: [{}]", currentStreamId);
        }

        // if it is not found return received tuple without any normalization, it is kind of pass through normalization for streams
        // which does not have normalization configuration.
        return Collections.singletonList(new Result(NormalizationProcessor.DEFAULT_STREAM_ID, Collections.singletonList(outputEvent)));
    }

    @Override
    public void initialize(Map<String, Object> config) {
        final Iterator<Stream> iterator = normalizationProcessor.getOutputStreams().iterator();
        if(!iterator.hasNext()) {
            throw new IllegalStateException("normalization processor "+normalizationProcessor+" does not have output streams");
        }
        Stream outputStream = iterator.next();
        NormalizationRuntime.Factory factory = new NormalizationRuntime.Factory();
        Map<String, NormalizationRuntime> schemaRuntimes = new HashMap<>();
        for (Map.Entry<String, ? extends NormalizationConfig> entry : normalizationProcessor.getInputStreamsWithNormalizationConfig().entrySet()) {
            schemaRuntimes.put(entry.getKey(), factory.create(entry.getValue(), outputStream.getSchema(), normalizationProcessor.getType()));
        }
        schemasWithNormalizationRuntime = schemaRuntimes;
        schemaValidator = new SchemaValidator(outputStream.getSchema());
    }

    @Override
    public void cleanup() {

    }


    /**
     * This class provides validation of given field/values against a schema.
     */
    private static class SchemaValidator {
        private Set<String> fieldNames;
        private Set<Schema.Field> fields;

        private SchemaValidator(Schema schema) {
            fieldNames = new HashSet<>();
            fields = new HashSet<>();
            for (Schema.Field field : schema.getFields()) {
                fields.add(field);
                fieldNames.add(field.getName());
            }
        }

        /**
         * Validates {@code fieldNameValuePairs} with the given {@code schema} instance.
         *
         * @param fieldNameValuePairs field name values to be validated
         * @throws NormalizationException throws when there are any parse errors or validation failures.
         */
        private void validate(Map<String, Object> fieldNameValuePairs) throws NormalizationException {
            LOG.debug("Validating generated output field values: [{}] with [{}]", fieldNameValuePairs, fields);

            for (Map.Entry<String, Object> entry : fieldNameValuePairs.entrySet()) {
                try {
                    Object value = entry.getValue();
                    if (value != null && fieldNames.contains(entry.getKey()) && !fields.contains(new Schema.Field(entry.getKey(), Schema.fromJavaType(value)))) {
                        throw new NormalizationException("Normalized payload does not conform to declared output schema.");
                    }
                } catch (ParserException e) {
                    throw new NormalizationException("Error occurred while validating normalized payload.", e);
                }
            }
        }

    }
}
