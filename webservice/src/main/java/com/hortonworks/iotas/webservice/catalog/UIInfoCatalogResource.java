package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.UIInfo;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Collection;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class UIInfoCatalogResource {
    private final CatalogService catalogService;

    public UIInfoCatalogResource (CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/system/uiinfos")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUIInfos () {
        try {
            Collection<UIInfo> result = catalogService.listUIInfos();
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND);
    }

    @GET
    @Path("/system/uiinfos/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUIInfoByTopologyId (@PathParam("id") Long topologyId) {
        try {
            UIInfo result = catalogService.getUIInfo(topologyId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/system/uiinfos")
    @Timed
    public Response addUIInfo (UIInfo uiInfo) {
        try {
            UIInfo addedUIInfo = catalogService.addUIInfo(uiInfo);
            return WSUtils.respond(CREATED, SUCCESS, addedUIInfo);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/system/uiinfos/{id}")
    @Timed
    public Response removeUIInfo (@PathParam("id") Long topologyId) {
        try {
            UIInfo removedUIInfo = catalogService.removeUIInfo(topologyId);
            if (removedUIInfo != null) {
                return WSUtils.respond(OK, SUCCESS, removedUIInfo);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/system/uiinfos/{id}")
    @Timed
    public Response addOrUpdateUIInfo (@PathParam("id") Long topologyId, UIInfo uiInfo) {
        try {
            UIInfo newUIInfo = catalogService.addOrUpdateUIInfo(topologyId, uiInfo);
            return WSUtils.respond(OK, SUCCESS, uiInfo);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }
}
