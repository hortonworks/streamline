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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.hortonworks.registries.schemaregistry.SchemaIdVersion;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.SchemaVersion;
import com.hortonworks.registries.schemaregistry.SchemaVersionInfo;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.errors.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import static javax.ws.rs.core.Response.Status.OK;

/**
 *
 */
@Path("/v1/schemas")
public class SchemaResource {
    private static final Logger LOG = LoggerFactory.getLogger(SchemaResource.class);

    private final StreamlineAuthorizer authorizer;
    private final SchemaRegistryClient schemaRegistryClient;
    private final Subject subject;

    public SchemaResource(StreamlineAuthorizer authorizer, SchemaRegistryClient schemaRegistryClient, Subject subject) {
        this.authorizer = authorizer;
        this.schemaRegistryClient = schemaRegistryClient;
        this.subject = subject;
    }

    @POST
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response postStreamsSchema(StreamsSchemaInfo streamsSchemaInfo,
                                      @Context SecurityContext securityContext) throws IOException {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_SCHEMA_ADMIN);
        Preconditions.checkNotNull(streamsSchemaInfo, "streamsSchemaInfo can not be null");

        SchemaIdVersion schemaIdVersion = null;
        SchemaMetadata schemaMetadata = streamsSchemaInfo.getSchemaMetadata();
        String schemaName = schemaMetadata.getName();
        Long schemaMetadataId = Subject.doAs(subject, new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return schemaRegistryClient.registerSchemaMetadata(schemaMetadata);
            }
        });
        LOG.info("Registered schemaMetadataId [{}] for schema with name:[{}]", schemaMetadataId, schemaName);

        String streamsSchemaText = streamsSchemaInfo.getSchemaVersion().getSchemaText();
        try {
            // convert streams schema to avro schema.
            String avroSchemaText = AvroStreamlineSchemaConverter.convertStreamlineSchemaToAvroSchema(streamsSchemaText);
            SchemaVersion avroSchemaVersion = new SchemaVersion(avroSchemaText, streamsSchemaInfo.getSchemaVersion().getDescription());
            schemaIdVersion = Subject.doAs(subject, new PrivilegedExceptionAction<SchemaIdVersion>() {
                @Override
                public SchemaIdVersion run() throws SchemaNotFoundException, InvalidSchemaException, IncompatibleSchemaException {
                    return schemaRegistryClient.addSchemaVersion(schemaName, avroSchemaVersion);
                }
            });

        } catch (PrivilegedActionException e) {
            Exception ex = e.getException();
            if (SchemaNotFoundException.class.isAssignableFrom(ex.getClass())) {
                LOG.error("Schema not found for topic: [{}]", schemaName, e);
                throw EntityNotFoundException.byId(schemaName);
            } else if (InvalidSchemaException.class.isAssignableFrom(ex.getClass())) {
                String errMsg = String.format("Invalid schema received for schema with name [%s] : [%s]", schemaName, streamsSchemaText);
                LOG.error(errMsg, e);
                throw BadRequestException.message(errMsg, e);
            } else if (IncompatibleSchemaException.class.isAssignableFrom(ex.getClass())) {
                String errMsg = String.format("Incompatible schema received for schema with name [%s] : [%s]", schemaName, streamsSchemaText);
                LOG.error(errMsg, e);
                throw BadRequestException.message(errMsg, e);
            } else {
                throw new RuntimeException(e);
            }
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
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_SCHEMA_USER);
        try {
            LOG.info("Received path: [{}]", topicName);
            // for now, takes care of kafka for topic values. We will enhance to work this to get schema for different
            // sources based on given properties.
            String schemaName = topicName + ":v";
            SchemaVersionInfo schemaVersionInfo = Subject.doAs(subject, new PrivilegedExceptionAction<SchemaVersionInfo>() {
                @Override
                public SchemaVersionInfo run() throws SchemaNotFoundException {
                    return schemaRegistryClient.getLatestSchemaVersionInfo(schemaName);
                }
            });
            String schema = schemaVersionInfo != null ? schemaVersionInfo.getSchemaText() : null;
            LOG.debug("######### Received schema from schema registry: ", schema);
            if (schema != null && !schema.isEmpty()) {
                schema = AvroStreamlineSchemaConverter.convertAvroSchemaToStreamlineSchema(schema);
            }
            LOG.debug("######### Converted schema: {}", schema);
            return WSUtils.respondEntity(schema, OK);
        } catch (PrivilegedActionException e) {
            Exception ex = e.getException();
            if (SchemaNotFoundException.class.isAssignableFrom(ex.getClass())) {
                // ignore and log error
                LOG.error("Schema not found for topic: [{}]", topicName, e);
                throw EntityNotFoundException.byId(topicName);
            } else {
                throw new RuntimeException(e);
            }
        } catch (JsonProcessingException ex) {
            LOG.error("Error occurred while retrieving schema with name [{}]", topicName, ex);
            throw ex;
        }
    }

}
