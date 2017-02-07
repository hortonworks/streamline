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
