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
 * This class stores actual test records for specific test source.
 */
@StorableEntity
public class TopologyTestRunCaseSource extends AbstractStorable {
    public static final String NAMESPACE = "topology_test_run_case_source";

    private Long id;
    private Long testCaseId;
    private Long sourceId;
    private String records;
    private Integer occurrence;
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
     * The foreign key reference to the topology source id.
     */
    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * Test records for given source.
     */
    public String getRecords() {
        return records;
    }

    public void setRecords(String records) {
        this.records = records;
    }

    /**
     * The occurrence of test records. For example, if there're two test records and occurrence is set to 2,
     * test source will emit four records.
     */
    public Integer getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(Integer occurrence) {
        this.occurrence = occurrence;
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
        if (!(o instanceof TopologyTestRunCaseSource)) return false;

        TopologyTestRunCaseSource that = (TopologyTestRunCaseSource) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getTestCaseId() != null ? !getTestCaseId().equals(that.getTestCaseId()) : that.getTestCaseId() != null)
            return false;
        if (getSourceId() != null ? !getSourceId().equals(that.getSourceId()) : that.getSourceId() != null)
            return false;
        if (getRecords() != null ? !getRecords().equals(that.getRecords()) : that.getRecords() != null) return false;
        return getTimestamp() != null ? getTimestamp().equals(that.getTimestamp()) : that.getTimestamp() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getTestCaseId() != null ? getTestCaseId().hashCode() : 0);
        result = 31 * result + (getSourceId() != null ? getSourceId().hashCode() : 0);
        result = 31 * result + (getRecords() != null ? getRecords().hashCode() : 0);
        result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TopologyTestRunCaseSource{" +
                "id=" + id +
                ", testCaseId=" + testCaseId +
                ", sourceId=" + sourceId +
                ", records='" + records + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
