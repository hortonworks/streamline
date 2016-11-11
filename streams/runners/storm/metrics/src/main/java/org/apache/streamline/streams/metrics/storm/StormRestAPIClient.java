package org.apache.streamline.streams.metrics.storm;

import org.apache.streamline.streams.metrics.storm.topology.StormNotReachableException;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

public class StormRestAPIClient {
    private final String stormApiRootUrl;
    private final Client client;

    public StormRestAPIClient(Client client, String stormApiRootUrl) {
        this.client = client;
        this.stormApiRootUrl = stormApiRootUrl;
    }

    public Map getTopologySummary() {
        return doGetRequest(getTopologySummaryUrl());
    }

    public Map getTopology(String topologyId) {
        return doGetRequest(getTopologyUrl(topologyId));
    }

    private Map doGetRequest(String requestUrl) {
        try {
            return client.target(requestUrl).request(MediaType.APPLICATION_JSON_TYPE).get(Map.class);
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new StormNotReachableException("Exception while requesting " + requestUrl, e);
            }

            throw e;
        }
    }

    private String getTopologySummaryUrl() {
        return stormApiRootUrl + "/topology/summary";
    }

    private String getTopologyUrl(String topologyId) {
        return stormApiRootUrl + "/topology/" + topologyId;
    }
}
