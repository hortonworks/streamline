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

package com.hortonworks.streamline.streams.common.utils;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

/**
 * TODO: All the configs should be read from some config file.
 */
public class CatalogRestClient {

    private final Client client;

    private static final String NOTIFIER_URL = "notifiers";
    private static final String FILE_DOWNLOAD_URL = "files/download/";

    private final String rootCatalogURL;

    //TODO: timeouts should come from a config so probably make them constructor args.
    public CatalogRestClient(String rootCatalogURL) {
        this(rootCatalogURL, new ClientConfig());
    }

    public CatalogRestClient(String rootCatalogURL, ClientConfig clientConfig) {
        this.rootCatalogURL = rootCatalogURL;
        client = ClientBuilder.newClient(clientConfig);
        client.register(MultiPartFeature.class);
    }

    public InputStream getFile(Long jarId) {
        return getInputStream(jarId.toString(), FILE_DOWNLOAD_URL);
    }

    protected InputStream getInputStream(String fileId, String relativeUrl) {
        try {
            return client.target(String.format("%s/%s/%s", rootCatalogURL, relativeUrl, fileId))
                    .request(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.MULTIPART_FORM_DATA_TYPE)
                    .get(InputStream.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
