package org.apache.streamline.webservice;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.Constants;
import org.apache.streamline.common.util.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;



@Path("/v1/config")
@Produces(MediaType.APPLICATION_JSON)

public class StreamlineConfigurationResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineConfigurationResource.class);
    private Map<String, Object> conf;

    private final String CONFIG_REGISTRY = "registry";
    private final String CONFIG_REGISTRY_API_URL = "apiUrl";
    private final String CONFIG_HOST = "host";
    private final String CONFIG_PORT = "port";
    private final String CONFIG_PIVOT = "pivot";



    public StreamlineConfigurationResource(StreamlineConfiguration streamlineConfiguration) {
        this.conf = new HashMap<>();
        buildStreamlineConfig(streamlineConfiguration);
    }

    /**
     * List ALL streamline configuration.
     */
    @GET
    @Path("/streamline")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStreamlineConfiguraiton(@Context UriInfo uriInfo) {
        return WSUtils.respondEntity(this.conf, OK);
    }

    private void buildStreamlineConfig(StreamlineConfiguration streamlineConfiguration) {
        // do not add any storage configuration as we don't want to return username and passwords through api
        conf.put(Constants.CONFIG_MODULES, streamlineConfiguration.getModules());
        conf.put(Constants.CONFIG_CATALOG_ROOT_URL, streamlineConfiguration.getCatalogRootUrl());

        //adding schema regisry details to make it easier for UI to parser the host & port.
        Map<String, String> registryConf = new HashMap<>();
        for (ModuleConfiguration moduleConfiguration: streamlineConfiguration.getModules()) {
            String moduleName = moduleConfiguration.getName();
            if (moduleName.equals(Constants.CONFIG_STREAMS_MODULE)) {
                String schemaRegistryUrl = (String) moduleConfiguration.getConfig().get(Constants.CONFIG_SCHEMA_REGISTRY_URL);
                registryConf.put(CONFIG_REGISTRY_API_URL, schemaRegistryUrl);
                try {
                    URL url = new URL(schemaRegistryUrl);
                    registryConf.put(CONFIG_HOST, url.getHost());
                    registryConf.put(CONFIG_PORT, String.valueOf(url.getPort()));
                } catch (Exception e) {
                    LOG.error("Failed to parse the schemaRegistryUrl due to {}", e);
                }
            }
        }
        conf.put(CONFIG_REGISTRY, registryConf);
        conf.put(CONFIG_PIVOT, streamlineConfiguration.getPivotConfiguration());

    }


}
