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
package org.apache.streamline.streams.layout.component.impl.normalization;

import org.apache.registries.common.Schema;

import java.io.Serializable;

/**
 * This class represents design time value generator of a given field which is part of
 * {@link NormalizationProcessor}.
 *
 */
public class FieldValueGenerator implements Serializable {
    private Schema.Field field;
    private String script;
    private Object value;

    public FieldValueGenerator() {
    }

    public FieldValueGenerator(Schema.Field field, Object value) {
        this.field = field;
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
        if (!(o instanceof FieldValueGenerator)) return false;

        FieldValueGenerator that = (FieldValueGenerator) o;

        if (field != null ? !field.equals(that.field) : that.field != null) return false;
        if (script != null ? !script.equals(that.script) : that.script != null) return false;
        return !(value != null ? !value.equals(that.value) : that.value != null);

    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (script != null ? script.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
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
