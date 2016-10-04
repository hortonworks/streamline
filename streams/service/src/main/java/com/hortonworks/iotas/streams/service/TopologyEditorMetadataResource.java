package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.catalog.TopologyEditorMetadata;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyEditorMetadataResource {
    private final StreamCatalogService catalogService;

    public TopologyEditorMetadataResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/system/topologyeditormetadata")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTopologyEditorMetadata () {
        try {
            Collection<TopologyEditorMetadata> result = catalogService.listTopologyEditorMetadata();
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND);
    }

    @GET
    @Path("/system/topologyeditormetadata/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologyEditorMetadataByTopologyId (@PathParam("id") Long topologyId) {
        try {
            TopologyEditorMetadata result = catalogService.getTopologyEditorMetadata(topologyId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/system/topologyeditormetadata")
    @Timed
    public Response addTopologyEditorMetadata (TopologyEditorMetadata topologyEditorMetadata) {
        try {
            TopologyEditorMetadata addedTopologyEditorMetadata = catalogService.addTopologyEditorMetadata(topologyEditorMetadata);
            return WSUtils.respond(CREATED, SUCCESS, addedTopologyEditorMetadata);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/system/topologyeditormetadata/{id}")
    @Timed
    public Response removeTopologyEditorMetadata (@PathParam("id") Long topologyId) {
        try {
            TopologyEditorMetadata removedTopologyEditorMetadata = catalogService.removeTopologyEditorMetadata(topologyId);
            if (removedTopologyEditorMetadata != null) {
                return WSUtils.respond(OK, SUCCESS, removedTopologyEditorMetadata);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/system/topologyeditormetadata/{id}")
    @Timed
    public Response addOrUpdateTopologyEditorMetadata (@PathParam("id") Long topologyId, TopologyEditorMetadata topologyEditorMetadata) {
        try {
            TopologyEditorMetadata newTopologyEditorMetadata = catalogService.addOrUpdateTopologyEditorMetadata(topologyId, topologyEditorMetadata);
            return WSUtils.respond(OK, SUCCESS, topologyEditorMetadata);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }
}
