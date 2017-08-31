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
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.security.authentication.StreamlineSecurityContext;

import mockit.Deencapsulation;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.SecurityContext;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;

import static com.hortonworks.streamline.streams.security.authentication.StreamlineSecurityContext.AUTHENTICATION_SCHEME_NOT_KERBEROS;

@RunWith(JMockit.class)
public class StormMetadataServiceTest {
    private static final String STORM_TOPOLOGIES_SUMMARY_JSON = "metadata/topology-summary.json";
    private static final String STORM_UI_URL_EXPECTED =  "http://localhost:8080/api/v1/topology/summary";

    @Tested
    StormMetadataService stormService;

    @Injectable
    Client client;
    @Injectable
    String url;
    @Injectable
    String mainPageUrl;
    @Injectable
    SecurityContext securityContext;
    @Injectable
    ServiceConfiguration stormEnvConfig;
    @Injectable
    Subject subject;
    @Injectable
    Component nimbus;
    @Injectable
    Collection<ComponentProcess> nimbusProcesses;
    @Injectable
    Component stormUi;
    @Injectable
    Collection<ComponentProcess> stormUiProcesses;

    @Injectable
    Invocation.Builder builder;
    @Injectable
    EnvironmentService environmentService;


    @Test
    public void getTopologies() throws Exception {
        new Expectations() {{
            builder.get(String.class); result = getTopologiesSummary();
            // Means test run in insecure mode as they did before adding security
            securityContext.getAuthenticationScheme(); result = AUTHENTICATION_SCHEME_NOT_KERBEROS;
        }};

        Deencapsulation.setField(stormService, "nimbusProcesses", buildNimbusComponentProcesses());
        Deencapsulation.setField(stormService, "stormUiProcesses", buildUiComponentProcesses());

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
    public void buildUrl_insecureMode_noQueryParam() throws Exception {
        List<ComponentProcess> uiProcesses = buildUiComponentProcesses();

        new Expectations() {{
            securityContext.getAuthenticationScheme(); result = AUTHENTICATION_SCHEME_NOT_KERBEROS;
            stormUi.getId(); result = 2L;
            environmentService.listComponentProcesses(2L); result = uiProcesses;
        }};

        final StormMetadataService stormMetadataService = new StormMetadataService
                .Builder(environmentService, 1L, securityContext, subject).build();
        final String tpSumUrl = stormMetadataService.getTopologySummaryUrl();

        Assert.assertTrue(tpSumUrl.equals(STORM_UI_URL_EXPECTED));
    }

    private List<ComponentProcess> buildNimbusComponentProcesses() {
        ComponentProcess nimbus1 = new ComponentProcess();
        nimbus1.setHost("localhost");
        nimbus1.setPort(8080);

        ComponentProcess nimbus2 = new ComponentProcess();
        nimbus2.setHost("localhost");
        nimbus2.setPort(8080);

        return Lists.newArrayList(nimbus1, nimbus2);
    }

    private List<ComponentProcess> buildUiComponentProcesses() {
        ComponentProcess ui = new ComponentProcess();
        ui.setHost("localhost");
        ui.setPort(8080);

        return Collections.singletonList(ui);
    }

    @Test
    public void buildUrl_insecureMode_doAsUserQueryParam() throws Exception {
        final String principalUser = "user";

        List<ComponentProcess> componentProcesses = buildUiComponentProcesses();

        new Expectations() {{
            securityContext.getAuthenticationScheme(); result = StreamlineSecurityContext.KERBEROS_AUTH;
            securityContext.getUserPrincipal().getName(); result = principalUser;
            environmentService.listComponentProcesses(stormUi.getId()); result = componentProcesses;
        }};

        final StormMetadataService stormMetadataService = new StormMetadataService
                .Builder(environmentService, 1L, securityContext, subject).build();
        final String tpSumUrl = stormMetadataService.getTopologySummaryUrl();

        Assert.assertTrue(tpSumUrl.equals(STORM_UI_URL_EXPECTED +"?" + StormMetadataService.STORM_REST_API_DO_AS_USER_QUERY_PARAM
                + "=" + principalUser));
    }
}