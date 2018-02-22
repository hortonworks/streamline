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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        assertResult(metrics, aggrFunction);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--test.metric.name"))
                .withQueryParam("startTime", equalTo("1234"))
                .withQueryParam("endTime", equalTo("5678")));
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
        assertResult(metrics, aggrFunction);

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo(DEFAULT_APP_ID))
                .withQueryParam("hostname", equalTo(""))
                .withQueryParam("metricNames", equalTo("topology.testTopology.testComponent.%.--complete-latency.%"))
                .withQueryParam("startTime", equalTo("1234"))
                .withQueryParam("endTime", equalTo("5678")));
    }

    @Test
    public void getRawMetrics() throws Exception {
        stubMetricUrlForRawMetric();

        String metricName = "metric";
        String parameters = "precision=seconds,appId=appId";
        long from = 1234L;
        long to = 5678L;

        Map<String, Map<Long, Double>> metrics = querier.getRawMetrics(metricName, parameters, from, to);
        assertRawMetricResult(metrics.get("metric"));

        verify(getRequestedFor(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withQueryParam("appId", equalTo("appId"))
                .withQueryParam("metricNames", equalTo("metric"))
                .withQueryParam("startTime", equalTo("1234"))
                .withQueryParam("endTime", equalTo("5678")));
    }

    private void stubMetricUrl() throws JsonProcessingException {
        Map<String, List<Map<String, ?>>> stubBodyMap = new HashMap<>();

        List<Map<String, ?>> metrics = new ArrayList<>();

        // system stream
        Map<String, Object> metric1 = new HashMap<>();
        metric1.put("metricname", "topology.streamline-1-Topology.1-RULE.host1.6700.-1.--emit-count.--ack-ack");
        metric1.put("metrics", getTestTimestampToValueMap());
        metrics.add(metric1);

        // system stream
        Map<String, Object> metric2 = new HashMap<>();
        metric2.put("metricname", "topology.streamline-1-Topology.1-RULE.host1.6700.-1.--emit-count.--system");
        metric2.put("metrics", getTestTimestampToValueMap());
        metrics.add(metric2);

        // system stream
        Map<String, Object> metric3 = new HashMap<>();
        metric3.put("metricname", "topology.streamline-1-Topology.1-RULE.host1.6700.-1.--emit-count.--ack-init");
        metric3.put("metrics", getTestTimestampToValueMap());
        metrics.add(metric3);

        // system stream
        Map<String, Object> metric4 = new HashMap<>();
        metric4.put("metricname", "topology.streamline-1-Topology.1-RULE.host1.6700.-1.--emit-count.--metric");
        metric4.put("metrics", getTestTimestampToValueMap());
        metrics.add(metric4);

        // non-system stream
        Map<String, Object> metric5 = new HashMap<>();
        metric5.put("metricname", "topology.streamline-1-Topology.1-RULE.host1.6700.-1.--emit-count.stream1");
        metric5.put("metrics", getTestTimestampToValueMap());
        metrics.add(metric5);

        // non-system stream
        Map<String, Object> metric6 = new HashMap<>();
        metric6.put("metricname", "topology.streamline-1-Topology.1-RULE.host1.6700.-1.--emit-count.stream2");
        metric6.put("metrics", getTestTimestampToValueMap());
        metrics.add(metric6);

        stubBodyMap.put("metrics", metrics);

        ObjectMapper objectMapper = new ObjectMapper();
        String body = objectMapper.writeValueAsString(stubBodyMap);

        stubFor(get(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private void stubMetricUrlForRawMetric() {
        stubFor(get(urlPathEqualTo(TEST_COLLECTOR_API_PATH))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"metrics\": [ {\"metricname\": \"metric\", \"metrics\": { \"123456\": 456.789, \"567890\": 890.123 } } ] }")));
    }

    private void assertRawMetricResult(Map<Long, Double> metrics) {
        assertTrue(metrics.containsKey(123456L));
        assertTrue(metrics.containsKey(567890L));
        assertEquals(456.789, metrics.get(123456L), 0.00001);
        assertEquals(890.123, metrics.get(567890L), 0.00001);
    }

    private void assertResult(Map<Long, Double> metrics, TimeSeriesQuerier.AggregateFunction aggrFunction) {
        assertTrue(metrics.containsKey(123456L));
        assertTrue(metrics.containsKey(567890L));

        switch (aggrFunction) {
            case SUM:
                assertEquals(123.456 * 2, metrics.get(123456L), 0.00001);
                assertEquals(456.789 * 2, metrics.get(567890L), 0.00001);
                break;

            case AVG:
            case MAX:
            case MIN:
                assertEquals(123.456, metrics.get(123456L), 0.00001);
                assertEquals(456.789, metrics.get(567890L), 0.00001);
                break;

            default:
                throw new IllegalArgumentException("Not supported aggregated function.");

        }
    }

    private Map<String, Double> getTestTimestampToValueMap() {
        Map<String, Double> timestampToValueMap1 = new HashMap<>();
        timestampToValueMap1.put("123456", 123.456);
        timestampToValueMap1.put("567890", 456.789);
        return timestampToValueMap1;
    }

}