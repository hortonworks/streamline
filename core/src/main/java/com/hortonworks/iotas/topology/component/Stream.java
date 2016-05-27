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
package com.hortonworks.iotas.topology.component;

import com.hortonworks.iotas.common.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stream represents the schema of the
 * output stream that a component emits.
 */
public class Stream implements Serializable {
    private String id;
    private Schema schema;

    public enum Grouping {
        /**
         * Shuffle grouping
         */
        SHUFFLE,
        /**
         * Fields grouping
         */
        FIELDS
    }

    private Stream() {
    }

    /**
     * Stream where all fields are of String type.
     *
     * @param fields the fields
     */
    public Stream(String... fields) {
        this(schemaFields(fields));
    }

    /**
     * A stream of the given list of fields.
     *
     * @param fields the fields
     */
    public Stream(List<Schema.Field> fields) {
        this(UUID.randomUUID().toString(), fields);
    }

    /**
     * A stream of the given list of fields.
     *
     * @param id the unique id of the stream
     * @param fields the fields
     */
    public Stream(String id, List<Schema.Field> fields) {
        this(id, Schema.of(fields));
    }

    /**
     * A stream with the given schema and random UUID string as the stream id.
     *
     * @param schema the schema of the stream
     */
    public Stream(Schema schema) {
        this(UUID.randomUUID().toString(), schema);
    }
    /**
     * A stream with the given id and schema.
     *
     * @param id the unique id of the stream
     * @param schema the schema of the stream
     */
    public Stream(String id, Schema schema) {
        this.id = id;
        this.schema = schema;
    }

    public String getId() {
        return id;
    }

    public Schema getSchema() {
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
                ", schema=" + schema +
                '}';
    }

    private static List<Schema.Field> schemaFields(String... fields) {
        List<Schema.Field> schemaFields = new ArrayList<>();
        for(String field: fields) {
            schemaFields.add(Schema.Field.of(field, Schema.Type.STRING));
        }
        return schemaFields;
    }
}
