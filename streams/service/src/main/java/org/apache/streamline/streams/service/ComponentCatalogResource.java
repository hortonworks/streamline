package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.storage.exception.AlreadyExistsException;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.Component;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;

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

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_BY_NAME_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;


@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)

public class ComponentCatalogResource {
    private final StreamCatalogService catalogService;

    public ComponentCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * List ALL components or the ones matching specific query params.
     */
    @GET
    @Path("/services/{serviceId}/components")
    @Timed
    public Response listComponents(@PathParam("serviceId") Long serviceId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = buildServiceIdAwareQueryParams(serviceId, uriInfo);

        try {
            Collection<Component> components = catalogService.listComponents(queryParams);
            if (components != null) {
                return WSUtils.respond(components, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * List ALL components or the ones matching specific query params.
     */
    @GET
    @Path("/clusters/name/{clusterName}/services/name/{serviceName}/components")
    @Timed
    public Response listComponentsByName(@PathParam("clusterName") String clusterName,
        @PathParam("serviceName") String serviceName, @Context UriInfo uriInfo) {
        Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }

        Service service = catalogService.getServiceByName(cluster.getId(), serviceName);
        if (service == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "service name " + serviceName);
        }

        List<QueryParam> queryParams = buildServiceIdAwareQueryParams(service.getId(), uriInfo);

        try {
            Collection<Component> components = catalogService.listComponents(queryParams);
            if (components != null) {
                return WSUtils.respond(components, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/services/{serviceId}/components/{id}")
    @Timed
    public Response getComponentById(@PathParam("serviceId") Long serviceId, @PathParam("id") Long componentId) {
        try {
            Component component = catalogService.getComponent(componentId);
            if (component != null) {
                if (component.getServiceId() == null || !component.getServiceId().equals(serviceId)) {
                    return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "service: " + serviceId.toString());
                }
                return WSUtils.respond(component, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(serviceId, componentId));
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/name/{serviceName}/components/name/{componentName}")
    @Timed
    public Response getComponentByName(@PathParam("clusterName") String clusterName,
        @PathParam("serviceName") String serviceName, @PathParam("componentName") String componentName) {
        Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }

        Service service = catalogService.getServiceByName(cluster.getId(), serviceName);
        if (service == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "service name " + serviceName);
        }

        try {
            Component component = catalogService.getComponentByName(service.getId(), componentName);
            if (component != null) {
                return WSUtils.respond(component, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeName(clusterName, serviceName, componentName));
    }

    @POST
    @Path("/services/{serviceId}/components")
    @Timed
    public Response addComponent(@PathParam("serviceId") Long serviceId, Component component) {
        // overwrite service id to given path param
        component.setServiceId(serviceId);

        try {
            Service service = catalogService.getService(serviceId);
            if (service == null) {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "service: " + serviceId.toString());
            }

            String componentName = component.getName();
            Component result = catalogService.getComponentByName(serviceId, componentName);
            if (result != null) {
                throw new AlreadyExistsException("Component entity already exists with service id " +
                    serviceId + " and component name " + componentName);
            }

            Component createdComponent = catalogService.addComponent(component);
            return WSUtils.respond(createdComponent, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/services/{serviceId}/components")
    @Timed
    public Response addOrUpdateComponent(@PathParam("serviceId") Long serviceId, Component component) {
        // overwrite service id to given path param
        component.setServiceId(serviceId);

        Service service = catalogService.getService(serviceId);
        if (service == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "service: " + serviceId.toString());
        }

        try {
            Component createdComponent = catalogService.addOrUpdateComponent(serviceId, component);
            return WSUtils.respond(createdComponent, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/services/{serviceId}/components/{id}")
    @Timed
    public Response removeComponent(@PathParam("id") Long componentId) {
        try {
            Component removeComponent = catalogService.removeComponent(componentId);
            if (removeComponent != null) {
                return WSUtils.respond(removeComponent, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, componentId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/services/{serviceId}/components/{id}")
    @Timed
    public Response addOrUpdateComponent(@PathParam("serviceId") Long serviceId,
                                         @PathParam("id") Long componentId, Component component) {
        // overwrite service id to given path param
        component.setServiceId(serviceId);

        try {
            Component newComponent = catalogService.addOrUpdateComponent(serviceId, componentId, component);
            return WSUtils.respond(newComponent, OK, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private List<QueryParam> buildServiceIdAwareQueryParams(Long serviceId, UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam("serviceId", serviceId.toString()));
        if (uriInfo != null) {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            if (!params.isEmpty()) {
                queryParams.addAll(WSUtils.buildQueryParameters(params));
            }
        }

        return queryParams;
    }

    private String buildMessageForCompositeId(Long serviceId, Long componentId) {
        return String.format("service id <%d>, component id <%d>",
                serviceId, componentId);
    }

    private String buildMessageForCompositeName(String clusterName, String serviceName,
        String componentName) {
        return String.format("cluster name <%s>, service name <%s>, component name <%s>",
            clusterName, serviceName, componentName);
    }

}
