package org.apache.streamline.storage.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class StorageProviderConfigurationReader {
  public static final String JDBC_STORAGE_MANAGER_CLASS = "org.apache.registries.storage.impl.jdbc.JdbcStorageManager";
  public static final String STORAGE_PROVIDER_CONFIGURATION = "storageProviderConfiguration";
  public static final String PROPERTIES = "properties";
  public static final String PROVIDER_CLASS = "providerClass";
  public static final String DB_TYPE = "db.type";
  public static final String DB_PROPERTIES = "db.properties";
  public static final String DATA_SOURCE_CLASS_NAME = "dataSourceClassName";
  public static final String DATA_SOURCE_URL = "dataSource.url";
  public static final String JDBC_URL = "jdbcUrl";
  public static final String JDBC_DRIVER_CLASS = "jdbcDriverClass";
  public static final String PHOENIX = "phoenix";
  public static final String MYSQL = "mysql";

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("Usage: StorageProviderConfigurationReader [config file path]");
      System.exit(1);
    }

    if (args.length >= 1) {
      String confFile = args[0];

      ObjectMapper objectMapper = new YAMLMapper();
      Map<String, Object> confMap = objectMapper.readValue(new File(confFile), Map.class);

      Map<String, Object> storageConf = (Map<String, Object>) confMap.get(
          STORAGE_PROVIDER_CONFIGURATION);
      if (storageConf == null) {
        System.err.println("No storageProviderConfiguration in config file.");
        System.exit(1);
      }

      String providerClass = (String) storageConf.get(PROVIDER_CLASS);
      if (!providerClass.equals(JDBC_STORAGE_MANAGER_CLASS)) {
        System.err.println("Not supported provider class.");
        System.exit(1);
      }

      Map<String, Object> properties = (Map<String, Object>) storageConf.get(PROPERTIES);
      if (properties == null) {
        System.err.println("No properties presented to storageProviderConfiguration.");
        System.exit(1);
      }

      String dbType = (String) properties.get(DB_TYPE);
      if (dbType == null) {
        System.err.println("No db.type presented to properties.");
        System.exit(1);
      }

      switch (dbType) {
      case PHOENIX:
        handlePhoenixProperties((Map<String, Object>) properties.get(DB_PROPERTIES));
        break;

      case MYSQL:
        handleMySQLProperties((Map<String, Object>) properties.get(DB_PROPERTIES));
        break;

      default:
        System.err.println("Not supported DB type: " + dbType);
        System.exit(1);
      }

      System.exit(0);
    }
  }

  /**
   * storageProviderConfiguration:
   *   providerClass: "org.apache.registries.storage.impl.jdbc.JdbcStorageManager"
   *   properties:
   *     db.type: "mysql"
   *     queryTimeoutInSecs: 30
   *     db.properties:
   *       dataSourceClassName: "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
   *       dataSource.url: "jdbc:mysql://localhost/test"
   */
  private static void handleMySQLProperties(Map<String, Object> dbProperties) {
    String jdbcDriverClass = (String) dbProperties.get(DATA_SOURCE_CLASS_NAME);
    String jdbcUrl = (String) dbProperties.get(DATA_SOURCE_URL);

    System.out.println(String.format("%s\t%s\t%s", MYSQL, jdbcDriverClass, jdbcUrl));
  }

  /**
   * storageProviderConfiguration:
   *   providerClass: "org.apache.registries.storage.impl.jdbc.JdbcStorageManager"
   *   properties:
   *     db.type: "phoenix"
   *     queryTimeoutInSecs: 30
   *     db.properties:
   *       jdbcDriverClass: "org.apache.phoenix.jdbc.PhoenixDriver"
   *       jdbcUrl: "jdbc:phoenix:localhost:2181"
   */
  private static void handlePhoenixProperties(Map<String, Object> dbProperties) {
    String jdbcDriverClass = (String) dbProperties.get(JDBC_DRIVER_CLASS);
    String jdbcUrl = (String) dbProperties.get(JDBC_URL);

    System.out.println(String.format("%s\t%s\t%s", PHOENIX, jdbcDriverClass, jdbcUrl));
  }
}
