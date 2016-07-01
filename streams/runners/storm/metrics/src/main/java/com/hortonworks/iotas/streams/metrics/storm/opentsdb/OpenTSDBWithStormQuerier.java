package com.hortonworks.iotas.streams.metrics.storm.opentsdb;

import com.google.common.base.Joiner;
import com.hortonworks.iotas.common.exception.ConfigException;
import com.hortonworks.iotas.streams.metrics.AbstractTimeSeriesQuerier;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of TimeSeriesQuerier for OpenTSDB with Storm.
 * <p/>
 * Since there's no well-known metrics consumer for OpenTSDB, this class only supports raw query (getRawMetrics) for now.
 * (getMetrics will throw UnsupportedOperationException unless implemented)
 */
public class OpenTSDBWithStormQuerier extends AbstractTimeSeriesQuerier {
    private static final Logger log = LoggerFactory.getLogger(OpenTSDBWithStormQuerier.class);

    // the configuration keys
    public static final String QUERY_API_URL = "queryApiUrl";

    private Client client;
    private URI queryApiUri;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Map<String, String> conf) throws ConfigException {
        if (conf != null) {
            try {
                queryApiUri = new URI(conf.get(QUERY_API_URL));
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
    public Map<Long, Double> getMetrics(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction, long from, long to) {
        throw new UnsupportedOperationException("OpenTSDBWithStormQuerier only supports raw query");
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
        if (responseList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<Long, Double>> ret = new HashMap<>(responseList.size());
        for (Map<String, ?> responseMap : responseList) {
            String retrievedMetricName = buildMetricNameFromResp(responseMap);
            Map<String, Number> retrievedPoints = (Map<String, Number>) responseMap.get("dps");

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
    }

    private URI composeRawQueryParameters(String metricName, Map<String, String> queryParams, long from, long to) {
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder().uri(queryApiUri);
        for (Map.Entry<String, String> pair : queryParams.entrySet()) {
            uriBuilder = uriBuilder.queryParam(pair.getKey(), pair.getValue());
        }

        // force replacing values for m, start, end with parameters
        return uriBuilder.replaceQueryParam("m", metricName)
                .replaceQueryParam("start", String.valueOf(from))
                .replaceQueryParam("end", String.valueOf(to))
                .build();
    }

    private String buildMetricNameFromResp(Map<String, ?> responseMap) {
        String metricName = (String) responseMap.get("metric");
        Map<String, ?> tags = (Map<String, ?>) responseMap.get("tags");
        if (tags == null || tags.isEmpty()) {
            return metricName;
        }

        List<String> tagReprList = new ArrayList<>();
        for (Map.Entry<String, ?> tag : tags.entrySet()) {
            tagReprList.add(tag.getKey() + "=" + tag.getValue().toString());
        }

        return metricName + "[" + Joiner.on(",").join(tagReprList) + "]";
    }
}
