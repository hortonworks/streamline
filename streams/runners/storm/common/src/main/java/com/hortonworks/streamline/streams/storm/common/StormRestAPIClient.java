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
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.storm.common.logger.LogLevelLoggerResponse;
import com.hortonworks.streamline.streams.storm.common.logger.LogLevelRequest;
import com.hortonworks.streamline.streams.storm.common.logger.LogLevelResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Map;

import static com.hortonworks.streamline.common.util.WSUtils.encode;

public class StormRestAPIClient {
    private static final Logger LOG = LoggerFactory.getLogger(StormRestAPIClient.class);

    public static final MediaType STORM_REST_API_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE;
    private final String stormApiRootUrl;
    private final Subject subject;
    private final Client client;

    public StormRestAPIClient(Client client, String stormApiRootUrl, Subject subject) {
        this.client = client;
        this.stormApiRootUrl = stormApiRootUrl;
        this.subject = subject;
    }

    public Map getTopologySummary(String asUser) {
        return doGetRequest(generateTopologyUrl(null, asUser, "summary"));
    }

    public Map getTopology(String topologyId, String asUser) {
        // topology/<topologyId>?doAsUser=<asUser>
        return doGetRequest(generateTopologyUrl(topologyId, asUser, ""));
    }

    public Map getComponent(String topologyId, String componentId, String asUser) {
        // topology/<topologyId>/component/<componentId>?doAsUser=<asUser>
        return doGetRequest(generateTopologyUrl(topologyId, asUser, "component/" + encode(componentId)));
    }

    public boolean killTopology(String stormTopologyId, String asUser, int waitTime) {
        //// topology/<topologyId>/kill/<waitTime>?doAsUser=<asUser>
        Map result = doPostRequestWithEmptyBody(generateTopologyUrl(stormTopologyId, asUser, "kill/" + waitTime));
        return isPostOperationSuccess(result);
    }

    public boolean activateTopology(String stormTopologyId, String asUser) {
        // topology/<topologyId>/activate?doAsUser=<asUser>
        Map result = doPostRequestWithEmptyBody(generateTopologyUrl(stormTopologyId, asUser, "activate"));
        return isPostOperationSuccess(result);
    }

    public boolean deactivateTopology(String stormTopologyId, String asUser) {
        // topology/<topologyId>/deactivate?doAsUser=<asUser>
        Map result = doPostRequestWithEmptyBody(generateTopologyUrl(stormTopologyId, asUser, "deactivate"));
        return isPostOperationSuccess(result);
    }

    public LogLevelResponse configureLog(String stormTopologyId, String targetPackage, String targetLogLevel,
                                int durationSecs, String asUser) {
        LogLevelRequest request = new LogLevelRequest();
        request.addLoggerRequest(targetPackage, targetLogLevel, durationSecs);

        Map result = doPostRequest(generateTopologyLogConfigUrl(stormTopologyId, asUser), request);
        return buildLogLevelResponse(result);
    }

    public LogLevelResponse getLogLevel(String stormTopologyId, String asUser) {
        Map<String, Object> logLevelResponseMap = doGetRequest(generateTopologyLogConfigUrl(stormTopologyId, asUser));
        return buildLogLevelResponse(logLevelResponseMap);
    }

    private LogLevelResponse buildLogLevelResponse(Map<String, Object> logLevelResponseMap) {
        Map<String, Object> loggerToLevel = (Map<String, Object>) logLevelResponseMap.get("namedLoggerLevels");
        if (loggerToLevel == null) {
            return new LogLevelResponse();
        }

        LogLevelResponse response = new LogLevelResponse();
        loggerToLevel.forEach((logger, level) -> {
            LogLevelLoggerResponse loggerResponse = LogLevelLoggerResponse.of((Map<String, Object>) level);
            response.addLoggerResponse(logger, loggerResponse);
        });

        return response;
    }

    private Map doGetRequest(String requestUrl) {
        try {
            LOG.debug("GET request to Storm cluster: " + requestUrl);
            return Subject.doAs(subject, new PrivilegedAction<Map>() {
                @Override
                public Map run() {
                    return JsonClientUtil.getEntity(client.target(requestUrl), STORM_REST_API_MEDIA_TYPE, Map.class);
                }
            });
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            // JsonClientUtil wraps exception, so need to compare
            if (cause instanceof javax.ws.rs.ProcessingException) {
                if (ex.getCause().getCause() instanceof IOException) {
                    throw new StormNotReachableException("Exception while requesting " + requestUrl, ex);
                }
            } else if (cause instanceof WebApplicationException) {
                throw WrappedWebApplicationException.of((WebApplicationException)cause);
            }

            throw ex;
        }
    }

    private Map doPostRequestWithEmptyBody(String requestUrl) {
        try {
            LOG.debug("POST request to Storm cluster: " + requestUrl);
            return Subject.doAs(subject, new PrivilegedAction<Map>() {
                @Override
                public Map run() {
                    return JsonClientUtil.postForm(client.target(requestUrl), new MultivaluedHashMap<>(),
                            STORM_REST_API_MEDIA_TYPE, Map.class);
                }
            });
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new StormNotReachableException("Exception while requesting " + requestUrl, e);
            }

            throw e;
        } catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        }
    }

    private Map doPostRequest(String requestUrl, Object bodyObject) {
        try {
            LOG.debug("POST request to Storm cluster: " + requestUrl);
            return Subject.doAs(subject, new PrivilegedAction<Map>() {
                @Override
                public Map run() {
                    return JsonClientUtil.postEntity(client.target(requestUrl), bodyObject,
                            STORM_REST_API_MEDIA_TYPE, Map.class);
                }
            });
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new StormNotReachableException("Exception while requesting " + requestUrl, e);
            }

            throw e;
        } catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        }
    }

    private String generateTopologyUrl(String topologyId, String asUser, String operation) {
        String baseUrl = stormApiRootUrl + "/topology";

        if(StringUtils.isNotEmpty(topologyId)) {
            baseUrl += "/" + WSUtils.encode(topologyId);
        }

        if(StringUtils.isNotEmpty(operation)) {
            baseUrl += "/" + operation;
        }

        if (StringUtils.isNotEmpty(asUser)) {
            baseUrl += "?doAsUser=" + WSUtils.encode(asUser);
        }

        return baseUrl;
    }

    private String generateTopologyLogConfigUrl(String topologyId, String asUser) {
        String baseUrl = stormApiRootUrl + "/topology";

        if(StringUtils.isNotEmpty(topologyId)) {
            baseUrl += "/" + WSUtils.encode(topologyId);
        }

        baseUrl += "/logconfig";

        if (StringUtils.isNotEmpty(asUser)) {
            baseUrl += "?doAsUser=" + WSUtils.encode(asUser);
        }

        return baseUrl;
    }


    private boolean isPostOperationSuccess(Map result) {
        return result != null && result.get("status").equals("success");
    }

}
