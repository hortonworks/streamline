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
