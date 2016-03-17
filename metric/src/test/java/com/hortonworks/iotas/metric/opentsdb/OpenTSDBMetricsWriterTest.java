package com.hortonworks.iotas.metric.opentsdb;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

public class OpenTSDBMetricsWriterTest {
    private static final int WIREMOCK_PORT = 8089;

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

    @Test
    public void testWritePoint() throws IOException {
        String urlPath = "/api/put";
        stubFor(post(urlPathEqualTo(urlPath))
                .willReturn(aResponse()
                        .withStatus(204)));

        OpenTSDBMetricsWriter writer = new OpenTSDBMetricsWriter();
        HashMap<String, Object> config = new HashMap<>();
        config.put("host", "localhost");
        config.put("port", WIREMOCK_PORT);

        writer.initialize(config);

        HashMap<String, String> tags = new HashMap<>();
        tags.put("tag1", "tagvalue1");
        tags.put("tag2", "tagvalue2");

        long timestamp = System.currentTimeMillis();
        writer.writePoint("hello", 0.1f, tags, timestamp);

        String expectedRequestContentType = "application/json; charset=UTF-8";

        String expectedRequestBodyJson = "{\"metric\":\"hello\",\"value\":\"0.1\",\"timestamp\":" +
                timestamp + ",\"tags\":{\"tag1\":\"tagvalue1\",\"tag2\":\"tagvalue2\"}}";

        verify(postRequestedFor(urlPathEqualTo(urlPath))
                .withRequestBody(equalToJson(expectedRequestBodyJson))
                .withHeader("Content-Type", matching(expectedRequestContentType)));
    }
}
