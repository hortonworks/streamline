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
import com.hortonworks.iotas.layout.design.normalization.FieldValueGenerator;
import com.hortonworks.iotas.layout.runtime.script.GroovyScript;
import com.hortonworks.iotas.layout.runtime.script.engine.GroovyScriptEngine;
import com.hortonworks.iotas.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

/**
 * This class represents runtime component of {@link FieldValueGenerator}. It generates an output field with a value.
 * That value can be either static value or computed dynamically by running the given script with received IotasEvent.
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

    public Object generateValue(IotasEvent iotasEvent) throws NormalizationException {
        if(value != null) {
            LOG.debug("Returning default static value [{}] for [{}]", value, iotasEvent);
            return value;
        }

        try {
            LOG.debug("Running script [{}] with input [{}]", groovyScript, iotasEvent);
            Object evaluatedValue = groovyScript.evaluate(iotasEvent);

            LOG.debug("Computed value is {}. field: [{}] script: [{}] input: [{}]", value, field, groovyScript, iotasEvent);
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
        private Schema.Field field;
        private String script;
        private Object value;

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
