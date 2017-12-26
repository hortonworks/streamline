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

import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.Source;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIClient;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ForkJoinPool;

import static java.util.stream.Collectors.toMap;

/**
 * Storm implementation of the TopologyTimeSeriesMetrics interface
 */
public class StormTopologyTimeSeriesMetricsImpl implements TopologyTimeSeriesMetrics {
    private static final int FORK_JOIN_POOL_PARALLELISM = 30;

    // shared across the metrics instances
    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);

    private final StormRestAPIClient client;
    private TimeSeriesQuerier timeSeriesQuerier;
    private static final StormMappedMetric[] STATS_METRICS = new StormMappedMetric[]{
            StormMappedMetric.inputRecords, StormMappedMetric.outputRecords, StormMappedMetric.ackedRecords,
            StormMappedMetric.failedRecords, StormMappedMetric.processedTime, StormMappedMetric.recordsInWaitQueue,
            StormMappedMetric.executeTime
    };
    private static final StormMappedMetric[] STATS_METRICS_SOURCE = new StormMappedMetric[]{
            StormMappedMetric.inputRecords, StormMappedMetric.outputRecords, StormMappedMetric.ackedRecords,
            StormMappedMetric.failedRecords, StormMappedMetric.processedTime, StormMappedMetric.recordsInWaitQueue,
            StormMappedMetric.completeLatency
    };

    public StormTopologyTimeSeriesMetricsImpl(StormRestAPIClient client) {
        this.client = client;
    }

    @Override
    public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {
        this.timeSeriesQuerier = timeSeriesQuerier;
    }

    @Override
    public TimeSeriesQuerier getTimeSeriesQuerier() {
        return timeSeriesQuerier;
    }

    @Override
    public Map<Long, Double> getCompleteLatency(TopologyLayout topology, Component component, long from, long to, String asUser) {
        String stormTopologyName = StormTopologyUtil.findOrGenerateTopologyName(client, topology.getId(), topology.getName(), asUser);
        String stormComponentName = getComponentName(component);

        return queryComponentMetrics(stormTopologyName, stormComponentName, StormMappedMetric.completeLatency, from, to);
    }

    @Override
    public Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, Component component, long from, long to, String asUser) {
        String stormTopologyName = StormTopologyUtil.findOrGenerateTopologyName(client, topology.getId(), topology.getName(), asUser);
        String stormComponentName = getComponentName(component);

        String topicName = findKafkaTopicName(topology, component);
        if (topicName == null) {
            throw new IllegalStateException("Cannot find Kafka topic name from source config - topology name: " +
                    topology.getName() + " / source : " + component.getName());
        }

        StormMappedMetric[] metrics = { StormMappedMetric.logsize, StormMappedMetric.offset, StormMappedMetric.lag };

        Map<String, Map<Long, Double>> kafkaOffsets = new HashMap<>();
        for (StormMappedMetric metric : metrics) {
            kafkaOffsets.put(metric.name(), queryKafkaMetrics(stormTopologyName, stormComponentName, metric, topicName, from, to));
        }

        return kafkaOffsets;
    }

    @Override
    public TimeSeriesComponentMetric getTopologyStats(TopologyLayout topology, long from, long to, String asUser) {
        String stormTopologyName = StormTopologyUtil.findOrGenerateTopologyName(client, topology.getId(), topology.getName(), asUser);

        Map<String, Map<Long, Double>> stats = ParallelStreamUtil.execute(() ->
                        Arrays.asList(STATS_METRICS_SOURCE)
                                .parallelStream()
                                .collect(toMap(m -> m.name(), m -> queryTopologyMetrics(stormTopologyName, m, from, to))),
                FORK_JOIN_POOL);

        return buildTimeSeriesComponentMetric(topology.getName(), stats);
    }

    @Override
    public TimeSeriesComponentMetric getComponentStats(TopologyLayout topology, Component component, long from, long to, String asUser) {
        String stormTopologyName = StormTopologyUtil.findOrGenerateTopologyName(client, topology.getId(), topology.getName(), asUser);
        String stormComponentName = getComponentName(component);

        StormMappedMetric[] stats;
        if (component instanceof Source) {
            stats = STATS_METRICS_SOURCE;
        } else {
            stats = STATS_METRICS;
        }

        // empty map if time-series DB is not set to the namespace
        Map<String, Map<Long, Double>> componentStats = ParallelStreamUtil.execute(() ->
                        Arrays.asList(stats)
                                .parallelStream()
                                .collect(toMap(m -> m.name(),
                                        m -> queryComponentMetrics(stormTopologyName, stormComponentName, m, from, to))),
                FORK_JOIN_POOL);

        return buildTimeSeriesComponentMetric(component.getName(), componentStats);
    }

    private TimeSeriesComponentMetric buildTimeSeriesComponentMetric(String name, Map<String, Map<Long, Double>> stats) {
        Map<String, Map<Long, Double>> misc = new HashMap<>();
        misc.put(StormMappedMetric.ackedRecords.name(), stats.get(StormMappedMetric.ackedRecords.name()));
        if (stats.containsKey(StormMappedMetric.completeLatency.name())) {
            misc.put(StormMappedMetric.completeLatency.name(), stats.get(StormMappedMetric.completeLatency.name()));
        }
        if (stats.containsKey(StormMappedMetric.executeTime.name())) {
            misc.put(StormMappedMetric.executeTime.name(), stats.get(StormMappedMetric.executeTime.name()));
        }

        TimeSeriesComponentMetric metric = new TimeSeriesComponentMetric(name,
                stats.getOrDefault(StormMappedMetric.inputRecords.name(), Collections.emptyMap()),
                stats.getOrDefault(StormMappedMetric.outputRecords.name(), Collections.emptyMap()),
                stats.getOrDefault(StormMappedMetric.failedRecords.name(), Collections.emptyMap()),
                stats.getOrDefault(StormMappedMetric.processedTime.name(), Collections.emptyMap()),
                stats.getOrDefault(StormMappedMetric.recordsInWaitQueue.name(), Collections.emptyMap()),
                misc);

        return metric;
    }

    private String getComponentName(Component component) {
        return component.getId() + "-" + component.getName();
    }

    private String findKafkaTopicName(TopologyLayout topology, Component component) {
        String kafkaTopicName = null;
        try {
            Map<String, Object> topologyConfig = topology.getConfig().getProperties();
            List<Map<String, Object>> dataSources = (List<Map<String, Object>>) topologyConfig.get(TopologyLayoutConstants.JSON_KEY_DATA_SOURCES);

            for (Map<String, Object> dataSource : dataSources) {
                // UINAME and TYPE are mandatory fields for dataSource, so skip checking null
                String uiName = (String) dataSource.get(TopologyLayoutConstants.JSON_KEY_UINAME);
                String type = (String) dataSource.get(TopologyLayoutConstants.JSON_KEY_TYPE);

                if (!uiName.equals(component.getName())) {
                    continue;
                }

                if (!type.equalsIgnoreCase("KAFKA")) {
                    throw new IllegalStateException("Type of datasource should be KAFKA");
                }

                // config is a mandatory field for dataSource, so skip checking null
                Map<String, Object> dataSourceConfig = (Map<String, Object>) dataSource.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
                kafkaTopicName = (String) dataSourceConfig.get(TopologyLayoutConstants.JSON_KEY_TOPIC);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to parse topology configuration.", e);
        }

        return kafkaTopicName;
    }

    private Map<Long, Double> queryTopologyMetrics(String stormTopologyName, StormMappedMetric mappedMetric, long from, long to) {
        Map<Long, Double> metrics = Collections.emptyMap();
        // empty if time-series querier is not set
        if (timeSeriesQuerier != null) {
            metrics = timeSeriesQuerier.getTopologyLevelMetrics(stormTopologyName,
                    mappedMetric.getStormMetricName(), mappedMetric.getAggregateFunction(), from, to);
        }
        return new TreeMap<>(metrics);
    }

    private Map<Long, Double> queryComponentMetrics(String stormTopologyName, String sourceId, StormMappedMetric mappedMetric, long from, long to) {
        Map<Long, Double> metrics = Collections.emptyMap();
        // empty if time-series querier is not set
        if (timeSeriesQuerier != null) {
            metrics = timeSeriesQuerier.getMetrics(stormTopologyName, sourceId, mappedMetric.getStormMetricName(),
                    mappedMetric.getAggregateFunction(), from, to);
        }
        return new TreeMap<>(metrics);
    }

    private Map<Long, Double> queryKafkaMetrics(String stormTopologyName, String sourceId, StormMappedMetric mappedMetric,
                                                  String kafkaTopic, long from, long to) {
        Map<Long, Double> metrics = Collections.emptyMap();
        // empty if time-series querier is not set
        if (timeSeriesQuerier != null) {
            metrics = timeSeriesQuerier.getMetrics(stormTopologyName, sourceId, String.format(mappedMetric.getStormMetricName(), kafkaTopic),
                    mappedMetric.getAggregateFunction(), from, to);
        }
        return new TreeMap<>(metrics);
    }

}
