package org.apache.streamline.streams.service.metadata;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.service.EnvironmentService;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;

import org.apache.streamline.streams.catalog.exception.EntityNotFoundException;
import org.apache.streamline.streams.catalog.service.metadata.HiveMetadataService;

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
    @Path("/clusters/name/{clusterName}/services/hive/databases")
    @Timed
    public Response getDatabasesByClusterName(@PathParam("clusterName") String clusterName)
        throws Exception {
        final Cluster cluster = environmentService.getClusterByName(clusterName);
        if (cluster == null) {
            throw org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException.byName("cluster name " + clusterName);
        }
        return getDatabasesByClusterId(cluster.getId());
    }

    @GET
    @Path("/clusters/{clusterId}/services/hive/databases")
    @Timed
    public Response getDatabasesByClusterId(@PathParam("clusterId") Long clusterId)
        throws Exception {
        try(final HiveMetadataService hiveMetadataService = HiveMetadataService.newInstance(environmentService, clusterId)) {
            return WSUtils.respondEntity(hiveMetadataService.getHiveDatabases(), OK);
        } catch (EntityNotFoundException ex) {
            throw org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/hive/databases/{dbName}/tables")
    @Timed
    public Response getDatabaseTablesByClusterName(@PathParam("clusterName") String clusterName, @PathParam("dbName") String dbName)
        throws Exception {
        final Cluster cluster = environmentService.getClusterByName(clusterName);
        if (cluster == null) {
            throw org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException.byName("cluster name " + clusterName);
        }
        return getDatabaseTablesByClusterId(cluster.getId(), dbName);
    }

    @GET
    @Path("/clusters/{clusterId}/services/hive/databases/{dbName}/tables")
    @Timed
    public Response getDatabaseTablesByClusterId(@PathParam("clusterId") Long clusterId, @PathParam("dbName") String dbName)
        throws Exception {
        try(final HiveMetadataService hiveMetadataService = HiveMetadataService.newInstance(environmentService, clusterId)) {
            return WSUtils.respondEntity(hiveMetadataService.getHiveTables(dbName), OK);
        } catch (EntityNotFoundException ex) {
            throw org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }
}
