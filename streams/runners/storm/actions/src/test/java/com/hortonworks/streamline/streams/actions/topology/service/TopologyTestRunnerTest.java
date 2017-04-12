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
package com.hortonworks.streamline.streams.actions.topology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.utils.TopologyTestHelper;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCase;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCaseSink;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCaseSource;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Injectable;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class TopologyTestRunnerTest {

    private static String topologyTestRunResultDir;

    @Injectable
    private StreamCatalogService catalogService;

    @Injectable
    private TopologyActions topologyActions;

    private TopologyTestRunner topologyTestRunner;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeClass
    public static void beforeClass() throws Exception {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
        topologyTestRunResultDir = tempDir.getAbsolutePath();
    }

    @Before
    public void setUp() throws Exception {
        topologyTestRunner = new TopologyTestRunner(catalogService, topologyTestRunResultDir);
    }

    @Test
    public void runTest_withTestCaseId() throws Exception {
        Topology topology = createSimpleDAGInjectedTestTopology();

        Long topologyId = topology.getId();
        Long testCaseId = 1L;
        Map<String, Object> testRunInputMap = new HashMap<>();
        testRunInputMap.put("testCaseId", testCaseId);

        TopologyTestRunCase testCase = new TopologyTestRunCase();
        testCase.setId(testCaseId);
        testCase.setTopologyId(topology.getId());
        testCase.setName("testcase1");
        testCase.setTimestamp(System.currentTimeMillis());

        setTopologyTestRunCaseExpectations(topology, testCase);
        setTopologyTestRunCaseSinkNotFoundExpectations(topology, testCase);
        setTopologyTestRunHistoryExpectations();
        setSucceedTopologyActionsExpectations();

        long sourceCount = topology.getTopologyDag().getOutputComponents().stream()
                .filter(c -> c instanceof StreamlineSource)
                .count();

        long sinkCount = topology.getTopologyDag().getInputComponents().stream()
                .filter(c -> c instanceof StreamlineSink)
                .count();

        TopologyTestRunHistory resultHistory = topologyTestRunner.runTest(topologyActions, topology, "",
                objectMapper.writeValueAsString(testRunInputMap));

        assertNotNull(resultHistory);
        assertTrue(resultHistory.getFinished());
        assertTrue(resultHistory.getSuccess());

        new VerificationsInOrder() {{
            catalogService.getTopologyTestRunCase(topologyId, testCaseId);
            times = 1;

            catalogService.getTopologyTestRunCaseSourceBySourceId(testCaseId, anyLong);
            times = (int) sourceCount;

            catalogService.getTopologyTestRunCaseSinkBySinkId(testCaseId, anyLong);
            times = (int) sinkCount;

            TopologyTestRunHistory runHistory;
            // some fields are already modified after calling the method, so don't need to capture it
            catalogService.addTopologyTestRunHistory(withInstanceOf(TopologyTestRunHistory.class));
            times = 1;
            catalogService.addOrUpdateTopologyTestRunHistory(anyLong, runHistory = withCapture());
            times = 1;

            assertEquals(topology.getId(), runHistory.getTopologyId());
            assertTestRecords(topology, runHistory.getTestRecords());
            assertTrue(runHistory.getFinished());
            assertTrue(runHistory.getSuccess());
            assertNotNull(runHistory.getStartTime());
            assertNotNull(runHistory.getFinishTime());
            assertTrue(runHistory.getFinishTime() - runHistory.getStartTime() >= 0);
            assertTrue(isEmptyJson(runHistory.getExpectedOutputRecords()));
            assertTrue(isNotEmptyJson(runHistory.getActualOutputRecords()));
            assertFalse(runHistory.getMatched());
        }};
    }

    @Test(expected = IllegalArgumentException.class)
    public void runTest_withNotExistingTestCaseId() throws Exception {
        Topology topology = createSimpleDAGInjectedTestTopology();

        Long topologyId = topology.getId();
        Long testCaseId = 1L;
        Map<String, Object> testRunInputMap = new HashMap<>();
        testRunInputMap.put("testCaseId", testCaseId);

        new Expectations() {{
            catalogService.getTopologyTestRunCase(topologyId, testCaseId);
            result = null;
        }};

        topologyTestRunner.runTest(topologyActions, topology, "",
                objectMapper.writeValueAsString(testRunInputMap));
    }

    @Test
    public void runTest_topologyActionsTestRunFails() throws Exception {
        Topology topology = createSimpleDAGInjectedTestTopology();

        Long topologyId = topology.getId();
        Long testCaseId = 1L;
        Map<String, Object> testRunInputMap = new HashMap<>();
        testRunInputMap.put("testCaseId", testCaseId);

        TopologyTestRunCase testCase = new TopologyTestRunCase();
        testCase.setId(testCaseId);
        testCase.setTopologyId(topology.getId());
        testCase.setName("testcase1");
        testCase.setTimestamp(System.currentTimeMillis());

        setTopologyTestRunCaseExpectations(topology, testCase);
        setTopologyTestRunCaseSinkNotFoundExpectations(topology, testCase);
        setTopologyTestRunHistoryExpectations();
        setTopologyActionsThrowingExceptionExpectations();

        long sourceCount = topology.getTopologyDag().getOutputComponents().stream()
                .filter(c -> c instanceof StreamlineSource)
                .count();

        long sinkCount = topology.getTopologyDag().getInputComponents().stream()
                .filter(c -> c instanceof StreamlineSink)
                .count();

        TopologyTestRunHistory resultHistory = topologyTestRunner.runTest(topologyActions, topology, "",
                objectMapper.writeValueAsString(testRunInputMap));

        assertNotNull(resultHistory);

        new VerificationsInOrder() {{
            catalogService.getTopologyTestRunCase(topologyId, testCaseId);
            times = 1;

            catalogService.getTopologyTestRunCaseSourceBySourceId(testCaseId, anyLong);
            times = (int) sourceCount;

            catalogService.getTopologyTestRunCaseSinkBySinkId(testCaseId, anyLong);
            times = (int) sinkCount;

            TopologyTestRunHistory runHistory;
            // some fields are already modified after calling the method, so don't need to capture it
            catalogService.addTopologyTestRunHistory(withInstanceOf(TopologyTestRunHistory.class));
            times = 1;
            catalogService.addOrUpdateTopologyTestRunHistory(anyLong, runHistory = withCapture());
            times = 1;

            assertEquals(topology.getId(), runHistory.getTopologyId());
            assertTestRecords(topology, runHistory.getTestRecords());
            assertTrue(runHistory.getFinished());
            assertFalse(runHistory.getSuccess());
            assertNotNull(runHistory.getStartTime());
            assertNotNull(runHistory.getFinishTime());
            assertTrue(runHistory.getFinishTime() - runHistory.getStartTime() >= 0);
            assertTrue(isEmptyJson(runHistory.getExpectedOutputRecords()));
            assertNull(runHistory.getActualOutputRecords());
            assertFalse(runHistory.getMatched());
        }};
    }

    @Test
    public void runTest_withMatchedExpectedOutputRecords() throws Exception {
        Topology topology = createSimpleDAGInjectedTestTopology();

        Long testCaseId = 1L;
        Map<String, Object> testRunInputMap = new HashMap<>();
        testRunInputMap.put("testCaseId", testCaseId);

        TopologyTestRunCase testCase = new TopologyTestRunCase();
        testCase.setId(testCaseId);
        testCase.setTopologyId(topology.getId());
        testCase.setName("testcase1");
        testCase.setTimestamp(System.currentTimeMillis());

        Set<String> sinkNames = topology.getTopologyDag().getInputComponents()
                .stream()
                .filter(c -> c instanceof StreamlineSink)
                .map(c -> c.getName())
                .collect(Collectors.toSet());

        setTopologyTestRunCaseExpectations(topology, testCase);
        setTopologyTestRunCaseSinkExpectations(topology, testCase);
        setTopologyTestRunHistoryExpectations();
        setSucceedTopologyActionsExpectations();

        TopologyTestRunHistory resultHistory = topologyTestRunner.runTest(topologyActions, topology, "",
                objectMapper.writeValueAsString(testRunInputMap));

        assertNotNull(resultHistory);

        new VerificationsInOrder() {{
            TopologyTestRunHistory runHistory;
            // some fields are already modified after calling the method, so don't need to capture it
            catalogService.addTopologyTestRunHistory(withInstanceOf(TopologyTestRunHistory.class));
            times = 1;
            catalogService.addOrUpdateTopologyTestRunHistory(anyLong, runHistory = withCapture());
            times = 1;

            assertEquals(topology.getId(), runHistory.getTopologyId());
            assertTestRecords(topology, runHistory.getTestRecords());
            assertTrue(runHistory.getFinished());
            assertTrue(runHistory.getSuccess());
            assertNotNull(runHistory.getStartTime());
            assertNotNull(runHistory.getFinishTime());
            assertTrue(runHistory.getFinishTime() - runHistory.getStartTime() >= 0);
            assertTrue(isNotEmptyJson(runHistory.getExpectedOutputRecords()));
            assertTrue(isNotEmptyJson(runHistory.getActualOutputRecords()));
            assertTrue(runHistory.getMatched());
        }};
    }

    @Test
    public void runTest_withMismatchedExpectedOutputRecords() throws Exception {
        Topology topology = createSimpleDAGInjectedTestTopology();

        Long testCaseId = 1L;
        Map<String, Object> testRunInputMap = new HashMap<>();
        testRunInputMap.put("testCaseId", testCaseId);

        TopologyTestRunCase testCase = new TopologyTestRunCase();
        testCase.setId(testCaseId);
        testCase.setTopologyId(topology.getId());
        testCase.setName("testcase1");
        testCase.setTimestamp(System.currentTimeMillis());

        setTopologyTestRunCaseExpectations(topology, testCase);
        setTopologyTestRunCaseSinkMismatchedRecordsExpectations(topology, testCase);
        setTopologyTestRunHistoryExpectations();
        setSucceedTopologyActionsExpectations();

        TopologyTestRunHistory resultHistory = topologyTestRunner.runTest(topologyActions, topology, "",
                objectMapper.writeValueAsString(testRunInputMap));

        assertNotNull(resultHistory);

        new VerificationsInOrder() {{
            TopologyTestRunHistory runHistory;
            // some fields are already modified after calling the method, so don't need to capture it
            catalogService.addTopologyTestRunHistory(withInstanceOf(TopologyTestRunHistory.class));
            times = 1;
            catalogService.addOrUpdateTopologyTestRunHistory(anyLong, runHistory = withCapture());
            times = 1;

            assertEquals(topology.getId(), runHistory.getTopologyId());
            assertTestRecords(topology, runHistory.getTestRecords());
            assertTrue(runHistory.getFinished());
            assertTrue(runHistory.getSuccess());
            assertNotNull(runHistory.getStartTime());
            assertNotNull(runHistory.getFinishTime());
            assertTrue(runHistory.getFinishTime() - runHistory.getStartTime() >= 0);
            assertTrue(isNotEmptyJson(runHistory.getExpectedOutputRecords()));
            assertTrue(isNotEmptyJson(runHistory.getActualOutputRecords()));
            assertFalse(runHistory.getMatched());
        }};
    }

    private void setTopologyTestRunHistoryExpectations() throws Exception {
        new Expectations() {{
            catalogService.addTopologyTestRunHistory(withInstanceOf(TopologyTestRunHistory.class));
            result = new Delegate<TopologyTestRunHistory>() {
                TopologyTestRunHistory delegate(TopologyTestRunHistory history) {
                    history.setId(1L);
                    history.setTimestamp(System.currentTimeMillis());
                    return history;
                }
            };

            catalogService.addOrUpdateTopologyTestRunHistory(anyLong, withInstanceOf(TopologyTestRunHistory.class));
            result = new Delegate<TopologyTestRunHistory>() {
                TopologyTestRunHistory delegate(Long id, TopologyTestRunHistory history) {
                    history.setId(id);
                    history.setTimestamp(System.currentTimeMillis());
                    return history;
                }
            };
        }};
    }

    private void setSucceedTopologyActionsExpectations() throws Exception {
        new Expectations() {{
            topologyActions.testRun(withInstanceOf(TopologyLayout.class), anyString, withInstanceOf(Map.class),
                    withInstanceOf(Map.class));
            result = new Delegate<Object>() {
                Object delegate(TopologyLayout topology, String mavenArtifacts,
                                Map<String, TestRunSource> testRunSourcesForEachSource,
                                Map<String, TestRunSink> testRunSinksForEachSink) throws Exception {
                    Map<String, List<Map<String, Object>>> testOutputRecords =
                            TopologyTestHelper.createTestOutputRecords(testRunSinksForEachSink.keySet());

                    testRunSinksForEachSink.entrySet()
                            .forEach(entry -> {
                                String sinkName = entry.getKey();
                                TestRunSink sink = entry.getValue();
                                try (FileWriter fw = new FileWriter(sink.getOutputFilePath())) {
                                    List<Map<String, Object>> outputRecords = testOutputRecords.get(sinkName);
                                    for (Map<String, Object> record : outputRecords) {
                                        fw.write(objectMapper.writeValueAsString(record) + "\n");
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                    return null;
                }
            };
        }};
    }

    private void setTopologyActionsThrowingExceptionExpectations() throws Exception {
        new Expectations() {{
            topologyActions.testRun(withInstanceOf(TopologyLayout.class), anyString, withInstanceOf(Map.class),
                    withInstanceOf(Map.class));
            result = new RuntimeException("Topology test run failed!");
        }};
    }

    private Topology createSimpleDAGInjectedTestTopology() {
        Long topologyId = 1L;
        Long topologyVersionId = 1L;
        Long namespaceId = 1L;

        Topology topology = TopologyTestHelper.createTopology(topologyId, topologyVersionId, namespaceId);
        injectTestTopology(topology);
        return topology;
    }

    private void setTopologyTestRunCaseExpectations(Topology topology, TopologyTestRunCase testCase) {
        new Expectations() {{
            catalogService.getTopologyTestRunCase(topology.getId(), testCase.getId());
            result = testCase;

            topology.getTopologyDag().getOutputComponents().stream()
                    .filter(c -> c instanceof StreamlineSource)
                    .forEach(source -> {
                        catalogService.getTopologyTestRunCaseSourceBySourceId(testCase.getId(), Long.valueOf(source.getId()));
                        result = createTopologyTestRunCaseSource(testCase, (StreamlineSource) source);
                    });
        }};
    }

    private void setTopologyTestRunCaseSinkExpectations(Topology topology, TopologyTestRunCase testCase) {
        new Expectations() {{
            topology.getTopologyDag().getInputComponents().stream()
                    .filter(c -> c instanceof StreamlineSink)
                    .forEach(sink -> {
                        catalogService.getTopologyTestRunCaseSinkBySinkId(testCase.getId(), Long.valueOf(sink.getId()));
                        result = createTopologyTestRunCaseSink(testCase, (StreamlineSink) sink);
                    });
        }};
    }

    private void setTopologyTestRunCaseSinkMismatchedRecordsExpectations(Topology topology, TopologyTestRunCase testCase) {
        new Expectations() {{
            topology.getTopologyDag().getInputComponents().stream()
                    .filter(c -> c instanceof StreamlineSink)
                    .forEach(sink -> {
                        catalogService.getTopologyTestRunCaseSinkBySinkId(testCase.getId(), Long.valueOf(sink.getId()));
                        result = createTopologyTestRunCaseSinkWithMismatchedRecords(testCase, (StreamlineSink) sink);
                    });
        }};
    }

    private void setTopologyTestRunCaseSinkNotFoundExpectations(Topology topology, TopologyTestRunCase testCase) {
        new Expectations() {{
            topology.getTopologyDag().getInputComponents().stream()
                    .filter(c -> c instanceof StreamlineSink)
                    .forEach(sink -> {
                        catalogService.getTopologyTestRunCaseSinkBySinkId(testCase.getId(), Long.valueOf(sink.getId()));
                        result = null;
                    });
        }};
    }

    private TopologyTestRunCaseSource createTopologyTestRunCaseSource(TopologyTestRunCase testRunCase,
                                                                      StreamlineSource source) {

        Map<String, List<Map<String, Object>>> testRecords =
                Collections.singletonMap("default", TopologyTestHelper.createTestRecords());

        TopologyTestRunCaseSource testRunSource = new TopologyTestRunCaseSource();
        testRunSource.setId(Long.valueOf(source.getId()));
        testRunSource.setTestCaseId(testRunCase.getId());
        testRunSource.setSourceId(Long.valueOf(source.getId()));
        try {
            testRunSource.setRecords(objectMapper.writeValueAsString(testRecords));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can't serialize test records map into JSON");
        }

        testRunSource.setTimestamp(System.currentTimeMillis());

        return testRunSource;
    }

    private TopologyTestRunCaseSink createTopologyTestRunCaseSink(TopologyTestRunCase testRunCase, StreamlineSink sink) {
        List<Map<String, Object>> expectedRecords = TopologyTestHelper.createTestRecords();

        TopologyTestRunCaseSink testRunSink = new TopologyTestRunCaseSink();
        testRunSink.setId(Long.valueOf(sink.getId()));
        testRunSink.setTestCaseId(testRunCase.getId());
        testRunSink.setSinkId(Long.valueOf(sink.getId()));
        try {
            testRunSink.setRecords(objectMapper.writeValueAsString(expectedRecords));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can't serialize expected records map into JSON");
        }

        testRunSink.setTimestamp(System.currentTimeMillis());

        return testRunSink;
    }

    private TopologyTestRunCaseSink createTopologyTestRunCaseSinkWithMismatchedRecords(TopologyTestRunCase testRunCase,
                                                                                       StreamlineSink sink) {
        List<Map<String, Object>> expectedRecords = TopologyTestHelper.createTestRecords();
        expectedRecords.add(Collections.singletonMap("hello", "world"));

        TopologyTestRunCaseSink testRunSink = new TopologyTestRunCaseSink();
        testRunSink.setId(Long.valueOf(sink.getId()));
        testRunSink.setTestCaseId(testRunCase.getId());
        testRunSink.setSinkId(Long.valueOf(sink.getId()));
        try {
            testRunSink.setRecords(objectMapper.writeValueAsString(expectedRecords));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can't serialize expected records map into JSON");
        }

        testRunSink.setTimestamp(System.currentTimeMillis());

        return testRunSink;
    }

    private void injectTestTopology(Topology topology) {
        StreamlineSource originSource = TopologyTestHelper.createStreamlineSource("1");
        StreamlineProcessor originProcessor = TopologyTestHelper.createStreamlineProcessor("2");
        StreamlineSink originSink = TopologyTestHelper.createStreamlineSink("3");

        TopologyDag topologyDag = new TopologyDag();
        topologyDag.add(originSource);
        topologyDag.add(originProcessor);
        topologyDag.add(originSink);
        topologyDag.addEdge(new Edge("e1", originSource, originProcessor, "default", Stream.Grouping.SHUFFLE));
        topologyDag.addEdge(new Edge("e2", originProcessor, originSink, "default", Stream.Grouping.SHUFFLE));

        topology.setTopologyDag(topologyDag);
    }

    private Map<Long, Map<String, List<Map<String, Object>>>> readTestRecordsFromTestCaseSources(Topology topology) {
        return topology.getTopologyDag().getOutputComponents().stream()
                .filter(c -> c instanceof StreamlineSource)
                .collect(toMap(c -> Long.valueOf(c.getId()),
                        c -> Collections.singletonMap("default", TopologyTestHelper.createTestRecords()))
                );
    }

    private void assertTestRecords(Topology topology, String testRecords) {
        Map<Long, Map<String, List<Map<String, Object>>>> collectedTestRecords = readTestRecordsFromTestCaseSources(topology);

        try {
            Map<Long, Map<String, List<Map<String, Object>>>> testRecordsMap = objectMapper.readValue(
                    testRecords, new TypeReference<Map<Long, Map<String, List<Map<String, Object>>>>>() {});

            assertEquals(collectedTestRecords, testRecordsMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEmptyJson(String expectedOutputRecords) {
        try {
            Map<?, ?> recordsMap = (Map<?, ?>) objectMapper.readValue(expectedOutputRecords, Map.class);
            return recordsMap.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isNotEmptyJson(String expectedOutputRecords) {
        return !isEmptyJson(expectedOutputRecords);
    }
}