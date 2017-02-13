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
package com.hortonworks.streamline.streams.metrics.storm.opentsdb;

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
import static com.hortonworks.streamline.streams.metrics.storm.opentsdb.OpenTSDBWithStormQuerier.QUERY_API_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OpenTSDBWithStormQuerierTest {
    // FIXME: add tests like other querier tests
    private final String TEST_QUERY_API_PATH = "/api/query";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18089);
    private OpenTSDBWithStormQuerier querier;


    @Before
    public void setUp() throws Exception {
        querier = new OpenTSDBWithStormQuerier();

        Map<String, String> conf = new HashMap<>();
        conf.put(QUERY_API_URL, "http://localhost:18089" + TEST_QUERY_API_PATH);

        querier.init(conf);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test(expected = UnsupportedOperationException.class)
    public void getMetrics() throws Exception {
        String topologyName = "testTopology";
        String componentId = "testComponent";
        String metricName = "__test.metric.name";
        TimeSeriesQuerier.AggregateFunction aggrFunction = TimeSeriesQuerier.AggregateFunction.SUM;
        long from = 1234L;
        long to = 5678L;

        querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        fail("Shouldn't be reached here");
    }


    @Test
    public void getRawMetrics() throws Exception {
        stubMetricUrl();

        String metricName = "sum:sys.cpu.user[host=web01]";
        String parameters = "";
        long from = 1234L;
        long to = 5678L;

        Map<String, Map<Long, Double>> metrics = querier.getRawMetrics(metricName, parameters, from, to);
        assertResult(metrics.get("sys.cpu.user[host=web01]"));

        verify(getRequestedFor(urlPathEqualTo(TEST_QUERY_API_PATH))
                .withQueryParam("m", equalTo("sum:sys.cpu.user[host=web01]"))
                .withQueryParam("start", equalTo("1234"))
                .withQueryParam("end", equalTo("5678")));
    }

    private void stubMetricUrl() {
        stubFor(get(urlPathEqualTo(TEST_QUERY_API_PATH))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"metric\":\"sys.cpu.user\",\"tags\":{\"host\":\"web01\"},\"aggregateTags\":[\"cpu\"],\"dps\":{\"123456\":456.789, \"567890\":890.123}}]")));
    }


    private void assertResult(Map<Long, Double> metrics) {
        assertTrue(metrics.containsKey(123456L));
        assertEquals(456.789, metrics.get(123456L), 0.00001);
        assertEquals(890.123, metrics.get(567890L), 0.00001);
    }

}
