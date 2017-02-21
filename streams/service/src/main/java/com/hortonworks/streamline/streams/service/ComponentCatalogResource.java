/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityAlreadyExistsException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;

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
    private final EnvironmentService environmentService;

    public ComponentCatalogResource(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * List ALL components or the ones matching specific query params.
     */
    @GET
    @Path("/services/{serviceId}/components")
    @Timed
    public Response listComponents(@PathParam("serviceId") Long serviceId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = buildServiceIdAwareQueryParams(serviceId, uriInfo);
        Collection<Component> components = environmentService.listComponents(queryParams);
        if (components != null) {
            return WSUtils.respondEntities(components, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/services/{serviceId}/components/{id}")
    @Timed
    public Response getComponentById(@PathParam("serviceId") Long serviceId, @PathParam("id") Long componentId) {
        Component component = environmentService.getComponent(componentId);
        if (component != null) {
            if (component.getServiceId() == null || !component.getServiceId().equals(serviceId)) {
                throw EntityNotFoundException.byId("service: " + serviceId.toString());
            }
            return WSUtils.respondEntity(component, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(serviceId, componentId));
    }

    @POST
    @Path("/services/{serviceId}/components")
    @Timed
    public Response addComponent(@PathParam("serviceId") Long serviceId, Component component) {
        // overwrite service id to given path param
        component.setServiceId(serviceId);

        Service service = environmentService.getService(serviceId);
        if (service == null) {
            throw EntityNotFoundException.byId("service: " + serviceId.toString());
        }

        String componentName = component.getName();
        Component result = environmentService.getComponentByName(serviceId, componentName);
        if (result != null) {
            throw EntityAlreadyExistsException.byName("service id " + serviceId +
                " and component name " + componentName);
        }

        Component createdComponent = environmentService.addComponent(component);
        return WSUtils.respondEntity(createdComponent, CREATED);
    }

    @PUT
    @Path("/services/{serviceId}/components")
    @Timed
    public Response addOrUpdateComponent(@PathParam("serviceId") Long serviceId, Component component) {
        // overwrite service id to given path param
        component.setServiceId(serviceId);

        Service service = environmentService.getService(serviceId);
        if (service == null) {
            throw EntityNotFoundException.byId("service: " + serviceId.toString());
        }

        Component createdComponent = environmentService.addOrUpdateComponent(serviceId, component);
        return WSUtils.respondEntity(createdComponent, CREATED);
    }

    @DELETE
    @Path("/services/{serviceId}/components/{id}")
    @Timed
    public Response removeComponent(@PathParam("id") Long componentId) {
        Component removeComponent = environmentService.removeComponent(componentId);
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

        Component newComponent = environmentService.addOrUpdateComponent(serviceId, componentId, component);
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
}
