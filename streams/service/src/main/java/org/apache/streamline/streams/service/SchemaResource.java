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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hortonworks.registries.schemaregistry.SchemaVersionInfo;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    // This API would change once we consider other sources. Currently supports kafka sources for the given topic names.
    @GET
    @Path("/{topicName}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKafkaSourceSchema(@PathParam("topicName") String topicName)
        throws JsonProcessingException {
        try {
            LOG.info("Received path: [{}]", topicName);
            // for now, takes care of kafka for topic values. We will enhance to work this to get schema for different
            // sources based on given properties.
            String schemaName = topicName + ":v";
            SchemaVersionInfo schemaVersionInfo = schemaRegistryClient.getLatestSchemaVersionInfo(schemaName);
            String schema = schemaVersionInfo != null ? schemaVersionInfo.getSchemaText() : null;
            LOG.debug("######### Received schema from schema registry: ", schema);
            if (schema != null && !schema.isEmpty()) {
                schema = AvroStreamsSchemaConverter.convertAvro(schema);
            }
            LOG.debug("######### Converted schema: ", schema);
            return WSUtils.respondEntity(schema, OK);
        } catch (SchemaNotFoundException e) {
            // ignore and log error
            LOG.error("Schema not found for name: [{}]", topicName, e);
            throw EntityNotFoundException.byId(topicName);
        } catch (JsonProcessingException ex) {
            LOG.error("Error occurred while retrieving schema with name [{}]", topicName, ex);
            throw ex;
        }
    }

}
