package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.FileStorage;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.catalog.Cluster;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterCatalogResource.class);
    private final StreamCatalogService catalogService;
    private final FileStorage fileStorage;

    public ClusterCatalogResource(StreamCatalogService catalogService, FileStorage fileStorage) {
        this.catalogService = catalogService;
        this.fileStorage = fileStorage;
    }

    /**
     * List ALL clusters or the ones matching specific query params.
     */
    @GET
    @Path("/clusters")
    @Timed
    public Response listClusters(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<Cluster> clusters;
            if (params.isEmpty()) {
                clusters = catalogService.listClusters();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                clusters = catalogService.listClusters(queryParams);
            }
            if (clusters != null) {
                return WSUtils.respond(clusters, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/clusters/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClusterById(@PathParam("id") Long clusterId) {
        try {
            Cluster result = catalogService.getCluster(clusterId);
            if (result != null) {
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, clusterId.toString());
    }

    // curl -X POST 'http://localhost:8080/api/v1/catalog/clusters' -F clusterConfigFile=/tmp/hdfs-site.xml
    // -F cluster='{"name":"testcluster", "type":"KAFKA", "clusterConfigFileName":"hdfs-site.xml"};type=application/json'
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/clusters")
    public Response addCluster(@FormDataParam("clusterConfigFile") final InputStream inputStream,
                               @FormDataParam("clusterConfigFile") final FormDataContentDisposition contentDispositionHeader,
                               @FormDataParam("cluster") final FormDataBodyPart clusterConfig) {
        try {
            LOG.debug("Media type {}", clusterConfig.getMediaType());
            if (!clusterConfig.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return WSUtils.respond(UNSUPPORTED_MEDIA_TYPE, ResponseMessage.UNSUPPORTED_MEDIA_TYPE);
            }
            Cluster clusterObj = clusterConfig.getValueAs(Cluster.class);
            saveClusterConfig(inputStream, clusterObj);
            Cluster createdCluster = catalogService.addCluster(clusterObj);
            return WSUtils.respond(createdCluster, CREATED, SUCCESS);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/clusters/{id}")
    @Timed
    public Response removeCluster(@PathParam("id") Long clusterId) {
        try {
            Cluster removedCluster = catalogService.removeCluster(clusterId);
            if (removedCluster != null) {
                return WSUtils.respond(removedCluster, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, clusterId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/clusters/{id}")
    @Timed
    public Response addOrUpdateCluster(@PathParam("id") Long clusterId,
                                       @FormDataParam("clusterConfigFile") final InputStream inputStream,
                                       @FormDataParam("clusterConfigFile") final FormDataContentDisposition contentDispositionHeader,
                                       @FormDataParam("cluster") final FormDataBodyPart clusterConfig) {
        try {
            LOG.debug("Media type {}", clusterConfig.getMediaType());
            if (!clusterConfig.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return WSUtils.respond(UNSUPPORTED_MEDIA_TYPE, ResponseMessage.UNSUPPORTED_MEDIA_TYPE);
            }
            Cluster clusterObj = clusterConfig.getValueAs(Cluster.class);
            saveClusterConfig(inputStream, clusterObj);
            Cluster newCluster = catalogService.addOrUpdateCluster(clusterId, clusterObj);
            return WSUtils.respond(newCluster, OK, SUCCESS);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private void saveClusterConfig(InputStream is, Cluster cluster) throws IOException {
        String clusterConfigFileName = cluster.getClusterConfigFileName();
        if (clusterConfigFileName != null) {
            if (is != null) {
                String storageName = cluster.getClusterConfigFileName() + "-" + UUID.randomUUID().toString();
                String uploadedPath = this.fileStorage.uploadFile(is, storageName);
                cluster.setClusterConfigStorageName(storageName);
                LOG.debug("{} uploaded to {}", clusterConfigFileName, uploadedPath);
            } else {
                String message = String.format("clusterConfigFileName %s content is missing.", clusterConfigFileName);
                LOG.error(message);
                throw new IllegalArgumentException(message);
            }
        }
    }

}
