/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.hortonworks.iotas.topology.component.Stream.Grouping;

public class TopologyEdge extends AbstractStorable {

    public static class StreamGrouping {
        private Long streamId;
        private Grouping grouping;
        private List<String> fields;

        // for jackson
        private StreamGrouping() {
        }

        public Long getStreamId() {
            return streamId;
        }

        public Grouping getGrouping() {
            return grouping;
        }

        public List<String> getFields() {
            return fields;
        }

        @Override
        public String toString() {
            return "StreamGrouping{" +
                    "streamId=" + streamId +
                    ", grouping=" + grouping +
                    ", fields=" + fields +
                    '}';
        }
    }

    public static final String NAMESPACE = "topology_edges";
    public static final String ID = "id";
    public static final String TOPOLOGYID = "topologyId";
    public static final String FROMID = "fromId";
    public static final String TOID = "toId";
    public static final String STREAMGROUPINGS = "streamGroupings";
    public static final String STREAMGROUPINGSDATA = "streamGroupingsData";

    private Long id;
    private Long topologyId;
    private Long fromId;
    private Long toId;
    private List<StreamGrouping> streamGroupings;

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
    }

    public List<StreamGrouping> getStreamGroupings() {
        return streamGroupings;
    }

    public void setStreamGroupings(List<StreamGrouping> streamGroupings) {
        this.streamGroupings = streamGroupings;
    }

    @JsonIgnore
    public String getStreamGroupingsData() throws Exception {
        if (streamGroupings != null) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(streamGroupings);
        }
        return "";
    }

    @JsonIgnore
    public void setStreamGroupingsData(String streamGroupingData) throws Exception {
        if (!StringUtils.isEmpty(streamGroupingData)) {
            ObjectMapper mapper = new ObjectMapper();
            streamGroupings = mapper.readValue(streamGroupingData, new TypeReference<List<StreamGrouping>>() {
            });
        }
    }

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(
                Schema.Field.of(ID, Schema.Type.LONG),
                Schema.Field.of(TOPOLOGYID, Schema.Type.LONG),
                Schema.Field.of(FROMID, Schema.Type.LONG),
                Schema.Field.of(TOID, Schema.Type.LONG),
                Schema.Field.of(STREAMGROUPINGSDATA, Schema.Type.STRING));
    }

    @Override
    public String toString() {
        return "TopologyEdge{" +
                "id=" + id +
                ", topologyId=" + topologyId +
                ", fromId=" + fromId +
                ", toId=" + toId +
                ", streamGroupings=" + streamGroupings +
                '}';
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.remove(STREAMGROUPINGS);
        try {
            map.put(STREAMGROUPINGSDATA, getStreamGroupingsData());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }
}
