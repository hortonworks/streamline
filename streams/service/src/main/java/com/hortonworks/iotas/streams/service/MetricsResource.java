/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.streams.catalog.Topology;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import com.hortonworks.iotas.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.iotas.common.util.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
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

    private StreamCatalogService catalogService;

    public MetricsResource(StreamCatalogService service) {
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
