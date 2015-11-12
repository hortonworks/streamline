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
import com.hortonworks.iotas.layout.design.n11n.ValueGenerator;
import com.hortonworks.iotas.layout.runtime.script.GroovyScript;
import com.hortonworks.iotas.layout.runtime.script.engine.GroovyScriptEngine;
import com.hortonworks.iotas.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

/**
 *
 */
public class ValueGeneratorRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(ValueGeneratorRuntime.class);

    private final Schema.Field field;
    private GroovyScript<Object> groovyScript;
    private Object value;

    public ValueGeneratorRuntime(Schema.Field field) {
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
        } catch (ScriptException | ParseException e) {
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

        public Builder withValueGenerator(ValueGenerator valueGenerator) {
            this.field = valueGenerator.getField();
            this.script = valueGenerator.getScript();
            this.value = valueGenerator.getValue();

            return this;
        }

        public Builder withField(Schema.Field field) {
            this.field = field;
            return this;
        }

        public Builder withScript(String script) {
            this.script = script;
            return this;
        }

        public Builder withValue(Object value) {
            this.value = value;
            return this;
        }

        public ValueGeneratorRuntime build() throws NormalizationException {

            if(field == null) {
                throw new NormalizationException("field should always be set.");
            }

            if(value == null && script == null) {
                throw new NormalizationException("Either value or script must exist.");
            }

            ValueGeneratorRuntime valueGeneratorRuntime = new ValueGeneratorRuntime(field);
            if(value != null) {
                valueGeneratorRuntime.value = value;
            } else if(script != null) {
                valueGeneratorRuntime.groovyScript = new GroovyScript<>(script, new GroovyScriptEngine());
            }

            return valueGeneratorRuntime;
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
