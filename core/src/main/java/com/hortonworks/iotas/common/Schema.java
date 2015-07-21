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
package com.hortonworks.iotas.common;

import java.util.ArrayList;
import java.util.List;

public class Schema {
    public enum Type {
        BOOLEAN,
        BYTE, // 8-bit signed integer
        SHORT, // 16-bit
        INTEGER, // 32-bit
        LONG, // 64-bit
        FLOAT,
        DOUBLE,
        STRING,
        BINARY // raw data
    }

    public static class Field {
        String name;
        Type type;

        public Field(String name, Type type){
            this.name = name;
            this.type = type;
        }

        public String getName(){
            return this.name;
        }

        public Type getType(){
            return this.type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Field)) return false;

            Field field = (Field) o;

            if (name != null ? !name.equals(field.name) : field.name != null) return false;
            return type == field.type;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }

        //TODO: need to replace with actual ToJson from Json instead of toString/fromString
        public String toString() {
            return this.getName() + "=" + this.getType().name();
        }

        public static Field fromString(String str) {
            String[] split = str.split("=");
            return new Field(split[0], Type.valueOf(split[1]));
        }
    }

    private List<Field> fields;


    public Schema(){
        this.fields = new ArrayList<Field>();
    }

    public Schema(List<Field> fields){
        this.fields = fields;
    }

    public Schema(Field... fields){
        this();
        for(Field field : fields){
            this.fields.add(field);
        }
    }

    public List<Field> getFields(){
        return this.fields;
    }

    //TODO: need to replace with actual ToJson from Json
    public String toString() {
        if(fields == null) return "null";
        if(fields.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for(Field field : fields) {
            sb.append(field.toString()).append(",");
        }
        return sb.append("}").toString();
    }

    public static Schema fromString(String str) {
        if(str.equals("null")) return null;
        if(str.equals("{}")) return new Schema();

        str = str.replace("{","");
        str = str.replace("{","");


        String[] split = str.split(",");
        List<Field> fields = new ArrayList<Field>();
        for(String fieldStr : split) {
            fields.add(Field.fromString(fieldStr));
        }
        return new Schema(fields);
    }

}
