package com.hortonworks.iotas.streams.metrics.storm.topology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.iotas.streams.layout.component.TopologyLayout;
import com.hortonworks.iotas.streams.layout.TopologyLayoutConstants;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StormTopologyTimeSeriesMetricsImplTest {
    private StormTopologyTimeSeriesMetricsImpl stormTopologyTimeSeriesMetrics;

    @Mocked
    private TimeSeriesQuerier mockTimeSeriesQuerier;
    private Random random = new Random();
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        stormTopologyTimeSeriesMetrics = new StormTopologyTimeSeriesMetricsImpl();
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(mockTimeSeriesQuerier);
    }

    @Test(expected = IllegalStateException.class)
    public void testWithoutAssigningTimeSeriesQuerier() {
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(null);

        final TopologyLayout topology = new TopologyLayout(1L, "topology", null, null);
        final String sourceId = "device";

        final long from = 1L;
        final long to = 3L;

        stormTopologyTimeSeriesMetrics.getCompleteLatency(topology, sourceId, from, to);
        fail("It should throw Exception!");
    }

    @Test
    public void testGetCompleteLatency() throws Exception {
        final TopologyLayout topology = new TopologyLayout(1L, "topology", null, null);
        final String sourceId = "device";

        final long from = 1L;
        final long to = 3L;

        final Map<Long, Double> expected = generateTestPointsMap();

        // also verification
        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(StormMappedMetric.completeLatency.getStormMetricName()),
                    withEqual(StormMappedMetric.completeLatency.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected;
        }};

        Map<Long, Double> actual = stormTopologyTimeSeriesMetrics.getCompleteLatency(topology, sourceId, from, to);
        assertEquals(expected, actual);
    }

    @Test
    public void getKafkaTopicOffsets() throws Exception {
        final String sourceId = "device";
        final String topicName = "topic";

        Map<String, Object> configurations = buildTopologyConfigWithKafkaDataSource(sourceId, topicName);
        final TopologyLayout topology = new TopologyLayout(
                1L, "topology", mapper.writeValueAsString(configurations), null);

        final long from = 1L;
        final long to = 3L;

        final Map<String, Map<Long, Double>> expected = new HashMap<>();

        expected.put(StormMappedMetric.logsize.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.offset.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.lag.name(), generateTestPointsMap());

        // also verification
        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(String.format(StormMappedMetric.logsize.getStormMetricName(), topicName)),
                    withEqual(StormMappedMetric.logsize.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.logsize.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(String.format(StormMappedMetric.offset.getStormMetricName(), topicName)),
                    withEqual(StormMappedMetric.offset.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.offset.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(String.format(StormMappedMetric.lag.getStormMetricName(), topicName)),
                    withEqual(StormMappedMetric.lag.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.lag.name());
        }};

        Map<String, Map<Long, Double>> actual = stormTopologyTimeSeriesMetrics.getkafkaTopicOffsets(topology, sourceId, from, to);
        assertEquals(expected, actual);
    }

    @Test
    public void getComponentStats() throws Exception {
        final TopologyLayout topology = new TopologyLayout(1L, "topology", null, null);

        final String sourceId = "device";

        final long from = 1L;
        final long to = 3L;

        final Map<String, Map<Long, Double>> expected = new HashMap<>();

        expected.put(StormMappedMetric.inputRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.outputRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.failedRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.processedTime.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.recordsInWaitQueue.name(), generateTestPointsMap());

        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(StormMappedMetric.inputRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.inputRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.inputRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(StormMappedMetric.outputRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.outputRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.outputRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(StormMappedMetric.failedRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.failedRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.failedRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(StormMappedMetric.processedTime.getStormMetricName()),
                    withEqual(StormMappedMetric.processedTime.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.processedTime.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual("iotas-" + topology.getId() + "-" + topology.getName()),
                    withEqual(sourceId),
                    withEqual(StormMappedMetric.recordsInWaitQueue.getStormMetricName()),
                    withEqual(StormMappedMetric.recordsInWaitQueue.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.recordsInWaitQueue.name());
        }};

        Map<String, Map<Long, Double>> actual = stormTopologyTimeSeriesMetrics.getComponentStats(topology, sourceId, from, to);
        assertEquals(expected, actual);
    }

    private Map<String, Object> buildTopologyConfigWithKafkaDataSource(String sourceId, String topicName) {
        Map<String, Object> configurations = new HashMap<>();

        Map<String, Object> dataSource = new HashMap<>();
        dataSource.put(TopologyLayoutConstants.JSON_KEY_UINAME, sourceId);
        dataSource.put(TopologyLayoutConstants.JSON_KEY_TYPE, "KAFKA");

        Map<String, Object> dataSourceConfig = new HashMap<>();
        dataSourceConfig.put(TopologyLayoutConstants.JSON_KEY_TOPIC, topicName);
        dataSource.put(TopologyLayoutConstants.JSON_KEY_CONFIG, dataSourceConfig);

        configurations.put(TopologyLayoutConstants.JSON_KEY_DATA_SOURCES, Collections.singletonList(dataSource));
        return configurations;
    }

    private Map<Long, Double> generateTestPointsMap() {
        Map<Long, Double> ret = new HashMap<>();
        int count = random.nextInt(5);
        for (int i = 0 ; i < count ; i++) {
            ret.put(random.nextLong(), random.nextDouble());
        }

        return ret;
    }
}