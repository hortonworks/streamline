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

import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.function.SupplierException;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.exception.ServiceComponentNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Authorizer;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Keytabs;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Principals;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.StormTopologies;
import com.hortonworks.streamline.streams.security.SecurityUtil;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.SecurityContext;

public class StormMetadataService {
    private static final String STREAMS_JSON_SCHEMA_SERVICE_STORM = ServiceConfigurations.STORM.name();
    private static final String STREAMS_JSON_SCHEMA_COMPONENT_STORM_UI_SERVER = ComponentPropertyPattern.STORM_UI_SERVER.name();
    public static final String STREAMS_JSON_SCHEMA_CONFIG_STORM_ENV = ServiceConfigurations.STORM.getConfNames()[1];

    private static final String STORM_REST_API_TOPOLOGIES_DEFAULT_RELATIVE_PATH = "/api/v1/topology/summary";
    private static final String STORM_REST_API_TOPOLOGIES_KEY = "topologies";
    private static final String STORM_REST_API_TOPOLOGY_ID_KEY = "id";
    public static final String STORM_REST_API_DO_AS_USER_QUERY_PARAM = "doAsUser";

    // used to hack adding / getting Storm View
    public static final String SERVICE_STORM_VIEW = "STORM_VIEW";
    public static final String STORM_VIEW_CONFIGURATION_KEY_STORM_VIEW_URL = "storm.view.url";

    private final SecurityContext securityContext;
    private final Client httpClient;
    private final ServiceConfiguration stormEnvConfig;
    private Subject subject;
    private final String tplgySumUrl;     // http://cn067.l42scl.hortonworks.com:8744/api/v1/topology/summary
    private final String mainPageUrl;     // Views: http://172.12.128.67:8080/main/views/Storm_Monitoring/0.1.0/STORM_CLUSTER_INSTANCE
                                          // UI:    http://pm-eng1-cluster1.field.hortonworks.com:8744


    public StormMetadataService(Client httpClient, String tplgySumUrl, String mainPageUrl,
                                SecurityContext securityContext, ServiceConfiguration stormEnvConfig, Subject subject) {
        this.httpClient = httpClient;
        this.tplgySumUrl = tplgySumUrl;
        this.mainPageUrl = mainPageUrl;
        this.securityContext = securityContext;
        this.stormEnvConfig = stormEnvConfig;
        this.subject = subject;
    }

    public static class Builder {
        private EnvironmentService environmentService;
        private Long clusterId;
        private SecurityContext securityContext;
        private Subject subject;
        private String urlRelativePath = STORM_REST_API_TOPOLOGIES_DEFAULT_RELATIVE_PATH;
        private String username = "";
        private String password = "";

        public Builder(EnvironmentService environmentService, Long clusterId, SecurityContext securityContext, Subject subject) {
            this.environmentService = environmentService;
            this.clusterId = clusterId;
            this.securityContext = securityContext;
            this.subject = subject;
        }

        Builder setUrlRelativePath(String urlRelativePath) {
            this.urlRelativePath = urlRelativePath;
            return this;
        }

        Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public StormMetadataService build() throws ServiceNotFoundException, ServiceComponentNotFoundException {
            return new StormMetadataService(newHttpClient(), getTopologySummaryRestUrl(),
                    getMainPageUrl(), securityContext, getServiceConfig(STREAMS_JSON_SCHEMA_CONFIG_STORM_ENV), subject);
        }

        /**
         * @return If storm view is configured it returns its URL, otherwise it returns the URL of the Storm UI
         */
        private String getMainPageUrl() throws ServiceNotFoundException, ServiceComponentNotFoundException {
            final ServiceConfiguration stormViewConfiguration = getServiceConfig(SERVICE_STORM_VIEW);
            String url = null;
            if (stormViewConfiguration != null) {
                try {
                    Map<String, String> confMap = stormViewConfiguration.getConfigurationMap();
                    url = confMap.get(STORM_VIEW_CONFIGURATION_KEY_STORM_VIEW_URL);
                } catch (IOException e) {
                    // fail back
                }
            }

            if (url != null) {
                return url;
            } else {
                // just use Storm UI
                HostPort hostPort = getHostPort();
                return "http://" + hostPort.toString();
            }
        }

        private ServiceConfiguration getServiceConfig(String configName) throws ServiceNotFoundException {
            Service stormService = environmentService.getServiceByName(clusterId, STREAMS_JSON_SCHEMA_SERVICE_STORM);
            if (stormService == null) {
                throw new ServiceNotFoundException(clusterId, ServiceConfigurations.STORM.name());
            }

            return environmentService.getServiceConfigurationByName(stormService.getId(), configName);
        }

        private Client newHttpClient() {
            final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                    .credentials(username, password).build();
            final ClientConfig clientConfig = new ClientConfig();
            clientConfig.register(feature);
            return ClientBuilder.newClient(clientConfig);
        }

        private String getTopologySummaryRestUrl() throws ServiceNotFoundException, ServiceComponentNotFoundException {
            final HostPort hostPort = getHostPort();
            String url = "http://" + hostPort.toString() + (urlRelativePath.startsWith("/") ? urlRelativePath : "/" + urlRelativePath);
            if (SecurityUtil.isKerberosAuthenticated(securityContext)) {
                url += "?" + STORM_REST_API_DO_AS_USER_QUERY_PARAM + "=" + securityContext.getUserPrincipal().getName();
            }
            return url;
        }

        private HostPort getHostPort() throws ServiceNotFoundException, ServiceComponentNotFoundException {
            final Long serviceId = environmentService.getServiceIdByName(clusterId, STREAMS_JSON_SCHEMA_SERVICE_STORM);
            if (serviceId == null) {
                throw new ServiceNotFoundException(clusterId, ServiceConfigurations.STORM.name());
            }

            final Component stormUiComp = environmentService.getComponentByName(serviceId, STREAMS_JSON_SCHEMA_COMPONENT_STORM_UI_SERVER);

            if (stormUiComp == null) {
                throw new ServiceComponentNotFoundException(clusterId, ServiceConfigurations.STORM.name(), ComponentPropertyPattern.STORM_UI_SERVER.name());
            }

            return new HostPort(stormUiComp.getHosts().get(0), stormUiComp.getPort());
        }
    }

    /**
     * @return List of storm topologies as returned by Storm's REST API
     */
    public StormTopologies getTopologies() throws IOException, PrivilegedActionException {
        return executeSecure(() -> {
                    final Map<String, ?> jsonAsMap = JsonClientUtil.getEntity(httpClient.target(tplgySumUrl), Map.class);
                    List<String> topologies = Collections.emptyList();
                    if (jsonAsMap != null) {
                        final List<Map<String, String>> topologiesSummary = (List<Map<String, String>>) jsonAsMap.get(STORM_REST_API_TOPOLOGIES_KEY);
                        if (topologiesSummary != null) {
                            topologies = new ArrayList<>(topologiesSummary.size());
                            for (Map<String, String> tpSum : topologiesSummary) {
                                topologies.add(tpSum.get(STORM_REST_API_TOPOLOGY_ID_KEY));
                            }
                        }
                    }
                    return new StormTopologies(topologies, new Security(securityContext, new Authorizer(false), getPrincipals(), getKeytabs()));
                }
        );
    }

    private <T, E extends Exception> T executeSecure(SupplierException<T, E> action) throws PrivilegedActionException, E {
        return SecurityUtil.execute(action, securityContext, subject);
    }

    /**
     * @return The URL of main page for the Storm UI, or if storm view is configured it returns its URL
     */
    public String getMainPageUrl() {
        return mainPageUrl;
    }

    /**
     * @return the URL to retrieve the summary of deployed topologies from the storm UI
     */
    public String getTopologySummaryUrl() {
        return tplgySumUrl;
    }

    public Keytabs getKeytabs() throws IOException {
        return Keytabs.fromAmbariConfig(stormEnvConfig);
    }

    public Principals getPrincipals() throws IOException {
        return Principals.fromAmbariConfig(stormEnvConfig);
    }
}
