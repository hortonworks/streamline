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
package org.apache.streamline.registries.parser.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.Schema;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.registries.parser.ParserInfo;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ParserInfoCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ParserInfoCatalogResource.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ParsersCatalogService parserInfoCatalogService;

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
            return WSUtils.respond(parserInfos, OK, SUCCESS);
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
                    return WSUtils.respond(result, OK, SUCCESS);
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
                return WSUtils.respond(result, OK, SUCCESS);
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
                return WSUtils.respond(removedParser, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, parserId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }



    //Test curl command curl -X POST -i -F parserJar=@parsers-0.1.0.jar http://localhost:8080/api/v1/catalog/parsers/upload-verify
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers/upload-verify")
    public Response verifyParserUpload(@FormDataParam("parserJar") final InputStream inputStream) {
        try {
            return WSUtils.respond(parserInfoCatalogService.verifyParserUpload(inputStream), Response.Status.OK, SUCCESS);
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

    //Test curl command curl -X POST -i -F parserJar=@original-webservice-0.1.0.jar -F parserInfo='{"parserName":"TestParser","className":"some.test.parserClass","version":0}' http://localhost:8080/api/v1/catalog/parsers
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
            return WSUtils.respond(result, CREATED, SUCCESS);
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
