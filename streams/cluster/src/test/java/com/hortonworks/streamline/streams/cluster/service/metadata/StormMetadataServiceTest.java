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

import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.SecurityContext;

import mockit.Expectations;
import mockit.Injectable;
import mockit.MockUp;
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
    String mainPageUrl;
    @Injectable
    Invocation.Builder builder;
    @Injectable
    EnvironmentService environmentService;
    @Injectable
    SecurityContext securityContext;


    @Test
    public void getTopologies() throws Exception {
        new Expectations() {{
            builder.get(String.class); result = getTopologiesSummary();
        }};

        final List<String> actualTopologies = stormService.getTopologies().list();
        Collections.sort(actualTopologies);

        final List<String> expectedTopologies = Lists.newArrayList("kafka-topology-2-1474413185",
                "kafka-topology-2-3-1474413375");

        Assert.assertEquals(expectedTopologies, actualTopologies);
    }

    private String getTopologiesSummary() throws IOException {
        return IOUtils.toString(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(STORM_TOPOLOGIES_SUMMARY_JSON), Charset.forName("UTF-8"));
    }


    @Test
    public void buildUrl_secureMode_noQueryParam() throws Exception {
        new Expectations() {{
            securityContext.getUserPrincipal(); result = "user@REALM";
            securityContext.isSecure(); result = false;
        }};

        StormMetadataService stormMetadataService = new StormMetadataService.Builder(environmentService, 1L, securityContext).build();
        String tpSumUrl = stormMetadataService.getTopologySummaryUrl();

//        Assert.assertTrue(tpSumUrl.concat());

        //TODO WIP


    }

    class MyBuilder extends MockUp<StormMetadataService.Builder> {

    }

    @Test
    public void buildUrl_insecureMode_doAsUserQueryParam() {
        //TODO WIP
    }
}