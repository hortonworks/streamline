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
package com.hortonworks.iotas.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.storage.Storable;
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

    private Client client;

    private static final String DEVICE_URL = "devices";
    private static final String DATASOURCE_URL = "deprecated/datasources";
    private static final String FEED_URL = "feeds";
    private static final String PARSER_URL = "parsers";
    private static final String NOTIFIER_URL = "notifiers";
    private static final String PARSER_DOWNLOAD_URL = PARSER_URL + "/download";
    private static final String CUSTOM_PROCESSOR_JAR_DOWNLOAD_URL = "system/componentdefinitions/PROCESSOR/custom";
    private static final String JAR_DOWNLOAD_URL = "jars/download/";

    private String rootCatalogURL;
    private WebTarget rootTarget;
    private WebTarget dataSourceTarget;
    private WebTarget feedTarget;
    private WebTarget parserTarget;

    //TODO: timeouts should come from a config so probably make them constructor args.
    public CatalogRestClient(String rootCatalogURL) {
        this(rootCatalogURL, new ClientConfig());
    }

    public CatalogRestClient(String rootCatalogURL, ClientConfig clientConfig) {
        this.rootCatalogURL = rootCatalogURL;
        client = ClientBuilder.newClient(clientConfig);
        client.register(MultiPartFeature.class);
        rootTarget = client.target(rootCatalogURL);
        dataSourceTarget = rootTarget.path(DATASOURCE_URL);
        feedTarget = rootTarget.path(FEED_URL);
        parserTarget = rootTarget.path(PARSER_URL);
    }

    private <T extends Storable> List<T> getEntities(WebTarget target, Class<T> clazz) {
        List<T> entities = new ArrayList<T>();
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
        return getEntity(parserTarget.path(parserId.toString()), ParserInfo.class);
    }

    public NotifierInfo getNotifierInfo(String notifierName) {
        return getEntities(client.target(String.format("%s/%s/?name=%s", rootCatalogURL, NOTIFIER_URL, notifierName)),
                            NotifierInfo.class).get(0);
    }

    public DataSource getDataSource(String deviceId, String version) {
        return getEntities(client.target(String.format("%s/%s/type/DEVICE/?make=%s&model=%s",
                                            rootCatalogURL, DATASOURCE_URL, deviceId, version)),
                            DataSource.class).get(0);
    }

    public ParserInfo getParserInfo(String deviceId, String version) {
        String url = String.format("%s/%s/type/DEVICE?make=%s&model=%s", rootCatalogURL, DATASOURCE_URL, deviceId, version);
        DataSource dataSource = getEntities(client.target(url), DataSource.class).get(0);
        DataFeed dataFeed = getEntities(client.target(String.format("%s/%s?dataSourceId=%s",
                                        rootCatalogURL, FEED_URL, dataSource.getId())), DataFeed.class).get(0);

        return getParserInfo(dataFeed.getParserId());
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
        return getInputStream(parserId.toString(), PARSER_DOWNLOAD_URL);
    }

    public InputStream getJar(Long jarId) {
        return getInputStream(jarId.toString(), JAR_DOWNLOAD_URL);
    }

}
