package com.hortonworks.iotas.streams.cluster.discovery.ambari;

/**
 * Data structure of "Service Configuration" in Ambari REST API response.
 */
public class ServiceConfigurationItem {
    private String type;
    private Integer version;
    private String tag;
    private String href;

    public ServiceConfigurationItem(String type, Integer version, String tag, String href) {
      this.type = type;
      this.version = version;
      this.tag = tag;
      this.href = href;
    }

    public String getType() {
      return type;
    }

    public Integer getVersion() {
      return version;
    }

    public String getTag() {
      return tag;
    }

    public String getHref() {
      return href;
    }
  }