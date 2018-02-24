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
package com.hortonworks.streamline.streams.metrics.storm.ambari;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.common.util.DoubleUtils;
import com.hortonworks.streamline.streams.metrics.AbstractTimeSeriesQuerier;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.DoubleStream;

import static java.util.stream.Collectors.toMap;

/**
 * Implementation of TimeSeriesQuerier for Ambari Metric Service (AMS) with Storm.
 * <p/>
 * This class assumes that metrics for Storm is pushed to AMS via Ambari Storm Metrics Sink.
 * appId is user specific (default is 'nimbus'), and metric name is composed to 'topology.[topology name].[component name].[task id].[metric name](.[key of the value map])'.
 * <p/>
 * Please note that this class requires Ambari 2.4 or above.
 */
public class AmbariMetricsServiceWithStormQuerier extends AbstractTimeSeriesQuerier {
    private static final Logger log = LoggerFactory.getLogger(AmbariMetricsServiceWithStormQuerier.class);

    public static final String METRIC_NAME_PREFIX_KAFKA_OFFSET = "kafkaOffset.";

    // the configuration keys
    static final String COLLECTOR_API_URL = "collectorApiUrl";
    static final String APP_ID = "appId";

    // these metrics need '.%' as postfix to aggregate values for each stream
    private static final List<String> METRICS_NEED_AGGREGATION_ON_STREAMS = ImmutableList.<String>builder().add(
            "__complete-latency", "__emit-count", "__ack-count", "__fail-count",
            "__process-latency", "__execute-count", "__execute-latency"
    ).build();

    private static final Map<String, String> METRICS_APPLY_WEIGHTED_AVERAGE_PAIR =
            ImmutableMap.<String, String>builder()
                    .put("--complete-latency", "--ack-count")
                    .put("--process-latency", "--execute-count")
                    .put("--execute-latency", "--execute-count")
                    .build();

    // they're actually prefixed by '__' but in metric name, '__' is replaced to '--'
    private static final List<String> SYSTEM_STREAM_PREFIX = ImmutableList.<String>builder()
            .add("--metric", "--ack-init", "--ack-ack", "--ack-fail", "--ack-reset-timeout", "--system").build();

    static final String DEFAULT_APP_ID = "nimbus";
    private static final String WILDCARD_ALL_COMPONENTS = "%";

    private Client client;
    private URI collectorApiUri;
    private String appId;

    public AmbariMetricsServiceWithStormQuerier() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Map<String, String> conf) throws ConfigException {
        if (conf != null) {
            try {
                collectorApiUri = new URI(conf.get(COLLECTOR_API_URL));
                appId = conf.get(APP_ID);
                if (appId == null) {
                    appId = DEFAULT_APP_ID;
                }
            } catch (URISyntaxException e) {
                throw new ConfigException(e);
            }
        }
        client = ClientBuilder.newClient(new ClientConfig());
    }

    @Override
    public Map<Long, Double> getTopologyLevelMetrics(String topologyName, String metricName,
                                                     AggregateFunction aggrFunction, long from, long to) {
        return getMetrics(topologyName, WILDCARD_ALL_COMPONENTS, metricName, aggrFunction, from, to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Double> getMetrics(String topologyName, String componentId, String metricName,
                                        AggregateFunction aggrFunction, long from, long to) {

        Optional<String> weightMetric = findWeightMetric(metricName);

        if (weightMetric.isPresent()) {
            Map<Long, List<Pair<String, Double>>> keyMetrics = getMetricsStreamToValueMap(topologyName, componentId,
                    metricName, from, to);
            Map<Long, List<Pair<String, Double>>> weightMetrics = getMetricsStreamToValueMap(topologyName, componentId,
                    weightMetric.get(), from, to);
            return aggregateWithApplyingWeightedAverage(keyMetrics, weightMetrics);
        } else {
            Map<Long, List<Pair<String, Double>>> ret = getMetricsStreamToValueMap(topologyName, componentId, metricName, from, to);
            return aggregateStreamsForMetricsValues(ret, aggrFunction);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<Long, Double>> getRawMetrics(String metricName, String parameters, long from, long to) {
        Map<String, String> queryParams = parseParameters(parameters);
        URI targetUri = composeRawQueryParameters(metricName, queryParams, from, to);

        log.debug("Calling {} for querying metric", targetUri.toString());

        Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUri), Map.class);
        List<Map<String, ?>> metrics = (List<Map<String, ?>>) responseMap.get("metrics");

        if (metrics.size() > 0) {
            Map<String, Map<Long, Double>> ret = new HashMap<>(metrics.size());
            for (Map<String, ?> metric : metrics) {
                String retrievedMetricName = (String) metric.get("metricname");
                Map<String, Number> retrievedPoints = (Map<String, Number>) metric.get("metrics");

                Map<Long, Double> pointsForOutput;
                if (retrievedPoints == null || retrievedPoints.isEmpty()) {
                    pointsForOutput = Collections.emptyMap();
                } else {
                    pointsForOutput = new HashMap<>(retrievedPoints.size());
                    for (Map.Entry<String, Number> timestampToValue : retrievedPoints.entrySet()) {
                        pointsForOutput.put(Long.valueOf(timestampToValue.getKey()), timestampToValue.getValue().doubleValue());
                    }
                }

                ret.put(retrievedMetricName, pointsForOutput);
            }

            return ret;
        } else {
            return Collections.emptyMap();
        }
    }

    private URI composeRawQueryParameters(String metricName, Map<String, String> queryParams, long from, long to) {
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder().uri(collectorApiUri);
        for (Map.Entry<String, String> pair : queryParams.entrySet()) {
            uriBuilder = uriBuilder.queryParam(pair.getKey(), pair.getValue());
        }

        // force replacing values for metricNames, startTime, endTime with parameters
        return uriBuilder.replaceQueryParam("metricNames", metricName)
                .replaceQueryParam("startTime", String.valueOf(from))
                .replaceQueryParam("endTime", String.valueOf(to))
                .build();
    }

    private URI composeQueryParameters(String topologyName, String componentId, String metricName,
                                       long from, long to) {
        String actualMetricName = buildMetricName(topologyName, componentId, metricName);
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder();
        return uriBuilder.uri(collectorApiUri)
                .queryParam("appId", DEFAULT_APP_ID)
                .queryParam("hostname", "")
                .queryParam("metricNames", actualMetricName)
                .queryParam("startTime", String.valueOf(from))
                .queryParam("endTime", String.valueOf(to))
                .build();
    }

    private String buildMetricName(String topologyName, String componentId, String metricName) {
        String actualMetricName;

        if (metricName.startsWith(METRIC_NAME_PREFIX_KAFKA_OFFSET)) {
            actualMetricName = createKafkaOffsetMetricName(topologyName, metricName);
        } else {
            actualMetricName = "topology." + topologyName + "." + componentId + ".%." + metricName;
        }

        if (METRICS_NEED_AGGREGATION_ON_STREAMS.contains(metricName)) {
            actualMetricName = actualMetricName + ".%";
        }

        // since '._' is treat as special character (separator) so it should be replaced
        return actualMetricName.replace('_', '-');
    }

    private String createKafkaOffsetMetricName(String topologyName, String kafkaOffsetMetricName) {
        // get rid of "kafkaOffset."
        // <topic>/<metric name (starts with total)> or <topic>/partition_<partition_num>/<metricName>
        String tempMetricName = kafkaOffsetMetricName.substring(METRIC_NAME_PREFIX_KAFKA_OFFSET.length());

        String[] slashSplittedNames = tempMetricName.split("/");

        if (slashSplittedNames.length == 1) {
            // unknown metrics
            throw new IllegalArgumentException("Unknown metrics for kafka offset metric: " + kafkaOffsetMetricName);
        }

        String topic = slashSplittedNames[0];
        String metricName = "topology." + topologyName + ".kafka-topic." + topic;
        if (slashSplittedNames.length > 2) {
            // partition level
            metricName = metricName + "." + slashSplittedNames[1] + "." + slashSplittedNames[2];
        } else {
            // topic level
            metricName = metricName + "." + slashSplittedNames[1];
        }

        return metricName;
    }

    @VisibleForTesting
    Map<Long, Double> aggregateWithApplyingWeightedAverage(Map<Long, List<Pair<String, Double>>> keyMetric,
                                                                   Map<Long, List<Pair<String, Double>>> weightMetric) {
        Map<Long, Double> ret = new HashMap<>();
        for (Map.Entry<Long, List<Pair<String, Double>>> keyMetricEntry : keyMetric.entrySet()) {
            long timestamp = keyMetricEntry.getKey();
            List<Pair<String, Double>> keyStreamToValueList = keyMetricEntry.getValue();
            List<Pair<String, Double>> weightStreamToValueList = weightMetric.get(timestamp);

            if (weightStreamToValueList == null || weightStreamToValueList.isEmpty()) {
                // weight information not found
                ret.put(timestamp, 0.0d);
                continue;
            }

            Double totalWeight = weightStreamToValueList.stream().mapToDouble(p -> p.getRight()).sum();
            if (DoubleUtils.equalsToZero(totalWeight)) {
                // total weight is zero
                ret.put(timestamp, 0.0d);
                continue;
            }

            double weightedSum = keyStreamToValueList.stream().map(pair -> {
                String stream = pair.getLeft();
                Double value = pair.getRight();
                Double weightForStream = weightStreamToValueList.stream()
                        .filter(p -> p.getLeft().equals(stream)).findAny().map(op -> op.getRight()).orElse(0.0);
                Double weight = weightForStream / totalWeight;
                return value * weight;
            }).mapToDouble(d -> d).sum();

            ret.put(timestamp, weightedSum);
        }

        return ret;
    }

    private Optional<String> findWeightMetric(String metricName) {
        String weightMetric = METRICS_APPLY_WEIGHTED_AVERAGE_PAIR.get(metricName);
        return Optional.ofNullable(weightMetric);
    }

    private Map<Long, Double> aggregateStreamsForMetricsValues(Map<Long, List<Pair<String, Double>>> ret, AggregateFunction aggrFunction) {
        return ret.entrySet().stream()
                .collect(toMap(e -> e.getKey(), e -> {
                    DoubleStream valueStream = e.getValue().stream().mapToDouble(d -> d.getRight());
                    switch (aggrFunction) {
                        case SUM:
                            return valueStream.sum();

                        case AVG:
                            return valueStream.average().orElse(0.0d);

                        case MAX:
                            return valueStream.max().orElse(0.0d);

                        case MIN:
                            return valueStream.min().orElse(0.0d);

                        default:
                            throw new IllegalArgumentException("Not supported aggregated function.");

                    }
                }));
    }

    private Map<Long, List<Pair<String, Double>>> getMetricsStreamToValueMap(String topologyName, String componentId,
                                                                             String metricName, long from, long to) {
        List<Map<String, ?>> metrics = getMetricsMap(topologyName, componentId, metricName, from, to);
        Map<Long, List<Pair<String, Double>>> ret = new HashMap<>();
        if (metrics.size() > 0) {
            for (Map<String, ?> metric : metrics) {
                String retrievedMetricName = (String) metric.get("metricname");

                // exclude system streams
                if (!isMetricFromSystemStream(retrievedMetricName)) {
                    Map<String, Number> points = (Map<String, Number>) metric.get("metrics");
                    for (Map.Entry<String, Number> timestampToValue : points.entrySet()) {
                        Long timestamp = Long.valueOf(timestampToValue.getKey());
                        List<Pair<String, Double>> values = ret.getOrDefault(timestamp, new ArrayList<>());
                        if (values.isEmpty()) {
                            ret.put(timestamp, values);
                        }

                        values.add(Pair.of(retrievedMetricName, timestampToValue.getValue().doubleValue()));
                    }
                }
            }
        }
        return ret;
    }

    private List<Map<String, ?>> getMetricsMap(String topologyName, String componentId, String metricName, long from, long to) {
        URI targetUri = composeQueryParameters(topologyName, componentId, metricName, from, to);

        log.debug("Calling {} for querying metric", targetUri.toString());

        Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUri), Map.class);
        return (List<Map<String, ?>>) responseMap.get("metrics");
    }

    private boolean isMetricFromSystemStream(String metricName) {
        return SYSTEM_STREAM_PREFIX.stream().anyMatch(metricName::contains);
    }
}
