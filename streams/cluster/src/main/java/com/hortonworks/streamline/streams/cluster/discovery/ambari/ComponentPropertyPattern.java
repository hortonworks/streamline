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

import java.util.regex.Pattern;

/**
 * Defines the pattern to extract Component's properties from configuration.
 * Currently it only extracts protocol and port from one configuration key.
 */
public enum ComponentPropertyPattern {
  // HDFS
  // There're multiple ports assigned for each components in HDFS and HBASE
  // So what port to pick is subject to change for the future use cases
  NAMENODE("dfs.http.address"),
  SECONDARY_NAMENODE("dfs.secondary.http.address"),
  DATANODE("dfs.datanode.http.address", Pattern.compile("()[a-zA-Z0-9_\\-\\\\.]+:([0-9]+)")),

  // HBASE
  HBASE_MASTER("hbase.master.port"),
  HBASE_REGIONSERVER("hbase.regionserver.port"),

  // STORM
  NIMBUS("nimbus.thrift.port"),
  STORM_UI_SERVER("ui.port"),
  DRPC_SERVER("drpc.port"),

  // ZOOKEEPER
  ZOOKEEPER_SERVER("clientPort"),

  // HIVE
  HIVE_SERVER("hive.server2.thrift.port"),
  HIVE_METASTORE("hive.metastore.uris", Pattern.compile("([a-zA-Z]+)://[a-zA-Z0-9_\\-\\\\.]*:([0-9]+)")),

  // KAFKA
  // protocol (plaintext, ssl, kerberos, etc?), host (can be empty), port
  // https://cwiki.apache.org/confluence/display/KAFKA/Multiple+Listeners+for+Kafka+Brokers
  KAFKA_BROKER("listeners", Pattern.compile("([a-zA-Z]+)://[a-zA-Z0-9_\\-\\\\.]*:([0-9]+)")),

  // AMBARI_METRICS
  METRICS_COLLECTOR("timeline.metrics.service.webapp.address", Pattern.compile("()[a-zA-Z0-9_\\-\\\\.]*:([0-9]+)")),

  // AMBARI_INFRA
  INFRA_SOLR("infra_solr_port");

  private final String connectionConfName;
  private final Pattern parsePattern;

  ComponentPropertyPattern(String connectionConfName) {
    this(connectionConfName, Pattern.compile("()(.+)"));
  }

  ComponentPropertyPattern(String connectionConfName, Pattern parsePattern) {
    this.connectionConfName = connectionConfName;
    this.parsePattern = parsePattern;
  }

  public String getConnectionConfName() {
    return connectionConfName;
  }

  public Pattern getParsePattern() {
    return parsePattern;
  }
}
