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
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.TopologyRule;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;

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
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static com.hortonworks.streamline.common.util.WSUtils.buildTopologyIdAndVersionIdAwareQueryParams;

/**
 * REST resource for managing rules.
 * <p>
 * Having a separate resource for rules makes it easier to handle the parsing
 * and validation and keeps rule as an independent entity which
 * can be queried via the catalog service.
 * </p>
 * <p>
 * A {@link TopologyRule} sql is parsed and converted to
 * the corresponding {@link Rule} object and saved in the catalog db.
 * </p>
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class RuleCatalogResource {
    private final StreamCatalogService catalogService;

    public RuleCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the rules in the topology or the ones matching specific query params. For example to
     * list all the rules in the topology,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/rules</b>
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
     *     "sql": "select temperature, humidity from nest where humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "window": null,
     *     "actions": ...
     *   }]
     * }
     * </pre>
     */
    @GET
    @Path("/topologies/{topologyId}/rules")
    @Timed
    public Response listTopologyRules(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) throws Exception {
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        return listTopologyRules(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/rules")
    @Timed
    public Response listTopologySourcesForVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("versionId") Long versionId,
                                                  @Context UriInfo uriInfo) throws Exception {
        return listTopologyRules(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo));
    }

    private Response listTopologyRules(List<QueryParam> queryParams) throws Exception {
        Collection<TopologyRule> topologyRules = catalogService.listRules(queryParams);
        if (topologyRules != null) {
            return WSUtils.respondEntities(topologyRules, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }


    /**
     * <p>
     * Gets a specific rule by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/rules/:RULE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "rule1",
     *     "description": "rule test",
     *     "sql": "select temperature, humidity from nest where humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "window": null,
     *     "actions": ...
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/topologies/{topologyId}/rules/{id}")
    @Timed
    public Response getTopologyRuleById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId) throws Exception {
        TopologyRule topologyRule = catalogService.getRule(topologyId, ruleId);
        if (topologyRule != null) {
            return WSUtils.respondEntity(topologyRule, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, ruleId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/rules/{id}")
    @Timed
    public Response getTopologyRuleByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("id") Long ruleId,
                                                  @PathParam("versionId") Long versionId) throws Exception {
        TopologyRule topologyRule = catalogService.getRule(topologyId, ruleId, versionId);
        if (topologyRule != null) {
            return WSUtils.respondEntity(topologyRule, OK);
        }

        throw EntityNotFoundException.byVersion(buildMessageForCompositeId(topologyId, ruleId),
                versionId.toString());
    }

    /**
     * <p>
     * Creates a topology rule. For example,
     * </p>
     * <b>POST /api/v1/catalog/topologies/:TOPOLOGY_ID/rules</b>
     * <pre>
     * {
     *   "name": "rule1",
     *   "description": "rule test",
     *   "sql": "select temperature, humidity from nest where humidity > 90 AND celciusToFarenheit(temperature) > 80",
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
     *     "sql": "select temperature, humidity from nest where humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "window": null,
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
    @Path("/topologies/{topologyId}/rules")
    @Timed
    public Response addTopologyRule(@PathParam("topologyId") Long topologyId, TopologyRule topologyRule)
        throws Exception {
        TopologyRule createdTopologyRule = catalogService.addRule(topologyId, topologyRule);
        return WSUtils.respondEntity(createdTopologyRule, CREATED);
    }

    /**
     * <p>Updates a topology rule.</p>
     * <p>
     * <b>PUT /api/v1/catalog/topologies/:TOPOLOGY_ID/rules/:RULE_ID</b>
     * <pre>
     * {
     *   "name": "rule1",
     *   "description": "rule test",
     *   "sql": "select temperature, celciusToFarenheit(temperature), humidity from nest where humidity > 90 AND celciusToFarenheit(temperature) > 80",
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
     *     "sql": "select temperature, celciusToFarenheit(temperature), humidity from nest where humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "window": null,
     *     "actions": ...
     *   }
     * }
     * </pre>
     */
    @PUT
    @Path("/topologies/{topologyId}/rules/{id}")
    @Timed
    public Response addOrUpdateRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                                 TopologyRule topologyRule) throws Exception {
        TopologyRule createdTopologyRule = catalogService.addOrUpdateRule(topologyId, ruleId, topologyRule);
        return WSUtils.respondEntity(createdTopologyRule, CREATED);
    }


    /**
     * <p>
     * Removes a rule.
     * </p>
     * <b>DELETE /api/v1/catalog/topologies/:TOPOLOGY_ID/rules/:RULE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "rule1",
     *     "description": "rule test",
     *     "sql": "select temperature, celciusToFarenheit(temperature), humidity from nest where humidity > 90 AND celciusToFarenheit(temperature) > 80",
     *     "window": null,
     *     "actions": ...
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/topologies/{topologyId}/rules/{id}")
    @Timed
    public Response removeRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId) throws Exception {
        TopologyRule topologyRule = catalogService.removeRule(topologyId, ruleId);
        if (topologyRule != null) {
            return WSUtils.respondEntity(topologyRule, OK);
        }

        throw EntityNotFoundException.byId(ruleId.toString());
    }

    private String buildMessageForCompositeId(Long topologyId, Long ruleId) {
        return String.format("topology id <%d>, rule id <%d>", topologyId, ruleId);
    }
}
