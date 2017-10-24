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
package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.registries.storage.annotation.StorableEntity;
import com.hortonworks.registries.storage.PrimaryKey;
import com.hortonworks.registries.storage.catalog.AbstractStorable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A storable object mainly to store any information needed by UI in a persistent fashion
 */
@StorableEntity
public class TopologyEditorMetadata extends AbstractStorable {
    public static final String NAME_SPACE = "topology_editor_metadata";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String VERSION_ID = "versionId";
    public static final String DATA = "data";
    public static final String TIMESTAMP = "timestamp";

    private Long topologyId;
    private Long versionId;
    private String data;
    private Long timestamp;

    public TopologyEditorMetadata() {
    }

    public TopologyEditorMetadata(TopologyEditorMetadata other) {
        if (other != null) {
            setTopologyId(other.getTopologyId());
            setVersionId(other.getVersionId());
            setData(other.getData());
            setTimestamp(other.getTimestamp());
        }
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public String getData() {
        return data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    @Override
    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(TOPOLOGY_ID, Schema.Type.LONG), this.topologyId);
        fieldToObjectMap.put(new Schema.Field(VERSION_ID, Schema.Type.LONG), this.versionId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @Override
    public String toString() {
        return "TopologyEditorMetadata{" +
                "topologyId=" + topologyId +
                ", versionId=" + versionId +
                ", data='" + data + '\'' +
                ", timestamp=" + timestamp +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyEditorMetadata metadata = (TopologyEditorMetadata) o;

        if (topologyId != null ? !topologyId.equals(metadata.topologyId) : metadata.topologyId != null) return false;
        return versionId != null ? versionId.equals(metadata.versionId) : metadata.versionId == null;

    }

    @Override
    public int hashCode() {
        int result = topologyId != null ? topologyId.hashCode() : 0;
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        return result;
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }


    /**
     * positions of components in UI
     */
    public static class TopologyComponentUICordinates {
        private Double x;
        private Double y;
        private Long id;

        @JsonCreator
        public TopologyComponentUICordinates() {

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

    public static class GraphTransform {
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

    public static class TopologyUIData {
        private List<TopologyComponentUICordinates> sources;
        private List<TopologyComponentUICordinates> sinks;
        private List<TopologyComponentUICordinates> processors;
        private GraphTransform graphTransforms;
        private List<Object> customNames;

        @JsonCreator
        public TopologyUIData() {
        }

        public List<TopologyComponentUICordinates> getSources() {
            return sources;
        }

        public void setSources(List<TopologyComponentUICordinates> sources) {
            this.sources = sources;
        }

        public List<TopologyComponentUICordinates> getSinks() {
            return sinks;
        }

        public void setSinks(List<TopologyComponentUICordinates> sinks) {
            this.sinks = sinks;
        }

        public List<TopologyComponentUICordinates> getProcessors() {
            return processors;
        }

        public void setProcessors(List<TopologyComponentUICordinates> processors) {
            this.processors = processors;
        }

        public GraphTransform getGraphTransforms() {
            return graphTransforms;
        }

        public void setGraphTransforms(GraphTransform graphTranforms) {
            this.graphTransforms = graphTranforms;
        }

        public List<Object> getCustomNames() {
            return customNames;
        }

        public void setCustomNames(List<Object> customNames) {
            this.customNames = customNames;
        }
    }
}
