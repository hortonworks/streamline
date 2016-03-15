package com.hortonworks.iotas.metric;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;

public abstract class HttpAPIWithStringBodyBasedMetricsWriter implements MetricsWriter {
    private static final int DEFAULT_TIMEOUT = 2000;

    protected int connectTimeout = DEFAULT_TIMEOUT;
    protected int socketTimeout = DEFAULT_TIMEOUT;

    protected final Executor executor;

    public HttpAPIWithStringBodyBasedMetricsWriter() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        cm.setDefaultMaxPerRoute(15);
        executor = Executor.newInstance(client);
    }

    protected abstract Request requestForPut();
    protected abstract boolean isError(StatusLine statusLine);

    protected void doPutRequest(String requestBody, ContentType contentType) throws IOException {
        Request request = requestForPut();
        request = request.connectTimeout(connectTimeout).socketTimeout(socketTimeout)
                .bodyString(requestBody, contentType);
        HttpResponse httpResponse = executor.execute(request).returnResponse();

        StatusLine statusLine = httpResponse.getStatusLine();
        if (isError(statusLine)) {
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
    }
}
