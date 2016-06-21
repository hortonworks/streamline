package com.hortonworks.iotas.topology.storm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.metrics.TimeSeriesQuerier;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.topology.TopologyTimeSeriesMetrics;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Storm implementation of the TopologyTimeSeriesMetrics interface
 */
public class StormTopologyTimeSeriesMetricsImpl implements TopologyTimeSeriesMetrics {
    private TimeSeriesQuerier timeSeriesQuerier;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {
        this.timeSeriesQuerier = timeSeriesQuerier;
    }

    @Override
    public TimeSeriesQuerier getTimeSeriesQuerier() {
        return timeSeriesQuerier;
    }

    @Override
    public Map<Long, Double> getCompleteLatency(Topology topology, String sourceId, long from, long to) {
        assertTimeSeriesQuerierIsSet();

        String stormTopologyName = getTopologyName(topology);

        return queryMetrics(stormTopologyName, sourceId, StormMappedMetric.completeLatency, from, to);
    }

    @Override
    public Map<String, Map<Long, Double>> getkafkaTopicOffsets(Topology topology, String sourceId, long from, long to) {
        assertTimeSeriesQuerierIsSet();

        String stormTopologyName = getTopologyName(topology);

        String topicName = findKafkaTopicName(topology, sourceId);
        if (topicName == null) {
            throw new IllegalStateException("Cannot find Kafka topic name from source config - topology name: " +
                    topology.getName() + " / source : " + sourceId);
        }

        StormMappedMetric[] metrics = { StormMappedMetric.logsize, StormMappedMetric.offset, StormMappedMetric.lag };

        Map<String, Map<Long, Double>> kafkaOffsets = new HashMap<>();
        for (StormMappedMetric metric : metrics) {
            kafkaOffsets.put(metric.name(), queryKafkaMetrics(stormTopologyName, sourceId, metric, topicName, from, to));
        }

        return kafkaOffsets;
    }

    @Override
    public Map<String, Map<Long, Double>> getComponentStats(Topology topology, String componentId, long from, long to) {
        assertTimeSeriesQuerierIsSet();

        String stormTopologyName = getTopologyName(topology);

        StormMappedMetric[] metrics = { StormMappedMetric.inputRecords, StormMappedMetric.outputRecords,
                StormMappedMetric.failedRecords, StormMappedMetric.processedTime, StormMappedMetric.recordsInWaitQueue };

        Map<String, Map<Long, Double>> componentStats = new HashMap<>();
        for (StormMappedMetric metric : metrics) {
            componentStats.put(metric.name(), queryMetrics(stormTopologyName, componentId, metric, from, to));
        }

        return componentStats;
    }

    private void assertTimeSeriesQuerierIsSet() {
        if (timeSeriesQuerier == null) {
            throw new IllegalStateException("Time series querier is not set!");
        }
    }

    private String getTopologyName(Topology topology) {
        return "iotas-" + topology.getId() + "-" + topology.getName();
    }

    private String findKafkaTopicName(Topology topology, String sourceId) {
        String kafkaTopicName = null;
        try {
            Map<String, Object> topologyConfig = mapper.readValue(topology.getConfig(), new TypeReference<Map<String, Object>>(){});
            List<Map<String, Object>> dataSources = (List<Map<String, Object>>) topologyConfig.get(TopologyLayoutConstants.JSON_KEY_DATA_SOURCES);

            for (Map<String, Object> dataSource : dataSources) {
                // UINAME and TYPE are mandatory fields for dataSource, so skip checking null
                String uiName = (String) dataSource.get(TopologyLayoutConstants.JSON_KEY_UINAME);
                String type = (String) dataSource.get(TopologyLayoutConstants.JSON_KEY_TYPE);

                if (!uiName.equals(sourceId)) {
                    continue;
                }

                if (!type.equalsIgnoreCase("KAFKA")) {
                    throw new IllegalStateException("Type of datasource should be KAFKA");
                }

                // config is a mandatory field for dataSource, so skip checking null
                Map<String, Object> dataSourceConfig = (Map<String, Object>) dataSource.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
                kafkaTopicName = (String) dataSourceConfig.get(TopologyLayoutConstants.JSON_KEY_TOPIC);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse topology configuration.");
        }

        return kafkaTopicName;
    }

    private Map<Long, Double> queryMetrics(String stormTopologyName, String sourceId, StormMappedMetric mappedMetric, long from, long to) {
        Map<Long, Double> metrics = timeSeriesQuerier.getMetrics(stormTopologyName, sourceId, mappedMetric.getStormMetricName(),
                mappedMetric.getAggregateFunction(), from, to);
        return new TreeMap<>(metrics);
    }


    private Map<Long, Double> queryKafkaMetrics(String stormTopologyName, String sourceId, StormMappedMetric mappedMetric,
                                                  String kafkaTopic, long from, long to) {
        Map<Long, Double> metrics = timeSeriesQuerier.getMetrics(stormTopologyName, sourceId, String.format(mappedMetric.getStormMetricName(), kafkaTopic),
                mappedMetric.getAggregateFunction(), from, to);
        return new TreeMap<>(metrics);
    }

}
