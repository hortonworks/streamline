package com.hortonworks.iotas.topology.storm;

public class StormMetricsConstant {
    private StormMetricsConstant() {
    }

    public static final String COMPONENT_EXECUTED_TUPLES = "executed";
    public static final String COMPONENT_EMITTED_TUPLES = "emitted";
    public static final String COMPONENT_PROCESS_LATENCY = "processLatency";
    public static final String COMPONENT_FAILED_TUPLES = "failed";

    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGIES = "topologies";
    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME = "name";
    public static final String TOPOLOGY_SUMMARY_JSON_TOPOLOGY_ID_ENCODED = "encodedId";
}
