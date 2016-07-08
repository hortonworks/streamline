package com.hortonworks.iotas.streams.metrics.topology;

import com.hortonworks.iotas.streams.layout.component.TopologyLayout;
import com.hortonworks.iotas.streams.metrics.TimeSeriesQuerier;

import java.util.Map;

/**
 * Interface which defines methods for querying topology metrics from time-series DB.
 * <p/>
 * Implementation of this interface should convert metric name between IoTaS and streaming framework.
 * Converted metric name will be converted once again from TimeSeriesQuerier to perform actual query to time-series DB.
 */
public interface TopologyTimeSeriesMetrics {
    /**
     * Set instance of TimeSeriesQuerier. This method should be called before calling any requests for metrics.
     */
    void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier);

    /**
     * Retrieve "complete latency" on source.
     *
     * @param topology  topology catalog instance
     * @param sourceId  source id (same to component id)
     * @param from      beginning of the time period: timestamp (in milliseconds)
     * @param to        end of the time period: timestamp (in milliseconds)
     * @return Map of data points which are paired to (timestamp, value)
     */
    Map<Long, Double> getCompleteLatency(TopologyLayout topology, String sourceId, long from, long to);

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
     * @param topology  topology catalog instance
     * @param sourceId  source id (same to component id)
     * @param from      beginning of the time period: timestamp (in milliseconds)
     * @param to        end of the time period: timestamp (in milliseconds)
     * @return Map of metric name and Map of data points which are paired to (timestamp, value).
     */
    Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, String sourceId, long from, long to);

    /**
     * Retrieve "component stats" on component.
     * <p/>
     * This method retrieves five metrics,<br/>
     * 1) "inputRecords": Count of input records<br/>
     * 2) "outputRecords": Count of output records<br/>
     * 3) "failedRecords": Count of failed records<br/>
     * 4) "processedTime": Latency of processed time (processing one event)<br/>
     * 5) "recordsInWaitQueue": Count of records waiting in queue<br/>
     *
     * @param topology      topology catalog instance
     * @param componentId   component id
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @return Map of metric name and Map of data points which are paired to (timestamp, value).
     */
    Map<String, Map<Long, Double>> getComponentStats(TopologyLayout topology, String componentId, long from, long to);

    /**
     * Get instance of TimeSeriesQuerier.
     */
    TimeSeriesQuerier getTimeSeriesQuerier();
}
