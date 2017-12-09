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
package com.hortonworks.streamline.streams.cluster.discovery.ambari;

import java.util.Arrays;

/**
 * Defines mapping between Service and its configuration types. DO NOT Change the order of
 * these configuration mappings. If you need to add a new mapping, add it to the end.
 */
public enum ServiceConfigurations {
  ZOOKEEPER("zoo.cfg", "zookeeper-env"),
  // storm -> storm.yaml
  STORM("storm", "storm-env"),
  // server -> server.properties
  KAFKA("server", "kafka-env"),
  // excluded ssl configurations for security reason
  HDFS("core-site", "hadoop-env", "hadoop-policy", "hdfs-site"),
  HBASE("hbase-env", "hbase-policy", "hbase-site"),
  HIVE("hive-env", "hive-interactive-env", "hive-interactive-site",
      "hivemetastore-site", "hiveserver2-interactive-site",
      "hiveserver2-site","hive-site"),
  AMBARI_METRICS("ams-env", "ams-site"),
  DRUID("druid-common", "druid-overlord"),
  AMBARI_INFRA("infra-solr-env");

  private final String[] confNames;

  ServiceConfigurations(String... confNames) {
    this.confNames = confNames;
  }

  public String[] getConfNames() {
    return Arrays.copyOf(confNames, confNames.length);
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