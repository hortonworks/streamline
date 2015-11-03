package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.Component;
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

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;


@Path("/api/v1/catalog/clusters/{clusterId}/components")
@Produces(MediaType.APPLICATION_JSON)

public class ComponentCatalogResource {
    private CatalogService catalogService;

    public ComponentCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * List ALL components or the ones matching specific query params.
     */
    @GET
    @Timed
    public Response listComponents(@Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<Component> components;
            if (params.isEmpty()) {
                components = catalogService.listComponents();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                components = catalogService.listComponents(queryParams);
            }
            if (components != null && !components.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, components);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComponentById(@PathParam("id") Long componentId) {
        try {
            Component result = catalogService.getComponent(componentId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, componentId.toString());
    }

    @POST
    @Timed
    public Response addComponents(@PathParam("clusterId") Long clusterId, List<Component> components) {
        try {
            List<Component> createdComponents = new ArrayList<>();
            for (Component component : components) {
                Component createdComponent = catalogService.addComponent(clusterId, component);
                createdComponents.add(createdComponent);
            }
            return WSUtils.respond(CREATED, SUCCESS, createdComponents);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Timed
    public Response addOrUpdateComponents(@PathParam("clusterId") Long clusterId, List<Component> components) {
        try {
            List<Component> createdComponents = new ArrayList<>();
            for (Component component : components) {
                Component createdComponent = catalogService.addOrUpdateComponent(clusterId, component);
                createdComponents.add(createdComponent);
            }
            return WSUtils.respond(CREATED, SUCCESS, createdComponents);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/{id}")
    @Timed
    public Response removeComponent(@PathParam("id") Long componentId) {
        try {
            Component removeComponent = catalogService.removeComponent(componentId);
            if (removeComponent != null) {
                return WSUtils.respond(OK, SUCCESS, removeComponent);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, componentId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/{id}")
    @Timed
    public Response addOrUpdateComponent(@PathParam("clusterId") Long clusterId,
                                         @PathParam("id") Long componentId, Component component) {
        try {
            Component newComponent = catalogService.addOrUpdateComponent(clusterId, componentId, component);
            return WSUtils.respond(OK, SUCCESS, newComponent);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }
}
