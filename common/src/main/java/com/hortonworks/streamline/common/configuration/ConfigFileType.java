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

import com.google.common.io.Files;

/**
 * Defines configuration file's type. It is needed for writing configuration map to file.
 * @see ConfigFileWriter
 */
public enum ConfigFileType {
  HADOOP_XML, PROPERTIES, YAML, ZOOKEEPER_CFG;

  public static ConfigFileType getFileTypeFromFileName(String fileName) {
    String fileExt = Files.getFileExtension(fileName);
    if (fileExt.isEmpty()) {
      throw new IllegalArgumentException("Not supported config file extension: " + fileExt);
    }

    switch (fileExt) {
    case "properties":
      return PROPERTIES;
    case "xml":
      // NOTE: If there're other types of xml rather than Hadoop-configuration-like,
      // we shouldn't guess it only based on extension.
      return HADOOP_XML;
    case "yaml":
      return YAML;
    case "cfg":
      // NOTE: If there're other types of cfg rather than Zookeeper-configuration-like,
      // we shouldn't guess it only based on extension.
      return ZOOKEEPER_CFG;
    }

    throw new IllegalArgumentException("Not supported config file extension: " + fileExt);
  }
}
