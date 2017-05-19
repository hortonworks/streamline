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
package com.hortonworks.streamline.common;

/**
 * The components supported by default in Streamline
 */
public class ComponentTypes {

    public static final String KAFKA = "KAFKA";
    public static final String KINESIS = "KINESIS";
    public static final String EVENTHUB = "EVENTHUB";
    public static final String NORMALIZATION = "NORMALIZATION";
    public static final String SPLIT = "SPLIT";
    public static final String JOIN = "JOIN";
    public static final String STAGE = "STAGE";
    public static final String RULE = "RULE";
    public static final String PROJECTION = "PROJECTION";
    public static final String BRANCH = "BRANCH";
    public static final String WINDOW = "WINDOW";
    public static final String CUSTOM = "CUSTOM";
    public static final String OPENTSDB = "OPENTSDB";
    public static final String HBASE = "HBASE";
    public static final String HDFS = "HDFS";
    public static final String HIVE = "HIVE";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String PMML = "PMML";
    public static final String MULTILANG = "MULTILANG";
}
