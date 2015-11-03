package com.hortonworks.iotas.webservice;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.catalog.Cluster;
import com.hortonworks.iotas.catalog.Component;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.test.IntegrationTest;
import com.hortonworks.iotas.webservice.catalog.dto.DataSourceDto;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Tests the entire code path for our rest APIs. Currently tests Post, Put, Get(list, ById) and Delete.
 */
@Category(IntegrationTest.class)
public class RestIntegrationTest {

    /**
     * See https://dropwizard.github.io/dropwizard/manual/testing.html#integration-testing
     */
    @ClassRule
    public static final DropwizardAppRule RULE = new DropwizardAppRule(IotasApplication.class, ResourceHelpers.resourceFilePath("iotas-test.yaml"));

    private String rootUrl = String.format("http://localhost:%d/api/v1/catalog/", RULE.getLocalPort());

    /**
     * A Test element holder class
     */
    private class ResourceTestElement {
        Object resourceToPost; // resource that will be used to test post
        Object resourceToPut; // resource that will be used to test put
        String id; //Id by which Get(id) and Delete(id) will be tested, should match the actual Id set in post/put request.
        String url; //Rest Url to test.

        public ResourceTestElement(Object resourceToPost, Object resourceToPut, String id, String url) {
            this.resourceToPost = resourceToPost;
            this.resourceToPut = resourceToPut;
            this.id = id;
            this.url = url;
        }
    }

    /**
     * List of all things that will be tested
     */
    private Collection<ResourceTestElement> resourcesToTest = Lists.newArrayList(
            new ResourceTestElement(createDataFeed(1l, "testDataFeed"), createDataFeed(1l, "testDataFeedPut"), "1", rootUrl + "feeds"),
            new ResourceTestElement(createDataSource(1l, "testDataSource"), createDataSource(1l, "testDataSourcePut"), "1", rootUrl + "datasources"),
            new ResourceTestElement(createClusterInfo(1l, "testCluster"), createClusterInfo(1l, "testClusterPut"), "1", rootUrl + "clusters"),
            new ResourceTestElement(createComponent(1l, "testComponent"), createComponent(1l, "testComponentPut"), "1", rootUrl + "clusters/1/components"),
            new ResourceTestElement(createNotifierInfo(1l, "testNotifier"), createNotifierInfo(1l, "testNotifierPut"), "1", rootUrl + "notifiers"),

            new ResourceTestElement(createDataSourceDto(1l, "testDataSourceWithDataFeed:"+System.currentTimeMillis()),
                    createDataSourceDto(1l, "testDataSourceWithDataFeedPut:"+System.currentTimeMillis()), "1", rootUrl + "datasourceswithdatafeed")

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
            System.out.println("####################### response = " + response);
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
        ds.setDataSourceId(id);
        ds.setDataSourceName(name);
        ds.setDescription("desc");
        ds.setTags("t1, t2, t3");
        ds.setTimestamp(System.currentTimeMillis());
        ds.setType(DataSource.Type.DEVICE);
        ds.setTypeConfig("{\"deviceId\":\"1\",\"version\":1}");
        return ds;
    }

    private DataFeed createDataFeed(Long id, String name) {
        DataFeed df = new DataFeed();
        df.setDataFeedId(id);
        df.setDataSourceId(1L);
        df.setDataFeedName(name);
        df.setEndpoint("kafka://host:port/topic");
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
        df.setDataFeedId(System.currentTimeMillis());
        df.setDataSourceId(datasourceId);
        df.setDataFeedName(feedName);
        df.setEndpoint("kafka://host:port/topic");
        df.setParserId(datasourceId);
        return df;
    }

    private ParserInfo createParserInfo(Long id, String name) {
        ParserInfo pi = new ParserInfo();
        pi.setParserId(id);
        pi.setParserName(name);
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

    private Component createComponent(Long id, String name) {
        Component component = new Component();
        component.setClusterId(1l);
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
        notifierInfo.setNotifierName(name);
        return notifierInfo;
    }
}
