package com.hortonworks.iotas.webservice.catalog.dto;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DataSourceDto {

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
     * Support on the fly tag creation and make it backward compatible
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
     * Parser name of which parser implementation can be used to parse this feed.
     */
    private String parserName;

    /**
     * Where is the actual data for this feed being pushed. e.g. KAFKA, etc.
     * this should correspond to the subType field of the TopologyComponent
     * object
     */
    private String dataFeedType;

    public DataSourceDto() {
    }

    public DataSourceDto(DataSource dataSource, DataFeed dataFeed) {
        dataSourceId = dataSource.getId();
        dataSourceName = dataSource.getName();
        description = dataSource.getDescription();
        tags = getTagNames(dataSource.getTags());
        timestamp = dataSource.getTimestamp();
        type = dataSource.getType();
        typeConfig = dataSource.getTypeConfig();

        if (dataFeed != null) {
            dataFeedName = dataFeed.getName();
            parserId = dataFeed.getParserId();
            dataFeedType = dataFeed.getType();
        }
    }

    public String getTags() { return tags; }

    public void setTags(String tags) { this.tags = tags; }

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

    public String getDataFeedType() {
        return dataFeedType;
    }

    public void setDataFeedType(String dataFeedType) {
        this.dataFeedType = dataFeedType;
    }

    public String getParserName() {
        return parserName;
    }

    public void setParserName(String parserName) {
        this.parserName = parserName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSourceDto that = (DataSourceDto) o;

        if (dataSourceId != null ? !dataSourceId.equals(that.dataSourceId) : that.dataSourceId != null) return false;
        if (dataSourceName != null ? !dataSourceName.equals(that.dataSourceName) : that.dataSourceName != null)
            return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (type != that.type) return false;
        if (typeConfig != null ? !typeConfig.equals(that.typeConfig) : that.typeConfig != null) return false;
        if (dataFeedName != null ? !dataFeedName.equals(that.dataFeedName) : that.dataFeedName != null) return false;
        if (parserId != null ? !parserId.equals(that.parserId) : that.parserId != null) return false;
        return !(dataFeedType != null ? !dataFeedType.equals(that.dataFeedType) : that.dataFeedType != null);

    }

    @Override
    public int hashCode() {
        int result = dataSourceId != null ? dataSourceId.hashCode() : 0;
        result = 31 * result + (dataSourceName != null ? dataSourceName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (typeConfig != null ? typeConfig.hashCode() : 0);
        result = 31 * result + (dataFeedName != null ? dataFeedName.hashCode() : 0);
        result = 31 * result + (parserId != null ? parserId.hashCode() : 0);
        result = 31 * result + (dataFeedType != null ? dataFeedType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataSourceDto{" +
                "dataSourceId=" + dataSourceId +
                ", dataSourceName='" + dataSourceName + '\'' +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", typeConfig='" + typeConfig + '\'' +
                ", dataFeedName='" + dataFeedName + '\'' +
                ", parserId=" + parserId +
                ", dataFeedType='" + dataFeedType + '\'' +
                '}';
    }

    private String getTagNames (List<Tag> tags) {
        StringBuilder tagNames = new StringBuilder();
        if (tags != null && !tags.isEmpty()) {
            for (Tag tag : tags) {
                tagNames.append(tag.getName()).append(",");
            }
            tagNames.deleteCharAt(tagNames.length() - 1);
        }
        return tagNames.toString();
    }
}
