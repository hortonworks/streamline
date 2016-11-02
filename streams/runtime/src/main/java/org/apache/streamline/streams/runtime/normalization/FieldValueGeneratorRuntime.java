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
package org.apache.streamline.streams.runtime.normalization;

import org.apache.registries.common.Schema;
import org.apache.registries.common.exception.ParserException;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.layout.component.impl.normalization.FieldValueGenerator;
import org.apache.streamline.streams.runtime.script.GroovyScript;
import org.apache.streamline.streams.runtime.script.engine.GroovyScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

/**
 * This class represents runtime component of {@link FieldValueGenerator}. It generates an output field with a value.
 * That value can be either static value or computed dynamically by running the given script with received StreamlineEvent.
 *
 */
public class FieldValueGeneratorRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(FieldValueGeneratorRuntime.class);

    private final Schema.Field field;
    private GroovyScript<Object> groovyScript;
    private Object value;

    private FieldValueGeneratorRuntime(Schema.Field field) {
        this.field = field;
    }

    public Object generateValue(StreamlineEvent event) throws NormalizationException {
        if(value != null) {
            LOG.debug("Returning default static value [{}] for [{}]", value, event);
            return value;
        }

        try {
            LOG.debug("Running script [{}] with input [{}]", groovyScript, event);
            Object evaluatedValue = groovyScript.evaluate(event);

            LOG.debug("Computed value is {}. field: [{}] script: [{}] input: [{}]", value, field, groovyScript, event);
            Schema.Type type = field.getType();
            if(!type.valueOfSameType(evaluatedValue)) {
                throw new NormalizationException("Computed value is not of expected type: "+ type);
            }
            return evaluatedValue;
        } catch (ScriptException | ParserException e) {
            throw new NormalizationException(e);
        }
    }


    public Schema.Field getField() {
        return field;
    }

    public static class Builder {
        private final Schema.Field field;
        private final String script;
        private final Object value;

        public Builder(FieldValueGenerator fieldValueGenerator) {
            this.field = fieldValueGenerator.getField();
            this.script = fieldValueGenerator.getScript();
            this.value = fieldValueGenerator.getValue();
        }

        public FieldValueGeneratorRuntime build() {

            if(field == null) {
                throw new IllegalArgumentException("field is required");
            }

            if(value == null && script == null) {
                throw new IllegalArgumentException("Either value or script must exist.");
            }

            FieldValueGeneratorRuntime fieldValueGeneratorRuntime = new FieldValueGeneratorRuntime(field);
            if(value != null) {
                fieldValueGeneratorRuntime.value = value;
            } else if(script != null) {
                fieldValueGeneratorRuntime.groovyScript = new GroovyScript<>(script, new GroovyScriptEngine());
            }

            return fieldValueGeneratorRuntime;
        }
    }

    @Override
    public String toString() {
        return "ValueGeneratorRuntime{" +
                "field=" + field +
                ", groovyScript=" + groovyScript +
                ", value=" + value +
                '}';
    }
}
