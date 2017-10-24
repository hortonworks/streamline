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
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyBranchRule;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.streamline.common.util.WSUtils.buildTopologyIdAndVersionIdAwareQueryParams;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.security.Permission.WRITE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST resource for managing branch rules.
 * <p>
 * A {@link TopologyBranchRule} condition is parsed and converted to
 * the corresponding {@link Rule} object and saved in the catalog db.
 * </p>
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class BranchRuleCatalogResource {
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;

    public BranchRuleCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the branch rules in the topology or the ones matching specific query params. For example to
     * list all the branch rules in the topology,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/branchrules</b>
     * <p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [{
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "rule1",
     *     "description": "rule test",
     *     "condition": "humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "actions": ...
     *   }]
     * }
     * </pre>
     */
    @GET
    @Path("/topologies/{topologyId}/branchrules")
    @Timed
    public Response listTopologyBranchRules(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo,
                                            @Context SecurityContext securityContext) throws Exception {
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        return listTopologyBranchRules(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo),
                topologyId,
                securityContext);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/branchrules")
    @Timed
    public Response listTopologySourcesForVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("versionId") Long versionId,
                                                  @Context UriInfo uriInfo,
                                                  @Context SecurityContext securityContext) throws Exception {
        return listTopologyBranchRules(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo),
                topologyId,
                securityContext);
    }

    private Response listTopologyBranchRules(List<QueryParam> queryParams, Long topologyId, SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        Collection<TopologyBranchRule> topologyBranchRules = catalogService.listBranchRules(queryParams);
        if (topologyBranchRules != null) {
            return WSUtils.respondEntities(topologyBranchRules, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    /**
     * <p>
     * Gets a specific branch rule by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/branchrules/:RULE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "rule1",
     *     "description": "rule test",
     *     "actions": ...
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/topologies/{topologyId}/branchrules/{id}")
    @Timed
    public Response getTopologyBranchRuleById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                              @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologyBranchRule brRuleInfo = catalogService.getBranchRule(topologyId, ruleId);
        if (brRuleInfo != null && brRuleInfo.getTopologyId().equals(topologyId)) {
            return WSUtils.respondEntity(brRuleInfo, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, ruleId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/branchrules/{id}")
    @Timed
    public Response getTopologyBranchRuleByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                        @PathParam("id") Long ruleId,
                                                        @PathParam("versionId") Long versionId,
                                                        @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologyBranchRule topologyBranchRule = catalogService.getBranchRule(topologyId, ruleId, versionId);
        if (topologyBranchRule != null) {
            return WSUtils.respondEntity(topologyBranchRule, OK);
        }

        throw EntityNotFoundException.byVersion(buildMessageForCompositeId(topologyId, ruleId),
                versionId.toString());
    }


    /**
     * <p>
     * Creates a topology branch rule. For example,
     * </p>
     * <b>POST /api/v1/catalog/topologies/:TOPOLOGY_ID/branchrules</b>
     * <pre>
     * {
     *   "name": "rule1",
     *   "description": "rule test",
     *   "condition": "humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *   "actions": ...
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
     *     "name": "rule1",
     *     "description": "rule test",
     *     "condition": "humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "actions": ...
     *   }
     * }
     * </pre>
     *
     * <i>
     * Note:
     * </i>
     *  <ol>
     *      <li>'celciusToFarenheit' is a user defined function defined via UDFCatalogResource (/api/v1/catalog/udfs) api.</li>
     *      <li>'temperature' and 'humidity' are the fields of the 'nest' output stream which should have been defined via
     *      the TopologyStreamCatalogResource (/api/v1/catalog/topologies/{topologyId}/streams) api.</li>
     */
    @POST
    @Path("/topologies/{topologyId}/branchrules")
    @Timed
    public Response addTopologyRule(@PathParam("topologyId") Long topologyId, TopologyBranchRule brRuleInfo,
                                    @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyBranchRule createdTopologyBranchRule = catalogService.addBranchRule(topologyId, brRuleInfo);
        return WSUtils.respondEntity(createdTopologyBranchRule, CREATED);
    }

    /**
     * <p>Updates a topology branch rule.</p>
     * <p>
     * <b>PUT /api/v1/catalog/topologies/:TOPOLOGY_ID/branchrules/:RULE_ID</b>
     * <pre>
     * {
     *   "name": "rule1",
     *   "description": "rule test",
     *   "condition": "humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *   "actions": ...
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
     *     "name": "rule1",
     *     "description": "rule test",
     *     "condition": "humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "actions": ...
     *   }
     * }
     * </pre>
     */
    @PUT
    @Path("/topologies/{topologyId}/branchrules/{id}")
    @Timed
    public Response addOrUpdateRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                    TopologyBranchRule brRuleInfo, @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyBranchRule createdRuleInfo = catalogService.addOrUpdateBranchRule(topologyId, ruleId, brRuleInfo);
        return WSUtils.respondEntity(createdRuleInfo, CREATED);
    }


    /**
     * <p>
     * Removes a branch rule.
     * </p>
     * <b>DELETE /api/v1/catalog/topologies/:TOPOLOGY_ID/branchrules/:RULE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "rule1",
     *     "description": "rule test",
     *     "condition": "humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "actions": ...
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/topologies/{topologyId}/branchrules/{id}")
    @Timed
    public Response removeRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                               @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyBranchRule ruleInfo = catalogService.removeBranchRule(topologyId, ruleId);
        if (ruleInfo != null) {
            return WSUtils.respondEntity(ruleInfo, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, ruleId));
    }

    private String buildMessageForCompositeId(Long topologyId, Long ruleId) {
        return String.format("topology id <%d>, branch rule id <%d>", topologyId, ruleId);
    }
}
