package org.apache.streamline.streams.cluster.discovery.ambari;

/**
 * Defines mapping between Service and its configuration types.
 */
public enum ServiceConfigurations {
  ZOOKEEPER("zoo.cfg", "zookeeper-env"),
  STORM("storm-site", "storm-env"),
  KAFKA("kafka-broker", "kafka-env"),
  // excluded ssl configurations for security reason
  HDFS("core-site", "hadoop-env", "hadoop-policy", "hdfs-site"),
  HBASE("hbase-env", "hbase-policy", "hbase-site"),
  HIVE("hive-env", "hive-interactive-env", "hive-interactive-site",
      "hive-metastore-site", "hiveserver2-interactive-site",
      "hiveserver2-site");

  private final String[] confNames;

  ServiceConfigurations(String... confNames) {
    this.confNames = confNames;
  }

  public String[] getConfNames() {
    return confNames;
  }
}