package org.apache.streamline.streams.catalog.service;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Created by schendamaraikannan on 11/28/16.
 */
public class TopologyComponentGraphData {
    private Double x;
    private Double y;
    private Long id;

    @JsonCreator
    public TopologyComponentGraphData() {

    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Long getId() {
        return id;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
