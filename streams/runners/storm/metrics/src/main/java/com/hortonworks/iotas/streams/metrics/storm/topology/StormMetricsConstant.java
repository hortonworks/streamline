package com.hortonworks.iotas.streams.metrics.storm.topology;

public class StormMetricsConstant {
    private StormMetricsConstant() {
    }

    public static final String TOPOLOGY_JSON_UPTIME_SECS = "uptimeSeconds";
    public static final String TOPOLOGY_JSON_STATUS = "status";
    public static final String TOPOLOGY_JSON_STATS = "topologyStats";
    public static final String TOPOLOGY_JSON_WINDOW = "window";
    public static final String TOPOLOGY_JSON_SPOUTS = "spouts";
    public static final String TOPOLOGY_JSON_SPOUT_ID = "spoutId";
    public static final String TOPOLOGY_JSON_BOLTS = "bolts";
    public static final String TOPOLOGY_JSON_BOLT_ID = "boltId";

    public static final String STATS_JSON_EXECUTED_TUPLES = "executed";
    public static final String STATS_JSON_EMITTED_TUPLES = "emitted";
    public static final String STATS_JSON_PROCESS_LATENCY = "processLatency";
    public static final String STATS_JSON_COMPLETE_LATENCY = "completeLatency";
    public static final String STATS_JSON_ACKED_TUPLES = "acked";
    public static final String STATS_JSON_FAILED_TUPLES = "failed";

    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGIES = "topologies";
    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME = "name";
    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGY_ID_ENCODED = "encodedId";
}
