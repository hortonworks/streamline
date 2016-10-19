package org.apache.streamline.streams.metrics.topology;

import org.apache.streamline.streams.exception.ConfigException;
import org.apache.streamline.streams.layout.component.TopologyLayout;

import java.util.Map;

/**
 * Interface that shows metrics for IoTaS topology.
 * <p/>
 * Each underlying streaming framework should provide implementations to integrate metrics of framework into IoTaS.
 * <p/>
 * Note that this interface also extends TopologyTimeSeriesMetrics, which is for querying topology metrics from time-series DB.
 */
public interface TopologyMetrics extends TopologyTimeSeriesMetrics {
    /**
     * Initialize method. Any one time initialization is done here.
     *
     * @param conf Configuration for implementation of TopologyMetrics.
     * @throws ConfigException throw when instance can't be initialized with this configuration (misconfigured).
     */
    void init (Map<String, String> conf) throws ConfigException;

    /**
     * Retrieves topology metric for IoTaS topology/
     *
     * @param topology topology catalog instance. Implementations should find actual runtime topology with provided topology.
     * @return TopologyMetrics
     */
    TopologyMetric getTopologyMetric(TopologyLayout topology);

    /**
     * Retrieves metrics data for IoTaS topology.
     *
     * @param topology topology catalog instance. Implementations should find actual runtime topology with provided topology.
     * @return pair of (component name, ComponentMetric instance).
     * Implementations should ensure that component name is same to UI name of component
     * so that it can be matched to IoTas topology.
     */
    Map<String, ComponentMetric> getMetricsForTopology(TopologyLayout topology);

    /**
     * Data structure of Metrics for each component on topology.
     * Fields are extracted from common metrics among various streaming frameworks.
     *
     * Implementors of TopologyMetrics are encouraged to provide fields' value as many as possible.
     * If field is not available for that streaming framework, implementator can leave it as null or default value.
     */
    class TopologyMetric {
        private final String topologyName;
        private final String status;
        private final Long uptime;
        private final Long windowSecs;
        private final Double throughput;
        private final Double latency;
        private final Long failedRecords;

        /**
         * Constructor.
         *  @param topologyName  'topology name' for Streams.
         *                      If topology name for streaming framework is different from topology name for Streams,
         *                      implementation of TopologyMetrics should match the relation.
         * @param status        Status of the topology. Representation of status may be different among implementations.
         * @param uptime        Uptime for Streams.
         * @param windowSecs    Time window (seconds) for below metrics.
         * @param throughput    Throughput for Streams in window, represented via tps (processed records per second).
         * @param latency       Average latency of processed time (processing one record) in window.
         * @param failedRecords Failed records in window.
         */
        public TopologyMetric(String topologyName, String status, Long uptime, Long windowSecs,
            Double throughput, Double latency, Long failedRecords) {
            this.topologyName = topologyName;
            this.status = status;
            this.uptime = uptime;
            this.windowSecs = windowSecs;
            this.throughput = throughput;
            this.latency = latency;
            this.failedRecords = failedRecords;
        }

        public String getTopologyName() {
            return topologyName;
        }

        public String getStatus() {
            return status;
        }

        public Long getUptime() {
            return uptime;
        }

        public Long getWindowSecs() {
            return windowSecs;
        }

        public Double getThroughput() {
            return throughput;
        }

        public Double getLatency() {
            return latency;
        }

        public Long getFailedRecords() {
            return failedRecords;
        }
    }

    /**
     * Data structure of Metrics for each component on topology.
     * Fields are extracted from common metrics among various streaming frameworks.
     *
     * Implementors of TopologyMetrics are encouraged to provide fields' value as many as possible.
     * If field is not available for that streaming framework, implementator can leave it as null or default value.
     */
    class ComponentMetric {
        private final String componentName;
        private final Long inputRecords;
        private final Long outputRecords;
        private final Long failedRecords;
        private final Double processedTime;

        /**
         * Constructor.
         *
         * @param componentName 'component name' for Streams.
         *                      If component name for streaming framework is different from component name for Streams,
         *                      implementation of TopologyMetrics should match the relation.
         * @param inputRecords  Count of input records.
         * @param outputRecords Count of output records.
         * @param failedRecords Count of failed records.
         * @param processedTime Average latency of processed time (processing one record).
         */
        public ComponentMetric(String componentName, Long inputRecords, Long outputRecords, Long failedRecords, Double processedTime) {
            this.componentName = componentName;
            this.inputRecords = inputRecords;
            this.outputRecords = outputRecords;
            this.failedRecords = failedRecords;
            this.processedTime = processedTime;
        }

        public String getComponentName() {
            return componentName;
        }

        public Long getInputRecords() {
            return inputRecords;
        }

        public Long getOutputRecords() {
            return outputRecords;
        }

        public Long getFailedRecords() {
            return failedRecords;
        }

        public Double getProcessedTime() {
            return processedTime;
        }
    }
}
