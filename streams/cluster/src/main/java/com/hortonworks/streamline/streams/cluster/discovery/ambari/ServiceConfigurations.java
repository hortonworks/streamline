package com.hortonworks.streamline.streams.cluster.discovery.ambari;

/**
 * Defines mapping between Service and its configuration types. DO NOT Change the order of
 * these configuration mappings. If you need to add a new mapping, add it to the end.
 */
public enum ServiceConfigurations {
  ZOOKEEPER("zoo.cfg", "zookeeper-env"),
  STORM("storm-site", "storm-env"),
  KAFKA("kafka-broker", "kafka-env"),
  // excluded ssl configurations for security reason
  HDFS("core-site", "hadoop-env", "hadoop-policy", "hdfs-site"),
  HBASE("hbase-env", "hbase-policy", "hbase-site"),
  HIVE("hive-env", "hive-interactive-env", "hive-interactive-site",
      "hivemetastore-site", "hiveserver2-interactive-site",
      "hiveserver2-site","hive-site"),
  AMBARI_METRICS("ams-env", "ams-site");

  private final String[] confNames;

  ServiceConfigurations(String... confNames) {
    this.confNames = confNames;
  }

  public String[] getConfNames() {
    return confNames;
  }

  public static boolean contains(String serviceName) {
    try {
      ServiceConfigurations.valueOf(serviceName);
    } catch (IllegalArgumentException e) {
      return false;
    }

    return true;
  }
}