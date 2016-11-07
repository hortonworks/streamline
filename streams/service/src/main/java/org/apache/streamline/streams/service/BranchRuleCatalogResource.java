package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;

import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.BranchRuleInfo;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.layout.component.rule.Rule;

import java.util.Collection;
import java.util.List;

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

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;

/**
 * REST resource for managing branch rules.
 * <p>
 * A {@link BranchRuleInfo} condition is parsed and converted to
 * the corresponding {@link Rule} object and saved in the catalog db.
 * </p>
 */
@Path("/api/v1/catalog/topologies/{topologyId}/branchrules")
@Produces(MediaType.APPLICATION_JSON)
public class BranchRuleCatalogResource {
    private final StreamCatalogService catalogService;

    public BranchRuleCatalogResource(StreamCatalogService catalogService) {
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
     *     "stream": "stream1"
     *   }]
     * }
     * </pre>
     */
    @GET
    @Timed
    public Response listTopologyBranchRules(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAwareQueryParams(topologyId, uriInfo);
        try {
            Collection<BranchRuleInfo> brRuleInfos = catalogService.listBranchRules(queryParams);
            if (brRuleInfos != null) {
                return WSUtils.respond(brRuleInfos, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
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
     *     "stream": "stream1"
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/{id}")
    @Timed
    public Response getTopologyBranchRuleById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId) {
        try {
            BranchRuleInfo brRuleInfo = catalogService.getBranchRule(ruleId);
            if (brRuleInfo != null && brRuleInfo.getTopologyId().equals(topologyId)) {
                return WSUtils.respond(brRuleInfo, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, ruleId));
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
     *   "stream": "stream1"
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
     *     "stream": "stream1"
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
    @Timed
    public Response addTopologyRule(@PathParam("topologyId") Long topologyId, BranchRuleInfo brRuleInfo) {
        try {
            BranchRuleInfo createdBranchRuleInfo = catalogService.addBranchRule(topologyId, brRuleInfo);
            return WSUtils.respond(createdBranchRuleInfo, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
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
     *   "stream": "stream1"
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
     *     "stream": "stream1"
     *   }
     * }
     * </pre>
     */
    @PUT
    @Path("/{id}")
    @Timed
    public Response addOrUpdateRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                    BranchRuleInfo brRuleInfo) {
        try {
            BranchRuleInfo createdRuleInfo = catalogService.addOrUpdateBranchRule(topologyId, ruleId, brRuleInfo);
            return WSUtils.respond(createdRuleInfo, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
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
     *     "stream": "stream1"
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/{id}")
    @Timed
    public Response removeRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId) {
        try {
            BranchRuleInfo ruleInfo = catalogService.removeBranchRule(ruleId);
            if (ruleInfo != null) {
                return WSUtils.respond(ruleInfo, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, ruleId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private String buildMessageForCompositeId(Long topologyId, Long ruleId) {
        return String.format("topology id <%d>, branch rule id <%d>", topologyId, ruleId);
    }
}
