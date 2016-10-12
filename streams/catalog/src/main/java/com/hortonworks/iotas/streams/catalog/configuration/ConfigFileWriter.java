package com.hortonworks.iotas.streams.catalog.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writer that write configuration to file based on the file type.
 */
public class ConfigFileWriter {
  public void writeConfigToFile(ConfigFileType confFileType, Map<String, Object> configuration,
      File destPath) throws IOException {
    if (confFileType == null) {
      throw new IllegalArgumentException("You should provide valid config file type to write configuration to file. file type: " + confFileType);
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
      throw new IllegalArgumentException("Writer doesn't know how to write such kind of file type. file type: " + confFileType);
    }
  }

  private void writeConfigToHadoopXmlTypeFile(Map<String, Object> configuration, File destPath)
      throws IOException {
    ObjectMapper objectMapper = new XmlMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

    HadoopXml hadoopXml = new HadoopXml();
    for (Map.Entry<String, Object> property : configuration.entrySet()) {
      hadoopXml.addProperty(new HadoopXmlProperty(property.getKey(), property.getValue()));
    }

    objectMapper.writeValue(destPath, hadoopXml);
  }

  private void writeConfigToPropertiesTypeFile(Map<String, Object> configuration, File destPath)
      throws IOException {
    ObjectMapper objectMapper = new JavaPropsMapper();
    objectMapper.writeValue(destPath, configuration);
  }

  private void writeConfigToYamlTypeFile(Map<String, Object> configuration, File destPath)
      throws IOException {
    ObjectMapper objectMapper = new YAMLMapper();
    objectMapper.writeValue(destPath, configuration);
  }

  @JacksonXmlRootElement(localName = "configuration")
  static class HadoopXml {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "property")
    private List<HadoopXmlProperty> properties = new ArrayList<>();

    public List<HadoopXmlProperty> getProperties() {
      return properties;
    }

    public void setProperties(List<HadoopXmlProperty> properties) {
      this.properties = properties;
    }

    public void addProperty(HadoopXmlProperty property) {
      properties.add(property);
    }
  }

  static class HadoopXmlProperty {
    private String name;
    private Object value;

    public HadoopXmlProperty(String name, Object value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(Object value) {
      this.value = value;
    }
  }

}
