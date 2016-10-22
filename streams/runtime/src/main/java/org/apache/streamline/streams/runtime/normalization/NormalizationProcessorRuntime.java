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

import org.apache.streamline.common.Schema;
import org.apache.streamline.common.exception.ParserException;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.exception.ProcessingException;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationConfig;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationProcessor;
import org.apache.streamline.streams.runtime.ProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
     * todo: It should receive input Stream also and generate output Stream along with StreamlineEvent. This support should
     * come from processor framework, will add later.
     */
    @Override
    public List<Result> process(StreamlineEvent event) throws ProcessingException {
        String currentStreamId = event.getSourceStream() != null ? event.getSourceStream() : NormalizationProcessor.DEFAULT_STREAM_ID;
        NormalizationRuntime normalizationRuntime = schemasWithNormalizationRuntime.get(currentStreamId);
        LOG.debug("Normalization runtime for this stream [{}]", normalizationRuntime);

        StreamlineEvent outputEvent = event;
        if (normalizationRuntime != null) {
            try {
                outputEvent =  normalizationRuntime.execute(event);
                schemaValidator.validate(outputEvent.getFieldsAndValues());
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
            schemaRuntimes.put(entry.getKey(), factory.create(entry.getValue(), outputStream.getSchema(), normalizationProcessor.getNormalizationProcessorType()));
        }
        schemasWithNormalizationRuntime = schemaRuntimes;
        schemaValidator = new SchemaValidator(outputStream.getSchema());
    }

    @Override
    public void cleanup() {

    }


    /**
     * This class provides lenient validation of given field/values against a schema.
     */
    private static class SchemaValidator {
        private static final Logger LOG = LoggerFactory.getLogger(SchemaValidator.class);

        private final Map<String, Schema.Field> fields;

        private SchemaValidator(Schema schema) {
            fields = new HashMap<>();
            for (Schema.Field field : schema.getFields()) {
                fields.put(field.getName(), field);
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
                    if(!fields.containsKey(entry.getKey())) {
                        LOG.error("Schema does not contain field with name [{}]", entry.getKey());
                        throw new NormalizationException("Normalized payload does not conform to declared output schema.");
                    }

                    Object value = entry.getValue();
                    if (value != null) {
                        final Schema.Field field = new Schema.Field(entry.getKey(), Schema.fromJavaType(value));
                        if (!fields.containsValue(field)) {
                            LOG.error("Schema does not contain field with type [{}]", entry.getKey());
                            throw new NormalizationException("Normalized payload does not conform to declared output schema.");
                        }
                    }
                } catch (ParserException e) {
                    throw new NormalizationException("Error occurred while validating normalized payload.", e);
                }
            }
        }

    }
}
