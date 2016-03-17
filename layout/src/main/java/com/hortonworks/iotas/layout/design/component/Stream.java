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
package com.hortonworks.iotas.layout.design.component;

import com.hortonworks.iotas.common.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stream represents the schema of the
 * output stream that a component emits.
 */
public class Stream {
    private String id;
    private Schema schema;

    public enum Grouping {
        GROUPING_SHUFFLE
    }

    /**
     * Stream where all fields are of String type.
     * @param fields
     */
    public Stream(String... fields) {
        List<Schema.Field> schemaFields = new ArrayList<>();
        for(String field: fields) {
            schemaFields.add(new Schema.Field(field, Schema.Type.STRING));
        }
        this.id = UUID.randomUUID().toString();
        this.schema = Schema.of(schemaFields);
    }

    public Stream(Schema schema) {
        this.id = UUID.randomUUID().toString();
        this.schema = schema;
    }

    public String getId() {
        return id;
    }

    public Schema fields() {
        return schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stream stream = (Stream) o;

        return id != null ? id.equals(stream.id) : stream.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Stream{" +
                "id='" + id + '\'' +
                ", fields=" + schema +
                '}';
    }
}
