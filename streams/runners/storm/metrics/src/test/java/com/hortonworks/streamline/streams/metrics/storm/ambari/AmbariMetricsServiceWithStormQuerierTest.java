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

        // 10 mins: > 1 min but < 7 days
        long from = 0;
        long to = 10 * 60 * 1000;

        Map<Long, Double> metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--test.metric.name._sum"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.MINUTES.name()))
                .withQueryParam("seriesAggregateFunction", equalTo("SUM")));

        // 10 days: > 7 days but < 30 days
        from = 0;
        to = 10 * 24 * 60 * 60 * 1000;

        metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--test.metric.name._sum"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.HOURS.name()))
                .withQueryParam("seriesAggregateFunction", equalTo("SUM")));

        // 40 days: > 30 days
        from = 0;
        to = 40L * 24 * 60 * 60 * 1000;

        metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--test.metric.name._sum"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.DAYS.name()))
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

        // 10 mins: > 1 min but < 7 days
        long from = 0;
        long to = 10 * 60 * 1000;

        Map<Long, Double> metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--complete-latency.%._avg"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.MINUTES.name()))
                .withQueryParam("seriesAggregateFunction", equalTo("AVG")));

        // 10 days: > 7 days but < 30 days
        from = 0;
        to = 10 * 24 * 60 * 60 * 1000;
        aggrFunction = TimeSeriesQuerier.AggregateFunction.MAX;

        metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--complete-latency.%._max"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.HOURS.name()))
                .withQueryParam("seriesAggregateFunction", equalTo("MAX")));

        // 40 days: > 30 days
        from = 0;
        to = 40L * 24 * 60 * 60 * 1000;
        aggrFunction = TimeSeriesQuerier.AggregateFunction.MIN;

        metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--complete-latency.%._min"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.DAYS.name()))
                .withQueryParam("seriesAggregateFunction", equalTo("MIN")));
    }

    @Test
    public void getRawMetrics() throws Exception {
        stubMetricUrl();

        String metricName = "metric";
        String parameters = "precision=seconds,appId=appId";

        // 10 mins: > 1 min but < 7 days
        long from = 0;
        long to = 10 * 60 * 1000;

        Map<String, Map<Long, Double>> metrics = querier.getRawMetrics(metricName, parameters, from, to);
        assertResult(metrics.get("metric"));

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo("appId"))
                .withQueryParam("metricNames", equalTo("metric"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.MINUTES.name()))
        );

        // 10 days: > 7 days but < 30 days
        from = 0;
        to = 10 * 24 * 60 * 60 * 1000;

        metrics = querier.getRawMetrics(metricName, parameters, from, to);
        assertResult(metrics.get("metric"));

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo("appId"))
                .withQueryParam("metricNames", equalTo("metric"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.HOURS.name()))
        );

        // 40 days: > 30 days
        from = 0;
        to = 40L * 24 * 60 * 60 * 1000;

        metrics = querier.getRawMetrics(metricName, parameters, from, to);
        assertResult(metrics.get("metric"));

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo("appId"))
                .withQueryParam("metricNames", equalTo("metric"))
                .withQueryParam("startTime", equalTo(String.valueOf(from)))
                .withQueryParam("endTime", equalTo(String.valueOf(to)))
                .withQueryParam("precision", equalTo(AmbariMetricsServiceWithStormQuerier.Precision.DAYS.name()))
        );
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