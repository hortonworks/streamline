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
package com.hortonworks.streamline.streams.layout.component.rule.expression;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class Udf implements Serializable {
    public enum Type {
        FUNCTION, AGGREGATE
    }
    private String name;
    private Type type;
    private String className;

    // for jackson
    private Udf() {
    }

    public Udf(String name, String className, Type type) {
        this.name = name;
        this.className = className;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @JsonIgnore
    public boolean isAggregate() {
        return type == Type.AGGREGATE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Udf udf = (Udf) o;

        if (name != null ? !name.equals(udf.name) : udf.name != null) return false;
        if (type != udf.type) return false;
        return className != null ? className.equals(udf.className) : udf.className == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (className != null ? className.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Udf{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", className='" + className + '\'' +
                '}';
    }
}
