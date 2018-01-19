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

import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.metrics.AbstractTimeSeriesQuerier;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final List<String> METRICS_NEED_AGGREGATION_ON_STREAMS = Lists.newArrayList(
            "__complete-latency", "__emit-count", "__ack-count", "__fail-count",
            "__process-latency", "__execute-count", "__execute-latency"
    );
    public static final String DEFAULT_APP_ID = "nimbus";
    public static final String WILDCARD_ALL_COMPONENTS = "%";

    enum Precision {
        SECONDS, MINUTES, HOURS, DAYS
    }

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
    public Map<Long, Double> getMetrics(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction,
                                          long from, long to) {
        URI targetUri = composeQueryParameters(topologyName, componentId, metricName, aggrFunction, from, to);

        log.debug("Calling {} for querying metric", targetUri.toString());

        Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUri), Map.class);
        List<Map<String, ?>> metrics = (List<Map<String, ?>>) responseMap.get("metrics");

        if (metrics.size() > 0) {
            Map<String, Number> points = (Map<String, Number>) metrics.get(0).get("metrics");
            Map<Long, Double> ret = new HashMap<>(points.size());

            for (Map.Entry<String, Number> timestampToValue : points.entrySet()) {
                ret.put(Long.valueOf(timestampToValue.getKey()), timestampToValue.getValue().doubleValue());
            }

            return ret;
        } else {
            return Collections.emptyMap();
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

        Precision precision = determinePrecision(from, to);

        // force replacing values for metricNames, startTime, endTime, precision with parameters
        return uriBuilder.replaceQueryParam("metricNames", metricName)
                .replaceQueryParam("startTime", String.valueOf(from))
                .replaceQueryParam("endTime", String.valueOf(to))
                .queryParam("precision", precision.name())
                .build();
    }

    private URI composeQueryParameters(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction,
                                       long from, long to) {
        String actualMetricName = buildMetricName(topologyName, componentId, metricName);
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder();
        Precision precision = determinePrecision(from, to);
        return uriBuilder.uri(collectorApiUri)
                .queryParam("appId", DEFAULT_APP_ID)
                .queryParam("hostname", "")
                .queryParam("metricNames", actualMetricName)
                .queryParam("startTime", String.valueOf(from))
                .queryParam("endTime", String.valueOf(to))
                .queryParam("seriesAggregateFunction", aggrFunction.name())
                .queryParam("precision", precision.name())
                .build();
    }

    private Precision determinePrecision(long from, long to) {
        long timeDiff = to - from;

        // don't support time range smaller than 1 minute
        if (timeDiff < 1000 * 60) {
            throw new IllegalArgumentException("Time range should be greater than 1 minute.");
        } else if (timeDiff < (1000L * 60 * 60 * 24 * 7)) {
            // precision to minute for 1 min ~ 7 days
            return Precision.MINUTES;
        } else if (timeDiff < (1000L * 60 * 60 * 24 * 30)) {
            // precision to minute for 7 min ~ 30 days
            return Precision.HOURS;
        }

        return Precision.DAYS;
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
}
