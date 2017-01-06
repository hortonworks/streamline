package com.hortonworks.streamline.registries.dashboard.dto;

import com.hortonworks.streamline.registries.dashboard.entites.Widget;

import java.util.Set;

public class WidgetDto {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String data;
    private Long timestamp;
    private Long dashboardId;
    private Set<Long> datasourceIds;

    public Long getId() {
        return id;
    }

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

    public Set<Long> getDatasourceIds() {
        return datasourceIds;
    }

    public void setDatasourceIds(Set<Long> datasourceIds) {
        this.datasourceIds = datasourceIds;
    }

    public static WidgetDto fromWidget(Widget widget) {
        WidgetDto dto = new WidgetDto();
        dto.setId(widget.getId());
        dto.setName(widget.getName());
        dto.setDescription(widget.getDescription());
        dto.setType(widget.getType());
        dto.setData(widget.getData());
        dto.setTimestamp(widget.getTimestamp());
        dto.setDashboardId(widget.getDashboardId());
        return dto;
    }
}
