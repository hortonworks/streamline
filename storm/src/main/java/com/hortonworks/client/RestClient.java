package com.hortonworks.client;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;

/**
 * TODO: All the configs should be read from some config file.
 */
public class RestClient {

    private Client client;
    private String rootCatalogURL;

    private static final String DEVICE_URL = "devices";
    private static final String DATASOURCE_URL = "dataSources";
    private static final String FEED_URL = "feeds";
    private static final String PARSER_URL = "parsers";
    private static final String PARSER_DOWNLOAD_URL = PARSER_URL + "/download";

    public RestClient(String rootCatalogURL) {
        this.rootCatalogURL = rootCatalogURL;
        client = Client.create();
        client.setConnectTimeout(1000);
        client.setReadTimeout(5000);
    }

    public <T> T get(String url, Class<T> clazz) {
        WebResource resource = client.resource(url);
        return resource.accept(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.MULTIPART_FORM_DATA_TYPE).get(clazz);
    }

    public ParserInfo getParserInfo(String deviceId, long version) {
        Device device = get(String.format("%s/%s/%s/%s", rootCatalogURL, DEVICE_URL, deviceId, version), Device.class);
        DataSource dataSource = get(String.format("%s/%s/%s", rootCatalogURL, DATASOURCE_URL, device.getDataSourceId()), DataSource.class);
        DataFeed feed = get(String.format("%s/%s/%s", rootCatalogURL, FEED_URL, dataSource.getDatafeedId()), DataFeed.class);
        return get(String.format("%s/%s/%s", rootCatalogURL, PARSER_URL, device.getDataSourceId()), ParserInfo.class);
    }

    public InputStream getParserJar(Long parserId) {
        return get(String.format("%s/%s/%s",rootCatalogURL, PARSER_DOWNLOAD_URL, parserId), InputStream.class);
    }

    public static void main(String[] args) {
        RestClient restClient = new RestClient("http://localhost:8080/api/v1/catalog");
        ParserInfo nest = restClient.getParserInfo("nest", 1);
        System.out.println(nest);
        System.out.println(restClient.getParserJar(nest.getParserId()));
    }

}
