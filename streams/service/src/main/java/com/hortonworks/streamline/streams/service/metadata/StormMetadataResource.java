package com.hortonworks.streamline.streams.service.metadata;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.exception.EntityNotFoundException;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.metadata.StormMetadataService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Collections;

import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class StormMetadataResource {
    private final EnvironmentService environmentService;

    public StormMetadataResource(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/storm/topologies")
    @Timed
    public Response getTopologiesByClusterName(@PathParam("clusterName") String clusterName) {
        final Cluster cluster = environmentService.getClusterByName(clusterName);
        if (cluster == null) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byName("cluster name " + clusterName);
        }
        return getTopologiesByClusterId(cluster.getId());
    }

    @GET
    @Path("/clusters/{clusterId}/services/storm/topologies")
    @Timed
    public Response getTopologiesByClusterId(@PathParam("clusterId") Long clusterId) {
        try {
            StormMetadataService stormMetadataService = new StormMetadataService.Builder(environmentService, clusterId).build();
            return WSUtils.respondEntity(stormMetadataService.getTopologies(), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    @GET
    @Path("/clusters/{clusterId}/services/storm/mainpage/url")
    @Timed
    public Response getMainPageByClusterId(@PathParam("clusterId") Long clusterId) {
        try {
            StormMetadataService stormMetadataService = new StormMetadataService.Builder(environmentService, clusterId).build();
            return WSUtils.respondEntity(Collections.singletonMap("url", stormMetadataService.getMainPageUrl()), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }
}
