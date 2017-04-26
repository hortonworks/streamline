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
package com.hortonworks.streamline.streams.cluster.service.metadata;

import com.google.common.collect.Lists;

import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.exception.ZookeeperClientException;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;

import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.core.SecurityContext;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;

import static com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService.KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH;
import static com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService.KAFKA_TOPICS_ZK_RELATIVE_PATH;

@RunWith(JMockit.class)
public class KafkaMetadataServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMetadataServiceTest.class);

    private static final String CHROOT = "/chroot";
    private static final String PATH = "/path/d1/d2";

    private static final List<String> zkStrs = Lists.newArrayList("hostname1:port1", "hostname1:port1,hostname2:port2,hostname3:port3");
    private static final List<String> chRoots = Lists.newArrayList("", CHROOT + PATH, CHROOT + PATH + "/");

    private static final List<String> expectedChrootPath = Lists.newArrayList("/", CHROOT + PATH + "/");
    private static final List<String> expectedBrokerIdPath = Lists.newArrayList("/" + KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH,
            CHROOT + PATH + "/" + KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH);

    // Mocks
    @Tested
    private KafkaMetadataService kafkaMetadataService;
    @Injectable
    private EnvironmentService environmentService;
    @Injectable
    private ZookeeperClient zkCli;
    @Injectable
    private KafkaMetadataService.KafkaZkConnection kafkaZkConnection;
    @Mocked
    private SecurityContext securityContext;

    // === Test Methods ===

    @Test
    public void test_KafkaZkConnection_wellInitialized() throws Exception {
        for (String zkStr : zkStrs) {
            for (String chRoot : chRoots) {
                final String zkStrRaw = zkStr + chRoot;
                LOG.debug("zookeeper.connect=" + zkStrRaw);
                KafkaMetadataService.KafkaZkConnection kafkaZkConnection = KafkaMetadataService.KafkaZkConnection.newInstance(zkStrRaw);
                Assert.assertEquals(zkStr, kafkaZkConnection.getZkString());
                Assert.assertEquals(chRoot.isEmpty() ? expectedChrootPath.get(0) : expectedChrootPath.get(1), kafkaZkConnection.getChRoot());
            }
        }
    }

    @Test
    public void test_KafkaZkConnection_createPath() throws Exception {
        for (String zkStr : zkStrs) {
            for (String chRoot : chRoots) {
                final String zkStrRaw = zkStr + chRoot;
                LOG.debug("zookeeper.connect=" + zkStrRaw);
                KafkaMetadataService.KafkaZkConnection kafkaZkConnection = KafkaMetadataService.KafkaZkConnection.newInstance(zkStrRaw);
                final String zkPath = kafkaZkConnection.buildZkRootPath(KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH);
                Assert.assertEquals(chRoot.isEmpty() ? expectedBrokerIdPath.get(0) : expectedBrokerIdPath.get(1), zkPath);
            }
        }
    }

    @Test
    public void test_getBrokerHostPortFromStreamsJson(@Injectable final Component component) throws Exception {
        final List<String> expectedHosts = Lists.newArrayList("hostname1", "hostname2");
        final Integer expectedPort = 1234;

        new Expectations() {{
            component.getHosts(); result = expectedHosts;
            component.getPort(); result = expectedPort;
            environmentService.getComponentByName(anyLong, anyString); result = component;
        }};

        final KafkaMetadataService.BrokersInfo<HostPort> brokerHostPort = kafkaMetadataService.getBrokerHostPortFromStreamsJson(1L);
        // verify host
        Assert.assertEquals(expectedHosts.get(0), brokerHostPort.getInfo().get(0).getHost());
        Assert.assertEquals(expectedHosts.get(1), brokerHostPort.getInfo().get(1).getHost());
        // verify port
        Assert.assertEquals(expectedPort, brokerHostPort.getInfo().get(0).getPort());
        Assert.assertEquals(expectedPort, brokerHostPort.getInfo().get(1).getPort());
    }

    @Test
    public void test_getBrokerInfoFromZk() throws Exception {
        final ArrayList<String> brokerIdZkLeaves = Lists.newArrayList("1001", "1002");
        final ArrayList<String> brokerZkData = Lists.newArrayList(
                "{\"jmx_port\":-1,\"timestamp\":\"1475798012574\",\"endpoints\":[\"PLAINTEXT://cn035.l42scl.hortonworks.com:6667\"],\"host\":\"cn035.l42scl.hortonworks.com\",\"version\":3,\"port\":6667}",
                "{\"jmx_port\":-1,\"timestamp\":\"1475798017180\",\"endpoints\":[\"PLAINTEXT://cn067.l42scl.hortonworks.com:6667\"],\"host\":\"cn067.l42scl.hortonworks.com\",\"version\":3,\"port\":6667}");

        testZkCode(KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH,
                brokerIdZkLeaves,
                this::getActualBrokerData,
                p -> Assert.assertEquals(brokerZkData, p),
                brokerZkData);
    }

    private List<String> getActualBrokerData() {
        try {
            final KafkaMetadataService.BrokersInfo<String> brokerInfo= kafkaMetadataService.getBrokerInfoFromZk();
            return brokerInfo.getInfo().stream()
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
        } catch (ZookeeperClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_GetBrokerIdsFromZk() throws Exception {
        final ArrayList<String> brokerIdZkLeaves = Lists.newArrayList("1001", "1002");
        testZkCode(KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH,
                brokerIdZkLeaves,
                this::getActualBrokerIds,
                p -> Assert.assertEquals(brokerIdZkLeaves, p),
                null);
    }

    private List<String> getActualBrokerIds() {
        try {
            final List<KafkaMetadataService.BrokersInfo.BrokerId> brokers = kafkaMetadataService.getBrokerIdsFromZk().getInfo();
            return brokers.stream()
                    .map(KafkaMetadataService.BrokersInfo.BrokerId::getId)
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
        } catch (ZookeeperClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_getTopicsFromZk() throws Exception {
        final ArrayList<String> componentZkLeaves = Lists.newArrayList("topic_1", "topic_2");
        testZkCode(KAFKA_TOPICS_ZK_RELATIVE_PATH,
                componentZkLeaves,
                this::getActualTopics,
                p -> Assert.assertEquals(componentZkLeaves, p),
                null);
    }

    private List<String> getActualTopics() {
        try {
            final List<String> actualTopics = kafkaMetadataService.getTopicsFromZk().getTopics();
            Collections.sort(actualTopics);
            return actualTopics;
        } catch (ZookeeperClientException e) {
            throw new RuntimeException(e);
        }
    }

    public void startZk() throws Exception {
        final TestingServer server = new TestingServer();
        final String connectionString = server.getConnectString();
        zkCli = ZookeeperClient.newInstance(connectionString);
        zkCli.start();
//        kafkaMetadataService = new KafkaMetadataService(environmentService, zkCli, kafkaZkConnection);
    }

    public void stopZk() {
        zkCli.close();
    }

    private <T> void testZkCode(String componentZkPath, List<String> componentZkLeaves,
            Supplier<T> executionCode, Consumer<T> verificationCode, List<String> zkNodeData) throws Exception {

        startZk();

        // pass started zk to class under test
        kafkaMetadataService = new KafkaMetadataService(environmentService, zkCli, kafkaZkConnection, securityContext);

        try {
            if (zkNodeData != null) {
                Assert.assertEquals("Data array and list of zk leaf nodes must have the same size", componentZkLeaves.size(), zkNodeData.size());
            }

            for (int j = 0; j < chRoots.size() - 1; j++) {  // Don't include last index because it adds nothing new to testing and avoids //
                final String zkRootPath = chRoots.get(j) + "/" + componentZkPath;
                for (int i = 0; i < componentZkLeaves.size(); i++) {
                    final String zkFullPath = zkRootPath + "/" + componentZkLeaves.get(i);
                    zkCli.createPath(zkFullPath);
                    LOG.debug("Created zk path [{}]", zkFullPath);

                    if (zkNodeData != null) {
                        zkCli.setData(zkFullPath, zkNodeData.get(i).getBytes());
                        LOG.debug("Set data [{} => {}]", zkFullPath, zkNodeData.get(i));
                    }
                }

                // Record expectations
                new Expectations() {{
                    kafkaZkConnection.buildZkRootPath(anyString);
                    result = zkRootPath;
                }};

                // Executes the code to test and returns the actual result
                T actual = executionCode.get();

                /*
                Verifies that the result of the code execution (passed in as parameter) matches the actual result.
                This method should define the expected result and implement the assertions
                */
                verificationCode.accept(actual);
            }
        } finally {
            stopZk();
        }
    }
}