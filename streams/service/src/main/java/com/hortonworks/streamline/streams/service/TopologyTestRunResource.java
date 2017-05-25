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
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.server.UnhandledServerException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologySink;
import com.hortonworks.streamline.streams.catalog.TopologySource;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCase;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCaseSink;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCaseSource;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.datanucleus.util.StringUtils;
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyTestRunResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyTestRunResource.class);

    private static final Integer DEFAULT_LIST_ENTITIES_COUNT = 5;
    public static final Charset ENCODING_UTF_8 = Charset.forName("UTF-8");

    private final StreamCatalogService catalogService;
    private final TopologyActionsService actionsService;
    private final ObjectMapper objectMapper;

    public TopologyTestRunResource(StreamCatalogService catalogService, TopologyActionsService actionsService) {
        this.catalogService = catalogService;
        this.actionsService = actionsService;
        this.objectMapper = new ObjectMapper();
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
    @Path("/topologies/{topologyId}/testhistories")
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
    @Path("/topologies/{topologyId}/versions/{versionId}/testhistories")
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

    @GET
    @Path("/topologies/{topologyId}/testhistories/{historyId}")
    @Timed
    public Response getHistoryOfTestRunTopology (@Context UriInfo urlInfo,
                                                 @PathParam("topologyId") Long topologyId,
                                                 @PathParam("historyId") Long historyId,
                                                 @QueryParam("simplify") Boolean simplify) throws Exception {
        TopologyTestRunHistory history = catalogService.getTopologyTestRunHistory(historyId);

        if (history == null) {
            throw EntityNotFoundException.byId(String.valueOf(historyId));
        }

        if (!history.getTopologyId().equals(topologyId)) {
            throw BadRequestException.message("Test history " + historyId + " is not belong to topology " + topologyId);
        }

        if (BooleanUtils.isTrue(simplify)) {
            return WSUtils.respondEntity(new SimplifiedTopologyTestRunHistory(history), OK);
        } else {
            return WSUtils.respondEntity(history, OK);
        }
    }

    @GET
    @Path("/topologies/{topologyId}/testhistories/{historyId}/events")
    public Response getEventsOfTestRunTopologyHistory(@Context UriInfo urlInfo,
                                                      @PathParam("topologyId") Long topologyId,
                                                      @PathParam("historyId") Long historyId) throws Exception {
        return getEventsOfTestRunTopologyHistory(topologyId, historyId, null);
    }

    @GET
    @Path("/topologies/{topologyId}/testhistories/{historyId}/events/{componentName}")
    public Response getEventsOfTestRunTopologyHistory(@Context UriInfo urlInfo,
                                                      @PathParam("topologyId") Long topologyId,
                                                      @PathParam("historyId") Long historyId,
                                                      @PathParam("componentName") String componentName) throws Exception {
        return getEventsOfTestRunTopologyHistory(topologyId, historyId, componentName);
    }

    @GET
    @Path("/topologies/{topologyId}/testhistories/{historyId}/events/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadEventsOfTestRunTopologyHistory(@Context UriInfo urlInfo,
                                                           @PathParam("topologyId") Long topologyId,
                                                           @PathParam("historyId") Long historyId) throws Exception {
        File eventLogFile = getEventLogFile(topologyId, historyId);
        String content = FileUtils.readFileToString(eventLogFile, ENCODING_UTF_8);

        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String fileName = String.format("events-topology-%d-history-%d.log", topologyId, historyId);
        return Response.status(OK)
                .entity(is)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }

    private Response getEventsOfTestRunTopologyHistory(Long topologyId, Long historyId, String componentName) throws IOException {
        File eventLogFile = getEventLogFile(topologyId, historyId);

        List<String> lines = FileUtils.readLines(eventLogFile, ENCODING_UTF_8);
        Stream<Map<String, Object>> eventsStream = lines.stream().map(line -> {
            try {
                return objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        if (!StringUtils.isEmpty(componentName)) {
            eventsStream = eventsStream.filter(event -> {
                String eventComponentName = (String) event.get("componentName");
                return eventComponentName != null && eventComponentName.equals(componentName);
            });
        }

        return WSUtils.respondEntities(eventsStream.collect(toList()), OK);
    }

    private File getEventLogFile(Long topologyId, Long historyId) {
        TopologyTestRunHistory history = catalogService.getTopologyTestRunHistory(historyId);

        if (history == null) {
            throw EntityNotFoundException.byId(String.valueOf(historyId));
        }

        if (!history.getTopologyId().equals(topologyId)) {
            throw BadRequestException.message("Test history " + historyId + " is not belong to topology " + topologyId);
        }

        String eventLogFilePath = history.getEventLogFilePath();
        File eventLogFile = new File(eventLogFilePath);

        if (!eventLogFile.exists() || eventLogFile.isDirectory() || !eventLogFile.canRead()) {
            throw BadRequestException.message("Event log file of history " + historyId + " does not exist or is not readable.");
        }
        return eventLogFile;
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
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        testRunCase.setVersionId(currentVersionId);
        TopologyTestRunCase addedCase = catalogService.addTopologyTestRunCase(testRunCase);
        return WSUtils.respondEntity(addedCase, CREATED);
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/testcases")
    public Response addTestRunCase(@PathParam("topologyId") Long topologyId,
                                   @PathParam("versionId") Long versionId,
                                   TopologyTestRunCase testRunCase) {
        testRunCase.setTopologyId(topologyId);
        testRunCase.setVersionId(versionId);
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
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);

        Collection<TopologyTestRunCase> cases = catalogService.listTopologyTestRunCase(topologyId, currentVersionId);
        if (cases == null) {
            throw EntityNotFoundException.byFilter("topology id " + topologyId);
        }

        List<TopologyTestRunCase> filteredCases = filterTestRunCases(limit, cases);
        return WSUtils.respondEntities(filteredCases, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/testcases")
    @Timed
    public Response listTestRunCases(@Context UriInfo urlInfo,
                                     @PathParam("topologyId") Long topologyId,
                                     @PathParam("versionId") Long versionId,
                                     @QueryParam("limit") Integer limit) throws Exception {
        Collection<TopologyTestRunCase> cases = catalogService.listTopologyTestRunCase(topologyId, versionId);
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
        TopologySource topologySource = getAssociatedTopologySource(topologyId, testCaseId, testRunCaseSource.getSourceId());
        testRunCaseSource.setVersionId(topologySource.getVersionId());

        TopologyTestRunCaseSource addedCaseSource = catalogService.addTopologyTestRunCaseSource(testRunCaseSource);
        return WSUtils.respondEntity(addedCaseSource, CREATED);
    }

    @PUT
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sources/{id}")
    public Response addOrUpdateTestRunCaseSource(@PathParam("topologyId") Long topologyId,
                                           @PathParam("testCaseId") Long testCaseId,
                                           @PathParam("id") Long id,
                                           TopologyTestRunCaseSource testRunCaseSource) {
        testRunCaseSource.setId(id);
        testRunCaseSource.setTestCaseId(testCaseId);

        TopologySource topologySource = getAssociatedTopologySource(topologyId, testCaseId, testRunCaseSource.getSourceId());
        testRunCaseSource.setVersionId(topologySource.getVersionId());

        TopologyTestRunCaseSource updatedCase = catalogService.addOrUpdateTopologyTestRunCaseSource(testRunCaseSource.getId(), testRunCaseSource);
        return WSUtils.respondEntity(updatedCase, OK);
    }

    private TopologySource getAssociatedTopologySource(Long topologyId, Long testCaseId, Long topologySourceId) {
        TopologyTestRunCase testCase = catalogService.getTopologyTestRunCase(topologyId, testCaseId);
        if (testCase == null) {
            throw EntityNotFoundException.byId("Topology test case with topology id " + topologyId +
                    " and test case id " + testCaseId);
        }

        TopologySource topologySource = catalogService.getTopologySource(topologyId, topologySourceId,
                testCase.getVersionId());

        if (topologySource == null) {
            throw EntityNotFoundException.byId("Topology source with topology id " + topologyId +
                    " and version id " + testCase.getVersionId());
        } else if (!testCase.getVersionId().equals(topologySource.getVersionId())) {
            throw new IllegalStateException("Test case and topology source point to the different version id: "
                    + "version id of test case: " + testCase.getVersionId() + " / "
                    + "version id of topology source: " + topologySource.getVersionId());
        }
        return topologySource;
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
        TopologySink topologySink = getAssociatedTopologySink(topologyId, testCaseId, testRunCaseSink.getSinkId());
        testRunCaseSink.setVersionId(topologySink.getVersionId());

        TopologyTestRunCaseSink addedCaseSink = catalogService.addTopologyTestRunCaseSink(testRunCaseSink);
        return WSUtils.respondEntity(addedCaseSink, CREATED);
    }

    @PUT
    @Path("/topologies/{topologyId}/testcases/{testCaseId}/sinks/{id}")
    public Response addOrUpdateTestRunCaseSink(@PathParam("topologyId") Long topologyId,
                                           @PathParam("testCaseId") Long testCaseId,
                                           @PathParam("id") Long id,
                                           TopologyTestRunCaseSink testRunCaseSink) {
        testRunCaseSink.setId(id);
        testRunCaseSink.setTestCaseId(testCaseId);

        TopologySink topologySink = getAssociatedTopologySink(topologyId, testCaseId, testRunCaseSink.getSinkId());
        testRunCaseSink.setVersionId(topologySink.getVersionId());

        TopologyTestRunCaseSink updatedCase = catalogService.addOrUpdateTopologyTestRunCaseSink(testRunCaseSink.getId(), testRunCaseSink);
        return WSUtils.respondEntity(updatedCase, OK);
    }

    private TopologySink getAssociatedTopologySink(Long topologyId, Long testCaseId, Long topologySinkId) {
        TopologyTestRunCase testCase = catalogService.getTopologyTestRunCase(topologyId, testCaseId);
        if (testCase == null) {
            throw EntityNotFoundException.byId("Topology test case with topology id " + topologyId +
                    " and test case id " + testCaseId);
        }

        TopologySink topologySink = catalogService.getTopologySink(topologyId, topologySinkId,
                testCase.getVersionId());

        if (topologySink == null) {
            throw EntityNotFoundException.byId("Topology sink with topology id " + topologyId +
                    " and version id " + testCase.getVersionId());
        } else if (!testCase.getVersionId().equals(topologySink.getVersionId())) {
            throw new IllegalStateException("Test case and topology sink point to the different version id: "
                    + "version id of test case: " + testCase.getVersionId() + " / "
                    + "version id of topology sink: " + topologySink.getVersionId());
        }
        return topologySink;
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


    private static class SimplifiedTopologyTestRunHistory {
        private Long id;
        private Long topologyId;
        private Long versionId;
        private Boolean finished = false;
        private Boolean success = false;
        private Boolean matched = false;
        private Long startTime;
        private Long finishTime;
        private Long timestamp;

        SimplifiedTopologyTestRunHistory(TopologyTestRunHistory history) {
            id = history.getId();
            topologyId = history.getTopologyId();
            versionId = history.getVersionId();
            finished = history.getFinished();
            success = history.getSuccess();
            matched = history.getMatched();
            startTime = history.getStartTime();
            finishTime = history.getFinishTime();
            timestamp = history.getTimestamp();
        }

        public Long getId() {
            return id;
        }

        public Long getTopologyId() {
            return topologyId;
        }

        public Long getVersionId() {
            return versionId;
        }

        public Boolean getFinished() {
            return finished;
        }

        public Boolean getSuccess() {
            return success;
        }

        public Boolean getMatched() {
            return matched;
        }

        public Long getStartTime() {
            return startTime;
        }

        public Long getFinishTime() {
            return finishTime;
        }

        public Long getTimestamp() {
            return timestamp;
        }
    }

}
