/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.webservice.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.hortonworks.streamline.webservice.configurations.ModuleConfiguration;
import com.hortonworks.streamline.webservice.configurations.StreamlineConfiguration;
import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.webservice.resources.StreamlineConfigurationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This JSON serializer serializes StreamlineConfiguration to a format as required by
 * streamline api of StreamlineConfigurationResource
 */
public class StreamlineConfigurationSerializer extends JsonSerializer<StreamlineConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamlineConfigurationResource.class);
    public static final String DEFAULT_VERSION_FILE = "VERSION";
    private final String CONFIG_REGISTRY = "registry";
    private final String CONFIG_REGISTRY_API_URL = "apiUrl";
    private final String CONFIG_HOST = "host";
    private final String CONFIG_PORT = "port";
    private final String CONFIG_DASHBOARD = "dashboard";
    private final String CONFIG_AUTHORIZER = "authorizer";
    private final String CONFIG_VERSION = "version";
    private final String CONFIG_GIT = "git";

    @Override
    public void serialize(StreamlineConfiguration streamlineConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(createConfigurationMap(streamlineConfiguration));
    }

    private Map<String, Object> createConfigurationMap(StreamlineConfiguration streamlineConfiguration) {
        Map<String, Object> conf = new HashMap<>();
        // do not add any storage configuration as we don't want to return username and passwords through api
        conf.put(Constants.CONFIG_MODULES, streamlineConfiguration.getModules());
        conf.put(Constants.CONFIG_CATALOG_ROOT_URL, streamlineConfiguration.getCatalogRootUrl());
        Properties props = readStreamlineVersion();

        if (props.containsKey(CONFIG_VERSION)) {
            conf.put(CONFIG_VERSION, props.get(CONFIG_VERSION));
        }
        if (props.containsKey(CONFIG_GIT)) {
            conf.put(CONFIG_GIT, props.get(CONFIG_GIT));
        }

        //adding schema registry details to make it easier for UI to parser the host & port.
        Map<String, String> registryConf = new HashMap<>();
        for (ModuleConfiguration moduleConfiguration : streamlineConfiguration.getModules()) {
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
        conf.put(CONFIG_DASHBOARD, streamlineConfiguration.getDashboardConfiguration());
        conf.put(CONFIG_AUTHORIZER, streamlineConfiguration.getAuthorizerConfiguration());
        return conf;
    }

    private Properties readStreamlineVersion() {
        String versionFilePath = System.getProperty("streamline.version.file", DEFAULT_VERSION_FILE);
        File versionFile = new File(versionFilePath);
        Properties props = new Properties();
        if (versionFile.exists()) {
            try {
                FileInputStream fileInput = new FileInputStream(versionFile);
                props.load(fileInput);
                fileInput.close();
            } catch (IOException ie) {
                LOG.info("Failed to read VERSION file");
            }
        }
        return props;
    }
}
