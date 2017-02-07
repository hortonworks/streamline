/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/
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
