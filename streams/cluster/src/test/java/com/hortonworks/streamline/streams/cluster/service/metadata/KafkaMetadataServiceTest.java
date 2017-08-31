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

import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.exception.ZookeeperClientException;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokerListeners;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokersInfo;

import mockit.Deencapsulation;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

import static com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService.ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS;
import static com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService.ZK_RELATIVE_PATH_KAFKA_TOPICS;
import static com.hortonworks.streamline.streams.security.authentication.StreamlineSecurityContext.AUTHENTICATION_SCHEME_NOT_KERBEROS;

@RunWith(JMockit.class)
public class KafkaMetadataServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMetadataServiceTest.class);

    private static final String CHROOT = "/chroot";
    private static final String PATH = "/path/d1/d2";

    private static final List<String> zkStrs = Lists.newArrayList("hostname1:port1", "hostname1:port1,hostname2:port2,hostname3:port3");
    private static final List<String> chRoots = Lists.newArrayList("", CHROOT + PATH, CHROOT + PATH + "/");

    private static final List<String> expectedChrootPath = Lists.newArrayList("/", CHROOT + PATH + "/");
    private static final List<String> expectedBrokerIdPath = Lists.newArrayList("/" + ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS,
            CHROOT + PATH + "/" + ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS);

    // Mocks
    @Tested
    private KafkaMetadataService kafkaMetadataService;
    @Injectable
    private EnvironmentService environmentService;
    @Injectable
    private ZookeeperClient zkCli;
    @Injectable
    private KafkaMetadataService.KafkaZkConnection kafkaZkConnection;
    @Injectable
    private SecurityContext securityContext;
    @Injectable
    private Component kafkaBrokerComponent;
    @Injectable
    private Collection<ComponentProcess> kafkaBrokerProcesses;
    @Injectable
    private ServiceConfiguration kafkaBrokerConfig;
    @Injectable
    private ServiceConfiguration kafkaEnvConfig;
    @Mocked
    private KafkaBrokerListeners.ListenersPropParsed listenersPropParsed;
    @Mocked
    private KafkaBrokerListeners.ListenersPropEntry listenersPropEntry;


    // === Test Methods ===

    @Test
    public void test_KafkaZkConnection_wellInitialized() throws Exception {
        List<ComponentProcess> brokerProcesses = getBrokerComponentProcesses();
        Deencapsulation.setField(kafkaMetadataService, "kafkaBrokerProcesses", brokerProcesses);

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
        List<ComponentProcess> brokerProcesses = getBrokerComponentProcesses();
        Deencapsulation.setField(kafkaMetadataService, "kafkaBrokerProcesses", brokerProcesses);

        for (String zkStr : zkStrs) {
            for (String chRoot : chRoots) {
                final String zkStrRaw = zkStr + chRoot;
                LOG.debug("zookeeper.connect=" + zkStrRaw);
                KafkaMetadataService.KafkaZkConnection kafkaZkConnection = KafkaMetadataService.KafkaZkConnection.newInstance(zkStrRaw);
                final String zkPath = kafkaZkConnection.buildZkRootPath(ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS);
                Assert.assertEquals(chRoot.isEmpty() ? expectedBrokerIdPath.get(0) : expectedBrokerIdPath.get(1), zkPath);
            }
        }
    }

    private List<ComponentProcess> getBrokerComponentProcesses() {
        ComponentProcess broker1 = new ComponentProcess();
        broker1.setHost("hostname1");
        broker1.setPort(1234);
        broker1.setProtocol("PLAINTEXT");

        ComponentProcess broker2 = new ComponentProcess();
        broker2.setHost("hostname2");
        broker2.setPort(1234);
        broker2.setProtocol("PLAINTEXT");

        List<ComponentProcess> brokerProcesses = new ArrayList<>();
        brokerProcesses.add(broker1);
        brokerProcesses.add(broker2);
        return brokerProcesses;
    }

    @Test
    public void test_getBrokerInfoFromZk() throws Exception {
        Deencapsulation.setField(kafkaMetadataService, "kafkaBrokerProcesses", getBrokerComponentProcesses());

        final ArrayList<String> brokerIdZkLeaves = Lists.newArrayList("1001", "1002");
        final ArrayList<String> brokerZkData = Lists.newArrayList(
                "{\"jmx_port\":-1,\"timestamp\":\"1475798012574\",\"endpoints\":[\"PLAINTEXT://cn035.l42scl.hortonworks.com:6667\"],\"host\":\"cn035.l42scl.hortonworks.com\",\"version\":3,\"port\":6667}",
                "{\"jmx_port\":-1,\"timestamp\":\"1475798017180\",\"endpoints\":[\"PLAINTEXT://cn067.l42scl.hortonworks.com:6667\"],\"host\":\"cn067.l42scl.hortonworks.com\",\"version\":3,\"port\":6667}");

        testZkCode(ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS,
                brokerIdZkLeaves,
                this::getActualBrokerData,
                p -> Assert.assertEquals(brokerZkData, p),
                brokerZkData, true);
    }

    private List<String> getActualBrokerData() {
        try {
            final KafkaBrokersInfo<String> brokerInfo= kafkaMetadataService.getBrokerInfoFromZk();
            return brokerInfo.getBrokers().stream()
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
        } catch (ZookeeperClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_GetBrokerIdsFromZk() throws Exception {
        List<ComponentProcess> brokerProcesses = getBrokerComponentProcesses();
        Deencapsulation.setField(kafkaMetadataService, "kafkaBrokerProcesses", brokerProcesses);

        final ArrayList<String> brokerIdZkLeaves = Lists.newArrayList("1001", "1002");
        testZkCode(ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS,
                brokerIdZkLeaves,
                this::getActualBrokerIds,
                p -> Assert.assertEquals(brokerIdZkLeaves, p),
                null, true);
    }

    private List<String> getActualBrokerIds() {
        try {
            final List<KafkaBrokersInfo.BrokerId> brokers = kafkaMetadataService.getBrokerIdsFromZk().getBrokers();
            return brokers.stream()
                    .map(KafkaBrokersInfo.BrokerId::getId)
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
        } catch (ZookeeperClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_getTopicsFromZk() throws Exception {
        List<ComponentProcess> brokerProcesses = getBrokerComponentProcesses();
        Deencapsulation.setField(kafkaMetadataService, "kafkaBrokerProcesses", brokerProcesses);

        final ArrayList<String> componentZkLeaves = Lists.newArrayList("topic_1", "topic_2");
        testZkCode(ZK_RELATIVE_PATH_KAFKA_TOPICS,
                componentZkLeaves,
                this::getActualTopics,
                p -> Assert.assertEquals(componentZkLeaves, p),
                null, true);
    }

    private List<String> getActualTopics() {
        try {
            final List<String> actualTopics = kafkaMetadataService.getTopicsFromZk().list();
            Collections.sort(actualTopics);
            return actualTopics;
        } catch (ZookeeperClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startZk() throws Exception {
        final TestingServer server = new TestingServer();
        final String connectionString = server.getConnectString();
        zkCli = ZookeeperClient.newInstance(connectionString);
        zkCli.start();
    }

    public void stopZk() {
        zkCli.close();
    }

    private <T> void testZkCode(String componentZkPath, List<String> componentZkLeaves,
                                Supplier<T> executionCode, Consumer<T> verificationCode, List<String> zkNodeData,
                                boolean setSecurityExpectation) throws Exception {

        startZk();

        List<ComponentProcess> brokerProcesses = getBrokerComponentProcesses();

        // pass started zk to class under test
        // don't use mocked Collection implementation: it is problematic when using with stream
        kafkaMetadataService = new KafkaMetadataService(
                zkCli, kafkaZkConnection, securityContext, kafkaBrokerComponent, brokerProcesses, kafkaBrokerConfig, kafkaEnvConfig);

        try {
            if (zkNodeData != null) {
                Assert.assertEquals("Data array and list of zk leaf nodes must have the same size",
                        componentZkLeaves.size(), zkNodeData.size());
            }

            // Don't include last index because it adds nothing new to testing and avoids //
            for (int j = 0; j < chRoots.size() - 1; j++) {
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

                if (setSecurityExpectation) {
                    new Expectations() {{
                        // Means test run in insecure mode as they did before adding security
                        securityContext.getAuthenticationScheme();
                        result = AUTHENTICATION_SCHEME_NOT_KERBEROS;
                    }};
                }

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