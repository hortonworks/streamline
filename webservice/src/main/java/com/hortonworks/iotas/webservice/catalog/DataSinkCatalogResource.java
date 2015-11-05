package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataSink;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.apache.commons.lang3.StringUtils;

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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.DATASOURCE_TYPE_FILTER_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 *
 */
@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)

public class DataSinkCatalogResource {
    private CatalogService catalogService;

    public DataSinkCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @POST
    @Path("/datasinks")
    @Timed
    public Response addDataSink(DataSink dataSource) {
        try {
            if (StringUtils.isEmpty(dataSource.getTypeConfig())) {
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "typeConfig");
            }
            DataSink createdDataSink = catalogService.addDataSink(dataSource);
            return WSUtils.respond(CREATED, SUCCESS, createdDataSink);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/datasinks/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataSinkById(@PathParam("id") Long dataSourceId) {
        try {
            DataSink result = catalogService.getDataSink(dataSourceId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataSourceId.toString());
    }

    @GET
    @Path("/datasinks")
    @Timed
    public Response listDataSinks(@QueryParam("filter") List<String> filter) {
        try {
            Collection<DataSink> dataSources = catalogService.listDataSinks();
            return WSUtils.respond(OK, SUCCESS, dataSources);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * List datasink matching the type and the type specific fields and values.
     */
    @GET
    @Path("/datasinks/type/{type}")
    @Timed
    public Response listDataSinksForTypeWithFilter(@PathParam("type") DataSink.Type type,
                                                     @Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            queryParams = WSUtils.buildQueryParameters(params);
            Collection<DataSink> dataSources = catalogService.listDataSinksForType(type, queryParams);
            if (!dataSources.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, dataSources);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, DATASOURCE_TYPE_FILTER_NOT_FOUND, type.toString(), queryParams.toString());
    }

    @DELETE
    @Path("/datasinks/{id}")
    @Timed
    public Response removeDataSink(@PathParam("id") Long dataSourceId) {
        try {
            DataSink removedDataSink = catalogService.removeDataSink(dataSourceId);
            if (removedDataSink != null) {
                return WSUtils.respond(OK, SUCCESS, removedDataSink);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataSourceId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/datasinks/{id}")
    @Timed
    public Response addOrUpdateDataSink(@PathParam("id") Long dataSourceId, DataSink dataSource) {
        try {
            if (StringUtils.isEmpty(dataSource.getTypeConfig())) {
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "typeConfig");
            }
            DataSink result = catalogService.addOrUpdateDataSink(dataSourceId, dataSource);
            return WSUtils.respond(OK, SUCCESS, dataSource);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

}
