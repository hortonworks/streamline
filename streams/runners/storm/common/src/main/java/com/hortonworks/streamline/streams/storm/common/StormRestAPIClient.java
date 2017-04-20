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
package com.hortonworks.streamline.streams.storm.common;

import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.WrappedWebApplicationException;
import org.apache.commons.lang.StringUtils;
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

    public Map getTopologySummary(String asUser) {
        return doGetRequest(getTopologySummaryUrl(asUser));
    }

    public Map getTopology(String topologyId, String asUser) {
        return doGetRequest(getTopologyUrl(topologyId, asUser));
    }

    public Map getComponent(String topologyId, String componentId, String asUser) {
        return doGetRequest(getComponentUrl(topologyId, componentId, asUser));
    }

    public boolean killTopology(String stormTopologyId, String asUser, int waitTime) {
        Map result = doPostRequestWithEmptyBody(getTopologyKillUrl(stormTopologyId, asUser, waitTime));
        return isPostOperationSuccess(result);
    }

    public boolean activateTopology(String stormTopologyId, String asUser) {
        Map result = doPostRequestWithEmptyBody(getTopologyActivateUrl(stormTopologyId, asUser));
        return isPostOperationSuccess(result);
    }

    public boolean deactivateTopology(String stormTopologyId, String asUser) {
        Map result = doPostRequestWithEmptyBody(getTopologyDeactivateUrl(stormTopologyId, asUser));
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

    private String getTopologySummaryUrl(String asUser) {
        String baseUrl = stormApiRootUrl + "/topology/summary";
        if (StringUtils.isNotEmpty(asUser)) {
            baseUrl += "?doAsUser=" + asUser;
        }
        return baseUrl;
    }

    private String getTopologyUrl(String topologyId, String asUser) {
        String baseUrl = stormApiRootUrl + "/topology/" + topologyId;
        if (StringUtils.isNotEmpty(asUser)) {
            baseUrl += "?doAsUser=" + asUser;
        }
        return baseUrl;
    }

    private String getComponentUrl(String topologyId, String componentId, String asUser) {
        String baseUrl = getTopologyUrl(topologyId, asUser) + "/component/" + componentId;
        if (StringUtils.isNotEmpty(asUser)) {
            baseUrl += "?doAsUser=" + asUser;
        }
        return baseUrl;
    }

    private String getTopologyKillUrl(String topologyId, String asUser, int waitTime) {
        String baseUrl = stormApiRootUrl + "/topology/" + topologyId + "/kill/" + waitTime;
        if (StringUtils.isNotEmpty(asUser)) {
            baseUrl += "?doAsUser=" + asUser;
        }
        return baseUrl;
    }

    private String getTopologyActivateUrl(String topologyId, String asUser) {
        String baseUrl = stormApiRootUrl + "/topology/" + topologyId + "/activate";
        if (StringUtils.isNotEmpty(asUser)) {
            baseUrl += "?doAsUser=" + asUser;
        }
        return baseUrl;
    }

    private String getTopologyDeactivateUrl(String topologyId, String asUser) {
        String baseUrl = stormApiRootUrl + "/topology/" + topologyId + "/deactivate";
        if (StringUtils.isNotEmpty(asUser)) {
            baseUrl += "?doAsUser=" + asUser;
        }
        return baseUrl;
    }

    private boolean isPostOperationSuccess(Map result) {
        return result != null && result.get("status").equals("success");
    }

}
