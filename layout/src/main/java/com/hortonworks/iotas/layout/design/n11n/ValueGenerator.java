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
package com.hortonworks.iotas.layout.design.n11n;

import com.hortonworks.iotas.common.Schema;

import java.io.Serializable;

/**
 *
 */
public class ValueGenerator implements Serializable {
    private Schema.Field field;
    private String script;
    private Object value;

    public ValueGenerator() {
    }

    public ValueGenerator(Schema.Field field, String script, Object value) {
        this.field = field;
        this.script = script;
        this.value = value;
    }

    public Schema.Field getField() {
        return field;
    }

    public void setField(Schema.Field field) {
        this.field = field;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValueGenerator)) return false;

        ValueGenerator that = (ValueGenerator) o;

        if (!field.equals(that.field)) return false;
        if (!script.equals(that.script)) return false;
        return value.equals(that.value);

    }

    @Override
    public int hashCode() {
        int result = field.hashCode();
        result = 31 * result + script.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ValueGenerator{" +
                "field=" + field +
                ", script='" + script + '\'' +
                ", value=" + value +
                '}';
    }
}
