package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.catalog.RuleInfo;
import com.hortonworks.iotas.streams.catalog.WindowDto;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
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
import java.util.Collections;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST resource for managing window rule with aggregate function.
 * <p>
 * A separate endpoint is provided for UI to have a separate endpoint
 * to configure window with aggregate functions.
 */
@Path("/api/v1/catalog/topologies/{topologyId}/windows")
@Produces(MediaType.APPLICATION_JSON)
public class WindowCatalogResource {
    private final StreamCatalogService catalogService;

    public WindowCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Timed
    public Response listTopologyWindows(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAwareQueryParams(topologyId, uriInfo);
        try {
            Collection<RuleInfo> ruleInfos = catalogService.listRules(queryParams);
            if (ruleInfos != null) {
                Collection<RuleInfo> windowRules = getWindows(ruleInfos);
                Collection<WindowDto> windowDtos = Collections2.transform(windowRules, new Function<RuleInfo, WindowDto>() {
                    @Nullable
                    @Override
                    public WindowDto apply(@Nullable RuleInfo ruleInfo) {
                        return new WindowDto(ruleInfo);
                    }
                });
                return WSUtils.respond(OK, SUCCESS, windowDtos);
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
    public Response getTopologyWindowById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long windowId) {
        try {
            RuleInfo ruleInfo = getWindow(catalogService.getRule(windowId));
            if (ruleInfo != null && ruleInfo.getTopologyId().equals(topologyId)) {
                return WSUtils.respond(OK, SUCCESS, new WindowDto(ruleInfo));
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, windowId));
    }

    @POST
    @Timed
    public Response addTopologyWindow(@PathParam("topologyId") Long topologyId, WindowDto windowDto) {
        try {
            RuleInfo createdRuleInfo = catalogService.addRule(topologyId, getRuleInfo(windowDto));
            return WSUtils.respond(CREATED, SUCCESS, new WindowDto(createdRuleInfo));
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/{id}")
    @Timed
    public Response addOrUpdateRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                    WindowDto windowDto) {
        try {
            RuleInfo createdRuleInfo = catalogService.addOrUpdateRule(topologyId, ruleId, getRuleInfo(windowDto));
            return WSUtils.respond(CREATED, SUCCESS, new WindowDto(createdRuleInfo));
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/{id}")
    @Timed
    public Response removeWindowById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long windowId) {
        try {
            RuleInfo ruleInfo = null;
            if (getWindow(catalogService.getRule(windowId)) != null) {
                ruleInfo = catalogService.removeRule(windowId);
            }
            if (ruleInfo != null) {
                return WSUtils.respond(OK, SUCCESS, new WindowDto(ruleInfo));
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

    private RuleInfo getRuleInfo(WindowDto windowDto) {
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setId(windowDto.getId());
        ruleInfo.setName(windowDto.getName());
        ruleInfo.setDescription(windowDto.getDescription());
        ruleInfo.setStreams(windowDto.getStreams());
        ruleInfo.setProjections(windowDto.getProjections());
        ruleInfo.setGroupbykeys(windowDto.getGroupbykeys());
        ruleInfo.setWindow(windowDto.getWindow());
        ruleInfo.setActions(windowDto.getActions());
        return ruleInfo;
    }

    private RuleInfo getWindow(RuleInfo ruleInfo) {
        if (ruleInfo != null) {
            Collection<RuleInfo> res = getWindows(Collections.singletonList(ruleInfo));
            if (!res.isEmpty()) {
                return res.iterator().next();
            }
        }
        return null;
    }
    private Collection<RuleInfo> getWindows(Collection<RuleInfo> ruleInfos) {
        return Collections2.filter(ruleInfos,
                new Predicate<RuleInfo>() {
                    @Override
                    public boolean apply(@Nullable RuleInfo ruleInfo) {
                        return ruleInfo != null
                                && StringUtils.isEmpty(ruleInfo.getCondition())
                                && ruleInfo.getStreams() != null
                                && !ruleInfo.getStreams().isEmpty()
                                && ruleInfo.getWindow() != null;
                    }
                });
    }
}
