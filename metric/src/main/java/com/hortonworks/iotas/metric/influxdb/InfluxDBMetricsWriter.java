package com.hortonworks.iotas.metric.influxdb;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.metric.HttpAPIWithStringBodyBasedMetricsWriter;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Metrics Writer for InfluxDB
 */
public class InfluxDBMetricsWriter extends HttpAPIWithStringBodyBasedMetricsWriter {
    private static final String PUT_API_PATH = "/write";
    private static final ContentType CONTENT_TYPE = ContentType.create("text/plain", Consts.UTF_8);

    private String apiPath;

    @Override
    public void initialize(Map<String, Object> config) {
        String host = (String) config.get("host");
        int port = ((Number)config.get("port")).intValue();
        String dbName = (String) config.get("dbName");

        Number connTimeout = (Number) config.get("connectTimeout");
        Number sockTimeout = (Number) config.get("socketTimeout");

        if (connTimeout != null) {
            connectTimeout = connTimeout.intValue();
        }

        if (sockTimeout != null) {
            socketTimeout = sockTimeout.intValue();
        }

        apiPath = createPathForPut(host, port, dbName);
    }

    @Override
    public void writePoint(String metricName, float value, Map<String, String> tags, long timestamp) throws IOException {
        doPutRequest(buildRequestLineProtocol(metricName, String.valueOf(value), tags, timestamp), CONTENT_TYPE);
    }

    @Override
    public void writePoint(String metricName, int value, Map<String, String> tags, long timestamp) throws IOException {
        doPutRequest(buildRequestLineProtocol(metricName, String.valueOf(value), tags, timestamp), CONTENT_TYPE);
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

    private String createPathForPut(String host, int port, String dbName) {
        // Note: timestamp will have millisecond precision, where influxdb itself supports microsecond as default
        return String.format("http://%s:%d%s?db=%s&precision=ms", host, port, PUT_API_PATH, dbName);
    }

    private String buildRequestLineProtocol(String metricName, String valueStr, Map<String, String> tags, long timestamp) {
        List<String> seriesComponents = Lists.newArrayList();
        seriesComponents.add(metricName);

        for (Map.Entry<String, String> tagEntry : tags.entrySet()) {
            seriesComponents.add(tagEntry.getKey() + "=" + tagEntry.getValue());
        }

        String seriesName = Joiner.on(",").join(seriesComponents);

        return String.format("%s value=%s %d", seriesName, valueStr, timestamp);
    }
}
