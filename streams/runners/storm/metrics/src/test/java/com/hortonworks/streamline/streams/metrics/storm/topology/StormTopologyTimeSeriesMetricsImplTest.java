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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import mockit.Mock;
import mockit.MockUp;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.Component;
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
    private Component source;
    private Component processor;
    private Component sink;
    private String mockedTopologyName;

    @Before
    public void setUp() throws IOException {
        source = getSourceLayoutForTest();
        processor = getProcessorLayoutForTest();
        sink = getSinkLayoutForTest();
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
                public Map getTopologySummary(String asUser) {
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

    @Test
    public void testGetCompleteLatency() throws Exception {
        final long from = 1L;
        final long to = 3L;

        final Map<Long, Double> expected = generateTestPointsMap();

        // also verification
        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(source.getId() + "-" + source.getName()),
                    withEqual(StormMappedMetric.completeLatency.getStormMetricName()),
                    withEqual(StormMappedMetric.completeLatency.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected;
        }};

        Map<Long, Double> actual = stormTopologyTimeSeriesMetrics.getCompleteLatency(topology, source, from, to, null);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetCompleteLatencyWithoutAssigningTimeSeriesQuerier() throws IOException {
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(null);

        final long from = 1L;
        final long to = 3L;

        Map<Long, Double> completeLatency = stormTopologyTimeSeriesMetrics.getCompleteLatency(topology, source, from, to, null);
        assertEquals(Collections.emptyMap(), completeLatency);
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
                    withEqual(source.getId() + "-" + source.getName()),
                    withEqual(String.format(StormMappedMetric.logsize.getStormMetricName(), TOPIC_NAME)),
                    withEqual(StormMappedMetric.logsize.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.logsize.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(source.getId() + "-" + source.getName()),
                    withEqual(String.format(StormMappedMetric.offset.getStormMetricName(), TOPIC_NAME)),
                    withEqual(StormMappedMetric.offset.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.offset.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(source.getId() + "-" + source.getName()),
                    withEqual(String.format(StormMappedMetric.lag.getStormMetricName(), TOPIC_NAME)),
                    withEqual(StormMappedMetric.lag.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.lag.name());
        }};

        Map<String, Map<Long, Double>> actual = stormTopologyTimeSeriesMetrics.getkafkaTopicOffsets(topology, source, from, to, null);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetKafkaTopicOffsetsWithoutAssigningTimeSeriesQuerier() throws IOException {
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(null);

        final long from = 1L;
        final long to = 3L;

        Map<String, Map<Long, Double>> kafkaOffsets = stormTopologyTimeSeriesMetrics.getkafkaTopicOffsets(topology, source, from, to, null);
        assertEquals(3, kafkaOffsets.size());
        assertEquals(Collections.emptyMap(), kafkaOffsets.get("lag"));
        assertEquals(Collections.emptyMap(), kafkaOffsets.get("offset"));
        assertEquals(Collections.emptyMap(), kafkaOffsets.get("logsize"));
    }

    @Test
    public void getSourceComponentStats() throws Exception {
        final TopologyLayout topology = getTopologyLayoutForTest();

        final long from = 1L;
        final long to = 3L;

        final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric expectedMetric =
                setupExpectionToSpoutComponent(source, from, to);

        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric actual =
                stormTopologyTimeSeriesMetrics.getComponentStats(topology, source, from, to, null);
        assertEquals(expectedMetric, actual);
    }

    @Test
    public void getProcessorComponentStats() throws Exception {
        final TopologyLayout topology = getTopologyLayoutForTest();

        final long from = 1L;
        final long to = 3L;

        final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric expectedMetric =
                setupExpectionToBoltComponent(processor, from, to);

        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric actual =
                stormTopologyTimeSeriesMetrics.getComponentStats(topology, processor, from, to, null);
        assertEquals(expectedMetric, actual);
    }

    @Test
    public void getSinkComponentStats() throws Exception {
        final TopologyLayout topology = getTopologyLayoutForTest();

        final long from = 1L;
        final long to = 3L;

        final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric expectedMetric =
                setupExpectionToBoltComponent(sink, from, to);

        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric actual =
                stormTopologyTimeSeriesMetrics.getComponentStats(topology, sink, from, to, null);
        assertEquals(expectedMetric, actual);
    }


    @Test
    public void testGetSourceComponentStatsWithoutAssigningTimeSeriesQuerier() throws Exception {
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(null);

        final TopologyLayout topology = getTopologyLayoutForTest();

        final long from = 1L;
        final long to = 3L;

        final Map<String, Map<Long, Double>> expectedMisc = new HashMap<>();
        expectedMisc.put(StormMappedMetric.ackedRecords.name(), Collections.emptyMap());
        expectedMisc.put(StormMappedMetric.completeLatency.name(), Collections.emptyMap());

        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric actual =
                stormTopologyTimeSeriesMetrics.getComponentStats(topology, source, from, to, null);
        assertEquals(Collections.emptyMap(), actual.getInputRecords());
        assertEquals(Collections.emptyMap(), actual.getOutputRecords());
        assertEquals(Collections.emptyMap(), actual.getFailedRecords());
        assertEquals(Collections.emptyMap(), actual.getRecordsInWaitQueue());
        assertEquals(Collections.emptyMap(), actual.getProcessedTime());
        assertEquals(expectedMisc, actual.getMisc());
    }

    @Test
    public void testGetProcessorComponentStatsWithoutAssigningTimeSeriesQuerier() throws Exception {
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(null);

        final TopologyLayout topology = getTopologyLayoutForTest();

        final long from = 1L;
        final long to = 3L;

        final Map<String, Map<Long, Double>> expectedMisc = new HashMap<>();
        expectedMisc.put(StormMappedMetric.ackedRecords.name(), Collections.emptyMap());
        expectedMisc.put(StormMappedMetric.executeTime.name(), Collections.emptyMap());

        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric actual =
                stormTopologyTimeSeriesMetrics.getComponentStats(topology, processor, from, to, null);
        assertEquals(Collections.emptyMap(), actual.getInputRecords());
        assertEquals(Collections.emptyMap(), actual.getOutputRecords());
        assertEquals(Collections.emptyMap(), actual.getFailedRecords());
        assertEquals(Collections.emptyMap(), actual.getRecordsInWaitQueue());
        assertEquals(Collections.emptyMap(), actual.getProcessedTime());
        assertEquals(expectedMisc, actual.getMisc());
    }

    @Test
    public void testGetSinkComponentStatsWithoutAssigningTimeSeriesQuerier() throws Exception {
        stormTopologyTimeSeriesMetrics.setTimeSeriesQuerier(null);

        final TopologyLayout topology = getTopologyLayoutForTest();

        final long from = 1L;
        final long to = 3L;

        final Map<String, Map<Long, Double>> expectedMisc = new HashMap<>();
        expectedMisc.put(StormMappedMetric.ackedRecords.name(), Collections.emptyMap());
        expectedMisc.put(StormMappedMetric.executeTime.name(), Collections.emptyMap());

        TopologyTimeSeriesMetrics.TimeSeriesComponentMetric actual =
                stormTopologyTimeSeriesMetrics.getComponentStats(topology, sink, from, to, null);
        assertEquals(Collections.emptyMap(), actual.getInputRecords());
        assertEquals(Collections.emptyMap(), actual.getOutputRecords());
        assertEquals(Collections.emptyMap(), actual.getFailedRecords());
        assertEquals(Collections.emptyMap(), actual.getRecordsInWaitQueue());
        assertEquals(Collections.emptyMap(), actual.getProcessedTime());
        assertEquals(expectedMisc, actual.getMisc());
    }

    private TopologyLayout getTopologyLayoutForTest() throws IOException {
        Map<String, Object> configurations = buildTopologyConfigWithKafkaDataSource(TOPIC_NAME);
        return new TopologyLayout(1L, "topology", mapper.writeValueAsString(configurations), null);
    }

    private Component getSourceLayoutForTest() {
        StreamlineSource source = new StreamlineSource() {
            @Override
            public void accept(TopologyDagVisitor visitor) {
            }
        };
        source.setId("11");
        source.setName("device");
        return source;
    }

    private Component getProcessorLayoutForTest() {
        StreamlineProcessor processor = new StreamlineProcessor() {
            @Override
            public void accept(TopologyDagVisitor visitor) {
            }
        };
        processor.setId("12");
        processor.setName("rule");
        return processor;
    }

    private Component getSinkLayoutForTest() {
        StreamlineSink sink = new StreamlineSink() {
            @Override
            public void accept(TopologyDagVisitor visitor) {
            }
        };
        sink.setId("13");
        sink.setName("hdfs");
        return sink;
    }

    private Map<String, Object> buildTopologyConfigWithKafkaDataSource(String topicName) {
        Map<String, Object> configurations = new HashMap<>();

        Map<String, Object> dataSource = new HashMap<>();
        dataSource.put(TopologyLayoutConstants.JSON_KEY_UINAME, source.getName());
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

    private TopologyTimeSeriesMetrics.TimeSeriesComponentMetric setupExpectionToSpoutComponent(Component component, long from, long to) {
        final Map<String, Map<Long, Double>> expected = new HashMap<>();

        expected.put(StormMappedMetric.inputRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.outputRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.failedRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.processedTime.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.recordsInWaitQueue.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.ackedRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.completeLatency.name(), generateTestPointsMap());

        Map<String, Map<Long, Double>> miscExpected = new HashMap<>();
        miscExpected.put(StormMappedMetric.ackedRecords.name(), expected.get(StormMappedMetric.ackedRecords.name()));
        miscExpected.put(StormMappedMetric.completeLatency.name(), expected.get(StormMappedMetric.completeLatency.name()));

        final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric expectedMetric =
                new TopologyTimeSeriesMetrics.TimeSeriesComponentMetric(component.getName(),
                        expected.get(StormMappedMetric.inputRecords.name()),
                        expected.get(StormMappedMetric.outputRecords.name()),
                        expected.get(StormMappedMetric.failedRecords.name()),
                        expected.get(StormMappedMetric.processedTime.name()),
                        expected.get(StormMappedMetric.recordsInWaitQueue.name()),
                        miscExpected
                );

        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.inputRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.inputRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.inputRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.outputRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.outputRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.outputRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.failedRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.failedRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.failedRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.processedTime.getStormMetricName()),
                    withEqual(StormMappedMetric.processedTime.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.processedTime.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.recordsInWaitQueue.getStormMetricName()),
                    withEqual(StormMappedMetric.recordsInWaitQueue.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.recordsInWaitQueue.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.ackedRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.ackedRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.ackedRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.completeLatency.getStormMetricName()),
                    withEqual(StormMappedMetric.completeLatency.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.completeLatency.name());
        }};
        return expectedMetric;
    }

    private TopologyTimeSeriesMetrics.TimeSeriesComponentMetric setupExpectionToBoltComponent(Component component, long from, long to) {
        final Map<String, Map<Long, Double>> expected = new HashMap<>();

        expected.put(StormMappedMetric.inputRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.outputRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.failedRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.processedTime.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.recordsInWaitQueue.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.ackedRecords.name(), generateTestPointsMap());
        expected.put(StormMappedMetric.executeTime.name(), generateTestPointsMap());

        Map<String, Map<Long, Double>> miscExpected = new HashMap<>();
        miscExpected.put(StormMappedMetric.ackedRecords.name(), expected.get(StormMappedMetric.ackedRecords.name()));
        miscExpected.put(StormMappedMetric.executeTime.name(), expected.get(StormMappedMetric.executeTime.name()));

        final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric expectedMetric =
                new TopologyTimeSeriesMetrics.TimeSeriesComponentMetric(component.getName(),
                        expected.get(StormMappedMetric.inputRecords.name()),
                        expected.get(StormMappedMetric.outputRecords.name()),
                        expected.get(StormMappedMetric.failedRecords.name()),
                        expected.get(StormMappedMetric.processedTime.name()),
                        expected.get(StormMappedMetric.recordsInWaitQueue.name()),
                        miscExpected
                );

        new Expectations() {{
            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.inputRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.inputRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.inputRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.outputRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.outputRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.outputRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.failedRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.failedRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.failedRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.processedTime.getStormMetricName()),
                    withEqual(StormMappedMetric.processedTime.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.processedTime.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.recordsInWaitQueue.getStormMetricName()),
                    withEqual(StormMappedMetric.recordsInWaitQueue.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.recordsInWaitQueue.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.ackedRecords.getStormMetricName()),
                    withEqual(StormMappedMetric.ackedRecords.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.ackedRecords.name());

            mockTimeSeriesQuerier.getMetrics(
                    withEqual(mockedTopologyName),
                    withEqual(StormTopologyUtil.generateStormComponentId(Long.valueOf(component.getId()), component.getName())),
                    withEqual(StormMappedMetric.executeTime.getStormMetricName()),
                    withEqual(StormMappedMetric.executeTime.getAggregateFunction()),
                    withEqual(from), withEqual(to)
            );

            result = expected.get(StormMappedMetric.executeTime.name());
        }};
        return expectedMetric;
    }

}