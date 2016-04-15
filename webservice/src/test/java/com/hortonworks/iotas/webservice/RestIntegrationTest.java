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
package com.hortonworks.iotas.webservice;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.catalog.Cluster;
import com.hortonworks.iotas.catalog.Component;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.File;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.catalog.Tag;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.catalog.TopologyEditorMetadata;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.processor.CustomProcessorInfo;
import com.hortonworks.iotas.processor.examples.ConsoleCustomProcessor;
import com.hortonworks.iotas.test.IntegrationTest;
import com.hortonworks.iotas.topology.ConfigField;
import com.hortonworks.iotas.topology.TopologyComponent;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.webservice.catalog.TopologyCatalogResource;
import com.hortonworks.iotas.webservice.catalog.dto.DataSourceDto;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Tests the entire code path for our rest APIs. Currently tests Post, Put, Get(list, ById) and Delete.
 */
@Category(IntegrationTest.class)
public class RestIntegrationTest {

    /**
     * See https://dropwizard.github.io/dropwizard/manual/testing.html#integration-testing
     */
    @ClassRule
    public static final DropwizardAppRule<IotasConfiguration> RULE = new DropwizardAppRule<>(IotasApplication.class, ResourceHelpers.resourceFilePath("iotas-test.yaml"));

    private String rootUrl = String.format("http://localhost:%d/api/v1/catalog/", RULE.getLocalPort());
    private final InputStream IMAGE_FILE_STREAM = new ByteArrayInputStream("some gif gibberish".getBytes());
    private final InputStream JAR_FILE_STREAM = new ByteArrayInputStream("some jar gibberish".getBytes());
    /**
     * A Test element holder class
     */
    private class ResourceTestElement {
        final Object resourceToPost; // resource that will be used to test post
        final Object resourceToPut; // resource that will be used to test put
        final String id; //Id by which Get(id) and Delete(id) will be tested, should match the actual Id set in post/put request.
        final String url; //Rest Url to test.

        public ResourceTestElement(Object resourceToPost, Object resourceToPut, String id, String url) {
            this.resourceToPost = resourceToPost;
            this.resourceToPut = resourceToPut;
            this.id = id;
            this.url = url;
        }
    }

    /**
     * A Test element holder class for testing resources that support a get
     * with query params
     */
    private class QueryParamsResourceTestElement {
        final List<Object> resourcesToPost; // resources that will be posted to postUrl
        final String postUrl; // Rest Url to post.
        // get urls with different query parameters. To be called before
        // and after post
        final List<String> getUrls;
        // expected results for each get url above after the post. should be
        // same length as getUrls
        final List<List<Object>> getResults;

        public QueryParamsResourceTestElement (List<Object> resourcesToPost, String postUrl, List<String> getUrls, List<List<Object>> getResults) {
            this.resourcesToPost = resourcesToPost;
            this.postUrl = postUrl;
            this.getUrls = getUrls;
            this.getResults = getResults;
        }
    }

    /**
     * List of all things that will be tested
     */
    private Collection<ResourceTestElement> resourcesToTest = Lists.newArrayList(
            // TODO: The below test case needs to be fixed since it should first create the data source and then add the corresponding datafeed
            //new ResourceTestElement(createDataFeed(1l, "testDataFeed"), createDataFeed(1l, "testDataFeedPut"), "1", rootUrl + "feeds"),
            new ResourceTestElement(createClusterInfo(1l, "testCluster"), createClusterInfo(1l, "testClusterPut"), "1", rootUrl + "clusters"),
            new ResourceTestElement(createNotifierInfo(1l, "testNotifier"), createNotifierInfo(1l, "testNotifierPut"), "1", rootUrl + "notifiers"),
            new ResourceTestElement(createDataSourceDto(1l, "testDataSourceWithDataFeed:" + System.currentTimeMillis()), createDataSourceDto(1l, "testDataSourceWithDataFeedPut:" + System.currentTimeMillis()), "1", rootUrl + "datasources"),
            new ResourceTestElement(createTopology(1l, "iotasTopology"), createTopology(1l, "iotasTopologyPut"), "1", rootUrl + "topologies"),
            new ResourceTestElement(createTopologyEditorMetadata(1l, "{\"x\":5,\"y\":6}"), createTopologyEditorMetadata(1l, "{\"x\":6,\"y\":5}"), "1", rootUrl + "system/topologyeditormetadata"),
            new ResourceTestElement(createTopologyComponent(1l, "kafkaSpoutComponent", TopologyComponent.TopologyComponentType.SOURCE, "KAFKA"), createTopologyComponent(1l, "kafkaSpoutComponentPut", TopologyComponent.TopologyComponentType.SOURCE, "KAFKA") , "1", rootUrl + "system/componentdefinitions/SOURCE"),
            new ResourceTestElement(createTopologyComponent(2l, "parserProcessor", TopologyComponent .TopologyComponentType.PROCESSOR, "PARSER"), createTopologyComponent(2l, "parserProcessorPut", TopologyComponent.TopologyComponentType.PROCESSOR, "PARSER"), "2", rootUrl + "system/componentdefinitions/PROCESSOR"),
            new ResourceTestElement(createTopologyComponent(3l, "hbaseSink", TopologyComponent.TopologyComponentType.SINK, "HBASE"), createTopologyComponent(3l, "hbaseSinkPut", TopologyComponent.TopologyComponentType.SINK, "HBASE"), "3", rootUrl + "system/componentdefinitions/SINK"),
            new ResourceTestElement(createTopologyComponent(4l, "shuffleGroupingLink", TopologyComponent.TopologyComponentType.LINK, "SHUFFLE"), createTopologyComponent(4l, "shuffleGroupingLinkPut", TopologyComponent.TopologyComponentType.LINK, "SHUFFLE"), "4", rootUrl + "system/componentdefinitions/LINK")
            // parser is commented as parser takes a jar as input along with the parserInfo instance and so it needs a multipart request.
            //new ResourceTestElement(createParserInfo(1l, "testParser"), createParserInfo(1l, "testParserPut"), "1", rootUrl + "parsers")
    );

    /**
     * For each TestResource element in resourcesToTest List, tests Post, Put, Get and Delete.
     *
     * @throws Exception
     */
    @Test
    public void testAllResources() throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig());

        for (ResourceTestElement resourceToTest : resourcesToTest) {
            String url = resourceToTest.url;
            Object resourceToPost = resourceToTest.resourceToPost;
            Object resourceToPut = resourceToTest.resourceToPut;
            String id = resourceToTest.id;

            String response = client.target(url).request().post(Entity.json(resourceToPost), String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

            response = client.target(url).request().get(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            Assert.assertEquals(Lists.newArrayList(resourceToPost), getEntities(response, resourceToPost.getClass()));

            url = url + "/" + id;
            response = client.target(url).request().get(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            Assert.assertEquals(resourceToPost, getEntity(response, resourceToPost.getClass()));

            response = client.target(url).request().put(Entity.json(resourceToPut), String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

            response = client.target(url).request().get(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            Assert.assertEquals(resourceToPut, getEntity(response, resourceToPut.getClass()));

            response = client.target(url).request().delete(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

            try {
                client.target(url).request().get(String.class);
                Assert.fail("Should have thrown NotFoundException.");
            } catch (NotFoundException e) {
                response = e.getResponse().readEntity(String.class);
                Assert.assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode(), getResponseCode(response));
            }
        }
    }

    /**
     * For each TestResource element in resourcesToTest List, tests Post, Put, Get and Delete.
     *
     * @throws Exception
     */
    @Test
    public void testComponentAPIs() throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig());

        Long clusterId = 1L;
        List<Component> componentsToPost = new ArrayList<>();
        for(long i=1; i<4; i++) {
            componentsToPost.add(createComponent(clusterId, i, "testComponent:"+i));
        }
        String url = rootUrl + "clusters/1/components";
        Component resourceToPut = createComponent(clusterId, 1l, "testComponentPut");
        Class resourceToPostClass = Component.class;

        String response = client.target(url).request().post(Entity.json(componentsToPost), String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

        response = client.target(url).request().get(String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(new HashSet<>(componentsToPost), new HashSet<>(getEntities(response, resourceToPostClass)));

        for (int i=0; i<componentsToPost.size(); i++) {
            String curUrl = url + "/" + (i+1);
            response = client.target(curUrl).request().get(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            Assert.assertEquals(componentsToPost.get(i), getEntity(response, resourceToPostClass));
        }
        
        url = url + "/" + 1;
        response = client.target(url).request().put(Entity.json(resourceToPut), String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

        response = client.target(url).request().get(String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(resourceToPut, getEntity(response, resourceToPut.getClass()));

        response = client.target(url).request().delete(String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

        try {
            client.target(url).request().get(String.class);
            Assert.fail("Should have thrown NotFoundException.");
        } catch (NotFoundException e) {
            response = e.getResponse().readEntity(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode(), getResponseCode(response));
        }

    }

    /**
     * IOT-102: Test whether component API can distinguish cluster
     *
     * @throws Exception
     */
    @Test
    public void testComponentAPIsCanDistinguishCluster() throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig());

        Long clusterId = 1L;
        String componentBaseUrl = rootUrl + String.format("clusters/%d/components", clusterId);

        Component component = createComponent(clusterId, 1L, "testComponent:"+1);

        String componentEntityUrl = componentBaseUrl + "/" + 1;
        String response = client.target(componentEntityUrl).request().put(Entity.json(component), String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

        Long anotherClusterId = 2L;

        componentBaseUrl = rootUrl + String.format("clusters/%d/components", anotherClusterId);

        try {
            client.target(componentBaseUrl).request().get(String.class);
            Assert.fail("Should have thrown NotFoundException.");
        } catch (NotFoundException e) {
            response = e.getResponse().readEntity(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER.getCode(), getResponseCode(response));
        }

        componentEntityUrl = componentBaseUrl + "/" + 1;
        try {
            client.target(componentEntityUrl).request().get(String.class);
            Assert.fail("Should have thrown NotFoundException.");
        } catch (NotFoundException e) {
            response = e.getResponse().readEntity(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode(), getResponseCode(response));
        }
    }

    /*
    Test the get request for topology components. Currently we only support
    four types of topology components. SOURCE, PROCESSOR, LINK AND SINK
     */
    @Test
    public void testTopologyComponentTypes () throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig());

        String url = rootUrl + "system/componentdefinitions";

        String response = client.target(url).request().get(String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Object expected = Arrays.asList(TopologyComponent.TopologyComponentType.values());
        Object actual = getEntities(response, TopologyComponent.TopologyComponentType.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTopologyComponentsForTypeWithFilters () throws Exception {
        String prefixUrl = rootUrl + "system/componentdefinitions/";
        String[] postUrls = {
                prefixUrl + TopologyComponent.TopologyComponentType.SOURCE,
                prefixUrl + TopologyComponent.TopologyComponentType.PROCESSOR,
                prefixUrl + TopologyComponent.TopologyComponentType.SINK,
                prefixUrl + TopologyComponent.TopologyComponentType.LINK
        };
        List<List<Object>> resourcesToPost = new ArrayList<List<Object>>();
        Object source = createTopologyComponent(1l, "kafkaSpoutComponent", TopologyComponent.TopologyComponentType.SOURCE, "KAFKA");
        Object parser = createTopologyComponent(2l, "parserProcessor", TopologyComponent.TopologyComponentType.PROCESSOR, "PARSER");
        Object sink = createTopologyComponent(3l, "hbaseSink", TopologyComponent.TopologyComponentType.SINK, "HBASE");
        Object link = createTopologyComponent(4l, "shuffleGroupingLink", TopologyComponent.TopologyComponentType.LINK, "SHUFFLE");
        List<Object> sourcesPosted = Arrays.asList(source);
        List<Object> processorsPosted = Arrays.asList(parser);
        List<Object> sinksPosted = Arrays.asList(sink);
        List<Object> linksPosted = Arrays.asList(link);
        resourcesToPost.add(sourcesPosted);
        resourcesToPost.add(processorsPosted);
        resourcesToPost.add(sinksPosted);
        resourcesToPost.add(linksPosted);
        String prefixQueryParam = "?streamingEngine=STORM";
        List<List<String>> getUrlQueryParms = new ArrayList<List<String>>();
        getUrlQueryParms.add(Arrays.asList(prefixQueryParam + "&subType=KAFKA"));
        getUrlQueryParms.add(Arrays.asList(prefixQueryParam + "&subType=PARSER"));
        getUrlQueryParms.add(Arrays.asList(prefixQueryParam + "&subType=HBASE"));
        getUrlQueryParms.add(Arrays.asList(prefixQueryParam + "&subType=SHUFFLE"));
        List<List<List<Object>>> getResults = new ArrayList<List<List<Object>>>();
        getResults.add(Arrays.asList(sourcesPosted));
        getResults.add(Arrays.asList(processorsPosted));
        getResults.add(Arrays.asList(sinksPosted));
        getResults.add(Arrays.asList(linksPosted));
        List<QueryParamsResourceTestElement> testElements = new ArrayList<QueryParamsResourceTestElement>();
        for (int i = 0; i < postUrls.length; ++i) {
            List<String> getUrls = new ArrayList<String>();
            for (String queryParam: getUrlQueryParms.get(i)) {
                getUrls.add(postUrls[i] + queryParam);
            }
            testElements.add(new QueryParamsResourceTestElement
                    (resourcesToPost.get(i), postUrls[i], getUrls, getResults
                            .get(i)));
        }
        this.testResourcesWithQueryParams(testElements);
    }

    @Test
    @Ignore
    public void testCustomProcessorInfos () throws Exception {
        //Some issue with sending multi part for requests using this client and hence this test case is ignored for now. Fix later.
        String response;
        String prefixUrl = rootUrl + "system/componentdefinitions/PROCESSOR/custom";
        CustomProcessorInfo customProcessorInfo = createCustomProcessorInfo();
        String prefixQueryParam = "?streamingEngine=STORM";
        List<String> getUrlQueryParms = new ArrayList<String>();
        getUrlQueryParms.add(prefixQueryParam + "&name=ConsoleCustomProcessor");
        getUrlQueryParms.add(prefixQueryParam + "&imageFileName=image.gif");
        getUrlQueryParms.add(prefixQueryParam + "&jarFileName=iotas-core.jar");
        getUrlQueryParms.add(prefixQueryParam + "&customProcessorImpl=" + ConsoleCustomProcessor.class.getCanonicalName());
        List<List<CustomProcessorInfo>> getResults = new ArrayList<List<CustomProcessorInfo>>();
        getResults.add(Arrays.asList(customProcessorInfo));
        getResults.add(Arrays.asList(customProcessorInfo));
        getResults.add(Arrays.asList(customProcessorInfo));
        getResults.add(Arrays.asList(customProcessorInfo));
        List<String> getUrls = new ArrayList<String>();
        for (String queryParam: getUrlQueryParms) {
            getUrls.add(prefixUrl + queryParam);
        }
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartWriter.class);
        Client client = ClientBuilder.newClient(clientConfig);
        for (String getUrl: getUrls) {
            try {
                client.target(getUrl).request().get(String.class);
                Assert.fail("Should have thrown NotFoundException.");
            } catch (NotFoundException e) {
                response = e.getResponse().readEntity(String.class);
                Assert.assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER.getCode(), getResponseCode(response));
            }
        }
        /*FileDataBodyPart imageFileBodyPart = new FileDataBodyPart(TopologyCatalogResource.IMAGE_FILE_PARAM_NAME, getCpImageFile(), MediaType
                .APPLICATION_OCTET_STREAM_TYPE);
        FileDataBodyPart jarFileBodyPart = new FileDataBodyPart(TopologyCatalogResource.JAR_FILE_PARAM_NAME, getCpJarFile(), MediaType
                .APPLICATION_OCTET_STREAM_TYPE);*/
        MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(new StreamDataBodyPart(TopologyCatalogResource.IMAGE_FILE_PARAM_NAME, IMAGE_FILE_STREAM));
        multiPart.bodyPart(new StreamDataBodyPart(TopologyCatalogResource.JAR_FILE_PARAM_NAME, JAR_FILE_STREAM));
        multiPart.bodyPart(new FormDataBodyPart(TopologyCatalogResource.CP_INFO_PARAM_NAME, customProcessorInfo, MediaType.APPLICATION_JSON_TYPE));
        client.target(prefixUrl).request(MediaType.MULTIPART_FORM_DATA_TYPE).post(Entity.entity(multiPart, multiPart.getMediaType()));
        for (int i = 0; i < getUrls.size(); ++i) {
            String getUrl = getUrls.get(i);
            List<CustomProcessorInfo> expectedResults = getResults.get(i);
            response = client.target(getUrl).request().get(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            Assert.assertEquals(expectedResults, getEntities(response, expectedResults.get(i).getClass()));
        }
    }

    @Test
    public void testFileResources() throws Exception {
        Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
        String response = null;
        String url = rootUrl + "files";

        // POST
        File file = new File();
        file.setName("milkyway-jar");
        file.setVersion(System.currentTimeMillis());

        MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);

        String initialJarContent = "milkyway-jar-contents";

        final InputStream fileStream = new ByteArrayInputStream(initialJarContent.getBytes());
        multiPart.bodyPart(new StreamDataBodyPart("file", fileStream, "file"));
        multiPart.bodyPart(new FormDataBodyPart("fileInfo", file, MediaType.APPLICATION_JSON_TYPE));

        response = client.target(url)
                .request(MediaType.MULTIPART_FORM_DATA_TYPE, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .post(Entity.entity(multiPart, multiPart.getMediaType()), String.class);
        File postedFile = getEntity(response, File.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));


        //DOWNLOAD
        InputStream downloadInputStream = client.target(url+"/download/"+ postedFile.getId()).request().get(InputStream.class);
        ByteArrayOutputStream downloadedJarOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(downloadInputStream, downloadedJarOutputStream);

        ByteArrayOutputStream uploadedOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(new ByteArrayInputStream(initialJarContent.getBytes()), uploadedOutputStream);

        Assert.assertArrayEquals(uploadedOutputStream.toByteArray(), downloadedJarOutputStream.toByteArray());


        // GET all
        response = client.target(url).request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        List<File> files = getEntities(response, File.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(files.size(), 1);
        Assert.assertEquals(files.iterator().next().getName(), file.getName());


        // GET /files/1
        response = client.target(url+"/"+ postedFile.getId()).request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        File receivedFile = getEntity(response, File.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(receivedFile.getName(), postedFile.getName());
        Assert.assertEquals(receivedFile.getId(), postedFile.getId());


        // PUT
        postedFile.setName("andromeda-jar");
        postedFile.setVersion(System.currentTimeMillis());

        multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        InputStream updatedFileStream = new ByteArrayInputStream("andromeda-jar-contents".getBytes());
        multiPart.bodyPart(new StreamDataBodyPart("file", updatedFileStream, "file"));
        multiPart.bodyPart(new FormDataBodyPart("fileInfo", postedFile, MediaType.APPLICATION_JSON_TYPE));
        response = client.target(url)
                .request(MediaType.MULTIPART_FORM_DATA_TYPE, MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(multiPart, multiPart.getMediaType()), String.class);
        File updatedFile = getEntity(response, File.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(updatedFile.getId(), postedFile.getId());
        Assert.assertEquals(updatedFile.getName(), postedFile.getName());


        // DELETE
        response = client.target(url+"/"+ updatedFile.getId()).request().delete(String.class);
        final File deletedFile = getEntity(response, File.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(deletedFile.getId(), updatedFile.getId());


        // GET
        response = client.target(url).request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        files = getEntities(response, File.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertTrue(files.isEmpty());

    }

    /**
     * For each QueryParamsResourceTestElement it first try to send all get
     * requests and verifies the response. It then loads all resources via post
     * and executes get requests to match them with expected results
     * @param queryParamsResources
     * @throws Exception
     */
    public void testResourcesWithQueryParams (List<QueryParamsResourceTestElement> queryParamsResources) throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig());
        String response;
        for (QueryParamsResourceTestElement qpte: queryParamsResources) {
            // all gets first should return no entities
            for (String getUrl: qpte.getUrls) {
                try {
                    client.target(getUrl).request().get(String.class);
                    Assert.fail("Should have thrown NotFoundException.");
                } catch (NotFoundException e) {
                    response = e.getResponse().readEntity(String.class);
                    Assert.assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER.getCode(), getResponseCode(response));
                }
            }
            // post the resources now
            for (Object resource: qpte.resourcesToPost) {
                response = client.target(qpte.postUrl).request().post(Entity.json (resource), String.class);
                Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            }

            // send get requests and match the response with expected results
            for (int i = 0; i < qpte.getUrls.size(); ++i) {
                String getUrl = qpte.getUrls.get(i);
                List<Object> expectedResults = qpte.getResults.get(i);
                response = client.target(getUrl).request().get(String.class);
                Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
                Assert.assertEquals(expectedResults, getEntities(response, expectedResults.get(i).getClass()));
            }
        }
    }

    /**
     * Get response code from the response string.
     *
     * @param response
     * @return
     * @throws Exception
     */
    public int getResponseCode(String response) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);
        return mapper.treeToValue(node.get("responseCode"), Integer.class);
    }

    /**
     * Get the entities from response string
     *
     * @param response
     * @param clazz
     * @param <T>
     * @return
     */
    private <T extends Object> List<T> getEntities(String response, Class<T> clazz) {
        List<T> entities = new ArrayList<>();
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

    /**
     * Get entity from the response string.
     *
     * @param response
     * @param clazz
     * @param <T>
     * @return
     */
    private <T extends Object> T getEntity(String response, Class<T> clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("entity"), clazz);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    //============== Helper methods to create the actual objects that the rest APIS expect as Input ==========//

    private DataSource createDataSource(Long id, String name) {
        DataSource ds = new DataSource();
        ds.setId(id);
        ds.setName(name);
        ds.setDescription("desc");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("test-tag");
        tag.setDescription("test");
        ds.setTags(Arrays.asList(tag));
        ds.setTimestamp(System.currentTimeMillis());
        ds.setType(DataSource.Type.DEVICE);
        ds.setTypeConfig("{\"make\":\"1\",\"model\":\"1\"}");
        return ds;
    }

    private DataFeed createDataFeed(Long id, String name) {
        DataFeed df = new DataFeed();
        df.setId(id);
        df.setDataSourceId(1L);
        df.setName(name);
        df.setType("kafka://host:port/topic");
        df.setParserId(id);
        return df;
    }

    private DataSourceDto createDataSourceDto(Long dataSourceId, String dataSourceName) {

        DataSource ds = createDataSource(dataSourceId, dataSourceName);
        DataFeed df = createDataFeedWithDataSourceId(dataSourceId, "feed:" + dataSourceName);
        DataSourceDto dataSourceDto = new DataSourceDto(ds, df);

        return dataSourceDto;
    }

    private DataFeed createDataFeedWithDataSourceId(long datasourceId, String feedName) {
        DataFeed df = new DataFeed();
        df.setId(System.currentTimeMillis());
        df.setDataSourceId(datasourceId);
        df.setName(feedName);
        df.setName(feedName);
        df.setType("KAFKA");
        df.setParserId(datasourceId);
        return df;
    }

    private ParserInfo createParserInfo(Long id, String name) {
        ParserInfo pi = new ParserInfo();
        pi.setId(id);
        pi.setName(name);
        pi.setClassName("com.org.apache.TestParser");
        pi.setJarStoragePath("/tmp/parser.jar");
        pi.setParserSchema(new Schema.SchemaBuilder().fields(new Schema.Field("deviceId", Schema.Type.LONG),
                new Schema.Field("deviceName", Schema.Type.STRING)).build());
        pi.setVersion(0l);
        pi.setTimestamp(System.currentTimeMillis());
        return pi;
    }

    private Cluster createClusterInfo(Long id, String name) {
        Cluster cluster = new Cluster();
        cluster.setDescription("test");
        cluster.setId(id);
        cluster.setName(name);
        cluster.setTags("tags");
        cluster.setTimestamp(System.currentTimeMillis());
        cluster.setType(Cluster.Type.HDFS);
        return cluster;
    }

    private Component createComponent(Long clusterId, Long id, String name) {
        Component component = new Component();
        component.setClusterId(clusterId);
        component.setDescription("desc");
        component.setHosts("host-1, host-2");
        component.setId(id);
        component.setName(name);
        component.setPort(8080);
        component.setTimestamp(System.currentTimeMillis());
        component.setType(Component.ComponentType.BROKER);
        return component;
    }

    private NotifierInfo createNotifierInfo(Long id, String name) {
        NotifierInfo notifierInfo = new NotifierInfo();
        notifierInfo.setClassName("A.B.C");
        notifierInfo.setId(id);
        notifierInfo.setJarFileName(name);
        notifierInfo.setName(name);
        return notifierInfo;
    }

    private TopologyComponent createTopologyComponent (Long id, String name,
                                                       TopologyComponent
                                                               .TopologyComponentType topologyComponentType, String subType) {
        TopologyComponent topologyComponent = new TopologyComponent();
        topologyComponent.setId(id);
        topologyComponent.setName(name);
        topologyComponent.setType(topologyComponentType);
        topologyComponent.setStreamingEngine("STORM");
        topologyComponent.setConfig("{}");
        topologyComponent.setSubType(subType);
        topologyComponent.setTimestamp(System.currentTimeMillis());
        topologyComponent.setTransformationClass("com.hortonworks.iotas.topology.storm.KafkaSpoutFluxComponent");
        return topologyComponent;

    }

    private Topology createTopology (Long id, String name) {
        Topology topology = new Topology();
        topology.setId(id);
        topology.setName(name);
        topology.setConfig("{}");
        topology.setTimestamp(System.currentTimeMillis());
        return topology;
    }

    private TopologyEditorMetadata createTopologyEditorMetadata (Long topologyId, String info) {
        TopologyEditorMetadata topologyEditorMetadata = new TopologyEditorMetadata();
        topologyEditorMetadata.setTopologyId(topologyId);
        topologyEditorMetadata.setData(info);
        topologyEditorMetadata.setTimestamp(System.currentTimeMillis());
        return topologyEditorMetadata;
    }

    private CustomProcessorInfo createCustomProcessorInfo () {
        CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
        customProcessorInfo.setName("ConsoleCustomProcessor");
        customProcessorInfo.setDescription("Console Custom Processor");
        customProcessorInfo.setImageFileName("image.gif");
        customProcessorInfo.setJarFileName("iotas-core.jar");
        customProcessorInfo.setCustomProcessorImpl(ConsoleCustomProcessor.class.getCanonicalName());
        customProcessorInfo.setStreamingEngine(TopologyLayoutConstants.STORM_STREAMING_ENGINE);
        customProcessorInfo.setConfigFields(getConfigFields());
        customProcessorInfo.setInputSchema(getSchema());
        customProcessorInfo.setOutputStreamToSchema(getOutputStreamsToSchema());
        return customProcessorInfo;
    }

    private List<ConfigField> getConfigFields () {
        List<ConfigField> configFields = new ArrayList<>();
        ConfigField configField = new ConfigField();
        configField.setName("configField");
        configField.setDefaultValue(1);
        configField.setType(ConfigField.Type.NUMBER);
        configField.setTooltip("A number field");
        configField.setIsUserInput(true);
        configField.setIsOptional(true);
        configFields.add(configField);
        return configFields;
    }

    private Schema getSchema () {
        Schema schema = new Schema.SchemaBuilder().field(new Schema.Field("field1", Schema.Type.INTEGER)).build();
        return schema;
    }

    private Map<String, Schema> getOutputStreamsToSchema() {
        Map<String, Schema> outputStreamToSchema = new HashMap<>();
        outputStreamToSchema.put("outputStream", getSchema());
        return outputStreamToSchema;
    }

    private java.io.File getCpImageFile () throws IOException {
        java.io.File imageFile = new java.io.File("/tmp/image.gif");
        IOUtils.copy(IMAGE_FILE_STREAM, new FileOutputStream(imageFile));
        return imageFile;
    }

    private java.io.File getCpJarFile () throws IOException {
        java.io.File fileFile = new java.io.File("/tmp/iotas-core.jar");
        IOUtils.copy(JAR_FILE_STREAM, new FileOutputStream(fileFile));
        return fileFile;
    }

}
