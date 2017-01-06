package com.hortonworks.streamline.streams.storm.common;

import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.WrappedWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import java.util.Map;

public class StormRestAPIClient {
    private static final Logger LOG = LoggerFactory.getLogger(StormRestAPIClient.class);

    public static final MediaType STORM_REST_API_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE;
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

    public Map getComponent(String topologyId, String componentId) {
        return doGetRequest(getComponentUrl(topologyId, componentId));
    }

    public boolean killTopology(String stormTopologyId, int waitTime) {
        Map result = doPostRequestWithEmptyBody(getTopologyKillUrl(stormTopologyId, waitTime));
        return isPostOperationSuccess(result);
    }

    public boolean activateTopology(String stormTopologyId) {
        Map result = doPostRequestWithEmptyBody(getTopologyActivateUrl(stormTopologyId));
        return isPostOperationSuccess(result);
    }

    public boolean deactivateTopology(String stormTopologyId) {
        Map result = doPostRequestWithEmptyBody(getTopologyDeactivateUrl(stormTopologyId));
        return isPostOperationSuccess(result);
    }

    private Map doGetRequest(String requestUrl) {
        try {
            LOG.debug("GET request to Storm cluster: " + requestUrl);
            return JsonClientUtil.getEntity(client.target(requestUrl), STORM_REST_API_MEDIA_TYPE, Map.class);
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new StormNotReachableException("Exception while requesting " + requestUrl, e);
            }

            throw e;
        } catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        }
    }

    private Map doPostRequestWithEmptyBody(String requestUrl) {
        try {
            LOG.debug("POST request to Storm cluster: " + requestUrl);
            return JsonClientUtil.postForm(client.target(requestUrl), new MultivaluedHashMap<>(), STORM_REST_API_MEDIA_TYPE, Map.class);
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new StormNotReachableException("Exception while requesting " + requestUrl, e);
            }

            throw e;
        } catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        }
    }

    private String getTopologySummaryUrl() {
        return stormApiRootUrl + "/topology/summary";
    }

    private String getTopologyUrl(String topologyId) {
        return stormApiRootUrl + "/topology/" + topologyId;
    }

    private String getComponentUrl(String topologyId, String componentId) {
        return getTopologyUrl(topologyId) + "/component/" + componentId;
    }

    private String getTopologyKillUrl(String topologyId, int waitTime) {
        return stormApiRootUrl + "/topology/" + topologyId + "/kill/" + waitTime;
    }

    private String getTopologyActivateUrl(String topologyId) {
        return stormApiRootUrl + "/topology/" + topologyId + "/activate";
    }

    private String getTopologyDeactivateUrl(String topologyId) {
        return stormApiRootUrl + "/topology/" + topologyId + "/deactivate";
    }

    private boolean isPostOperationSuccess(Map result) {
        return result != null && result.get("status").equals("success");
    }

}
