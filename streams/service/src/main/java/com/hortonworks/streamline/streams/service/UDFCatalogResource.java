/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.common.exception.ParserException;
import com.hortonworks.streamline.common.util.FileStorage;
import com.hortonworks.streamline.common.util.FileUtil;
import com.hortonworks.streamline.common.util.ProxyUtil;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.UDFInfo;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.commons.codec.binary.Hex;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.request.UnsupportedMediaTypeException;
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
import java.lang.reflect.Method;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog/streams")
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
     *       "className": "com.hortonworks.streamline.streams.rule.udaf.Stddev"
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
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        Collection<UDFInfo> udfs;
        if (params.isEmpty()) {
            udfs = catalogService.listUDFs();
        } else {
            queryParams = WSUtils.buildQueryParameters(params);
            udfs = catalogService.listUDFs(queryParams);
        }
        if (udfs != null) {
            return WSUtils.respondEntities(udfs, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
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
    public Response getUDFById(@PathParam("id") Long id) {
        UDFInfo result = catalogService.getUDF(id);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
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
                           @FormDataParam("builtin") final boolean builtin) throws Exception {
        MediaType mediaType = udfConfig.getMediaType();
        LOG.debug("Media type {}", mediaType);
        if (!mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            throw new UnsupportedMediaTypeException(mediaType.toString());
        }
        UDFInfo udfInfo = udfConfig.getValueAs(UDFInfo.class);
        processUdf(inputStream, udfInfo, true, builtin);
        UDFInfo createdUdfInfo = catalogService.addUDF(udfInfo);
        return WSUtils.respondEntity(createdUdfInfo, CREATED);
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
        UDFInfo removedUDF = catalogService.removeUDF(id);
        if (removedUDF != null) {
            return WSUtils.respondEntity(removedUDF, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }

    /**
     * Update a udf.
     * <p>
     *     curl -X PUT 'http://localhost:8080/api/v1/catalog/udfs/34'
     *     -F udfJarFile=@/tmp/streams-functions-0.1.0-SNAPSHOT.jar
     *     -F udfConfig='{"name":"stddev", "description": "stddev",
     *                   "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.rule.udaf.Stddev"};type=application/json'
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
                                   @FormDataParam("builtin") final boolean builtin)
        throws Exception {
        MediaType mediaType = udfConfig.getMediaType();
        LOG.debug("Media type {}", mediaType);
        if (!mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            throw new UnsupportedMediaTypeException(mediaType.toString());
        }
        UDFInfo udfInfo = udfConfig.getValueAs(UDFInfo.class);
        processUdf(inputStream, udfInfo, false, builtin);
        UDFInfo newUdfInfo = catalogService.addOrUpdateUDF(udfId, udfInfo);
        return WSUtils.respondEntity(newUdfInfo, CREATED);
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
    public Response downloadUdf(@PathParam("udfId") Long udfId) throws IOException {
        UDFInfo udfInfo = catalogService.getUDF(udfId);
        if (udfInfo != null) {
            StreamingOutput streamOutput = WSUtils.wrapWithStreamingOutput(
                    catalogService.downloadFileFromStorage(udfInfo.getJarStoragePath()));
            return Response.ok(streamOutput).build();
        }

        throw EntityNotFoundException.byId(udfId.toString());
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
                tmpFile = FileUtil.writeInputStreamToTempFile(dis, ".jar");
            }
            Map<String, Class<?>> udfs = catalogService.loadUdfsFromJar(tmpFile);
            validateUDF(new HashSet<>(ProxyUtil.canonicalNames(udfs.values())), udfInfo, checkDuplicate);
            updateTypeInfo(udfInfo, udfs.get(udfInfo.getClassName()));
            String digest = Hex.encodeHexString(md.digest());
            LOG.debug("Digest: {}", digest);
            udfInfo.setDigest(digest);
            String jarPath = getExistingJarPath(digest).or(uploadJar(new FileInputStream(tmpFile), udfInfo.getName()));
            udfInfo.setJarStoragePath(jarPath);
        }
    }

    private void updateTypeInfo(UDFInfo udfInfo, Class<?> clazz) {
        if (udfInfo.isAggregate()) {
            udfInfo.setReturnType(getReturnType(clazz, "result"));
            udfInfo.setArgTypes(getArgTypes(clazz, "add", 1));
        } else {
            udfInfo.setReturnType(getReturnType(clazz, "evaluate"));
            udfInfo.setArgTypes(getArgTypes(clazz, "evaluate", 0));
        }
    }

    private Schema.Type getReturnType(Class<?> clazz, String methodName) {
        try {
            Method method = findMethod(clazz, methodName);
            if (method != null) {
                return Schema.fromJavaType(method.getReturnType());
            }
        } catch (ParserException ex) {
            LOG.warn("Could not determine return type for {}", clazz);
        }
        return null;
    }

    private List<String> getArgTypes(Class<?> clazz, String methodname, int argStartIndex) {
        Method addMethod = findMethod(clazz, methodname);
        if (addMethod == null) {
            return Collections.emptyList();
        }
        final Class<?>[] params = addMethod.getParameterTypes();
        List<String> argTypes = new ArrayList<>();
        for (int i = argStartIndex; i < params.length; i++) {
            final Class<?> arg = params[i];
            try {
                argTypes.add(Schema.fromJavaType(arg).toString());
            } catch (ParserException ex) {
                Collection<Schema.Type> types = Collections2.filter(Arrays.asList(Schema.Type.values()), new Predicate<Schema.Type>() {
                    public boolean apply(Schema.Type input) {
                        return arg.isAssignableFrom(input.getJavaType());
                    }
                });
                if (types.isEmpty()) {
                    LOG.error("Could not find a compatible type in schema for {} argument types", addMethod);
                    return Collections.emptyList();
                } else {
                    argTypes.add(Joiner.on("|").join(types));
                }
            }
        }
        return argTypes;
    }

    private Method findMethod(Class<?> clazz, String name) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name) && !method.isBridge()) {
                return method;
            }
        }
        return null;
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
