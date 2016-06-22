package com.hortonworks.iotas.metrics.storm.ambari;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.errors.ConfigException;
import com.hortonworks.iotas.metrics.AbstractTimeSeriesQuerier;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
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
 * appId is 'topology name', and metric name is composed to '[component name].[task id].[metric name](.[key of the value map])'.
 * <p/>
 * Please note that this class requires Ambari 2.4 or above.
 * <p/>
 * TODO: confirm AMBARI-16946 is merged and included to Ambari 2.4.0 since this rule will be introduced as these issues. <br/>
 * TODO: also confirm AMBARI-16949 and AMBARI-17027, and AMBARI-17133 are included to Ambari 2.4.0 as well. <br/>
 * TODO: if any of them cannot be included as Ambari 2.4, we should modify compatible version. <br/>
 */
public class AmbariMetricsServiceWithStormQuerier extends AbstractTimeSeriesQuerier {
    private static final Logger log = LoggerFactory.getLogger(AmbariMetricsServiceWithStormQuerier.class);

    // the configuration keys
    static final String COLLECTOR_API_URL = "collectorApiUrl";

    // these metrics need '.%' as postfix to aggregate values for each stream
    private static final List<String> METRICS_NEED_AGGREGATION_ON_STREAMS = Lists.newArrayList(
            "__complete-latency", "__emit-count", "__ack-count", "__fail-count",
            "__process-latency", "__execute-count", "__execute-latency"
    );

    private Client client;
    private URI collectorApiUri;

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
            } catch (URISyntaxException e) {
                throw new ConfigException(e);
            }
        }
        client = ClientBuilder.newClient(new ClientConfig());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Double> getMetrics(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction,
                                          long from, long to) {
        URI targetUri = composeQueryParameters(topologyName, componentId, metricName, aggrFunction, from, to);

        log.debug("Calling {} for querying metric", targetUri.toString());

        Map<String, ?> responseMap = client.target(targetUri).request(MediaType.APPLICATION_JSON_TYPE).get(Map.class);
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

        Map<String, ?> responseMap = client.target(targetUri).request(MediaType.APPLICATION_JSON_TYPE).get(Map.class);
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

    private URI composeQueryParameters(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction,
                                       long from, long to) {
        String actualMetricName = buildMetricName(componentId, metricName);
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder();
        return uriBuilder.uri(collectorApiUri)
                .queryParam("appId", topologyName)
                .queryParam("hostname", "")
                .queryParam("metricNames", actualMetricName)
                .queryParam("startTime", String.valueOf(from))
                .queryParam("precision", "seconds")
                .queryParam("endTime", String.valueOf(to))
                .queryParam("seriesAggregateFunction", aggrFunction.name())
                .build();
    }

    private String buildMetricName(String componentId, String metricName) {
        String actualMetricName = componentId + ".%." + metricName;

        if (METRICS_NEED_AGGREGATION_ON_STREAMS.contains(metricName)) {
            actualMetricName = actualMetricName + ".%";
        }

        return actualMetricName.replace('_', '-');
    }
}
