package org.apache.streamline.streams.metrics.storm.graphite;

import com.google.common.collect.Lists;
import org.apache.streamline.streams.exception.ConfigException;
import org.apache.streamline.streams.metrics.AbstractTimeSeriesQuerier;
import org.apache.commons.lang.BooleanUtils;
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
 * Implementation of TimeSeriesQuerier for Graphite with Storm.
 * <p/>
 * This class assumes that metrics for Storm is pushed to Graphite via verisign/storm-graphite.
 * https://github.com/verisign/storm-graphite
 * <p/>
 * appId is 'topology name', and metric name is composed to '[component name].[task id].[metric name](.[key of the value map])'.
 *
 * FIXME: Improve verisign/storm-graphite to escape dot(.) characters on worker host and remove option useFQDN.
 */
public class GraphiteWithStormQuerier extends AbstractTimeSeriesQuerier {
    private static final Logger log = LoggerFactory.getLogger(GraphiteWithStormQuerier.class);

    // the configuration keys
    static final String RENDER_API_URL = "renderApiUrl";
    static final String METRIC_NAME_PREFIX = "metricNamePrefix";
    static final String USE_FQDN = "useFQDN";

    // these metrics need '.%' as postfix to aggregate values for each stream
    private static final List<String> METRICS_NEED_AGGREGATION_ON_STREAMS = Lists.newArrayList(
            "__complete-latency", "__emit-count", "__ack-count", "__fail-count",
            "__process-latency", "__execute-count", "__execute-latency"
    );
    public static final String WILDCARD_ALL_COMPONENTS = "*";

    private Client client;
    private URI renderApiUrl;
    private String metricNamePrefix;
    private boolean useFQDN;

    public GraphiteWithStormQuerier() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Map<String, String> conf) throws ConfigException {
        if (conf != null) {
            try {
                renderApiUrl = new URI(conf.get(RENDER_API_URL));
                metricNamePrefix = conf.get(METRIC_NAME_PREFIX);
                useFQDN = BooleanUtils.toBoolean(conf.get(USE_FQDN));
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

        List<Map<String, ?>> responseList = client.target(targetUri).request(MediaType.APPLICATION_JSON_TYPE).get(List.class);
        if (responseList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ?> metrics = responseList.get(0);
        List<List<Number>> dataPoints = (List<List<Number>>) metrics.get("datapoints");
        return formatDataPointsFromGraphiteToMap(dataPoints);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<Long, Double>> getRawMetrics(String metricName, String parameters, long from, long to) {
        Map<String, String> queryParams = parseParameters(parameters);
        URI targetUri = composeRawQueryParameters(metricName, queryParams, from, to);

        log.debug("Calling {} for querying metric", targetUri.toString());

        List<Map<String, ?>> responseList = client.target(targetUri).request(MediaType.APPLICATION_JSON_TYPE).get(List.class);
        if (responseList.size() > 0) {
            Map<String, Map<Long, Double>> ret = new HashMap<>(responseList.size());
            for (Map<String, ?> metric : responseList) {
                String target = (String) metric.get("target");
                List<List<Number>> dataPoints = (List<List<Number>>) metric.get("datapoints");
                Map<Long, Double> pointsForOutput = formatDataPointsFromGraphiteToMap(dataPoints);
                ret.put(target, pointsForOutput);
            }

            return ret;
        } else {
            return Collections.emptyMap();
        }
    }

    private URI composeQueryParameters(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction,
                                       long from, long to) {
        String actualMetricName = buildMetricName(topologyName, componentId, metricName, aggrFunction);
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder();
        return uriBuilder.uri(renderApiUrl)
                .queryParam("target", actualMetricName)
                .queryParam("format", "json")
                .queryParam("from", String.valueOf((int) (from / 1000)))
                .queryParam("until", String.valueOf((int) (to / 1000)))
                .build();
    }

    private URI composeRawQueryParameters(String metricName, Map<String, String> queryParams, long from, long to) {
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder().uri(renderApiUrl);
        for (Map.Entry<String, String> pair : queryParams.entrySet()) {
            uriBuilder = uriBuilder.queryParam(pair.getKey(), pair.getValue());
        }

        // force replacing values for target, format, from, until with parameters
        return uriBuilder.replaceQueryParam("target", metricName)
                .queryParam("format", "json")
                .queryParam("from", String.valueOf((int) (from / 1000)))
                .queryParam("until", String.valueOf((int) (to / 1000)))
                .build();
    }

    private Map<Long, Double> formatDataPointsFromGraphiteToMap(List<List<Number>> dataPoints) {
        Map<Long, Double> pointsForOutput = new HashMap<>();

        if (dataPoints != null && dataPoints.size() > 0) {
            for (List<Number> dataPoint : dataPoints) {
                // ex. [2940.0, 1465803540] -> 1465803540000, 2940.0
                Number valueNum = dataPoint.get(0);
                Number timestampNum = dataPoint.get(1);
                if (valueNum == null) {
                    continue;
                }
                pointsForOutput.put(timestampNum.longValue() * 1000, valueNum.doubleValue());
            }
        }
        return pointsForOutput;
    }

    private String buildMetricName(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction) {
        // Example: http://localhost:9999/render/?target=sumSeries(storm.production.spout.*.*.*.*.*.*.__emit-count.*)&format=json&from=1465802460&until=1465803540
        // prefix.topologyName.componentId.hostname.port.taskId.metricName.field
        // we use wildcard on hostname, port, taskId, and field (stream)
        String actualMetricNameFormat = "%s(%s.%s.%s.%s.*.*.%s)";

        // worker host could be IP or fqdn, but storm-graphite doesn't apply escape on IP
        // Graphite doesn't support wildcard across multiple buckets so we should know about this in order to apply wildcard...
        // So applying workaround instead
        String hostName;
        if (useFQDN) {
            // assuming FQDN doesn't have dot
            hostName = "*";
        } else {
            // assuming hostname is represented as IP
            hostName = "*.*.*.*";
        }

        String metricNameForAggregation = metricName;

        if (METRICS_NEED_AGGREGATION_ON_STREAMS.contains(metricName)) {
            metricNameForAggregation = metricName + ".*";
        }
        // verisign/storm-kafka replaces '/' to '.'
        metricNameForAggregation = metricNameForAggregation.replace('/', '.');

        String functionName;
        switch (aggrFunction) {
            case SUM:
                functionName = "sumSeries";
                break;
            case AVG:
                functionName = "averageSeries";
                break;
            case MIN:
                functionName = "minSeries";
                break;
            case MAX:
                functionName = "maxSeries";
                break;
            default:
                throw new IllegalArgumentException("Aggregate function should be one of [sum / avg / min / max]");
        }

        return String.format(actualMetricNameFormat, functionName, metricNamePrefix, topologyName, componentId, hostName,
                metricNameForAggregation);
    }
}
