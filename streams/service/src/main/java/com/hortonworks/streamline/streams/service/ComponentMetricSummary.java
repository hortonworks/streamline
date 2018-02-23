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
package com.hortonworks.streamline.streams.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.annotations.VisibleForTesting;
import com.hortonworks.streamline.common.util.DoubleUtils;
import com.hortonworks.streamline.streams.metrics.storm.topology.StormMappedMetric;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;

import java.util.Collections;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Note: Given that UI of view mode is tied to Apache Storm, using the term of Storm.
 */
public class ComponentMetricSummary {
    private static final String METRIC_NAME_ACK_COUNT = "ackedRecords";

    private final Long emitted;
    private final Long acked;
    private final Long failed;
    private final Double latency;
    private final Double completeLatency;
    private final Double processTime;
    private final Double executeTime;

    private final Long prevEmitted;
    private final Long prevAcked;
    private final Long prevFailed;
    private final Double prevLatency;
    private final Double prevCompleteLatency;
    private final Double prevProcessTime;
    private final Double prevExecuteTime;

    public ComponentMetricSummary(Long emitted, Long acked, Long failed, Double latency, Double completeLatency,
                                  Double processTime, Double executeTime,
                                  Long prevEmitted, Long prevAcked, Long prevFailed, Double prevLatency,
                                  Double prevCompleteLatency, Double prevProcessTime, Double prevExecuteTime) {
        this.emitted = emitted;
        this.acked = acked;
        this.failed = failed;
        this.latency = latency;
        this.completeLatency = completeLatency;
        this.processTime = processTime;
        this.executeTime = executeTime;
        this.prevEmitted = prevEmitted;
        this.prevAcked = prevAcked;
        this.prevFailed = prevFailed;
        this.prevLatency = prevLatency;
        this.prevCompleteLatency = prevCompleteLatency;
        this.prevProcessTime = prevProcessTime;
        this.prevExecuteTime = prevExecuteTime;
    }

    public Long getEmitted() {
        return emitted;
    }

    public Long getAcked() {
        return acked;
    }

    public Long getFailed() {
        return failed;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getLatency() {
        return latency;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getCompleteLatency() {
        return completeLatency;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getProcessTime() {
        return processTime;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getExecuteTime() {
        return executeTime;
    }

    public Long getPrevEmitted() {
        return prevEmitted;
    }

    public Long getPrevAcked() {
        return prevAcked;
    }

    public Long getPrevFailed() {
        return prevFailed;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getPrevLatency() {
        return prevLatency;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getPrevCompleteLatency() {
        return prevCompleteLatency;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getPrevProcessTime() {
        return prevProcessTime;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getPrevExecuteTime() {
        return prevExecuteTime;
    }

    public static ComponentMetricSummary convertTopologyMetric(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                               TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevMetrics) {
        Long emitted = aggregateEmitted(metrics);
        Long acked = aggregateAcked(metrics);
        Double latency = aggregateCompleteLatency(metrics);
        Long failed = aggregateFailed(metrics);

        Long prevEmitted = null;
        Long prevAcked = null;
        Double prevLatency = null;
        Long prevFailed = null;

        // aggregate the value only if it is available
        if (prevMetrics != null) {
            prevEmitted = aggregateEmitted(prevMetrics);
            prevAcked = aggregateAcked(prevMetrics);
            prevLatency = aggregateCompleteLatency(prevMetrics);
            prevFailed = aggregateFailed(prevMetrics);
        }

        return new ComponentMetricSummary(emitted, acked, failed, latency, null, null, null,
                prevEmitted, prevAcked, prevFailed, prevLatency, null, null, null);
    }

    public static ComponentMetricSummary convertSourceMetric(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                             TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevMetrics) {
        Long emitted = aggregateEmitted(metrics);
        Long acked = aggregateAcked(metrics);
        Double completeLatency = aggregateCompleteLatency(metrics);
        Long failed = aggregateFailed(metrics);

        Long prevEmitted = null;
        Long prevAcked = null;
        Double prevCompleteLatency = null;
        Long prevFailed = null;

        // aggregate the value only if it is available
        if (prevMetrics != null) {
            prevEmitted = aggregateEmitted(prevMetrics);
            prevAcked = aggregateAcked(prevMetrics);
            prevCompleteLatency = aggregateCompleteLatency(prevMetrics);
            prevFailed = aggregateFailed(prevMetrics);
        }

        return new ComponentMetricSummary(emitted, acked, failed, null, completeLatency, null, null,
                prevEmitted, prevAcked, prevFailed, null, prevCompleteLatency, null, null);
    }

    public static ComponentMetricSummary convertNonSourceMetric(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                                TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevMetrics) {
        Long emitted = aggregateEmitted(metrics);
        Long acked = aggregateAcked(metrics);
        Double processLatency = aggregateProcessLatency(metrics);
        Double executeLatency = aggregateExecuteLatency(metrics);
        Long failed = aggregateFailed(metrics);

        Long prevEmitted = null;
        Long prevAcked = null;
        Double prevProcessLatency = null;
        Double prevExecuteLatency = null;
        Long prevFailed = null;

        // aggregate the value only if it is available
        if (prevMetrics != null) {
            prevEmitted = aggregateEmitted(prevMetrics);
            prevAcked = aggregateAcked(prevMetrics);
            prevProcessLatency = aggregateProcessLatency(prevMetrics);
            prevExecuteLatency = aggregateExecuteLatency(prevMetrics);
            prevFailed = aggregateFailed(prevMetrics);
        }

        return new ComponentMetricSummary(emitted, acked, failed, null, null, processLatency, executeLatency,
                prevEmitted, prevAcked, prevFailed, null, null, prevProcessLatency, prevExecuteLatency);
    }

    @VisibleForTesting
    static long aggregateFailed(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
        return metrics.getFailedRecords().values().stream().mapToLong(Double::longValue).sum();
    }

    @VisibleForTesting
    static double aggregateProcessLatency(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
        return calculateWeightedAverage(metrics.getProcessedTime(), metrics.getInputRecords());
    }

    @VisibleForTesting
    static double aggregateExecuteLatency(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
        Map<Long, Double> keyMetrics = metrics.getMisc().getOrDefault(StormMappedMetric.executeTime.name(), Collections.emptyMap());
        return calculateWeightedAverage(keyMetrics, metrics.getInputRecords());
    }

    @VisibleForTesting
    static double aggregateCompleteLatency(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
        Map<Long, Double> keyMetrics = metrics.getMisc().getOrDefault(StormMappedMetric.completeLatency.name(), Collections.emptyMap());
        Map<Long, Double> weightMetrics = metrics.getMisc().getOrDefault(METRIC_NAME_ACK_COUNT, Collections.emptyMap());
        return calculateWeightedAverage(keyMetrics, weightMetrics);
    }

    @VisibleForTesting
    static long aggregateEmitted(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
        return metrics.getOutputRecords().values().stream().mapToLong(Double::longValue).sum();
    }

    @VisibleForTesting
    static long aggregateAcked(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
        return metrics.getMisc().getOrDefault(METRIC_NAME_ACK_COUNT, Collections.emptyMap())
                .values().stream().mapToLong(Double::longValue).sum();
    }

    @VisibleForTesting
    static double calculateWeightedAverage(Map<Long, Double> keyMetrics, Map<Long, Double> weightMetrics) {
        Map<Long, Double> filteredKeyMetrics = keyMetrics.entrySet().stream()
                .filter(entry -> entry.getValue() != null && DoubleUtils.notEqualsToZero(entry.getValue()))
                .collect(toMap(e -> e.getKey(), e -> e.getValue()));

        Map<Long, Double> filteredWeightMetrics = weightMetrics.entrySet().stream()
                .filter(entry -> filteredKeyMetrics.containsKey(entry.getKey()))
                .collect(toMap(e -> e.getKey(), e -> e.getValue()));

        Double sumInputRecords = filteredWeightMetrics.values().stream().mapToDouble(d -> d).sum();

        if (DoubleUtils.equalsToZero(sumInputRecords)) {
            // total weight is zero
            return 0.0d;
        }

        return filteredKeyMetrics.entrySet().stream()
                .map(entry -> {
                    Long timestamp = entry.getKey();
                    double weight = filteredWeightMetrics.getOrDefault(timestamp, 0.0) / sumInputRecords;
                    return entry.getValue() * weight;
                }).mapToDouble(d -> d).sum();
    }
}
