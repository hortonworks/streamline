package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.Cluster;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterCatalogResource {
    private CatalogService catalogService;

    public ClusterCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
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

}
