package org.apache.streamline.registries.dashboard.entites;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

public class Dashboard extends AbstractStorable {
    public static final String NAMESPACE = "dashboard";
    public static final String ID = "id";
    public static final String DASHBOARD_ID = "dashboardId";

    private Long id;
    private String name;
    private String description;
    private String data;
    private Long timestamp;

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Dashboard{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", data='" + data + '\'' +
                ", timestamp=" + timestamp +
                "} " + super.toString();
    }
}
