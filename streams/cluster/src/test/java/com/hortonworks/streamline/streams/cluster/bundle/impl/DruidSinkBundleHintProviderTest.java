package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(JMockit.class)
public class DruidSinkBundleHintProviderTest {
    private DruidSinkBundleHintProvider provider = new DruidSinkBundleHintProvider();

    @Mocked
    private EnvironmentService environmentService;

    @Test
    public void getHintsOnCluster() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");

        Service service = new Service();
        service.setId(1L);
        service.setName(Constants.Druid.SERVICE_NAME);

        Map<String, String> confMap = new HashMap<>();
        confMap.put(Constants.Druid.PROPERTY_KEY_ZK_SERVICE_HOSTS, "svr1:2181,svr2:2181");
        confMap.put(Constants.Druid.PROPERTY_KEY_INDEXING_SERVICE_NAME, "druid/overlord");
        confMap.put(Constants.Druid.PROPERTY_KEY_DISCOVERY_CURATOR_PATH, "/discovery");

        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setId(1L);
        serviceConfiguration.setName(Constants.Druid.CONF_TYPE_COMMON_RUNTIME);
        serviceConfiguration.setConfiguration(objectMapper.writeValueAsString(confMap));

        new Expectations() {{
            environmentService.getServiceByName(cluster.getId(), Constants.Druid.SERVICE_NAME);
            result = service;

            environmentService.getServiceConfigurationByName(service.getId(), Constants.Druid.CONF_TYPE_COMMON_RUNTIME);
            result = serviceConfiguration;
        }};

        provider.init(environmentService);

        Map<String, Object> hints = provider.getHintsOnCluster(cluster, null, null);
        Assert.assertNotNull(hints);
        Assert.assertEquals("svr1:2181,svr2:2181", hints.get(DruidSinkBundleHintProvider.FIELD_NAME_ZK_CONNECT));
        Assert.assertEquals("druid/overlord", hints.get(DruidSinkBundleHintProvider.FIELD_NAME_INDEX_SERVICE));
        Assert.assertEquals("/discovery", hints.get(DruidSinkBundleHintProvider.FIELD_NAME_DISCOVERY_PATH));
    }

}
