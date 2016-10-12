package com.hortonworks.iotas.streams.catalog.configuration;

import com.google.common.io.Files;

/**
 * Defines configuration file's type. It is needed for writing configuration map to file.
 * @see ConfigFileWriter
 */
public enum ConfigFileType {
  HADOOP_XML, PROPERTIES, YAML;

  public static ConfigFileType getFileTypeFromFileName(String fileName) {
    String fileExt = Files.getFileExtension(fileName);
    if (fileExt.isEmpty()) {
      return null;
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
    }

    return null;
  }
}
