package com.hortonworks.iotas.streams.catalog.configuration;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigFileTypeTest {
  @Test
  public void getFileTypeFromFileName() throws Exception {
    assertEquals(ConfigFileType.HADOOP_XML, ConfigFileType.getFileTypeFromFileName("core-site.xml"));
    assertEquals(ConfigFileType.YAML, ConfigFileType.getFileTypeFromFileName("storm.yaml"));
    assertEquals(ConfigFileType.PROPERTIES, ConfigFileType.getFileTypeFromFileName("config.properties"));
    assertNull(ConfigFileType.getFileTypeFromFileName("config"));
    assertNull(ConfigFileType.getFileTypeFromFileName("config.hadoop"));
  }

}