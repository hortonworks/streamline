package com.hortonworks.iotas.webservice.schema;


import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.layout.schema.CatalogServiceAware;
import com.hortonworks.iotas.layout.schema.EvolvingSchema;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.TopologyComponent;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/schema")
@Produces(MediaType.APPLICATION_JSON)
public class SchemaAPI {
    private final CatalogService catalogService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<Long, EvolvingSchema> EVOLVING_SCHEMA_MAP = new ConcurrentHashMap<>();

    public SchemaAPI(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Simulate schema evolution with given informations including configuration of component and input stream.
     */
    @GET
    @Path("/evolve")
    @Timed
    public Response simulateEvolution(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

        // TODO: include to path variable?
        Long componentId = Long.parseLong(params.getFirst("componentId"));
        String configurationJson = params.getFirst("configuration");
        String streamJson = params.getFirst("stream");

        try {
            if (!EVOLVING_SCHEMA_MAP.containsKey(componentId)) {
                TopologyComponent topologyComponent = catalogService.getTopologyComponent(componentId);
                if (topologyComponent == null) {
                    throw new NotFoundException("Component ID " + componentId + " not found in catalog");
                }

                String schemaClass = topologyComponent.getSchemaClass();
                if (schemaClass == null) {
                    throw new UnsupportedOperationException("This component doesn't support Schema Evolution");
                }

                ensureLoadSchemaEvolvingClass(componentId, schemaClass);
            }

            EvolvingSchema evolvingInstance = EVOLVING_SCHEMA_MAP.get(componentId);
            Set<Stream> appliedStreams = evolvingInstance.apply(configurationJson, objectMapper.readValue(streamJson, Stream.class));
            return WSUtils.respond(OK, SUCCESS, appliedStreams);
        } catch (NotFoundException ex) {
            return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, componentId.toString());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private void ensureLoadSchemaEvolvingClass(Long componentId, String schemaClass)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class klazz = Class.forName(schemaClass);
        EvolvingSchema evolvingSchemaInstance = (EvolvingSchema) klazz.newInstance();
        // inject CatalogService if needed
        if (evolvingSchemaInstance instanceof CatalogServiceAware) {
            ((CatalogServiceAware) evolvingSchemaInstance).setCatalogService(catalogService);
        }
        EVOLVING_SCHEMA_MAP.putIfAbsent(componentId, evolvingSchemaInstance);
    }

}
