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

package com.hortonworks.streamline.streams.cluster.resource;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.streamline.common.exception.ComponentConfigException;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.registries.storage.exception.AlreadyExistsException;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceBundle;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceBundleResource {
    private final EnvironmentService environmentService;

    public ServiceBundleResource(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * List all service bundles only the ones that match query params
     */
    @GET
    @Path("/servicebundles")
    @Timed
    public Response listServiceBundles(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams;
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        queryParams = WSUtils.buildQueryParameters(params);
        Collection<ServiceBundle> serviceBundles = environmentService
                .listServiceBundles(queryParams);
        if (serviceBundles != null) {
            return WSUtils.respondEntities(serviceBundles, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    /**
     * Get service bundle matching the id
     */
    @GET
    @Path("/servicebundles/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServiceBundleById(@PathParam ("id") Long id) {
        ServiceBundle result = environmentService.getServiceBundle(id);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }

    /**
     * Get service bundle matching the name
     */
    @GET
    @Path("/servicebundles/name/{serviceName}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServiceBundleByName(@PathParam ("serviceName") String serviceName) {
        ServiceBundle result = environmentService.getServiceBundleByName(serviceName);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byName(serviceName);
    }

    /**
     * Add a new service bundle.
     */
    @POST
    @Path("/servicebundles")
    @Timed
    public Response addServiceBundle(ServiceBundle serviceBundle) throws IOException, ComponentConfigException {
        try {
            String serviceName = serviceBundle.getName();
            ServiceBundle result = environmentService.getServiceBundleByName(serviceName);
            if (result != null) {
                throw new AlreadyExistsException("Service bundle for " + serviceName + " is already registered.");
            }

            ServiceBundle created = environmentService.addServiceBundle(serviceBundle);
            return WSUtils.respondEntity(created, CREATED);
        } catch (ProcessingException ex) {
            throw BadRequestException.of();
        }
    }

    /**
     * Update an existing service bundle.
     */
    @PUT
    @Path("/servicebundles/{name}")
    @Timed
    public Response putServiceBundle(@PathParam("name") String serviceName, ServiceBundle serviceBundle) throws IOException, ComponentConfigException {
        try {
            ServiceBundle updatedBundle = environmentService.updateServiceBundle(serviceName, serviceBundle);
            return WSUtils.respondEntity(updatedBundle, OK);
        } catch (ProcessingException ex) {
            throw BadRequestException.of();
        } catch (com.hortonworks.streamline.streams.cluster.exception.EntityNotFoundException e) {
            throw EntityNotFoundException.byMessage(e.getMessage());
        }
    }


    /**
     * Delete a service bundle.
     */
    @DELETE
    @Path("/servicebundles/{id}")
    @Timed
    public Response removeServiceBundle (@PathParam ("id") Long id) throws IOException {
        ServiceBundle removedServiceBundle = environmentService.removeServiceBundle(id);
        if (removedServiceBundle != null) {
            return WSUtils.respondEntity(removedServiceBundle, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }
}


