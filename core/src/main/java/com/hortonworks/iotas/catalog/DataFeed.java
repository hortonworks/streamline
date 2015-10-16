package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class DataFeed extends AbstractStorable {
    public static final String NAME_SPACE = "datafeeds";
    public static final String DATAFEED_ID = "dataFeedId";
    public static final String DATASOURCE_ID = "dataSourceId";
    public static final String DATAFEED_NAME = "dataFeedName";
    public static final String PARSER_ID = "parserId";
    public static final String TYPE = "type";

    /**
     * Unique Id, this is the primary key.
     */
    private Long dataFeedId;

    /**
     * The foreign key reference to data source.
     */
    private Long dataSourceId;

    /**
     * Human readable name.
     */
    private String dataFeedName;

    /**
     * Foreign key reference to a parser info that defines which parser implementation can be used to parse this feed.
     */
    private Long parserId;

    /**
     * Where is the actual data for this feed being pushed. e.g. KAFKA, etc.
     * this should correspond to the subType field of the TopologyComponent
     * object
     */
    private String type;

    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(DATAFEED_ID, Schema.Type.LONG), this.dataFeedId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataFeed dataFeed = (DataFeed) o;

        if (dataFeedId != null ? !dataFeedId.equals(dataFeed.dataFeedId) : dataFeed.dataFeedId != null) return false;
        if (dataSourceId != null ? !dataSourceId.equals(dataFeed.dataSourceId) : dataFeed.dataSourceId != null)
            return false;
        if (dataFeedName != null ? !dataFeedName.equals(dataFeed.dataFeedName) : dataFeed.dataFeedName != null)
            return false;
        if (parserId != null ? !parserId.equals(dataFeed.parserId) : dataFeed.parserId != null) return false;
        return !(type != null ? !type.equals(dataFeed.type) : dataFeed.type != null);

    }

    @Override
    public int hashCode() {
        int result = dataFeedId != null ? dataFeedId.hashCode() : 0;
        result = 31 * result + (dataSourceId != null ? dataSourceId.hashCode() : 0);
        result = 31 * result + (dataFeedName != null ? dataFeedName.hashCode() : 0);
        result = 31 * result + (parserId != null ? parserId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataFeed{" +
                "dataFeedId=" + dataFeedId +
                ", dataSourceId=" + dataSourceId +
                ", dataFeedName='" + dataFeedName + '\'' +
                ", parserId=" + parserId +
                ", type='" + type + '\'' +
                '}';
    }

    public Long getDataFeedId() {
        return dataFeedId;
    }

    public void setDataFeedId(Long dataFeedId) {
        this.dataFeedId = dataFeedId;
    }

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
