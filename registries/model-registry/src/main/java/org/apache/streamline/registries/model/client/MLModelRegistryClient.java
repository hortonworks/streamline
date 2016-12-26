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
package org.apache.streamline.registries.model.client;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;

/**
 * Client class for accessing the MLModelRegistry Service APIs.
 */
public final class MLModelRegistryClient {
    private static final Logger LOG = LoggerFactory.getLogger(MLModelRegistryClient.class);
    private final String modelRegistryURL;
    private final Client client;

    public MLModelRegistryClient(String catalogURL) {
        this(catalogURL, ClientBuilder.newClient(new ClientConfig()));
    }

    public MLModelRegistryClient(String catalogURL, Client client) {
        this.modelRegistryURL = String.join("/", catalogURL, "ml", "models");
        this.client = client;
        client.register(MultiPartFeature.class);
    }

    public String getMLModelContents(String modelName) {
        try {
            Response response = client.target(String.format("%s/%s", modelRegistryURL, modelName)).request().get();
            if(response.getStatus() != OK.getStatusCode()) {
                throw new RuntimeException(
                        String.format("Error occurred while getting the response %s", response.getStatus()));
            } else {
                return response.readEntity(String.class);
            }
        } catch (Exception exception) {
            LOG.error(String.format("An error was thrown while reading the pmml file contents for %s", modelName),
                      exception);
            throw new RuntimeException(exception);
        }
    }
}
