package org.apache.streamline.storage.tool;

public class StorageProviderConfiguration {
    private static final String MYSQL_DELIMITER = ";";
    private static final String PHOENIX_DELIMITER = "\n";
    private static final String POSTGRESQL_DELIMITER = ";";
    private static final String PHOENIX = "phoenix";
    private static final String MYSQL = "mysql";
    private static final String POSTGRESQL = "postgresql";

    private String driverClass;
    private String url;
    private String user = "";
    private String password = "";
    private String dbType;
    private String delimiter;

    private StorageProviderConfiguration(String driverClass, String url, String user, String password, String dbType,
                                         String delimiter) {
        this.driverClass = driverClass;
        this.url = url;
        this.user = user;
        this.password = password;
        this.dbType = dbType;
        this.delimiter = delimiter;
    }


    public static StorageProviderConfiguration mysql(String driverClass, String url) {
        return new StorageProviderConfiguration(driverClass, url, "", "", MYSQL, MYSQL_DELIMITER);
    }

    public static StorageProviderConfiguration mysql(String driverClass, String url, String user, String password) {
        return new StorageProviderConfiguration(driverClass, url, user, password, MYSQL, MYSQL_DELIMITER);
    }

    public static StorageProviderConfiguration phoenix(String driverClass, String url) {
        return new StorageProviderConfiguration(driverClass, url, "", "", PHOENIX, PHOENIX_DELIMITER);
    }

    public static StorageProviderConfiguration postgresql(String driverClass, String url, String user, String password) {
        return new StorageProviderConfiguration(driverClass, url, user, password, POSTGRESQL, POSTGRESQL_DELIMITER);
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDbType() {
        return dbType;
    }

    public String getDelimiter() {
        return delimiter;
    }

}