/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hortonworks.registries.schemaregistry.SchemaBranch;
import com.hortonworks.registries.schemaregistry.SchemaIdVersion;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.SchemaVersion;
import com.hortonworks.registries.schemaregistry.SchemaVersionInfo;
import com.hortonworks.registries.schemaregistry.SchemaVersionKey;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.errors.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import com.hortonworks.registries.schemaregistry.state.InbuiltSchemaVersionLifecycleState;
import com.hortonworks.registries.schemaregistry.state.SchemaVersionLifecycleState;
import com.hortonworks.registries.schemaregistry.state.SchemaVersionLifecycleStates;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 *
 */
@Path("/v1/schemas")
public class SchemaResource {
    private static final Logger LOG = LoggerFactory.getLogger(SchemaResource.class);

    private final SchemaRegistryClient schemaRegistryClient;

    public SchemaResource(SchemaRegistryClient schemaRegistryClient) {
        this.schemaRegistryClient = schemaRegistryClient;
    }

    @POST
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response postStreamsSchema(StreamsSchemaInfo streamsSchemaInfo,
                                      @Context SecurityContext securityContext) throws IOException {
        Preconditions.checkNotNull(streamsSchemaInfo, "streamsSchemaInfo can not be null");

        SchemaIdVersion schemaIdVersion = null;
        SchemaMetadata schemaMetadata = streamsSchemaInfo.getSchemaMetadata();
        String schemaName = schemaMetadata.getName();
        Long schemaMetadataId = schemaRegistryClient.registerSchemaMetadata(schemaMetadata);
        LOG.info("Registered schemaMetadataId [{}] for schema with name:[{}]", schemaMetadataId, schemaName);

        String streamsSchemaText = streamsSchemaInfo.getSchemaVersion().getSchemaText();
        try {
            // convert streams schema to avro schema.
            String avroSchemaText = AvroStreamlineSchemaConverter.convertStreamlineSchemaToAvroSchema(streamsSchemaText);
            SchemaVersion avroSchemaVersion = new SchemaVersion(avroSchemaText, streamsSchemaInfo.getSchemaVersion().getDescription());
            schemaIdVersion = schemaRegistryClient.addSchemaVersion(schemaName, avroSchemaVersion);
        } catch (InvalidSchemaException e) {
            String errMsg = String.format("Invalid schema received for schema with name [%s] : [%s]", schemaName, streamsSchemaText);
            LOG.error(errMsg, e);
            throw BadRequestException.message(errMsg, e);
        } catch (SchemaNotFoundException e) {
            String errMsg = String.format("Schema not found for topic: [%s]", schemaName);
            LOG.error(errMsg, e);
            throw EntityNotFoundException.byId(schemaName);
        } catch (IncompatibleSchemaException e) {
            String errMsg = String.format("Incompatible schema received for schema with name [%s] : [%s]", schemaName, streamsSchemaText);
            LOG.error(errMsg, e);
            throw BadRequestException.message(errMsg, e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return WSUtils.respondEntity(schemaIdVersion, OK);
    }

    @GET
    @Path("/{schemaName}/versions")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSchemaVersions(@PathParam("schemaName") String schemaName,
                                         @Context SecurityContext securityContext) {
        return doGetAllSchemaVersionForBranch(schemaName, null);
    }

    private Response doGetAllSchemaVersionForBranch(String schemaName, String branchName) {
        try {
            LOG.info("Get all versions for schema : {}", schemaName);
            Collection<SchemaVersionInfo> schemaVersionInfos = schemaRegistryClient.getAllVersions(effectiveBranchName(branchName), schemaName);
            LOG.debug("Received schema versions [{}] from schema registry for schema: {}", schemaVersionInfos, schemaName);

            if (schemaVersionInfos != null && !schemaVersionInfos.isEmpty()) {
                List<String> schemaVersions = schemaVersionInfos.stream().map(x -> x.getVersion().toString()).collect(Collectors.toList());
                return WSUtils.respondEntities(schemaVersions, OK);
            } else {
                return WSUtils.respondEntity(Collections.EMPTY_LIST, NOT_FOUND);
            }
        } catch (SchemaNotFoundException e) {
            LOG.error("Schema not found: [{}]", schemaName, e);
            throw EntityNotFoundException.byName(schemaName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String effectiveBranchName(String branchName) {
        return branchName == null || branchName.isEmpty() ? SchemaBranch.MASTER_BRANCH : branchName;
    }

    @GET
    @Path("/{schemaName}/{branchName}/versions")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSchemaVersionsForBranch(@PathParam("schemaName") String schemaName,
                                                  @PathParam("branchName") String branchName,
                                                  @Context SecurityContext securityContext) {
        return doGetAllSchemaVersionForBranch(schemaName, branchName);
    }

    @GET
    @Path("/{schemaName}/versions/{version}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchemaForVersion(@PathParam("schemaName") String schemaName, @PathParam("version") String version,
                                        @Context SecurityContext securityContext) {
        try {
            LOG.info("Get schema:version [{}:{}]", schemaName, version);
            SchemaVersionInfo schemaVersionInfo = schemaRegistryClient.getSchemaVersionInfo(new SchemaVersionKey(schemaName, Integer.parseInt(version)));

            if (schemaVersionInfo != null) {
                LOG.debug("Received schema version from schema registry: [{}]", schemaVersionInfo);
                String schema = schemaVersionInfo.getSchemaText();
                if (schema != null && !schema.isEmpty()) {
                    schema = AvroStreamlineSchemaConverter.convertAvroSchemaToStreamlineSchema(schema);
                }
                LOG.debug("Converted schema: [{}]", schema);
                return WSUtils.respondEntity(schema, OK);
            } else {
                throw new SchemaNotFoundException("Version " + version + " of schema " + schemaName + " not found ");
            }
        } catch (SchemaNotFoundException e) {
            LOG.error("Schema not found: [{}]", schemaName, e);
            throw EntityNotFoundException.byName(schemaName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{schemaName}/branches")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchemaBranches(@PathParam("schemaName") String schemaName,
                                      @Context SecurityContext securityContext) {
        try {
            Collection<SchemaBranch> schemaBranches = schemaRegistryClient.getSchemaBranches(schemaName);
            LOG.info("Schema branches for schema [{}] : {}", schemaName, schemaBranches);
            List<String> schemaBranchNames = Collections.emptyList();
            if (schemaBranches != null && !schemaBranches.isEmpty()) {
                schemaBranchNames = schemaBranches.stream().map(SchemaBranch::getName).collect(Collectors.toList());
            }

            return WSUtils.respondEntities(schemaBranchNames, OK);
        } catch (SchemaNotFoundException e) {
            LOG.error("Schema not found with name: [{}]", schemaName, e);
            throw EntityNotFoundException.byName(schemaName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
