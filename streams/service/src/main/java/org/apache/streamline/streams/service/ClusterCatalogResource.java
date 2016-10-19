package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.storage.exception.AlreadyExistsException;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.Component;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.ServiceConfiguration;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.cluster.discovery.ServiceNodeDiscoverer;
import org.apache.streamline.streams.cluster.discovery.ambari.AmbariServiceNodeDiscoverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_BY_NAME_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.IMPORT_ALREADY_IN_PROGRESS;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterCatalogResource.class);
    private final StreamCatalogService catalogService;
    private static final Set<Long> importInProgressCluster = new HashSet<>();

    public ClusterCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * List ALL clusters or the ones matching specific query params.
     */
    @GET
    @Path("/clusters")
    @Timed
    public Response listClusters(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<Cluster> clusters;
            if (params.isEmpty()) {
                clusters = catalogService.listClusters();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                clusters = catalogService.listClusters(queryParams);
            }
            if (clusters != null) {
                return WSUtils.respond(clusters, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/clusters/{id}")
    @Timed
    public Response getClusterById(@PathParam("id") Long clusterId) {
        try {
            Cluster result = catalogService.getCluster(clusterId);
            if (result != null) {
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, clusterId.toString());
    }

    @GET
    @Path("/clusters/name/{clusterName}")
    @Timed
    public Response getClusterByName(@PathParam("clusterName") String clusterName) {
        try {
            Cluster result = catalogService.getClusterByName(clusterName);
            if (result != null) {
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, clusterName);
    }

    @Timed
    @POST
    @Path("/clusters")
    public Response addCluster(Cluster cluster) {
        try {
            String clusterName = cluster.getName();
            Cluster result = catalogService.getClusterByName(clusterName);
            if (result != null) {
                throw new AlreadyExistsException("Cluster entity already exists with name " + clusterName);
            }

            Cluster createdCluster = catalogService.addCluster(cluster);
            return WSUtils.respond(createdCluster, CREATED, SUCCESS);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/clusters/{id}")
    @Timed
    public Response removeCluster(@PathParam("id") Long clusterId) {
        try {
            Cluster removedCluster = catalogService.removeCluster(clusterId);
            if (removedCluster != null) {
                return WSUtils.respond(removedCluster, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, clusterId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/clusters/{id}")
    @Timed
    public Response addOrUpdateCluster(@PathParam("id") Long clusterId,
                                       Cluster cluster) {
        try {
            Cluster newCluster = catalogService.addOrUpdateCluster(clusterId, cluster);
            return WSUtils.respond(newCluster, OK, SUCCESS);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @POST
    @Path("/cluster/import/ambari")
    @Timed
    public Response importServicesFromAmbari(AmbariClusterImportParams params) {
        Long clusterId = params.getClusterId();
        if (clusterId == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "clusterId");
        }

        Cluster retrievedCluster = catalogService.getCluster(clusterId);
        if (retrievedCluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, String.valueOf(clusterId));
        }

        boolean acquired = false;
        try {
            synchronized (importInProgressCluster) {
                if (importInProgressCluster.contains(clusterId)) {
                    return WSUtils.respond(SERVICE_UNAVAILABLE, IMPORT_ALREADY_IN_PROGRESS, String.valueOf(clusterId));
                }

                importInProgressCluster.add(clusterId);
                acquired = true;
            }

            ServiceNodeDiscoverer discoverer = new AmbariServiceNodeDiscoverer(params.getAmbariRestApiRootUrl(),
                params.getUsername(), params.getPassword());

            discoverer.init(null);

            retrievedCluster = catalogService.importClusterServices(discoverer, retrievedCluster);

            ClusterServicesImportResult result = new ClusterServicesImportResult(retrievedCluster);

            for (Service service : catalogService.listServices(clusterId)) {
                Collection<ServiceConfiguration> configurations = catalogService.listServiceConfigurations(service.getId());
                Collection<Component> components = catalogService.listComponents(service.getId());

                ClusterServicesImportResult.ServiceWithComponents s =
                    new ClusterServicesImportResult.ServiceWithComponents(service);
                s.setComponents(components);
                s.setConfigurations(configurations);

                result.addService(s);
            }

            return WSUtils.respond(result, OK, SUCCESS);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        } finally {
            if (acquired) {
                synchronized (importInProgressCluster) {
                    importInProgressCluster.remove(clusterId);
                }
            }
        }
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
