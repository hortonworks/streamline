package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class DataFeed extends AbstractStorable {
    public static final String NAME_SPACE = "datafeeds";
    public static final String DATAFEED_ID = "id";
    public static final String DATASOURCE_ID = "dataSourceId";
    public static final String DATAFEED_NAME = "name";
    public static final String PARSER_ID = "parserId";
    public static final String ENDPOINT = "endpoint";

    /**
     * Unique Id, this is the primary key.
     */
    private Long id;

    /**
     * The foreign key reference to data source.
     */
    private Long dataSourceId;

    /**
     * Human readable name.
     */
    private String name;

    /**
     * Foreign key reference to a parser info that defines which parser implementation can be used to parse this feed.
     */
    private Long parserId;

    /**
     * Where is the actual data for this feed being pushed. i.e "kafka:\\host1:port\nest-device-data-topic", "twitter:\\twitter-api.host:port\feedname"
     */
    private String endpoint;

    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(DATAFEED_ID, Schema.Type.LONG), this.id);
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

        if (id != null ? !id.equals(dataFeed.id) : dataFeed.id != null) return false;
        if (dataSourceId != null ? !dataSourceId.equals(dataFeed.dataSourceId) : dataFeed.dataSourceId != null)
            return false;
        if (name != null ? !name.equals(dataFeed.name) : dataFeed.name != null)
            return false;
        if (parserId != null ? !parserId.equals(dataFeed.parserId) : dataFeed.parserId != null) return false;
        return !(endpoint != null ? !endpoint.equals(dataFeed.endpoint) : dataFeed.endpoint != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (dataSourceId != null ? dataSourceId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (parserId != null ? parserId.hashCode() : 0);
        result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataFeed{" +
                "id=" + id +
                ", dataSourceId=" + dataSourceId +
                ", name='" + name + '\'' +
                ", parserId=" + parserId +
                ", endpoint='" + endpoint + '\'' +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
