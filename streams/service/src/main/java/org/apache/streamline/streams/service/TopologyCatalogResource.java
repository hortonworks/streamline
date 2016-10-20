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
package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.processor.CustomProcessorInfo;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.catalog.topology.TopologyComponentDefinition;
import org.apache.streamline.streams.layout.component.TopologyActions;
import org.apache.streamline.streams.metrics.storm.topology.TopologyNotAliveException;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.commons.lang3.StringUtils;
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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.CUSTOM_PROCESSOR_ONLY;
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
public class TopologyCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyCatalogResource.class);
    public static final String JAR_FILE_PARAM_NAME = "jarFile";
    public static final String CP_INFO_PARAM_NAME = "customProcessorInfo";
    private final StreamCatalogService catalogService;
    private final URL SCHEMA = Thread.currentThread().getContextClassLoader()
            .getResource("assets/schemas/topology.json");

    public TopologyCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/topologies")
    @Timed
    public Response listTopologies (@javax.ws.rs.QueryParam("withMetric") Boolean withMetric) {
        try {
            Collection<Topology> topologies = catalogService.listTopologies();
            Response response;
            if (topologies != null) {
                if (withMetric == null || !withMetric) {
                    response = WSUtils.respond(topologies, OK, SUCCESS);
                } else {
                    List<TopologyCatalogWithMetric> topologiesWithMetric = enrichMetricToTopologies(
                        topologies);
                    response = WSUtils.respond(topologiesWithMetric, OK, SUCCESS);
                }
            } else {
                response = WSUtils.respond(Collections.emptyList(), OK, SUCCESS);
            }

            return response;
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/topologies/{topologyId}")
    @Timed
    public Response getTopologyById (@PathParam("topologyId") Long topologyId,
        @javax.ws.rs.QueryParam("withMetric") Boolean withMetric) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                if (withMetric == null || !withMetric) {
                    return WSUtils.respond(result, OK, SUCCESS);
                } else {
                    TopologyCatalogWithMetric topologiesWithMetric = enrichMetricToTopology(
                        result);
                    return WSUtils.respond(topologiesWithMetric, OK, SUCCESS);
                }
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies")
    @Timed
    public Response addTopology (Topology topology) {
        try {
            if (StringUtils.isEmpty(topology.getName())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.NAME);
            }
            if (StringUtils.isEmpty(topology.getConfig())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.CONFIG);
            }
            Topology createdTopology = catalogService.addTopology
                    (topology);
            return WSUtils.respond(createdTopology, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/topologies/{topologyId}")
    @Timed
    public Response removeTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology removedTopology = catalogService.removeTopology
                    (topologyId);
            if (removedTopology != null) {
                return WSUtils.respond(removedTopology, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND,
                        topologyId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/topologies/{topologyId}")
    @Timed
    public Response addOrUpdateTopology (@PathParam("topologyId") Long topologyId,
                                        Topology topology) {
        try {
            if (StringUtils.isEmpty(topology.getName())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.NAME);
            }
            if (StringUtils.isEmpty(topology.getConfig())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.CONFIG);
            }
            Topology result = catalogService.addOrUpdateTopology
                    (topologyId, topology);
            return WSUtils.respond(topology, OK, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/topologies/{topologyId}/actions/status")
    @Timed
    public Response topologyStatus (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                TopologyActions.Status status = catalogService.topologyStatus(result);
                return WSUtils.respond(status, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/validate")
    @Timed
    public Response validateTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                //catalogService.validateTopology(SCHEMA, topologyId);
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/deploy")
    @Timed
    public Response deployTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
//TODO: fix     catalogService.validateTopology(SCHEMA, topologyId);
                catalogService.deployTopology(result);
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Failed to deploy the topology ", topologyId, ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/kill")
    @Timed
    public Response killTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                catalogService.killTopology(result);
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/suspend")
    @Timed
    public Response suspendTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                catalogService.suspendTopology(result);
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/resume")
    @Timed
    public Response resumeTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                catalogService.resumeTopology(result);
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @GET
    @Path("/system/componentdefinitions")
    @Timed
    public Response listTopologyComponentTypes () {
        try {
            Collection<TopologyComponentDefinition.TopologyComponentType>
                    topologyComponents = catalogService.listTopologyComponentTypes();
            if (topologyComponents != null) {
                return WSUtils.respond(topologyComponents, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(Collections.emptyList(), NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER);
    }

    /**
     * List topology component configs matching the component and component
     * specific query parameter filters.
     */
    @GET
    @Path("/system/componentdefinitions/{component}")
    @Timed
    public Response listTopologyComponentsForTypeWithFilter (@PathParam
                                                                   ("component") TopologyComponentDefinition.TopologyComponentType componentType, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            queryParams = WSUtils.buildQueryParameters(params);
            Collection<TopologyComponentDefinition> topologyComponentDefinitions = catalogService
                    .listTopologyComponentsForTypeWithFilter(componentType, queryParams);
            if (topologyComponentDefinitions != null) {
                return WSUtils.respond(topologyComponentDefinitions, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/system/componentdefinitions/{component}/{id}")
    @Timed
    public Response getTopologyById (@PathParam("component") TopologyComponentDefinition.TopologyComponentType componentType, @PathParam ("id") Long id) {
        try {
            TopologyComponentDefinition result = catalogService
                    .getTopologyComponent(id);
            if (result != null) {
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @POST
    @Path("/system/componentdefinitions/{component}")
    @Timed
    public Response addTopologyComponentConfig (@PathParam("component") TopologyComponentDefinition.TopologyComponentType componentType,
                                                TopologyComponentDefinition topologyComponentDefinition) {
        try {
            Response response = validateTopologyComponent(topologyComponentDefinition);
            if (response != null) {
                return response;
            }
            topologyComponentDefinition.setType(componentType);
            TopologyComponentDefinition createdComponent = catalogService
                    .addTopologyComponent(topologyComponentDefinition);
            return WSUtils.respond(createdComponent, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/system/componentdefinitions/{component}/{id}")
    @Timed
    public Response addOrUpdateTopologyComponentConfig (@PathParam("component")
                                              TopologyComponentDefinition.TopologyComponentType componentType, @PathParam("id") Long id, TopologyComponentDefinition topologyComponentDefinition) {
        try {
            Response response = validateTopologyComponent(topologyComponentDefinition);
            if (response != null) {
                return response;
            }
            topologyComponentDefinition.setType(componentType);
            TopologyComponentDefinition result = catalogService.addOrUpdateTopologyComponent(id,
                    topologyComponentDefinition);
            return WSUtils.respond(result, OK, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/system/componentdefinitions/{component}/{id}")
    @Timed
    public Response removeTopologyComponentConfig (@PathParam("component") TopologyComponentDefinition.TopologyComponentType componentType, @PathParam ("id") Long id) {
        try {
            TopologyComponentDefinition removedTopologyComponentDefinition = catalogService.removeTopologyComponent(id);
            if (removedTopologyComponentDefinition != null) {
                return WSUtils.respond(removedTopologyComponentDefinition, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @Timed
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/system/componentdefinitions/{processor}/custom/{fileName}")
    public Response downloadCustomProcessorFile (@PathParam("processor") TopologyComponentDefinition.TopologyComponentType componentType, @PathParam
            ("fileName") String fileName) {
        try {
            if (!TopologyComponentDefinition.TopologyComponentType.PROCESSOR.equals(componentType)) {
                return WSUtils.respond(NOT_FOUND, CUSTOM_PROCESSOR_ONLY);
            }
            final InputStream inputStream = catalogService.getFileFromJarStorage(fileName);
            if (inputStream != null) {
                StreamingOutput streamOutput = WSUtils.wrapWithStreamingOutput(inputStream);
                return Response.ok(streamOutput).build();
            }
        } catch (Exception ex) {
            LOG.debug(ex.getMessage(), ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, fileName);
    }

    /**
     * List custom processors matching specific query parameter filters.
     */
    @GET
    @Path("/system/componentdefinitions/{processor}/custom")
    @Timed
    public Response listCustomProcessorsWithFilters (@PathParam("processor") TopologyComponentDefinition.TopologyComponentType componentType, @Context UriInfo uriInfo) {
        if (!TopologyComponentDefinition.TopologyComponentType.PROCESSOR.equals(componentType)) {
            return WSUtils.respond(NOT_FOUND, CUSTOM_PROCESSOR_ONLY);
        }
        List<QueryParam> queryParams;
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            queryParams = WSUtils.buildQueryParameters(params);
            Collection<CustomProcessorInfo> customProcessorInfos = catalogService.listCustomProcessorsWithFilter(queryParams);
            if (customProcessorInfos != null) {
                return WSUtils.respond(customProcessorInfos, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.debug(ex.getMessage(), ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    //curl -X POST -i -F jarFile=@../core/target/core-0.1.0-SNAPSHOT.jar -F imageFile=@../webservice/src/main/resources/assets/libs/bower/jquery-ui/css/images/animated-overlay.gif http://localhost:8080/api/v1/catalog/system/componentdefinitions/PROCESSOR/custom -F customProcessorInfo=@console_custom_processor
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/system/componentdefinitions/{processor}/custom")
    @Timed
    public Response addCustomProcessor (@PathParam("processor") TopologyComponentDefinition.TopologyComponentType componentType, FormDataMultiPart form) {
        if (!TopologyComponentDefinition.TopologyComponentType.PROCESSOR.equals(componentType)) {
           return  WSUtils.respond(NOT_FOUND, CUSTOM_PROCESSOR_ONLY);
        }
        InputStream jarFile = null;
        try {
            jarFile = this.getFormDataFromMultiPartRequestAs(InputStream.class, form, JAR_FILE_PARAM_NAME);
            String customProcessorInfoStr = this.getFormDataFromMultiPartRequestAs(String.class, form, CP_INFO_PARAM_NAME);
            String missingParam = (jarFile == null ? JAR_FILE_PARAM_NAME : (customProcessorInfoStr == null ? CP_INFO_PARAM_NAME : null));
            if (missingParam != null) {
                LOG.debug(missingParam + " is missing or invalid while adding custom processor");
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, missingParam);
            }
            CustomProcessorInfo customProcessorInfo = new ObjectMapper().readValue(customProcessorInfoStr, CustomProcessorInfo.class);
            CustomProcessorInfo createdCustomProcessor = catalogService.addCustomProcessorInfo(customProcessorInfo, jarFile);
            return WSUtils.respond(createdCustomProcessor, CREATED, SUCCESS);
        } catch (Exception e) {
            LOG.debug("Exception thrown while trying to add a custom processor", e);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, e.getMessage());
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                LOG.debug("Error while closing jar file stream", e);
            }
        }
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/system/componentdefinitions/{processor}/custom")
    @Timed
    public Response updateCustomProcessor (@PathParam("processor") TopologyComponentDefinition.TopologyComponentType componentType, FormDataMultiPart form) {
        if (!TopologyComponentDefinition.TopologyComponentType.PROCESSOR.equals(componentType)) {
            return WSUtils.respond(NOT_FOUND, CUSTOM_PROCESSOR_ONLY);
        }
        InputStream jarFile = null;
        try {
            jarFile = this.getFormDataFromMultiPartRequestAs(InputStream.class, form, JAR_FILE_PARAM_NAME);
            String customProcessorInfoStr = this.getFormDataFromMultiPartRequestAs(String.class, form, CP_INFO_PARAM_NAME);
            String missingParam = (jarFile == null ? JAR_FILE_PARAM_NAME : (customProcessorInfoStr == null ? CP_INFO_PARAM_NAME : null));
            if (missingParam != null) {
                LOG.debug(missingParam + " is missing or invalid while adding/updating custom processor");
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, missingParam);
            }
            CustomProcessorInfo customProcessorInfo = new ObjectMapper().readValue(customProcessorInfoStr, CustomProcessorInfo.class);
            CustomProcessorInfo updatedCustomProcessor = catalogService.updateCustomProcessorInfo(customProcessorInfo, jarFile);
            return WSUtils.respond(updatedCustomProcessor, OK, SUCCESS);
        } catch (Exception e) {
            LOG.debug("Exception thrown while trying to add/update a custom processor", e);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, e.getMessage());
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                LOG.debug("Error while closing jar file stream", e);
            }
        }
    }

    @DELETE
    @Path("/system/componentdefinitions/{processor}/custom/{name}")
    @Timed
    public Response removeCustomProcessorInfo (@PathParam("processor") TopologyComponentDefinition.TopologyComponentType componentType, @PathParam ("name") String name) {
        if (!TopologyComponentDefinition.TopologyComponentType.PROCESSOR.equals(componentType)) {
            return WSUtils.respond(NOT_FOUND, CUSTOM_PROCESSOR_ONLY);
        }
        try {
            CustomProcessorInfo removedCustomProcessorInfo = catalogService.removeCustomProcessorInfo(name);
            if (removedCustomProcessorInfo != null) {
                return WSUtils.respond(removedCustomProcessorInfo, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, name);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private List<TopologyCatalogWithMetric> enrichMetricToTopologies(
        Collection<Topology> topologies) {
        // need to also provide Topology Metric
        List<TopologyCatalogWithMetric> topologiesWithMetric = new ArrayList<>(topologies.size());
        for (Topology topology : topologies) {
            TopologyCatalogWithMetric topologyCatalogWithMetric = enrichMetricToTopology(topology);
            topologiesWithMetric.add(topologyCatalogWithMetric);
        }
        return topologiesWithMetric;
    }

    private TopologyCatalogWithMetric enrichMetricToTopology(Topology topology) {
        TopologyCatalogWithMetric topologyCatalogWithMetric;
        try {
            TopologyMetrics.TopologyMetric topologyMetric = catalogService.getTopologyMetric(topology);
            topologyCatalogWithMetric = new TopologyCatalogWithMetric(topology, true, topologyMetric);
        } catch (TopologyNotAliveException e) {
            topologyCatalogWithMetric = new TopologyCatalogWithMetric(topology, false, null);
        }
        return topologyCatalogWithMetric;
    }

    private Response validateTopologyComponent (TopologyComponentDefinition topologyComponentDefinition) {
        Response response = null;
        if (StringUtils.isEmpty(topologyComponentDefinition.getName())) {
            response = WSUtils.respond(BAD_REQUEST,
                    BAD_REQUEST_PARAM_MISSING, TopologyComponentDefinition.NAME);
        }
        if (StringUtils.isEmpty(topologyComponentDefinition.getStreamingEngine())) {
            response = WSUtils.respond(BAD_REQUEST,
                    BAD_REQUEST_PARAM_MISSING, TopologyComponentDefinition.STREAMING_ENGINE);
        }
        if (StringUtils.isEmpty(topologyComponentDefinition.getSubType())) {
            response = WSUtils.respond(BAD_REQUEST,
                    BAD_REQUEST_PARAM_MISSING, TopologyComponentDefinition.SUB_TYPE);
        }
        if (StringUtils.isEmpty(topologyComponentDefinition.getConfig())) {
            response = WSUtils.respond(BAD_REQUEST,
                    BAD_REQUEST_PARAM_MISSING, TopologyComponentDefinition.CONFIG);
        }
        return response;
    }

    private <T> T getFormDataFromMultiPartRequestAs (Class<T> clazz, FormDataMultiPart form, String paramName) {
        T result = null;
        try {
            FormDataBodyPart part = form.getField(paramName);
            if (part != null) {
                result = part.getValueAs(clazz);
            }
        } catch (Exception e) {
            LOG.debug("Cannot get param " + paramName + " as" + clazz + " from multipart form" );
        }
        return result;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class TopologyCatalogWithMetric {
        private final Topology topology;
        private final boolean running;
        private final TopologyMetrics.TopologyMetric metric;

        public TopologyCatalogWithMetric(Topology topology, boolean running, TopologyMetrics.TopologyMetric metric) {
            this.topology = topology;
            this.running = running;
            this.metric = metric;
        }

        public Topology getTopology() {
            return topology;
        }

        public boolean isRunning() {
            return running;
        }

        public TopologyMetrics.TopologyMetric getMetric() {
            return metric;
        }
     }
}

