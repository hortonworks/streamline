package org.apache.streamline.registries.model.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.catalog.CatalogResponse;
import org.glassfish.jersey.client.ClientConfig;
import org.apache.streamline.registries.model.data.ModelInfo;
import org.glassfish.jersey.media.multipart.MultiPartFeature;


import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

/**
 * Created by schendamaraikannan on 12/5/16.
 */
public final class ModelRegistryClient {
    private final String catalogURL;
    private final String modelRegistryURL;
    private final Client client;

    public ModelRegistryClient(String catalogURL) {
        this(catalogURL, new ClientConfig());
    }

    public ModelRegistryClient(String catalogURL, ClientConfig clientConfig) {
        this.catalogURL = catalogURL;
        this.modelRegistryURL = String.join("/", catalogURL, "models");
        client = ClientBuilder.newClient(clientConfig);
        client.register(MultiPartFeature.class);
    }
    public ModelInfo getModelInfo(Long modelId) {
        Response response = client.target(String.format("%s/%s", modelRegistryURL, modelId)).request().get();
        return getEntity(response, ModelInfo.class);
    }

    public InputStream downloadFile(Long modelId) throws IOException {
        return (InputStream) client.target(String.format("%s/%s/%s", modelRegistryURL, "files", modelId)).request().get(InputStream.class);
    }

    private <T> T getEntity(Response r, Class<T> clazz) {
        try {
            String response = r.readEntity(String.class);
            int responseCode =  getResponseCode(response);
            if (responseCode == CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode()) {
                return null;
            }
            else if(responseCode != CatalogResponse.ResponseMessage.SUCCESS.getCode()) {
                throw new RuntimeException("Error occurred "+ getResponseMessage(response));
            }
            else {
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
