/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.File;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Catalog resource for {@link File} resources.
 */
@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class FileCatalogResource {
    private static final Logger log = LoggerFactory.getLogger(FileCatalogResource.class);

    private final CatalogService catalogService;

    public FileCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/files")
    @Timed
    public Response listFiles(@Context UriInfo uriInfo) {
        try {
            Collection<File> files = null;
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            if (params == null || params.isEmpty()) {
                files = catalogService.listFiles();
            } else {
                files = catalogService.listFiles(WSUtils.buildQueryParameters(params));
            }
            return WSUtils.respond(OK, SUCCESS, files);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * Adds given resource to the configured file-storage and adds an entry in entity storage.
     *
     * Below example describes how a file can be added along with metadata
     * <blockquote><pre>
     * curl -X POST -i -F file=@user-lib.jar -F "fileInfo={\"name\":\"jar-1\",\"version\":1};type=application/json"  http://localhost:8080/api/v1/catalog/files
     *
     * HTTP/1.1 100 Continue
     *
     * HTTP/1.1 201 Created
     * Date: Fri, 15 Apr 2016 10:36:33 GMT
     * Content-Type: application/json
     * Content-Length: 239
     *
     * {"responseCode":1000,"responseMessage":"Success","entity":{"id":1234,"name":"jar-1","className":null,"storedFileName":"/tmp/test-hdfs/jar-1-ea41fe3a-12f9-45d4-ae24-818d570b8963.jar","version":1,"timestamp":1460716593157,"auxiliaryInfo":null}}
     * </pre></blockquote>
     *
     * @param inputStream actual file content as {@link InputStream}.
     * @param contentDispositionHeader {@link FormDataContentDisposition} instance of the received file
     * @param file configuration of the file resource {@link File}
     * @return
     */
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/files")
    public Response addFile(@FormDataParam("file") final InputStream inputStream,
                            @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader,
                            @FormDataParam("fileInfo") final File file) {

        try {
            log.info("Received file: [{}]", file);
            File updatedFile = addOrUpdateFile(inputStream, file);

            return WSUtils.respond(CREATED, SUCCESS, updatedFile);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    protected String getFileStorageName(String fileName) {
        return (StringUtils.isBlank(fileName) ? "file" : fileName) + "-" + UUID.randomUUID().toString();
    }

    /**
     *
     * @param inputStream
     * @param contentDispositionHeader
     * @param file
     */
    @Timed
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/files")
    public Response updateFile(@FormDataParam("file") final InputStream inputStream,
                               @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader,
                               @FormDataParam("fileInfo") final File file) {
        try {
            log.info("Received file: [{}]", file);
            String oldFileStorageName = null;
            final File existingFile = catalogService.getFile(file.getId());
            if(existingFile != null) {
                oldFileStorageName = existingFile.getStoredFileName();
            }

            final File updatedFile = addOrUpdateFile(inputStream, file);

            if(oldFileStorageName != null) {
                final boolean deleted = catalogService.deleteFileFromStorage(oldFileStorageName);
                logDeletionMessage(oldFileStorageName, deleted);
            }

            return WSUtils.respond(CREATED, SUCCESS, updatedFile);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    protected File addOrUpdateFile(InputStream inputStream, File file) throws IOException {
        final String updatedFileStorageName = getFileStorageName(file.getName());
        file.setStoredFileName(updatedFileStorageName);
        log.info("Uploading File [{}]", file);
        final String uploadedFileStoragePath = catalogService.uploadFileToStorage(inputStream, updatedFileStorageName);
        log.info("Received File file is uploaded to [{}]", uploadedFileStoragePath);
        file.setTimestamp(System.currentTimeMillis());
        return catalogService.addOrUpdateFile(file);
    }

    @GET
    @Path("/files/{id}")
    @Timed
    public Response getFile(@PathParam("id") Long fileId) {
        try {
            File result = catalogService.getFile(fileId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, fileId.toString());
    }

    /**
     * Deletes the file of given {@code fileId}
     *
     * @param fileId
     */
    @DELETE
    @Path("/files/{id}")
    @Timed
    public Response removeFile(@PathParam("id") Long fileId) {
        try {
            File removedFile = catalogService.removeFile(fileId);
            log.info("Removed File entry is [{}]", removedFile);
            if (removedFile != null) {
                boolean removed = catalogService.deleteFileFromStorage(removedFile.getStoredFileName());
                logDeletionMessage(removedFile.getStoredFileName(), removed);
                return WSUtils.respond(OK, SUCCESS, removedFile);
            } else {
                log.info("File entry with id [{}] is not found", fileId);
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, fileId.toString());
            }
        } catch (Exception ex) {
            log.error("Encountered error in removing file with id [{}]", fileId, ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    protected void logDeletionMessage(String removedFileName, boolean removed) {
        log.info("Delete action for File [{}] from storage is [{}]", removedFileName, removed ? "success" : "failure" );
    }

    /**
     * Downloads a given {@link File} resource for given {@code fileId}
     *
     * @param fileId
     */
    @Timed
    @GET
    @Produces({"application/octet-stream", "application/json"})
    @Path("/files/download/{fileId}")
    public Response downloadFile(@PathParam("fileId") Long fileId) {
        try {
            File file = catalogService.getFile(fileId);
            if (file != null) {
                StreamingOutput streamOutput = WSUtils.wrapWithStreamingOutput(catalogService.downloadFileFromStorage(file.getStoredFileName()));
                return Response.ok(streamOutput).build();
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, fileId.toString());
    }

}
