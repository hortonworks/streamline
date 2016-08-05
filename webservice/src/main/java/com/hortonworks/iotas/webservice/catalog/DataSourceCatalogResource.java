package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.streams.catalog.DataFeed;
import com.hortonworks.iotas.streams.catalog.DataSource;
import com.hortonworks.iotas.streams.catalog.ParserInfo;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.streams.catalog.service.CatalogService;
import com.hortonworks.iotas.common.util.WSUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.DATASOURCE_SCHEMA_NOT_UNIQUE;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.DATASOURCE_TYPE_FILTER_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class DataSourceCatalogResource {
    private CatalogService catalogService;

    public DataSourceCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/deprecated/datasources")
    @Timed
    public Response listDataSources(@javax.ws.rs.QueryParam("filter") List<String> filter) {
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
    @Path("/deprecated/datasources/type/{type}")
    @Timed
    public Response listDataSourcesForTypeWithFilter(@PathParam("type") DataSource.Type type,
                                                     @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            queryParams = WSUtils.buildQueryParameters(params);
            Collection<DataSource> dataSources = catalogService.listDataSourcesForType(type, queryParams);
            if (!dataSources.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, dataSources);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, DATASOURCE_TYPE_FILTER_NOT_FOUND, type.toString(), queryParams.toString());
    }

    @GET
    @Path("/deprecated/datasources/{id}")
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

    @GET
    @Path("/deprecated/datasources/{id}/schema")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataSourceSchema(@PathParam("id") Long dataSourceId) {
        try {
            List<QueryParam> qp = Arrays.asList(new QueryParam("dataSourceId", dataSourceId.toString()));
            Collection<DataFeed> feeds = catalogService.listDataFeeds(qp);
            if (feeds != null) {
                if (feeds.size() == 1) {
                    ParserInfo parserInfo = catalogService.getParserInfo(feeds.iterator().next().getParserId());
                    if (parserInfo != null) {
                        Schema schema = parserInfo.getParserSchema();
                        if (schema != null) {
                            return WSUtils.respond(OK, SUCCESS, schema);
                        }
                    }
                } else if (feeds.size() > 1) {
                    return WSUtils.respond(NOT_FOUND, DATASOURCE_SCHEMA_NOT_UNIQUE, String.valueOf(feeds.size()));
                }
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND, dataSourceId.toString());
    }

    @POST
    @Path("/deprecated/datasources")
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
    @Path("/deprecated/datasources/{id}")
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
    @Path("/deprecated/datasources/{id}")
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

