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
package com.hortonworks.streamline.streams.metrics.storm.topology;

import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.streams.exception.ConfigException;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIClient;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.STATS_JSON_ACKED_TUPLES;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.STATS_JSON_COMPLETE_LATENCY;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.STATS_JSON_EMITTED_TUPLES;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.STATS_JSON_EXECUTED_TUPLES;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.STATS_JSON_FAILED_TUPLES;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.STATS_JSON_PROCESS_LATENCY;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.STATS_JSON_TOPOLOGY_ERROR_COUNT;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.STATS_JSON_TRANSFERRED_TUPLES;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_BOLTS;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_BOLT_ID;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_COMPONENT_ERRORS;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_EXECUTORS_TOTAL;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_SPOUTS;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_SPOUT_ID;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_STATS;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_STATUS;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_UPTIME_SECS;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_WINDOW;
import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.TOPOLOGY_JSON_WORKERS_TOTAL;

/**
 * Storm implementation of the TopologyMetrics interface
 */
public class StormTopologyMetricsImpl implements TopologyMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologyMetricsImpl.class);

    private static final String FRAMEWORK = "STORM";
    private static final int MAX_SIZE_TOPOLOGY_CACHE = 10;
    private static final int MAX_SIZE_COMPONENT_CACHE = 50;
    private static final int CACHE_DURATION_SECS = 5;
    private static final int FORK_JOIN_POOL_PARALLELISM = 50;

    // shared across the metrics instances
    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);

    private StormRestAPIClient client;
    private TopologyTimeSeriesMetrics timeSeriesMetrics;

    private LoadingCache<Pair<String, String>, Map<String, ?>> topologyRetrieveCache;
    private LoadingCache<Pair<Pair<String, String>, String>, Map<String, ?>> componentRetrieveCache;

    public StormTopologyMetricsImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Map<String, Object> conf) throws ConfigException {
        String stormApiRootUrl = null;
        Subject subject = null;
        if (conf != null) {
            stormApiRootUrl = (String) conf.get(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY);
            subject = (Subject) conf.get(TopologyLayoutConstants.SUBJECT_OBJECT);
        }
        Client restClient = ClientBuilder.newClient(new ClientConfig());
        this.client = new StormRestAPIClient(restClient, stormApiRootUrl, subject);
        timeSeriesMetrics = new StormTopologyTimeSeriesMetricsImpl(client);
        topologyRetrieveCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_SIZE_TOPOLOGY_CACHE)
                .expireAfterWrite(CACHE_DURATION_SECS, TimeUnit.SECONDS)
                .build(new CacheLoader<Pair<String, String>, Map<String, ?>>() {
                    @Override
                    public Map<String, ?> load(Pair<String, String> stormTopologyIdAndAsUser) {
                        String stormTopologyId = stormTopologyIdAndAsUser.getLeft();
                        String asUser = stormTopologyIdAndAsUser.getRight();
                        LOG.debug("retrieving topology info - topology id: {}, asUser: {}", stormTopologyId, asUser);
                        return client.getTopology(stormTopologyId, asUser);
                    }
                });
        componentRetrieveCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_SIZE_COMPONENT_CACHE)
                .expireAfterWrite(CACHE_DURATION_SECS, TimeUnit.SECONDS)
                .build(new CacheLoader<Pair<Pair<String, String>, String>, Map<String, ?>>() {
                    @Override
                    public Map<String, ?> load(Pair<Pair<String, String>, String> componentIdAndAsUserPair) {
                        String topologyId = componentIdAndAsUserPair.getLeft().getLeft();
                        String componentId = componentIdAndAsUserPair.getLeft().getRight();
                        String asUser = componentIdAndAsUserPair.getRight();
                        LOG.debug("retrieving component info - topology id: {}, component id: {}, asUser: {}",
                                topologyId, componentId, asUser);
                        return client.getComponent(topologyId, componentId, asUser);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyMetric getTopologyMetric(TopologyLayout topology, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (StringUtils.isEmpty(topologyId)) {
            throw new TopologyNotAliveException("Topology not found in Storm Cluster - topology id: " + topology.getId());
        }

        Map<String, ?> responseMap = getTopologyInfo(topologyId, asUser);

        Long uptimeSeconds = ((Number) responseMap.get(TOPOLOGY_JSON_UPTIME_SECS)).longValue();
        String status = (String) responseMap.get(TOPOLOGY_JSON_STATUS);
        Long workerTotal = ((Number) responseMap.get(TOPOLOGY_JSON_WORKERS_TOTAL)).longValue();
        Long executorTotal = ((Number) responseMap.get(TOPOLOGY_JSON_EXECUTORS_TOTAL)).longValue();

        List<Map<String, ?>> topologyStatsList = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_STATS);
        List<Map<String, ?>> spouts = (List<Map<String,?>>) responseMap.get(TOPOLOGY_JSON_SPOUTS);
        List<Map<String, ?>> bolts = (List<Map<String,?>>) responseMap.get(TOPOLOGY_JSON_BOLTS);

        // pick smallest time window
        Map<String, ?> topologyStatsMap = null;
        Long smallestWindow = Long.MAX_VALUE;
        for (Map<String, ?> topoStats : topologyStatsList) {
            String windowStr = (String) topoStats.get(TOPOLOGY_JSON_WINDOW);
            Long window = convertWindowString(windowStr, uptimeSeconds);
            if (smallestWindow > window) {
                smallestWindow = window;
                topologyStatsMap = topoStats;
            }
        }

        // extract metrics from smallest time window
        Long window = smallestWindow;
        Long acked = getLongValueOrDefault(topologyStatsMap, STATS_JSON_ACKED_TUPLES, 0L);
        Long failedRecords = getLongValueOrDefault(topologyStatsMap, STATS_JSON_FAILED_TUPLES, 0L);
        Double completeLatency = getDoubleValueFromStringOrDefault(topologyStatsMap, STATS_JSON_COMPLETE_LATENCY, 0.0d);

        // Storm specific metrics
        Long emittedTotal = getLongValueOrDefault(topologyStatsMap, STATS_JSON_EMITTED_TUPLES, 0L);
        Long transferred = getLongValueOrDefault(topologyStatsMap, STATS_JSON_TRANSFERRED_TUPLES, 0L);
        Long errorsTotal = getErrorCountFromAllComponents(topologyId, spouts, bolts, asUser);

        Map<String, Number> miscMetrics = new HashMap<>();
        miscMetrics.put(TOPOLOGY_JSON_WORKERS_TOTAL, workerTotal);
        miscMetrics.put(TOPOLOGY_JSON_EXECUTORS_TOTAL, executorTotal);
        miscMetrics.put(STATS_JSON_EMITTED_TUPLES, emittedTotal);
        miscMetrics.put(STATS_JSON_TRANSFERRED_TUPLES, transferred);
        miscMetrics.put(STATS_JSON_TOPOLOGY_ERROR_COUNT, errorsTotal);

        return new TopologyMetric(FRAMEWORK, topology.getName(), status, uptimeSeconds, window,
            acked * 1.0 / window, completeLatency, failedRecords, miscMetrics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ComponentMetric> getMetricsForTopology(TopologyLayout topology, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (StringUtils.isEmpty(topologyId)) {
            throw new TopologyNotAliveException("Topology not found in Storm Cluster - topology id: " + topology.getId());
        }

        Map<String, ?> responseMap = getTopologyInfo(topologyId, asUser);

        Map<String, ComponentMetric> metricMap = new HashMap<>();
        List<Map<String, ?>> spouts = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_SPOUTS);
        extractMetrics(metricMap, spouts, TOPOLOGY_JSON_SPOUT_ID);

        List<Map<String, ?>> bolts = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_BOLTS);
        extractMetrics(metricMap, bolts, TOPOLOGY_JSON_BOLT_ID);

        return metricMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {
        timeSeriesMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeSeriesQuerier getTimeSeriesQuerier() {
        return timeSeriesMetrics.getTimeSeriesQuerier();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Double> getCompleteLatency(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return timeSeriesMetrics.getCompleteLatency(topology, component, from, to, asUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return timeSeriesMetrics.getkafkaTopicOffsets(topology, component, from, to, asUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeSeriesComponentMetric getTopologyStats(TopologyLayout topology, long from, long to, String asUser) {
        return timeSeriesMetrics.getTopologyStats(topology, from, to, asUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeSeriesComponentMetric getComponentStats(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return timeSeriesMetrics.getComponentStats(topology, component, from, to, asUser);
    }

    private long getErrorCountFromAllComponents(String topologyId, List<Map<String, ?>> spouts, List<Map<String, ?>> bolts, String asUser) {
        LOG.debug("[START] getErrorCountFromAllComponents - topology id: {}, asUser: {}", topologyId, asUser);
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            List<String> componentIds = new ArrayList<>();

            if (spouts != null) {
                for (Map<String, ?> spout : spouts) {
                    componentIds.add((String) spout.get(TOPOLOGY_JSON_SPOUT_ID));
                }
            }

            if (bolts != null) {
                for (Map<String, ?> bolt : bolts) {
                    componentIds.add((String) bolt.get(TOPOLOGY_JSON_BOLT_ID));
                }
            }

            // query to components in parallel
            long errorCount = ParallelStreamUtil.execute(() ->
                            componentIds.parallelStream().mapToLong(componentId -> {
                                Map componentStats = getComponentInfo(topologyId, componentId, asUser);
                                List<?> componentErrors = (List<?>) componentStats.get(TOPOLOGY_JSON_COMPONENT_ERRORS);
                                if (componentErrors != null && !componentErrors.isEmpty()) {
                                    return componentErrors.size();
                                } else {
                                    return 0;
                                }
                            }).sum(), FORK_JOIN_POOL);

            LOG.debug("[END] getErrorCountFromAllComponents - topology id: {}, elapsed: {} ms", topologyId,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return errorCount;
        } finally {
            stopwatch.stop();
        }
    }

    private void extractMetrics(Map<String, ComponentMetric> metricMap, List<Map<String, ?>> components, String topologyJsonID) {
        for (Map<String, ?> component : components) {
            String name = (String) component.get(topologyJsonID);
            String componentId = getComponentIDInStreamline(name);
            ComponentMetric metric = extractMetric(name, component);
            metricMap.put(componentId, metric);
        }
    }

    private ComponentMetric extractMetric(String componentName, Map<String, ?> componentMap) {
        Long inputRecords = getLongValueOrDefault(componentMap, STATS_JSON_EXECUTED_TUPLES, 0L);
        Long outputRecords = getLongValueOrDefault(componentMap, STATS_JSON_EMITTED_TUPLES, 0L);
        Long failedRecords = getLongValueOrDefault(componentMap, STATS_JSON_FAILED_TUPLES, 0L);
        Double processedTime = getDoubleValueFromStringOrDefault(componentMap, STATS_JSON_PROCESS_LATENCY, 0.0d);

        return new ComponentMetric(getComponentNameInStreamline(componentName), inputRecords, outputRecords, failedRecords, processedTime);
    }

    private Long convertWindowString(String windowStr, Long uptime) {
        if (windowStr.equals(":all-time")) {
            return uptime;
        } else {
            return Long.valueOf(windowStr);
        }
    }

    private Long getLongValueOrDefault(Map<String, ?> map, String key, Long defaultValue) {
        if (map.containsKey(key)) {
            Number number = (Number) map.get(key);
            if (number != null) {
                return number.longValue();
            }
        }
        return defaultValue;
    }

    private Double getDoubleValueFromStringOrDefault(Map<String, ?> map, String key,
        Double defaultValue) {
        if (map.containsKey(key)) {
            String valueStr = (String) map.get(key);
            if (valueStr != null) {
                try {
                    return Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    // noop
                }
            }
        }
        return defaultValue;
    }

    private String getComponentIDInStreamline(String componentNameInStorm) {
        // removes all starting from first '-'
        return componentNameInStorm.substring(0, componentNameInStorm.indexOf('-'));
    }

    private String getComponentNameInStreamline(String componentNameInStorm) {
        // removes (ID + '-')
        return componentNameInStorm.substring(componentNameInStorm.indexOf('-') + 1);
    }

    private Map<String, ?> getTopologyInfo(String topologyId, String asUser) {
        LOG.debug("[START] getTopologyInfo - topology id: {}, asUser: {}", topologyId, asUser);
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            Map<String, ?> responseMap;
            try {
                responseMap = topologyRetrieveCache.get(new ImmutablePair<>(topologyId, asUser));
            } catch (ExecutionException e) {
                if (e.getCause() != null) {
                    throw new RuntimeException(e.getCause());
                } else {
                    throw new RuntimeException(e);
                }

            } catch (UncheckedExecutionException e) {
                if (e.getCause() != null) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e);
                }
            }

            LOG.debug("[END] getTopologyInfo - topology id: {}, elapsed: {} ms", topologyId,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return responseMap;
        } finally {
            stopwatch.stop();
        }
    }

    private Map<String, ?> getComponentInfo(String topologyId, String componentId, String asUser) {
        LOG.debug("[START] getComponentInfo - topology id: {}, component id: {}, asUser: {}", topologyId, componentId, asUser);
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            Map<String, ?> responseMap;
            try {
                responseMap = componentRetrieveCache.get(new ImmutablePair<>(new ImmutablePair<>(topologyId, componentId), asUser));
            } catch (ExecutionException e) {
                if (e.getCause() != null) {
                    throw new RuntimeException(e.getCause());
                } else {
                    throw new RuntimeException(e);
                }

            } catch (UncheckedExecutionException e) {
                if (e.getCause() != null) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e);
                }
            }

            LOG.debug("[END] getComponentInfo - topology id: {}, component id: {}, elapsed: {} ms", topologyId,
                    componentId, stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return responseMap;
        } finally {
            stopwatch.stop();
        }
    }

}