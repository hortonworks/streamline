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
package com.hortonworks.streamline.streams.catalog.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Reader that read configuration stream based on the file type.
 */
public class ConfigFileReader {
    private ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> readConfig(ConfigFileType confFileType, InputStream configFileStream) throws IOException {
        if (confFileType == null) {
            throw new IllegalArgumentException("Config file type cannot be null.");
        }

        switch (confFileType) {
            case HADOOP_XML:
                return readConfigFromHadoopXmlType(configFileStream);

            case PROPERTIES:
            case ZOOKEEPER_CFG:
                return readConfigFromJavaPropertiesType(configFileStream);

            case YAML:
                return readConfigFromYamlType(configFileStream);

            default:
                throw new IllegalArgumentException("Reader does not support file type. file type: " + confFileType);
        }
    }

    private Map<String, String> readConfigFromHadoopXmlType(InputStream configFileStream) throws IOException {
        ObjectReader reader = new XmlMapper().reader();
        HadoopXml hadoopXml = reader.forType(HadoopXml.class).readValue(configFileStream);

        Map<String, String> configMap = new HashMap<>();
        for (HadoopXml.HadoopXmlProperty property : hadoopXml.getProperties()) {
            configMap.put(property.getName(), property.getValue());
        }

        return configMap;
    }

    private Map<String, String> readConfigFromJavaPropertiesType(InputStream configFileStream) throws IOException {
        ObjectReader reader = new JavaPropsMapper().reader(JavaPropsSchema.emptySchema().withoutPathSeparator());
        return reader.forType(new TypeReference<Map<String, Object>>() {}).readValue(configFileStream);
    }

    private Map<String, String> readConfigFromYamlType(InputStream configFileStream) throws IOException {
        ObjectReader reader = new YAMLMapper().reader();
        Map<String, Object> confMap = reader.forType(new TypeReference<Map<String, Object>>() {})
                .readValue(configFileStream);
        return confMap.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> convertValueAsString(e.getValue())));
    }

    private String convertValueAsString(Object value) {
        if (value == null) {
            // treat this as special case, as Ambari does it
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
