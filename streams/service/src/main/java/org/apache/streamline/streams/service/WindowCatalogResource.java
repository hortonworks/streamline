package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.RuleInfo;
import org.apache.streamline.streams.catalog.WindowInfo;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.streamline.common.util.WSUtils.buildTopologyIdAndVersionIdAwareQueryParams;

/**
 * REST resource for managing window rule with aggregate function.
 * <p>
 * A separate endpoint is provided for UI to have a separate endpoint
 * to configure window with aggregate functions.
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class WindowCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(WindowCatalogResource.class);

    private final StreamCatalogService catalogService;

    public WindowCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/topologies/{topologyId}/windows")
    @Timed
    public Response listTopologyWindows(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        Long currentVersionId = catalogService.getCurrentTopologyVersionId(topologyId);
        return listTopologyWindows(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/windows")
    @Timed
    public Response listTopologySourcesForVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("versionId") Long versionId,
                                                  @Context UriInfo uriInfo) {
        return listTopologyWindows(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo));
    }

    private Response listTopologyWindows(List<QueryParam> queryParams) {
        try {
            Collection<WindowInfo> windowInfos = catalogService.listWindows(queryParams);
            if (windowInfos != null) {
                return WSUtils.respond(windowInfos, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/windows/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologyWindowById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long windowId) {
        try {
            WindowInfo windowInfo = catalogService.getWindow(topologyId, windowId);
            if (windowInfo != null) {
                return WSUtils.respond(windowInfo, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, windowId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/windows/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologyWindowByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                    @PathParam("id") Long windowId,
                                                    @PathParam("versionId") Long versionId) {
        try {
            WindowInfo windowInfo = catalogService.getWindow(topologyId, windowId, versionId);
            if (windowInfo != null) {
                return WSUtils.respond(windowInfo, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, windowId));
    }

    @POST
    @Path("/topologies/{topologyId}/windows")
    @Timed
    public Response addTopologyWindow(@PathParam("topologyId") Long topologyId, WindowInfo windowInfo) {
        try {
            WindowInfo createdWindowInfo = catalogService.addWindow(topologyId, windowInfo);
            return WSUtils.respond(createdWindowInfo, CREATED, SUCCESS);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/topologies/{topologyId}/windows/{id}")
    @Timed
    public Response addOrUpdateWindow(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                    WindowInfo windowInfo) {
        try {
            WindowInfo createdWindowInfo = catalogService.addOrUpdateWindow(topologyId, ruleId, windowInfo);
            return WSUtils.respond(createdWindowInfo, CREATED, SUCCESS);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/topologies/{topologyId}/windows/{id}")
    @Timed
    public Response removeWindowById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long windowId) {
        try {
            WindowInfo windowInfo = catalogService.removeWindow(topologyId, windowId);
            if (windowInfo != null) {
                return WSUtils.respond(windowInfo, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, windowId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private String buildMessageForCompositeId(Long topologyId, Long ruleId) {
        return String.format("topology id <%d>, window id <%d>", topologyId, ruleId);
    }

}
