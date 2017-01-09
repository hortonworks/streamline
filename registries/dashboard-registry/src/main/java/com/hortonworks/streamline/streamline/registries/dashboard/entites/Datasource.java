package com.hortonworks.streamline.registries.dashboard.entites;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

public class Datasource extends AbstractStorable {
    public static final String NAMESPACE = "datasource";
    public static final String ID = "id";

    private Long id;
    private String name;
    private String description;
    private String type;
    private String url;
    private String data;
    private Long timestamp;
    private Long dashboardId;

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
        fieldToObjectMap.put(new Schema.Field(Dashboard.DASHBOARD_ID, Schema.Type.LONG), this.dashboardId);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public Long getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(Long dashboardId) {
        this.dashboardId = dashboardId;
    }

    @Override
    public String toString() {
        return "Datasource{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", data='" + data + '\'' +
                ", timestamp=" + timestamp +
                ", dashboardId=" + dashboardId +
                "} " + super.toString();
    }
}
