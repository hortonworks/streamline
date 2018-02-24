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

import com.hortonworks.streamline.common.util.DoubleUtils;
import com.hortonworks.streamline.streams.metrics.storm.topology.StormMappedMetric;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ComponentMetricSummaryTest {

    @Test
    public void testAggregateEmitted() {
        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metric = getTestMetric();

        long actual = ComponentMetricSummary.aggregateEmitted(metric);

        // 20 + 40
        long expected = 60;

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAggregatedAcked() {
        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metric = getTestMetric();

        long actual = ComponentMetricSummary.aggregateAcked(metric);

        // 15 + 25
        long expected = 40;

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAggregateFailed() {
        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metric = getTestMetric();

        long actual = ComponentMetricSummary.aggregateFailed(metric);

        // 1 + 2
        long expected = 3;

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAggregateProcessLatency() {
        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metric = getTestMetric();

        double actual = ComponentMetricSummary.aggregateProcessLatency(metric);

        // note that weighted average will be applied
        // 1.234 * 10.0 / (10.0 + 20.0) + 4.567 * 20.0 / (10.0 + 20.0)
        double expected = 3.456d;

        Assert.assertEquals(expected, actual, DoubleUtils.EPSILON);
    }

    @Test
    public void testAggregateExecuteLatency() {
        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metric = getTestMetric();

        double actual = ComponentMetricSummary.aggregateExecuteLatency(metric);

        // note that weighted average will be applied
        // 0.123 * 10.0 / (10.0 + 20.0) + 0.456 * 20.0 / (10.0 + 20.0)
        double expected = 0.345d;

        Assert.assertEquals(expected, actual, DoubleUtils.EPSILON);
    }

    @Test
    public void testAggregateCompleteLatency() {
        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metric = getTestMetric();

        double actual = ComponentMetricSummary.aggregateCompleteLatency(metric);

        // note that weighted average will be applied
        // 123.456 * 15.0 / (15.0 + 25.0) + 456.789 * 25.0 / (15.0 + 25.0)
        double expected = 331.789125d;

        Assert.assertEquals(expected, actual, DoubleUtils.EPSILON);
    }

    @Test
    public void testCalculateWeightedAverage() {
        Map<Long, Double> keyMetrics = new HashMap<>();
        keyMetrics.put(1L, 10.0);
        keyMetrics.put(2L, 20.0);

        Map<Long, Double> weightMetrics = new HashMap<>();
        weightMetrics.put(1L, 10.0);
        weightMetrics.put(2L, 5.0);

        double actual = ComponentMetricSummary.calculateWeightedAverage(keyMetrics, weightMetrics);

        // 10.0 * 10.0 / (10.0 + 5.0) + 20.0 * 5.0 / (10.0 + 5.0)
        double expected = 13.333333333d;

        Assert.assertEquals(expected, actual, DoubleUtils.EPSILON);
    }

    @Test
    public void testCalculateWeightedAverageLacksWeightInformation() {
        Map<Long, Double> keyMetrics = new HashMap<>();
        keyMetrics.put(1L, 10.0);
        keyMetrics.put(2L, 20.0);

        // no weight for both 1L and 2L
        double actual = ComponentMetricSummary.calculateWeightedAverage(keyMetrics, Collections.emptyMap());
        Assert.assertEquals(0.0, actual, DoubleUtils.EPSILON);

        Map<Long, Double> weightMetrics = new HashMap<>();
        // no weight for 1L
        weightMetrics.put(2L, 5.0);

        actual = ComponentMetricSummary.calculateWeightedAverage(keyMetrics, weightMetrics);

        // only weight and value for 2L is considered
        Assert.assertEquals(20.0, actual, DoubleUtils.EPSILON);
    }

    private TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTestMetric() {
        Map<Long, Double> inputRecords = new HashMap<>();
        inputRecords.put(1L, 10.0d);
        inputRecords.put(2L, 20.0d);

        Map<Long, Double> outputRecords = new HashMap<>();
        outputRecords.put(1L, 20.0d);
        outputRecords.put(2L, 40.0d);

        Map<Long, Double> failedRecords = new HashMap<>();
        failedRecords.put(1L, 1.0d);
        failedRecords.put(2L, 2.0d);

        Map<Long, Double> processedTime = new HashMap<>();
        processedTime.put(1L, 1.234d);
        processedTime.put(2L, 4.567d);

        Map<Long, Double> recordsInWaitQueue = new HashMap<>();
        recordsInWaitQueue.put(1L, 1d);
        recordsInWaitQueue.put(2L, 2d);

        Map<String, Map<Long, Double>> misc = new HashMap<>();

        Map<Long, Double> executeTime = new HashMap<>();
        executeTime.put(1L, 0.123d);
        executeTime.put(2L, 0.456d);

        Map<Long, Double> completeLatency = new HashMap<>();
        completeLatency.put(1L, 123.456d);
        completeLatency.put(2L, 456.789d);

        Map<Long, Double> ackedRecords = new HashMap<>();
        ackedRecords.put(1L, 15.0d);
        ackedRecords.put(2L, 25.0d);

        misc.put(StormMappedMetric.executeTime.name(), executeTime);
        misc.put(StormMappedMetric.completeLatency.name(), completeLatency);
        misc.put("ackedRecords", ackedRecords);

        return new TopologyTimeSeriesMetrics.TimeSeriesComponentMetric(
                "testComponent", inputRecords, outputRecords, failedRecords, processedTime,
                recordsInWaitQueue, misc
        );
    }

}