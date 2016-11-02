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

package org.apache.streamline.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.registries.common.Schema;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.registries.common.Schema.Type;
import static org.junit.Assert.assertEquals;

public class SchemaTest {
    @Test
    public void testGetTypeOfValue() {
        final List<String> queryValues = Lists.newArrayList(Boolean.TRUE.toString(), Byte.toString(Byte.MAX_VALUE), Short.toString(Short.MAX_VALUE),
                                                            Integer.toString(Integer.MAX_VALUE), Long.toString(Long.MAX_VALUE), Float.toString(Float.MAX_VALUE), Double.toString(Double.MAX_VALUE), "SOME_STRING");

        final List<Type> expectedTypes = Lists.newArrayList(Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.INTEGER, Type.LONG, Type.FLOAT, Type.DOUBLE, Type.STRING);

        final List<Integer> indexes = Lists.newArrayList(0, 1, 2, 3, 4, 5, 6, 7);

        assertEquals(queryValues.size(), expectedTypes.size());
        assertEquals(queryValues.size(), indexes.size());

        // Try a few times randomizing the locations just to make sure
        for (int j = 0; j < 3; j++) {
            for (int idx : indexes) {
                assertEquals(expectedTypes.get(idx), Type.getTypeOfVal(queryValues.get(idx)));
            }
            Collections.shuffle(indexes);
        }
    }

    @Test
    public void testSchemaFromJson() throws Exception {
        String json = "{\"fields\":[{\"name\":\"field1\", \"type\":\"STRING\"},{\"name\":\"field2\", \"type\":\"STRING\"}]}";
        System.out.println(json);
        ObjectMapper mapper = new ObjectMapper();
        Schema schema = mapper.readValue(json, Schema.class);
        System.out.println(schema);
    }

    @Test
//    @Ignore
    public void testSerializeNestedSchema() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Schema nested = new Schema();
        Schema.Field f1 = new Schema.Field("field1", Type.INTEGER);
        Schema.Field x = new Schema.Field("x", Type.INTEGER);
        Schema.Field y = new Schema.Field("y", Type.INTEGER);
        Schema.Field f2 = Schema.NestedField.of("field2", Arrays.asList(x, y));
        nested.setFields(Arrays.asList(f1, f2));
        String expected = "{\"fields\":[{\"name\":\"field1\",\"type\":\"INTEGER\",\"optional\":false}," +
                "{\"name\":\"field2\",\"type\":\"NESTED\",\"optional\":false,\"fields\":[{\"name\":\"x\",\"type\":\"INTEGER\"," +
                "\"optional\":false},{\"name\":\"y\",\"type\":\"INTEGER\",\"optional\":false}]}]}";
        assertEquals(expected, mapper.writeValueAsString(nested));
        Schema schema2;
        schema2 = mapper.readValue(expected, Schema.class);
        assertEquals(nested, schema2);
    }
}
