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
 * Constants which is used for parsing Ambari REST API response.
 */
public final class AmbariRestAPIConstants {
  private AmbariRestAPIConstants() {}

  public static final String AMBARI_JSON_SCHEMA_COMMON_ITEMS = "items";
  public static final String AMBARI_JSON_SCHEMA_COMMON_TYPE = "type";
  public static final String AMBARI_JSON_SCHEMA_COMMON_VERSION = "version";
  public static final String AMBARI_JSON_SCHEMA_COMMON_TAG = "tag";
  public static final String AMBARI_JSON_SCHEMA_COMMON_HREF = "href";
  public static final String AMBARI_JSON_SCHEMA_COMMON_PROPERTIES = "properties";
  public static final String AMBARI_JSON_SCHEMA_COMMON_VERSIONS = "versions";
  public static final String AMBARI_JSON_SCHEMA_COMMON_INSTANCES = "instances";
  public static final String AMBARI_JSON_SCHEMA_COMMON_VIEW_INSTANCE_INFO = "ViewInstanceInfo";


  public static final String AMBARI_JSON_SCHEMA_SERVICE_INFO = "ServiceInfo";
  public static final String AMBARI_JSON_SCHEMA_SERVICE_NAME = "service_name";
  public static final String ABARI_JSON_SCHEMA_SERVICE_COMPONENT_INFO = "ServiceComponentInfo";
  public static final String AMBARI_JSON_SCHEMA_COMPONENTS = "components";
  public static final String AMBARI_JSON_SCHEMA_COMPONENT_NAME = "component_name";
  public static final String AMBARI_JSON_SCHEMA_HOST_COMPONENTS = "host_components";
  public static final String AMBARI_JSON_SCHEMA_HOST_ROLES = "HostRoles";
  public static final String AMBARI_JSON_SCHEMA_HOST_NAME = "host_name";
  public static final String AMBARI_JSON_SCHEMA_CONTEXT_PATH = "context_path";
}
