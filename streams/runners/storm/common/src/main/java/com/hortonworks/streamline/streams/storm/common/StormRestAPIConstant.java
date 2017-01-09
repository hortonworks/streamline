package org.apache.streamline.streams.storm.common;

public class StormRestAPIConstant {
    private StormRestAPIConstant() {
    }

    public static final String TOPOLOGY_JSON_UPTIME_SECS = "uptimeSeconds";
    public static final String TOPOLOGY_JSON_STATUS = "status";
    public static final String TOPOLOGY_JSON_WORKERS_TOTAL = "workersTotal";
    public static final String TOPOLOGY_JSON_EXECUTORS_TOTAL = "executorsTotal";
    public static final String TOPOLOGY_JSON_STATS = "topologyStats";
    public static final String TOPOLOGY_JSON_WINDOW = "window";
    public static final String TOPOLOGY_JSON_SPOUTS = "spouts";
    public static final String TOPOLOGY_JSON_SPOUT_ID = "spoutId";
    public static final String TOPOLOGY_JSON_BOLTS = "bolts";
    public static final String TOPOLOGY_JSON_BOLT_ID = "boltId";
    public static final String TOPOLOGY_JSON_COMPONENT_ERRORS = "componentErrors";

    public static final String STATS_JSON_EXECUTED_TUPLES = "executed";
    public static final String STATS_JSON_EMITTED_TUPLES = "emitted";
    public static final String STATS_JSON_TRANSFERRED_TUPLES = "transferred";
    public static final String STATS_JSON_PROCESS_LATENCY = "processLatency";
    public static final String STATS_JSON_COMPLETE_LATENCY = "completeLatency";
    public static final String STATS_JSON_ACKED_TUPLES = "acked";
    public static final String STATS_JSON_FAILED_TUPLES = "failed";
    public static final String STATS_JSON_TOPOLOGY_ERROR_COUNT = "errors";

    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGIES = "topologies";
    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME = "name";
    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGY_ID_ENCODED = "encodedId";
}
