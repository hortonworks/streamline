package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.DataStream;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.util.DataStreamLayoutValidator;
import com.hortonworks.iotas.util.exception.BadDataStreamLayoutException;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.crypto.Data;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class DataStreamCatalogResource {
    private CatalogService catalogService;
    private final URL SCHEMA = Thread.currentThread().getContextClassLoader()
            .getResource("assets/schemas/datastream.json");

    public DataStreamCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/datastreams")
    @Timed
    public Response listDataStreams() {
        try {
            Collection<DataStream> dataStreams = catalogService
                    .listDataStreams();
            return WSUtils.respond(OK, SUCCESS, dataStreams);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/datastreams/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataStreamById(@PathParam("id") Long dataStreamId) {
        try {
            DataStream result = catalogService.getDataStream(dataStreamId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataStreamId.toString());
    }

    @POST
    @Path("/datastreams")
    @Timed
    public Response addDataStream(DataStream dataStream) {
        try {
            if (StringUtils.isEmpty(dataStream.getDataStreamName())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, "dataStreamName");
            }
            if (StringUtils.isEmpty(dataStream.getJson())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, "json");
            }
            DataStream createdDataStream = catalogService.addDataStream
                    (dataStream);
            return WSUtils.respond(CREATED, SUCCESS, createdDataStream);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/datastreams/{id}")
    @Timed
    public Response removeDataStream(@PathParam("id") Long dataStreamId) {
        try {
            DataStream removedDataStream = catalogService.removeDataStream
                    (dataStreamId);
            if (removedDataStream != null) {
                return WSUtils.respond(OK, SUCCESS, removedDataStream);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND,
                        dataStreamId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/datastreams/{id}")
    @Timed
    public Response addOrUpdateDataStream(@PathParam("id") Long dataStreamId,
                                      DataStream dataStream) {
        try {
            if (StringUtils.isEmpty(dataStream.getDataStreamName())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, "dataStreamName");
            }
            if (StringUtils.isEmpty(dataStream.getJson())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, "json");
            }
            DataStream result = catalogService.addOrUpdateDataStream
                    (dataStreamId, dataStream);
            return WSUtils.respond(OK, SUCCESS, dataStream);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @POST
    @Path("/datastreams/{id}/actions/validate")
    @Timed
    public Response validateDataStream (@PathParam("id") Long dataStreamId) {
        try {
            DataStream result = catalogService.getDataStream(dataStreamId);
            if (result != null) {
                catalogService.validateDataStream(SCHEMA, dataStreamId);
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (BadDataStreamLayoutException ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataStreamId.toString());
    }

    @POST
    @Path("/datastreams/{id}/actions/deploy")
    @Timed
    public Response deployDataStream (@PathParam("id") Long dataStreamId) {
        try {
            DataStream result = catalogService.getDataStream(dataStreamId);
            if (result != null) {
                catalogService.validateDataStream(SCHEMA, dataStreamId);
                catalogService.deployDataStream(result);
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataStreamId.toString());
    }

    @POST
    @Path("/datastreams/{id}/actions/kill")
    @Timed
    public Response killDataStream (@PathParam("id") Long dataStreamId) {
        try {
            DataStream result = catalogService.getDataStream(dataStreamId);
            if (result != null) {
                catalogService.killDataStream(result);
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataStreamId.toString());
    }

}

