package com.hortonworks.streamline.streams.cluster.discovery.ambari;

/**
 * Defines mapping between config type and actual file name.
 * If Services are imported from import via Ambari API, this pattern applies to ServiceConfiguration.
 */
public enum ConfigFilePattern {
  CORE_SITE("core-site", "core-site.xml"),
  HDFS_SITE("hdfs-site", "hdfs-site.xml"),
  HIVE_SITE("hive-site", "hive-site.xml"),
  HBASE_SITE("hbase-site", "hbase-site.xml");

  private final String confType;
  private final String actualFileName;

  ConfigFilePattern(String confType, String actualFileName) {
    this.confType = confType;
    this.actualFileName = actualFileName;
  }

  public static String getActualFileName(String confType) {
    ConfigFilePattern pattern = lookup(confType);
    if (pattern != null) {
      return pattern.actualFileName;
    }

    return null;
  }

  public static ConfigFilePattern lookup(String confType) {
    for (ConfigFilePattern value : values()) {
      if (value.confType.equals(confType)) {
        return value;
      }
    }

    return null;
  }

}
