package com.hortonworks.iotas.metric.influxdb;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

public class InfluxDBMetricsWriterTest {
    private static final int WIREMOCK_PORT = 8089;
    private static final String DB_NAME = "mydb";

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

    @Test
    public void testWritePoint() throws IOException {
        stubFor(post(urlPathEqualTo("/write"))
                .withQueryParam("db", equalTo(DB_NAME))
                .withQueryParam("precision", equalTo("ms"))
                .willReturn(aResponse().withStatus(204)));

        InfluxDBMetricsWriter writer = new InfluxDBMetricsWriter();
        HashMap<String, Object> config = new HashMap<>();
        config.put("host", "localhost");
        config.put("port", WIREMOCK_PORT);
        config.put("dbName", DB_NAME);

        writer.initialize(config);

        HashMap<String, String> tags = new HashMap<>();
        tags.put("tag1", "tagvalue1");
        tags.put("tag2", "tagvalue2");

        long timestamp = System.currentTimeMillis();
        writer.writePoint("hello", 0.1f, tags, timestamp);

        String expectedRequestBodyRegex = "hello,tag1=tagvalue1,tag2=tagvalue2 value=0\\.1 " + timestamp;
        String expectedRequestContentType = "text/plain; charset=UTF-8";

        verify(postRequestedFor(urlPathEqualTo("/write"))
                .withQueryParam("db", equalTo(DB_NAME))
                .withQueryParam("precision", equalTo("ms"))
                .withRequestBody(matching(expectedRequestBodyRegex))
                .withHeader("Content-Type", matching(expectedRequestContentType)));
    }
}
