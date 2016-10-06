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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.catalog.CatalogResponse;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.catalog.CatalogRestClient;
import com.hortonworks.iotas.streams.catalog.TopologySource;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import com.hortonworks.registries.schemaregistry.SchemaNotFoundException;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Source component within an IotasTopology
 */
@Path("/api/v1/catalog/topologies/{topologyId}/sources")
@Produces(MediaType.APPLICATION_JSON)
public class TopologySourceCatalogResource {
    private StreamCatalogService catalogService;

    public TopologySourceCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the sources in the topology or the ones matching specific query params. For example to
     * list all the sources in the topology,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/sources</b>
     * <p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [{
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 60
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }]
     * }
     * </pre>
     */
    @GET
    @Timed
    public Response listTopologySources(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAwareQueryParams(topologyId, uriInfo);

        try {
            Collection<TopologySource> sources = catalogService.listTopologySources(queryParams);
            if (sources != null) {
                return WSUtils.respond(OK, SUCCESS, sources);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * <p>
     * Gets a specific topology source by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SOURCE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 60
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologySourceById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sourceId) {
        try {
            TopologySource source = catalogService.getTopologySource(sourceId);
            if (source != null && source.getTopologyId().equals(topologyId)) {
                return WSUtils.respond(OK, SUCCESS, source);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, sourceId));
    }

    @GET
    @Path("/{id}/schema")
    @Timed
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTopologySourceSchema(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sourceId) {
        String schema = null;
        try {
            schema = catalogService.getSchema(sourceId);
            return WSUtils.respond(OK, SUCCESS, schema);
        } catch (SchemaNotFoundException e) {
            // ignore and log error
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, sourceId));
    }

    /**
     * <p>
     * Creates a topology source. For example,
     * </p>
     * <b>POST /api/v1/catalog/topologies/:TOPOLOGY_ID/sources</b>
     * <pre>
     * {
     *   "name": "kafkaDataSource",
     *   "config": {
     *     "properties": {
     *       "zkUrl": "localhost:2181",
     *       "zkPath": "/brokers",
     *       "refreshFreqSecs": 60
     *     }
     *   },
     *   "type": "KAFKA",
     *
     *   "outputStreamIds": [1]
     *   OR
     *   "outputStreams" : [{stream1 data..}, {stream2 data..}]
     * }
     * </pre>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 60
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreamIds": [1] OR "outputStreams" : {..}
     *   }
     * }
     * </pre>
     */
    @POST
    @Timed
    public Response addTopologySource(@PathParam("topologyId") Long topologyId, TopologySource topologySource) {
        try {
            TopologySource createdSource = catalogService.addTopologySource(topologyId, topologySource);
            return WSUtils.respond(CREATED, SUCCESS, createdSource);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>Updates a topology source.</p>
     * <p>
     * <b>PUT /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SOURCE_ID</b>
     * <pre>
     * {
     *   "name": "kafkaDataSource",
     *   "config": {
     *     "properties": {
     *       "zkUrl": "localhost:2181",
     *       "zkPath": "/brokers",
     *       "refreshFreqSecs": 120
     *     }
     *   },
     *   "type": "KAFKA",
     *   "outputStreamIds": [1]
     * }
     * </pre>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 120
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreamIds": [1]
     *   }
     * }
     * </pre>
     */
    @PUT
    @Path("/{id}")
    @Timed
    public Response addOrUpdateTopologySource(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sourceId,
                                              TopologySource topologySource) {
        try {
            TopologySource createdTopologySource = catalogService.addOrUpdateTopologySource(topologyId, sourceId, topologySource);
            return WSUtils.respond(CREATED, SUCCESS, createdTopologySource);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>
     * Removes a topology source.
     * </p>
     * <b>DELETE /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SOURCE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 60
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/{id}")
    @Timed
    public Response removeTopologySource(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sourceId) {
        try {
            TopologySource topologySource = catalogService.removeTopologySource(sourceId);
            if (topologySource != null) {
                return WSUtils.respond(OK, SUCCESS, topologySource);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, sourceId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private String buildMessageForCompositeId(Long topologyId, Long sourceId) {
        return String.format("topology id <%d>, source id <%d>", topologyId, sourceId);
    }
}
