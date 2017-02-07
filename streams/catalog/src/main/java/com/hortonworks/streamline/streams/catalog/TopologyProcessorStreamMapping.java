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
package com.hortonworks.streamline.streams.catalog;

import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

public class TopologyProcessorStreamMapping extends AbstractStorable {
    public static final String NAMESPACE = "topology_processor_stream_mapping";
    public static final String FIELD_PROCESSOR_ID = "processorId";
    public static final String FIELD_VERSION_ID = "versionId";
    public static final String FIELD_STREAM_ID = "streamId";

    private Long processorId;
    private Long versionId;
    private Long streamId;

    // for jackson
    public TopologyProcessorStreamMapping() {

    }

    public TopologyProcessorStreamMapping(Long processorId, Long versionId, Long streamId) {
        this.processorId = processorId;
        this.versionId = versionId;
        this.streamId = streamId;
    }

    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(Schema.Field.of(FIELD_PROCESSOR_ID, Schema.Type.LONG), this.processorId);
        fieldToObjectMap.put(Schema.Field.of(FIELD_VERSION_ID, Schema.Type.LONG), this.versionId);
        fieldToObjectMap.put(new Schema.Field(FIELD_STREAM_ID, Schema.Type.LONG), this.streamId);
        return new PrimaryKey(fieldToObjectMap);
    }


    public Long getProcessorId() {
        return processorId;
    }

    public void setProcessorId(Long processorId) {
        this.processorId = processorId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
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

        TopologyProcessorStreamMapping that = (TopologyProcessorStreamMapping) o;

        if (processorId != null ? !processorId.equals(that.processorId) : that.processorId != null) return false;
        if (versionId != null ? !versionId.equals(that.versionId) : that.versionId != null) return false;
        return streamId != null ? streamId.equals(that.streamId) : that.streamId == null;

    }

    @Override
    public int hashCode() {
        int result = processorId != null ? processorId.hashCode() : 0;
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        result = 31 * result + (streamId != null ? streamId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TopologyProcessorStreamMapping{" +
                "processorId=" + processorId +
                ", versionId=" + versionId +
                ", streamId=" + streamId +
                "} " + super.toString();
    }
}
