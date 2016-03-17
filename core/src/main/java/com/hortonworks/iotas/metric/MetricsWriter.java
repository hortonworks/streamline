package com.hortonworks.iotas.metric;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for storing metric point to time-series external storages.
 *
 * Features of Point are defined by extracting common features of point from OpenTSDB, InfluxDB, Graphite.
 */
public interface MetricsWriter {
    /**
     * Initialize any necessary resources needed for the implementation
     * @param config
     */
    void initialize(Map<String, Object> config);

    /**
     * write metric point to storage
     *
     * @param metricName metric name
     * @param value metric value
     * @param tags key-value pair of tags
     * @param timestamp timestamp, millisecond resolution
     * @throws IOException
     */
    void writePoint(String metricName, float value, Map<String, String> tags, long timestamp) throws IOException;

    /**
     * write metric point to storage
     *
     * @param metricName metric name
     * @param value metric value
     * @param tags key-value pair of tags
     * @param timestamp timestamp, millisecond resolution
     * @throws IOException
     */
    void writePoint(String metricName, int value, Map<String, String> tags, long timestamp) throws IOException;

}