package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.catalog.UDFInfo;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.util.FileStorage;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class UDFCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(UDFCatalogResource.class);
    private final CatalogService catalogService;
    private final FileStorage fileStorage;

    public UDFCatalogResource(CatalogService catalogService, FileStorage fileStorage) {
        this.catalogService = catalogService;
        this.fileStorage = fileStorage;
    }

    /**
     * List ALL UDFs or the ones matching specific query params.
     */
    @GET
    @Path("/udfs")
    @Timed
    public Response listUDFs(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<UDFInfo> udfs;
            if (params.isEmpty()) {
                udfs = catalogService.listUDFs();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                udfs = catalogService.listUDFs(queryParams);
            }
            if (udfs != null && !udfs.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, udfs);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/udfs/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUDFById(@PathParam("id") Long id) {
        try {
            UDFInfo result = catalogService.getUDF(id);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    // curl -X POST 'http://localhost:8080/api/v1/catalog/udfs' -F udfJarFile=/tmp/foo-function.jar
    // -F udfConfig='{"name":"foo", "description": "testing", "type":"FUNCTION", "className":"com.test.Foo"};type=application/json'
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/udfs")
    public Response addUDF(@FormDataParam("udfJarFile") final InputStream inputStream,
                               @FormDataParam("udfJarFile") final FormDataContentDisposition contentDispositionHeader,
                               @FormDataParam("udfConfig") final FormDataBodyPart udfConfig) {
        try {
            LOG.debug("Media type {}", udfConfig.getMediaType());
            if (!udfConfig.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return WSUtils.respond(UNSUPPORTED_MEDIA_TYPE, CatalogResponse.ResponseMessage.UNSUPPORTED_MEDIA_TYPE);
            }
            UDFInfo udfInfo = udfConfig.getValueAs(UDFInfo.class);
            saveUDF(inputStream, udfInfo);
            UDFInfo createdUdfInfo = catalogService.addUDF(udfInfo);
            return WSUtils.respond(CREATED, SUCCESS, createdUdfInfo);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, CatalogResponse.ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/udfs/{id}")
    @Timed
    public Response removeUDF(@PathParam("id") Long id) {
        try {
            UDFInfo removedUDF = catalogService.removeUDF(id);
            if (removedUDF != null) {
                return WSUtils.respond(OK, SUCCESS, removedUDF);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/udfs/{id}")
    @Timed
    public Response addOrUpdateUDF(@PathParam("id") Long udfId,
                                       @FormDataParam("udfJarFile") final InputStream inputStream,
                                       @FormDataParam("udfJarFile") final FormDataContentDisposition contentDispositionHeader,
                                       @FormDataParam("udfConfig") final FormDataBodyPart udfConfig) {
        try {
            LOG.debug("Media type {}", udfConfig.getMediaType());
            if (!udfConfig.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return WSUtils.respond(UNSUPPORTED_MEDIA_TYPE, CatalogResponse.ResponseMessage.UNSUPPORTED_MEDIA_TYPE);
            }
            UDFInfo udfInfo = udfConfig.getValueAs(UDFInfo.class);
            saveUDF(inputStream, udfInfo);
            UDFInfo newUdfInfo = catalogService.addOrUpdateUDF(udfId, udfInfo);
            return WSUtils.respond(OK, SUCCESS, newUdfInfo);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, CatalogResponse.ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @Timed
    @GET
    @Produces({"application/java-archive", "application/json"})
    @Path("/udfs/download/{udfId}")
    public Response downloadParserJar(@PathParam("udfId") Long udfId) {
        try {
            UDFInfo udfInfo = catalogService.getUDF(udfId);
            if (udfInfo != null) {
                StreamingOutput streamOutput = WSUtils.wrapWithStreamingOutput(
                        catalogService.downloadFileFromStorage(udfInfo.getJarStoragePath()));
                return Response.ok(streamOutput).build();
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, udfId.toString());
    }

    private void saveUDF(InputStream is, UDFInfo udfInfo) throws IOException {
        if (is != null) {
            String jarFileName = udfInfo.getName() + "-" + UUID.randomUUID().toString();
            String uploadedPath = this.fileStorage.uploadFile(is, jarFileName);
            udfInfo.setJarStoragePath(uploadedPath);
            LOG.debug("Jar uploaded to {}", uploadedPath);
        } else {
            String message = String.format("Udf %s jar content is missing.", udfInfo.getName());
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
    }
}
