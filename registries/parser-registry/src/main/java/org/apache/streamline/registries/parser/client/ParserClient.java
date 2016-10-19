package org.apache.streamline.registries.parser.client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.catalog.CatalogResponse;
import org.apache.streamline.registries.parser.ParserInfo;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

public class ParserClient {
    private final Client client;
    private final String parserRootUrl;

    public ParserClient (String catalogRootUrl) {
        this(catalogRootUrl, new ClientConfig());
    }

    public ParserClient (String catalogRootUrl, ClientConfig clientConfig) {
        this.parserRootUrl= catalogRootUrl + "/parsers";
        client = ClientBuilder.newClient(clientConfig);
        client.register(MultiPartFeature.class);
    }

    public ParserInfo getParserInfo (Long parserId) {
        Response response = client.target(String.format("%s/%s/", parserRootUrl, parserId)).request().get();
        return getEntity(response, ParserInfo.class);
    }

    public InputStream getParserJar (Long parserId) {
        return client.target(String.format("%s/%s/%s", parserRootUrl, "download", parserId))
                .request(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.MULTIPART_FORM_DATA_TYPE)
                .get(InputStream.class);
    }

    private <T> T getEntity(Response r, Class<T> clazz) {
        try {
            String response = r.readEntity(String.class);
            int responseCode =  getResponseCode(response);
            if (responseCode == CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode()) {
                return null;
            } else if(responseCode != CatalogResponse.ResponseMessage.SUCCESS.getCode()) {
                throw new RuntimeException("Error occurred "+ getResponseMessage(response));
            } else {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response);
                return mapper.treeToValue(node.get("entity"), clazz);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int getResponseCode(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("responseCode"), Integer.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getResponseMessage(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("responseMessage"), String.class);
        }  catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
