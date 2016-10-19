package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.storage.exception.AlreadyExistsException;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
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
import java.util.List;

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_BY_NAME_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceCatalogResource.class);
    private final StreamCatalogService catalogService;

    public ServiceCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * List ALL services or the ones matching specific query params.
     */
    @GET
    @Path("/clusters/{clusterId}/services")
    @Timed
    public Response listServices(@PathParam("clusterId") Long clusterId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = buildClusterIdAwareQueryParams(clusterId, uriInfo);
        try {
            Collection<Service> services;
            services = catalogService.listServices(queryParams);
            if (services != null) {
                return WSUtils.respond(services, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * List ALL services or the ones matching specific query params.
     */
    @GET
    @Path("/clusters/name/{clusterName}/services")
    @Timed
    public Response listServicesByName(@PathParam("clusterName") String clusterName, @Context UriInfo uriInfo) {
        Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }

        List<QueryParam> queryParams = buildClusterIdAwareQueryParams(cluster.getId(), uriInfo);
        try {
            Collection<Service> services;
            services = catalogService.listServices(queryParams);
            if (services != null) {
                return WSUtils.respond(services, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/clusters/{clusterId}/services/{id}")
    @Timed
    public Response getServiceById(@PathParam("clusterId") Long clusterId, @PathParam("id") Long serviceId) {
        try {
            Service result = catalogService.getService(serviceId);
            if (result != null) {
                if (result.getClusterId() == null || !result.getClusterId().equals(clusterId)) {
                    return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "cluster: " + clusterId.toString());
                }
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(clusterId, serviceId));
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/name/{serviceName}")
    @Timed
    public Response getServiceByName(@PathParam("clusterName") String clusterName,
        @PathParam("serviceName") String serviceName) {
        Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            return WSUtils.respond(NOT_FOUND, ENTITY_BY_NAME_NOT_FOUND, "cluster name " + clusterName);
        }

        try {
            Service result = catalogService.getServiceByName(cluster.getId(), serviceName);
            if (result != null) {
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeName(clusterName, serviceName));
    }

    @Timed
    @POST
    @Path("/clusters/{clusterId}/services")
    public Response addService(@PathParam("clusterId") Long clusterId, Service service) {
        // overwrite cluster id to given path param
        service.setClusterId(clusterId);

        try {
            Cluster cluster = catalogService.getCluster(clusterId);
            if (cluster == null) {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "cluster: " + clusterId.toString());
            }

            String serviceName = service.getName();
            Service result = catalogService.getServiceByName(clusterId, serviceName);
            if (result != null) {
                throw new AlreadyExistsException("Service entity already exists with cluster id " +
                    clusterId + " and service name " + serviceName);
            }

            Service createdService = catalogService.addService(service);
            return WSUtils.respond(createdService, CREATED, SUCCESS);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/clusters/{clusterId}/services/{id}")
    @Timed
    public Response removeService(@PathParam("id") Long serviceId) {
        try {
            Service removedService = catalogService.removeService(serviceId);
            if (removedService != null) {
                return WSUtils.respond(removedService, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, serviceId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/clusters/{clusterId}/services/{id}")
    @Timed
    public Response addOrUpdateService(@PathParam("clusterId") Long clusterId,
        @PathParam("id") Long serviceId, Service service) {
        // overwrite cluster id to given path param
        service.setClusterId(clusterId);

        try {
            Cluster cluster = catalogService.getCluster(clusterId);
            if (cluster == null) {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "cluster: " + clusterId.toString());
            }

            Service newService = catalogService.addOrUpdateService(serviceId, service);
            return WSUtils.respond(newService, OK, SUCCESS);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, ResponseMessage.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
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

    private String buildMessageForCompositeName(String clusterName, String serviceName) {
        return String.format("cluster name <%s>, service name <%s>",
            clusterName, serviceName);
    }

}
