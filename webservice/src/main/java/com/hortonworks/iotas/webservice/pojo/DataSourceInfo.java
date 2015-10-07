package com.hortonworks.iotas.webservice.pojo;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;

/**
 *
 */
public class DataSourceInfo {

    /**
     * Unique identifier. This is the primary key.
     */
    private Long dataSourceId;

    /**
     * Human readable name.
     */
    private String dataSourceName;

    /**
     * Human readable description.
     */
    private String description;

    /**
     * Free form tags.
     */
    private String tags;

    /**
     * Time when this entry was created/updated.
     */
    private Long timestamp;

    /**
     * The type of the datasource e.g. 'DEVICE'
     */
    private DataSource.Type type;

    /**
     * The config string specific to the type. e.g. Device Config.
     */
    private String typeConfig;

    /**
     * Human readable name.
     */
    private String dataFeedName;

    /**
     * Foreign key reference to a parser info that defines which parser implementation can be used to parse this feed.
     */
    private Long parserId;

    /**
     * Where is the actual data for this feed being pushed. i.e "kafka:\\host1:port\nest-device-data-topic", "twitter:\\twitter-api.host:port\feedname"
     */
    private String endpoint;


    public DataSourceInfo() {
    }

    public DataSourceInfo(DataSource dataSource, DataFeed dataFeed) {
        dataSourceId = dataSource.getDataSourceId();
        dataSourceName = dataSource.getDataSourceName();
        description = dataSource.getDescription();
        tags = dataSource.getTags();
        timestamp = dataSource.getTimestamp();
        type = dataSource.getType();
        typeConfig = dataSource.getTypeConfig();

        dataFeedName = dataFeed.getDataFeedName();
        parserId = dataFeed.getParserId();
        endpoint = dataFeed.getEndpoint();

    }

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public DataSource.Type getType() {
        return type;
    }

    public void setType(DataSource.Type type) {
        this.type = type;
    }

    public String getTypeConfig() {
        return typeConfig;
    }

    public void setTypeConfig(String typeConfig) {
        this.typeConfig = typeConfig;
    }

    public String getDataFeedName() {
        return dataFeedName;
    }

    public void setDataFeedName(String dataFeedName) {
        this.dataFeedName = dataFeedName;
    }

    public Long getParserId() {
        return parserId;
    }

    public void setParserId(Long parserId) {
        this.parserId = parserId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
