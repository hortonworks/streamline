package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.service.NotificationService;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.TopologyMetrics;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for metrics
 */
@Path("/api/v1/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);

    private CatalogService catalogService;

    public MetricsResource(CatalogService service) {
        this.catalogService = service;
    }

    @GET
    @Path("/topologies/{id}")
    @Timed
    public Response getTopologyMetricsById(@PathParam("id") Long id) {
        try {
            Topology topology = catalogService.getTopology(id);
            if (topology != null) {
                Map<String, TopologyMetrics.ComponentMetric> topologyMetrics = catalogService.getTopologyMetrics(topology);
                return WSUtils.respond(OK, SUCCESS, topologyMetrics);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @GET
    @Path("/topologies/{id}/sources/{sourceId}/complete_latency")
    @Timed
    public Response getCompleteLatency(@PathParam("id") Long id,
                                       @PathParam("sourceId") String sourceId,
                                       @QueryParam("from") Long from,
                                       @QueryParam("to") Long to) {
        if (from == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "from");
        }
        if (to == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "to");
        }

        try {
            Topology topology = catalogService.getTopology(id);
            if (topology != null) {
                Map<Long, Double> metrics = catalogService.getCompleteLatency(topology, sourceId, from, to);
                return WSUtils.respond(OK, SUCCESS, metrics);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @GET
    @Path("/topologies/{id}/sources/{sourceId}/component_stats")
    @Timed
    public Response getComponentStats(@PathParam("id") Long id,
                                      @PathParam("sourceId") String sourceId,
                                      @QueryParam("from") Long from,
                                      @QueryParam("to") Long to) {
        if (from == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "from");
        }
        if (to == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "to");
        }

        try {
            Topology topology = catalogService.getTopology(id);
            if (topology != null) {
                Map<String, Map<Long, Double>> metrics = catalogService.getComponentStats(topology, sourceId, from, to);
                return WSUtils.respond(OK, SUCCESS, metrics);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @GET
    @Path("/topologies/{id}/sources/{sourceId}/kafka_topic_offsets")
    @Timed
    public Response getKafkaTopicOffsets(@PathParam("id") Long id,
                                         @PathParam("sourceId") String sourceId,
                                         @QueryParam("from") Long from,
                                         @QueryParam("to") Long to) {
        if (from == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "from");
        }
        if (to == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "to");
        }

        try {
            Topology topology = catalogService.getTopology(id);
            if (topology != null) {
                Map<String, Map<Long, Double>> metrics = catalogService.getKafkaTopicOffsets(topology, sourceId, from, to);
                return WSUtils.respond(OK, SUCCESS, metrics);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @GET
    @Path("/raw")
    @Timed
    public Response getMetrics(@QueryParam("metricName") String metricName,
                               @QueryParam("parameters") String parameters,
                               @QueryParam("from") Long from,
                               @QueryParam("to") Long to) {
        if (metricName == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "metricName");
        }
        if (from == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "from");
        }
        if (to == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "to");
        }

        try {
            Map<String, Map<Long, Double>> metrics = catalogService.getMetrics(metricName, parameters, from, to);
            return WSUtils.respond(OK, SUCCESS, metrics);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }
}
