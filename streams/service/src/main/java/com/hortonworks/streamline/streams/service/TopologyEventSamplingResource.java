package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.sampling.service.TopologySampling;
import com.hortonworks.streamline.streams.sampling.service.TopologySamplingService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.List;

import static com.hortonworks.streamline.streams.sampling.service.storm.StormTopologySamplingService.BYTES_TO_FETCH;
import static com.hortonworks.streamline.streams.security.Permission.EXECUTE;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Resource for event sampling requests for topology
 */
@Path("/v1/sampling")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyEventSamplingResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyEventSamplingResource.class);

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private TopologySamplingService samplingService;

    public TopologyEventSamplingResource(StreamlineAuthorizer authorizer,
                                         TopologySamplingService samplingService,
                                         StreamCatalogService catalogService) {
        this.authorizer = authorizer;
        this.samplingService = samplingService;
        this.catalogService = catalogService;
    }

    @POST
    @Path("/topologies/{topologyId}/enable/{pct}")
    @Timed
    public Response enableSampling(@PathParam("topologyId") Long topologyId,
                                   @PathParam("pct") Integer pct,
                                   @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        ensureValidSamplingPct(pct);
        if (topology != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            boolean success = samplingService.enableSampling(topology, pct, asUser);
            return WSUtils.respondEntity(SamplingResponse.of(topologyId).pct(pct).success(success).build(), OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/component/{componentId}/enable/{pct}")
    @Timed
    public Response enableSampling(@PathParam("topologyId") Long topologyId,
                                   @PathParam("componentId") Long componentId,
                                   @PathParam("pct") Integer pct,
                                   @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(topologyId, componentId);
        ensureValidSamplingPct(pct);
        if (topology != null && topologyComponent != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            boolean success = samplingService.enableSampling(topology, topologyComponent, pct, asUser);
            return WSUtils.respondEntity(SamplingResponse.of(topologyId)
                    .componentId(componentId)
                    .pct(pct)
                    .success(success)
                    .build(), OK);
        }

        throw EntityNotFoundException.byId(buildTopologyComponentId(topologyId, componentId));
    }

    @POST
    @Path("/topologies/{topologyId}/disable")
    @Timed
    public Response disableSampling(@PathParam("topologyId") Long topologyId,
                                    @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            boolean success = samplingService.disableSampling(topology, asUser);
            return WSUtils.respondEntity(SamplingResponse.of(topologyId).success(success).build(), OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/component/{componentId}/disable")
    @Timed
    public Response disableSampling(@PathParam("topologyId") Long topologyId,
                                    @PathParam("componentId") Long componentId,
                                    @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(topologyId, componentId);
        if (topology != null && topologyComponent != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            boolean success = samplingService.disableSampling(topology, topologyComponent, asUser);
            return WSUtils.respondEntity(SamplingResponse.of(topologyId)
                    .componentId(componentId)
                    .success(success)
                    .build(), OK);
        }

        throw EntityNotFoundException.byId(buildTopologyComponentId(topologyId, componentId));
    }

    @GET
    @Path("/topologies/{topologyId}")
    @Timed
    public Response samplingStatus(@PathParam("topologyId") Long topologyId,
                                    @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            TopologySampling.SamplingStatus status = samplingService.samplingStatus(topology, asUser);
            SamplingResponse response;
            if (status == null) {
                response = SamplingResponse.of(topologyId).success(false).build();
            } else {
                response = SamplingResponse.of(topologyId)
                        .success(true)
                        .enabled(status.getEnabled())
                        .pct(status.getPct())
                        .build();
            }

            return WSUtils.respondEntity(response, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/component/{componentId}")
    @Timed
    public Response samplingStatus(@PathParam("topologyId") Long topologyId,
                                   @PathParam("componentId") Long componentId,
                                   @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(topologyId, componentId);
        if (topology != null && topologyComponent != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            TopologySampling.SamplingStatus status = samplingService.samplingStatus(topology, topologyComponent, asUser);
            SamplingResponse response;
            if (status == null) {
                response = SamplingResponse.of(topologyId).success(false).build();
            } else {
                response = SamplingResponse.of(topologyId)
                        .componentId(componentId)
                        .success(true)
                        .enabled(status.getEnabled())
                        .pct(status.getPct())
                        .build();
            }

            return WSUtils.respondEntity(response, OK);
        }

        throw EntityNotFoundException.byId(buildTopologyComponentId(topologyId, componentId));
    }

    /*
       if desc == false start = 0, count =x

       start = null, count = 10, desc = true

       10 --
       15 --
       ..
  --?  85
       95
       ..
      100 --
      110 --
      ..
      ..
      200 --
     */

    @GET
    @Path("/topologies/{topologyId}/component/{componentId}/events")
    @Timed
    public Response getSampledEvents(@PathParam("topologyId") Long topologyId,
                                     @PathParam("componentId") Long componentId,
                                     @QueryParam("start") Long start,
                                     @QueryParam("length") Integer length,
                                     @QueryParam("count") Integer count,
                                     @QueryParam("desc") Boolean desc,
                                   @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(topologyId, componentId);
        if (topology != null && topologyComponent != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            TopologySampling.EventQueryParams qps = buildEventQueryParams(start, length, count, desc);
            TopologySampling.SampledEvents sampledEvents = samplingService.getSampledEvents(topology, topologyComponent, qps, asUser);
            SamplingResponse response = SamplingResponse.of(topologyId)
                    .componentId(componentId)
                    .success(true)
                    .events(sampledEvents)
                    .build();
            return WSUtils.respondEntity(response, OK);
        }

        throw EntityNotFoundException.byId(buildTopologyComponentId(topologyId, componentId));
    }

    private TopologySampling.EventQueryParams buildEventQueryParams(Long start, Integer length, Integer count, Boolean desc) {
        return new TopologySampling.EventQueryParams() {
            @Override
            public Long start() {
                return start;
            }

            @Override
            public int count() {
                return count == null ? 10 : count;
            }

            @Override
            public boolean desc() {
                return desc == null ? true : desc;
            }

            @Override
            public Integer length() {
                return length == null ? Integer.valueOf(BYTES_TO_FETCH) : length;
            }
        };
    }

    private void ensureValidSamplingPct(Integer pct) {
        if (pct < 0 || pct > 100) {
            throw new IllegalArgumentException("Invalid sampling percentage");
        }
    }

    private static class SamplingResponse {
        private Long topologyId;
        private Long componentId;
        private Integer pct;
        private Boolean enabled;
        private Boolean success;
        private TopologySampling.SampledEvents events;

        private SamplingResponse() {
        }

        public static Builder of(Long topologyId) {
            return new Builder(topologyId);
        }

        public static class Builder {
            private Long topologyId;
            private Long componentId;
            private Integer pct;
            private Boolean enabled;
            private Boolean success;
            private TopologySampling.SampledEvents events;


            public Builder(Long topologyId) {
                this.topologyId = topologyId;
            }

            public Builder componentId(Long componentId) {
                this.componentId = componentId;
                return this;
            }

            public Builder pct(Integer pct) {
                this.pct = pct;
                return this;
            }

            public Builder enabled(Boolean enabled) {
                this.enabled = enabled;
                return this;
            }
            public Builder success(Boolean success) {
                this.success = success;
                return this;
            }

            public Builder events(TopologySampling.SampledEvents events) {
                this.events = events;
                return this;
            }

            public SamplingResponse build() {
                SamplingResponse response = new SamplingResponse();
                response.topologyId = this.topologyId;
                response.componentId = this.componentId;
                response.pct = this.pct;
                response.enabled = this.enabled;
                response.success = this.success;
                response.events = this.events;
                return response;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Long getTopologyId() {
            return topologyId;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Integer getPct() {
            return pct;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Boolean getSuccess() {
            return success;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Boolean getEnabled() {
            return enabled;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Long getComponentId() {
            return componentId;
        }


        @JsonInclude(JsonInclude.Include.NON_NULL)
        public TopologySampling.SampledEvents getEvents() {
            return events;
        }
    }

    private String buildTopologyComponentId(Long topologyId, Long componentId) {
        return String.format("topology id <%d>, component id <%d>", topologyId, componentId);
    }
}
