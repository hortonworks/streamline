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
package org.apache.streamline.webservice;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.streamline.common.Schema;
import org.apache.streamline.common.catalog.CatalogResponse;
import org.apache.streamline.common.test.IntegrationTest;
import org.apache.streamline.registries.parser.ParserInfo;
import org.apache.streamline.registries.tag.Tag;
import org.apache.streamline.registries.tag.TaggedEntity;
import org.apache.streamline.registries.tag.dto.TagDto;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.Component;
import org.apache.streamline.streams.catalog.FileInfo;
import org.apache.streamline.streams.catalog.NotifierInfo;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.ServiceConfiguration;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyEditorMetadata;
import org.apache.streamline.streams.catalog.processor.CustomProcessorInfo;
import org.apache.streamline.streams.catalog.topology.ConfigField;
import org.apache.streamline.streams.catalog.topology.TopologyComponentDefinition;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.runtime.processor.ConsoleCustomProcessorRuntime;
import org.apache.streamline.streams.service.TopologyCatalogResource;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
    public static final DropwizardAppRule<StreamlineConfiguration> RULE = new DropwizardAppRule<>(StreamlineApplication.class, ResourceHelpers.resourceFilePath("streamline-test.yaml"));

    private String rootUrl = String.format("http://localhost:%d/api/v1/catalog/", RULE.getLocalPort());
    private final InputStream JAR_FILE_STREAM = new ByteArrayInputStream("some jar gibberish".getBytes());
    /**
     * A Test element holder class
     */
    private class ResourceTestElement {
        final Object resourceToPost; // resource that will be used to test post
        final Object resourceToPut; // resource that will be used to test put
        final String id; //Id by which Get(id) and Delete(id) will be tested, should match the actual Id set in post/put request.
        final String url; //Rest Url to test.
        boolean multipart;
        String entityNameHeader;
        String fileNameHeader;
        File fileToUpload;
        List<String> fieldsToIgnore;

        List<ResourceTestElement> resourcesToPostFirst; // dependent entities

        public ResourceTestElement(Object resourceToPost, Object resourceToPut, String id, String url) {
            this.resourceToPost = resourceToPost;
            this.resourceToPut = resourceToPut;
            this.id = id;
            this.url = url;

            resourcesToPostFirst = new ArrayList<>();
        }

        public ResourceTestElement withMultiPart() {
            this.multipart = true;
            return this;
        }

        public ResourceTestElement withEntitiyNameHeader(String entitiyNameHeader) {
            this.entityNameHeader = entitiyNameHeader;
            return this;
        }

        public ResourceTestElement withFileNameHeader(String fileNameHeader) {
            this.fileNameHeader = fileNameHeader;
            return this;
        }

        public ResourceTestElement withFileToUpload(String fileName) {
            try {
                this.fileToUpload = Paths.get(this.getClass().getClassLoader().getResource(fileName).toURI()).toFile();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public ResourceTestElement withFieldsToIgnore(List<String> fields) {
            this.fieldsToIgnore = fields;
            return this;
        }

        public ResourceTestElement withDependentResource(ResourceTestElement resourceToPostFirst) {
            this.resourcesToPostFirst.add(resourceToPostFirst);
            return this;
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

    private ResourceTestElement clusterResourceToTest = new ResourceTestElement(
        createCluster(1l, "testCluster"), createCluster(1l, "testClusterPut"), "1", rootUrl + "clusters");

    private ResourceTestElement serviceResourceToTest = new ResourceTestElement(
        createService(1l, 1l, "testService"), createService(1l, 1l, "testServicePut"), "1", rootUrl + "clusters/1/services")
        .withDependentResource(clusterResourceToTest);

    private ResourceTestElement serviceConfigurationResourceToTest = new ResourceTestElement(
        createServiceConfig(1l, 1l, "testServiceConfig"), createServiceConfig(1l, 1l, "testServiceConfigPut"), "1",
        rootUrl + "services/1/configurations")
        .withDependentResource(clusterResourceToTest).withDependentResource(serviceResourceToTest);

    private ResourceTestElement componentResourceToTest = new ResourceTestElement(
        createComponent(1l, 1l, "testComponent"), createComponent(1l, 1l, "testComponentPut"), "1", rootUrl + "services/1/components")
        .withDependentResource(clusterResourceToTest).withDependentResource(serviceResourceToTest);

    /**
     * List of all things that will be tested
     */
    private Collection<ResourceTestElement> resourcesToTest = Lists.newArrayList(
            new ResourceTestElement(createTag(1L, "foo-tag"), createTag(1L, "foo-tag-new"), "1", rootUrl + "tags"),
            // TODO: The below test case needs to be fixed since it should first create the data source and then add the corresponding datafeed
            //new ResourceTestElement(createDataFeed(1l, "testDataFeed"), createDataFeed(1l, "testDataFeedPut"), "1", rootUrl + "feeds"),
            clusterResourceToTest, serviceResourceToTest, componentResourceToTest,
            new ResourceTestElement(createNotifierInfo(1l, "testNotifier"), createNotifierInfo(1l, "testNotifierPut"), "1", rootUrl + "notifiers"),
            new ResourceTestElement(createTopology(1l, "iotasTopology"), createTopology(1l, "iotasTopologyPut"), "1", rootUrl + "topologies"),
            new ResourceTestElement(createTopologyEditorMetadata(1l, "{\"x\":5,\"y\":6}"), createTopologyEditorMetadata(1l, "{\"x\":6,\"y\":5}"), "1", rootUrl + "system/topologyeditormetadata"),
            new ResourceTestElement(createTopologyComponent(1l, "kafkaSpoutComponent", TopologyComponentDefinition.TopologyComponentType.SOURCE, "KAFKA"), createTopologyComponent(1l, "kafkaSpoutComponentPut", TopologyComponentDefinition.TopologyComponentType.SOURCE, "KAFKA") , "1", rootUrl + "system/componentdefinitions/SOURCE"),
            new ResourceTestElement(createTopologyComponent(2l, "parserProcessor", TopologyComponentDefinition.TopologyComponentType.PROCESSOR, "PARSER"), createTopologyComponent(2l, "parserProcessorPut", TopologyComponentDefinition.TopologyComponentType.PROCESSOR, "PARSER"), "2", rootUrl + "system/componentdefinitions/PROCESSOR"),
            new ResourceTestElement(createTopologyComponent(3l, "hbaseSink", TopologyComponentDefinition.TopologyComponentType.SINK, "HBASE"), createTopologyComponent(3l, "hbaseSinkPut", TopologyComponentDefinition.TopologyComponentType.SINK, "HBASE"), "3", rootUrl + "system/componentdefinitions/SINK"),
            new ResourceTestElement(createTopologyComponent(4l, "shuffleGroupingLink", TopologyComponentDefinition.TopologyComponentType.LINK, "SHUFFLE"), createTopologyComponent(4l, "shuffleGroupingLinkPut", TopologyComponentDefinition.TopologyComponentType.LINK, "SHUFFLE"), "4", rootUrl + "system/componentdefinitions/LINK"),
            // parser is commented as parser takes a jar as input along with the parserInfo instance and so it needs a multipart request.
            new ResourceTestElement(createParserInfo(1l, "testParser"), null, "1", rootUrl + "parsers")
                                    .withMultiPart().withEntitiyNameHeader("parserInfo").withFileNameHeader("parserJar")
                                    .withFileToUpload("parser.jar").withFieldsToIgnore(Collections.singletonList("jarStoragePath"))
    );

    private MultiPart getMultiPart(ResourceTestElement resourceToTest, Object entity) {
        MultiPart multiPart = new MultiPart();
        BodyPart filePart = new FileDataBodyPart(resourceToTest.fileNameHeader, resourceToTest.fileToUpload);
        BodyPart entityPart = new FormDataBodyPart(resourceToTest.entityNameHeader, entity, MediaType.APPLICATION_JSON_TYPE);
        multiPart.bodyPart(filePart).bodyPart(entityPart);
        return multiPart;
    }

    public <T> T filterFields(T object, List<String> fields) throws Exception {
        if (fields != null && !fields.isEmpty()) {
            Class<?> clazz = object.getClass();
            for (String fieldName : fields) {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(object, null);
            }
        }
        return object;
    }
    /**
     * For each TestResource element in resourcesToTest List, tests Post, Put, Get and Delete.
     *
     * @throws Exception
     */
    @Test
    public void testAllResources() throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig());
        client.register(MultiPartFeature.class);
        for (ResourceTestElement resourceToTest : resourcesToTest) {
            String url = resourceToTest.url;
            Object resourceToPost = resourceToTest.resourceToPost;
            Object resourceToPut = resourceToTest.resourceToPut;
            List<ResourceTestElement> resourcesToPostFirst = resourceToTest.resourcesToPostFirst;

            for (ResourceTestElement dependantResource : resourcesToPostFirst) {
                String response;
                if (dependantResource.multipart) {
                    response = client.target(dependantResource.url).request().post(Entity.entity(getMultiPart(dependantResource, dependantResource.resourceToPost),
                        MediaType.MULTIPART_FORM_DATA), String.class);
                } else {
                    response = client.target(dependantResource.url).request().post(Entity.json(dependantResource.resourceToPost), String.class);
                }
                Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            }

            String id = resourceToTest.id;
            String response;

            if (resourceToTest.multipart) {
                response = client.target(url).request().post(Entity.entity(getMultiPart(resourceToTest, resourceToPost),
                                                                           MediaType.MULTIPART_FORM_DATA), String.class);
            } else {
                response = client.target(url).request().post(Entity.json(resourceToPost), String.class);
            }
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

            response = client.target(url).request().get(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            Assert.assertEquals(Lists.newArrayList(filterFields(resourceToPost, resourceToTest.fieldsToIgnore)),
                                getEntities(response, resourceToPost.getClass(), resourceToTest.fieldsToIgnore));

            url = url + "/" + id;
            response = client.target(url).request().get(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            Assert.assertEquals(resourceToPost, getEntity(response, resourceToPost.getClass(), resourceToTest.fieldsToIgnore));

            if (resourceToPut != null) {
                if (resourceToTest.multipart) {
                    response = client.target(url).request().put(Entity.entity(getMultiPart(resourceToTest, resourceToPut),
                                                                              MediaType.MULTIPART_FORM_DATA), String.class);
                } else {
                    response = client.target(url).request().put(Entity.json(resourceToPut), String.class);
                }
                Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
                response = client.target(url).request().get(String.class);
                Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
                Assert.assertEquals(resourceToPut, getEntity(response, resourceToPut.getClass(), resourceToTest.fieldsToIgnore));
            }

            try {
              response = client.target(url).request().delete(String.class);
              Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
            } finally {
              for (ResourceTestElement dependantResource : resourcesToPostFirst) {
                response = client.target(dependantResource.url + "/" + dependantResource.id).request().delete(String.class);
                Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
              }
            }

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
     * Test whether service API can distinguish cluster
     *
     * @throws Exception
     */
    @Test
    public void testServiceAPIsCanDistinguishCluster() throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig());

        Long clusterId = 1L;

        // create Cluster first
        storeTestCluster(client, clusterId);

        String serviceBaseUrl = rootUrl + String.format("clusters/%d/services", clusterId);

        Service service = createService(clusterId, 1L, "testService:"+1);

        String serviceEntityUrl = serviceBaseUrl + "/" + 1;

        String response = client.target(serviceEntityUrl).request()
                .put(Entity.json(service), String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

        Long anotherClusterId = 2L;

        serviceBaseUrl = rootUrl + String.format("clusters/%d/services", anotherClusterId);
        response = client.target(serviceBaseUrl).request().get(String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(Collections.emptyList(), getEntities(response, Service.class));

        serviceEntityUrl = serviceBaseUrl + "/" + 1;
        try {
            client.target(serviceEntityUrl).request().get(String.class);
            Assert.fail("Should have thrown NotFoundException.");
        } catch (NotFoundException e) {
            response = e.getResponse().readEntity(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode(), getResponseCode(response));
        }

        removeService(client, clusterId, service.getId());
        removeCluster(client, clusterId);
    }

    /**
     * Test whether component API can distinguish cluster and service
     *
     * @throws Exception
     */
    @Test
    public void testComponentAPIsCanDistinguishCluster() throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig());

        Long clusterId = 1L;
        Long serviceId = 1L;

        // create Cluster and Service first
        storeTestCluster(client, clusterId);
        storeTestService(client, clusterId, serviceId);

        String componentBaseUrl = rootUrl + String.format("services/%d/components", clusterId, serviceId);

        Component component = createComponent(serviceId, 1L, "testComponent:"+1);

        String componentEntityUrl = componentBaseUrl + "/" + 1;
        String response = client.target(componentEntityUrl).request().put(Entity.json(component), String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));

        Long anotherClusterId = 2L;
        Long anotherServiceId = 2L;

        componentBaseUrl = rootUrl + String.format("services/%d/components", anotherClusterId, anotherServiceId);
        response = client.target(componentBaseUrl).request().get(String.class);
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(Collections.emptyList(), getEntities(response, Component.class));

        componentEntityUrl = componentBaseUrl + "/" + 1;
        try {
            client.target(componentEntityUrl).request().get(String.class);
            Assert.fail("Should have thrown NotFoundException.");
        } catch (NotFoundException e) {
            response = e.getResponse().readEntity(String.class);
            Assert.assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode(), getResponseCode(response));
        }

        removeComponent(client, clusterId, serviceId, component.getId());
        removeService(client, clusterId, serviceId);
        removeCluster(client, clusterId);
    }

    private void storeTestCluster(Client client, Long clusterId) {
        Cluster cluster = createCluster(clusterId, "testcluster");
        String clusterBaseUrl = rootUrl + String.format("clusters");
        client.target(clusterBaseUrl).request().post(Entity.json(cluster));
    }

    private void storeTestService(Client client, Long clusterId, Long serviceId) {
        Service service = createService(clusterId, serviceId, "test");
        String serviceBaseUrl = rootUrl + String.format("clusters/%d/services", clusterId);
        client.target(serviceBaseUrl).request().post(Entity.json(service));
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
        Object expected = Arrays.asList(TopologyComponentDefinition.TopologyComponentType.values());
        Object actual = getEntities(response, TopologyComponentDefinition.TopologyComponentType.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTopologyComponentsForTypeWithFilters () throws Exception {
        String prefixUrl = rootUrl + "system/componentdefinitions/";
        String[] postUrls = {
                prefixUrl + TopologyComponentDefinition.TopologyComponentType.SOURCE,
                prefixUrl + TopologyComponentDefinition.TopologyComponentType.PROCESSOR,
                prefixUrl + TopologyComponentDefinition.TopologyComponentType.SINK,
                prefixUrl + TopologyComponentDefinition.TopologyComponentType.LINK
        };
        List<List<Object>> resourcesToPost = new ArrayList<List<Object>>();
        Object source = createTopologyComponent(1l, "kafkaSpoutComponent", TopologyComponentDefinition.TopologyComponentType.SOURCE, "KAFKA");
        Object parser = createTopologyComponent(2l, "parserProcessor", TopologyComponentDefinition.TopologyComponentType.PROCESSOR, "PARSER");
        Object sink = createTopologyComponent(3l, "hbaseSink", TopologyComponentDefinition.TopologyComponentType.SINK, "HBASE");
        Object link = createTopologyComponent(4l, "shuffleGroupingLink", TopologyComponentDefinition.TopologyComponentType.LINK, "SHUFFLE");
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
            for (String queryParam : getUrlQueryParms.get(i)) {
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
        getUrlQueryParms.add(prefixQueryParam + "&name=ConsoleCustomProcessorRuntime");
        getUrlQueryParms.add(prefixQueryParam + "&jarFileName=streamline-core.jar");
        getUrlQueryParms.add(prefixQueryParam + "&customProcessorImpl=" + ConsoleCustomProcessorRuntime.class.getCanonicalName());
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
        FileInfo file = new FileInfo();
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
        FileInfo postedFile = getEntity(response, FileInfo.class);

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
        List<FileInfo> files = getEntities(response, FileInfo.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(files.size(), 1);
        Assert.assertEquals(files.iterator().next().getName(), file.getName());


        // GET /files/1
        response = client.target(url+"/"+ postedFile.getId()).request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        FileInfo receivedFile = getEntity(response, FileInfo.class);

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
        FileInfo updatedFile = getEntity(response, FileInfo.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(updatedFile.getId(), postedFile.getId());
        Assert.assertEquals(updatedFile.getName(), postedFile.getName());


        // DELETE
        response = client.target(url+"/"+ updatedFile.getId()).request().delete(String.class);
        final FileInfo deletedFile = getEntity(response, FileInfo.class);

        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
        Assert.assertEquals(deletedFile.getId(), updatedFile.getId());


        // GET
        response = client.target(url).request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        files = getEntities(response, FileInfo.class);

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
            for (int i = 0; i < qpte.getUrls.size(); ++i) {
                 String getUrl = qpte.getUrls.get(i);
                response = client.target(getUrl).request().get(String.class);
                Assert.assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), getResponseCode(response));
                 Assert.assertEquals(Collections.emptyList(), getEntities(response, qpte.getResults.get(i).getClass()));
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

    private <T extends Object> List<T> getEntities(String response, Class<T> clazz) {
        return getEntities(response, clazz, Collections.<String>emptyList());
    }

    /**
     * Get the entities from response string
     *
     * @param response
     * @param clazz
     * @param <T>
     * @return
     */
    private <T extends Object> List<T> getEntities(String response, Class<T> clazz,
                                                   List<String> fieldsToIgnore) {
        List<T> entities = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.get("entities").elements();
            while (it.hasNext()) {
                entities.add(filterFields(mapper.treeToValue(it.next(), clazz), fieldsToIgnore));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return entities;
    }

    private <T> T getEntity(String response, Class<T> clazz) {
        return getEntity(response, clazz, Collections.<String>emptyList());
    }

    /**
     * Get entity from the response string.
     *
     * @param response
     * @param clazz
     * @param <T>
     * @return
     */
    private <T> T getEntity(String response, Class<T> clazz,
                                           List<String> fieldsToIgnore) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return filterFields(mapper.treeToValue(node.get("entity"), clazz), fieldsToIgnore);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    //============== Helper methods to create the actual objects that the rest APIS expect as Input ==========//

    private ParserInfo createParserInfo(Long id, String name) {
        ParserInfo pi = new ParserInfo();
        pi.setId(id);
        pi.setName(name);
        pi.setClassName("org.apache.streamline.registries.parser.nest.NestParser");
        pi.setJarStoragePath("/tmp/parser.jar");
        pi.setParserSchema(new Schema.SchemaBuilder().fields(new Schema.Field("deviceId", Schema.Type.LONG),
                new Schema.Field("deviceName", Schema.Type.STRING)).build());
        pi.setVersion(0l);
        pi.setTimestamp(System.currentTimeMillis());
        return pi;
    }

    private TagDto createTag(Long id, String name, List<Long> tagIds) {
        TagDto tag = new TagDto();
        tag.setId(id);
        tag.setName(name);
        tag.setDescription("test");
        tag.setTimestamp(System.currentTimeMillis());
        tag.setTagIds(tagIds);
        return tag;
    }

    private TagDto createTag(Long id, String name) {
        return createTag(id, name, Collections.<Long>emptyList());
    }

    private Cluster createCluster(Long id, String name) {
        Cluster cluster = new Cluster();
        cluster.setDescription("test");
        cluster.setId(id);
        cluster.setName(name);
        cluster.setTimestamp(System.currentTimeMillis());
        return cluster;
    }

    private Service createService(Long clusterId, Long id, String name) {
        Service service = new Service();
        service.setDescription("test-component");
        service.setName(name);
        service.setId(id);
        service.setClusterId(clusterId);
        service.setTimestamp(System.currentTimeMillis());
        return service;
    }

    private ServiceConfiguration createServiceConfig(Long serviceId, Long id, String name) {
        ServiceConfiguration configuration = new ServiceConfiguration();
        configuration.setId(id);
        configuration.setServiceId(serviceId);
        configuration.setName(name);
        configuration.setFilename("core-site.xml");
        configuration.setTimestamp(System.currentTimeMillis());
        configuration.setDescription("core site of HDFS");
        configuration.setConfiguration("{\"configkey\": \"value\"}");
        return configuration;
    }

    private Component createComponent(Long serviceId, Long id, String name) {
        Component component = new Component();
        component.setHosts(Lists.newArrayList("host-1","host-2"));
        component.setId(id);
        component.setName(name);
        component.setPort(8080);
        component.setServiceId(serviceId);
        component.setTimestamp(System.currentTimeMillis());
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

    private TopologyComponentDefinition createTopologyComponent (Long id, String name,
                                                                 TopologyComponentDefinition
                                                               .TopologyComponentType topologyComponentType, String subType) {
        TopologyComponentDefinition topologyComponentDefinition = new TopologyComponentDefinition();
        topologyComponentDefinition.setId(id);
        topologyComponentDefinition.setName(name);
        topologyComponentDefinition.setType(topologyComponentType);
        topologyComponentDefinition.setStreamingEngine("STORM");
        topologyComponentDefinition.setConfig("{}");
        topologyComponentDefinition.setSubType(subType);
        topologyComponentDefinition.setTimestamp(System.currentTimeMillis());
        topologyComponentDefinition.setTransformationClass("org.apache.streamline.streams.layout.storm.KafkaSpoutFluxComponent");
        return topologyComponentDefinition;

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
        customProcessorInfo.setName("ConsoleCustomProcessorRuntime");
        customProcessorInfo.setDescription("Console Custom Processor");
        customProcessorInfo.setJarFileName("streamline-core.jar");
        customProcessorInfo.setCustomProcessorImpl(ConsoleCustomProcessorRuntime.class.getCanonicalName());
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
        return new Schema.SchemaBuilder().field(new Schema.Field("field1", Schema.Type.INTEGER)).build();
    }

    private Map<String, Schema> getOutputStreamsToSchema() {
        Map<String, Schema> outputStreamToSchema = new HashMap<>();
        outputStreamToSchema.put("outputStream", getSchema());
        return outputStreamToSchema;
    }

    private java.io.File getCpJarFile () throws IOException {
        java.io.File fileFile = new java.io.File("/tmp/streamline-core.jar");
        IOUtils.copy(JAR_FILE_STREAM, new FileOutputStream(fileFile));
        return fileFile;
    }


    private void removeCluster(Client client, Long clusterId) {
        client.target(rootUrl + String.format("clusters/%d", clusterId)).request().delete();
    }

    private void removeService(Client client, Long clusterId, Long serviceId) {
        client.target(rootUrl + String.format("clusters/%d/services/%d", clusterId, serviceId))
            .request().delete();
    }

    private void removeComponent(Client client, Long clusterId, Long serviceId, Long componentId) {
        client.target(rootUrl + String.format("clusters/%d/services/%d/components/%d", clusterId, serviceId, componentId))
            .request().delete();
    }
}
