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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.cluster.ServiceBundle;
import com.hortonworks.streamline.streams.catalog.topology.TopologyComponentUISpecification;
import com.hortonworks.streamline.streams.cluster.model.ServiceWithComponents;
import com.hortonworks.streamline.streams.cluster.register.ManualServiceRegistrar;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityAlreadyExistsException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceCatalogResource.class);
    private final EnvironmentService environmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private static class ServiceRegisterDefinitionResponseForm {
        private final String serviceName;
        private final List<String> requiredComponents;
        private final List<String> requiredConfigFiles;

        public ServiceRegisterDefinitionResponseForm(String serviceName, List<String> requiredComponents, List<String> requiredConfigFiles) {
            this.serviceName = serviceName;
            this.requiredComponents = requiredComponents;
            this.requiredConfigFiles = requiredConfigFiles;
        }

        public String getServiceName() {
            return serviceName;
        }

        public List<String> getRequiredComponents() {
            return requiredComponents;
        }

        public List<String> getRequiredConfigFiles() {
            return requiredConfigFiles;
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/clusters/{clusterId}/services/register/{serviceName}")
    @Timed
    public Response registerService(@PathParam("clusterId") Long clusterId,
                                    @PathParam("serviceName") String serviceName,
                                    FormDataMultiPart form) {
        ServiceBundle serviceBundle = environmentService.getServiceBundleByName(serviceName);
        if (serviceBundle == null) {
            throw BadRequestException.message("Not supported service: " + serviceName);
        }

        ManualServiceRegistrar registrar;
        try {
            Class<?> clazz = Class.forName(serviceBundle.getRegisterClass());
            registrar = (ManualServiceRegistrar) clazz.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }

        Cluster cluster = environmentService.getCluster(clusterId);
        if (cluster == null) {
            throw EntityNotFoundException.byId("Cluster " + clusterId);
        }

        Service service = environmentService.getServiceByName(clusterId, serviceName);
        if (service != null) {
            throw EntityAlreadyExistsException.byName("Service " + serviceName + " is already exist in Cluster " +
                    clusterId);
        }

        registrar.init(environmentService);

        TopologyComponentUISpecification specification = serviceBundle.getServiceUISpecification();

        List<String> fileFieldNames = specification.getFields().stream()
                .filter(uiField -> uiField.getType().equals(TopologyComponentUISpecification.UIFieldType.FILE))
                .map(uiField -> uiField.getFieldName())
                .collect(toList());

        Map<String, List<FormDataBodyPart>> fields = form.getFields();

        List<FormDataBodyPart> cfgFormList = fields.getOrDefault("config", Collections.emptyList());
        Config config;
        if (!cfgFormList.isEmpty()) {
            String jsonConfig = cfgFormList.get(0).getEntityAs(String.class);
            try {
                config = objectMapper.readValue(jsonConfig, Config.class);
            } catch (IOException e) {
                throw BadRequestException.message("config is missing");
            }
        } else {
            config = new Config();
        }

        List<ManualServiceRegistrar.ConfigFileInfo> configFileInfos = fields.entrySet().stream()
                .filter(entry -> fileFieldNames.contains(entry.getKey()))
                .flatMap(entry -> {
                    String key = entry.getKey();
                    List<FormDataBodyPart> values = entry.getValue();
                    return values.stream()
                            .map(val -> new ManualServiceRegistrar.ConfigFileInfo(key, val.getEntityAs(InputStream.class)));
                }).collect(toList());

        try {
            Service registeredService = registrar.register(cluster, config, configFileInfos);
            return WSUtils.respondEntity(buildManualServiceRegisterResult(registeredService), CREATED);
        } catch (IllegalArgumentException e) {
            throw BadRequestException.message(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private ServiceWithComponents buildManualServiceRegisterResult(Service service) {
        Collection<ServiceConfiguration> configurations = environmentService.listServiceConfigurations(service.getId());
        Collection<Component> components = environmentService.listComponents(service.getId());

        ServiceWithComponents s = new ServiceWithComponents(service);
        s.setComponents(components);
        s.setConfigurations(configurations);

        return s;
    }
}
