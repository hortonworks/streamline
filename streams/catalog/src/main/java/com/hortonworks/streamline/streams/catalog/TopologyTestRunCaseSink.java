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

import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores expected records for specific test sink.
 */
@StorableEntity
public class TopologyTestRunCaseSink extends AbstractStorable {
    public static final String NAMESPACE = "topology_test_run_case_sink";

    private Long id;
    private Long testCaseId;
    private Long sinkId;
    private String records;
    private Long timestamp;

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

    /**
     * The primary key
     */
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * The foreign key reference to the test case id.
     */
    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    /**
     * The foreign key reference to the topology sink id.
     */
    public Long getSinkId() {
        return sinkId;
    }

    public void setSinkId(Long sinkId) {
        this.sinkId = sinkId;
    }

    /**
     * Test expected records for given sink.
     */
    public String getRecords() {
        return records;
    }

    public void setRecords(String records) {
        this.records = records;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopologyTestRunCaseSink)) return false;

        TopologyTestRunCaseSink that = (TopologyTestRunCaseSink) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getTestCaseId() != null ? !getTestCaseId().equals(that.getTestCaseId()) : that.getTestCaseId() != null)
            return false;
        if (getSinkId() != null ? !getSinkId().equals(that.getSinkId()) : that.getSinkId() != null)
            return false;
        if (getRecords() != null ? !getRecords().equals(that.getRecords()) : that.getRecords() != null) return false;
        return getTimestamp() != null ? getTimestamp().equals(that.getTimestamp()) : that.getTimestamp() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getTestCaseId() != null ? getTestCaseId().hashCode() : 0);
        result = 31 * result + (getSinkId() != null ? getSinkId().hashCode() : 0);
        result = 31 * result + (getRecords() != null ? getRecords().hashCode() : 0);
        result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TopologyTestRunCaseSource{" +
                "id=" + id +
                ", testCaseId=" + testCaseId +
                ", sinkId=" + sinkId +
                ", records='" + records + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
