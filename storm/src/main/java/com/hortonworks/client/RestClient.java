package com.hortonworks.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * TODO: All the configs should be read from some config file.
 */
public class RestClient {

    private Client client;
    private String rootCatalogURL;

    private static final String DEVICE_URL = "devices";
    private static final String DATASOURCE_URL = "datasources";
    private static final String FEED_URL = "feeds";
    private static final String PARSER_URL = "parsers";
    private static final String PARSER_DOWNLOAD_URL = PARSER_URL + "/download";

    //TODO: timeouts should come from a config so probably make them constructor args.
    public RestClient(String rootCatalogURL) {
        this.rootCatalogURL = rootCatalogURL;
        client = Client.create();
        client.setConnectTimeout(1000);
        client.setReadTimeout(5000);
    }

    public <T> T get(String url, Class<T> clazz) {
        try {
            WebResource resource = client.resource(url);
            return resource.accept(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.MULTIPART_FORM_DATA_TYPE).get(clazz);
        } catch (Exception ex) {
            //System.out.println(ex);
        }
        return null;
    }

    public <T> List<T> getEntities(String url, Class<T> clazz) {
        List<T> entities = new ArrayList<T>();
        try {
            WebResource resource = client.resource(url);
            String response = resource.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.get("entities").elements();
            while (it.hasNext()) {
                entities.add(mapper.treeToValue(it.next(), clazz));
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return entities;
    }

    public <T> T getEntity(String url, Class<T> clazz) {
        try {
            WebResource resource = client.resource(url);
            String response = resource.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("entity"), clazz);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return null;
    }

    public ParserInfo getParserInfo(String deviceId, Long version) {
        DataSource dataSource = getEntities(String.format("%s/%s/type/DEVICE/?deviceId=%s&version=%s",
                                            rootCatalogURL, DATASOURCE_URL, deviceId, version), DataSource.class).get(0);

        DataFeed dataFeed = getEntities(String.format("%s/%s/?dataSourceId=%s",
                                        rootCatalogURL, FEED_URL, dataSource.getDataSourceId()), DataFeed.class).get(0);

        return getEntity(String.format("%s/%s/%s", rootCatalogURL, PARSER_URL, dataFeed.getParserId()), ParserInfo.class);
    }

    public InputStream getParserJar(Long parserId) {
        return get(String.format("%s/%s/%s", rootCatalogURL, PARSER_DOWNLOAD_URL, parserId), InputStream.class);
    }

    public static void main(String[] args) {
        RestClient restClient = new RestClient("http://localhost:8080/api/v1/catalog");
        ParserInfo nest = restClient.getParserInfo("nest", 1L);
        System.out.println(nest);
        System.out.println(restClient.getParserJar(nest.getParserId()));
    }

}
