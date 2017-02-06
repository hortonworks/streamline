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
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
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
public class ServiceConfigurationCatalogResource {
    private EnvironmentService environmentService;

    public ServiceConfigurationCatalogResource(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * List ALL configurations or the ones matching specific query params.
     */
    @GET
    @Path("/services/{serviceId}/configurations")
    @Timed
    public Response listServiceConfigurations(@PathParam("serviceId") Long serviceId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = buildServiceIdAwareQueryParams(serviceId, uriInfo);

        Collection<ServiceConfiguration> configurations = environmentService.listServiceConfigurations(queryParams);
        if (configurations != null) {
            return WSUtils.respondEntities(configurations, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/services/{serviceId}/configurations/{id}")
    @Timed
    public Response getConfigurationById(@PathParam("serviceId") Long serviceId, @PathParam("id") Long configurationId) {
        ServiceConfiguration configuration = environmentService.getServiceConfiguration(configurationId);
        if (configuration != null) {
            if (configuration.getServiceId() == null || !configuration.getServiceId().equals(serviceId)) {
                throw EntityNotFoundException.byId("service: " + serviceId.toString());
            }
            return WSUtils.respondEntity(configuration, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(serviceId, configurationId));
    }

    @POST
    @Path("/services/{serviceId}/configurations")
    @Timed
    public Response addServiceConfiguration(@PathParam("serviceId") Long serviceId, ServiceConfiguration serviceConfiguration) {
        // just overwrite the service id to given path param
        serviceConfiguration.setServiceId(serviceId);

        Service service = environmentService.getService(serviceId);
        if (service == null) {
            throw EntityNotFoundException.byId("service: " + serviceId.toString());
        }

        String configurationName = serviceConfiguration.getName();
        ServiceConfiguration result = environmentService.getServiceConfigurationByName(serviceId, configurationName);
        if (result != null) {
            throw EntityAlreadyExistsException.byName("service id " +
                serviceId + " and configuration name " + configurationName);
        }

        ServiceConfiguration createdConfiguration = environmentService.addServiceConfiguration(serviceConfiguration);
        return WSUtils.respondEntity(createdConfiguration, CREATED);
    }

    @PUT
    @Path("/services/{serviceId}/configurations")
    @Timed
    public Response addOrUpdateServiceConfiguration(@PathParam("serviceId") Long serviceId,
        ServiceConfiguration serviceConfiguration) {
        // overwrite service id to given path param
        serviceConfiguration.setServiceId(serviceId);

        Service service = environmentService.getService(serviceId);
        if (service == null) {
            throw EntityNotFoundException.byId("service: " + serviceId.toString());
        }

        ServiceConfiguration createdConfiguration = environmentService.addOrUpdateServiceConfiguration(serviceId,
            serviceConfiguration);
        return WSUtils.respondEntity(createdConfiguration, CREATED);
    }

    @DELETE
    @Path("/services/{serviceId}/configurations/{id}")
    @Timed
    public Response removeServiceConfiguration(@PathParam("id") Long serviceConfigurationId) {
        ServiceConfiguration removedConfiguration = environmentService.removeServiceConfiguration(serviceConfigurationId);
        if (removedConfiguration != null) {
            return WSUtils.respondEntity(removedConfiguration, OK);
        }

        throw EntityNotFoundException.byId(serviceConfigurationId.toString());
    }

    @PUT
    @Path("/services/{serviceId}/configurations/{id}")
    @Timed
    public Response addOrUpdateServiceConfiguration(@PathParam("serviceId") Long serviceId,
        @PathParam("id") Long serviceConfigurationId, ServiceConfiguration serviceConfiguration) {
        // overwrite service id to given path param
        serviceConfiguration.setServiceId(serviceId);

        ServiceConfiguration newConfiguration = environmentService.addOrUpdateServiceConfiguration(serviceId,
            serviceConfigurationId, serviceConfiguration);
        return WSUtils.respondEntity(newConfiguration, CREATED);
    }

    private List<QueryParam> buildServiceIdAwareQueryParams(Long serviceId, UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
        queryParams.add(new QueryParam("serviceId", serviceId.toString()));
        if (uriInfo != null) {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            if (!params.isEmpty()) {
                queryParams.addAll(WSUtils.buildQueryParameters(params));
            }
        }

        return queryParams;
    }

    private String buildMessageForCompositeId(Long serviceId, Long serviceConfigurationId) {
        return String.format("service id <%d>, configuration id <%d>",
                serviceId, serviceConfigurationId);
    }

}
