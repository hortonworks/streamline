package com.hortonworks.iotas.catalog;


import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;

import java.util.HashMap;
import java.util.Map;

public class DataSource implements Storable {
    public static final String DATA_SOURCE_ID = "dataSourceId";
    public static final String DATA_SOURCE_NAME = "dataSourceName";
    public static final String DESCRIPTION = "description";
    public static final String DATAFEED_ID = "datafeedId";
    public static final String TAGS = "tags";
    public static final String TIMESTAMP = "timestamp";

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
     * Reference to the datafeed that this data source is going to generate.
     */
    private Long datafeedId;

    /**
     * Free form tags.
     */
    private String tags;

    /**
     * Time when this entry was created/updated.
     */
    private Long timestamp;

    public String getNameSpace() {
        return "datasources";
    }

    public Schema getSchema() {
         return new Schema(new Schema.Field(DATA_SOURCE_ID, Schema.Type.LONG),
                 new Schema.Field(DATA_SOURCE_NAME, Schema.Type.STRING),
                 new Schema.Field(DESCRIPTION, Schema.Type.STRING),
                 new Schema.Field(DATAFEED_ID, Schema.Type.LONG),
                 new Schema.Field(TAGS, Schema.Type.STRING),
                 new Schema.Field(TIMESTAMP, Schema.Type.LONG));
    }

    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(DATA_SOURCE_ID, Schema.Type.LONG), this.DATA_SOURCE_ID);
        return new PrimaryKey(fieldToObjectMap);
    }

    public Map toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DATA_SOURCE_ID, this.datafeedId);
        map.put(DATA_SOURCE_NAME, this.dataSourceName);
        map.put(DESCRIPTION, this.description);
        map.put(TAGS, this.tags);
        map.put(DATAFEED_ID, this.datafeedId);
        map.put(TIMESTAMP, this.timestamp);
        return map;
    }

    public DataSource fromMap(Map<String, Object> map) {
        this.dataSourceId = (Long) map.get(DATA_SOURCE_ID);
        this.dataSourceName = (String)  map.get(DATA_SOURCE_NAME);
        this.description = (String)  map.get(DESCRIPTION);
        this.tags = (String)  map.get(TAGS);
        this.datafeedId = (Long) map.get(DATAFEED_ID);
        this.timestamp = (Long) map.get(TIMESTAMP);
        return this;
    }

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getDescription() {
        return description;
    }

    public Long getDatafeedId() {
        return datafeedId;
    }

    public String getTags() {
        return tags;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public static class DataSourceBuilder {
        private Long dataSourceId;
        private String dataSourceName;
        private String description;
        private Long datafeedId;
        private String tags;
        private Long timestamp;

        public DataSourceBuilder setDataSourceId(Long dataSourceId) {
            this.dataSourceId = dataSourceId;
            return this;
        }

        public DataSourceBuilder setDataSourceName(String dataSourceName) {
            this.dataSourceName = dataSourceName;
            return this;
        }

        public DataSourceBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public DataSourceBuilder setDatafeedId(Long datafeedId) {
            this.datafeedId = datafeedId;
            return this;
        }

        public DataSourceBuilder setTags(String tags) {
            this.tags = tags;
            return this;
        }

        public DataSourceBuilder setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public DataSource createDataSource() {
            DataSource dataSource = new DataSource();
            dataSource.dataSourceId = this.dataSourceId;
            dataSource.dataSourceName = this.dataSourceName;
            dataSource.description = this.description;
            dataSource.tags = this.tags;
            dataSource.datafeedId = this.datafeedId;
            dataSource.timestamp = this.timestamp;
            return dataSource;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataSource)) return false;

        DataSource that = (DataSource) o;

        if (!dataSourceId.equals(that.dataSourceId)) return false;
        if (!dataSourceName.equals(that.dataSourceName)) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (!datafeedId.equals(that.datafeedId)) return false;
        if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;
        return timestamp.equals(that.timestamp);

    }

    @Override
    public int hashCode() {
        int result = dataSourceId.hashCode();
        result = 31 * result + dataSourceName.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + datafeedId.hashCode();
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + timestamp.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "dataSourceId=" + dataSourceId +
                ", dataSourceName='" + dataSourceName + '\'' +
                ", description='" + description + '\'' +
                ", datafeedId=" + datafeedId +
                ", tags='" + tags + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
