package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.RuleInfo;
import org.apache.streamline.streams.catalog.TopologySource;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.layout.component.rule.Rule;

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

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.streamline.common.util.WSUtils.buildTopologyIdAndVersionIdAwareQueryParams;

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
    public Response listTopologyRules(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        Long currentVersionId = catalogService.getCurrentTopologyVersionId(topologyId);
        return listTopologyRules(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/rules")
    @Timed
    public Response listTopologySourcesForVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("versionId") Long versionId,
                                                  @Context UriInfo uriInfo) {
        return listTopologyRules(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo));
    }

    private Response listTopologyRules(List<QueryParam> queryParams) {
        try {
            Collection<RuleInfo> ruleInfos = catalogService.listRules(queryParams);
            if (ruleInfos != null) {
                return WSUtils.respond(ruleInfos, OK, SUCCESS);
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
    @Path("/topologies/{topologyId}/rules/{id}")
    @Timed
    public Response getTopologyRuleById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId) {
        try {
            RuleInfo ruleInfo = catalogService.getRule(topologyId, ruleId);
            if (ruleInfo != null) {
                return WSUtils.respond(ruleInfo, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, ruleId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/rules/{id}")
    @Timed
    public Response getTopologyRuleByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("id") Long ruleId,
                                                  @PathParam("versionId") Long versionId) {
        try {
            RuleInfo ruleInfo = catalogService.getRule(topologyId, ruleId, versionId);
            if (ruleInfo != null) {
                return WSUtils.respond(ruleInfo, OK, SUCCESS);
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
    @Path("/topologies/{topologyId}/rules")
    @Timed
    public Response addTopologyRule(@PathParam("topologyId") Long topologyId, RuleInfo ruleInfo) {
        try {
            RuleInfo createdRuleInfo = catalogService.addRule(topologyId, ruleInfo);
            return WSUtils.respond(createdRuleInfo, CREATED, SUCCESS);
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
    @Path("/topologies/{topologyId}/rules/{id}")
    @Timed
    public Response addOrUpdateRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                                 RuleInfo ruleInfo) {
        try {
            RuleInfo createdRuleInfo = catalogService.addOrUpdateRule(topologyId, ruleId, ruleInfo);
            return WSUtils.respond(createdRuleInfo, CREATED, SUCCESS);
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
    @Path("/topologies/{topologyId}/rules/{id}")
    @Timed
    public Response removeRule(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId) {
        try {
            RuleInfo ruleInfo = catalogService.removeRule(topologyId, ruleId);
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
        return String.format("topology id <%d>, rule id <%d>", topologyId, ruleId);
    }
}
