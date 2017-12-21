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
package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.logsearch.EventSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.LogSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.topology.service.TopologyLogSearchService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Collections;
import java.util.List;

import static com.hortonworks.streamline.streams.catalog.Topology.NAMESPACE;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Resource for log/event search requests for topology
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyLoggingResource {

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final TopologyActionsService actionsService;
    private final TopologyLogSearchService logSearchService;

    public TopologyLoggingResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService,
                                   TopologyActionsService actionsService, TopologyLogSearchService logSearchService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.actionsService = actionsService;
        this.logSearchService = logSearchService;
    }

    @POST
    @Path("/topologies/{topologyId}/logconfig")
    @Timed
    public Response configureLogLevel(@PathParam("topologyId") Long topologyId,
                                      @QueryParam("targetLogLevel") TopologyActions.LogLevel targetLogLevel,
                                      @QueryParam("durationSecs") int durationSecs,
                                      @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);

        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            TopologyActions.LogLevelInformation logInfo = actionsService.configureLogLevel(topology, targetLogLevel,
                    durationSecs, WSUtils.getUserFromSecurityContext(securityContext));

            LogLevelResponse response;
            if (logInfo != null) {
                response = LogLevelResponse.succeed().of(logInfo).build();
            } else {
                response = LogLevelResponse.fail().build();
            }
            return WSUtils.respondEntity(response, OK);
        }
        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/logconfig")
    @Timed
    public Response getLogLevel(@PathParam("topologyId") Long topologyId,
                                @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);

        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            TopologyActions.LogLevelInformation logInfo = actionsService.getLogLevel(topology,
                    WSUtils.getUserFromSecurityContext(securityContext));
            LogLevelResponse response;
            if (logInfo != null) {
                response = LogLevelResponse.succeed().of(logInfo).build();
            } else {
                response = LogLevelResponse.fail().build();
            }
            return WSUtils.respondEntity(response, OK);
        }
        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/logs")
    @Timed
    public Response searchTopologyLogs(@PathParam("topologyId") Long topologyId,
                                       @QueryParam("componentName") List<String> componentNames,
                                       @QueryParam("logLevel") List<String> logLevels,
                                       @QueryParam("searchString") String searchString,
                                       @QueryParam("from") Long from,
                                       @QueryParam("to") Long to,
                                       @QueryParam("start") Integer start,
                                       @QueryParam("limit") Integer limit,
                                       @Context SecurityContext securityContext) {

        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            if (from == null) {
                throw BadRequestException.missingParameter("from");
            }
            if (to == null) {
                throw BadRequestException.missingParameter("to");
            }

            LogSearchCriteria criteria = new LogSearchCriteria(String.valueOf(topologyId),
                    componentNames, logLevels, searchString, from, to, start, limit);

            return WSUtils.respondEntity(logSearchService.search(topology, criteria), OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/events")
    @Timed
    public Response searchEvents(@PathParam("topologyId") Long topologyId,
                                 @QueryParam("componentName") List<String> componentNames,
                                 @QueryParam("searchString") String searchString,
                                 @QueryParam("searchEventId") String searchEventId,
                                 @QueryParam("from") Long from,
                                 @QueryParam("to") Long to,
                                 @QueryParam("start") Integer start,
                                 @QueryParam("limit") Integer limit,
                                 @QueryParam("ascending") Boolean ascending,
                                 @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            if (from == null) {
                throw BadRequestException.missingParameter("from");
            }
            if (to == null) {
                throw BadRequestException.missingParameter("to");
            }

            EventSearchCriteria criteria = new EventSearchCriteria(String.valueOf(topologyId),
                    componentNames, searchString, searchEventId, from, to, start, limit, ascending);

            return WSUtils.respondEntity(logSearchService.searchEvent(topology, criteria), OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    private static class LogLevelResponse {
        private TopologyActions.LogLevel logLevel;
        private Long epoch;
        private Boolean enabled;
        private Boolean success;

        private LogLevelResponse() {
        }

        public static Builder succeed() {
            return new Builder(true);
        }

        public static Builder fail() {
            return new Builder(false);
        }

        public static class Builder {
            private TopologyActions.LogLevel logLevel;
            private Long epoch;
            private Boolean enabled;
            private Boolean success;

            public Builder(boolean success) {
                this.success = success;
            }

            public Builder of(TopologyActions.LogLevelInformation logLevelInformation) {
                this.logLevel = logLevelInformation.getLogLevel();
                this.epoch = logLevelInformation.getEpoch();
                this.enabled = logLevelInformation.isEnabled();
                return this;
            }

            public Builder logLevel(TopologyActions.LogLevel logLevel) {
                this.logLevel = logLevel;
                return this;
            }

            public Builder epoch(long epoch) {
                this.epoch = epoch;
                return this;
            }

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public LogLevelResponse build() {
                LogLevelResponse response = new LogLevelResponse();
                response.success = this.success;
                response.enabled = this.enabled;
                response.logLevel = this.logLevel;
                response.epoch = this.epoch;
                return response;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public TopologyActions.LogLevel getLogLevel() {
            return logLevel;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Long getEpoch() {
            return epoch;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Boolean getEnabled() {
            return enabled;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Boolean getSuccess() {
            return success;
        }
    }
}
