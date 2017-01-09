package com.hortonworks.streamline.streams.metrics.storm.ambari;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.hortonworks.streamline.streams.metrics.storm.ambari.AmbariMetricsServiceWithStormQuerier.COLLECTOR_API_URL;
import static com.hortonworks.streamline.streams.metrics.storm.ambari.AmbariMetricsServiceWithStormQuerier.DEFAULT_APP_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AmbariMetricsServiceWithStormQuerierTest {
    private final String TEST_COLLECTOR_API_PATH = "/ws/v1/timeline/metrics";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18089);
    private AmbariMetricsServiceWithStormQuerier querier;

    @Before
    public void setUp() throws Exception {
        querier = new AmbariMetricsServiceWithStormQuerier();

        Map<String, String> conf = new HashMap<>();
        conf.put(COLLECTOR_API_URL, "http://localhost:18089" + TEST_COLLECTOR_API_PATH);

        querier.init(conf);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void getMetrics() throws Exception {
        stubMetricUrl();

        String topologyName = "testTopology";
        String componentId = "testComponent";
        String metricName = "__test.metric.name";
        TimeSeriesQuerier.AggregateFunction aggrFunction = TimeSeriesQuerier.AggregateFunction.SUM;
        long from = 1234L;
        long to = 5678L;

        Map<Long, Double> metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--test.metric.name"))
                .withQueryParam("startTime", equalTo("1234"))
                .withQueryParam("endTime", equalTo("5678"))
                .withQueryParam("seriesAggregateFunction", equalTo("SUM")));
    }

    @Test
    public void getMetricsWithStreamAggregation() throws Exception {
        stubMetricUrl();

        String topologyName = "testTopology";
        String componentId = "testComponent";
        // this is one of metric which needs stream aggregation
        String metricName = "__complete-latency";
        TimeSeriesQuerier.AggregateFunction aggrFunction = TimeSeriesQuerier.AggregateFunction.AVG;
        long from = 1234L;
        long to = 5678L;

        Map<Long, Double> metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--complete-latency.%"))
                .withQueryParam("startTime", equalTo("1234"))
                .withQueryParam("endTime", equalTo("5678"))
                .withQueryParam("seriesAggregateFunction", equalTo("AVG")));
    }

    @Test
    public void getRawMetrics() throws Exception {
        stubMetricUrl();

        String metricName = "metric";
        String parameters = "precision=seconds,appId=appId";
        long from = 1234L;
        long to = 5678L;

        Map<String, Map<Long, Double>> metrics = querier.getRawMetrics(metricName, parameters, from, to);
        assertResult(metrics.get("metric"));

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo("appId"))
                .withQueryParam("metricNames", equalTo("metric"))
                .withQueryParam("startTime", equalTo("1234"))
                .withQueryParam("endTime", equalTo("5678")));
    }


    private void stubMetricUrl() {
        stubFor(get(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"metrics\": [ {\"metricname\": \"metric\", \"metrics\": { \"123456\": 456.789, \"567890\": 890.123 } } ] }")));
    }

    private void assertResult(Map<Long, Double> metrics) {
        assertTrue(metrics.containsKey(123456L));
        assertEquals(456.789, metrics.get(123456L), 0.00001);
        assertEquals(890.123, metrics.get(567890L), 0.00001);
    }

}