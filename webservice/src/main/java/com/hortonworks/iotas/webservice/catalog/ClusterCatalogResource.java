package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.Cluster;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterCatalogResource {
    private CatalogService catalogService;

    public ClusterCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * List ALL clusters or the ones matching specific query params.
     */
    @GET
    @Path("/clusters")
    @Timed
    public Response listClusters(@Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<Cluster> clusters;
            if (params.isEmpty()) {
                clusters = catalogService.listClusters();
            } else {
                for (String param : params.keySet()) {
                    queryParams.add(new CatalogService.QueryParam(param, params.getFirst(param)));
                }
                clusters = catalogService.listClusters(queryParams);
            }
            if(clusters != null && ! clusters.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, clusters);
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
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, clusterId.toString());
    }


    @POST
    @Path("/clusters")
    @Timed
    public Response addCluster(Cluster cluster) {
        try {
            Cluster createdCluster = catalogService.addCluster(cluster);
            return WSUtils.respond(CREATED, SUCCESS, createdCluster);
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
                return WSUtils.respond(OK, SUCCESS, removedCluster);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, clusterId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/clusters/{id}")
    @Timed
    public Response addOrUpdateCluster(@PathParam("id") Long clusterId, Cluster cluster) {
        try {
            Cluster newCluster = catalogService.addOrUpdateCluster(clusterId, cluster);
            return WSUtils.respond(OK, SUCCESS, newCluster);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

}
