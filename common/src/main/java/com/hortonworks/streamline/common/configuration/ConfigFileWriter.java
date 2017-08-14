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
package com.hortonworks.streamline.common.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Writer that write configuration to file based on the file type.
 */
public class ConfigFileWriter {
  public void writeConfigToFile(ConfigFileType confFileType, Map<String, String> configuration,
      File destPath) throws IOException {
    if (confFileType == null) {
      throw new IllegalArgumentException("Config file type cannot be null.");
    }

    switch (confFileType) {
    case HADOOP_XML:
      writeConfigToHadoopXmlTypeFile(configuration, destPath);
      break;

    case PROPERTIES:
      writeConfigToPropertiesTypeFile(configuration, destPath);
      break;

    case YAML:
      writeConfigToYamlTypeFile(configuration, destPath);
      break;

    default:
      throw new IllegalArgumentException("Writer does not support file type. file type: " + confFileType);
    }
  }

  private void writeConfigToHadoopXmlTypeFile(Map<String, String> configuration, File destPath)
      throws IOException {
    ObjectMapper objectMapper = new XmlMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

    HadoopXml hadoopXml = new HadoopXml();
    for (Map.Entry<String, String> property : configuration.entrySet()) {
      hadoopXml.addProperty(new HadoopXml.HadoopXmlProperty(property.getKey(), property.getValue()));
    }

    objectMapper.writeValue(destPath, hadoopXml);
  }

  private void writeConfigToPropertiesTypeFile(Map<String, String> configuration, File destPath)
      throws IOException {
    ObjectMapper objectMapper = new JavaPropsMapper();
    objectMapper.writeValue(destPath, configuration);
  }

  private void writeConfigToYamlTypeFile(Map<String, String> configuration, File destPath)
      throws IOException {
    ObjectMapper objectMapper = new YAMLMapper();
    objectMapper.writeValue(destPath, configuration);
  }

}
