package org.apache.streamline.streams.service.metadata;

import com.codahale.metrics.annotation.Timed;

import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.exception.EntityNotFoundException;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.catalog.service.metadata.HBaseMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_BY_NAME_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class HBaseMetadataResource {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseMetadataResource.class);
    private final StreamCatalogService catalogService;

    public HBaseMetadataResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/hbase/namespaces")
    @Timed
    public Response getNamespacesByClusterName(@PathParam("clusterName") String clusterName) {
        final Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }
        return getNamespacesByClusterId(cluster.getId());
    }

    @GET
    @Path("/clusters/{clusterId}/services/hbase/namespaces")
    @Timed
    public Response getNamespacesByClusterId(@PathParam("clusterId") Long clusterId) {
        try (HBaseMetadataService hbaseMetadataService = HBaseMetadataService.newInstance(catalogService, clusterId)) {
            return WSUtils.respond(hbaseMetadataService.getHBaseNamespaces(), OK, SUCCESS);
        } catch (EntityNotFoundException ex) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, ex.getMessage());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    // ===

    @GET
    @Path("/clusters/name/{clusterName}/services/hbase/tables")
    @Timed
    public Response getTablesByClusterName(@PathParam("clusterName") String clusterName) {
        final Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }
        return getTablesByClusterId(cluster.getId());
    }

    @GET
    @Path("/clusters/{clusterId}/services/hbase/tables")
    @Timed
    public Response getTablesByClusterId(@PathParam("clusterId") Long clusterId) {
        try (HBaseMetadataService hbaseMetadataService = HBaseMetadataService.newInstance(catalogService, clusterId)) {
            return WSUtils.respond(hbaseMetadataService.getHBaseTables(), OK, SUCCESS);
        } catch (EntityNotFoundException ex) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, ex.getMessage());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    // ===

    @GET
    @Path("/clusters/name/{clusterName}/services/hbase/namespaces/{namespace}/tables")
    @Timed
    public Response getNamespaceTablesByClusterName(@PathParam("clusterName") String clusterName, @PathParam("namespace") String namespace) {
        final Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }
        return getNamespaceTablesByClusterId(cluster.getId(), namespace);
    }

    @GET
    @Path("/clusters/{clusterId}/services/hbase/namespaces/{namespace}/tables")
    @Timed
    public Response getNamespaceTablesByClusterId(@PathParam("clusterId") Long clusterId, @PathParam("namespace") String namespace) {
        try (HBaseMetadataService hbaseMetadataService = HBaseMetadataService.newInstance(catalogService, clusterId)) {
            return WSUtils.respond(hbaseMetadataService.getHBaseTables(namespace), OK, SUCCESS);
        } catch (EntityNotFoundException ex) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, ex.getMessage());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }


}
