package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.RuleInfo;
import com.hortonworks.iotas.catalog.TopologyProcessor;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.component.rule.Rule;
import com.hortonworks.iotas.webservice.util.WSUtils;

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

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST resource for managing rules.
 * <p>
 * Having a separate resource for rules makes it easier to handle the parsing
 * and validation and keeps rule as an independent entity which
 * can be queried via the catalog service.
 * </p>
 * <p>
 * A {@link RuleInfo} sql is parsed and converted to
 * the corresponding {@link Rule} object and saved in the catalog db.
 * </p>
 */
@Path("/api/v1/catalog/topologies/{topologyId}/rules")
@Produces(MediaType.APPLICATION_JSON)
public class RuleCatalogResource {
    private CatalogService catalogService;

    public RuleCatalogResource(CatalogService catalogService) {
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
    @Timed
    public Response listTopologyRules(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAwareQueryParams(topologyId, uriInfo);
        try {
            Collection<RuleInfo> ruleInfos = catalogService.listRules(queryParams);
            if (ruleInfos != null && !ruleInfos.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, ruleInfos);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
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
    @Path("/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologyRuleById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId) {
        try {
            RuleInfo ruleInfo = catalogService.getRule(ruleId);
            if (ruleInfo != null && ruleInfo.getTopologyId().equals(topologyId)) {
                return WSUtils.respond(OK, SUCCESS, ruleInfo);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, ruleId));
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
    @Timed
    public Response addTopologyRule(@PathParam("topologyId") Long topologyId, RuleInfo ruleInfo) {
        try {
            RuleInfo createdRuleInfo = catalogService.addRule(topologyId, ruleInfo);
            return WSUtils.respond(CREATED, SUCCESS, createdRuleInfo);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
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
    @Path("/{id}")
    @Timed
    public Response addOrUpdateRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                                 RuleInfo ruleInfo) {
        try {
            RuleInfo createdRuleInfo = catalogService.addOrUpdateRule(topologyId, ruleId, ruleInfo);
            return WSUtils.respond(CREATED, SUCCESS, createdRuleInfo);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
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
    @Path("/{id}")
    @Timed
    public Response removeRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId) {
        try {
            RuleInfo ruleInfo = catalogService.removeRule(ruleId);
            if (ruleInfo != null) {
                return WSUtils.respond(OK, SUCCESS, ruleInfo);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, ruleId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private String buildMessageForCompositeId(Long topologyId, Long ruleId) {
        return String.format("topology id <%d>, rule id <%d>", topologyId, ruleId);
    }
}
