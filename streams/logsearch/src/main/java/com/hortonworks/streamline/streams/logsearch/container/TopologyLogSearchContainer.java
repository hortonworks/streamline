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
package com.hortonworks.streamline.streams.logsearch.container;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.cluster.catalog.*;
import com.hortonworks.streamline.streams.cluster.container.NamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.logsearch.DefaultTopologyLogSearch;
import com.hortonworks.streamline.streams.logsearch.TopologyLogSearch;
import com.hortonworks.streamline.streams.logsearch.container.mapping.MappedTopologyLogSearchImpl;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TopologyLogSearchContainer extends NamespaceAwareContainer<TopologyLogSearch> {
    public static final String COMPONENT_NAME_INFRA_SOLR = ComponentPropertyPattern.INFRA_SOLR.name();
    public static final String SERVICE_CONFIGURATION_NAME_INFRA_SOLR = ServiceConfigurations.AMBARI_INFRA.getConfNames()[0];
    public static final String SOLR_API_URL_KEY = "solrApiUrl";
    public static final String SECURED_CLUSTER_KEY = "secured";
    public static final String AMBARI_INFRA_SOLR_KERBEROS_PRINCIPAL = "infra_solr_kerberos_principal";

    private final Subject subject;

    public TopologyLogSearchContainer(EnvironmentService environmentService, Subject subject) {
        super(environmentService);
        this.subject = subject;
    }

    @Override
    protected TopologyLogSearch initializeInstance(Namespace namespace) {
        MappedTopologyLogSearchImpl topologyLogSearchImpl;

        String className;
        Map<String, Object> confLogSearch;

        String streamingEngine = namespace.getStreamingEngine();
        String logSearchService = namespace.getLogSearchService();
        if (logSearchService != null && !logSearchService.isEmpty()) {
            String key = MappedTopologyLogSearchImpl.getName(streamingEngine, logSearchService);
            try {
                topologyLogSearchImpl = MappedTopologyLogSearchImpl.valueOf(key);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Unsupported log search service: " + logSearchService, e);
            }

            confLogSearch = buildAmbariInfraLogSearchConfigMap(namespace, logSearchService, subject);
            className = topologyLogSearchImpl.getClassName();
        } else {
            confLogSearch = Collections.emptyMap();
            className = DefaultTopologyLogSearch.class.getName();
        }

        return initTopologyLogSearch(confLogSearch, className);
    }

    private TopologyLogSearch initTopologyLogSearch(Map<String, Object> conf, String className) {
        try {
            TopologyLogSearch topologyLogSearch = instantiate(className);
            topologyLogSearch.init(conf);
            return topologyLogSearch;
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ConfigException e) {
            throw new RuntimeException("Can't initialize Topology log search instance - Class Name: " + className, e);
        }
    }

    private Map<String, Object> buildAmbariInfraLogSearchConfigMap(Namespace namespace, String logSearchServiceName,
                                                                   Subject subject) {
        // Assuming that a namespace has one mapping of log search service
        Service logSearchService = getFirstOccurenceServiceForNamespace(namespace, logSearchServiceName);
        if (logSearchService == null) {
            throw new RuntimeException("Log search service " + logSearchServiceName + " is not associated to the namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }

        Component infraSolr = getComponent(logSearchService, COMPONENT_NAME_INFRA_SOLR)
                .orElseThrow(() -> new RuntimeException(logSearchService + " doesn't have " + COMPONENT_NAME_INFRA_SOLR + " as component"));

        Collection<ComponentProcess> solrProcesses = environmentService.listComponentProcesses(infraSolr.getId());
        if (solrProcesses.isEmpty()) {
            throw new RuntimeException(logSearchService + " doesn't have any process for " + COMPONENT_NAME_INFRA_SOLR + " as component");
        }

        ComponentProcess solrProcess = solrProcesses.iterator().next();
        String solrHost = solrProcess.getHost();
        Integer solrPort = solrProcess.getPort();

        assertHostAndPort(COMPONENT_NAME_INFRA_SOLR, solrHost, solrPort);

        ServiceConfiguration infraSolrConf = getServiceConfiguration(logSearchService, SERVICE_CONFIGURATION_NAME_INFRA_SOLR)
                .orElseThrow(() -> new RuntimeException(logSearchService + "doesn't have " + SERVICE_CONFIGURATION_NAME_INFRA_SOLR + " as service configuration"));

        boolean secured;
        try {
            secured = infraSolrConf.getConfigurationMap().containsKey(AMBARI_INFRA_SOLR_KERBEROS_PRINCIPAL);
        } catch (IOException e) {
            throw new RuntimeException("Fail to read service configuration " + SERVICE_CONFIGURATION_NAME_INFRA_SOLR);
        }

        Map<String, Object> confForLogSearchService = new HashMap<>();
        confForLogSearchService.put(SOLR_API_URL_KEY, buildAmbariInfraSolrRestApiRootUrl(solrHost, solrPort));
        confForLogSearchService.put(TopologyLayoutConstants.SUBJECT_OBJECT, subject);
        confForLogSearchService.put(SECURED_CLUSTER_KEY, secured);
        return confForLogSearchService;
    }

    private String buildAmbariInfraSolrRestApiRootUrl(String host, Integer port) {
        return "http://" + host + ":" + port + "/solr";
    }
}
