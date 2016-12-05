package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.Component;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.common.exception.service.exception.request.EntityAlreadyExistsException;
import org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException;

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

import static javax.ws.rs.core.Response.Status.CREATED;
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
        Collection<Component> components = catalogService.listComponents(queryParams);
        if (components != null) {
            return WSUtils.respondEntities(components, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
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
            throw EntityNotFoundException.byName("cluster name " + clusterName);
        }

        Service service = catalogService.getServiceByName(cluster.getId(), serviceName);
        if (service == null) {
            throw EntityNotFoundException.byName("service name " + serviceName);
        }

        List<QueryParam> queryParams = buildServiceIdAwareQueryParams(service.getId(), uriInfo);

        Collection<Component> components = catalogService.listComponents(queryParams);
        if (components != null) {
            return WSUtils.respondEntities(components, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/services/{serviceId}/components/{id}")
    @Timed
    public Response getComponentById(@PathParam("serviceId") Long serviceId, @PathParam("id") Long componentId) {
        Component component = catalogService.getComponent(componentId);
        if (component != null) {
            if (component.getServiceId() == null || !component.getServiceId().equals(serviceId)) {
                throw EntityNotFoundException.byId("service: " + serviceId.toString());
            }
            return WSUtils.respondEntity(component, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(serviceId, componentId));
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/name/{serviceName}/components/name/{componentName}")
    @Timed
    public Response getComponentByName(@PathParam("clusterName") String clusterName,
        @PathParam("serviceName") String serviceName, @PathParam("componentName") String componentName) {
        Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            throw EntityNotFoundException.byName("cluster name " + clusterName);
        }

        Service service = catalogService.getServiceByName(cluster.getId(), serviceName);
        if (service == null) {
            throw EntityNotFoundException.byName("service name " + serviceName);
        }

        Component component = catalogService.getComponentByName(service.getId(), componentName);
        if (component != null) {
            return WSUtils.respondEntity(component, OK);
        }

        throw EntityNotFoundException.byName(buildMessageForCompositeName(clusterName,
            serviceName, componentName));
    }

    @POST
    @Path("/services/{serviceId}/components")
    @Timed
    public Response addComponent(@PathParam("serviceId") Long serviceId, Component component) {
        // overwrite service id to given path param
        component.setServiceId(serviceId);

        Service service = catalogService.getService(serviceId);
        if (service == null) {
            throw EntityNotFoundException.byId("service: " + serviceId.toString());
        }

        String componentName = component.getName();
        Component result = catalogService.getComponentByName(serviceId, componentName);
        if (result != null) {
            throw EntityAlreadyExistsException.byName("service id " + serviceId +
                " and component name " + componentName);
        }

        Component createdComponent = catalogService.addComponent(component);
        return WSUtils.respondEntity(createdComponent, CREATED);
    }

    @PUT
    @Path("/services/{serviceId}/components")
    @Timed
    public Response addOrUpdateComponent(@PathParam("serviceId") Long serviceId, Component component) {
        // overwrite service id to given path param
        component.setServiceId(serviceId);

        Service service = catalogService.getService(serviceId);
        if (service == null) {
            throw EntityNotFoundException.byId("service: " + serviceId.toString());
        }

        Component createdComponent = catalogService.addOrUpdateComponent(serviceId, component);
        return WSUtils.respondEntity(createdComponent, CREATED);
    }

    @DELETE
    @Path("/services/{serviceId}/components/{id}")
    @Timed
    public Response removeComponent(@PathParam("id") Long componentId) {
        Component removeComponent = catalogService.removeComponent(componentId);
        if (removeComponent != null) {
            return WSUtils.respondEntity(removeComponent, CREATED);
        }

        throw EntityNotFoundException.byId(componentId.toString());
    }

    @PUT
    @Path("/services/{serviceId}/components/{id}")
    @Timed
    public Response addOrUpdateComponent(@PathParam("serviceId") Long serviceId,
                                         @PathParam("id") Long componentId, Component component) {
        // overwrite service id to given path param
        component.setServiceId(serviceId);

        Component newComponent = catalogService.addOrUpdateComponent(serviceId, componentId, component);
        return WSUtils.respondEntity(newComponent, CREATED);
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
