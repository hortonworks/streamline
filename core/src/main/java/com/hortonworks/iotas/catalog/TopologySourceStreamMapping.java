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

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

public class TopologySourceStreamMapping extends AbstractStorable {
    public static final String NAMESPACE = "topology_source_stream_mapping";
    public static final String FIELD_SOURCE_ID = "sourceId";
    public static final String FIELD_STREAM_ID = "streamId";

    private Long sourceId;
    private Long streamId;


    // for jackson
    public TopologySourceStreamMapping() {

    }

    public TopologySourceStreamMapping(Long sourceId, Long streamId) {
        this.sourceId = sourceId;
        this.streamId = streamId;
    }

    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(Schema.Field.of(FIELD_SOURCE_ID, Schema.Type.LONG), this.sourceId);
        fieldToObjectMap.put(new Schema.Field(FIELD_STREAM_ID, Schema.Type.LONG), this.streamId);
        return new PrimaryKey(fieldToObjectMap);
    }


    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologySourceStreamMapping that = (TopologySourceStreamMapping) o;

        if (sourceId != null ? !sourceId.equals(that.sourceId) : that.sourceId != null) return false;
        return streamId != null ? streamId.equals(that.streamId) : that.streamId == null;

    }

    @Override
    public int hashCode() {
        int result = sourceId != null ? sourceId.hashCode() : 0;
        result = 31 * result + (streamId != null ? streamId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TopologySourceStreamMapping{" +
                "sourceId=" + sourceId +
                ", streamId=" + streamId +
                '}';
    }
}
