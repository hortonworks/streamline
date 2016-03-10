package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.TopologyActions;
import com.hortonworks.iotas.topology.TopologyComponent;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.apache.commons.lang3.StringUtils;

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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyCatalogResource {
    private CatalogService catalogService;
    private final URL SCHEMA = Thread.currentThread().getContextClassLoader()
            .getResource("assets/schemas/topology.json");

    public TopologyCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/topologies")
    @Timed
    public Response listTopologies () {
        try {
            Collection<Topology> topologies = catalogService
                    .listTopologies();
            return WSUtils.respond(OK, SUCCESS, topologies);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/topologies/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologyById (@PathParam("id") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies")
    @Timed
    public Response addTopology (Topology topology) {
        try {
            if (StringUtils.isEmpty(topology.getName())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.NAME);
            }
            if (StringUtils.isEmpty(topology.getConfig())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.CONFIG);
            }
            Topology createdTopology = catalogService.addTopology
                    (topology);
            return WSUtils.respond(CREATED, SUCCESS, createdTopology);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/topologies/{id}")
    @Timed
    public Response removeTopology (@PathParam("id") Long topologyId) {
        try {
            Topology removedTopology = catalogService.removeTopology
                    (topologyId);
            if (removedTopology != null) {
                return WSUtils.respond(OK, SUCCESS, removedTopology);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND,
                        topologyId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/topologies/{id}")
    @Timed
    public Response addOrUpdateTopology (@PathParam("id") Long topologyId,
                                        Topology topology) {
        try {
            if (StringUtils.isEmpty(topology.getName())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.NAME);
            }
            if (StringUtils.isEmpty(topology.getConfig())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.CONFIG);
            }
            Topology result = catalogService.addOrUpdateTopology
                    (topologyId, topology);
            return WSUtils.respond(OK, SUCCESS, topology);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/topologies/{id}/actions/status")
    @Timed
    public Response topologyStatus (@PathParam("id") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                TopologyActions.Status status = catalogService.topologyStatus(result);
                return WSUtils.respond(OK, SUCCESS, status);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{id}/actions/validate")
    @Timed
    public Response validateTopology (@PathParam("id") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                catalogService.validateTopology(SCHEMA, topologyId);
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{id}/actions/deploy")
    @Timed
    public Response deployTopology (@PathParam("id") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                catalogService.validateTopology(SCHEMA, topologyId);
                catalogService.deployTopology(result);
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{id}/actions/kill")
    @Timed
    public Response killTopology (@PathParam("id") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                catalogService.killTopology(result);
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{id}/actions/suspend")
    @Timed
    public Response suspendTopology (@PathParam("id") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                catalogService.suspendTopology(result);
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{id}/actions/resume")
    @Timed
    public Response resumeTopology (@PathParam("id") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                catalogService.resumeTopology(result);
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @GET
    @Path("/system/componentdefinitions")
    @Timed
    public Response listTopologyComponentTypes () {
        Collection<TopologyComponent.TopologyComponentType>
                topologyComponents = catalogService.listTopologyComponentTypes();
        return WSUtils.respond(OK, SUCCESS, topologyComponents);
    }

    /**
     * List topology component configs matching the component and component
     * specific query parameter filters.
     */
    @GET
    @Path("/system/componentdefinitions/{component}")
    @Timed
    public Response listTopologyComponentsForTypeWithFilter (@PathParam
                                                                   ("component") TopologyComponent.TopologyComponentType componentType, @Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            queryParams = WSUtils.buildQueryParameters(params);
            Collection<TopologyComponent> topologyComponents = catalogService
                    .listTopologyComponentsForTypeWithFilter(componentType, queryParams);
            if (!topologyComponents.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, topologyComponents);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/system/componentdefinitions/{component}/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologyById (@PathParam("component") TopologyComponent.TopologyComponentType componentType, @PathParam ("id") Long id) {
        try {
            TopologyComponent result = catalogService
                    .getTopologyComponent(id);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @POST
    @Path("/system/componentdefinitions/{component}")
    @Timed
    public Response addTopologyComponentConfig (@PathParam("component") TopologyComponent.TopologyComponentType componentType,
                                                TopologyComponent topologyComponent) {
        try {
            Response response = validateTopologyComponent(topologyComponent);
            if (response != null) {
                return response;
            }
            topologyComponent.setType(componentType);
            TopologyComponent createdComponent = catalogService
                    .addTopologyComponent(topologyComponent);
            return WSUtils.respond(CREATED, SUCCESS, createdComponent);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/system/componentdefinitions/{component}/{id}")
    @Timed
    public Response addOrUpdateTopologyComponentConfig (@PathParam("component")
                                              TopologyComponent.TopologyComponentType componentType, @PathParam("id") Long id, TopologyComponent topologyComponent) {
        try {
            Response response = validateTopologyComponent(topologyComponent);
            if (response != null) {
                return response;
            }
            topologyComponent.setType(componentType);
            TopologyComponent result = catalogService.addOrUpdateTopologyComponent(id,
                    topologyComponent);
            return WSUtils.respond(OK, SUCCESS, result);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/system/componentdefinitions/{component}/{id}")
    @Timed
    public Response removeTopologyComponentConfig (@PathParam("component") TopologyComponent.TopologyComponentType componentType, @PathParam ("id") Long id) {
        try {
            TopologyComponent removedTopologyComponent = catalogService
                    .removeTopologyComponent
                            (id);
            if (removedTopologyComponent != null) {
                return WSUtils.respond(OK, SUCCESS, removedTopologyComponent);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND,
                        id.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private Response validateTopologyComponent (TopologyComponent topologyComponent) {
        Response response = null;
        if (StringUtils.isEmpty(topologyComponent.getName())) {
            response = WSUtils.respond(BAD_REQUEST,
                    BAD_REQUEST_PARAM_MISSING, TopologyComponent.NAME);
        }
        if (StringUtils.isEmpty(topologyComponent.getStreamingEngine())) {
            response = WSUtils.respond(BAD_REQUEST,
                    BAD_REQUEST_PARAM_MISSING, TopologyComponent.STREAMING_ENGINE);
        }
        if (StringUtils.isEmpty(topologyComponent.getSubType())) {
            response = WSUtils.respond(BAD_REQUEST,
                    BAD_REQUEST_PARAM_MISSING, TopologyComponent.SUB_TYPE);
        }
        if (StringUtils.isEmpty(topologyComponent.getConfig())) {
            response = WSUtils.respond(BAD_REQUEST,
                    BAD_REQUEST_PARAM_MISSING, TopologyComponent.CONFIG);
        }
        return response;
    }
}

