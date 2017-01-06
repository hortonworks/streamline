package com.hortonworks.streamline.streams.metrics.storm.topology;

import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Mock;
import mockit.MockUp;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.StreamlineComponent;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import mockit.Expectations;
import mockit.Mocked;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIClient;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    private static final String TOPIC_NAME = "topic";

    private TopologyLayout topology;
    private Component component;
    private String mockedTopologyName;

    @Before
    public void setUp() throws IOException {
        component = getComponentLayoutForTest();
        topology = getTopologyLayoutForTest();

        String generatedTopologyName = StormTopologyUtil.generateStormTopologyName(topology.getId(), topology.getName());
        mockedTopologyName = generatedTopologyName + "-old";

        StormRestAPIClient mockClient = createMockStormRestAPIClient(mockedTopologyName);

        stormTopologyTimeSeriesMetrics = new StormTopologyTimeSeriesMetricsImpl(mockClient);
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(mockTimeSeriesQuerier);
    }

    private StormRestAPIClient createMockStormRestAPIClient(final String mockTopologyName) {
        return new MockUp<StormRestAPIClient>() {
                @Mock
                public Map getTopologySummary() {
                    Map<String, Object> topologySummary = new HashMap<>();
                    List<Map<String, Object>> topologies = new ArrayList<>();
                    topologySummary.put(StormRestAPIConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGIES, topologies);
                    Map<String, Object> mockTopology = new HashMap<>();
                    mockTopology.put(StormRestAPIConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME, mockTopologyName);
                    mockTopology.put(StormRestAPIConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_ID_ENCODED, mockTopologyName + "-1234567890");
                    topologies.add(mockTopology);
                    return topologySummary;
                }
            }.getMockInstance();
    }

    @Test(expected = IllegalStateException.class)
    public void testWithoutAssigningTimeSeriesQuerier() throws IOException {
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(null);

        final long from = 1L;
        final long to = 3L;

        stormTopologyTimeSeriesMetrics.getCompleteLatency(topology, component, from, to);
        fail("It should throw Exception!");
    }

    @Test
    public void testGetCompleteLatency() throws Exception {
        final long from = 1L;
        final long to = 3L;

        final Map<Long, Double> expected = generateTestPointsMap();

        // also verification
        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(StormMappedMetric.completeLatency.getStormMetricName()),
                    withEqual(StormMappedMetric.completeLatency.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected;
        }};

        Map<Long, Double> actual = stormTopologyTimeSeriesMetrics.getCompleteLatency(topology, component, from, to);
        assertEquals(expected, actual);
    }

    @Test
    public void getKafkaTopicOffsets() throws Exception {
        final long from = 1L;
        final long to = 3L;

        final Map<String, Map<Long, Double>> expected = new HashMap<>();

        expected.put(StormMappedMetric.logsize.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.offset.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.lag.name(), generateTestPointsMap());

        // also verification
        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(String.format(StormMappedMetric.logsize.getStormMetricName(), TOPIC_NAME)),
                    withEqual(StormMappedMetric.logsize.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.logsize.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(String.format(StormMappedMetric.offset.getStormMetricName(), TOPIC_NAME)),
                    withEqual(StormMappedMetric.offset.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.offset.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(String.format(StormMappedMetric.lag.getStormMetricName(), TOPIC_NAME)),
                    withEqual(StormMappedMetric.lag.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.lag.name());
        }};

        Map<String, Map<Long, Double>> actual = stormTopologyTimeSeriesMetrics.getkafkaTopicOffsets(topology, component, from, to);
        assertEquals(expected, actual);
    }

    @Test
    public void getComponentStats() throws Exception {
        final TopologyLayout topology = getTopologyLayoutForTest();

        final long from = 1L;
        final long to = 3L;

        final Map<String, Map<Long, Double>> expected = new HashMap<>();

        expected.put(StormMappedMetric.inputRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.outputRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.failedRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.processedTime.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.recordsInWaitQueue.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.ackedRecords.name(), generateTestPointsMap());

        final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric expectedMetric =
                new TopologyTimeSeriesMetrics.TimeSeriesComponentMetric(component.getName(),
                        expected.get(StormMappedMetric.inputRecords.name()),
                        expected.get(StormMappedMetric.outputRecords.name()),
                        expected.get(StormMappedMetric.failedRecords.name()),
                        expected.get(StormMappedMetric.processedTime.name()),
                        expected.get(StormMappedMetric.recordsInWaitQueue.name()),
                        Collections.singletonMap(StormMappedMetric.ackedRecords.name(),
                                expected.get(StormMappedMetric.ackedRecords.name()))
                );

        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(StormMappedMetric.inputRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.inputRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.inputRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(StormMappedMetric.outputRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.outputRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.outputRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(StormMappedMetric.failedRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.failedRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.failedRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(StormMappedMetric.processedTime.getStormMetricName()),
                    withEqual(StormMappedMetric.processedTime.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.processedTime.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(StormMappedMetric.recordsInWaitQueue.getStormMetricName()),
                    withEqual(StormMappedMetric.recordsInWaitQueue.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.recordsInWaitQueue.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(component.getId() + "-" + component.getName()),
                    withEqual(StormMappedMetric.ackedRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.ackedRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.ackedRecords.name());
        }};

        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric actual =
                stormTopologyTimeSeriesMetrics.getComponentStats(topology, component, from, to);
        assertEquals(expectedMetric, actual);
    }

    private TopologyLayout getTopologyLayoutForTest() throws IOException {
        Map<String, Object> configurations = buildTopologyConfigWithKafkaDataSource(TOPIC_NAME);
        return new TopologyLayout(1L, "topology", mapper.writeValueAsString(configurations), null);
    }

    private Component getComponentLayoutForTest() {
        StreamlineComponent component = new StreamlineComponent() {
            @Override
            public void accept(TopologyDagVisitor visitor) {
            }
        };
        component.setId("11");
        component.setName("device");
        return component;
    }

    private Map<String, Object> buildTopologyConfigWithKafkaDataSource(String topicName) {
        Map<String, Object> configurations = new HashMap<>();

        Map<String, Object> dataSource = new HashMap<>();
        dataSource.put(TopologyLayoutConstants.JSON_KEY_UINAME, component.getName());
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