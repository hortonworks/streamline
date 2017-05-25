package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores the information of topology test run.
 */
@StorableEntity
public class TopologyTestRunHistory extends AbstractStorable {
    public static final String NAMESPACE = "topology_test_run_histories";

    public static final String ID = "id";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String VERSION_ID = "versionId";
    public static final String TEST_CASE_ID = "testCaseId";
    private static final String FINISHED = "finished";
    private static final String SUCCESS = "success";
    private static final String EXPECTED_OUTPUT_RECORDS = "expectedOutputRecords";
    private static final String ACTUAL_OUTPUT_RECORDS = "actualOutputRecords";
    private static final String MATCHED = "matched";

    // Intentionally hidden to the REST API, getter method should be hidden to @JsonIgnore
    private static final String EVENT_LOG_FILE_PATH = "eventLogFilePath";

    private static final String START_TIME = "startTime";
    private static final String FINISH_TIME = "finishTime";
    private static final String TIMESTAMP = "timestamp";

    private Long id;
    private Long topologyId;
    private Long versionId;
    private Long testCaseId;
    private Boolean finished = false;
    private Boolean success = false;
    private String expectedOutputRecords;
    private String actualOutputRecords;
    private Boolean matched = false;
    private String eventLogFilePath;
    private Long startTime;
    private Long finishTime;
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
     *  The foreign key reference to the topology id.
     */
    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    /**
     * The foreign key reference to the version id.
     */
    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
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
     * The flag indicating topology test run is finished.
     */
    public Boolean getFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    /**
     * The Flag indicating topology test run works without problem. It doesn't cover streaming runtime errors.
     */
    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    /**
     * The JSON representation of expected output records.
     */
    public String getExpectedOutputRecords() {
        return expectedOutputRecords;
    }

    public void setExpectedOutputRecords(String expectedOutputRecords) {
        this.expectedOutputRecords = expectedOutputRecords;
    }

    /**
     * The JSON representation of actual output records.
     */
    public String getActualOutputRecords() {
        return actualOutputRecords;
    }

    public void setActualOutputRecords(String actualOutputRecords) {
        this.actualOutputRecords = actualOutputRecords;
    }

    /**
     * The flag indicating whether expected and actual are matched or not.
     */
    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    /**
     * The path of file which event log is (or will be) written.
     * Intentionally hidden to the REST API, getter method should be hidden to @JsonIgnore.
     */
    @JsonIgnore
    public String getEventLogFilePath() {
        return eventLogFilePath;
    }

    public void setEventLogFilePath(String eventLogFilePath) {
        this.eventLogFilePath = eventLogFilePath;
    }

    /**
     * The timestamp of start time of test run.
     */
    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    /**
     * The timestamp of finish time of test run.
     */
    public Long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Long finishTime) {
        this.finishTime = finishTime;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void start() {
        finished = false;
        startTime = System.currentTimeMillis();
    }

    public void finish() {
        this.finished = true;
        this.startTime = System.currentTimeMillis();
    }

    public void finishSuccessfully() {
        this.finished = true;
        this.finishTime = System.currentTimeMillis();
        this.success = true;
    }

    public void finishWithFailures() {
        this.finished = true;
        this.finishTime = System.currentTimeMillis();
        this.success = false;
    }

    @Override
    public Schema getSchema() {
        return Schema.of(
                new Schema.Field(ID, Schema.Type.LONG),
                new Schema.Field(TOPOLOGY_ID, Schema.Type.LONG),
                new Schema.Field(VERSION_ID, Schema.Type.LONG),
                new Schema.Field(TEST_CASE_ID, Schema.Type.LONG),
                new Schema.Field(FINISHED, Schema.Type.STRING),
                Schema.Field.optional(SUCCESS, Schema.Type.STRING),
                Schema.Field.optional(EXPECTED_OUTPUT_RECORDS, Schema.Type.STRING),
                Schema.Field.optional(ACTUAL_OUTPUT_RECORDS, Schema.Type.STRING),
                Schema.Field.optional(MATCHED, Schema.Type.STRING),
                new Schema.Field(EVENT_LOG_FILE_PATH, Schema.Type.STRING),
                new Schema.Field(START_TIME, Schema.Type.LONG),
                Schema.Field.optional(FINISH_TIME, Schema.Type.LONG),
                new Schema.Field(TIMESTAMP, Schema.Type.LONG)
        );
    }

    @Override
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ID, id);
        map.put(TOPOLOGY_ID, topologyId);
        map.put(VERSION_ID, versionId);
        map.put(TEST_CASE_ID, testCaseId);

        if (finished != null) {
            map.put(FINISHED, finished.toString());
        } else {
            map.put(FINISHED, null);
        }

        if (success != null) {
            map.put(SUCCESS, success.toString());
        } else {
            map.put(SUCCESS, null);
        }

        map.put(EXPECTED_OUTPUT_RECORDS, expectedOutputRecords);
        map.put(ACTUAL_OUTPUT_RECORDS, actualOutputRecords);

        if (matched != null) {
            map.put(MATCHED, matched.toString());
        } else {
            map.put(MATCHED, null);
        }

        map.put(EVENT_LOG_FILE_PATH, eventLogFilePath);

        map.put(START_TIME, startTime);
        map.put(FINISH_TIME, finishTime);
        map.put(TIMESTAMP, timestamp);
        return map;
    }

    @Override
    public Storable fromMap(Map<String, Object> map) {
        id = (Long) map.get(ID);
        topologyId = (Long) map.get(TOPOLOGY_ID);
        versionId = (Long) map.get(VERSION_ID);
        testCaseId = (Long) map.get(TEST_CASE_ID);

        if (map.get(FINISHED) != null) {
            finished = Boolean.valueOf((String) map.get(FINISHED));
        } else {
            finished = null;
        }

        if (map.get(SUCCESS) != null) {
            success = Boolean.valueOf((String) map.get(SUCCESS));
        } else {
            success = null;
        }

        expectedOutputRecords = (String) map.get(EXPECTED_OUTPUT_RECORDS);
        actualOutputRecords = (String) map.get(ACTUAL_OUTPUT_RECORDS);

        if (map.get(MATCHED) != null) {
            matched = Boolean.valueOf((String) map.get(MATCHED));
        } else {
            matched = null;
        }

        eventLogFilePath = (String) map.get(EVENT_LOG_FILE_PATH);

        startTime = (Long) map.get(START_TIME);
        finishTime = (Long) map.get(FINISH_TIME);
        timestamp = (Long) map.get(TIMESTAMP);

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopologyTestRunHistory)) return false;

        TopologyTestRunHistory that = (TopologyTestRunHistory) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getTopologyId() != null ? !getTopologyId().equals(that.getTopologyId()) : that.getTopologyId() != null)
            return false;
        if (getVersionId() != null ? !getVersionId().equals(that.getVersionId()) : that.getVersionId() != null)
            return false;
        if (getTestCaseId() != null ? !getTestCaseId().equals(that.getTestCaseId()) : that.getTestCaseId() != null)
            return false;
        if (getFinished() != null ? !getFinished().equals(that.getFinished()) : that.getFinished() != null)
            return false;
        if (getSuccess() != null ? !getSuccess().equals(that.getSuccess()) : that.getSuccess() != null) return false;
        if (getExpectedOutputRecords() != null ? !getExpectedOutputRecords().equals(that.getExpectedOutputRecords()) : that.getExpectedOutputRecords() != null)
            return false;
        if (getActualOutputRecords() != null ? !getActualOutputRecords().equals(that.getActualOutputRecords()) : that.getActualOutputRecords() != null)
            return false;
        if (getMatched() != null ? !getMatched().equals(that.getMatched()) : that.getMatched() != null) return false;
        if (getEventLogFilePath() != null ? !getEventLogFilePath().equals(that.getEventLogFilePath()) : that.getEventLogFilePath() != null)
            return false;
        if (getStartTime() != null ? !getStartTime().equals(that.getStartTime()) : that.getStartTime() != null)
            return false;
        if (getFinishTime() != null ? !getFinishTime().equals(that.getFinishTime()) : that.getFinishTime() != null)
            return false;
        return getTimestamp() != null ? getTimestamp().equals(that.getTimestamp()) : that.getTimestamp() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getTopologyId() != null ? getTopologyId().hashCode() : 0);
        result = 31 * result + (getVersionId() != null ? getVersionId().hashCode() : 0);
        result = 31 * result + (getTestCaseId() != null ? getTestCaseId().hashCode() : 0);
        result = 31 * result + (getFinished() != null ? getFinished().hashCode() : 0);
        result = 31 * result + (getSuccess() != null ? getSuccess().hashCode() : 0);
        result = 31 * result + (getExpectedOutputRecords() != null ? getExpectedOutputRecords().hashCode() : 0);
        result = 31 * result + (getActualOutputRecords() != null ? getActualOutputRecords().hashCode() : 0);
        result = 31 * result + (getMatched() != null ? getMatched().hashCode() : 0);
        result = 31 * result + (getEventLogFilePath() != null ? getEventLogFilePath().hashCode() : 0);
        result = 31 * result + (getStartTime() != null ? getStartTime().hashCode() : 0);
        result = 31 * result + (getFinishTime() != null ? getFinishTime().hashCode() : 0);
        result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TopologyTestRunHistory{" +
                "id=" + id +
                ", topologyId=" + topologyId +
                ", versionId=" + versionId +
                ", testCaseId=" + testCaseId +
                ", finished=" + finished +
                ", success=" + success +
                ", expectedOutputRecords='" + expectedOutputRecords + '\'' +
                ", actualOutputRecords='" + actualOutputRecords + '\'' +
                ", matched=" + matched +
                ", eventLogFilePath='" + eventLogFilePath + '\'' +
                ", startTime=" + startTime +
                ", finishTime=" + finishTime +
                ", timestamp=" + timestamp +
                '}';
    }
}
