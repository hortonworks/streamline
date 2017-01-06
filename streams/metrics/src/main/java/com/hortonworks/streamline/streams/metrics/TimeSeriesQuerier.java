package com.hortonworks.streamline.streams.metrics;

import com.hortonworks.streamline.streams.exception.ConfigException;

import java.util.Map;

/**
 * Interface for querying to Time-series DB.
 * <p/>
 * Implementation of this interface should convert metric name streaming framework and actual stored metric name.
 * Since metric name depends on metrics sink so implementation may want to be coupled with specific metrics sink.
 */
public interface TimeSeriesQuerier {
    /**
     * Initialize method. Any one time initialization is done here.
     *
     * @param conf Configuration for implementation of TopologyMetrics.
     * @throws ConfigException throw when instance can't be initialized with this configuration (misconfigured).
     */
    void init (Map<String, String> conf) throws ConfigException;

    /**
     * Query metrics to time-series DB. The result should aggregate all components metrics to one (meaning topology level).
     *
     * @param topologyName  topology name (not ID)
     * @param metricName    metric name
     * @param aggrFunction  function to apply while aggregating task level series
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @return Map of data points which are paired to (timestamp, value)
     */
    Map<Long, Double> getTopologyLevelMetrics(String topologyName, String metricName, AggregateFunction aggrFunction, long from, long to);

    /**
     * Query metrics to time-series DB.
     *
     * @param topologyName  topology name (not ID)
     * @param componentId   component id
     * @param metricName    metric name
     * @param aggrFunction  function to apply while aggregating task level series
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @return Map of data points which are paired to (timestamp, value)
     */
    Map<Long, Double> getMetrics(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction, long from, long to);

    /**
     * Query metrics without modification (raw) to time-series DB.
     *
     * @param metricName    metric name
     * @param parameters    parameters separated by ',', and represented as 'key=value'
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @return Map of metric name and Map of data points which are paired to (timestamp, value)
     */
    Map<String, Map<Long, Double>> getRawMetrics(String metricName, String parameters, long from, long to);

    /**
     * Function to apply while aggregating multiple series
     */
    enum AggregateFunction {
        SUM, AVG, MIN, MAX
    }
}
