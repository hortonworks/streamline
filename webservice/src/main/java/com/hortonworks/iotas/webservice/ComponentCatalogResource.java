package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.Component;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("/api/v1/catalog/clusters/{clusterId}")
@Produces(MediaType.APPLICATION_JSON)

public class ComponentCatalogResource {
    private CatalogService catalogService;

    public ComponentCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @POST
    @Path("/components")
    @Timed
    public Response addComponent(@PathParam("clusterId") Long clusterId, Component component) {
        try {
            Component createdComponent = catalogService.addComponent(clusterId, component);
            return WSUtils.respond(CREATED, SUCCESS, createdComponent);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

}
