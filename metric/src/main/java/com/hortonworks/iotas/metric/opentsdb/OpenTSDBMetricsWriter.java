package com.hortonworks.iotas.metric.opentsdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.metric.HttpAPIWithStringBodyBasedMetricsWriter;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Metrics Writer for OpenTSDB.
 *
 * It uses /api/put API which is supported via starting 2.0, So you should use at least OpenTSDB 2.0 to use this implementation.
 */
public class OpenTSDBMetricsWriter extends HttpAPIWithStringBodyBasedMetricsWriter {
    private static final String PUT_API_PATH = "/api/put";

    private final ObjectMapper mapper;

    private String apiPath;

    public OpenTSDBMetricsWriter() {
        mapper = new ObjectMapper();
    }

    @Override
    public void initialize(Map<String, Object> config) {
        String host = (String) config.get("host");
        int port = ((Number)config.get("port")).intValue();

        Number connTimeout = (Number) config.get("connectTimeout");
        Number sockTimeout = (Number) config.get("socketTimeout");

        if (connTimeout != null) {
            connectTimeout = connTimeout.intValue();
        }

        if (sockTimeout != null) {
            socketTimeout = sockTimeout.intValue();
        }

        apiPath = createPathForPut(host, port);
    }

    @Override
    public void writePoint(String metricName, float value, Map<String, String> tags, long timestamp) throws IOException {
        doPutRequest(buildRequestJson(metricName, String.valueOf(value), tags, timestamp), ContentType.APPLICATION_JSON);
    }

    @Override
    public void writePoint(String metricName, int value, Map<String, String> tags, long timestamp) throws IOException {
        doPutRequest(buildRequestJson(metricName, String.valueOf(value), tags, timestamp), ContentType.APPLICATION_JSON);
    }

    @Override
    protected Request requestForPut() {
        if (apiPath == null) {
            throw new IllegalStateException("Seems like request occurred before initializing");
        }

        return Request.Post(apiPath);
    }

    @Override
    protected boolean isError(StatusLine statusLine) {
        // if operation is succeed, it returns 204 and no content is returned
        // otherwise it returns 400
        // it may be other codes, but we don't need to care about cause it indicates failed operation anyway
        return statusLine.getStatusCode() != 204;
    }

    private String createPathForPut(String host, int port) {
        return String.format("http://%s:%d%s", host, port, PUT_API_PATH);
    }

    private String buildRequestJson(String metricName, String valueStr, Map<String, String> tags, long timestamp)
            throws JsonProcessingException {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("metric", metricName);
        requestData.put("timestamp", timestamp);
        requestData.put("value", valueStr);
        requestData.put("tags", tags);

        return mapper.writeValueAsString(requestData);
    }
}
