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
public class NotificationSinkBundleSourceHintProviderTest {
    private NotificationSinkBundleHintProvider provider = new NotificationSinkBundleHintProvider();

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
        service.setName(Constants.Email.SERVICE_NAME);

        Map<String, String> confMap = new HashMap<>();
        confMap.put(Constants.Email.PROPERTY_KEY_HOST, "svr1");
        confMap.put(Constants.Email.PROPERTY_KEY_PORT, "1111");
        confMap.put(Constants.Email.PROPERTY_KEY_SSL, "true");
        confMap.put(Constants.Email.PROPERTY_KEY_STARTTLS, "true");
        confMap.put(Constants.Email.PROPERTY_KEY_PROTOCOL, "smtp");
        confMap.put(Constants.Email.PROPERTY_KEY_AUTH, "true");

        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setId(1L);
        serviceConfiguration.setName(Constants.Email.CONF_TYPE_PROPERTIES);
        serviceConfiguration.setConfiguration(objectMapper.writeValueAsString(confMap));

        new Expectations() {{
            environmentService.getServiceByName(cluster.getId(), Constants.Email.SERVICE_NAME);
            result = service;

            environmentService.getServiceConfigurationByName(service.getId(), Constants.Email.CONF_TYPE_PROPERTIES);
            result = serviceConfiguration;
        }};

        provider.init(environmentService);

        Map<String, Object> hints = provider.getHintsOnCluster(cluster);
        Assert.assertNotNull(hints);
        Assert.assertEquals("svr1", hints.get(NotificationSinkBundleHintProvider.FIELD_NAME_HOST));
        Assert.assertEquals("1111", hints.get(NotificationSinkBundleHintProvider.FIELD_NAME_PORT));
        Assert.assertEquals("true", hints.get(NotificationSinkBundleHintProvider.FIELD_NAME_SSL));
        Assert.assertEquals("true", hints.get(NotificationSinkBundleHintProvider.FIELD_NAME_STARTTLS));
        Assert.assertEquals("smtp", hints.get(NotificationSinkBundleHintProvider.FIELD_NAME_PROTOCOL));
        Assert.assertEquals("true", hints.get(NotificationSinkBundleHintProvider.FIELD_NAME_AUTH));
    }
}