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

import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;

import java.util.Map;

/**
 * Interface which defines methods for querying topology metrics from time-series DB.
 * <p/>
 * Implementation of this interface should convert metric name between Streamline and streaming framework.
 * Converted metric name will be converted once again from TimeSeriesQuerier to perform actual query to time-series DB.
 */
public interface TopologyTimeSeriesMetrics {
    /**
     * Set instance of TimeSeriesQuerier. This method should be called before calling any requests for metrics.
     */
    void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier);

    /**
     * Retrieve "topology stats" on topology.
     * Implementator should aggregate all components' metrics values to make topology stats.
     * The return value is a TimeSeriesComponentMetric which value of componentName is dummy or topology name.
     *
     * @param topology      topology catalog instance
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @param asUser        username if request needs impersonation to specific user
     * @return Map of metric name and Map of data points which are paired to (timestamp, value).
     */
    TimeSeriesComponentMetric getTopologyStats(TopologyLayout topology, long from, long to, String asUser);

    /**
     * Retrieve "complete latency" on source.
     *
     * @param topology  topology catalog instance
     * @param component component layout instance
     * @param from      beginning of the time period: timestamp (in milliseconds)
     * @param to        end of the time period: timestamp (in milliseconds)
     * @param asUser        username if request needs impersonation to specific user
     * @return Map of data points which are paired to (timestamp, value)
     */
    Map<Long, Double> getCompleteLatency(TopologyLayout topology, Component component, long from, long to, String asUser);

    /**
     * Retrieve "kafka topic offsets" on source.
     * <p/>
     * This method retrieves three metrics,<br/>
     * 1) "logsize": sum of partition's available offsets for all partitions<br/>
     * 2) "offset": sum of source's current offsets for all partitions<br/>
     * 3) "lag": sum of lags (available offset - current offset) for all partitions<br/>
     * <p/>
     * That source should be "KAFKA" type and have topic name from configuration.
     *
     * @param topology  topology layout instance
     * @param component component layout instance
     * @param from      beginning of the time period: timestamp (in milliseconds)
     * @param to        end of the time period: timestamp (in milliseconds)
     * @param asUser        username if request needs impersonation to specific user
     * @return Map of metric name and Map of data points which are paired to (timestamp, value).
     */
    Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, Component component, long from, long to, String asUser);

    /**
     * Retrieve "component stats" on component.
     *
     * @param topology      topology catalog instance
     * @param component     component layout instance
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @param asUser        username if request needs impersonation to specific user
     * @return Map of metric name and Map of data points which are paired to (timestamp, value).
     */
    TimeSeriesComponentMetric getComponentStats(TopologyLayout topology, Component component, long from, long to, String asUser);

    /**
     * Get instance of TimeSeriesQuerier.
     */
    TimeSeriesQuerier getTimeSeriesQuerier();

    /**
     * Data structure of Metrics for each component on topology.
     * Fields are extracted from common metrics among various streaming frameworks.
     *
     * Implementors of TopologyTimeSeriesMetrics are encouraged to provide fields' value as many as possible.
     * If field is not available for that streaming framework, implementator can leave it as null or default value.
     */
    class TimeSeriesComponentMetric {
        private final String componentName;
        private final Map<Long, Double> inputRecords;
        private final Map<Long, Double> outputRecords;
        private final Map<Long, Double> failedRecords;
        private final Map<Long, Double> processedTime;
        private final Map<Long, Double> recordsInWaitQueue;
        private final Map<String, Map<Long, Double>> misc;

        /**
         * Constructor.
         * @param componentName 'component name' for Streamline.
         *                      If component name for streaming framework is different from component name for Streamline,
         *                      implementation of TopologyTimeSeriesMetrics should match the relation.
         * @param inputRecords  Count of input records.
         * @param outputRecords Count of output records.
         * @param failedRecords Count of failed records.
         * @param processedTime Average latency of processed time (processing one record).
         * @param recordsInWaitQueue    Count of records which are waiting in a queue.
         * @param misc          Additional metrics which are framework specific.
         */
        public TimeSeriesComponentMetric(String componentName, Map<Long, Double> inputRecords,
                                         Map<Long, Double> outputRecords, Map<Long, Double> failedRecords,
                                         Map<Long, Double> processedTime, Map<Long, Double> recordsInWaitQueue,
                                         Map<String, Map<Long, Double>> misc) {
            this.componentName = componentName;
            this.inputRecords = inputRecords;
            this.outputRecords = outputRecords;
            this.failedRecords = failedRecords;
            this.processedTime = processedTime;
            this.recordsInWaitQueue = recordsInWaitQueue;
            this.misc = misc;
        }

        public String getComponentName() {
            return componentName;
        }

        public Map<Long, Double> getInputRecords() {
            return inputRecords;
        }

        public Map<Long, Double> getOutputRecords() {
            return outputRecords;
        }

        public Map<Long, Double> getFailedRecords() {
            return failedRecords;
        }

        public Map<Long, Double> getProcessedTime() {
            return processedTime;
        }

        public Map<Long, Double> getRecordsInWaitQueue() {
            return recordsInWaitQueue;
        }

        public Map<String, Map<Long, Double>> getMisc() {
            return misc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TimeSeriesComponentMetric)) return false;

            TimeSeriesComponentMetric that = (TimeSeriesComponentMetric) o;

            if (getComponentName() != null ? !getComponentName().equals(that.getComponentName()) : that.getComponentName() != null)
                return false;
            if (getInputRecords() != null ? !getInputRecords().equals(that.getInputRecords()) : that.getInputRecords() != null)
                return false;
            if (getOutputRecords() != null ? !getOutputRecords().equals(that.getOutputRecords()) : that.getOutputRecords() != null)
                return false;
            if (getFailedRecords() != null ? !getFailedRecords().equals(that.getFailedRecords()) : that.getFailedRecords() != null)
                return false;
            if (getProcessedTime() != null ? !getProcessedTime().equals(that.getProcessedTime()) : that.getProcessedTime() != null)
                return false;
            if (getRecordsInWaitQueue() != null ? !getRecordsInWaitQueue().equals(that.getRecordsInWaitQueue()) : that.getRecordsInWaitQueue() != null)
                return false;
            return getMisc() != null ? getMisc().equals(that.getMisc()) : that.getMisc() == null;
        }

        @Override
        public int hashCode() {
            int result = getComponentName() != null ? getComponentName().hashCode() : 0;
            result = 31 * result + (getInputRecords() != null ? getInputRecords().hashCode() : 0);
            result = 31 * result + (getOutputRecords() != null ? getOutputRecords().hashCode() : 0);
            result = 31 * result + (getFailedRecords() != null ? getFailedRecords().hashCode() : 0);
            result = 31 * result + (getProcessedTime() != null ? getProcessedTime().hashCode() : 0);
            result = 31 * result + (getRecordsInWaitQueue() != null ? getRecordsInWaitQueue().hashCode() : 0);
            result = 31 * result + (getMisc() != null ? getMisc().hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TimeSeriesComponentMetric{" +
                    "componentName='" + componentName + '\'' +
                    ", inputRecords=" + inputRecords +
                    ", outputRecords=" + outputRecords +
                    ", failedRecords=" + failedRecords +
                    ", processedTime=" + processedTime +
                    ", recordsInWaitQueue=" + recordsInWaitQueue +
                    ", misc=" + misc +
                    '}';
        }
    }
}
