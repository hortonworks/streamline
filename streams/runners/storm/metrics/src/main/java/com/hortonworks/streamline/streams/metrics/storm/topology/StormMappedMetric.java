package com.hortonworks.streamline.streams.metrics.storm.topology;

import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;

/**
 * Metric name conversion table between Streamline and Storm. It also contains function information for aggregation.
 */
enum StormMappedMetric {
    completeLatency("__complete-latency", TimeSeriesQuerier.AggregateFunction.AVG),
    inputRecords("__execute-count", TimeSeriesQuerier.AggregateFunction.SUM),
    outputRecords("__emit-count", TimeSeriesQuerier.AggregateFunction.SUM),
    ackedRecords("__ack-count", TimeSeriesQuerier.AggregateFunction.SUM),
    failedRecords("__fail-count", TimeSeriesQuerier.AggregateFunction.SUM),
    processedTime("__process-latency", TimeSeriesQuerier.AggregateFunction.AVG),
    recordsInWaitQueue("__receive.population", TimeSeriesQuerier.AggregateFunction.AVG),

    // Kafka related metrics are already partitions aggregated value so actually don't need to have aggregate function
    // but they need topic name to be queried
    logsize("kafkaOffset.%s/totalLatestTimeOffset", TimeSeriesQuerier.AggregateFunction.AVG),
    offset("kafkaOffset.%s/totalLatestCompletedOffset", TimeSeriesQuerier.AggregateFunction.AVG),
    lag("kafkaOffset.%s/totalSpoutLag", TimeSeriesQuerier.AggregateFunction.AVG);

    private final String stormMetricName;
    private final TimeSeriesQuerier.AggregateFunction aggregateFunction;

    StormMappedMetric(String stormMetricName, TimeSeriesQuerier.AggregateFunction aggregateFunction) {
        this.stormMetricName = stormMetricName;
        this.aggregateFunction = aggregateFunction;
    }

    public String getStormMetricName() {
        return stormMetricName;
    }

    public TimeSeriesQuerier.AggregateFunction getAggregateFunction() {
        return aggregateFunction;
    }
}
