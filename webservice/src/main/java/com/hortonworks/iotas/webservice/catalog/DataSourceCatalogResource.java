package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class DataSourceCatalogResource {
    private CatalogService catalogService;

    public DataSourceCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/datasources")
    @Timed
    public Response listDataSources(@QueryParam("filter") List<String> filter) {
        try {
            Collection<DataSource> dataSources = catalogService.listDataSources();
            return WSUtils.respond(OK, SUCCESS, dataSources);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * List datasource matching the type and the type specific fields and values.
     */
    @GET
    @Path("/datasources/type/{type}")
    @Timed
    public Response listDataSourcesForTypeWithFilter(@PathParam("type") DataSource.Type type,
                                                     @Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            for(String param: params.keySet()) {
                queryParams.add(new CatalogService.QueryParam(param, params.getFirst(param)));
            }
            Collection<DataSource> dataSources = catalogService.listDataSourcesForType(type, queryParams);
            if(! dataSources.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, dataSources);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, DATASOURCE_TYPE_FILTER_NOT_FOUND, type.toString(), queryParams.toString());
    }

    @GET
    @Path("/datasources/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataSourceById(@PathParam("id") Long dataSourceId) {
        try {
            DataSource result = catalogService.getDataSource(dataSourceId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataSourceId.toString());
    }

    @POST
    @Path("/datasources")
    @Timed
    public Response addDataSource(DataSource dataSource) {
        try {
           if (StringUtils.isEmpty(dataSource.getTypeConfig())) {
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "typeConfig");
            }
            DataSource createdDataSource = catalogService.addDataSource(dataSource);
            return WSUtils.respond(CREATED, SUCCESS, createdDataSource);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/datasources/{id}")
    @Timed
    public Response removeDataSource(@PathParam("id") Long dataSourceId) {
        try {
            DataSource removedDataSource = catalogService.removeDataSource(dataSourceId);
            if (removedDataSource != null) {
                return WSUtils.respond(OK, SUCCESS, removedDataSource);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataSourceId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/datasources/{id}")
    @Timed
    public Response addOrUpdateDataSource(@PathParam("id") Long dataSourceId, DataSource dataSource) {
        try {
           if (StringUtils.isEmpty(dataSource.getTypeConfig())) {
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "typeConfig");
            }
            DataSource result = catalogService.addOrUpdateDataSource(dataSourceId, dataSource);
            return WSUtils.respond(OK, SUCCESS, dataSource);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

}

