package com.hortonworks.streamline.streams.service.metadata;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;

import com.hortonworks.streamline.streams.catalog.exception.EntityNotFoundException;
import com.hortonworks.streamline.streams.catalog.service.metadata.HiveMetadataService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class HiveMetadataResource {
    private final EnvironmentService environmentService;

    public HiveMetadataResource(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GET
    @Path("/clusters/{clusterId}/services/hive/databases")
    @Timed
    public Response getDatabasesByClusterId(@PathParam("clusterId") Long clusterId)
        throws Exception {
        try(final HiveMetadataService hiveMetadataService = HiveMetadataService.newInstance(environmentService, clusterId)) {
            return WSUtils.respondEntity(hiveMetadataService.getHiveDatabases(), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    @GET
    @Path("/clusters/{clusterId}/services/hive/databases/{dbName}/tables")
    @Timed
    public Response getDatabaseTablesByClusterId(@PathParam("clusterId") Long clusterId, @PathParam("dbName") String dbName)
        throws Exception {
        try(final HiveMetadataService hiveMetadataService = HiveMetadataService.newInstance(environmentService, clusterId)) {
            return WSUtils.respondEntity(hiveMetadataService.getHiveTables(dbName), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }
}
