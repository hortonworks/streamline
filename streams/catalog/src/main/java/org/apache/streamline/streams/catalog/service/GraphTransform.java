package org.apache.streamline.streams.catalog.service;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by schendamaraikannan on 11/28/16.
 */
public class GraphTransform {
    private List<Double> dragCoords;
    private Double zoomScale;

    @JsonCreator
    public GraphTransform() {
    }

    public List<Double> getDragCoords() {
        return dragCoords;
    }

    public void setDragCoords(List<Double> dragCoords) {
        if (dragCoords != null) {
            this.dragCoords = new ArrayList<>(dragCoords);
        }
    }

    public Double getZoomScale() {
        return zoomScale;
    }

    public void setZoomScale(Double zoomScale) {
        this.zoomScale = zoomScale;
    }
}
