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
package com.hortonworks.streamline.streams.metrics.container;

import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.metrics.container.mapping.MappedTimeSeriesQuerierImpl;
import com.hortonworks.streamline.streams.metrics.container.mapping.MappedTopologyMetricsImpl;
import com.hortonworks.streamline.streams.cluster.container.NamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;

import javax.security.auth.Subject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TopologyMetricsContainer extends NamespaceAwareContainer<TopologyMetrics> {

    public static final String COMPONENT_NAME_METRICS_COLLECTOR = ComponentPropertyPattern.METRICS_COLLECTOR.name();
    public static final String COLLECTOR_API_URL_KEY = "collectorApiUrl";

    private final Subject subject;

    public TopologyMetricsContainer(EnvironmentService environmentService, Subject subject) {
        super(environmentService);
        this.subject = subject;
    }

    @Override
    protected TopologyMetrics initializeInstance(Namespace namespace) {
        String streamingEngine = namespace.getStreamingEngine();

        MappedTopologyMetricsImpl metricsImpl;
        // Only Storm is supported as streaming engine
        try {
            metricsImpl = MappedTopologyMetricsImpl.valueOf(streamingEngine);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported streaming engine: " + streamingEngine, e);
        }

        // FIXME: "how to initialize" is up to implementation detail - now we just only consider about Storm implementation
        Map<String, Object> conf = buildStormTopologyMetricsConfigMap(namespace, streamingEngine, subject);

        String className = metricsImpl.getClassName();
        TopologyMetrics topologyMetrics = initTopologyMetrics(conf, className);

        String timeSeriesDB = namespace.getTimeSeriesDB();
        if (timeSeriesDB != null && !timeSeriesDB.isEmpty()) {
            String querierKey = MappedTimeSeriesQuerierImpl.getName(streamingEngine, timeSeriesDB);

            MappedTimeSeriesQuerierImpl timeSeriesQuerierImpl;
            try {
                timeSeriesQuerierImpl = MappedTimeSeriesQuerierImpl.valueOf(querierKey);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Unsupported streaming engine and time-series DB combination: " + streamingEngine +
                        " & " + timeSeriesDB, e);
            }

            // FIXME: "how to initialize" is up to implementation detail - now we just only consider about Storm & AMS implementation
            Map<String, String> confTimeSeriesQuerier = buildAMSTimeSeriesQuerierConfigMap(namespace, timeSeriesDB);

            className = timeSeriesQuerierImpl.getClassName();
            TimeSeriesQuerier timeSeriesQuerier = initTimeSeriesQuerier(confTimeSeriesQuerier, className);

            topologyMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
        }

        return topologyMetrics;
    }

    private TopologyMetrics initTopologyMetrics(Map<String, Object> conf, String className) {
        try {
            TopologyMetrics topologyMetrics = instantiate(className);
            topologyMetrics.init(conf);
            return topologyMetrics;
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ConfigException e) {
            throw new RuntimeException("Can't initialize Topology metrics instance - Class Name: " + className, e);
        }
    }

    private TimeSeriesQuerier initTimeSeriesQuerier(Map<String, String> conf, String className) {
        try {
            Class<?> timeSeriesQuerierImplClass = Class.forName(className);
            TimeSeriesQuerier timeSeriesQuerier = (TimeSeriesQuerier) timeSeriesQuerierImplClass.newInstance();
            timeSeriesQuerier.init(conf);
            return timeSeriesQuerier;
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ConfigException e) {
            throw new RuntimeException("Can't initialize Time-series Querier instance - Class Name: " + className, e);
        }
    }

    private Map<String, Object> buildStormTopologyMetricsConfigMap(Namespace namespace, String streamingEngine, Subject subject) {
        Map<String, Object> conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY, buildStormRestApiRootUrl(namespace, streamingEngine));
        conf.put(TopologyLayoutConstants.SUBJECT_OBJECT, subject);
        return conf;
    }

    private Map<String, String> buildAMSTimeSeriesQuerierConfigMap(Namespace namespace, String timeSeriesDB) {
        // Assuming that a namespace has one mapping of time-series DB service
        Service timeSeriesDBService = getFirstOccurenceServiceForNamespace(namespace, timeSeriesDB);
        if (timeSeriesDBService == null) {
            throw new RuntimeException("Time-series DB " + timeSeriesDB + " is not associated to the namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }

        Component metricsCollector = getComponent(timeSeriesDBService, COMPONENT_NAME_METRICS_COLLECTOR)
                .orElseThrow(() -> new RuntimeException(timeSeriesDB + " doesn't have " + COMPONENT_NAME_METRICS_COLLECTOR + " as component"));

        Collection<ComponentProcess> metricsCollectorProcesses = environmentService.listComponentProcesses(metricsCollector.getId());
        if (metricsCollectorProcesses.isEmpty()) {
            throw new RuntimeException(timeSeriesDB + " doesn't have any process for " + COMPONENT_NAME_METRICS_COLLECTOR + " as component");
        }

        ComponentProcess metricsCollectorProcess = metricsCollectorProcesses.iterator().next();
        String metricsCollectorHost = metricsCollectorProcess.getHost();
        Integer metricsCollectorPort = metricsCollectorProcess.getPort();

        assertHostAndPort(COMPONENT_NAME_METRICS_COLLECTOR, metricsCollectorHost, metricsCollectorPort);

        Map<String, String> confForTimeSeriesQuerier = new HashMap<>();
        confForTimeSeriesQuerier.put(COLLECTOR_API_URL_KEY, buildAMSCollectorRestApiRootUrl(metricsCollectorHost, metricsCollectorPort));
        return confForTimeSeriesQuerier;
    }


    private String buildAMSCollectorRestApiRootUrl(String host, Integer port) {
        return "http://" + host + ":" + port + "/ws/v1/timeline/metrics";
    }
}