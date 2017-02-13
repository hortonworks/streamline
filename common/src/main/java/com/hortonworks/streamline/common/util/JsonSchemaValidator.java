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
package com.hortonworks.streamline.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.IOException;
import java.net.URL;

public class JsonSchemaValidator {
    /**
     *
     * @param jsonSchema URL representing a resource that is a schema json
     * @param jsonData JSON.stringify(json) string representing
     *                       data json to be validated against the schema
     * @return
     */

    public static boolean isValidJsonAsPerSchema (URL jsonSchema,
                                                  String jsonData) throws IOException, ProcessingException {
        boolean result;
        final JsonNode schemaNode = JsonLoader.fromURL(jsonSchema);
        final JsonNode dataNode = JsonLoader.fromString(jsonData);
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        result = schema.validate(dataNode).isSuccess();
        //throw new RuntimeException("Error while validating json: ", ex);
        return result;
    }

}
