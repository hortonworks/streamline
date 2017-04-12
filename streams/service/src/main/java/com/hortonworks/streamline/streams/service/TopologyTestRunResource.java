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
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCase;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCaseSink;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCaseSource;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyTestRunResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyTestRunResource.class);

    private static final Integer DEFAULT_LIST_ENTITIES_COUNT = 5;

    private final StreamCatalogService catalogService;
    private final TopologyActionsService actionsService;

    public TopologyTestRunResource(StreamCatalogService catalogService, TopologyActionsService actionsService) {
        this.catalogService = catalogService;
        this.actionsService = actionsService;
    }

    @POST
    @Path("/topologies/{topologyId}/actions/testrun")
    @Timed
    public Response testRunTopology (@Context UriInfo urlInfo,
                                     @PathParam("topologyId") Long topologyId,
                                     String testRunInputJson) throws Exception {
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            TopologyTestRunHistory history = actionsService.testRunTopology(result, testRunInputJson);
            return WSUtils.respondEntity(history, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/actions/testrun/histories")
    @Timed
    public Response getHistoriesOfTestRunTopology (@Context UriInfo urlInfo,
                                                   @PathParam("topologyId") Long topologyId,
                                                   @QueryParam("limit") Integer limit) throws Exception {
        Collection<TopologyTestRunHistory> histories = catalogService.listTopologyTestRunHistory(topologyId);
        if (histories == null) {
            throw EntityNotFoundException.byFilter("topology id " + topologyId);
        }

        List<TopologyTestRunHistory> filteredHistories = filterHistories(limit, histories);
        return WSUtils.respondEntities(filteredHistories, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/testrun/histories")
    @Timed
    public Response getHistoriesOfTestRunTopology (@Context UriInfo urlInfo,
                                                   @PathParam("topologyId") Long topologyId,
                                                   @PathParam("versionId") Long versionId,
                                                   @QueryParam("limit") Integer limit) throws Exception {
        Collection<TopologyTestRunHistory> histories = catalogService.listTopologyTestRunHistory(topologyId, versionId);
        if (histories == null) {
            throw EntityNotFoundException.byFilter("topology id " + topologyId);
        }

        List<TopologyTestRunHistory> filteredHistories = filterHistories(limit, histories);
        return WSUtils.respondEntities(filteredHistories, OK);
    }

    private List<TopologyTestRunHistory> filterHistories(Integer limit, Collection<TopologyTestRunHistory> histories) {
        if (limit == null) {
            limit = DEFAULT_LIST_ENTITIES_COUNT;
        }

        return histories.stream()
                // reverse order
                .sorted((h1, h2) -> (int) (h2.getId() - h1.getId()))
                .limit(limit)
                .collect(toList());
    }

    @POST
    @Path("/topologies/{topologyId}/testcases")
    public Response addTestRunCase(@PathParam("topologyId") Long topologyId,
                                      TopologyTestRunCase testRunCase) {
        testRunCase.setTopologyId(topologyId);
        TopologyTestRunCase addedCase = catalogService.addTopologyTestRunCase(testRunCase);
        return WSUtils.respondEntity(addedCase, CREATED);
    }

    @PUT
    @Path("/topologies/{topologyId}/testcases/{testCaseId}")
    public Response addOrUpdateTestRunCase(@PathParam("topologyId") Long topologyId,
                                           @PathParam("testCaseId") Long testCaseId,
                                           TopologyTestRunCase testRunCase) {
        testRunCase.setTopologyId(topologyId);
        testRunCase.setId(testCaseId);
        TopologyTestRunCase updatedCase = catalogService.addOrUpdateTopologyTestRunCase(topologyId, testRunCase);
        return WSUtils.respondEntity(updatedCase, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/testcases/{testCaseId}")
    public Response getTestRunCase(@PathParam("topologyId") Long topologyId,
                                   @PathParam("testCaseId") Long testCaseId) {
        TopologyTestRunCase testcase = catalogService.getTopologyTestRunCase(topologyId, testCaseId);
        if (testcase == null) {
            throw EntityNotFoundException.byId(Long.toString(testCaseId));
        }

        return WSUtils.respondEntity(testcase, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/testcases")
    @Timed
    public Response listTestRunCases(@Context UriInfo urlInfo,
                                     @PathParam("topologyId") Long topologyId,
                                     @QueryParam("limit") Integer limit) throws Exception {
        Collection<TopologyTestRunCase> cases = catalogService.listTopologyTestRunCase(topologyId);

        if (cases == null) {
            throw EntityNotFoundException.byFilter("topology id " + topologyId);
        }

        List<TopologyTestRunCase> filteredCases = filterTestRunCases(limit, cases);
        return WSUtils.respondEntities(filteredCases, OK);
    }

    @DELETE
    @Path("/topologies/{topologyId}/testcases/{testCaseId}")
    public Response removeTestRunCase(@PathParam("topologyId") Long topologyId,
                                      @PathParam("testCaseId") Long testCaseId) {
        TopologyTestRunCase testRunCase = catalogService.removeTestRunCase(topologyId, testCaseId);
        if (testRunCase != null) {
            return WSUtils.respondEntity(testRunCase, OK);
        }

        throw EntityNotFoundException.byId(testCaseId.toString());
    }

    private List<TopologyTestRunCase> filterTestRunCases(Integer limit, Collection<TopologyTestRunCase> cases) {
        if (limit == null) {
            limit = DEFAULT_LIST_ENTITIES_COUNT;
        }

        return cases.stream()
                // reverse order
                .sorted((h1, h2) -> (int) (h2.getId() - h1.getId()))
                .limit(limit)
                .collect(toList());
    }

    @POST
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sources")
    public Response addTestRunCaseSource(@PathParam("topologyId") Long topologyId,
                                         @PathParam("testCaseId") Long testCaseId,
                                         TopologyTestRunCaseSource testRunCaseSource) {
        TopologyTestRunCase testCase = catalogService.getTopologyTestRunCase(topologyId, testCaseId);
        if (testCase == null) {
            throw EntityNotFoundException.byId("Topology test case with topology id " + topologyId +
                    " and test case id " + testCaseId);
        }

        TopologyTestRunCaseSource addedCaseSource = catalogService.addTopologyTestRunCaseSource(testRunCaseSource);
        return WSUtils.respondEntity(addedCaseSource, CREATED);
    }

    @PUT
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sources/{id}")
    public Response addOrUpdateTestRunCaseSource(@PathParam("topologyId") Long topologyId,
                                           @PathParam("testCaseId") Long testCaseId,
                                           @PathParam("id") Long id,
                                           TopologyTestRunCaseSource testRunCaseSource) {
        testRunCaseSource.setTestCaseId(testCaseId);
        testRunCaseSource.setId(id);
        TopologyTestRunCaseSource updatedCase = catalogService.addOrUpdateTopologyTestRunCaseSource(testRunCaseSource.getId(), testRunCaseSource);
        return WSUtils.respondEntity(updatedCase, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/testcases/{testcaseId}/sources/{id}")
    public Response getTestRunCaseSource(@PathParam("topologyId") Long topologyId,
                                         @PathParam("testcaseId") Long testcaseId,
                                         @PathParam("id") Long id) {
        TopologyTestRunCaseSource testCaseSource = catalogService.getTopologyTestRunCaseSource(testcaseId, id);
        if (testCaseSource == null) {
            throw EntityNotFoundException.byId(Long.toString(id));
        }

        return WSUtils.respondEntity(testCaseSource, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sources/topologysource/{sourceId}")
    public Response getTestRunCaseSourceByTopologySource(@PathParam("topologyId") Long topologyId,
                                                         @PathParam("testCaseId") Long testCaseId,
                                                         @PathParam("sourceId") Long sourceId) {
        TopologyTestRunCaseSource testCaseSource = catalogService.getTopologyTestRunCaseSourceBySourceId(testCaseId, sourceId);
        if (testCaseSource == null) {
            throw EntityNotFoundException.byId("test case id: " + testCaseId + " , topology source id: " + sourceId);
        }

        return WSUtils.respondEntity(testCaseSource, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sources")
    public Response listTestRunCaseSource(@PathParam("topologyId") Long topologyId,
                                          @PathParam("testCaseId") Long testCaseId) {
        Collection<TopologyTestRunCaseSource> sources = catalogService.listTopologyTestRunCaseSource(topologyId, testCaseId);
        if (sources == null) {
            throw EntityNotFoundException.byFilter("topologyId: " + topologyId + " / testCaseId: " + testCaseId);
        }

        return WSUtils.respondEntities(sources, OK);
    }

    @POST
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sinks")
    public Response addTestRunCaseSink(@PathParam("topologyId") Long topologyId,
                                         @PathParam("testCaseId") Long testCaseId,
                                         TopologyTestRunCaseSink testRunCaseSink) {
        TopologyTestRunCase testCase = catalogService.getTopologyTestRunCase(topologyId, testCaseId);
        if (testCase == null) {
            throw EntityNotFoundException.byId("Topology test case with topology id " + topologyId +
                    " and test case id " + testCaseId);
        }

        TopologyTestRunCaseSink addedCaseSink = catalogService.addTopologyTestRunCaseSink(testRunCaseSink);
        return WSUtils.respondEntity(addedCaseSink, CREATED);
    }

    @PUT
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sinks/{id}")
    public Response addOrUpdateTestRunCaseSink(@PathParam("topologyId") Long topologyId,
                                           @PathParam("testCaseId") Long testCaseId,
                                           @PathParam("id") Long id,
                                           TopologyTestRunCaseSink testRunCaseSink) {
        testRunCaseSink.setTestCaseId(testCaseId);
        testRunCaseSink.setId(id);
        TopologyTestRunCaseSink updatedCase = catalogService.addOrUpdateTopologyTestRunCaseSink(testRunCaseSink.getId(), testRunCaseSink);
        return WSUtils.respondEntity(updatedCase, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/testcases/{testcaseId}/sinks/{id}")
    public Response getTestRunCaseSink(@PathParam("topologyId") Long topologyId,
                                         @PathParam("testcaseId") Long testcaseId,
                                         @PathParam("id") Long id) {
        TopologyTestRunCaseSink testCaseSink = catalogService.getTopologyTestRunCaseSink(testcaseId, id);
        if (testCaseSink == null) {
            throw EntityNotFoundException.byId(Long.toString(id));
        }

        return WSUtils.respondEntity(testCaseSink, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sinks/topologysink/{sinkId}")
    public Response getTestRunCaseSinkByTopologySink(@PathParam("topologyId") Long topologyId,
                                                         @PathParam("testCaseId") Long testCaseId,
                                                         @PathParam("sinkId") Long sinkId) {
        TopologyTestRunCaseSink testCaseSink = catalogService.getTopologyTestRunCaseSinkBySinkId(testCaseId, sinkId);
        if (testCaseSink == null) {
            throw EntityNotFoundException.byId("test case id: " + testCaseId + " , topology source id: " + sinkId);
        }

        return WSUtils.respondEntity(testCaseSink, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sinks")
    public Response listTestRunCaseSink(@PathParam("topologyId") Long topologyId,
                                          @PathParam("testCaseId") Long testCaseId) {
        Collection<TopologyTestRunCaseSink> sources = catalogService.listTopologyTestRunCaseSink(topologyId, testCaseId);
        if (sources == null) {
            throw EntityNotFoundException.byFilter("topologyId: " + topologyId + " / testCaseId: " + testCaseId);
        }

        return WSUtils.respondEntities(sources, OK);
    }

}
