package org.apache.streamline.streams.service.metadata;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;

import org.apache.streamline.streams.catalog.exception.EntityNotFoundException;
import org.apache.streamline.streams.catalog.service.metadata.HiveMetadataService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_BY_NAME_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class HiveMetadataResource {
    private final StreamCatalogService catalogService;

    public HiveMetadataResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/hive/databases")
    @Timed
    public Response getDatabasesByClusterName(@PathParam("clusterName") String clusterName) {
        final Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }
        return getDatabasesByClusterId(cluster.getId());
    }

    @GET
    @Path("/clusters/{clusterId}/services/hive/databases")
    @Timed
    public Response getDatabasesByClusterId(@PathParam("clusterId") Long clusterId) {
        try(final HiveMetadataService hiveMetadataService = HiveMetadataService.newInstance(catalogService, clusterId)) {
            return WSUtils.respond(hiveMetadataService.getHiveDatabases(), OK, SUCCESS);
        } catch (EntityNotFoundException ex) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, ex.getMessage());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/hive/databases/{dbName}/tables")
    @Timed
    public Response getDatabaseTablesByClusterName(@PathParam("clusterName") String clusterName, @PathParam("dbName") String dbName) {
        final Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }
        return getDatabaseTablesByClusterId(cluster.getId(), dbName);
    }

    @GET
    @Path("/clusters/{clusterId}/services/hive/databases/{dbName}/tables")
    @Timed
    public Response getDatabaseTablesByClusterId(@PathParam("clusterId") Long clusterId, @PathParam("dbName") String dbName) {
        try(final HiveMetadataService hiveMetadataService = HiveMetadataService.newInstance(catalogService, clusterId)) {
            return WSUtils.respond(hiveMetadataService.getHiveTables(dbName), OK, SUCCESS);
        } catch (EntityNotFoundException ex) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, ex.getMessage());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }
}
