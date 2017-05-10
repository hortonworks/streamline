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
package com.hortonworks.streamline.streams.metrics.topology;

import org.apache.commons.lang3.tuple.Pair;
import com.hortonworks.streamline.streams.exception.ConfigException;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;

import java.util.List;
import java.util.Map;

/**
 * Interface that shows metrics for Streamline topology.
 * <p/>
 * Each underlying streaming framework should provide implementations to integrate metrics of framework into Streamline.
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
     * Retrieves topology metric for Streamline topology/
     *
     * @param topology topology catalog instance. Implementations should find actual runtime topology with provided topology.
     * @param asUser   username if request needs impersonation to specific user
     * @return TopologyMetrics
     */
    TopologyMetric getTopologyMetric(TopologyLayout topology, String asUser);

    /**
     * Retrieves metrics data for Streamline topology.
     *
     * @param topology topology catalog instance. Implementations should find actual runtime topology with provided topology.
     * @param asUser   username if request needs impersonation to specific user
     * @return pair of (component id, ComponentMetric instance).
     * Implementations should ensure that component name is same to UI name of component
     * so that it can be matched to Streamline topology.
     */
    Map<String, ComponentMetric> getMetricsForTopology(TopologyLayout topology, String asUser);

    /**
     * Data structure of Metrics for each component on topology.
     * Fields are extracted from common metrics among various streaming frameworks.
     *
     * Implementors of TopologyMetrics are encouraged to provide fields' value as many as possible.
     * If field is not available for that streaming framework, implementator can leave it as null or default value.
     */
    class TopologyMetric {
        private final String framework;
        private final String topologyName;
        private final String status;
        private final Long uptime;
        private final Long windowSecs;
        private final Double throughput;
        private final Double latency;
        private final Long failedRecords;
        private final Map<String, Number> misc;

        /**
         * Constructor.
         * @param framework        Which streaming framework runs this topology. (e.g. Storm, Spark Streaming, etc.)
         * @param topologyName  'topology name' for Streams.
         *                      If topology name for streaming framework is different from topology name for Streams,
         *                      implementation of TopologyMetrics should match the relation.
         * @param status        Status of the topology. Representation of status may be different among implementations.
         * @param uptime        Uptime for Streams.
         * @param windowSecs    Time window (seconds) for below metrics.
         * @param throughput    Throughput for Streams in window, represented via tps (processed records per second).
         * @param latency       Average latency of processed time (processing one record) in window.
         * @param failedRecords Failed records in window.
         * @param misc          Additional metrics which are framework specific.
         */
        public TopologyMetric(String framework, String topologyName, String status, Long uptime,
            Long windowSecs, Double throughput, Double latency, Long failedRecords,
            Map<String, Number> misc) {
            this.framework = framework;
            this.topologyName = topologyName;
            this.status = status;
            this.uptime = uptime;
            this.windowSecs = windowSecs;
            this.throughput = throughput;
            this.latency = latency;
            this.failedRecords = failedRecords;
            this.misc = misc;
        }

        public String getFramework() {
            return framework;
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

        public Map<String, Number> getMisc() {
            return misc;
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
         * @param componentName 'component name' for Streamline.
         *                      If component name for streaming framework is different from component name for Streamline,
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
