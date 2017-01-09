package com.hortonworks.streamline.registries.dashboard.entites;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

public class WidgetDatasourceMapping extends AbstractStorable {
    public static final String NAMESPACE = "widget_datasource_mapping";
    public static final String WIDGET_ID = "widgetId";
    public static final String DATASOURCE_ID = "datasourceId";

    private Long widgetId;
    private Long datasourceId;

    public WidgetDatasourceMapping() {
    }

    public WidgetDatasourceMapping(Long widgetId, Long datasourceId) {
        this.widgetId = widgetId;
        this.datasourceId = datasourceId;
    }

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(WIDGET_ID, Schema.Type.LONG), this.widgetId);
        fieldToObjectMap.put(new Schema.Field(DATASOURCE_ID, Schema.Type.LONG), this.datasourceId);
        return new PrimaryKey(fieldToObjectMap);
    }

    public Long getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(Long widgetId) {
        this.widgetId = widgetId;
    }

    public Long getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(Long datasourceId) {
        this.datasourceId = datasourceId;
    }

    @Override
    public String toString() {
        return "WidgetDatasourceMapping{" +
                "widgetId=" + widgetId +
                ", datasourceId=" + datasourceId +
                "}";
    }
}
