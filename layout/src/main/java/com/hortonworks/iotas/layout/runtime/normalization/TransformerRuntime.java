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
import com.hortonworks.iotas.layout.design.normalization.Transformer;
import com.hortonworks.iotas.layout.runtime.script.GroovyScript;
import com.hortonworks.iotas.layout.runtime.script.engine.GroovyScriptEngine;
import com.hortonworks.iotas.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

/**
 * Runtime component of {@link Transformer}. It transforms a given input field to an output field. Output field value will be
 * generated if a groovy script is configured else it copies the input field value.
 *
 */
public class TransformerRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerRuntime.class);

    private final Transformer transformer;
    private GroovyScript<Object> groovyScript;

    private TransformerRuntime(Transformer transformer, GroovyScript groovyScript) {
        this.transformer = transformer;
        this.groovyScript = groovyScript;
    }

    public Object execute(IotasEvent iotasEvent) throws NormalizationException {
        try {
            String inputFieldName = transformer.getInputField().getName();
            if (!iotasEvent.getFieldsAndValues().containsKey(inputFieldName)) {
                throw new IllegalArgumentException("IotasEvent does not have input field: " + inputFieldName);
            }

            Object value = null;
            if (groovyScript != null) {
                LOG.debug("Running script [{}] with input [{}]", groovyScript, iotasEvent);

                value = groovyScript.evaluate(iotasEvent);

                LOG.debug("Computed value is {}. transformer: [{}] script: [{}] input: [{}]", value, transformer, groovyScript, iotasEvent);
                Schema.Type type = transformer.getOutputField().getType();
                if (!type.equals(Schema.fromJavaType(value))) {
                    throw new NormalizationException("Computed value is not of expected type: " + type);
                }
            } else {
                value = iotasEvent.getFieldsAndValues().get(inputFieldName);
                LOG.debug("Input field value returned: {}", value);
            }
            return value;
        } catch (ScriptException | ParserException e) {
            LOG.error("Error occurred while converting input fields in transformer: " + transformer);
            throw new NormalizationException("Error occurred while converting input fields in a normalization", e);
        }

    }

    public Transformer getTransformer() {
        return transformer;
    }

    public static class Builder {
        private Transformer transformer;

        public Builder(Transformer transformer) {
            this.transformer = transformer;
        }

        public TransformerRuntime build() {
            if (transformer.getInputField() == null) {
                throw new IllegalArgumentException("input field should always be set for transformer");
            }
            //todo may add more validation for script.
            GroovyScript groovyScript = null;
            if (transformer.getConverterScript() != null) {
                groovyScript = new GroovyScript<>(transformer.getConverterScript(), new GroovyScriptEngine());
            }

            return new TransformerRuntime(transformer, groovyScript);
        }
    }

    @Override
    public String toString() {
        return "TransformerRuntime{" +
                "transformer=" + transformer +
                ", groovyScript=" + groovyScript +
                '}';
    }
}
