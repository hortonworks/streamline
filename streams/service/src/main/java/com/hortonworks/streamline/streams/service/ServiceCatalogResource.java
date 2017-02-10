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
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityAlreadyExistsException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
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
public class ServiceCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceCatalogResource.class);
    private final EnvironmentService environmentService;

    public ServiceCatalogResource(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * List ALL services or the ones matching specific query params.
     */
    @GET
    @Path("/clusters/{clusterId}/services")
    @Timed
    public Response listServices(@PathParam("clusterId") Long clusterId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = buildClusterIdAwareQueryParams(clusterId, uriInfo);
        Collection<Service> services;
        services = environmentService.listServices(queryParams);
        if (services != null) {
            return WSUtils.respondEntities(services, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/clusters/{clusterId}/services/{id}")
    @Timed
    public Response getServiceById(@PathParam("clusterId") Long clusterId, @PathParam("id") Long serviceId) {
        Service result = environmentService.getService(serviceId);
        if (result != null) {
            if (result.getClusterId() == null || !result.getClusterId().equals(clusterId)) {
                throw EntityNotFoundException.byId("cluster: " + clusterId.toString());
            }
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(clusterId, serviceId));
    }

    @Timed
    @POST
    @Path("/clusters/{clusterId}/services")
    public Response addService(@PathParam("clusterId") Long clusterId, Service service) {
        // overwrite cluster id to given path param
        service.setClusterId(clusterId);

        Cluster cluster = environmentService.getCluster(clusterId);
        if (cluster == null) {
            throw EntityNotFoundException.byId("cluster: " + clusterId.toString());
        }

        String serviceName = service.getName();
        Service result = environmentService.getServiceByName(clusterId, serviceName);
        if (result != null) {
            throw EntityAlreadyExistsException.byName("cluster id " +
                clusterId + " and service name " + serviceName);
        }

        Service createdService = environmentService.addService(service);
        return WSUtils.respondEntity(createdService, CREATED);
    }

    @DELETE
    @Path("/clusters/{clusterId}/services/{id}")
    @Timed
    public Response removeService(@PathParam("id") Long serviceId) {
        Service removedService = environmentService.removeService(serviceId);
        if (removedService != null) {
            return WSUtils.respondEntity(removedService, OK);
        }

        throw EntityNotFoundException.byId(serviceId.toString());
    }

    @PUT
    @Path("/clusters/{clusterId}/services/{id}")
    @Timed
    public Response addOrUpdateService(@PathParam("clusterId") Long clusterId,
        @PathParam("id") Long serviceId, Service service) {
        // overwrite cluster id to given path param
        service.setClusterId(clusterId);

        Cluster cluster = environmentService.getCluster(clusterId);
        if (cluster == null) {
            throw EntityNotFoundException.byId("cluster: " + clusterId.toString());
        }

        Service newService = environmentService.addOrUpdateService(serviceId, service);
        return WSUtils.respondEntity(newService, OK);
    }

    private List<QueryParam> buildClusterIdAwareQueryParams(Long clusterId, UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam("clusterId", clusterId.toString()));
        if (uriInfo != null) {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            if (!params.isEmpty()) {
                queryParams.addAll(WSUtils.buildQueryParameters(params));
            }
        }

        return queryParams;
    }

    private String buildMessageForCompositeId(Long clusterId, Long serviceId) {
        return String.format("cluster id <%d>, service id <%d>",
            clusterId, serviceId);
    }

}
