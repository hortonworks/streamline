package org.apache.streamline.streams.catalog.service.metadata;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.streamline.streams.catalog.service.metadata.StormMetadataService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class StormMetadataServiceTest {
    private static final String STORM_TOPOLOGIES_SUMMARY_JSON = "metadata/topology-summary.json";

    @Tested
    StormMetadataService stormService;

    @Injectable
    String url;
    @Injectable
    Client client;
    @Injectable
    Invocation.Builder builder;

    @Test
    public void getTopologies() throws Exception {
        new Expectations() {{
            builder.get(Map.class); result = getTopologiesSummary();
        }};

        final List<String> actualTopologies = stormService.getTopologies().asList();
        Collections.sort(actualTopologies);

        final List<String> expectedTopologies = Lists.newArrayList("kafka-topology-2-1474413185",
                "kafka-topology-2-3-1474413375");

        Assert.assertEquals(expectedTopologies, actualTopologies);
    }

    private Map<String, ?> getTopologiesSummary() throws IOException {
        return new ObjectMapper().readValue(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(STORM_TOPOLOGIES_SUMMARY_JSON),
                new TypeReference<Map<String, ?>>() { });
    }
}