package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.catalog.CatalogResponse;
import com.hortonworks.iotas.common.util.FileStorage;
import com.hortonworks.iotas.common.util.FileUtil;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.catalog.UDFInfo;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import org.apache.commons.codec.binary.Hex;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;

@Path("/api/v1/catalog/streams")
@Produces(MediaType.APPLICATION_JSON)
public class UDFCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(UDFCatalogResource.class);
    private final StreamCatalogService catalogService;
    private final FileStorage fileStorage;

    public UDFCatalogResource(StreamCatalogService catalogService, FileStorage fileStorage) {
        this.catalogService = catalogService;
        this.fileStorage = fileStorage;
    }

    /**
     * List ALL UDFs or the ones matching specific query params.
     * <p>
     * GET api/v1/catalog/udfs
     * </p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [
     *     {
     *       "id": 34,
     *       "name": "STDDEV",
     *       "description": "Standard deviation",
     *       "type": "AGGREGATE",
     *       "className": "com.hortonworks.iotas.streams.rule.udaf.Stddev"
     *     },
     *     { "id": 46,
     *        "name": "LOWER",
     *        "description": "Lowercase",
     *        "type": "FUNCTION",
     *        "className": "builtin"
     *     }
     *    ]
     * }
     * </pre>
     */
    @GET
    @Path("/udfs")
    @Timed
    public Response listUDFs(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<UDFInfo> udfs;
            if (params.isEmpty()) {
                udfs = catalogService.listUDFs();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                udfs = catalogService.listUDFs(queryParams);
            }
            if (udfs != null) {
                return WSUtils.respond(udfs, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * Get a specific UDF by id.
     *
     * <p>
     * GET api/v1/catalog/udfs/:ID
     * </p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 48,
     *     "name": "SUBSTRING",
     *     "description": "Substring",
     *     "type": "FUNCTION",
     *     "className": "builtin"
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/udfs/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUDFById(@PathParam("id") Long id) {
        try {
            UDFInfo result = catalogService.getUDF(id);
            if (result != null) {
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    /**
     * Add a new UDF.
     * <p>
     * curl -X POST 'http://localhost:8080/api/v1/catalog/udfs' -F udfJarFile=/tmp/foo-function.jar
     * -F udfConfig='{"name":"Foo", "description": "testing", "type":"FUNCTION", "className":"com.test.Foo"};type=application/json'
     * </p>
     */
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/udfs")
    public Response addUDF(@FormDataParam("udfJarFile") final InputStream inputStream,
                           @FormDataParam("udfJarFile") final FormDataContentDisposition contentDispositionHeader,
                           @FormDataParam("udfConfig") final FormDataBodyPart udfConfig,
                           @FormDataParam("builtin") final boolean builtin) {
        try {
            LOG.debug("Media type {}", udfConfig.getMediaType());
            if (!udfConfig.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return WSUtils.respond(UNSUPPORTED_MEDIA_TYPE, CatalogResponse.ResponseMessage.UNSUPPORTED_MEDIA_TYPE);
            }
            UDFInfo udfInfo = udfConfig.getValueAs(UDFInfo.class);
            processUdf(inputStream, udfInfo, true, builtin);
            UDFInfo createdUdfInfo = catalogService.addUDF(udfInfo);
            return WSUtils.respond(createdUdfInfo, CREATED, SUCCESS);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, CatalogResponse.ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * Remove a UDF by ID.
     *
     * <p>
     * DELETE api/v1/catalog/udfs/:ID
     * </p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 48,
     *     "name": "SUBSTRING",
     *     "description": "Substring",
     *     "type": "FUNCTION",
     *     "className": "builtin"
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/udfs/{id}")
    @Timed
    public Response removeUDF(@PathParam("id") Long id) {
        try {
            UDFInfo removedUDF = catalogService.removeUDF(id);
            if (removedUDF != null) {
                return WSUtils.respond(removedUDF, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * Update a udf.
     * <p>
     *     curl -X PUT 'http://localhost:8080/api/v1/catalog/udfs/34'
     *     -F udfJarFile=@/tmp/streams-functions-0.1.0-SNAPSHOT.jar
     *     -F udfConfig='{"name":"stddev", "description": "stddev",
     *                   "type":"AGGREGATE", "className":"com.hortonworks.iotas.streams.rule.udaf.Stddev"};type=application/json'
     * </p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 48,
     *     "name": "SUBSTRING",
     *     "description": "Substring",
     *     "type": "FUNCTION",
     *     "className": "builtin"
     *   }
     * }
     * </pre>
     */
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/udfs/{id}")
    @Timed
    public Response addOrUpdateUDF(@PathParam("id") Long udfId,
                                   @FormDataParam("udfJarFile") final InputStream inputStream,
                                   @FormDataParam("udfJarFile") final FormDataContentDisposition contentDispositionHeader,
                                   @FormDataParam("udfConfig") final FormDataBodyPart udfConfig,
                                   @FormDataParam("builtin") final boolean builtin) {
        try {
            LOG.debug("Media type {}", udfConfig.getMediaType());
            if (!udfConfig.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return WSUtils.respond(UNSUPPORTED_MEDIA_TYPE, CatalogResponse.ResponseMessage.UNSUPPORTED_MEDIA_TYPE);
            }
            UDFInfo udfInfo = udfConfig.getValueAs(UDFInfo.class);
            processUdf(inputStream, udfInfo, false, builtin);
            UDFInfo newUdfInfo = catalogService.addOrUpdateUDF(udfId, udfInfo);
            return WSUtils.respond(newUdfInfo, OK, SUCCESS);
        } catch (ProcessingException ex) {
            return WSUtils.respond(BAD_REQUEST, CatalogResponse.ResponseMessage.BAD_REQUEST);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * Download the jar corresponding to a specific UDF.
     * <p>
     * E.g. curl http://localhost:8080/api/v1/catalog/udfs/download/34 -o /tmp/file.jar
     * </p>
     */
    @Timed
    @GET
    @Produces({"application/java-archive", "application/json"})
    @Path("/udfs/download/{udfId}")
    public Response downloadUdf(@PathParam("udfId") Long udfId) {
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

    private void processUdf(InputStream inputStream,
                            UDFInfo udfInfo,
                            boolean checkDuplicate,
                            boolean builtin) throws Exception {
        if (builtin) {
            udfInfo.setDigest("builtin");
            udfInfo.setJarStoragePath("builtin");
            checkDuplicate(udfInfo);
        } else {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            File tmpFile;
            try (DigestInputStream dis = new DigestInputStream(inputStream, md)) {
                tmpFile = FileUtil.writeInputStreamToTempFile(inputStream, ".jar");
            }
            Set<String> udafs = catalogService.loadUdafsFromJar(tmpFile);
            validateUDF(udafs, udfInfo, checkDuplicate);
            String digest = Hex.encodeHexString(md.digest());
            LOG.debug("Digest: {}", digest);
            udfInfo.setDigest(digest);
            String jarPath = getExistingJarPath(digest).or(uploadJar(new FileInputStream(tmpFile), udfInfo.getName()));
            udfInfo.setJarStoragePath(jarPath);
        }
    }

    private String uploadJar(InputStream is, String udfName) throws IOException {
        String jarFileName;
        if (is != null) {
            jarFileName = UUID.randomUUID().toString() + ".jar";
            String uploadedPath = this.fileStorage.uploadFile(is, jarFileName);
            LOG.debug("Jar uploaded to {}", uploadedPath);
        } else {
            String message = String.format("Udf %s jar content is missing.", udfName);
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        return jarFileName;
    }

    private void validateUDF(Set<String> udfs, UDFInfo udfInfo, boolean checkDuplicate) {
        if (!udfs.contains(udfInfo.getClassName())) {
            throw new RuntimeException("Cannot load class from uploaded Jar: " + udfInfo.getClassName());
        }
        LOG.debug("Validating UDF, Class {} is in the available classes {}", udfInfo.getClassName(), udfs);
        if (checkDuplicate) {
            checkDuplicate(udfInfo);
        }
    }

    private void checkDuplicate(UDFInfo udfInfo) {
        Collection<UDFInfo> existing = catalogService.listUDFs(
                Collections.singletonList(new QueryParam(UDFInfo.NAME, udfInfo.getName())));
        if (!existing.isEmpty()) {
            throw new RuntimeException("UDF with the same name already exists, use update (PUT) api instead");
        }
    }

    /**
     * See if there is already a jar with the same digest
     */
    private Optional<String> getExistingJarPath(String digest) {
        Collection<UDFInfo> existing = catalogService.listUDFs(
                Collections.singletonList(new QueryParam(UDFInfo.DIGEST, digest)));
        if (existing.size() >= 1) {
            return Optional.of(existing.iterator().next().getJarStoragePath());
        }
        return Optional.absent();
    }
}
