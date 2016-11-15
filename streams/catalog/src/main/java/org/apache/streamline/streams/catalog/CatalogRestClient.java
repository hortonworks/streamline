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
package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.registries.parser.ParserInfo;
import org.apache.streamline.registries.parser.client.ParserClient;
import org.apache.streamline.storage.Storable;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: All the configs should be read from some config file.
 */
public class CatalogRestClient {

    private final Client client;

    private static final String NOTIFIER_URL = "notifiers";
    private static final String CUSTOM_PROCESSOR_JAR_DOWNLOAD_URL = "streams/componentbundles/PROCESSOR/custom";
    private static final String FILE_DOWNLOAD_URL = "files/download/";

    private final String rootCatalogURL;
    private final ParserClient parserClient;

    //TODO: timeouts should come from a config so probably make them constructor args.
    public CatalogRestClient(String rootCatalogURL) {
        this(rootCatalogURL, new ClientConfig());
    }

    public CatalogRestClient(String rootCatalogURL, ClientConfig clientConfig) {
        this.rootCatalogURL = rootCatalogURL;
        client = ClientBuilder.newClient(clientConfig);
        client.register(MultiPartFeature.class);
        parserClient = new ParserClient(rootCatalogURL);
    }

    private <T> List<T> getEntities(WebTarget target, Class<T> clazz) {
        List<T> entities = new ArrayList<>();
        String response = target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.get("entities").elements();
            while (it.hasNext()) {
                entities.add(mapper.treeToValue(it.next(), clazz));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return entities;
    }

    private <T extends Storable> T getEntity(WebTarget target, Class<T> clazz) {
        String response = target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("entity"), clazz);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ParserInfo getParserInfo(Long parserId) {
        return parserClient.getParserInfo(parserId);
    }

    public NotifierInfo getNotifierInfo(String notifierName) {
        return getEntities(client.target(String.format("%s/%s/?name=%s", rootCatalogURL, NOTIFIER_URL, notifierName)),
                            NotifierInfo.class).get(0);
    }

    protected InputStream getInputStream(String fileId, String relativeUrl) {
        return client.target(String.format("%s/%s/%s", rootCatalogURL, relativeUrl, fileId))
                .request(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.MULTIPART_FORM_DATA_TYPE)
                .get(InputStream.class);
    }

    public InputStream getCustomProcessorJar (String jarFileName) {
        return getInputStream(jarFileName, CUSTOM_PROCESSOR_JAR_DOWNLOAD_URL);
    }

    public InputStream getParserJar(Long parserId) {
        return parserClient.getParserJar(parserId);
    }

    public InputStream getFile(Long jarId) {
        return getInputStream(jarId.toString(), FILE_DOWNLOAD_URL);
    }

}
