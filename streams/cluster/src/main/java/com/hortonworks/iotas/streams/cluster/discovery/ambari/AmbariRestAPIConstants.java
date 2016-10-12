package com.hortonworks.iotas.streams.cluster.discovery.ambari;

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


  public static final String AMBARI_JSON_SCHEMA_SERVICE_INFO = "ServiceInfo";
  public static final String AMBARI_JSON_SCHEMA_SERVICE_NAME = "service_name";
  public static final String ABARI_JSON_SCHEMA_SERVICE_COMPONENT_INFO = "ServiceComponentInfo";
  public static final String AMBARI_JSON_SCHEMA_COMPONENTS = "components";
  public static final String AMBARI_JSON_SCHEMA_COMPONENT_NAME = "component_name";
  public static final String AMBARI_JSON_SCHEMA_HOST_COMPONENTS = "host_components";
  public static final String AMBARI_JSON_SCHEMA_HOST_ROLES = "HostRoles";
  public static final String AMBARI_JSON_SCHEMA_HOST_NAME = "host_name";

}
