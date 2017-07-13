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

import com.google.common.base.Preconditions;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hortonworks.registries.schemaregistry.SchemaIdVersion;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.SchemaVersion;
import com.hortonworks.registries.schemaregistry.SchemaVersionInfo;
import com.hortonworks.registries.schemaregistry.SchemaVersionKey;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.errors.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
            String errMsg = String.format("Schema not found for topic: [{}]", schemaName, e);
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

    // This API would change once we consider other sources. Currently supports kafka sources for the given topic names.
    @GET
    @Path("/{topicName}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKafkaSourceSchema(@PathParam("topicName") String topicName,
                                         @Context SecurityContext securityContext) throws JsonProcessingException {
        try {
            LOG.info("Received path: [{}]", topicName);
            // for now, takes care of kafka for topic values. We will enhance to work this to get schema for different
            // sources based on given properties.
            final String schemaName = topicName;
            SchemaVersionInfo schemaVersionInfo = schemaRegistryClient.getLatestSchemaVersionInfo(schemaName);
            String schema = schemaVersionInfo != null ? schemaVersionInfo.getSchemaText() : null;
            LOG.debug("######### Received schema from schema registry: ", schema);
            if (schema != null && !schema.isEmpty()) {
                schema = AvroStreamlineSchemaConverter.convertAvroSchemaToStreamlineSchema(schema);
            }
            LOG.debug("######### Converted schema: {}", schema);
            return WSUtils.respondEntity(schema, OK);
        } catch (SchemaNotFoundException e) {
            LOG.error("Schema not found for topic: [{}]", topicName, e);
            throw EntityNotFoundException.byId(topicName);
        } catch (JsonProcessingException ex) {
            LOG.error("Error occurred while retrieving schema with name [{}]", topicName, ex);
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{schemaName}/versions")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSchemaVersions(@PathParam("schemaName") String schemaName,
                                         @Context SecurityContext securityContext) {
        try {
            LOG.info("Get all versions for schema : {}", schemaName);
            Collection<SchemaVersionInfo> schemaVersionInfos = schemaRegistryClient.getAllVersions(schemaName);
            Collection<String> schemaVersions = new ArrayList<>();
            if (schemaVersionInfos != null && !schemaVersionInfos.isEmpty()) {
                LOG.debug("######### Received schema versions from schema registry: {}", schemaVersionInfos);
                for (SchemaVersionInfo schemaVersionInfo: schemaVersionInfos) {
                    schemaVersions.add(schemaVersionInfo.getVersion().toString());
                }
                return WSUtils.respondEntities(schemaVersions, OK);
            } else {
                LOG.debug("######### Received null response for schema versions from schema registry for schema: {}", schemaName);
                return WSUtils.respondEntity(schemaVersions, NOT_FOUND);
            }
        } catch (SchemaNotFoundException e) {
            LOG.error("Schema not found: [{}]", schemaName, e);
            throw EntityNotFoundException.byName(schemaName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{schemaName}/versions/{version}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchemaForVersion(@PathParam("schemaName") String schemaName, @PathParam("version") String version,
                                        @Context SecurityContext securityContext) {
        try {
            LOG.info("Get schema:version {}", schemaName + ":" + version);
            SchemaVersionInfo schemaVersionInfo = schemaRegistryClient.getSchemaVersionInfo(new SchemaVersionKey(schemaName, Integer.parseInt
                    (version)));
            if (schemaVersionInfo != null) {
                LOG.debug("######### Received schema version from schema registry: {}", schemaVersionInfo);
                String schema = schemaVersionInfo.getSchemaText();
                if (schema != null && !schema.isEmpty()) {
                    schema = AvroStreamlineSchemaConverter.convertAvroSchemaToStreamlineSchema(schema);
                }
                LOG.debug("######### Converted schema: {}", schema);
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

}
