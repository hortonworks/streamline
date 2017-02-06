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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.exception.WrappedWebApplicationException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.NamespaceServiceClusterMapping;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.StormMetadataService;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.AmbariServiceNodeDiscoverer;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.ClusterImportAlreadyInProgressException;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterCatalogResource.class);
    public static final String RESPONSE_MESSAGE_BAD_INPUT_NOT_VALID_AMBARI_CLUSTER_REST_API_URL = "Bad input: Not valid Ambari cluster Rest API URL";
    public static final String RESPONSE_MESSAGE = "responseMessage";
    public static final String VERIFIED = "verified";
    private final EnvironmentService environmentService;
    private static final Set<Long> importInProgressCluster = new HashSet<>();

    public ClusterCatalogResource(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * List ALL clusters or the ones matching specific query params.
     */
    @GET
    @Path("/clusters")
    @Timed
    public Response listClusters(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        Collection<Cluster> clusters;
        Boolean detail = false;

        if (params.isEmpty()) {
            clusters = environmentService.listClusters();
        } else {
            MultivaluedMap<String, String> copiedParams = new MultivaluedHashMap<>();
            copiedParams.putAll(params);
            List<String> detailOption = copiedParams.remove("detail");
            if (detailOption != null && !detailOption.isEmpty()) {
                detail = BooleanUtils.toBooleanObject(detailOption.get(0));
            }

            queryParams = WSUtils.buildQueryParameters(copiedParams);
            clusters = environmentService.listClusters(queryParams);
        }

        if (clusters != null) {
            return buildClustersGetResponse(clusters, detail);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/clusters/{id}")
    @Timed
    public Response getClusterById(@PathParam("id") Long clusterId,
                                   @javax.ws.rs.QueryParam("detail") Boolean detail) {
        Cluster result = environmentService.getCluster(clusterId);
        if (result != null) {
            return buildClusterGetResponse(result, detail);
        }

        throw EntityNotFoundException.byId(clusterId.toString());
    }

    @Timed
    @POST
    @Path("/clusters")
    public Response addCluster(Cluster cluster) {
        String clusterName = cluster.getName();
        String ambariImportUrl = cluster.getAmbariImportUrl();
        Cluster result = environmentService.getClusterByNameAndImportUrl(clusterName, ambariImportUrl);
        if (result != null) {
            throw EntityAlreadyExistsException.byName(cluster.getNameWithImportUrl());
        }

        Cluster createdCluster = environmentService.addCluster(cluster);
        return WSUtils.respondEntity(createdCluster, CREATED);
    }

    @DELETE
    @Path("/clusters/{id}")
    @Timed
    public Response removeCluster(@PathParam("id") Long clusterId) {
        assertNoNamespaceRefersCluster(clusterId);

        Cluster removedCluster = environmentService.removeCluster(clusterId);
        if (removedCluster != null) {
            return WSUtils.respondEntity(removedCluster, OK);
        }

        throw EntityNotFoundException.byId(clusterId.toString());
    }

    @PUT
    @Path("/clusters/{id}")
    @Timed
    public Response addOrUpdateCluster(@PathParam("id") Long clusterId,
                                       Cluster cluster) {
        Cluster newCluster = environmentService.addOrUpdateCluster(clusterId, cluster);
        return WSUtils.respondEntity(newCluster, CREATED);
    }

    @POST
    @Path("/cluster/import/ambari/verify/url")
    @Timed
    public Response verifyAmbariUrl(AmbariClusterImportParams params) {
        // Not assigning to interface to apply a hack
        AmbariServiceNodeDiscoverer discoverer = new AmbariServiceNodeDiscoverer(params.getAmbariRestApiRootUrl(),
                params.getUsername(), params.getPassword());

        Map<String, Object> response;
        try {
            discoverer.init(null);
            discoverer.validateApiUrl();
            response = Collections.singletonMap(VERIFIED, true);
        } catch (WrappedWebApplicationException e) {
            Throwable cause = e.getCause();
            if (cause == null || !(cause instanceof WebApplicationException)) {
                response = Collections.singletonMap(RESPONSE_MESSAGE, e.getMessage());
            } else {
                String message = getMessageFromAmbariAPIResponse(cause);
                response = Collections.singletonMap(RESPONSE_MESSAGE, message);
            }
        } catch (Throwable e) {
            // other exceptions
            response = Collections.singletonMap(RESPONSE_MESSAGE, e.getMessage());
        }

        return WSUtils.respondEntity(response, OK);
    }

    @POST
    @Path("/cluster/import/ambari")
    @Timed
    public Response importServicesFromAmbari(AmbariClusterImportParams params) throws Exception {
        Long clusterId = params.getClusterId();
        if (clusterId == null) {
            throw BadRequestException.missingParameter("clusterId");
        }

        Cluster retrievedCluster = environmentService.getCluster(clusterId);
        if (retrievedCluster == null) {
            throw EntityNotFoundException.byId(String.valueOf(clusterId));
        }

        boolean acquired = false;
        try {
            synchronized (importInProgressCluster) {
                if (importInProgressCluster.contains(clusterId)) {
                    throw new ClusterImportAlreadyInProgressException(String.valueOf(clusterId));
                }

                importInProgressCluster.add(clusterId);
                acquired = true;
            }

            // Not assigning to interface to apply a hack
            AmbariServiceNodeDiscoverer discoverer = new AmbariServiceNodeDiscoverer(params.getAmbariRestApiRootUrl(),
                params.getUsername(), params.getPassword());

            discoverer.init(null);

            retrievedCluster = environmentService.importClusterServices(discoverer, retrievedCluster);

            injectStormViewAsStormConfiguration(clusterId, discoverer);

            ClusterServicesImportResult result = buildClusterServicesImportResult(retrievedCluster);
            return WSUtils.respondEntity(result, OK);
        } finally {
            if (acquired) {
                synchronized (importInProgressCluster) {
                    importInProgressCluster.remove(clusterId);
                }
            }
        }
    }

    private Response buildClustersGetResponse(Collection<Cluster> clusters, Boolean detail) {
        if (BooleanUtils.isTrue(detail)) {
            List<ClusterServicesImportResult> clustersWithServices = clusters.stream()
                    .map(c -> buildClusterServicesImportResult(c))
                    .collect(Collectors.toList());
            return WSUtils.respondEntities(clustersWithServices, OK);
        } else {
            return WSUtils.respondEntities(clusters, OK);
        }
    }

    private Response buildClusterGetResponse(Cluster cluster, Boolean detail) {
        if (BooleanUtils.isTrue(detail)) {
            ClusterServicesImportResult clusterWithServices = buildClusterServicesImportResult(cluster);
            return WSUtils.respondEntity(clusterWithServices, OK);
        } else {
            return WSUtils.respondEntity(cluster, OK);
        }
    }

    private ClusterServicesImportResult buildClusterServicesImportResult(Cluster cluster) {
        ClusterServicesImportResult result = new ClusterServicesImportResult(cluster);

        for (Service service : environmentService.listServices(cluster.getId())) {
            Collection<ServiceConfiguration> configurations = environmentService.listServiceConfigurations(service.getId());
            Collection<Component> components = environmentService.listComponents(service.getId());

            ClusterServicesImportResult.ServiceWithComponents s =
                new ClusterServicesImportResult.ServiceWithComponents(service);
            s.setComponents(components);
            s.setConfigurations(configurations);

            result.addService(s);
        }
        return result;
    }

    private void injectStormViewAsStormConfiguration(Long clusterId, AmbariServiceNodeDiscoverer discoverer) {
        Service stormService = environmentService.getServiceByName(clusterId, ServiceConfigurations.STORM.name());
        if (stormService != null) {
            // hack: find Storm View and inject to one of ServiceConfiguration for Storm service if available
            String stormViewURL = discoverer.getStormViewUrl();
            if (StringUtils.isNotEmpty(stormViewURL)) {
                ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
                serviceConfiguration.setServiceId(stormService.getId());
                serviceConfiguration.setName(StormMetadataService.SERVICE_STORM_VIEW);
                serviceConfiguration.setConfiguration("{\"" + StormMetadataService.STORM_VIEW_CONFIGURATION_KEY_STORM_VIEW_URL +
                        "\":\"" + stormViewURL + "\"}");
                serviceConfiguration.setDescription("a hack to store Storm View URL");

                environmentService.addServiceConfiguration(serviceConfiguration);
            }
        }
    }

    private void assertNoNamespaceRefersCluster(Long clusterId) {
        Collection<Namespace> namespaces = environmentService.listNamespaces();
        if (namespaces != null) {
            for (Namespace namespace : namespaces) {
                Collection<NamespaceServiceClusterMapping> mappings =
                        environmentService.listServiceClusterMapping(namespace.getId());
                if (mappings != null) {
                    boolean matched = mappings.stream().anyMatch(m -> Objects.equals(m.getClusterId(), clusterId));
                    if (matched) {
                        throw BadRequestException.message("Namespace refers the cluster trying to remove - cluster id: " +
                                clusterId);
                    }
                }
            }
        }
    }

    private String getMessageFromAmbariAPIResponse(Throwable cause) {
        WebApplicationException reason = (WebApplicationException) cause;

        String message = cause.getMessage();
        try {
            String responseBody = reason.getResponse().readEntity(String.class);

            if (StringUtils.isNotEmpty(responseBody)) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    Map jsonDict = objectMapper.readValue(responseBody, Map.class);
                    String ambariMessage = jsonDict.get("message").toString();
                    if (StringUtils.isNotEmpty(ambariMessage)) {
                        message = ambariMessage;
                    }
                } catch (IOException e1) {
                    // we're setting default
                }
            }
        } catch (Throwable e) {
            // we're setting default
        }

        return message;
    }

    private static class AmbariClusterImportParams {
        // This is up to how UI makes input for cluster.
        // If UI can enumerate available clusters, no need to worry about using ID directly.
        private Long clusterId;
        private String ambariRestApiRootUrl;
        private String username;
        private String password;

        public Long getClusterId() {
            return clusterId;
        }

        public void setClusterId(Long clusterId) {
            this.clusterId = clusterId;
        }

        public String getAmbariRestApiRootUrl() {
            return ambariRestApiRootUrl;
        }

        public void setAmbariRestApiRootUrl(String ambariRestApiRootUrl) {
            this.ambariRestApiRootUrl = ambariRestApiRootUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    private static class ClusterServicesImportResult {
        private Cluster cluster;
        private Collection<ServiceWithComponents> services = new ArrayList<>();

        static class ServiceWithComponents {
            private Service service;
            private Collection<ServiceConfiguration> configurations;
            private Collection<Component> components;

            public ServiceWithComponents(Service service) {
                this.service = service;
            }

            public Service getService() {
                return service;
            }

            public Collection<Component> getComponents() {
                return components;
            }

            public void setComponents(Collection<Component> components) {
                this.components = components;
            }

            public Collection<ServiceConfiguration> getConfigurations() {
                return configurations;
            }

            public void setConfigurations(Collection<ServiceConfiguration> configurations) {
                this.configurations = configurations;
            }
        }

        public ClusterServicesImportResult(Cluster cluster) {
            this.cluster = cluster;
        }

        public Cluster getCluster() {
            return cluster;
        }

        public Collection<ServiceWithComponents> getServices() {
            return services;
        }

        public void setServices(Collection<ServiceWithComponents> services) {
            this.services = services;
        }

        public void addService(ServiceWithComponents service) {
            services.add(service);
        }
    }

}
