package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.storage.exception.AlreadyExistsException;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.ServiceConfiguration;
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
public class ServiceConfigurationCatalogResource {
    private StreamCatalogService catalogService;

    public ServiceConfigurationCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * List ALL configurations or the ones matching specific query params.
     */
    @GET
    @Path("/services/{serviceId}/configurations")
    @Timed
    public Response listServiceConfigurations(@PathParam("serviceId") Long serviceId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = buildServiceIdAwareQueryParams(serviceId, uriInfo);

        try {
            Collection<ServiceConfiguration> configurations = catalogService.listServiceConfigurations(queryParams);
            if (configurations != null) {
                return WSUtils.respond(configurations, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * List ALL configurations or the ones matching specific query params.
     */
    @GET
    @Path("/clusters/name/{clusterName}/services/name/{serviceName}/configurations")
    @Timed
    public Response listServiceConfigurationsByName(@PathParam("clusterName") String clusterName,
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
            Collection<ServiceConfiguration> configurations = catalogService.listServiceConfigurations(queryParams);
            if (configurations != null) {
                return WSUtils.respond(configurations, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/services/{serviceId}/configurations/{id}")
    @Timed
    public Response getConfigurationById(@PathParam("serviceId") Long serviceId, @PathParam("id") Long configurationId) {
        try {
            ServiceConfiguration configuration = catalogService.getServiceConfiguration(configurationId);
            if (configuration != null) {
                if (configuration.getServiceId() == null || !configuration.getServiceId().equals(serviceId)) {
                    return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "service: " + serviceId.toString());
                }
                return WSUtils.respond(configuration, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(serviceId, configurationId));
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/name/{serviceName}/configurations/name/{configurationName}")
    @Timed
    public Response getConfigurationByName(@PathParam("clusterName") String clusterName,
        @PathParam("serviceName") String serviceName, @PathParam("configurationName") String configurationName) {
        Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }

        Service service = catalogService.getServiceByName(cluster.getId(), serviceName);
        if (service == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "service name " + serviceName);
        }

        try {
            ServiceConfiguration configuration = catalogService.getServiceConfigurationByName(service.getId(), configurationName);
            if (configuration != null) {
                return WSUtils.respond(configuration, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeName(clusterName, serviceName, configurationName));
    }

    @POST
    @Path("/services/{serviceId}/configurations")
    @Timed
    public Response addServiceConfiguration(@PathParam("serviceId") Long serviceId, ServiceConfiguration serviceConfiguration) {
        // just overwrite the service id to given path param
        serviceConfiguration.setServiceId(serviceId);

        try {
            Service service = catalogService.getService(serviceId);
            if (service == null) {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "service: " + serviceId.toString());
            }

            String configurationName = serviceConfiguration.getName();
            ServiceConfiguration result = catalogService.getServiceConfigurationByName(serviceId, configurationName);
            if (result != null) {
                throw new AlreadyExistsException("ServiceConfiguration entity already exists with service id " +
                    serviceId + " and configuration name " + configurationName);
            }

            ServiceConfiguration createdConfiguration = catalogService.addServiceConfiguration(serviceConfiguration);
            return WSUtils.respond(createdConfiguration, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/services/{serviceId}/configurations")
    @Timed
    public Response addOrUpdateServiceConfiguration(@PathParam("serviceId") Long serviceId,
        ServiceConfiguration serviceConfiguration) {
        // overwrite service id to given path param
        serviceConfiguration.setServiceId(serviceId);

        Service service = catalogService.getService(serviceId);
        if (service == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "service: " + serviceId.toString());
        }

        try {
            ServiceConfiguration createdConfiguration = catalogService.addOrUpdateServiceConfiguration(serviceId,
                serviceConfiguration);
            return WSUtils.respond(createdConfiguration, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/services/{serviceId}/configurations/{id}")
    @Timed
    public Response removeServiceConfiguration(@PathParam("id") Long serviceConfigurationId) {
        try {
            ServiceConfiguration removedConfiguration = catalogService.removeServiceConfiguration(serviceConfigurationId);
            if (removedConfiguration != null) {
                return WSUtils.respond(removedConfiguration, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, serviceConfigurationId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/services/{serviceId}/configurations/{id}")
    @Timed
    public Response addOrUpdateServiceConfiguration(@PathParam("serviceId") Long serviceId,
        @PathParam("id") Long serviceConfigurationId, ServiceConfiguration serviceConfiguration) {
        // overwrite service id to given path param
        serviceConfiguration.setServiceId(serviceId);

        try {
            ServiceConfiguration newConfiguration = catalogService.addOrUpdateServiceConfiguration(serviceId,
                serviceConfigurationId, serviceConfiguration);
            return WSUtils.respond(newConfiguration, OK, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
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

    private String buildMessageForCompositeName(String clusterName, String serviceName,
        String serviceConfigurationName) {
        return String.format("cluster name <%s>, service name <%s>, configuration name <%s>",
            clusterName, serviceName, serviceConfigurationName);
    }

}
