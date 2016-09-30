package com.hortonworks.iotas.streams.catalog.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigFileWriterTest {

  private ConfigFileWriter writer = new ConfigFileWriter();

  @Test
  public void writeHadoopXmlConfigToFile() throws Exception {
    Map<String, Object> configuration = createDummyConfiguration();

    File destPath = Files.createTempFile("config", "xml").toFile();
    destPath.deleteOnExit();

    writer.writeConfigToFile(ConfigFileType.HADOOP_XML, configuration, destPath);
    assertTrue(destPath.exists());
    try (InputStream is = new FileInputStream(destPath)) {
      String content = IOUtils.toString(is);

      System.out.println("Test print: XML content - " + content);

      assertTrue(content.trim().startsWith("<configuration>"));
      assertTrue(content.trim().endsWith("</configuration>"));
      assertTrue(content.contains("<property>"));
      assertTrue(content.contains("</property>"));

      for (Map.Entry<String, Object> property : configuration.entrySet()) {
        assertTrue(content.contains("<name>" + property.getKey() + "</name>"));
        assertTrue(content.contains("<value>" + property.getValue() + "</value>"));
      }
    }
  }

  @Test
  public void writeJavaPropertiesConfigToFile() throws Exception {
    Map<String, Object> configuration = createDummyConfiguration();

    File destPath = Files.createTempFile("config", "properties").toFile();
    destPath.deleteOnExit();

    writer.writeConfigToFile(ConfigFileType.PROPERTIES, configuration, destPath);
    assertTrue(destPath.exists());
    try (InputStream is = new FileInputStream(destPath)) {
      String content = IOUtils.toString(is);
      System.out.println("Test print: properties content - " + content);
    }

    try (InputStream is = new FileInputStream(destPath)) {
      Properties prop = new Properties();
      prop.load(is);

      for (Map.Entry<String, Object> property : configuration.entrySet()) {
        assertEquals(property.getValue().toString(), prop.getProperty(property.getKey()));
      }
    }
  }

  @Test
  public void writeYamlConfigToFile() throws Exception {
    Map<String, Object> configuration = createDummyConfiguration();

    File destPath = Files.createTempFile("config", "yaml").toFile();
    destPath.deleteOnExit();

    writer.writeConfigToFile(ConfigFileType.YAML, configuration, destPath);
    assertTrue(destPath.exists());
    try (InputStream is = new FileInputStream(destPath)) {
      String content = IOUtils.toString(is);
      System.out.println("Test print: yaml content - " + content);

      ObjectMapper mapper = new YAMLMapper();
      Map<String, Object> readConfig = mapper.readValue(content, Map.class);

      assertEquals(configuration, readConfig);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void writeUnknownTypeConfigToFile() throws Exception {
    Map<String, Object> configuration = createDummyConfiguration();

    File destPath = Files.createTempFile("config", "unknown").toFile();
    destPath.deleteOnExit();

    writer.writeConfigToFile(null, configuration, destPath);
    fail("It shouldn't reach here.");
  }

  private Map<String, Object> createDummyConfiguration() {
    Map<String, Object> dummyConfiguration = new HashMap<>();
    dummyConfiguration.put("javax.jdo.option.ConnectionDriverName", "com.mysql.jdbc.Driver");
    dummyConfiguration.put("ipc.client.connect.max.retries", 50);
    return dummyConfiguration;
  }
}