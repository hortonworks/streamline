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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.Storable;

import java.io.IOException;
import java.util.Map;

/**
 * Utility methods for the core package.
 */
public final class CoreUtils {

    public static final String CATALOG_ROOT_URL = "catalog.root.url";
    public static final String LOCAL_JAR_PATH = "local.jar.path";

    private CoreUtils() {
    }

    public static <T extends Storable> T jsonToStorable(String json, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, clazz);
    }

    public static String storableToJson(Storable storable) throws IOException {
        return storable != null ? new ObjectMapper().writeValueAsString(storable) : null;
    }

    /**
     * This method takes in a schema represented as a map and returns a {@link Schema}
     * @param schemaConfig A map representing {@link Schema}
     * @return schema generated from the map argument
     * @throws IOException
     */
    public static Schema getSchemaFromConfig (Map schemaConfig) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String inputSchemaStr = objectMapper.writeValueAsString(schemaConfig);
        Schema schema = objectMapper.readValue(inputSchemaStr, Schema.class);
        return schema;
    }
}
