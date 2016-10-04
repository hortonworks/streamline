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
package com.hortonworks.iotas.registries.parser.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.registries.parser.ParserInfo;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ParserInfoCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ParserInfoCatalogResource.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ParsersCatalogService parserInfoCatalogService;

    public ParserInfoCatalogResource(ParsersCatalogService service) {
        this.parserInfoCatalogService = service;
    }

    @GET
    @Path("/parsers")
    @Timed
    // TODO add a way to query/filter and/or page results
    public Response listParsers(@Context UriInfo uriInfo) {
        try {
            Collection<ParserInfo> parserInfos = null;
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            if (params == null || params.isEmpty()) {
                parserInfos = parserInfoCatalogService.listParsers();
            } else {
                parserInfos = parserInfoCatalogService.listParsers(WSUtils.buildQueryParameters(params));
            }
            return WSUtils.respond(OK, SUCCESS, parserInfos);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/parsers/{id}/schema")
    @Timed
    public Response getParserSchema(@PathParam("id") Long parserId) {
        try {
            ParserInfo parserInfo = doGetParserInfoById(parserId);
            if (parserInfo != null) {
                Schema result = parserInfo.getParserSchema();
                if (result != null) {
                    return WSUtils.respond(OK, SUCCESS, result);
                }
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND, parserId.toString());
    }

    @GET
    @Path("/parsers/{id}")
    @Timed
    public Response getParserInfoById(@PathParam("id") Long parserId) {
        try {
            ParserInfo result = doGetParserInfoById(parserId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, parserId.toString());
    }

    @DELETE
    @Path("/parsers/{id}")
    @Timed
    public Response removeParser(@PathParam("id") Long parserId) {
        try {
            ParserInfo removedParser = parserInfoCatalogService.removeParser(parserId);
            if (removedParser != null) {
                return WSUtils.respond(OK, SUCCESS, removedParser);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, parserId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }



    //Test curl command curl -X POST -i -F parserJar=@parsers-0.1.0-SNAPSHOT.jar http://localhost:8080/api/v1/catalog/parsers/upload-verify
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers/upload-verify")
    public Response verifyParserUpload(@FormDataParam("parserJar") final InputStream inputStream) {
        try {
            return WSUtils.respond(Response.Status.OK, SUCCESS, parserInfoCatalogService.verifyParserUpload(inputStream));
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOG.debug("Swallowing file close exception", e);
            }
        }
    }

    //Test curl command curl -X POST -i -F parserJar=@original-webservice-0.1.0-SNAPSHOT.jar -F parserInfo='{"parserName":"TestParser","className":"some.test.parserClass","version":0}' http://localhost:8080/api/v1/catalog/parsers
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers")
    public Response addParser(@FormDataParam("parserJar") final InputStream inputStream, @FormDataParam("parserJar") final FormDataContentDisposition contentDispositionHeader,
                              @FormDataParam("parserInfo") final String parserInfoStr, @FormDataParam("schemaFromParserJar") boolean schemaFromParserJar) {

        try {
            ParserInfo parserInfo = objectMapper.readValue(parserInfoStr, ParserInfo.class);
            String prefix = StringUtils.isBlank(parserInfo.getName()) ? "parser-" : parserInfo.getName() + "-";
            String jarStoragePath = prefix + UUID.randomUUID().toString() + ".jar";
            parserInfo.setJarStoragePath(jarStoragePath);
            ParserInfo result = parserInfoCatalogService.addParser(parserInfo, schemaFromParserJar, inputStream);
            return WSUtils.respond(CREATED, SUCCESS, result);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOG.debug("Swallowing file close exception", e);
            }
        }
    }

    //TODO Still need to implement update/PUT

    //TODO, is it better to expect that clients will know the parserId or the jarStoragePath? I like parserId as it
    //hides the storage details from clients.
    @Timed
    @GET
    @Produces({"application/java-archive", "application/json"})
    @Path("/parsers/download/{parserId}")
    public Response downloadParserJar(@PathParam("parserId") Long parserId) {
        try {
            ParserInfo parserInfo = doGetParserInfoById(parserId);
            if (parserInfo != null) {
                StreamingOutput streamOutput = WSUtils.wrapWithStreamingOutput(parserInfoCatalogService.getParserJar(parserInfo));
                return Response.ok(streamOutput).build();
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, parserId.toString());
    }


    private ParserInfo doGetParserInfoById(Long parserId) {
        return parserInfoCatalogService.getParserInfo(parserId);
    }
}
